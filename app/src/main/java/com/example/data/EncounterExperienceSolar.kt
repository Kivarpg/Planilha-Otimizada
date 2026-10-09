package com.example.data

import com.example.model.CaixaVitalidade
import com.example.model.EspecialidadeEncontro
import com.example.model.HistoricoXpBatch
import com.example.model.NOME_CORPO_DE_TOURO
import com.example.model.NpcEncontro
import com.example.model.ExaltedConstants

/**
 * Progressão de XP específica de Solares / Sangue de Dragão (Aba 11).
 *
 * Extraído de EncounterExperienceService (refatoração de organização —
 * roteiro de refatoração agressiva, sem mudança de comportamento).
 * Isola a trilha baseada em Habilidades da trilha Lunar (Atributos).
 *
 * Helpers compartilhados permanecem em EncounterExperienceService.
 */
internal object EncounterExperienceSolar {
    private const val XP_RESERVADO_HABILIDADE_POR_LOTE = 10
    private const val CUSTO_XP_PONTO_HABILIDADE = 7
    private const val CUSTO_XP_ESPECIALIZACAO = 3

    private data class IndiceXpSolar(
        val candidatosPorHabilidade: Map<String, List<EncantoSolarDefinition>>,
        val ordemHabilidades: List<String>,
        val custoPorHabilidade: Map<String, Int>,
        val custoMinimoEncanto: Int?,
        val catalogoPreparado: EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition>
    )

    // Extraído do corpo de expandInternal (refatoração de organização —
    // pedido explícito do usuário, feito com cautela extra por lidar com
    // progressão de XP de NPCs). Fase 1 do lote: tenta comprar Encantos
    // elegíveis enquanto houver saldo. charmsAtuais/nomesSelecionados/
    // categoriasSelecionadas são mutados IN PLACE (mesmas referências
    // recebidas) — o retorno os inclui de qualquer forma por clareza no
    // chamador, mas a mutação real já aconteceu nos objetos passados.
    // corpoDeTouro é Int (valor), por isso precisa vir pelo retorno.
    private data class ResultadoCompraCharms(
        val xpDisponivel: Int,
        val xpGastoTotal: Int,
        val corpoDeTouro: Int,
        val nomesCharmsNesteLote: List<String>,
        val comprouAlgo: Boolean,
        val teveElegivel: Boolean
    )

    private fun tentarComprarCharms(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        indice: IndiceXpSolar,
        habilidadeCombate: String?,
        xpDisponivelInicial: Int,
        xpGastoTotalInicial: Int,
        corpoDeTouroInicial: Int,
        abilitiesAtuais: Map<String, Int>,
        charmsAtuais: MutableList<com.example.model.EncantoEncontro>,
        nomesSelecionados: MutableSet<String>,
        categoriasSelecionadas: MutableMap<String, Int>
    ): ResultadoCompraCharms {
        var xpDisponivel = xpDisponivelInicial
        var xpGastoTotal = xpGastoTotalInicial
        var corpoDeTouro = corpoDeTouroInicial
        val nomesCharmsNesteLote = mutableListOf<String>()

        val custoPorHabilidade = indice.custoPorHabilidade
        // O otimizador é chamado repetidamente dentro dos loops de progressão.
        // O catálogo e sua classificação são invariantes durante todo o lote.
        val catalogoPreparado = indice.catalogoPreparado
        // O mesmo estado mecânico aparece em chamadas consecutivas do beam
        // enquanto um bloco é aprofundado. Compartilhar o memo durante o lote
        // evita reavaliar todo o catálogo para estados já observados.
        val elegibilidadeMemo = HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()
        var essenciaMemoAtual: Int? = null
        var tentativas = 0
        var encantoCompradoNesteLote = false
        var encantoElegivelNesteLote = false
        val ramos = indice.ordemHabilidades
        val arvoresAtivas = mutableListOf<String>().apply { ramos.firstOrNull()?.let(::add) }
        // Incompatibilidade de combate depende apenas do ramo + habilidade
        // principal do NPC durante este lote. Pré-calcular evita repetir a
        // mesma consulta ao conjunto de Habilidades de combate a cada tentativa.
        val ramosCombateIncompativeis = if (habilidadeCombate == null) {
            emptySet()
        } else {
            ramos.asSequence()
                .filter { it in EncounterGenerationRules.COMBAT_ABILITIES && it != habilidadeCombate }
                .toHashSet()
        }
        // Cursor monotônico é seguro somente para ABRIR ramos: ramos nunca são
        // removidos/reordenados neste lote. Não é usado para candidatos, cuja
        // elegibilidade pode mudar depois da compra de pré-requisitos.
        var proximoRamoIndex = if (arvoresAtivas.isEmpty()) 0 else 1
        // Um ramo que já foi avaliado sem conseguir comprar nada no estado
        // mecânico atual não precisa executar o mesmo beam novamente. O conjunto
        // é invalidado imediatamente após qualquer compra, quando elegibilidade,
        // contagens ou pré-requisitos podem ter mudado.
        val ramosSemCompraNoEstado = HashSet<String>()

        // Se o saldo não alcança nem o Encanto mais barato, o beam não pode
        // produzir uma compra. Ainda precisamos distinguir "não há elegível" de
        // "há elegível, mas falta XP", pois esta segunda situação preserva o
        // saldo para a próxima chamada. Faça somente a consulta de legalidade.
        val custoMinimoEncanto = indice.custoMinimoEncanto
        if (custoMinimoEncanto != null && xpDisponivel < custoMinimoEncanto) {
            val essenciaAtual = EncounterExperienceService.essenciaPara(npc, xpGastoTotal)
            val limiteCorpoDeTouro = (abilitiesAtuais["Resistência"] ?: 0).coerceAtLeast(0)
            val existeElegivel = ramos.asSequence()
                .filterNot { it in ramosCombateIncompativeis }
                .flatMap { indice.candidatosPorHabilidade[it].orEmpty().asSequence() }
                .any { def ->
                    (def.nome !in nomesSelecionados ||
                        (def.nome == NOME_CORPO_DE_TOURO && corpoDeTouro < limiteCorpoDeTouro)) &&
                        elegivel(
                            def, abilitiesAtuais, essenciaAtual, nomesSelecionados, catalogo,
                            contagemCategoriaSelecionada = { categoria ->
                                categoriasSelecionadas[categoria.lowercase()] ?: 0
                            }
                        )
                }
            return ResultadoCompraCharms(
                xpDisponivel, xpGastoTotal, corpoDeTouro, emptyList(),
                comprouAlgo = false, teveElegivel = existeElegivel
            )
        }

        // Progressão por blocos: completa 3 Encantos na mesma Árvore antes de
        // abrir outra. As três primeiras Árvores prioritárias são aprofundadas
        // antes que novas Árvores entrem na construção. Isso evita a antiga
        // distribuição round-robin 1/1/1/1 e preserva sinergias.
        while (tentativas++ < 500 && xpDisponivel > 0) {
            var progressoRodada = false

            fun comprarBloco(habilidadeRamo: String): Boolean {
                if (habilidadeRamo in ramosSemCompraNoEstado) return false
                var progresso = false
                val categoriaRamo = habilidadeRamo.lowercase()
                val custoRamo = custoPorHabilidade[habilidadeRamo]
                val alvo = (categoriasSelecionadas[categoriaRamo] ?: 0) + 3
                var tentativasBloco = 0
                while (
                    tentativasBloco++ < 100 &&
                    (categoriasSelecionadas[categoriaRamo] ?: 0) < alvo
                ) {
                    val essenciaAtual = EncounterExperienceService.essenciaPara(npc, xpGastoTotal)
                    // A Essência participa da elegibilidade, mas não da chave compacta.
                    // Uma mudança de círculo exige invalidar os resultados anteriores.
                    if (essenciaAtual != essenciaMemoAtual) {
                        elegibilidadeMemo.clear()
                        essenciaMemoAtual = essenciaAtual
                    }
                    val limiteCorpoDeTouro = (abilitiesAtuais["Resistência"] ?: 0).coerceAtLeast(0)
                    // O índice já agrupa por habilidade: todos os Encantos deste
                    // bucket compartilham habilidadeRamo. Portanto, a restrição
                    // de combate pode ser decidida uma vez por ramo, sem criar
                    // uma nova List a cada tentativa do bloco.
                    val candidatosDoRamo =
                        if (habilidadeRamo in ramosCombateIncompativeis) emptyList()
                        else indice.candidatosPorHabilidade[habilidadeRamo].orEmpty()
                    val candidatoElegivel = EncounterCharmRouteOptimizer.escolher(
                        candidatos = candidatosDoRamo,
                        catalogoCompleto = catalogo,
                        nomesSelecionados = nomesSelecionados,
                        contagensCategorias = categoriasSelecionadas,
                        elegivel = { def, nomes, contagens ->
                            (def.nome !in nomes || (def.nome == NOME_CORPO_DE_TOURO && corpoDeTouro < limiteCorpoDeTouro)) &&
                                elegivel(
                                    def, abilitiesAtuais, essenciaAtual, nomes, catalogo,
                                    contagemCategoriaSelecionada = { categoria -> contagens[categoria.lowercase()] ?: 0 }
                                )
                        },
                        nome = { it.nome },
                        categoria = { it.habilidade },
                        custoXp = { custoPorHabilidade[it.habilidade] ?: 10 },
                        // A compra repetida pode ser legal no estado real,
                        // mas não deve ser projetada repetidamente sem contador
                        // individual de Corpo de Touro no estado do beam.
                        permiteAquisicaoRepetida = { it.nome == NOME_CORPO_DE_TOURO },
                        repeatableOnlyAtRoot = true,
                        compactEligibilityMemo = elegibilidadeMemo,
                        preparedCatalog = catalogoPreparado
                    ) ?: break

                    // Um Encanto elegível, mas ainda caro demais para o saldo atual,
                    // deve fazer o XP permanecer acumulado para a próxima chamada.
                    // Não é permitido gastar esse saldo em Habilidade/Especialização
                    // enquanto existe uma compra de Encanto prioritária pendente.
                    val custo = custoRamo ?: break
                    if (custo > xpDisponivel) {
                        encantoElegivelNesteLote = true
                        break
                    }
                    val candidato = candidatoElegivel
                    xpDisponivel -= custo
                    xpGastoTotal += custo
                    charmsAtuais += com.example.model.EncantoEncontro(candidato.nome, candidato.habilidade, candidato.custo)
                    nomesSelecionados += candidato.nome
                    nomesCharmsNesteLote += candidato.nome
                    categoriasSelecionadas[categoriaRamo] = (categoriasSelecionadas[categoriaRamo] ?: 0) + 1
                    if (candidato.nome == NOME_CORPO_DE_TOURO) {
                        corpoDeTouro++
                        // A elegibilidade de compras repetidas depende deste contador,
                        // que não faz parte da chave compacta do otimizador.
                        elegibilidadeMemo.clear()
                    }
                    encantoCompradoNesteLote = true
                    encantoElegivelNesteLote = true
                    // A compra altera o estado usado pelo otimizador. Ramos que
                    // antes não tinham compra possível precisam poder ser
                    // reconsiderados sob o novo conjunto de pré-requisitos.
                    ramosSemCompraNoEstado.clear()
                    progresso = true
                    progressoRodada = true
                }
                if (!progresso) {
                    ramosSemCompraNoEstado += habilidadeRamo
                }
                return progresso
            }

            // A Árvore atual recebe prioridade integral. Só quando ela não
            // possui mais compras possíveis é que uma nova Árvore é aberta.
            // As três primeiras Árvores formam o núcleo prioritário; depois
            // disso, novas Árvores podem ser consideradas como complementares.
            for (ramo in arvoresAtivas) {
                if (xpDisponivel <= 0) break
                comprarBloco(ramo)
                if (progressoRodada) break
            }
            if (progressoRodada) continue

            // Só quando as Árvores atuais não têm mais compras possíveis é que
            // uma nova Árvore pode ser aberta.
            if (proximoRamoIndex >= ramos.size) break
            val proximaArvore = ramos[proximoRamoIndex++]
            arvoresAtivas += proximaArvore
            comprarBloco(proximaArvore)
        }
        return ResultadoCompraCharms(
            xpDisponivel, xpGastoTotal, corpoDeTouro, nomesCharmsNesteLote,
            encantoCompradoNesteLote, encantoElegivelNesteLote
        )
    }

    private fun criarIndiceXpSolar(
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        custoEncanto: (String) -> Int,
        catalogoPorHabilidadePreparado: Map<String, List<EncantoSolarDefinition>>? = null,
        catalogoRotasPreparado: EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition>? = null
    ): IndiceXpSolar {
        val catalogoPorHabilidade = catalogoPorHabilidadePreparado ?: catalogo.groupBy { it.habilidade }
        // Constrói a união ordenada uma única vez. Antes, a mesma
        // concatenação + filtro + distinct era percorrida duas vezes por +XP:
        // uma para custos e outra para a ordem efetiva dos ramos.
        val ordemHabilidadesUnicas = (ordemHabilidades + ExaltedConstants.ALL_25_ABILITIES)
            .filter { it.isNotBlank() }
            .distinct()
        val custoPorHabilidade = ordemHabilidadesUnicas.associateWith(custoEncanto)
        // A prioridade define a ordem de preferência, não um filtro de ramos.
        // Depois dela, todas as 25 Habilidades entram no índice para que uma
        // linha não selecionada inicialmente continue disponível na progressão.
        // O catálogo já veio agrupado por Habilidade. Reutilizar diretamente
        // esses buckets evita um segundo groupBy sobre candidatosNaOrdem em
        // cada +XP, sem alterar ordem, conteúdo ou decisão do otimizador.
        val candidatosPorHabilidade = catalogoPorHabilidade
        return IndiceXpSolar(
            candidatosPorHabilidade = candidatosPorHabilidade,
            ordemHabilidades = ordemHabilidadesUnicas,
            custoPorHabilidade = custoPorHabilidade,
            custoMinimoEncanto = custoPorHabilidade.values.minOrNull(),
            catalogoPreparado = catalogoRotasPreparado ?: EncounterCharmRouteOptimizer.prepareCatalog(
                catalogoCompleto = catalogo,
                nome = { it.nome },
                categoria = { it.habilidade }
            )
        )
    }

    fun expand(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        custoEncanto: (String) -> Int,
        trilhaVitalidade: (Int, Int) -> List<CaixaVitalidade>,
        habilidadeCombate: String? = null
    ): NpcEncontro = expandInternalResult(
        npc, catalogo, ordemHabilidades, trilhaVitalidade,
        criarIndiceXpSolar(catalogo, ordemHabilidades, custoEncanto), habilidadeCombate
    ).npcResultante

    fun expandWithBatch(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        custoEncanto: (String) -> Int,
        trilhaVitalidade: (Int, Int) -> List<CaixaVitalidade>,
        habilidadeCombate: String? = null,
        catalogoPorHabilidadePreparado: Map<String, List<EncantoSolarDefinition>>? = null,
        catalogoRotasPreparado: EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition>? = null
    ): ExpansionResult = expandInternalResult(
        npc, catalogo, ordemHabilidades, trilhaVitalidade,
        criarIndiceXpSolar(
            catalogo, ordemHabilidades, custoEncanto,
            catalogoPorHabilidadePreparado, catalogoRotasPreparado
        ), habilidadeCombate
    )

    /**
     * Expande vários lotes mantendo o índice imutável do catálogo durante toda
     * a operação. A semântica permanece exatamente a de N chamadas sucessivas
     * a [expand]: cada lote continua criando seu próprio histórico e pode ser
     * desfeito individualmente.
     *
     * O ganho é relevante durante a criação de NPCs com muitos Bonus Points:
     * groupBy/flatMap/associateWith do catálogo deixa de ser repetido para cada
     * lote.
     */
    fun expandRepeated(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        custoEncanto: (String) -> Int,
        trilhaVitalidade: (Int, Int) -> List<CaixaVitalidade>,
        quantidade: Int,
        habilidadeCombate: String? = null
    ): NpcEncontro {
        if (quantidade <= 0) return npc
        val indice = criarIndiceXpSolar(catalogo, ordemHabilidades, custoEncanto)
        var atual = npc
        repeat(quantidade) {
            atual = expandInternalResult(atual, catalogo, ordemHabilidades, trilhaVitalidade, indice, habilidadeCombate).npcResultante
        }
        return atual
    }

    private fun expandInternalResult(
        npc: NpcEncontro,
        catalogo: List<EncantoSolarDefinition>,
        ordemHabilidades: List<String>,
        trilhaVitalidade: (Int, Int) -> List<CaixaVitalidade>,
        indice: IndiceXpSolar,
        habilidadeCombate: String? = null
    ): ExpansionResult {
        var xpDisponivel = npc.xpAtual + EncounterExperienceService.XP_POR_CHAMADA
        var xpGastoTotal = npc.xpGastoTotal
        val charmsAtuais = npc.charms.toMutableList()
        // Um único percurso prepara nome + contagem por categoria. Este estado
        // é mutável durante o lote, portanto continua local e semanticamente
        // idêntico ao par map/groupingBy anterior.
        val nomesSelecionados = HashSet<String>(charmsAtuais.size * 2)
        val categoriasSelecionadas = HashMap<String, Int>()
        charmsAtuais.forEach { charm ->
            nomesSelecionados += charm.nome
            val categoria = charm.habilidadeVinculada.lowercase()
            categoriasSelecionadas[categoria] = (categoriasSelecionadas[categoria] ?: 0) + 1
        }
        var corpoDeTouro = npc.corpoDeTouroCount
        val abilitiesAtualizadas = npc.abilities.toMutableMap()
        val especialidadesAtualizadas = npc.especialidades.toMutableList()
        var habilidadeMelhoradaNesteLote: String? = null
        var pontosGanhosNesteLote = 0
        var especializacaoAdicionadaNesteLote: String? = null
        var forcaDeVontadeAtualizada = npc.forcaDeVontade
        var pontosForcaDeVontadeCompradosNesteLote = 0

        // XP do botão é gasto assim que houver saldo suficiente para um Encanto
        // elegível. Encantos favorecidos/casta/aspecto custam 8 XP. Quando há
        // um Encanto disponível, ele tem prioridade: o saldo não fica parado
        // até um marco arbitrário (como 90 XP) nem é consumido antes por uma
        // melhoria de Habilidade/Especialização.
        val resultadoCompra = tentarComprarCharms(
            npc, catalogo, indice, habilidadeCombate,
            xpDisponivel, xpGastoTotal, corpoDeTouro, abilitiesAtualizadas,
            charmsAtuais, nomesSelecionados, categoriasSelecionadas
        )
        xpDisponivel = resultadoCompra.xpDisponivel
        xpGastoTotal = resultadoCompra.xpGastoTotal
        corpoDeTouro = resultadoCompra.corpoDeTouro
        val nomesCharmsNesteLote = resultadoCompra.nomesCharmsNesteLote
        val encantoCompradoNesteLote = resultadoCompra.comprouAlgo
        val encantoElegivelNesteLote = resultadoCompra.teveElegivel

        // Mantemos a progressão antiga de Habilidade/Especialização quando
        // nenhum Encanto pôde ser comprado neste lote. Assim, o botão continua
        // útil para NPCs sem catálogo de Encantos ou enquanto nenhum Encanto
        // estiver elegível, mas nunca impede uma compra de Encanto que já caiba
        // no saldo.
        if (!encantoCompradoNesteLote && !encantoElegivelNesteLote) {
            var xpReservado = XP_RESERVADO_HABILIDADE_POR_LOTE.coerceAtMost(xpDisponivel)
            val habilidadeParaMelhorar = ordemHabilidades.firstOrNull {
                it in abilitiesAtualizadas && (abilitiesAtualizadas[it] ?: 0) < 5
            }
            if (habilidadeParaMelhorar != null) {
                if (xpReservado >= CUSTO_XP_PONTO_HABILIDADE) {
                    abilitiesAtualizadas[habilidadeParaMelhorar] = (abilitiesAtualizadas[habilidadeParaMelhorar] ?: 0) + 1
                    xpDisponivel -= CUSTO_XP_PONTO_HABILIDADE
                    xpGastoTotal += CUSTO_XP_PONTO_HABILIDADE
                    xpReservado -= CUSTO_XP_PONTO_HABILIDADE
                    habilidadeMelhoradaNesteLote = habilidadeParaMelhorar
                    pontosGanhosNesteLote = 1
                }
                if (xpReservado >= CUSTO_XP_ESPECIALIZACAO &&
                    (abilitiesAtualizadas[habilidadeParaMelhorar] ?: 0) >= 2 &&
                    especialidadesAtualizadas.none { it.habilidade == habilidadeParaMelhorar }
                ) {
                    xpDisponivel -= CUSTO_XP_ESPECIALIZACAO
                    xpGastoTotal += CUSTO_XP_ESPECIALIZACAO
                    especialidadesAtualizadas += EspecialidadeEncontro(habilidadeParaMelhorar)
                    especializacaoAdicionadaNesteLote = habilidadeParaMelhorar
                }
            }
        }

        val essenciaFinal = EncounterExperienceService.essenciaPara(npc, xpGastoTotal)
        val vigor = npc.attributes["Vigor"] ?: 1
        val healthBoxesBase = trilhaVitalidade(vigor, corpoDeTouro)
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(npc.merits)
        val healthBoxesAtualizadas = EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, healthBoxesBase)
        val healthFinal = EncounterExperienceService.recalcularVitalidadePreservandoDano(npc.healthBoxes, healthBoxesAtualizadas)

        val batch = HistoricoXpBatch(
            xpGasto = xpGastoTotal - npc.xpGastoTotal,
            nomesEncantosAdicionados = nomesCharmsNesteLote,
            habilidadeMelhorada = habilidadeMelhoradaNesteLote,
            pontosGanhosNaHabilidade = pontosGanhosNesteLote,
            especializacaoAdicionada = especializacaoAdicionadaNesteLote,
            pontosForcaDeVontadeComprados = pontosForcaDeVontadeCompradosNesteLote
        )
        val npcAtualizado = npc.copy(
            charms = charmsAtuais, abilities = abilitiesAtualizadas, especialidades = especialidadesAtualizadas,
            corpoDeTouroCount = corpoDeTouro, essencia = essenciaFinal, xpAtual = xpDisponivel, xpGastoTotal = xpGastoTotal,
            healthBoxes = healthFinal, forcaDeVontade = forcaDeVontadeAtualizada,
            motesPersonais = EncounterExperienceService.motesPersonaisPara(npc, essenciaFinal),
            motesPerifericos = EncounterExperienceService.motesPerifericosPara(npc, essenciaFinal),
            historicoXpBatches = npc.historicoXpBatches + batch
        )
        val atualizado = EncounterExperienceService.recalcularDerivados(EncounterExperienceService.atualizarAlertasValidacao(npcAtualizado))
        return ExpansionResult(npcResultante = atualizado, batchAplicado = batch)
    }

    fun reduce(npc: NpcEncontro): NpcEncontro {
        val ultimoLote = npc.historicoXpBatches.lastOrNull() ?: return npc
        val charmsRestantes = EncounterExperienceService.removerEncantosDoLote(npc.charms, ultimoLote.nomesEncantosAdicionados)
        val abilitiesRevertidas = npc.abilities.toMutableMap()
        if (ultimoLote.habilidadeMelhorada != null && ultimoLote.pontosGanhosNaHabilidade > 0) {
            val atual = abilitiesRevertidas[ultimoLote.habilidadeMelhorada] ?: 0
            abilitiesRevertidas[ultimoLote.habilidadeMelhorada] = (atual - ultimoLote.pontosGanhosNaHabilidade).coerceAtLeast(0)
        }
        val especialidadesRevertidas = npc.especialidades.toMutableList()
        ultimoLote.especializacaoAdicionada?.let { habilidade ->
            especialidadesRevertidas.indexOfLast { it.habilidade == habilidade }.takeIf { it >= 0 }?.let(especialidadesRevertidas::removeAt)
        }
        val corpoDeTouroRestante = charmsRestantes.count { it.nome == NOME_CORPO_DE_TOURO }
        val xpGastoTotalNovo = (npc.xpGastoTotal - ultimoLote.xpGasto).coerceAtLeast(0)
        val essenciaNova = EncounterExperienceService.essenciaPara(npc, xpGastoTotalNovo)
        val vigor = npc.attributes["Vigor"] ?: 1
        val ehSangueDeDragao = npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO
        val healthBoxesBase = if (ehSangueDeDragao) EncounterGenerator.trilhaVitalidadeSangueDeDragaoPorVigor(vigor, corpoDeTouroRestante) else EncounterGenerator.trilhaVitalidadePorVigor(vigor, corpoDeTouroRestante)
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(npc.merits)
        val healthBoxesNovas = EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, healthBoxesBase)
        val healthFinal = EncounterExperienceService.recalcularVitalidadePreservandoDano(npc.healthBoxes, healthBoxesNovas)
        val forcaDeVontadeRevertida = (npc.forcaDeVontade - ultimoLote.pontosForcaDeVontadeComprados).coerceAtLeast(0)

        // xpAtual é o saldo disponível acumulado. Cada lote acrescenta
        // EncounterExperienceService.XP_POR_CHAMADA e consome uma parte desse valor; ao desfazê-lo,
        // removemos exatamente o saldo líquido acrescentado pelo lote.
        // Isso torna a operação reversível em qualquer profundidade: desfazer
        // o último lote restaura exatamente o estado imediatamente anterior.
        val xpLiquidoAdicionado = EncounterExperienceService.XP_POR_CHAMADA - ultimoLote.xpGasto
        val xpAtualRevertido = (npc.xpAtual - xpLiquidoAdicionado).coerceAtLeast(0)

        val npcAtualizado = npc.copy(
            charms = charmsRestantes, abilities = abilitiesRevertidas, especialidades = especialidadesRevertidas,
            corpoDeTouroCount = corpoDeTouroRestante, essencia = essenciaNova, xpAtual = xpAtualRevertido,
            xpGastoTotal = xpGastoTotalNovo, healthBoxes = healthFinal, forcaDeVontade = forcaDeVontadeRevertida,
            motesPersonais = EncounterExperienceService.motesPersonaisPara(npc, essenciaNova),
            motesPerifericos = EncounterExperienceService.motesPerifericosPara(npc, essenciaNova),
            historicoXpBatches = npc.historicoXpBatches.dropLast(1)
        )
        return EncounterExperienceService.recalcularDerivados(EncounterExperienceService.atualizarAlertasValidacao(npcAtualizado))
    }


    private fun elegivel(
        def: EncantoSolarDefinition,
        abilities: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogo: List<EncantoSolarDefinition>,
        contagemCategoriaSelecionada: ((String) -> Int)? = null
    ): Boolean = EncounterCharmSelectionService.elegivel(
        def, abilities, essencia, nomesSelecionados, catalogo, contagemCategoriaSelecionada
    )
}
