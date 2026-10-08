package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.EspecialidadeEncontro
import com.example.model.ExaltedConstants
import kotlin.random.Random

/** Pure distribution rules used by encounter generation. */
object EncounterDistributionService {

    // --- 5. Distribuição de atributos: primário+8, secundário+6, terciário+4, todos começando em 1 ---
    fun distribuirAtributos(
        primarios: List<String>,
        secundarios: List<String>,
        terciarios: List<String>,
        random: Random,
        arquetipo: ArquetipoEncontro? = null
    ): Map<String, Int> {
        val todos = ExaltedConstants.PHYSICAL_ATTRIBUTES + ExaltedConstants.SOCIAL_ATTRIBUTES + ExaltedConstants.MENTAL_ATTRIBUTES
        // Correção de bug: a versão anterior distribuía os pontos extras em
        // ordem fixa (round-robin sempre começando do índice 0), então o
        // mesmo grupo de Atributos (ex.: sempre Força/Destreza/Vigor como
        // Primário no Físico) produzia sempre o MESMO resultado — nenhuma
        // variação real entre NPCs gerados, mesmo em rodadas diferentes.
        // Agora cada ponto sorteia aleatoriamente entre os Atributos do
        // grupo que ainda não bateram o teto de 5.
        fun distribuirDentro(lista: List<String>, pontosExtras: Int): Map<String, Int> {
            if (lista.isEmpty() || pontosExtras <= 0) return lista.associateWith { 1 }
            val valores = lista.associateWith { 1 }.toMutableMap()
            var restante = pontosExtras
            // Evita criar uma nova List via filter() para cada ponto.
            // Seleciona o n-ésimo atributo disponível em uma única passagem.
            while (restante > 0) {
                var disponiveis = 0
                for (nome in lista) if (valores.getValue(nome) < 5) disponiveis++
                if (disponiveis == 0) break
                val alvoIndice = random.nextInt(disponiveis)
                var cursor = 0
                var alvo: String? = null
                for (nome in lista) {
                    if (valores.getValue(nome) < 5) {
                        if (cursor == alvoIndice) { alvo = nome; break }
                        cursor++
                    }
                }
                val nomeAlvo = alvo ?: break
                valores[nomeAlvo] = valores.getValue(nomeAlvo) + 1
                restante--
            }
            return valores
        }
        val mapaPrimarios = distribuirDentro(primarios, 8).toMutableMap()
        // Otimização de qualidade: qualquer arquétipo deve ter pelo menos um
        // Atributo 5 em sua categoria primária. Isso evita planilhas diluídas
        // (4/4/3) e torna o conceito do arquétipo imediatamente perceptível.
        if (mapaPrimarios.values.maxOrNull() ?: 1 < 5) {
            val alvo = primarios.maxByOrNull { mapaPrimarios[it] ?: 1 } ?: primarios.firstOrNull()
            if (alvo != null) {
                var menor = primarios.minByOrNull { mapaPrimarios[it] ?: 1 }
                while ((mapaPrimarios[alvo] ?: 1) < 5 && menor != null && (mapaPrimarios[menor] ?: 1) > 1) {
                    mapaPrimarios[alvo] = (mapaPrimarios[alvo] ?: 1) + 1
                    mapaPrimarios[menor] = (mapaPrimarios[menor] ?: 1) - 1
                    menor = primarios.minByOrNull { mapaPrimarios[it] ?: 1 }
                }
            }
        }
        val mapaSecundarios = distribuirDentro(secundarios, 6)
        val mapaTerciarios = distribuirDentro(terciarios, 4)
        val resultado = mutableMapOf<String, Int>()
        todos.forEach { resultado[it] = 1 }
        resultado.putAll(mapaPrimarios)
        resultado.putAll(mapaSecundarios)
        resultado.putAll(mapaTerciarios)

        // Juntar-se à Batalha usa Raciocínio + Prontidão. Em uma construção
        // explicitamente física/combatente, Raciocínio é portanto um Atributo
        // estrutural de combate, mesmo pertencendo ao grupo Mental. Priorizá-lo
        // por troca dentro do próprio grupo preserva exatamente o orçamento
        // de Atributos e não interfere nas regras de rolagem.
        if (arquetipo == ArquetipoEncontro.FISICO) {
            val grupoMental = ExaltedConstants.MENTAL_ATTRIBUTES
            val raciocinio = "Raciocínio"
            val maiorMental = grupoMental.maxOf { resultado[it] ?: 1 }
            val atual = resultado[raciocinio] ?: 1
            if (atual < maiorMental) {
                val doador = grupoMental.firstOrNull {
                    it != raciocinio && (resultado[it] ?: 1) == maiorMental
                }
                if (doador != null) {
                    resultado[raciocinio] = maiorMental
                    resultado[doador] = atual
                }
            }
        }
        return resultado
    }

    /**
     * Ajusta Força/Destreza conforme a Habilidade de combate do NPC (Aba 11).
     *
     * Regras (pedido explícito):
     * - Arqueirismo ou Arremesso: Destreza = 5 e Força no máximo 2.
     * - Armas Brancas ou Briga: Destreza nunca abaixo de 4.
     *
     * Os pontos extras/deficitários são redistribuídos dentro da categoria
     * Física, respeitando teto 5 e piso 1. Assim a regra de combate não
     * altera os totais 11/9/7 das três categorias.
     */
    fun ajustarAtributosPorHabilidadeCombate(
        attributes: Map<String, Int>,
        habilidadeCombate: String,
        random: Random = Random.Default
    ): Map<String, Int> {
        val fisicos = ExaltedConstants.PHYSICAL_ATTRIBUTES
        val grupos = listOf(
            ExaltedConstants.PHYSICAL_ATTRIBUTES,
            ExaltedConstants.SOCIAL_ATTRIBUTES,
            ExaltedConstants.MENTAL_ATTRIBUTES
        )
        val antes = attributes.toMutableMap()
        fun valor(nome: String) = antes[nome] ?: 1
        val totaisAntes = grupos.associateWith { grupo -> grupo.sumOf { valor(it) } }
        val totalFisico = totaisAntes.getValue(ExaltedConstants.PHYSICAL_ATTRIBUTES)

        // Em vez de executar uma sequência de transferências que pode depender
        // da ordem dos doadores, procura diretamente um estado físico válido.
        // Isso torna as garantias de Destreza/Força matematicamente explícitas
        // e preserva os totais das três categorias.
        val candidatos = buildList {
            for (forca in 1..5) for (destreza in 1..5) for (vigor in 1..5) {
                if (forca + destreza + vigor != totalFisico) continue
                if (listOf(forca, destreza, vigor).none { it == 5 }) continue
                when (habilidadeCombate) {
                    "Arqueirismo", "Arremesso" -> {
                        if (destreza != 5 || forca > 2) continue
                    }
                    "Armas Brancas", "Briga" -> {
                        if (destreza < 4) continue
                    }
                    else -> error("Habilidade de combate inválida: $habilidadeCombate")
                }
                add(mapOf("Força" to forca, "Destreza" to destreza, "Vigor" to vigor))
            }
        }
        check(candidatos.isNotEmpty()) {
            "Não existe distribuição física válida para a Habilidade de combate $habilidadeCombate"
        }

        val distancia = { candidato: Map<String, Int> ->
            fisicos.sumOf { kotlin.math.abs((candidato[it] ?: 1) - valor(it)) }
        }
        val menorDistancia = candidatos.minOf { distancia(it) }
        val melhores = candidatos.filter { distancia(it) == menorDistancia }
        val escolhido = melhores.shuffled(random).first()

        val resultado = antes.toMutableMap()
        resultado.putAll(escolhido)

        check(grupos.all { grupo -> grupo.sumOf { resultado[it] ?: 1 } == totaisAntes.getValue(grupo) }) {
            "Ajuste de combate alterou o total de uma categoria de Atributos"
        }
        check(resultado.values.sum() == attributes.values.sum()) {
            "Ajuste de combate alterou a soma global de Atributos"
        }
        check(resultado.getValue("Destreza") >= if (habilidadeCombate in setOf("Armas Brancas", "Briga")) 4 else 5) {
            "Ajuste de combate não atingiu a Destreza mínima"
        }
        if (habilidadeCombate in setOf("Arqueirismo", "Arremesso")) {
            check(resultado.getValue("Força") <= 2) {
                "Ajuste de combate não respeitou Força máxima para combate à distância"
            }
        }
        return resultado
    }

    data class ResultadoHabilidades(val abilities: Map<String, Int>, val habilidadeSocialOuMental: String)

    // --- 6. Distribuição de habilidades: 28 pontos normais (máx. 3 cada),
    // Pontos de Bônus elevam habilidade de combate/Supernal/Esquiva
    // (quando obrigatória) pra 5, Resistência sempre ao menos 1. ---
    fun distribuirHabilidades(
        arquetipo: ArquetipoEncontro,
        habilidadeCombate: String,
        habilidadeDefensivaObrigatoria: String,
        supernal: String?,
        habilidadesFavorecidas: List<String>,
        random: Random,
        habilidadesEstruturaisRelevantes: List<String> = emptyList(),
        habilidadesMinimoUm: Set<String> = emptySet()
    ): ResultadoHabilidades {
        val pontos = mutableMapOf<String, Int>().apply {
            ExaltedConstants.ALL_25_ABILITIES.forEach { put(it, 0) }
        }
        var restante = 28

        fun gastar(habilidade: String, quantidade: Int) {
            val atual = pontos.getValue(habilidade)
            val aplicavel = minOf(quantidade, 3 - atual, restante)
            if (aplicavel > 0) {
                pontos[habilidade] = atual + aplicavel
                restante -= aplicavel
            }
        }

        val favorecidas = habilidadesFavorecidas
            .filter { it in ExaltedConstants.ALL_25_ABILITIES }
            .distinct()

        // Primeiro damos 2 pontos às Favorecidas. Depois concentramos o saldo
        // em Habilidades de foco até 3. O algoritmo não pressupõe que existam
        // exatamente cinco Favorecidas: isso também é usado pelo Lunar, que
        // não possui Favorecidas de Habilidade. Quando a aritmética dos 28
        // pontos torna inevitável um único ponto residual, ele é colocado só
        // depois de esgotar todas as combinações possíveis sem ratings 1.
        favorecidas.forEach { gastar(it, 2) }

        val habilidadeSocialOuMental = when (arquetipo) {
            ArquetipoEncontro.SOCIAL -> EncounterGenerationRules.SOCIAL_ABILITIES.let { opcoes ->
                supernal?.takeIf { it in opcoes } ?: opcoes.random(random)
            }
            ArquetipoEncontro.MENTAL -> EncounterGenerationRules.MENTAL_ABILITIES.let { opcoes ->
                supernal?.takeIf { it in opcoes } ?: opcoes.random(random)
            }
            ArquetipoEncontro.FISICO -> habilidadeCombate
        }

        val habilidadesLimitadas = if (arquetipo == ArquetipoEncontro.SOCIAL || arquetipo == ArquetipoEncontro.MENTAL) {
            buildSet {
                add(habilidadeCombate)
                if (habilidadeDefensivaObrigatoria == "Esquiva") add("Esquiva")
            }
        } else emptySet()

        // As prioridades obrigatórias entram primeiro, mas continuam sujeitas
        // ao mesmo teto de 3 e ao mesmo orçamento dos demais focos.
        val prioridadesObrigatorias = buildList {
            add(habilidadeCombate)
            // Raciocínio + Prontidão define Juntar-se à Batalha e, portanto,
            // a ordem de atuação. Para combatentes Prontidão é estrutural.
            if (arquetipo == ArquetipoEncontro.FISICO) add("Prontidão")
            add(habilidadeSocialOuMental)
            if (habilidadeDefensivaObrigatoria == "Esquiva") add("Esquiva")
            addAll(habilidadesEstruturaisRelevantes)
            add("Resistência")
            add("Linguística")
            add("Prontidão")
        }

        val ordemPolitica = EncounterArchetypePolicy.abilityPriority(
            archetype = arquetipo,
            combat = habilidadeCombate,
            support = habilidadeSocialOuMental,
            favored = habilidadesFavorecidas,
            supernal = supernal
        )

        // Primeiro escolhe as Habilidades não favorecidas que melhor
        // representam o arquétipo. O número de focos é calculado a partir do
        // saldo real, em vez de assumir 5 Favorecidas.
        val candidatos = (
            prioridadesObrigatorias +
                habilidadesEstruturaisRelevantes +
                ordemPolitica +
                ExaltedConstants.ALL_25_ABILITIES
            )
            .filter { it in ExaltedConstants.ALL_25_ABILITIES }
            .filter { it !in favorecidas }
            .filter { it !in habilidadesLimitadas || it == habilidadeCombate || it == habilidadeDefensivaObrigatoria }
            .distinct()
            .shuffled(random)
            .let { embaralhados ->
                val prioridade = ordemPolitica.withIndex().associate { it.value to it.index }
                embaralhados.sortedWith(
                    compareBy<String> {
                        when {
                            it in prioridadesObrigatorias -> 0
                            it in habilidadesEstruturaisRelevantes -> 1
                            else -> 2
                        }
                    }.thenBy { prioridade[it] ?: Int.MAX_VALUE }
                )
            }

        // Não esgotamos cegamente o orçamento em blocos de 3 quando isso
        // deixaria resto 1. Ex.: 28 = 8*3 + 2 + 2 produz uma distribuição
        // mais consistente que 9*3 + 1. O rating 1 fica reservado somente
        // para situações em que restrições externas realmente o tornem
        // inevitável.
        val reservarQuatro = restante % 3 == 1 && restante >= 4 && candidatos.count { pontos.getValue(it) == 0 } >= 2
        val quantidadeFocosDeTres = if (reservarQuatro) (restante - 4) / 3 else restante / 3
        candidatos.filter { pontos.getValue(it) == 0 }.take(quantidadeFocosDeTres).forEach { gastar(it, 3) }
        if (reservarQuatro) {
            candidatos.filter { pontos.getValue(it) == 0 }.take(2).forEach { gastar(it, 2) }
        }

        // Se sobrarem pontos, primeiro completamos Habilidades que já possuem
        // 2. A criação de um rating 1 é o último recurso, nunca a consequência
        // automática de uma divisão gulosa por três.
        while (restante > 0) {
            val alvoDois = candidatos.firstOrNull { pontos.getValue(it) == 2 }
            if (alvoDois != null) {
                gastar(alvoDois, 1)
                continue
            }
            val alvoFavorecida = favorecidas.firstOrNull { pontos.getValue(it) == 2 }
            if (alvoFavorecida != null) {
                gastar(alvoFavorecida, 1)
                continue
            }
            break
        }

        // Para perfis com 0, 1 ou 3 Favorecidas, um único ponto pode ser
        // matematicamente inevitável após o teto 3. Nesses casos ele fica no
        // final da lista de prioridades, nunca em uma Habilidade estrutural
        // que já tenha sido elevada para 3.
        if (restante > 0) {
            val alvoResidual = candidatos.firstOrNull { pontos.getValue(it) == 0 }
                ?: ExaltedConstants.ALL_25_ABILITIES.firstOrNull { pontos.getValue(it) == 0 }
            if (alvoResidual != null) gastar(alvoResidual, restante)
        }

        // Ratings 1 funcionais são permitidos quando representam acesso real
        // a uma função (ex.: Linguística para leitura). Eles não consomem um
        // ponto extra: transferimos um círculo de uma Habilidade 3 não
        // obrigatória, preservando exatamente o orçamento de 28 pontos.
        val minimosFuncionais = habilidadesMinimoUm
            .filter { it in ExaltedConstants.ALL_25_ABILITIES }
            .toSet()
        for (habilidade in minimosFuncionais) {
            if (pontos.getValue(habilidade) >= 1) continue
            val doador = pontos.entries
                .asSequence()
                .filter { (nome, valor) -> valor >= 3 && nome !in minimosFuncionais }
                .sortedByDescending { it.value }
                .map { it.key }
                .firstOrNull()
                ?: error("Não há Habilidade doadora para garantir $habilidade 1 sem alterar o orçamento")
            pontos[doador] = pontos.getValue(doador) - 1
            pontos[habilidade] = 1
        }

        check(restante == 0) {
            "Distribuição normal de Habilidades não consumiu os 28 pontos: $restante"
        }
        check(pontos.values.all { it in 0..3 }) {
            "Distribuição normal criou rating fora do intervalo 0..3"
        }

        return ResultadoHabilidades(pontos, habilidadeSocialOuMental)
    }

    // ------------------------------------------------------------------
    // Pontos de Bônus — implementação em EncounterBonusPointDistribution.kt
    // ------------------------------------------------------------------

    data class ResultadoPontosDeBonusLunar(
        val abilities: Map<String, Int>,
        val forcaDeVontade: Int,
        val attributes: Map<String, Int>,
        val precisaEspecialidadeAdicional: Boolean = false
    )

    fun distribuirPontosDeBonusLunar(
        arquetipo: ArquetipoEncontro,
        abilitiesBase: Map<String, Int>,
        attributesBase: Map<String, Int>,
        atributosCastaOuFavorecidos: List<String>,
        habilidadeCombate: String,
        habilidadeDefensiva: String,
        habilidadeSuporte: String = "",
        random: Random,
        prioridadeAtributos: List<String> = emptyList()
    ): ResultadoPontosDeBonusLunar {
        val r = EncounterBonusPointDistribution.distribuirPontosDeBonusLunar(
            arquetipo = arquetipo,
            abilitiesBase = abilitiesBase,
            attributesBase = attributesBase,
            atributosCastaOuFavorecidos = atributosCastaOuFavorecidos,
            habilidadeCombate = habilidadeCombate,
            habilidadeDefensiva = habilidadeDefensiva,
            habilidadeSuporte = habilidadeSuporte,
            random = random,
            prioridadeAtributos = prioridadeAtributos
        )
        return ResultadoPontosDeBonusLunar(
            abilities = r.abilities,
            forcaDeVontade = r.forcaDeVontade,
            attributes = r.attributes,
            precisaEspecialidadeAdicional = r.precisaEspecialidadeAdicional
        )
    }

    fun distribuirPontosDeBonus(
        arquetipo: ArquetipoEncontro,
        abilitiesBase: Map<String, Int>,
        habilidadesFavorecidasOuCasta: List<String>,
        forcaDeVontadeBase: Int,
        ordemPrioridade: List<String>,
        habilidadeCombate: String? = null,
        habilidadesEstruturaisRelevantes: List<String> = emptyList()
    ): Triple<Map<String, Int>, Int, Boolean> =
        EncounterBonusPointDistribution.distribuirPontosDeBonus(
            arquetipo = arquetipo,
            abilitiesBase = abilitiesBase,
            habilidadesFavorecidasOuCasta = habilidadesFavorecidasOuCasta,
            forcaDeVontadeBase = forcaDeVontadeBase,
            ordemPrioridade = ordemPrioridade,
            habilidadeCombate = habilidadeCombate,
            habilidadesEstruturaisRelevantes = habilidadesEstruturaisRelevantes
        )

    fun adicionarEspecialidadeAdicional(
        especialidades: List<EspecialidadeEncontro>,
        abilities: Map<String, Int>,
        ordemPrioridade: List<String>,
        necessaria: Boolean
    ): List<EspecialidadeEncontro> {
        if (!necessaria) return especialidades
        val existentes = especialidades.map { it.habilidade }.toSet()
        val prioridade = ordemPrioridade.withIndex().associate { it.value to it.index }
        val candidata = abilities.keys
            .filter { (abilities[it] ?: 0) in 2..5 && it !in existentes }
            .sortedWith(
                compareByDescending<String> { abilities[it] ?: 0 }
                    .thenBy { prioridade[it] ?: Int.MAX_VALUE }
                    .thenBy { it }
            )
            .firstOrNull()
            ?: return especialidades
        return especialidades + EspecialidadeEncontro(candidata)
    }

    // --- 8. Especialidades: 4 no total — ataque e defesa são obrigatórias,
    // as outras 2 seguem a prioridade do perfil. ---
    fun distribuirEspecialidades(
        arquetipo: ArquetipoEncontro,
        habilidadeCombate: String,
        habilidadeDefensivaObrigatoria: String,
        habilidadeSocialOuMental: String,
        abilities: Map<String, Int>,
        random: Random,
        habilidadesEstruturaisRelevantes: List<String> = emptyList()
    ): List<EspecialidadeEncontro> {
        val escolhidas = mutableListOf<String>()
        // Materializa uma vez: a mesma coleção de habilidades com pontos era
        // filtrada repetidamente em cada ramo e no fallback.
        val habilidadesComPontos = abilities.filterValues { it >= 2 }.keys.toList()
        val habAtaque = if (arquetipo == ArquetipoEncontro.FISICO) habilidadeCombate else habilidadeSocialOuMental
        val habDefesa = if (arquetipo == ArquetipoEncontro.FISICO) habilidadeDefensivaObrigatoria else habilidadeSocialOuMental
        if ((abilities[habAtaque] ?: 0) >= 2) escolhidas += habAtaque
        if ((abilities[habDefesa] ?: 0) >= 2 && habDefesa !in escolhidas) escolhidas += habDefesa

        // Pedido explícito do usuário: seguir a prioridade documentada,
        // não só incluir como candidata entre várias — Social prioriza
        // Socialização ou Integridade como 2ª especialidade (protege
        // Astúcia/Perseverança), Mental prioriza Investigação ou
        // Ocultismo como 2ª (escrutínio/pesquisa).
        val segundaPrioridade = when (arquetipo) {
            ArquetipoEncontro.SOCIAL -> listOf("Socialização", "Integridade").firstOrNull { (abilities[it] ?: 0) >= 2 && it !in escolhidas }
            ArquetipoEncontro.MENTAL -> listOf("Investigação", "Ocultismo").firstOrNull { (abilities[it] ?: 0) >= 2 && it !in escolhidas }
            ArquetipoEncontro.FISICO -> null
        }
        if (segundaPrioridade != null && escolhidas.size < 4) escolhidas += segundaPrioridade

        // Depois das especialidades obrigatórias e da prioridade específica
        // do arquétipo, favorece Habilidades que participam diretamente de
        // caminhos de Encantos. A especialidade não altera o custo de XP, mas
        // concentra o perfil da ficha nas Habilidades que já foram elevadas
        // para abrir esses caminhos. Mantemos a ordem fornecida pelo gerador
        // para que a decisão continue determinística para a mesma semente.
        val estruturais = habilidadesEstruturaisRelevantes
            .filter { it in habilidadesComPontos && it !in escolhidas }
            .distinct()
        for (hab in estruturais) {
            if (escolhidas.size >= 4) break
            escolhidas += hab
        }

        val candidatasPerfil = when (arquetipo) {
            ArquetipoEncontro.FISICO -> habilidadesComPontos.shuffled(random)
            ArquetipoEncontro.SOCIAL -> (listOf("Presença", "Performance", "Socialização", "Integridade") + habilidadesComPontos).distinct().shuffled(random)
            ArquetipoEncontro.MENTAL -> (listOf("Ocultismo", "Conhecimento", "Investigação", "Medicina") + habilidadesComPontos).distinct().shuffled(random)
        }
        for (hab in candidatasPerfil) {
            if (escolhidas.size >= 4) break
            if (hab !in escolhidas) escolhidas += hab
        }
        // Se ainda faltar (raro), completa com qualquer habilidade com pontos.
        if (escolhidas.size < 4) {
            for (hab in habilidadesComPontos) {
                if (escolhidas.size >= 4) break
                if (hab !in escolhidas) escolhidas += hab
            }
        }
        return escolhidas.take(4).map { EspecialidadeEncontro(it) }
    }

}
