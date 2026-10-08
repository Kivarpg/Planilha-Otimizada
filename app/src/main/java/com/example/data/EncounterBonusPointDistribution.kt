package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.BpCostTable
import com.example.model.ExaltedConstants
import kotlin.random.Random

/**
 * Distribuição dos 15 Pontos de Bônus de criação (Solar/DB e Lunar).
 *
 * Extraído de EncounterDistributionService (refatoração de organização —
 * roteiro de refatoração agressiva, sem mudança de comportamento).
 */
internal object EncounterBonusPointDistribution {
    /**
     * 16: distribui os 15 Pontos de Bônus de criação do NPC — prioridade
     * total em elevar Habilidades (respeitando o teto de 5), usando a
     * mesma tabela de custo já usada na planilha de personagem (confirmada
     * pelo usuário): 1 PB por ponto se a habilidade for de Casta/Aspecto
     * ou Favorecida, 2 PB se não for. O que sobrar (se sobrar — abilidades
     * têm prioridade de verdade, então pode não sobrar nada) compra
     * Força de Vontade a 2 PB o ponto, até um teto que depende do
     * arquétipo.
     *
     * NOTA/SUPOSIÇÃO: o usuário confirmou explicitamente "até 2 pontos"
     * de Força de Vontade pra arquétipos Social/Mental, mas não deu um
     * número pra Físico. Usei 1 como teto do Físico aqui — é uma
     * suposição documentada, não uma confirmação; fácil de ajustar se
     * estiver errado.
     */
    data class ResultadoPontosDeBonusLunar(
        val abilities: Map<String, Int>,
        val forcaDeVontade: Int,
        val attributes: Map<String, Int>,
        val precisaEspecialidadeAdicional: Boolean = false
    )

    /**
     * Distribuição específica dos 15 Pontos de Bônus dos Lunares.
     *
     * Ordem obrigatória:
     * 1) compra 1 ou 2 pontos de Força de Vontade;
     * 2) escolhe uma das duas metas de Habilidades: 2 habilidades 3->5
     *    ou 4 habilidades 3->4;
     * 3) usa o máximo possível do saldo em Atributos de Casta/Favorecidos;
     * 4) se restar saldo insuficiente para outro Atributo, prioriza
     *    Habilidade 4->5 e depois Habilidade 3->4.
     *
     * Toda escolha de Habilidade é restrita ao perfil do arquétipo.
     */
    // Extraído do corpo de distribuirPontosDeBonusLunar (refatoração de
    // organização — pedido explícito do usuário, feito com cautela extra).
    // Fase de compra de Atributos de Casta/Favorecidos: distribui o saldo
    // em round-robin entre os candidatos elegíveis (abaixo do teto 5),
    // priorizando os de maior valor atual primeiro na ordem, mas girando
    // entre eles a cada compra em vez de esgotar um de cada vez.
    // attributes é mutado por referência (MutableMap); retorna o saldo
    // restante (Int, tipo valor, não pode vir por referência).
    private fun comprarAtributosCastaOuFavorecidos(
        atributosCastaOuFavorecidos: List<String>,
        attributes: MutableMap<String, Int>,
        saldoInicial: Int,
        custoAtributo: Int,
        prioridadeAtributos: List<String> = emptyList()
    ): Int {
        if (atributosCastaOuFavorecidos.isEmpty()) return saldoInicial
        var saldo = saldoInicial
        val candidatosLegais = atributosCastaOuFavorecidos.distinct()
        val prioridade = prioridadeAtributos
            .asSequence()
            .filter { it in candidatosLegais }
            .distinct()
            .toList()
        val ordemPreferida = (prioridade + candidatosLegais).distinct()
        val indicePrioridade = ordemPreferida.withIndex().associate { it.value to it.index }
        val atributosOrdenados = if (prioridade.isEmpty()) {
            // Compatibilidade estrita: sem planejador, preserva exatamente a
            // ordenação histórica (rating atual; empates mantêm ordem original).
            candidatosLegais.sortedByDescending { attributes[it] ?: 1 }
        } else {
            // Quando a prioridade estratégica for explicitamente ativada, ela
            // passa a decidir entre os mesmos candidatos legais. Rating atual
            // continua como desempate; custos, teto e round-robin não mudam.
            candidatosLegais.sortedWith(
                compareBy<String> { indicePrioridade[it] ?: Int.MAX_VALUE }
                    .thenByDescending { attributes[it] ?: 1 }
            )
        }
        var indice = 0
        while (saldo >= custoAtributo) {
            var candidatos = 0
            for (nome in atributosOrdenados) if ((attributes[nome] ?: 1) < 5) candidatos++
            if (candidatos == 0) break
            var alvo: String? = null
            var cursor = indice % candidatos
            for (nome in atributosOrdenados) {
                if ((attributes[nome] ?: 1) < 5) {
                    if (cursor == 0) { alvo = nome; break }
                    cursor--
                }
            }
            val nomeAlvo = alvo ?: break
            attributes[nomeAlvo] = (attributes[nomeAlvo] ?: 1) + 1
            saldo -= custoAtributo
            indice++
        }
        return saldo
    }

    data class LunarAttributePurchasePreview(
        val attributes: Map<String, Int>,
        val saldoRestante: Int
    )

    /**
     * Preview determinístico da fase 3 dos PB Lunares. Não usa Random e não
     * modifica o mapa recebido; serve para A/B histórico x estratégico sobre
     * exatamente o mesmo estado intermediário.
     */
    fun previewComprasAtributosLunares(
        attributesBase: Map<String, Int>,
        atributosCastaOuFavorecidos: List<String>,
        saldoInicial: Int,
        prioridadeAtributos: List<String> = emptyList()
    ): LunarAttributePurchasePreview {
        val copia = attributesBase.toMutableMap()
        val saldo = comprarAtributosCastaOuFavorecidos(
            atributosCastaOuFavorecidos = atributosCastaOuFavorecidos,
            attributes = copia,
            saldoInicial = saldoInicial,
            custoAtributo = BpCostTable.Lunar.ATRIBUTO_CASTA_OU_FAVORECIDO,
            prioridadeAtributos = prioridadeAtributos
        )
        return LunarAttributePurchasePreview(copia.toMap(), saldo)
    }

    fun distribuirPontosDeBonusLunar(
        arquetipo: ArquetipoEncontro,
        abilitiesBase: Map<String, Int>,
        attributesBase: Map<String, Int>,
        atributosCastaOuFavorecidos: List<String>,
        habilidadeCombate: String,
        habilidadeDefensiva: String,
        habilidadeSuporte: String,
        random: Random,
        prioridadeAtributos: List<String> = emptyList()
    ): ResultadoPontosDeBonusLunar {
        val custoHabilidade = BpCostTable.Lunar.HABILIDADE
        val custoAtributo = BpCostTable.Lunar.ATRIBUTO_CASTA_OU_FAVORECIDO
        val custoForcaDeVontade = BpCostTable.Lunar.FORCA_DE_VONTADE
        val totalPb = 15

        var saldo = totalPb
        var forcaDeVontade = 5
        val abilities = abilitiesBase.toMutableMap()
        val attributes = attributesBase.toMutableMap()

        // Força de Vontade: sempre 1 ou 2 pontos. A escolha é feita uma vez
        // para evitar que o restante do algoritmo altere essa decisão.
        val pontosVontade = if (random.nextBoolean()) 1 else 2
        repeat(pontosVontade) {
            if (saldo >= custoForcaDeVontade) {
                forcaDeVontade++
                saldo -= custoForcaDeVontade
            }
        }

        // Conjunto de Habilidades coerente com o arquétipo. A habilidade
        // principal, defensiva e de suporte sempre têm prioridade máxima.
        // O arquétipo não compra uma segunda Habilidade de combate com PB.
        // A habilidade de ataque escolhida é a única Habilidade de combate
        // elegível para receber compras automáticas; Esquiva/qualquer outra
        // habilidade de combate permanece apenas no conjunto-base.
        val habilidadesDeCombateSecundarias = EncounterGenerationRules.COMBAT_ABILITIES
            .filter { it != habilidadeCombate }
            .toSet()
        val cabecaPerfil = when (arquetipo) {
            ArquetipoEncontro.FISICO -> listOf(habilidadeCombate, habilidadeDefensiva, habilidadeSuporte)
            ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL -> listOf(habilidadeSuporte, habilidadeDefensiva, habilidadeCombate)
        }
        val habilidadesLimitadas = if (arquetipo == ArquetipoEncontro.SOCIAL || arquetipo == ArquetipoEncontro.MENTAL) {
            buildSet {
                add(habilidadeCombate)
                if (habilidadeDefensiva == "Esquiva") add("Esquiva")
            }
        } else emptySet()
        val perfil = (cabecaPerfil +
            EncounterGenerationRules.LUNAR_ABILITY_PROFILES.getValue(arquetipo))
            .filter { it in ExaltedConstants.ALL_25_ABILITIES }
            .filter { it !in habilidadesDeCombateSecundarias }
            .filter { it !in habilidadesLimitadas }
            .distinct()

        // Complementa SOMENTE com Habilidades do perfil do arquétipo.
        // Não é permitido escapar para a lista global das 25 Habilidades:
        // isso violava a regra "sempre respeitar o arquétipo" e fazia o PB
        // lunar investir em Habilidades aleatórias fora do conceito.
        // A aleatoriedade ocorre apenas dentro do perfil, preservando as
        // prioridades funcionais nas posições iniciais.
        val prioridadePerfil = perfil.withIndex().associate { it.value to it.index }
        // O índice de prioridade é único por Habilidade; portanto não existe
        // empate real para o shuffle resolver. Ordenar diretamente evita uma
        // alocação e, principalmente, não consome aleatoriedade sem efeito.
        val candidatas3 = perfil
            .filter { abilities[it] == 3 }
            .sortedBy { prioridadePerfil[it] ?: Int.MAX_VALUE }

        // Uma das duas formas pedidas pelo usuário, ambas custando 8 PB:
        // 2 habilidades 3->5 = 2 * 2 * 2 PB; ou 4 habilidades 3->4 =
        // 4 * 2 PB.
        val usarDuasAteCinco = random.nextBoolean() && candidatas3.size >= 2
        val quantidadeAlvo = if (usarDuasAteCinco) 2 else 4

        if (candidatas3.size >= quantidadeAlvo) {
            val escolhidas = candidatas3.take(quantidadeAlvo)
            if (usarDuasAteCinco) {
                escolhidas.forEach {
                    if (saldo >= custoHabilidade * 2) {
                        abilities[it] = 5
                        saldo -= custoHabilidade * 2
                    }
                }
            } else {
                escolhidas.forEach {
                    if (saldo >= custoHabilidade) {
                        abilities[it] = 4
                        saldo -= custoHabilidade
                    }
                }
            }
        } else {
            // Catálogo excepcionalmente pobre: usa as Habilidades 3 do
            // arquétipo que existirem, sem elevar Habilidades fora do perfil.
            candidatas3.take(quantidadeAlvo).forEach {
                if (saldo >= custoHabilidade) {
                    abilities[it] = 4
                    saldo -= custoHabilidade
                }
            }
        }

        // Todo o saldo que conseguir comprar um Atributo vai primeiro para
        // Casta/Favorecido. O teto 5 é sempre respeitado.
        saldo = comprarAtributosCastaOuFavorecidos(
            atributosCastaOuFavorecidos, attributes, saldo, custoAtributo, prioridadeAtributos
        )

        // Saldo residual: primeiro 4->5, depois 3->4. Se nenhuma dessas
        // faixas existir no perfil, ainda é permitido gastar o saldo em uma
        // Habilidade do próprio perfil abaixo de 3 (1->2 ou 2->3).
        // Isso é necessário para que a distribuição seja total e determinística:
        // um perfil pode, dependendo da semente aleatória, não possuir
        // nenhuma Habilidade em 3/4, mas o PB restante continua válido.
        while (saldo >= custoHabilidade) {
            val alvo = perfil.firstOrNull { (abilities[it] ?: 0) == 4 }
                ?: perfil.firstOrNull { (abilities[it] ?: 0) == 3 }
                ?: perfil.firstOrNull { (abilities[it] ?: 0) in 1..2 }
                ?: break
            abilities[alvo] = (abilities[alvo] ?: 0) + 1
            saldo -= custoHabilidade
        }

        val precisaEspecialidadeAdicional = saldo == 1
        if (precisaEspecialidadeAdicional) saldo = 0

        check(saldo == 0) { "Distribuição Lunar de PB incompleta: $saldo PB restantes" }

        return ResultadoPontosDeBonusLunar(
            abilities = abilities,
            forcaDeVontade = forcaDeVontade,
            attributes = attributes,
            precisaEspecialidadeAdicional = precisaEspecialidadeAdicional
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
    ): Triple<Map<String, Int>, Int, Boolean> {
        val CUSTO_HABILIDADE_FAVORECIDA = 1
        val CUSTO_HABILIDADE_NAO_FAVORECIDA = 2
        val CUSTO_FORCA_DE_VONTADE = 2
        val tetoForcaDeVontadePorArquetipo = if (arquetipo == ArquetipoEncontro.FISICO) 1 else 2

        var pontosRestantes = 15
        val abilities = abilitiesBase.toMutableMap()
        // Nunca comprar automaticamente uma nova Habilidade de combate.
        // Quando a habilidade de ataque é informada, somente ela pode
        // receber PB; as demais Habilidades de combate ficam bloqueadas.
        val habilidadesDeCombateBloqueadas = habilidadeCombate?.let { principal ->
            EncounterGenerationRules.COMBAT_ABILITIES.filter { it != principal }.toSet()
        } ?: emptySet()
        val habilidadesLimitadas = if (arquetipo == ArquetipoEncontro.SOCIAL || arquetipo == ArquetipoEncontro.MENTAL) {
            buildSet {
                add(habilidadeCombate ?: "")
                if (habilidadeCombate != null && habilidadeCombate in setOf("Arqueirismo", "Arremesso")) add("Esquiva")
            }
        } else emptySet()
        // Habilidades estruturais relevantes (por exemplo, Ocultismo quando
        // existe rota de Feitiçaria) recebem uma segunda camada de prioridade
        // também nos Pontos de Bônus. Isso permite repetir a compra acima do
        // ponto inicial e reduz o custo de XP futuro sem alterar o orçamento.
        val ordemCompleta = (ordemPrioridade + habilidadesEstruturaisRelevantes + ExaltedConstants.ALL_25_ABILITIES)
            .distinct()
            .filter { it !in habilidadesDeCombateBloqueadas }
            .filter { it !in habilidadesLimitadas || (abilitiesBase[it] ?: 0) < 3 }
        val habilidadesFavorecidasSet = habilidadesFavorecidasOuCasta.toSet()

        // Primeiro esgota Habilidades que já possuem 2+ pontos. Isso é
        // especialmente importante para Solar/Sangue de Dragão: com cinco
        // Favorecidas começando em 2, os 15 PB podem ser gastos integralmente
        // elevando essas cinco Habilidades até 5, sem criar ratings 1 em
        // Casta/Aspecto ou em Habilidades não favorecidas. Só depois, se ainda
        // houver saldo, abrimos compras em Habilidades abaixo de 2.
        fun comprarEmFases(minimoAtual: Int) {
            var houveCompra = true
            while (pontosRestantes > 0 && houveCompra) {
                houveCompra = false
                for (hab in ordemCompleta) {
                    val atual = abilities[hab] ?: 0
                    if (atual < minimoAtual) continue
                    if (hab in habilidadesLimitadas && atual >= 3) continue
                    if (atual >= 5) continue
                    val custo = if (hab in habilidadesFavorecidasSet) CUSTO_HABILIDADE_FAVORECIDA else CUSTO_HABILIDADE_NAO_FAVORECIDA
                    if (custo <= pontosRestantes) {
                        abilities[hab] = atual + 1
                        pontosRestantes -= custo
                        houveCompra = true
                    }
                    if (pontosRestantes <= 0) break
                }
            }
        }

        comprarEmFases(minimoAtual = 2)
        if (pontosRestantes > 0) comprarEmFases(minimoAtual = 0)

        var forcaDeVontade = forcaDeVontadeBase
        while (pontosRestantes >= CUSTO_FORCA_DE_VONTADE && forcaDeVontade < forcaDeVontadeBase + tetoForcaDeVontadePorArquetipo) {
            forcaDeVontade++
            pontosRestantes -= CUSTO_FORCA_DE_VONTADE
        }

        // O 1 PB residual é identificado SOMENTE depois de esgotar as
        // compras de Força de Vontade. Assim, saldos como 3 ou 5 PB podem
        // primeiro gastar 2 PB em Força de Vontade e converter o 1 PB final
        // em Especialidade adicional.
        val precisaEspecialidadeAdicional = pontosRestantes == 1
        if (precisaEspecialidadeAdicional) pontosRestantes = 0

        check(pontosRestantes == 0) {
            "Distribuição de PB incompleta: $pontosRestantes PB restantes"
        }
        return Triple(abilities, forcaDeVontade, precisaEspecialidadeAdicional)
    }

    /**
     * Adiciona a Especialidade comprada com o 1 PB residual, quando houver.
     * A Habilidade é escolhida por valor decrescente (5 -> 1) e depois pela
     * ordem de prioridade fornecida. Habilidades que já possuem Especialidade
     * são ignoradas.
     */
}
