package com.example.data

import com.example.model.CaixaVitalidade
import com.example.model.ArmaduraEncontro
import com.example.model.NpcEncontro

/**
 * Regras de progressão de Experiência de NPCs de encontro.
 *
 * Solar e Sangue de Dragão compartilham o algoritmo de compra/desfazimento;
 * somente catálogo, custo de Encanto, ordem de prioridade e trilha de
 * vitalidade são políticas fornecidas pela fachada EncounterGenerator.
 */
object EncounterExperienceService {
    internal const val XP_POR_CHAMADA = 5
    // ------------------------------------------------------------------
    // Solar / Sangue de Dragão — implementação em EncounterExperienceSolar.kt
    // ------------------------------------------------------------------

    fun expand(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        custoEncanto: (String) -> Int,
        trilhaVitalidade: (Int, Int) -> List<CaixaVitalidade>,
        habilidadeCombate: String? = null
    ): NpcEncontro = EncounterExperienceSolar.expand(
        npc, catalogo, ordemHabilidades, custoEncanto, trilhaVitalidade, habilidadeCombate
    )

    internal fun expandWithBatch(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        custoEncanto: (String) -> Int,
        trilhaVitalidade: (Int, Int) -> List<CaixaVitalidade>,
        habilidadeCombate: String? = null,
        catalogoPorHabilidadePreparado: Map<String, List<EncantoSolarDefinition>>? = null,
        catalogoRotasPreparado: EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition>? = null
    ): ExpansionResult = EncounterExperienceSolar.expandWithBatch(
        npc, catalogo, ordemHabilidades, custoEncanto, trilhaVitalidade, habilidadeCombate,
        catalogoPorHabilidadePreparado, catalogoRotasPreparado
    )

    fun expandRepeated(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        custoEncanto: (String) -> Int,
        trilhaVitalidade: (Int, Int) -> List<CaixaVitalidade>,
        quantidade: Int,
        habilidadeCombate: String? = null
    ): NpcEncontro = EncounterExperienceSolar.expandRepeated(
        npc, catalogo, ordemHabilidades, custoEncanto, trilhaVitalidade, quantidade, habilidadeCombate
    )

    fun reduce(npc: NpcEncontro): NpcEncontro = EncounterExperienceSolar.reduce(npc)

    fun aplicarPassoRoadmap(
        npc: NpcEncontro,
        passo: com.example.model.EncounterProgressionStep,
        expectedCatalogFingerprint: String = ""
    ): NpcEncontro? = aplicarPassoRoadmapInterno(
        npc = npc,
        passo = passo,
        expectedCatalogFingerprint = expectedCatalogFingerprint,
        validarPrecondicao = true
    )

    /**
     * Aplica um passo que acabou de ser devolvido por
     * [EncounterProgressionRoadmapService.proximo] para o mesmo snapshot.
     *
     * `proximo` já compara o fingerprint da pré-condição; repetir aqui a mesma
     * serialização + SHA-256 no caminho crítico do +XP era trabalho duplicado.
     * As demais validações (catálogo e orçamento de XP) continuam ativas.
     */
    fun aplicarPassoRoadmapJaValidado(
        npc: NpcEncontro,
        passo: com.example.model.EncounterProgressionStep,
        expectedCatalogFingerprint: String = ""
    ): NpcEncontro? = aplicarPassoRoadmapInterno(
        npc = npc,
        passo = passo,
        expectedCatalogFingerprint = expectedCatalogFingerprint,
        validarPrecondicao = false
    )

    private fun aplicarPassoRoadmapInterno(
        npc: NpcEncontro,
        passo: com.example.model.EncounterProgressionStep,
        expectedCatalogFingerprint: String,
        validarPrecondicao: Boolean
    ): NpcEncontro? {
        if (validarPrecondicao && passo.preconditionFingerprint.isNotBlank() &&
            passo.preconditionFingerprint != EncounterProgressionRoadmapService.fingerprint(npc)
        ) return null
        if (expectedCatalogFingerprint.isNotBlank() &&
            passo.catalogFingerprint != expectedCatalogFingerprint
        ) return null
        val xpDisponivel = npc.xpAtual + XP_POR_CHAMADA
        if (passo.xpGasto < 0 || passo.xpGasto > xpDisponivel) return null

        val charms = if (passo.encantos.isEmpty()) npc.charms else npc.charms + passo.encantos
        val abilities = if (passo.habilidadeMelhorada != null && passo.pontosGanhosNaHabilidade > 0) {
            val nome = passo.habilidadeMelhorada
            npc.abilities + (nome to ((npc.abilities[nome] ?: 0) + passo.pontosGanhosNaHabilidade))
        } else {
            npc.abilities
        }
        val especialidades = passo.especializacaoAdicionada?.let { habilidade ->
            if ((abilities[habilidade] ?: 0) >= 2) {
                npc.especialidades + com.example.model.EspecialidadeEncontro(habilidade)
            } else npc.especialidades
        } ?: npc.especialidades
        val corpoDeTouro = npc.corpoDeTouroCount + passo.encantos.count { it.nome == com.example.model.NOME_CORPO_DE_TOURO }
        val xpGastoTotal = npc.xpGastoTotal + passo.xpGasto
        val xpAtual = xpDisponivel - passo.xpGasto
        val essencia = essenciaPara(npc, xpGastoTotal)
        val health = if (corpoDeTouro == npc.corpoDeTouroCount) {
            npc.healthBoxes
        } else {
            val vigor = npc.attributes["Vigor"] ?: 1
            val healthBase = when (npc.tipoExaltado) {
                com.example.model.TipoExaltadoEncontro.LUNAR ->
                    LunarEncounterGenerator.trilhaVitalidadePorVigor(vigor, corpoDeTouro)
                com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO ->
                    EncounterGenerator.trilhaVitalidadeSangueDeDragaoPorVigor(vigor, corpoDeTouro)
                else ->
                    EncounterGenerator.trilhaVitalidadePorVigor(vigor, corpoDeTouro)
            }
            val efeitosMeritos = EncounterMeritEffectsService.efeitos(npc.merits)
            recalcularVitalidadePreservandoDano(
                npc.healthBoxes,
                EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, healthBase)
            )
        }
        val historico = npc.historicoXpBatches + com.example.model.HistoricoXpBatch(
            xpGasto = passo.xpGasto,
            nomesEncantosAdicionados = passo.encantos.map { it.nome },
            habilidadeMelhorada = passo.habilidadeMelhorada,
            pontosGanhosNaHabilidade = passo.pontosGanhosNaHabilidade,
            especializacaoAdicionada = passo.especializacaoAdicionada,
            pontosForcaDeVontadeComprados = passo.pontosForcaDeVontadeComprados
        )
        val atualizado = npc.copy(
            charms = charms, abilities = abilities, especialidades = especialidades,
            corpoDeTouroCount = corpoDeTouro, essencia = essencia, xpAtual = xpAtual,
            xpGastoTotal = xpGastoTotal, healthBoxes = health,
            motesPersonais = motesPersonaisPara(npc, essencia),
            motesPerifericos = motesPerifericosPara(npc, essencia),
            historicoXpBatches = historico
        )
        return recalcularDerivados(atualizarAlertasValidacao(atualizado))
    }


    /** Reprocessa a trilha sem apagar ferimentos: a quantidade de caixas
     * danificadas é projetada proporcionalmente para o novo tamanho e os
     * tipos de dano existentes são preservados na mesma proporção. */
    internal fun recalcularVitalidadePreservandoDano(
        antigas: List<CaixaVitalidade>,
        novas: List<CaixaVitalidade>
    ): List<CaixaVitalidade> {
        if (novas.isEmpty()) return novas
        if (antigas.isEmpty()) return novas

        // Reaproveita a identidade das caixas que continuam existindo. Isso
        // evita que uma simples recalculação (por exemplo, uma compra de XP
        // que não altere o tamanho da trilha) pareça uma troca completa de
        // componentes para o Compose e, mais importante, preserva qualquer
        // estado associado à caixa. Novas caixas recebem os IDs gerados pela
        // própria trilha.
        val comIdsPreservados = novas.mapIndexed { index, caixa ->
            antigas.getOrNull(index)?.let { antiga ->
                caixa.copy(id = antiga.id, tipoDano = antiga.tipoDano)
            } ?: caixa
        }

        val danificadas = antigas.withIndex()
            .sortedBy { it.value.penaltyRank() }
            .map { it.value }
            .filter { it.tipoDano != 0 }
        if (danificadas.isEmpty()) return comIdsPreservados

        // A quantidade de dano já sofrida não muda quando a capacidade máxima
        // da trilha é ampliada. O que muda é a posição das marcas: elas devem
        // ocupar as primeiras caixas da ordem visual, da esquerda para a direita
        // e linha por linha. Assim, adquirir Corpo de Touro nunca cria buracos
        // nem espalha dano proporcionalmente pela trilha. Se a capacidade
        // diminuir, o dano é apenas limitado ao novo máximo disponível.
        val novasEmOrdemVisual = comIdsPreservados.withIndex()
            .sortedBy { it.value.penaltyRank() }
            .map { it.value }
        val tiposPorId = HashMap<String, Int>(novasEmOrdemVisual.size)
        novasEmOrdemVisual.forEachIndexed { index, caixa ->
            tiposPorId[caixa.id] = danificadas.getOrNull(index)?.tipoDano ?: 0
        }
        return comIdsPreservados.map { caixa ->
            caixa.copy(tipoDano = tiposPorId[caixa.id] ?: 0)
        }
    }

    // APPROVED PERFORMANCE REFACTOR
    // Centraliza a regra de cálculo de Essência por XP para evitar duplicação
    // de condicionais nos caminhos de expand/reduce. A fórmula continua
    // dependente do tipo de Exaltado exatamente como antes.
    internal fun essenciaPara(npc: NpcEncontro, xpGastoTotal: Int): Int =
        if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO) {
            EncounterGenerator.essenciaPorXpGastoSangueDeDragao(xpGastoTotal).coerceAtMost(5)
        } else {
            EncounterGenerator.essenciaPorXpGasto(xpGastoTotal).coerceAtMost(5)
        }

    // APPROVED PERFORMANCE REFACTOR
    // Recursos derivados da Essência são recalculados junto com a própria
    // Essência. Antes, expand/reduce alterava a Essência mas preservava os
    // valores antigos de Motes, deixando a planilha internamente inconsistente.
    // A regra permanece exatamente a dos geradores: Solar/Lunar compartilham
    // a progressão 10/26 + 3/7 por Essência; Sangue de Dragão usa 11/23 + 1/4.
    internal fun motesPersonaisPara(npc: NpcEncontro, essencia: Int): Int =
        EncounterMoteService.totais(npc.tipoExaltado, essencia, npc.arma, npc.armadura).pessoaisMax

    internal fun motesPerifericosPara(npc: NpcEncontro, essencia: Int): Int =
        EncounterMoteService.totais(npc.tipoExaltado, essencia, npc.arma, npc.armadura).perifericosDisponiveis


    // ------------------------------------------------------------------
    // Lunar — implementação extraída para EncounterExperienceLunar.kt
    // ------------------------------------------------------------------

    fun expandLunar(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>
    ): NpcEncontro = EncounterExperienceLunar.expandLunar(npc, catalogo, ordemAtributos)

    fun expandLunarWithBatch(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>
    ): ExpansionResult = EncounterExperienceLunar.expandLunarWithBatch(npc, catalogo, ordemAtributos)

    fun expandLunarRepeated(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>,
        quantidade: Int
    ): NpcEncontro = EncounterExperienceLunar.expandLunarRepeated(npc, catalogo, ordemAtributos, quantidade)

    fun reduceLunar(npc: NpcEncontro): NpcEncontro =
        EncounterExperienceLunar.reduceLunar(npc)


    // APPROVED PERFORMANCE/CONSISTENCY REFACTOR
    // XP altera Habilidades/Especializações; portanto, os derivados armazenados
    // no NPC precisam ser recalculados no mesmo lote. Sem isso, o gerador criava
    // valores corretos, mas expand/reduce deixava ataques, defesas e iniciativa
    // com os números antigos. A política específica de cada Exaltado permanece
    // centralizada aqui e nenhuma fórmula nova é introduzida.
    // CORREÇÃO FUNCIONAL — remoção reversível de Encantos adquiridos no lote.
    // Alguns Encantos são repetíveis (em particular Técnica do Corpo de Touro).
    // filterNot por nome removia também aquisições de lotes anteriores.
    // O histórico registra cada aquisição; portanto, a redução deve consumir
    // exatamente essa multiplicidade e preservar as ocorrências anteriores.
    internal fun removerEncantosDoLote(
        charms: List<com.example.model.EncantoEncontro>,
        nomesDoLote: List<String>
    ): List<com.example.model.EncantoEncontro> {
        if (nomesDoLote.isEmpty()) return charms
        val quantidadeParaRemover = nomesDoLote.groupingBy { it }.eachCount().toMutableMap()
        return charms.filter { encanto ->
            val restante = quantidadeParaRemover[encanto.nome] ?: 0
            if (restante > 0) {
                quantidadeParaRemover[encanto.nome] = restante - 1
                false
            } else {
                true
            }
        }
    }

    // APPROVED CONSISTENCY REFACTOR
    // XP pode corrigir exatamente as condições que produziram um alerta na
    // geração (por exemplo, adquirir Corpo de Touro ou atingir Atributo 5).
    // Sem esta atualização, a planilha continuava exibindo alertas obsoletos até
    // ser regenerada. Alertas de geração que não pertencem à validação central
    // são preservados; somente as mensagens produzidas por validar() são
    // substituídas pelo estado atual do NPC.
    internal fun atualizarAlertasValidacao(npc: NpcEncontro): NpcEncontro {
        // As mensagens antigas de validação da Aba 11 eram apenas avisos de
        // exceção estatística. Elas não representam erro e não devem voltar a
        // aparecer após alterações de XP.
        val alertasDeErro = npc.alertasValidacao.filterNot { alerta ->
            alerta.startsWith("Nenhum Atributo ") ||
                alerta.startsWith("Corpo de Touro não foi adquirido") || alerta.startsWith("Combatente físico sem Corpo de Touro") ||
                alerta.startsWith("Círculo de Magia não foi sorteado") ||
                (alerta.startsWith("Só ") && (alerta.contains("Feitiço(s) — o esperado pro Mental é pelo menos 4.") || alerta.contains("apesar do acesso à Feitiçaria"))) ||
                alerta.startsWith("Só ") && alerta.contains("Encantos iniciais elegíveis") ||
                alerta.startsWith("Nenhum Feitiço selecionado — Social costuma ter 1-2")
        }
        return npc.copy(alertasValidacao = alertasDeErro)
    }

    internal fun recalcularDerivados(npc: NpcEncontro): NpcEncontro {
        val especialidades = npc.especialidades.asSequence().map { it.habilidade }.toSet()
        val temIntegridade = "Integridade" in especialidades
        val temSocializacao = "Socialização" in especialidades
        val temAtletismo = "Atletismo" in especialidades
        val temEsquiva = "Esquiva" in especialidades
        val temProntidao = "Prontidão" in especialidades
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(npc.merits)
        val bonusJuntar = when (npc.tipoExaltado) {
            com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO ->
                (if (temProntidao) 1 else 0) + efeitosMeritos.bonusJuntarABatalha
            else -> 1 + efeitosMeritos.bonusJuntarABatalha
        }
        // NPCs mínimos de teste (e alguns estados intermediários) podem não ter
        // armadura/habilidadeDefensiva preenchidos. Usamos defaults seguros para
        // não quebrar o pipeline de XP.
        val armaduraEfetiva = npc.armadura ?: ArmaduraEncontro(
            nome = "Nenhuma",
            peso = "Leve",
            tipo = "Mundana",
            absorcao = 0,
            dureza = 0,
            penalidadeMobilidade = 0,
            motesComitados = 0
        )
        val habilidadeDefensivaEfetiva = npc.habilidadeDefensiva
            ?.takeIf { it.isNotBlank() }
            ?: npc.habilidadePrincipal.takeIf { it.isNotBlank() }
            ?: "Esquiva"
        val derivados = EncounterDerivedStatsService.calcularDerivadosComuns(
            npc.attributes, npc.abilities, armaduraEfetiva,
            bonusPerseveranca = if (temIntegridade) 1 else 0,
            bonusAstucia = if (temSocializacao) 1 else 0,
            bonusJuntarBatalha = bonusJuntar,
            bonusInvestida = (if (temAtletismo) 1 else 0) + efeitosMeritos.bonusInvestida,
            bonusDesengajamento = (if (temEsquiva) 1 else 0) + efeitosMeritos.bonusDesengajamento
        )
        val combate = EncounterCombatCalculationService.calcular(
            arquetipo = npc.arquetipo,
            habilidadeCombate = npc.habilidadePrincipal,
            attributes = npc.attributes,
            abilities = npc.abilities,
            arma = npc.arma,
            armadura = armaduraEfetiva
        )
        val motes = EncounterMoteService.totais(
            npc.tipoExaltado, npc.essencia, npc.arma, armaduraEfetiva
        )
        return npc.copy(
            acaoPrincipal = combate.acaoPrincipal,
            acaoDecisiva = combate.acaoDecisiva,
            defesaPrimaria = combate.defesaPrimaria,
            esquiva = combate.esquiva,
            dano = combate.dano,
            absorcaoNatural = derivados.absorcaoNatural,
            absorcao = derivados.absorcaoTotal,
            perseveranca = derivados.perseveranca,
            astucia = derivados.astucia,
            juntarABatalha = derivados.juntarABatalha,
            investida = derivados.investida,
            desengajamento = derivados.desengajamento,
            motesPersonais = motes.pessoaisMax,
            motesPerifericos = motes.perifericosDisponiveis
        )
    }
}