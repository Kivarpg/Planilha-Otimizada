package com.example.data

import com.example.model.HistoricoXpBatch
import com.example.model.NOME_CORPO_DE_TOURO
import com.example.model.NpcEncontro
import com.example.model.ExaltedConstants

/**
 * Progressão de XP específica de Lunares (Aba 11).
 *
 * Extraído de EncounterExperienceService (refatoração de organização —
 * roteiro de refatoração agressiva, sem mudança de comportamento).
 * Isola a trilha Lunar (Atributos) da trilha Solar (Habilidades) para
 * reduzir o tamanho do objeto principal e melhorar legibilidade.
 *
 * Helpers compartilhados (essenciaPara, motes*, recalcularVitalidade*,
 * removerEncantosDoLote, atualizarAlertasValidacao, recalcularDerivados,
 * XP_POR_CHAMADA) permanecem em EncounterExperienceService e são
 * chamados daqui.
 */
internal object EncounterExperienceLunar {
    private fun normalizarContextoQuimera(npc: NpcEncontro): NpcEncontro {
        val possuiQuimera = npc.charms.any {
            it.nome.equals("Expressão da Alma da Quimera", ignoreCase = true)
        }
        if (!possuiQuimera || npc.formaEspiritualSecundaria.isNotBlank()) return npc
        val principal = SpiritualFormService.animalPorNome(npc.formaEspiritual) ?: return npc
        val secundaria = SpiritualFormService.selecionarSecundaria(
            principal, kotlin.random.Random(npc.id.hashCode())
        )
        val traits = (
            LunarSpiritShapeArchetypeTraits.forAnimal(principal) +
                LunarSpiritShapeArchetypeTraits.forAnimal(secundaria)
        ).map { it.name }.sorted()
        return npc.copy(
            formaEspiritualSecundaria = SpiritualFormService.exibir(secundaria),
            lunarPrimaryArchetypeTraits = LunarSpiritShapeArchetypeTraits.forAnimal(principal)
                .map { it.name }.sorted(),
            lunarArchetypeTraits = traits
        )
    }

    private fun spiritTraits(npc: NpcEncontro): Set<LunarSpiritTrait> {
        val persisted = npc.lunarArchetypeTraits.mapNotNull { name ->
            runCatching { LunarSpiritTrait.valueOf(name) }.getOrNull()
        }.toSet()
        if (persisted.isNotEmpty()) return persisted
        // Compatibilidade com NPCs antigos, anteriores à persistência da taxonomia.
        return (LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritual) +
            LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritualSecundaria)).toSet()
    }

    // Evolução Lunar usa árvores de Atributos, não de Habilidades.
    // Diferentemente do caminho Solar, não reserva XP para aumentar
    // Atributos ou Especialidades: o saldo é destinado à compra de Encantos.
    // A elegibilidade, as rotas de aquisição e o limite entre Vigor e a
    // única árvore ofensiva do arquétipo Físico são tratados aqui.
    fun expandLunar(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>
    ): NpcEncontro {
        val normalizado = normalizarContextoQuimera(npc)
        return expandLunarInternalResult(
            normalizado, catalogo, ordemAtributos,
            criarIndiceXpLunar(catalogo, ordemAtributos, spiritTraits(normalizado))
        ).npcResultante
    }

    fun expandLunarWithBatch(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>
    ): ExpansionResult {
        val normalizado = normalizarContextoQuimera(npc)
        return expandLunarInternalResult(
            normalizado, catalogo, ordemAtributos,
            criarIndiceXpLunar(catalogo, ordemAtributos, spiritTraits(normalizado))
        )
    }

    /**
     * Expansor preparado para roadmap/prefetch Lunar. Reutiliza o índice
     * enquanto a taxonomia da Forma Espiritual não muda; Quimera invalida
     * naturalmente a chave e força apenas a reconstrução necessária.
     */
    fun criarExpansorXpPreparado(
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributosInicial: List<String>
    ): EncounterXpExpander {
        var traitsCache: Set<LunarSpiritTrait>? = null
        var ordemCache: List<String>? = null
        var indiceCache: IndiceXpLunar? = null
        return EncounterXpExpander { npc ->
            val normalizado = normalizarContextoQuimera(npc)
            val traits = spiritTraits(normalizado)
            val ordem = EncounterRulePolicy.lunarAttributePriorityFor(normalizado)
                .ifEmpty { ordemAtributosInicial }
            val indice = if (indiceCache != null && traitsCache == traits && ordemCache == ordem) {
                indiceCache!!
            } else {
                criarIndiceXpLunar(catalogo, ordem, traits).also {
                    traitsCache = traits
                    ordemCache = ordem
                    indiceCache = it
                }
            }
            expandLunarInternalResult(normalizado, catalogo, ordem, indice)
        }
    }

    /**
     * Expande vários lotes Lunares usando um único índice imutável do catálogo.
     * Cada iteração continua sendo um lote XP independente no histórico.
     */
    fun expandLunarRepeated(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>,
        quantidade: Int
    ): NpcEncontro {
        if (quantidade <= 0) return npc
        var atual = normalizarContextoQuimera(npc)
        // A compra da Quimera pode alterar os traits entre lotes. Reutilize
        // apenas quando o conjunto efetivo de traits continuar igual.
        var traitsAnteriores: Set<LunarSpiritTrait>? = null
        var indiceAnterior: IndiceXpLunar? = null
        repeat(quantidade) {
            val traits = spiritTraits(atual)
            val indice = if (indiceAnterior != null && traitsAnteriores == traits) {
                indiceAnterior!!
            } else {
                criarIndiceXpLunar(catalogo, ordemAtributos, traits).also {
                    traitsAnteriores = traits
                    indiceAnterior = it
                }
            }
            atual = expandLunarInternalResult(atual, catalogo, ordemAtributos, indice).npcResultante
        }
        return atual
    }

    internal data class IndiceXpLunar(
        val ordemAtributos: List<String>,
        val routeContext: LunarCharmArchetypePolicy.RouteContext,
        val spiritTraits: Set<LunarSpiritTrait>
    )

    private fun criarIndiceXpLunar(
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>,
        traits: Set<LunarSpiritTrait>
    ): IndiceXpLunar {
        val routeContext = LunarCharmArchetypePolicy.prepare(catalogo, traits)
        val ordemAtributosUnica = (ordemAtributos + EncounterGenerationRules.ALL_ATTRIBUTES + "Universal")
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        return IndiceXpLunar(
            ordemAtributos = ordemAtributosUnica,
            routeContext = routeContext,
            spiritTraits = traits
        )
    }

    // Extraído do corpo de expandLunarInternal (mesmo padrão da extração
    // Solar em tentarComprarCharms — refatoração de organização, pedido
    // explícito do usuário, feito com cautela extra). Diferente do Solar,
    // Lunar não tem fase de fallback (100% do XP vai pra Encantos), então
    // aqui a função cobre o lote inteiro, não só uma fase.
    private fun custoXpEncantoLunar(castaOuFavorecidos: Set<String>, atributoAquisicao: String): Int =
        if (atributoAquisicao.lowercase() in castaOuFavorecidos || atributoAquisicao.equals("Universal", ignoreCase = true)) 8 else 10

    private data class ResultadoCompraCharmsLunar(
        val xpDisponivel: Int,
        val xpGastoTotal: Int,
        val corpoDeTouro: Int,
        val contagemAtributoMentalSelecionada: Int,
        val nomesCharmsNesteLote: List<String>,
        val formaEspiritualSecundaria: SpiritualFormService.Animal?,
        val spiritTraitsEfetivos: Set<LunarSpiritTrait>,
        val ataqueEscolhido: String?
    )

    private fun tentarComprarCharmsLunar(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        indice: IndiceXpLunar,
        xpDisponivelInicial: Int,
        xpGastoTotalInicial: Int,
        corpoDeTouroInicial: Int,
        contagemAtributoMentalInicial: Int,
        attributesAtuais: Map<String, Int>,
        charmsAtuais: MutableList<com.example.model.EncantoEncontro>,
        nomesSelecionados: MutableSet<String>,
        categoriasSelecionadas: MutableMap<String, Int>
    ): ResultadoCompraCharmsLunar {
        var xpDisponivel = xpDisponivelInicial
        var xpGastoTotal = xpGastoTotalInicial
        var corpoDeTouro = corpoDeTouroInicial
        var contagemAtributoMentalSelecionada = contagemAtributoMentalInicial
        val nomesCharmsNesteLote = mutableListOf<String>()
        // O conjunto de Casta/Favorecidas nao muda dentro de um lote de XP.
        // Construir uma vez evita alocacoes por rota avaliada, sem mudar custos.
        val castaOuFavorecidos = (npc.lunarAtributosCasta + npc.habilidadesFavorecidas)
            .mapTo(hashSetOf()) { it.lowercase() }
        var routeContextAtual = indice.routeContext
        var spiritTraitsEfetivos = indice.spiritTraits
        var formaSecundariaAdquirida: SpiritualFormService.Animal? = null

        // DIRETRIZ DE DISTRIBUIÇÃO DE ENCANTOS — Lunar usa Atributos como
        // Árvores. Completa blocos de 3 no mesmo Atributo antes de abrir outro
        // e aprofunda as três primeiras Árvores prioritárias antes de espalhar
        // a progressão.
        var tentativas = 0
        // Preservar a arvore ofensiva registrada na geracao inicial ao evoluir XP.
        // Para saves antigos, recorrer ao foco explicito e depois a contagem atual.
        val ofensivos = listOf("Força", "Destreza")
        val ataqueEscolhido = if (npc.arquetipo == com.example.model.ArquetipoEncontro.FISICO) {
            npc.lunarAtaqueEscolhido?.let { salvo -> ofensivos.firstOrNull { it.equals(salvo.trim(), ignoreCase = true) } }
                ?: npc.focoProgressaoExplicito?.let { foco -> ofensivos.firstOrNull { it.equals(foco.trim(), ignoreCase = true) } }
                ?: ofensivos.maxWithOrNull(
                    compareBy<String> { categoriasSelecionadas[it.lowercase()] ?: 0 }
                        .thenBy {
                            val posicao = indice.ordemAtributos.indexOfFirst { atributo ->
                                atributo.equals(it, ignoreCase = true)
                            }
                            if (posicao >= 0) -posicao else Int.MIN_VALUE
                        }
                )
        } else null
        val ataqueDescartado = ataqueEscolhido?.let { escolhido -> ofensivos.firstOrNull { it != escolhido } }
        fun vigorPermitido(contagens: Map<String, Int>): Boolean =
            ataqueEscolhido == null ||
                (contagens["vigor"] ?: 0) < (contagens[ataqueEscolhido.lowercase()] ?: 0) + 3
        // No Fisico, manter o ataque escolhido e Vigor no conjunto inicial de
        // arvores, mesmo quando a ordem geral prioriza outros atributos.
        // O restante segue a prioridade original e nao reintroduz o ataque descartado.
        val ramos = (if (ataqueEscolhido != null) {
            listOf("Vigor", ataqueEscolhido) + indice.ordemAtributos
        } else indice.ordemAtributos).distinctBy { it.lowercase() }.filter {
            ataqueEscolhido == null || !it.equals(ataqueDescartado, ignoreCase = true)
        }
        val arvoresAtivas = ramos.take(3).toMutableList()
        if (ramos.isEmpty()) return ResultadoCompraCharmsLunar(
            xpDisponivel, xpGastoTotal, corpoDeTouro, contagemAtributoMentalSelecionada,
            nomesCharmsNesteLote, formaSecundariaAdquirida, spiritTraitsEfetivos, ataqueEscolhido
        )

        // Cache por contexto e ramo: ordenar a árvore apenas uma vez por
        // configuração de Forma Espiritual, não a cada compra de Encanto.
        val candidatosPorRamo = mutableMapOf<String, List<EncantoLunarDefinition>>()
        // Atributos permanecem constantes neste lote. Reutilizar a viabilidade
        // estrutural da rota até que Quimera altere o contexto de aquisição.
        val rotaViavelPorRamo = mutableMapOf<Pair<String, String>, Boolean>()
        // Normalizar as chaves uma vez: o catálogo é indexado por atributo
        // e a procura não deve percorrer todas as gavetas a cada mudança de Essência.
        var encantosPorRamoNormalizado = routeContextAtual.charmsByAttribute
            .entries.associate { (atributo, encantos) -> atributo.lowercase() to encantos }
        // O mesmo Encanto pode aparecer em várias gavetas por rotas de
        // Arquétipo. Compartilhar a pontuação evita percorrer sua árvore
        // de descendentes novamente em cada gaveta.
        val alcancePorEncanto = mutableMapOf<String, Int>()
        val alcanceAtualPorEncanto = mutableMapOf<String, Int>()
        var essenciaPontuacaoCache: Int? = null
        // A ordenação por alcance é necessária apenas no Físico.
        // Nos demais arquétipos, não executar os cálculos de profundidade.
        val classificarPorProfundidade = ataqueEscolhido != null
        // Só o arquétipo Físico utiliza a classificação por profundidade.
        // Evitar índices e varreduras de catálogo para Social e Mental.
        fun definicoesPermitidas(): Map<String, EncantoLunarDefinition> =
            if (ataqueEscolhido != null) {
                // Recalcular após Quimera: a Forma pode liberar novas rotas.
                catalogo.filter { def ->
                    routeContextAtual.routesFor(def).any { route ->
                        !route.atributo.equals(ataqueDescartado, ignoreCase = true)
                    }
                }.associateBy { it.nome }
            } else emptyMap()
        var definicoesPorNome = definicoesPermitidas()
        // Uma rota alternativa do atributo ofensivo descartado não deve
        // inflar a pontuação de um Encanto que só pode ser comprado pelo outro.
        val atributosParaPontuacao = if (ataqueDescartado != null) {
            attributesAtuais.filterKeys { !it.equals(ataqueDescartado, ignoreCase = true) }
        } else attributesAtuais
        val limiteEssenciaCatalogo = if (classificarPorProfundidade) {
            catalogo.maxOfOrNull { it.minEssencia } ?: 0
        } else 0
        // O catálogo é estável durante o lote; sua Essência máxima é o teto
        // da classificação e evita reordenar por valores superiores a ele.
        fun candidatosOrdenados(atributoRamo: String, essenciaAtual: Int): List<EncantoLunarDefinition> {
            // Os demais arquétipos usam a ordem canônica: não precisam
            // recalcular Essência nem invalidar a ordenação a cada compra.
            val essenciaPontuacao = if (classificarPorProfundidade) {
                essenciaAtual.coerceAtMost(limiteEssenciaCatalogo)
            } else 0
            if (classificarPorProfundidade && essenciaPontuacaoCache != essenciaPontuacao) {
                // A Essência só aumenta durante o lote de XP: não há retorno
                // a patamares anteriores que justifique guardar cópias do mapa.
                // Invalidar apenas a classificação do patamar anterior.
                candidatosPorRamo.clear()
                alcanceAtualPorEncanto.clear()
                essenciaPontuacaoCache = essenciaPontuacao
            }
            return candidatosPorRamo.getOrPut(atributoRamo.lowercase()) {
                val encantos = encantosPorRamoNormalizado[atributoRamo.lowercase()].orEmpty()
                // A nova estratégia de profundidade é exclusiva do arquétipo
                // Físico. Social e Mental mantêm a ordem canônica anterior.
                if (!classificarPorProfundidade) return@getOrPut encantos
                // Calcular alcance uma vez por Encanto, não a cada comparação
                // do sort (que pode comparar o mesmo Encanto repetidamente).
                // Quando a Essência atual já alcançou o teto do catálogo,
                // os dois critérios de alcance são idênticos. Compartilhar
                // o resultado evita uma segunda travessia dos descendentes.
                val alcanceNoTeto = essenciaPontuacao >= limiteEssenciaCatalogo
                encantos.forEach { def ->
                    val alcanceMaximo = alcancePorEncanto.getOrPut(def.nome) {
                        // Contar somente descendentes cujos mínimos de
                        // Essência e Atributo podem ser atingidos com a ficha.
                        routeContextAtual.reachableDependentCountWithinLimits(
                            def.nome, definicoesPorNome, atributosParaPontuacao, limiteEssenciaCatalogo
                        )
                    }
                    alcanceAtualPorEncanto.getOrPut(def.nome) {
                        if (alcanceNoTeto) alcanceMaximo
                        else routeContextAtual.reachableDependentCountWithinLimits(
                            def.nome, definicoesPorNome, atributosParaPontuacao, essenciaPontuacao
                        )
                    }
                }
                encantos.sortedWith(
                    compareByDescending<EncantoLunarDefinition> { alcanceAtualPorEncanto[it.nome] ?: 0 }
                        .thenByDescending { alcancePorEncanto[it.nome] ?: 0 }
                        .thenBy { it.minEssencia }.thenBy { it.nome }
                )
            }
        }

        fun comprarBloco(atributoRamo: String): Boolean {
            if (ataqueDescartado != null && atributoRamo.equals(ataqueDescartado, ignoreCase = true)) return false
            var progresso = false
            // A chave da categoria nao muda durante as tentativas deste bloco.
            val chaveCategoriaRamo = atributoRamo.lowercase()
            val alvo = (categoriasSelecionadas[chaveCategoriaRamo] ?: 0) + 3
            // O custo do atributo é constante enquanto o lote é processado.
            // Evitar novas consultas a cada compra ou rota candidata.
            val custoRamo = custoXpEncantoLunar(castaOuFavorecidos, atributoRamo)
            // Esta restrição depende somente do ramo, não do Encanto.
            val ramoEhVigor = atributoRamo.equals("Vigor", ignoreCase = true)
            // Os atributos não são alterados por compras de Encantos neste lote.
            val limiteCorpoDeTouro = (attributesAtuais["Vigor"] ?: 0).coerceAtLeast(0)
            var tentativasBloco = 0
            while (
                xpDisponivel >= custoRamo &&
                (categoriasSelecionadas[chaveCategoriaRamo] ?: 0) < alvo &&
                tentativasBloco++ < 100
            ) {
                // O limite de Vigor pode mudar após cada compra; verificar
                // uma única vez antes de ordenar ou avaliar candidatos.
                if (ataqueEscolhido != null && ramoEhVigor &&
                    !vigorPermitido(categoriasSelecionadas)
                ) break
                val essenciaAtual = EncounterExperienceService.essenciaPara(npc, xpGastoTotal)
                var candidato: EncantoLunarDefinition? = null
                var rotaEscolhida: LunarCharmArchetypePolicy.AcquisitionRoute? = null
                // O catálogo não muda durante esta tentativa: consultar uma vez
                // a lista de candidatos e o índice de rotas por Encanto.
                val candidatosDoRamo = candidatosOrdenados(atributoRamo, essenciaAtual)
                for (def in candidatosDoRamo) {
                    if (def.minEssencia > essenciaAtual) continue
                    if (def.nome in nomesSelecionados && !(def.nome == NOME_CORPO_DE_TOURO && corpoDeTouro < limiteCorpoDeTouro)) continue
                    // Um Encanto pode estar indexado por rotas alternativas.
                    // Descartar antecipadamente as rotas do ramo cujo mínimo
                    // de Atributo não pode ser satisfeito por este NPC.
                    val chaveRota = atributoRamo.lowercase() to def.nome
                    if (!rotaViavelPorRamo.getOrPut(chaveRota) {
                            routeContextAtual.routesFor(def).any { route ->
                                route.atributo.equals(atributoRamo, ignoreCase = true) &&
                                    (attributesAtuais[route.atributo] ?: 0) >= route.minAtributo
                            }
                        }) continue
                    val rotasElegiveis = LunarCharmArchetypePolicy.eligibleRoutes(
                        def, routeContextAtual, attributesAtuais, essenciaAtual, nomesSelecionados, catalogo,
                        contagemCategoriaSelecionada = { categoria ->
                            if (categoria.equals("Atributo Mental", ignoreCase = true)) {
                                contagemAtributoMentalSelecionada
                            } else {
                                categoriasSelecionadas[categoria.lowercase()] ?: 0
                            }
                        }
                    )
                    val rota = rotasElegiveis.firstOrNull { route ->
                        route.atributo.equals(atributoRamo, ignoreCase = true)
                    } ?: continue
                    candidato = def
                    rotaEscolhida = rota
                    break
                }
                val candidatoSelecionado = candidato ?: break
                val atributoAquisicao = rotaEscolhida?.atributo ?: candidatoSelecionado.atributo
                xpDisponivel -= custoRamo
                xpGastoTotal += custoRamo
                charmsAtuais += com.example.model.EncantoEncontro(candidatoSelecionado.nome, atributoAquisicao, candidatoSelecionado.custo)
                nomesSelecionados += candidatoSelecionado.nome
                nomesCharmsNesteLote += candidatoSelecionado.nome

                // Quimera muda a legalidade de Arquétipos imediatamente. A segunda
                // Forma não pode ser adiada até o fim do lote, pois as compras
                // seguintes deste mesmo lote já tratam ambos os animais como
                // Formas Espirituais.
                if (
                    candidatoSelecionado.nome.equals("Expressão da Alma da Quimera", ignoreCase = true) &&
                    npc.formaEspiritualSecundaria.isBlank() &&
                    formaSecundariaAdquirida == null
                ) {
                    val principal = SpiritualFormService.animalPorNome(npc.formaEspiritual)
                    formaSecundariaAdquirida = principal?.let {
                        SpiritualFormService.selecionarSecundaria(it, kotlin.random.Random(npc.id.hashCode()))
                    }
                    formaSecundariaAdquirida?.let { secundaria ->
                        spiritTraitsEfetivos = spiritTraitsEfetivos +
                            LunarSpiritShapeArchetypeTraits.forAnimal(secundaria)
                        routeContextAtual = LunarCharmArchetypePolicy.prepare(catalogo, spiritTraitsEfetivos)
                        encantosPorRamoNormalizado = routeContextAtual.charmsByAttribute
                            .entries.associate { (atributo, encantos) -> atributo.lowercase() to encantos }
                        candidatosPorRamo.clear() // novas rotas após Quimera
                        rotaViavelPorRamo.clear() // Quimera pode abrir rotas antes inviáveis
                        alcancePorEncanto.clear() // dependências podem mudar
                        alcanceAtualPorEncanto.clear()
                        definicoesPorNome = definicoesPermitidas()
                    }
                }

                val categoriaCandidato = atributoAquisicao.lowercase()
                categoriasSelecionadas[categoriaCandidato] = (categoriasSelecionadas[categoriaCandidato] ?: 0) + 1
                if (atributoAquisicao in ExaltedConstants.MENTAL_ATTRIBUTES) contagemAtributoMentalSelecionada++
                if (candidatoSelecionado.nome == NOME_CORPO_DE_TOURO) corpoDeTouro++
                progresso = true
            }
            return progresso
        }

        while (tentativas++ < 500 && xpDisponivel >= CUSTO_XP_ENCANTO_LUNAR_FAVORECIDO) {
            var progressoRodada = false
            // Nenhuma arvore e adicionada durante esta iteracao: evitar
            // copiar a lista a cada rodada do expansor Lunar.
            for (atributo in arvoresAtivas) {
                if (xpDisponivel < CUSTO_XP_ENCANTO_LUNAR_FAVORECIDO) break
                if (comprarBloco(atributo)) progressoRodada = true
            }
            if (progressoRodada) continue
            val proximaArvore = ramos.firstOrNull { ramo -> arvoresAtivas.none { it.equals(ramo, ignoreCase = true) } } ?: break
            arvoresAtivas += proximaArvore
            comprarBloco(proximaArvore)
        }
        return ResultadoCompraCharmsLunar(
            xpDisponivel, xpGastoTotal, corpoDeTouro, contagemAtributoMentalSelecionada,
            nomesCharmsNesteLote, formaSecundariaAdquirida, spiritTraitsEfetivos, ataqueEscolhido
        )
    }

    private fun expandLunarInternalResult(
        npc: NpcEncontro,
        catalogo: List<EncantoLunarDefinition>,
        ordemAtributos: List<String>,
        indice: IndiceXpLunar
    ): ExpansionResult {
        var xpDisponivel = npc.xpAtual + EncounterExperienceService.XP_POR_CHAMADA
        var xpGastoTotal = npc.xpGastoTotal
        val charmsAtuais = npc.charms.toMutableList()
        val nomesSelecionados = charmsAtuais.map { it.nome }.toMutableSet()
        val categoriasSelecionadas = charmsAtuais
            .groupingBy { it.habilidadeVinculada.lowercase() }
            .eachCount()
            .toMutableMap()
        // Índice especializado para o requisito "Atributo Mental". Esse
        // requisito é consultado repetidamente durante a expansão; manter o
        // total incremental elimina um sumOf dos três Atributos Mentais para
        // cada candidato testado.
        val contagemAtributoMentalInicial = ExaltedConstants.MENTAL_ATTRIBUTES.sumOf { atributo ->
            categoriasSelecionadas[atributo.lowercase()] ?: 0
        }
        var corpoDeTouro = npc.corpoDeTouroCount
        val attributesAtualizados = npc.attributes.toMutableMap()
        val especialidadesAtualizadas = npc.especialidades.toMutableList()
        var atributoMelhoradoNesteLote: String? = null
        var pontosGanhosNesteLote = 0
        var especializacaoAdicionadaNesteLote: String? = null

        // Pedido explícito do usuário: Lunar NÃO reserva XP pra
        // melhorar Atributo/Especialização — 100% do XP disponível vai
        // pra Encantos. atributoMelhoradoNesteLote/pontosGanhosNesteLote/
        // especializacaoAdicionadaNesteLote ficam nos valores padrão
        // (null/0), então o histórico deste lote simplesmente não
        // registra nenhuma melhoria de Atributo — só Encantos.
        val resultadoCompra = tentarComprarCharmsLunar(
            npc, catalogo, indice, xpDisponivel, xpGastoTotal, corpoDeTouro,
            contagemAtributoMentalInicial, attributesAtualizados,
            charmsAtuais, nomesSelecionados, categoriasSelecionadas
        )
        xpDisponivel = resultadoCompra.xpDisponivel
        xpGastoTotal = resultadoCompra.xpGastoTotal
        corpoDeTouro = resultadoCompra.corpoDeTouro
        val nomesCharmsNesteLote = resultadoCompra.nomesCharmsNesteLote

        val essenciaFinal = EncounterExperienceService.essenciaPara(npc, xpGastoTotal)
        val vigor = attributesAtualizados["Vigor"] ?: 1
        val healthBoxesBase = LunarEncounterGenerator.trilhaVitalidadePorVigor(vigor, corpoDeTouro)
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(npc.merits)
        val healthBoxesAtualizadas = EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, healthBoxesBase)
        val healthFinal = EncounterExperienceService.recalcularVitalidadePreservandoDano(npc.healthBoxes, healthBoxesAtualizadas)

        val batch = HistoricoXpBatch(
            xpGasto = xpGastoTotal - npc.xpGastoTotal,
            nomesEncantosAdicionados = nomesCharmsNesteLote,
            habilidadeMelhorada = atributoMelhoradoNesteLote,
            pontosGanhosNaHabilidade = pontosGanhosNesteLote,
            especializacaoAdicionada = especializacaoAdicionadaNesteLote,
            pontosForcaDeVontadeComprados = 0
        )
        val novaFormaSecundaria = resultadoCompra.formaEspiritualSecundaria
        val secundaria = novaFormaSecundaria?.let { SpiritualFormService.exibir(it) }
            ?: npc.formaEspiritualSecundaria
        val traitsEfetivosFinais = resultadoCompra.spiritTraitsEfetivos
        val npcAtualizado = npc.copy(
            charms = charmsAtuais, attributes = attributesAtualizados, especialidades = especialidadesAtualizadas,
            corpoDeTouroCount = corpoDeTouro, essencia = essenciaFinal, xpAtual = xpDisponivel, xpGastoTotal = xpGastoTotal,
            healthBoxes = healthFinal, historicoXpBatches = npc.historicoXpBatches + batch,
            formaEspiritualSecundaria = secundaria,
            lunarArchetypeTraits = traitsEfetivosFinais.map { it.name }.sorted(),
            // Fixa a escolha tambem em fichas antigas sem o campo persistido.
            lunarAtaqueEscolhido = if (npc.arquetipo == com.example.model.ArquetipoEncontro.FISICO) {
                resultadoCompra.ataqueEscolhido
            } else null
        )
        val resultado = EncounterExperienceService.recalcularDerivados(EncounterExperienceService.atualizarAlertasValidacao(npcAtualizado))
        return ExpansionResult(npcResultante = resultado, batchAplicado = batch)
    }

    // Arquétipo usa o Atributo da rota de aquisição também para o desconto
    // de Casta/Favorecido: 8 XP favorecido, 10 XP não favorecido.
    private const val CUSTO_XP_ENCANTO_LUNAR_FAVORECIDO = 8

    // Contraparte de reduce() pra Lunar — reverte Atributos (não
    // Habilidades) e usa a trilha de vitalidade padrão.
    fun reduceLunar(npc: NpcEncontro): NpcEncontro {
        val ultimoLote = npc.historicoXpBatches.lastOrNull() ?: return npc
        val charmsRestantes = EncounterExperienceService.removerEncantosDoLote(npc.charms, ultimoLote.nomesEncantosAdicionados)
        val attributesRevertidos = npc.attributes.toMutableMap()
        if (ultimoLote.habilidadeMelhorada != null && ultimoLote.pontosGanhosNaHabilidade > 0) {
            val atual = attributesRevertidos[ultimoLote.habilidadeMelhorada] ?: 0
            attributesRevertidos[ultimoLote.habilidadeMelhorada] = (atual - ultimoLote.pontosGanhosNaHabilidade).coerceAtLeast(0)
        }
        val especialidadesRevertidas = npc.especialidades.toMutableList()
        ultimoLote.especializacaoAdicionada?.let { atributo ->
            especialidadesRevertidas.indexOfLast { it.habilidade == atributo }.takeIf { it >= 0 }?.let(especialidadesRevertidas::removeAt)
        }
        val corpoDeTouroRestante = charmsRestantes.count { it.nome == NOME_CORPO_DE_TOURO }
        val xpGastoTotalNovo = (npc.xpGastoTotal - ultimoLote.xpGasto).coerceAtLeast(0)
        val essenciaNova = EncounterExperienceService.essenciaPara(npc, xpGastoTotalNovo)
        val vigor = attributesRevertidos["Vigor"] ?: 1
        val healthBoxesBase = LunarEncounterGenerator.trilhaVitalidadePorVigor(vigor, corpoDeTouroRestante)
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(npc.merits)
        val healthBoxesNovas = EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, healthBoxesBase)
        val healthFinal = EncounterExperienceService.recalcularVitalidadePreservandoDano(npc.healthBoxes, healthBoxesNovas)

        val xpLiquidoAdicionado = EncounterExperienceService.XP_POR_CHAMADA - ultimoLote.xpGasto
        val xpAtualRevertido = (npc.xpAtual - xpLiquidoAdicionado).coerceAtLeast(0)

        val mantemQuimera = charmsRestantes.any {
            it.nome.equals("Expressão da Alma da Quimera", ignoreCase = true)
        }
        val secundariaRevertida = if (mantemQuimera) npc.formaEspiritualSecundaria else ""
        val traitsRevertidos = if (mantemQuimera) {
            npc.lunarArchetypeTraits
        } else {
            npc.lunarPrimaryArchetypeTraits.ifEmpty {
                LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritual).map { it.name }.sorted()
            }
        }
        val npcAtualizado = npc.copy(
            // A reversao de XP nao desfaz a escolha da arvore ofensiva feita na geracao.
            lunarAtaqueEscolhido = npc.lunarAtaqueEscolhido,
            charms = charmsRestantes, attributes = attributesRevertidos, especialidades = especialidadesRevertidas,
            corpoDeTouroCount = corpoDeTouroRestante, essencia = essenciaNova, xpAtual = xpAtualRevertido,
            xpGastoTotal = xpGastoTotalNovo, healthBoxes = healthFinal,
            historicoXpBatches = npc.historicoXpBatches.dropLast(1),
            formaEspiritualSecundaria = secundariaRevertida,
            lunarArchetypeTraits = traitsRevertidos
        )
        return EncounterExperienceService.recalcularDerivados(EncounterExperienceService.atualizarAlertasValidacao(npcAtualizado))
    }

}
