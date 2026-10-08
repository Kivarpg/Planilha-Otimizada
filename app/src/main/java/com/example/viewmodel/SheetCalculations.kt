package com.example.viewmodel

import com.example.model.isDragonBlooded
import com.example.model.isLunar
import com.example.data.EncantosSolaresCatalog
import com.example.model.CaixaVitalidade
import com.example.model.CharacterSheet
import com.example.model.Encanto
import com.example.model.ExaltedConstants
import com.example.model.NOME_CORPO_DE_TOURO

// Funções de cálculo genuinamente puras — recebem uma CharacterSheet como
// parâmetro e retornam um resultado, sem ler nem escrever nenhum estado do
// ViewModel (nenhuma referência a _sheetState, _commitmentError, etc.).
// Extraídas do SheetViewModel original como parte da reorganização por
// domínio: são a peça mais segura de extrair primeiro, já que praticamente
// já eram funções puras antes mesmo de sair do arquivo original — só
// viviam ali por conveniência de onde foram escritas, não por
// necessidade de acesso a estado mutável.
//
// BpBreakdown e as demais data classes usadas aqui continuam declaradas
// em SheetViewModel.kt (mesmo pacote com.example.viewmodel — visível sem
// import extra).
object SheetCalculations {

    fun calculateAbilityPointsRemaining(sheet: CharacterSheet): Int {
        val totalDots13 = ExaltedConstants.ALL_25_ABILITIES.sumOf { abName ->
            (sheet.abilities[abName] ?: 0).coerceAtMost(3)
        }
        return (28 - totalDots13).coerceAtLeast(0)
    }

    // --- Tabela 1.6: Custos em Experiência ---
    // Custo do nível "nivel" -> "nivel + 1" de uma Habilidade ou Arte Marcial.
    // O primeiro ponto (nivel 0 -> 1) custa sempre 3, independente de Casta/Favorecida.
    fun custoAumentoHabilidade(nivel: Int, favorecida: Boolean): Int =
        if (nivel == 0) 3 else if (favorecida) (nivel * 2 - 1) else (nivel * 2)

    // Custo de um Encanto/Feitiço/Necromancia recém-adquirido.
    fun custoExperienciaEncanto(charm: Encanto, sheet: CharacterSheet): Int {
        return when (charm.categoria) {
            "Feitiçaria", "Necromancia" -> {
                if (sheet.tipoPersonagem.isDragonBlooded()) {
                    if (sheet.ocultismoDoAspecto()) 10 else 12
                } else if (sheet.ocultismoCastaOuFavorecida()) 8 else 10
            }
            else -> {
                val ehArteMarcial = sheet.martialArts.any { it.nome == charm.habilidadeVinculada }
                val habilidadeAlvo = if (ehArteMarcial) "Briga" else charm.habilidadeVinculada
                if (sheet.tipoPersonagem.isLunar()) {
                    val atributosCasteFavorecidos = (sheet.lunarCasteAttributesEscolhidos + sheet.favoredAttributes).toSet()
                    val favorecida = habilidadeAlvo in atributosCasteFavorecidos
                    return if (favorecida) com.example.model.BpCostTable.Lunar.ENCANTO_CASTA_OU_FAVORECIDO
                    else if (sheet.lunarCasta == com.example.model.LunarCasta.Casteless) com.example.model.BpCostTable.Lunar.ENCANTO_NAO_FAVORECIDO_SEM_CASTA
                    else com.example.model.BpCostTable.Lunar.ENCANTO_NAO_FAVORECIDO
                }
                val favorecida = if (sheet.tipoPersonagem.isDragonBlooded()) {
                    val aspectoEnum = com.example.model.Aspecto.entries.firstOrNull { it.displayName == sheet.aspecto }
                    (aspectoEnum != null && habilidadeAlvo in aspectoEnum.allowedAbilities()) || habilidadeAlvo in sheet.favoredAbilities
                } else {
                    sheet.casteAbilities.any { EncantosSolaresCatalog.sameName(it, habilidadeAlvo) } ||
                        sheet.favoredAbilities.any { EncantosSolaresCatalog.sameName(it, habilidadeAlvo) }
                }
                if (favorecida) 8 else 10
            }
        }
    }

    // --- Indicador de Status & "Planilha Concluída" ---
    // Lista de pendências que impedem o indicador de ficar verde e o botão
    // "Planilha concluída" de ser habilitado. Mais rigorosa que a validação de
    // salvamento (validateAndSaveSheet), pois exige o saldo de Pontos de Bônus
    // exatamente zerado e os campos obrigatórios preenchidos.
    // LOGICA: lista de pendências para permitir marcar "Planilha Concluída".
    // Cada item corresponde a um campo obrigatório específico, exibido por
    // nome na mensagem de aviso — não simplificar/reverter sem confirmar antes.
    // Retorna os requisitos pendentes agrupados por aba (chave = rótulo da
    // aba, ex.: "Aba 2 — Casta"; valor = lista de requisitos daquela aba,
    // sem repetir o rótulo em cada item). LinkedHashMap preserva a ordem de
    // inserção (mesma ordem das abas no app). "Pontos de Bônus" não é uma
    // aba específica — fica com sua própria chave, sem prefixo "Aba N".
    fun computeCompletionIssues(sheet: CharacterSheet): LinkedHashMap<String, MutableList<String>> {
        val issues = LinkedHashMap<String, MutableList<String>>()
        fun adicionar(aba: String, requisito: String) {
            issues.getOrPut(aba) { mutableListOf() }.add(requisito)
        }

        // 1. Aba 1 — Dados Pessoais: Seleção da Casta.
        // `casta` sempre tem um valor válido no modelo (nunca nulo/vazio), então
        // esta condição não bloqueia na prática — mantida para documentar o
        // requisito e cobrir uma futura mudança que permita Casta indefinida.

        // 2. Aba 2 — Casta. Lunares têm 2 Atributos de Casta + 2
        // Atributos Favorecidos adicionais; não usam Habilidades de Casta,
        // Favorecidas ou Supernal.
        if (sheet.tipoPersonagem.isLunar()) {
            if (sheet.lunarCasteAttributesEscolhidos.size != 2) {
                adicionar("Aba 2 — Casta", "Seleção dos Atributos de Casta (${sheet.lunarCasteAttributesEscolhidos.size}/2).")
            }
            if (sheet.favoredAttributes.size != 2) {
                adicionar("Aba 2 — Casta", "Seleção dos Atributos Favorecidos (${sheet.favoredAttributes.size}/2).")
            }
            val duplicados = sheet.lunarCasteAttributesEscolhidos.toSet().intersect(sheet.favoredAttributes.toSet())
            if (duplicados.isNotEmpty()) {
                adicionar("Aba 2 — Casta", "Atributos de Casta e Favorecidos não podem se repetir (${duplicados.joinToString(", ")}).")
            }
        } else {
            if (sheet.casteAbilities.size != 5) adicionar("Aba 2 — Casta", "Seleção das Habilidades de Casta (${sheet.casteAbilities.size}/5).")
            if (sheet.supernalAbility.isNullOrEmpty()) adicionar("Aba 2 — Casta", "Seleção da Habilidade Supernal.")
            if (sheet.favoredAbilities.size != 5) adicionar("Aba 2 — Casta", "Seleção das Habilidades Favorecidas (${sheet.favoredAbilities.size}/5).")
        }

        // 3. Aba 3 — Atributos: uso integral dos pontos das três categorias.
        val gruposAtributos = mapOf(
            "Físicos" to ExaltedConstants.PHYSICAL_ATTRIBUTES,
            "Sociais" to ExaltedConstants.SOCIAL_ATTRIBUTES,
            "Mentais" to ExaltedConstants.MENTAL_ATTRIBUTES
        )
        gruposAtributos.forEach { (nomeGrupo, attrList) ->
            val prio = sheet.attributePriorities[nomeGrupo] ?: "1º"
            val pontosDoGrupo = when (prio) { "1º" -> 8; "2º" -> 6; else -> 4 }
            val pontosUsados = attrList.sumOf { (sheet.attributes[it] ?: 1) - 1 }
            if (pontosUsados < pontosDoGrupo) {
                adicionar("Aba 3 — Atributos", "Distribuir todos os pontos de $nomeGrupo ($pontosUsados/$pontosDoGrupo).")
            }
        }

        // 4. Aba 4 — Habilidades: distribuição integral dos 28 pontos base.
        val pontosHabilidadesUsados = ExaltedConstants.ALL_25_ABILITIES.sumOf { (sheet.abilities[it] ?: 0).coerceAtMost(3) }
        if (pontosHabilidadesUsados < 28) {
            adicionar("Aba 4 — Habilidades", "Distribuir todos os 28 pontos ($pontosHabilidadesUsados/28).")
        }

        // 5. Aba 6 — Méritos: distribuição integral dos pontos básicos
        // (13 pra Sangue de Dragão, 10 pra Solar — achado junto da
        // correção do custo em BP acima: essa checagem também hardcodeava
        // o valor do Solar incondicionalmente).
        val pontosMeritosBasicos = if (sheet.tipoPersonagem.isDragonBlooded()) 13 else 10
        val pontosMeritosUsados = sheet.merits.sumOf { it.valor }
        if (pontosMeritosUsados < pontosMeritosBasicos) {
            adicionar("Aba 6 — Méritos", "Distribuir todos os $pontosMeritosBasicos pontos ($pontosMeritosUsados/$pontosMeritosBasicos).")
        }

        // 6. Aba 7 — Equipamento: segunda camada de validação de Recursos
        // pra armas/armaduras Mundanas — pedido explícito do usuário. A
        // primeira camada (WeaponSection.kt/ArmorSection.kt) já bloqueia
        // no momento da escolha do catálogo; esta cobre qualquer outro
        // caminho que tenha colocado o item na planilha (edição manual do
        // nome, por exemplo) sem passar por aquela checagem. Remove o
        // sufixo " (Peso)" que o catálogo adiciona ao nome antes de
        // comparar. Presente: fica marcado como tal no nome, então nunca
        // aparece aqui — a validação não se aplica a itens de presente.
        val nivelRecursosAtual = sheet.nivelRecursos()
        sheet.weapons.filter { it.tipoArma == "Mundana" && !it.nome.endsWith("[Presente]") }.forEach { arma ->
            val nomeBase = arma.nome.substringBeforeLast(" (").trim()
            val candidata = com.example.data.WeaponCatalogMundano
                .candidatasPorHabilidade(arma.habilidadeVinculada)
                .firstOrNull { it.nome == nomeBase }
            if (candidata != null && candidata.custoRecursos > nivelRecursosAtual) {
                adicionar("Aba 7 — Equipamento", "${arma.nome} exige Recursos ${candidata.custoRecursos}. O personagem possui Recursos $nivelRecursosAtual.")
            }
        }
        sheet.armaduras.filter { it.tipoArmadura == "Mundana" && !it.nome.endsWith("[Presente]") }.forEach { armadura ->
            val nomeBase = armadura.nome.substringBeforeLast(" (").trim()
            val candidata = com.example.data.ArmorCatalogMundano.candidatas(armadura.categoriaPeso)
                .firstOrNull { it.nome == nomeBase }
            if (candidata != null && candidata.custoRecursos > nivelRecursosAtual) {
                adicionar("Aba 7 — Equipamento", "${armadura.nome} exige Recursos ${candidata.custoRecursos}. O personagem possui Recursos $nivelRecursosAtual.")
            }
        }

        // 7. Aba 8 — Encantos: seleção dos 15 Encantos.
        if (sheet.charms.size < 15) {
            adicionar("Aba 8 — Encantos", "Selecionar os 15 Encantos (${sheet.charms.size}/15).")
        }

        // 7. Pontos de Bônus: distribuição integral dos 15 pontos.
        val bpInfo = calculateBpBreakdown(sheet)
        if (bpInfo.remainingBalance != 0) {
            adicionar(
                "Pontos de Bônus",
                if (bpInfo.remainingBalance > 0)
                    "Distribuir todos os 15 pontos (restam ${bpInfo.remainingBalance})."
                else
                    "Saldo insuficiente (excedido em ${-bpInfo.remainingBalance})."
            )
        }

        return issues
    }

    // Conta quantos Encantos/Feitiços adquiridos estão vinculados a uma
    // Habilidade de Casta ou Favorecida (itens que não são "Encanto" — como
    // Feitiços registrados sob outra categoria — sempre contam). Usada por
    // validateAndSaveSheet() (validação do botão "Salvar", independente dos
    // requisitos de "Planilha Concluída" em computeCompletionIssues()).
    fun contarEncantosDeCastaOuFavorecidos(sheet: CharacterSheet): Int {
        val atributosCasteFavorecidos = if (sheet.tipoPersonagem.isLunar()) {
            (sheet.lunarCasteAttributesEscolhidos + sheet.favoredAttributes).toHashSet()
        } else emptySet()
        val habilidadesFavorecidasNormalizadas =
            (sheet.casteAbilities + sheet.favoredAbilities)
                .asSequence()
                .map(EncantosSolaresCatalog::normalize)
                .toHashSet()
        val artesMarciais = sheet.martialArts.asSequence().map { it.nome }.toHashSet()

        return sheet.charms.count { charm ->
            if (charm.categoria != "Encanto") return@count true
            if (sheet.tipoPersonagem.isLunar()) {
                return@count charm.habilidadeVinculada in atributosCasteFavorecidos
            }
            val habilidadeParaChecar = if (charm.habilidadeVinculada in artesMarciais) "Briga" else charm.habilidadeVinculada
            EncantosSolaresCatalog.normalize(habilidadeParaChecar) in habilidadesFavorecidasNormalizadas
        }
    }

    fun isSheetComplete(sheet: CharacterSheet): Boolean = computeCompletionIssues(sheet).isEmpty()

    // --- Cálculo de Essência a partir de Experiência gasta ---
    fun essenceFromXp(xpGasto: Int): Int = when {
        xpGasto >= 300 -> 5
        xpGasto >= 200 -> 4
        xpGasto >= 125 -> 3
        xpGasto >= 50 -> 2
        else -> 1
    }

    // Tabela própria de Sangue de Dragão (pedido explícito do usuário) —
    // começa em 2 (não 1) e usa limiares diferentes da tabela Solar
    // acima. Achado real: normalizeEssence() aplicava a tabela Solar
    // incondicionalmente pros dois templates antes desta correção.
    fun essenceFromXpSangueDeDragao(xpGasto: Int): Int = when {
        xpGasto >= 325 -> 5
        xpGasto >= 250 -> 4
        xpGasto >= 75 -> 3
        else -> 2
    }

    fun normalizeEssence(sheet: CharacterSheet): CharacterSheet =
        sheet.copy(
            essencia = if (sheet.tipoPersonagem.isDragonBlooded())
                essenceFromXpSangueDeDragao(sheet.experienciaGastaTotal)
            else
                essenceFromXp(sheet.experienciaGastaTotal)
        )

    // Retorna o retrato da planilha no momento em que "Planilha Concluída" foi
    // marcada (ou null se nunca foi, ou se o retrato estiver corrompido) —
    // usado para impedir que o usuário reduza um valor abaixo do que já
    // tinha no momento da conclusão (não dá pra "devolver" XP já gasto).
    fun planilhaNaConclusao(sheet: CharacterSheet): CharacterSheet? {
        if (sheet.snapshotConclusao.isBlank()) return null
        return try { com.example.data.CharacterSheetJsonCodec.decode(sheet.snapshotConclusao) } catch (e: Exception) { null }
    }

    // Recalcula as caixas de vitalidade extra da Técnica do Corpo de Touro
    // sempre que Vigor muda ou o Encanto é adquirido/removido — preserva o
    // dano já marcado nas caixas antigas (associando por penalidade) para
    // não perder progresso de combate ao recalcular.
    fun recalcularCaixasCorpoDeTouro(sheet: CharacterSheet): CharacterSheet {
        // APPROVED PERFORMANCE REFACTOR
        // Esta rotina é chamada após alterações de Vigor/Encantos. A trilha é
        // pequena, mas o caminho antigo fazia duas filtragens completas, depois
        // groupBy + mapValues + listas mutáveis e, por fim, flatMap + map.
        // Um único percurso separa as caixas e cria os buckets de dano herdável.
        val semAntigas = ArrayList<CaixaVitalidade>(sheet.healthBoxes.size)
        val antigasPorPenalidade = HashMap<String, MutableList<CaixaVitalidade>>()
        for (caixa in sheet.healthBoxes) {
            if (caixa.origemAutomatica == NOME_CORPO_DE_TOURO) {
                antigasPorPenalidade.getOrPut(caixa.penalidade) { ArrayList(2) }.add(caixa)
            } else {
                semAntigas.add(caixa)
            }
        }

        val quantidade = sheet.quantidadeCorpoDeTouroAdquirida()
        if (quantidade <= 0) {
            return if (antigasPorPenalidade.isEmpty()) sheet else sheet.copy(healthBoxes = semAntigas)
        }

        val lote = sheet.loteCorpoDeTouroPorVigor()
        val quantidadeNovas = lote.size * quantidade
        val novas = ArrayList<CaixaVitalidade>(quantidadeNovas)
        val proximasHerdadas = HashMap<String, Int>(antigasPorPenalidade.size)
        repeat(quantidade) {
            for (penalidade in lote) {
                val herdadas = antigasPorPenalidade[penalidade]
                val indice = proximasHerdadas[penalidade] ?: 0
                val herdada = if (herdadas != null && indice < herdadas.size) herdadas[indice] else null
                if (herdada != null) proximasHerdadas[penalidade] = indice + 1
                novas.add(
                    CaixaVitalidade(
                        penalidade = penalidade,
                        tipoDano = herdada?.tipoDano ?: 0,
                        isPermanente = false,
                        origemAutomatica = NOME_CORPO_DE_TOURO
                    )
                )
            }
        }

        val resultado = ArrayList<CaixaVitalidade>(semAntigas.size + novas.size)
        resultado.addAll(semAntigas)
        resultado.addAll(novas)
        return sheet.copy(healthBoxes = resultado)
    }

    // Extraído do corpo de calculateBpBreakdown (Seção 6 — refatoração de
    // organização, pedido explícito do usuário). Auto-contida: só lê de
    // `sheet`, não compartilha variáveis com as outras 5 seções do
    // cálculo. Mesma lógica de alocação do orçamento gratuito já usada
    // em Habilidades: os poderes mais caros (5 BP) recebem prioridade no
    // limite gratuito, sobrando os mais baratos (4 BP) pro excedente.
    private fun calcularCharmBp(sheet: CharacterSheet): Int {
        val freeCharmLimit = sheet.limitePoderesGratuitos()
        var expensivePowers = 0 // custo 5
        var cheapPowers = 0 // custo 4
        val habilidadesFavorecidasNormalizadas =
            (sheet.casteAbilities + sheet.favoredAbilities)
                .asSequence()
                .map(EncantosSolaresCatalog::normalize)
                .toSet()
        val artesMarciaisNormalizadas = sheet.martialArts
            .asSequence()
            .map { EncantosSolaresCatalog.normalize(it.nome) }
            .toHashSet()
        val ocultismoFavorecido = sheet.ocultismoCastaOuFavorecida()
        sheet.charms.forEach { charm ->
            val isCheap = if (charm.categoria == "Encanto") {
                val habilidadeNormalizada = EncantosSolaresCatalog.normalize(charm.habilidadeVinculada)
                val habilidadeParaChecar = if (habilidadeNormalizada in artesMarciaisNormalizadas) "Briga" else charm.habilidadeVinculada
                EncantosSolaresCatalog.normalize(habilidadeParaChecar) in habilidadesFavorecidasNormalizadas
            } else {
                ocultismoFavorecido
            }
            if (isCheap) cheapPowers++ else expensivePowers++
        }

        val freeExpensiveAllocated = expensivePowers.coerceAtMost(freeCharmLimit)
        val extraExpensive = expensivePowers - freeExpensiveAllocated
        val expensiveBp = extraExpensive * 5

        val remainingFreeCharms = (freeCharmLimit - freeExpensiveAllocated).coerceAtLeast(0)
        val freeCheapAllocated = cheapPowers.coerceAtMost(remainingFreeCharms)
        val extraCheap = cheapPowers - freeCheapAllocated
        val cheapBp = extraCheap * 4

        return expensiveBp + cheapBp
    }

    fun calculateBpBreakdown(sheet: CharacterSheet): BpBreakdown {
        if (sheet.tipoPersonagem.isLunar()) return calculateBpBreakdownLunar(sheet)
        // 1. Attributes BP
        var attrBp = 0
        val groups = arrayOf(
            "Físicos" to ExaltedConstants.PHYSICAL_ATTRIBUTES,
            "Sociais" to ExaltedConstants.SOCIAL_ATTRIBUTES,
            "Mentais" to ExaltedConstants.MENTAL_ATTRIBUTES
        )
        groups.forEach { (groupName, attrList) ->
            val prio = sheet.attributePriorities[groupName] ?: "1º"
            val grantedBaseDots = when (prio) {
                "1º" -> 8
                "2º" -> 6
                else -> 4
            }
            val addedDots = attrList.sumOf { (sheet.attributes[it] ?: 1) - 1 }
            val excess = (addedDots - grantedBaseDots).coerceAtLeast(0)
            val costPerExcess = if (prio == "3º") 3 else 4
            attrBp += excess * costPerExcess
        }

        // 2. Abilities Combinatorial BP Optimization (28 Base Free Dots)
        var abilityDirect45Bp = 0
        var sumNonFav13 = 0
        var sumFav13 = 0
        val habilidadesFavorecidas = (sheet.casteAbilities + sheet.favoredAbilities).toSet()

        ExaltedConstants.ALL_25_ABILITIES.forEach { abName ->
            val rating = sheet.abilities[abName] ?: 0
            val dots13 = rating.coerceAtMost(3)
            val dots45 = (rating - 3).coerceAtLeast(0)
            val isFav = abName in habilidadesFavorecidas

            abilityDirect45Bp += dots45 * (if (isFav) 1 else 2)
            if (isFav) {
                sumFav13 += dots13
            } else {
                sumNonFav13 += dots13
            }
        }

        val baseNonFavAllocated = sumNonFav13.coerceAtMost(28)
        val extraNonFav13 = sumNonFav13 - baseNonFavAllocated
        val nonFav13Bp = extraNonFav13 * 2

        val remainingBaseDots = 28 - baseNonFavAllocated
        val baseFavAllocated = sumFav13.coerceAtMost(remainingBaseDots)
        val extraFav13 = sumFav13 - baseFavAllocated
        val fav13Bp = extraFav13 * 1

        val abilityBp = abilityDirect45Bp + nonFav13Bp + fav13Bp

        // 3. Specializations BP
        val totalSpecs = sheet.specializations.size
        val specBp = (totalSpecs - 4).coerceAtLeast(0) * 1

        // 4. Willpower BP
        val willpowerBp = (sheet.forcaVontadeBase - 5).coerceAtLeast(0) * 2

        // 5. Merits BP
        // Sangue de Dragão nunca gasta Pontos de Bônus em Méritos — nem os
        // 13 pontos básicos, nem os 5 adicionais restritos por categoria
        // (pedido explícito do usuário). Achado real: essa fórmula usava
        // o limiar do Solar (10 pontos livres, excedente custa 1 BP)
        // incondicionalmente pros dois templates antes desta correção.
        val totalMeritVal = sheet.merits.sumOf { it.valor }
        val meritBp = if (sheet.tipoPersonagem.isDragonBlooded()) 0 else (totalMeritVal - 10).coerceAtLeast(0) * 1

        // 6. Poderes (Encantos, Feitiços e Necromancia) BP
        // Os 15 poderes iniciais são gratuitos (+1 para cada tag Controle obtida
        // entre esses 15, ver limitePoderesGratuitos()). Além do limite, cada
        // poder custa Pontos de Bônus conforme sua categoria:
        // - Encanto de Casta/Favorecido: 4 | Encanto fora de Casta/Favorecido: 5
        // - Feitiço/Necromancia com Ocultismo de Casta/Favorecida: 4 | sem: 5
        // Para minimizar o custo total, o limite gratuito é alocado primeiro aos
        // poderes mais caros (5 pontos), sobrando os mais baratos (4 pontos)
        // para eventual excedente — mesma lógica já usada nas Habilidades.
        val charmBp = calcularCharmBp(sheet)

        val totalSpent = attrBp + abilityBp + specBp + willpowerBp + meritBp + charmBp
        val remainingBalance = 15 - totalSpent

        return BpBreakdown(
            attributeBp = attrBp,
            abilityBp = abilityBp,
            specBp = specBp,
            willpowerBp = willpowerBp,
            meritBp = meritBp,
            charmBp = charmBp,
            totalSpent = totalSpent,
            remainingBalance = remainingBalance
        )
    }

    // Custos de Pontos de Bônus do Lunar — tabela fornecida pelo usuário,
    // estruturalmente diferente do Solar/Sangue de Dragão (ver
    // com.example.model.BpCostTable.Lunar pros valores centralizados).
    // NOTA: "Habilidade Latente de Animal" (custo 3) não é computada aqui
    // — não há campo correspondente na planilha ainda; precisa de
    // esclarecimento do usuário sobre o que esse traço representa antes
    // de implementar.
    // Extraído do corpo de calculateBpBreakdownLunar (refatoração de
    // organização — mesmo padrão já aplicado à versão Solar em
    // calcularCharmBp). Auto-contida: só lê de `sheet` e do conjunto de
    // Atributos favorecidos já calculado pelo chamador, sem compartilhar
    // mais nada com o resto do cálculo Lunar.
    private fun calcularCharmBpLunar(sheet: CharacterSheet, atributosFavorecidos: Set<String>): Int {
        val ehSemCasta = sheet.lunarCasta == com.example.model.LunarCasta.Casteless
        val freeCharmLimit = sheet.limitePoderesGratuitos()
        var expensivePowers = 0
        var cheapPowers = 0
        sheet.charms.forEach { charm ->
            val isCheap = if (charm.categoria == "Encanto") {
                charm.habilidadeVinculada in atributosFavorecidos
            } else {
                "Inteligência" in atributosFavorecidos
            }
            if (isCheap) cheapPowers++ else expensivePowers++
        }
        val freeExpensiveAllocated = expensivePowers.coerceAtMost(freeCharmLimit)
        val extraExpensive = expensivePowers - freeExpensiveAllocated
        val custoEncantoCaro = if (ehSemCasta) com.example.model.BpCostTable.Lunar.ENCANTO_NAO_FAVORECIDO_SEM_CASTA else com.example.model.BpCostTable.Lunar.ENCANTO_NAO_FAVORECIDO
        val expensiveBp = extraExpensive * custoEncantoCaro
        val remainingFreeCharms = (freeCharmLimit - freeExpensiveAllocated).coerceAtLeast(0)
        val freeCheapAllocated = cheapPowers.coerceAtMost(remainingFreeCharms)
        val extraCheap = cheapPowers - freeCheapAllocated
        val cheapBp = extraCheap * com.example.model.BpCostTable.Lunar.ENCANTO_CASTA_OU_FAVORECIDO
        return expensiveBp + cheapBp
    }

    private fun calculateBpBreakdownLunar(sheet: CharacterSheet): BpBreakdown {
        // O custo usa somente os 4 Atributos efetivamente escolhidos na
        // criação: 2 de Casta + 2 Favorecidos adicionais. O terceiro
        // Atributo disponível no pool da Casta não conta.
        val atributosFavorecidos = (sheet.lunarCasteAttributesEscolhidos + sheet.favoredAttributes).toSet()
        val groups = mapOf(
            "Físicos" to ExaltedConstants.PHYSICAL_ATTRIBUTES,
            "Sociais" to ExaltedConstants.SOCIAL_ATTRIBUTES,
            "Mentais" to ExaltedConstants.MENTAL_ATTRIBUTES
        )
        var attrBp = 0
        groups.forEach { (groupName, attrList) ->
            val prio = sheet.attributePriorities[groupName] ?: "1º"
            val grantedBaseDots = when (prio) {
                "1º" -> 8
                "2º" -> 6
                else -> 4
            }
            val addedDots = attrList.sumOf { (sheet.attributes[it] ?: 1) - 1 }
            val excess = (addedDots - grantedBaseDots).coerceAtLeast(0)
            if (excess > 0) {
                // O custo do excedente deve ser calculado por ponto e pela
                // categoria do Atributo que recebe o excedente. Usar o maior
                // custo do grupo inteiro supercobrava grupos mistos: bastava
                // existir um Atributo não favorecido para todos os pontos
                // excedentes serem cobrados a 4 PB, mesmo quando havia espaço
                // em Atributos de Casta/Favorecidos (3 PB).
                var restante = excess
                val favorecidosDisponiveis = attrList.sumOf {
                    if (it in atributosFavorecidos)
                        5 - (sheet.attributes[it] ?: 1).coerceAtLeast(1)
                    else 0
                }
                val barato = minOf(restante, favorecidosDisponiveis)
                attrBp += barato * com.example.model.BpCostTable.Lunar.ATRIBUTO_CASTA_OU_FAVORECIDO
                restante -= barato
                if (restante > 0) {
                    attrBp += restante * com.example.model.BpCostTable.Lunar.ATRIBUTO_NAO_FAVORECIDO
                }
            }
        }

        val totalAbilityDots = ExaltedConstants.ALL_25_ABILITIES.sumOf { (sheet.abilities[it] ?: 0) }
        val abilityExcess = (totalAbilityDots - 28).coerceAtLeast(0)
        val abilityBp = abilityExcess * com.example.model.BpCostTable.Lunar.HABILIDADE

        val totalSpecs = sheet.specializations.size
        val specBp = (totalSpecs - 4).coerceAtLeast(0) * com.example.model.BpCostTable.Lunar.ESPECIALIZACAO

        val willpowerBp = (sheet.forcaVontadeBase - 5).coerceAtLeast(0) * com.example.model.BpCostTable.Lunar.FORCA_DE_VONTADE

        val totalMeritVal = sheet.merits.sumOf { it.valor }
        val meritBp = (totalMeritVal - 10).coerceAtLeast(0) * com.example.model.BpCostTable.Lunar.MERITO

        val charmBp = calcularCharmBpLunar(sheet, atributosFavorecidos)

        val totalSpent = attrBp + abilityBp + specBp + willpowerBp + meritBp + charmBp
        val remainingBalance = 15 - totalSpent

        return BpBreakdown(
            attributeBp = attrBp,
            abilityBp = abilityBp,
            specBp = specBp,
            willpowerBp = willpowerBp,
            meritBp = meritBp,
            charmBp = charmBp,
            totalSpent = totalSpent,
            remainingBalance = remainingBalance
        )
    }
}
