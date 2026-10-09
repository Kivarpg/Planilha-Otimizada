package com.example.data

/**
 * Busca limitada por rota para seleção de aquisições.
 *
 * IMPORTANTE:
 * - não interpreta texto de pré-requisito;
 * - não conta descendentes por substring;
 * - não corta o catálogo por posição arbitrária;
 * - legalidade continua pertencendo ao callback `elegivel`;
 * - não escolhe ações de combate.
 *
 * Este adaptador mantém a API histórica para facilitar integração posterior.
 * O valor futuro é medido por mudanças REAIS de elegibilidade.
 */
object EncounterCharmRouteOptimizer {
    private const val DEFAULT_BEAM_WIDTH = 6
    internal const val DEFAULT_MAX_WORK_UNITS = 100_000

    /** Estado completo observado pelo callback de legalidade. */
    data class EligibilityKey(
        val selecionados: Set<String>,
        val contagens: Map<String, Int>
    )

    /**
     * Chave mecânica compacta do mesmo estado observado por [EligibilityKey].
     * BitSet possui equals/hashCode por conteúdo; as contagens são serializadas
     * em ordem canônica de categoria. Nenhuma regra de elegibilidade é movida
     * para esta representação: ela serve apenas para memo/deduplicação.
     */
    internal data class CompactEligibilityKey(
        val selectedBits: java.util.BitSet,
        val categoryCounts: List<Int>,
        /** Categorias dinâmicas não previstas no catálogo estático continuam na chave. */
        val overflowCounts: List<Pair<String, Int>>
    )

    /**
     * Classificação textual de uma definição é imutável. Geração, XP e roadmap
     * reutilizam as mesmas instâncias dos catálogos, portanto recalcular texto +
     * regex de sinergia em cada chamada do beam search só consome CPU. WeakHashMap
     * evita reter catálogos antigos quando o owner deixa de existir.
     */
    private data class PreparedSynergy(
        val combat: Set<EncounterCombatSynergy.Tag>,
        val social: Set<EncounterSocialSynergy.Tag>
    )
    private val preparedSynergyCache = java.util.WeakHashMap<Any, PreparedSynergy>()
    private val preparedSynergyLock = Any()

    private fun preparedSynergy(definition: Any): PreparedSynergy =
        synchronized(preparedSynergyLock) {
            preparedSynergyCache[definition] ?: run {
                val text = EncounterCombatSynergy.text(definition)
                PreparedSynergy(
                    combat = EncounterCombatSynergy.tags(text),
                    social = EncounterSocialSynergy.tags(text)
                ).also { preparedSynergyCache[definition] = it }
            }
        }
    private const val DEFAULT_DEPTH = 3

    /** Dados invariantes do catálogo preparados uma vez por seleção/geração. */
    internal class PreparedCatalog<D : Any> internal constructor(
        internal val catalogoPorNome: Map<String, D>,
        internal val combatTagsPorNome: Map<String, Set<EncounterCombatSynergy.Tag>>,
        internal val socialTagsPorNome: Map<String, Set<EncounterSocialSynergy.Tag>>,
        internal val categoryPorNome: Map<String, String>,
        internal val nameOrdinal: Map<String, Int>,
        internal val categoryOrdinal: Map<String, Int>,
        internal val normalizedCategoryPorNome: Map<String, String>,
        /** Afinidade é memoizada sob demanda: evita o custo O(N²) no início de cada seleção. */
        internal val combatPairAffinity: MutableMap<Pair<String, String>, Int> = java.util.concurrent.ConcurrentHashMap()
    ) {
        internal fun compactKey(
            selecionados: Set<String>,
            contagens: Map<String, Int>
        ): CompactEligibilityKey {
            val bits = java.util.BitSet(nameOrdinal.size)
            for (selected in selecionados) nameOrdinal[selected]?.let(bits::set)
            val counts = IntArray(categoryOrdinal.size)
            var overflow: ArrayList<Pair<String, Int>>? = null
            for ((category, count) in contagens) {
                val normalized = category.trim().lowercase()
                val ordinal = categoryOrdinal[normalized]
                if (ordinal != null) {
                    counts[ordinal] = count
                } else {
                    val extras = overflow ?: ArrayList<Pair<String, Int>>().also { overflow = it }
                    extras += normalized to count
                }
            }
            val overflowCounts = overflow?.apply { sortBy { it.first } } ?: emptyList()
            return CompactEligibilityKey(bits, counts.toList(), overflowCounts)
        }

        internal fun compactKeyAfter(
            parent: CompactEligibilityKey,
            selectedName: String,
            category: String
        ): CompactEligibilityKey {
            val bits = parent.selectedBits.clone() as java.util.BitSet
            nameOrdinal[selectedName]?.let(bits::set)
            val normalized = category.trim().lowercase()
            val ordinal = categoryOrdinal[normalized]
            if (ordinal != null) {
                val counts = parent.categoryCounts.toMutableList()
                counts[ordinal] = counts[ordinal] + 1
                return CompactEligibilityKey(bits, counts, parent.overflowCounts)
            }
            val overflow = ArrayList<Pair<String, Int>>(parent.overflowCounts.size + 1)
            var inserted = false
            for ((key, count) in parent.overflowCounts) {
                when {
                    key == normalized -> {
                        overflow += key to (count + 1)
                        inserted = true
                    }
                    !inserted && key > normalized -> {
                        overflow += normalized to 1
                        overflow += key to count
                        inserted = true
                    }
                    else -> overflow += key to count
                }
            }
            if (!inserted) overflow += normalized to 1
            return CompactEligibilityKey(bits, parent.categoryCounts, overflow)
        }
    }

    internal fun <D : Any> prepareCatalog(
        catalogoCompleto: List<D>,
        nome: (D) -> String,
        categoria: (D) -> String
    ): PreparedCatalog<D> {
        val byName = LinkedHashMap<String, D>((catalogoCompleto.size * 4 / 3) + 1)
        for (def in catalogoCompleto) byName[nome(def)] = def
        val combatTags = HashMap<String, Set<EncounterCombatSynergy.Tag>>((byName.size * 4 / 3) + 1)
        val socialTags = HashMap<String, Set<EncounterSocialSynergy.Tag>>((byName.size * 4 / 3) + 1)
        for ((name, def) in byName) {
            val preparedSynergy = preparedSynergy(def)
            combatTags[name] = preparedSynergy.combat
            socialTags[name] = preparedSynergy.social
        }
        // Não materializamos uma matriz N×N aqui. A maioria dos pares nunca é
        // visitada pelo beam e o prefetch de roadmap precisa iniciar rápido.
        // A afinidade pura é calculada apenas no primeiro acesso ao par.
        val categoryByName = byName.mapValues { (_, def) -> categoria(def) }
        val normalizedCategoryByName = categoryByName.mapValues { (_, value) -> value.trim().lowercase() }
        val normalizedCategories = normalizedCategoryByName.values
            .distinct()
            .sorted()
        return PreparedCatalog(
            catalogoPorNome = byName,
            combatTagsPorNome = combatTags,
            socialTagsPorNome = socialTags,
            categoryPorNome = categoryByName,
            normalizedCategoryPorNome = normalizedCategoryByName,
            nameOrdinal = byName.keys.sorted().withIndex().associate { it.value to it.index },
            categoryOrdinal = normalizedCategories.withIndex().associate { it.value to it.index }
        )
    }

    internal fun <D : Any> escolher(
        candidatos:List<D>,
        catalogoCompleto:List<D>,
        nomesSelecionados:Set<String>,
        contagensCategorias:Map<String,Int>,
        elegivel:(D,Set<String>,Map<String,Int>)->Boolean,
        nome:(D)->String,
        categoria:(D)->String,
        categoriaNoEstado:((D,Set<String>,Map<String,Int>)->String)? = null,
        custoXp:(D)->Int,
        permiteAquisicaoRepetida:(D)->Boolean = { false },
        beamWidth:Int = DEFAULT_BEAM_WIDTH,
        profundidade:Int = DEFAULT_DEPTH,
        metrics:EncounterCharmRouteMetrics? = null,
        martialStyleResolver: ((String) -> EstiloArteMarcialDefinition?)? = null,
        eligibilityMemo: MutableMap<EligibilityKey, Set<String>>? = null,
        compactEligibilityMemo: MutableMap<CompactEligibilityKey, Set<String>>? = null,
        preparedCatalog: PreparedCatalog<D>? = null,
        monotonicEligibility: Boolean = false,
        monotonicAffectedNames: ((D, Set<String>, Map<String, Int>, Map<String, Int>) -> Set<String>)? = null,
        maxWorkUnits: Int = DEFAULT_MAX_WORK_UNITS
    ):D? {
        if(candidatos.isEmpty()) return null
        metrics?.recordOptimizerCall()
        require(beamWidth>0) { "beamWidth deve ser positivo." }
        require(profundidade>0) { "profundidade deve ser positiva." }
        require(maxWorkUnits>0) { "maxWorkUnits deve ser positivo." }
        var workUnits = 0
        var budgetExhausted = false
        fun consumeWork(): Boolean {
            if (workUnits >= maxWorkUnits) {
                budgetExhausted = true
                return false
            }
            workUnits++
            return true
        }

        data class Estado<D>(
            val selecionados:Set<String>,
            val contagens:Map<String,Int>,
            val primeiro:D?,
            val score:Int,
            // Reutilizada por memo/deduplicação; evita reconstruir BitSet e
            // vetor de contagens depois que o mesmo estado já foi materializado.
            val compactKey:CompactEligibilityKey
        )

        fun cat(
            x:D,
            selecionados:Set<String>,
            contagens:Map<String,Int>
        )=(categoriaNoEstado?.invoke(x, selecionados, contagens) ?: categoria(x)).trim().lowercase()

        data class StateAfter(
            val selected:Set<String>,
            val counts:Map<String,Int>,
            val category:String
        )

        fun stateAfter(
            candidato:D,
            selecionados:Set<String>,
            contagens:Map<String,Int>
        ):StateAfter {
            val candidateName = nome(candidato)
            val selected = HashSet<String>((selecionados.size + 1) * 4 / 3 + 1).apply {
                addAll(selecionados)
                add(candidateName)
            }
            val c=cat(candidato, selecionados, contagens)
            val counts = HashMap<String, Int>((contagens.size + 1) * 4 / 3 + 1).apply {
                putAll(contagens)
                this[c]=(this[c]?:0)+1
            }
            return StateAfter(selected, counts, c)
        }

        /**
         * Conta somente aquisições cuja legalidade muda de false -> true.
         * Isso preserva AND/OR/Bridge/Archetype/regras específicas porque
         * a verdade vem do serviço de elegibilidade, não do texto.
         */
        val prepared = preparedCatalog ?: prepareCatalog(catalogoCompleto, nome, categoria)
        val catalogoPorNome=prepared.catalogoPorNome
        val combatTagsPorNome=prepared.combatTagsPorNome
        val socialTagsPorNome=prepared.socialTagsPorNome
        val categoryPorNome=prepared.categoryPorNome
        val normalizedCategoryPorNome=prepared.normalizedCategoryPorNome
        val combatPairAffinity=prepared.combatPairAffinity
        val selectedSocialTagsCache=HashMap<Set<String>,List<Set<EncounterSocialSynergy.Tag>>>()
        // Custo XP e Essência mínima não dependem do estado do beam, mas a maioria
        // do catálogo pode nunca ser pontuada. Memoize somente candidatos visitados
        // nesta chamada; custoXp continua isolado entre chamadas do otimizador.
        val staticScoreByName = HashMap<String, Int>()
        fun staticScore(def:D):Int = staticScoreByName.getOrPut(nome(def)) {
            val economia = if (custoXp(def) <= 8) 18 else 0
            economia - minEssenciaAproximada(def) * 2
        }

        val legacyEligibilityCache = eligibilityMemo
        val compactCache = compactEligibilityMemo ?: if (eligibilityMemo == null) HashMap() else null

        fun eligibleNames(selecionados:Set<String>,contagens:Map<String,Int>):Set<String> {
            val compactKey = compactCache?.let { prepared.compactKey(selecionados, contagens) }
            val legacyKey = if (legacyEligibilityCache != null) EligibilityKey(selecionados, contagens) else null
            (compactKey?.let { compactCache[it] } ?: legacyKey?.let { key -> legacyEligibilityCache?.get(key) })?.let {
                metrics?.recordEligibilityCacheHit()
                return it
            }
            val started = if (metrics != null) System.nanoTime() else 0L
            var checks = 0
            val computed = LinkedHashSet<String>()
            var avaliacaoCompleta = true
            for (def in catalogoCompleto) {
                val defName = nome(def)
                if (defName in selecionados && !permiteAquisicaoRepetida(def)) continue
                if (!consumeWork()) {
                    avaliacaoCompleta = false
                    break
                }
                checks++
                if (elegivel(def, selecionados, contagens)) computed += defName
            }
            // Nunca publique no memo um conjunto parcial produzido pelo teto de
            // trabalho. Uma chamada posterior possui orçamento novo e precisa
            // poder concluir a avaliação em vez de herdar um falso resultado.
            if (avaliacaoCompleta) {
                if (compactKey != null) compactCache!![compactKey] = computed
                if (legacyKey != null) legacyEligibilityCache!![legacyKey] = computed
            }
            metrics?.recordEligibilityCacheMiss()
            metrics?.recordEligibilityChecks(checks)
            if (metrics != null) metrics.recordEligibleNamesNanos(System.nanoTime() - started)
            return computed
        }

        data class UnlockCounts(val total: Int, val sameRoute: Int)

        fun unlockCounts(
            candidato:D,
            selecionados:Set<String>,
            contagens:Map<String,Int>,
            afterSelected:Set<String>,
            afterCounts:Map<String,Int>,
            before:Set<String> = eligibleNames(selecionados,contagens),
            derivedCompactKey:CompactEligibilityKey? = null,
            candidateCategory:String = cat(candidato, selecionados, contagens)
        ):UnlockCounts {
            val started = if (metrics != null) System.nanoTime() else 0L
            val afterCompactKey = compactCache?.let { derivedCompactKey ?: prepared.compactKey(afterSelected, afterCounts) }
            val afterLegacyKey = if (legacyEligibilityCache != null) EligibilityKey(afterSelected, afterCounts) else null
            val cachedAfter = afterCompactKey?.let { compactCache[it] }
                ?: afterLegacyKey?.let { key -> legacyEligibilityCache?.get(key) }
            val after = if (monotonicEligibility) {
                cachedAfter?.also { metrics?.recordEligibilityCacheHit() } ?: run {
                    val startedDelta = if (metrics != null) System.nanoTime() else 0L
                    val next = LinkedHashSet(before)
                    if (!permiteAquisicaoRepetida(candidato)) next.remove(nome(candidato))
                    var checks = 0
                    val affectedNames = monotonicAffectedNames
                        ?.invoke(candidato, selecionados, contagens, afterCounts)
                    val definitionsToCheck: Sequence<D> = if (affectedNames == null) {
                        catalogoCompleto.asSequence()
                    } else {
                        affectedNames.asSequence().mapNotNull(catalogoPorNome::get)
                    }
                    var avaliacaoCompleta = true
                    for (def in definitionsToCheck) {
                        val defName = nome(def)
                        if (defName in afterSelected && !permiteAquisicaoRepetida(def)) {
                            // O conjunto anterior pode conter o Encanto adquirido
                            // antes de ele entrar em afterSelected. Não o mantenha
                            // como elegível nem o recoloque no cache pós-compra.
                            next.remove(defName)
                            continue
                        }
                        if (defName in next) continue
                        if (!consumeWork()) {
                            avaliacaoCompleta = false
                            break
                        }
                        checks++
                        if (elegivel(def, afterSelected, afterCounts)) next += defName
                    }
                    if (avaliacaoCompleta) {
                        if (afterCompactKey != null) compactCache!![afterCompactKey] = next
                        if (afterLegacyKey != null) legacyEligibilityCache!![afterLegacyKey] = next
                    }
                    metrics?.recordEligibilityCacheMiss()
                    metrics?.recordEligibilityChecks(checks)
                    if (metrics != null) metrics.recordEligibleNamesNanos(System.nanoTime() - startedDelta)
                    next
                }
            } else {
                eligibleNames(afterSelected,afterCounts)
            }
            var total = 0
            var sameRoute = 0
            for (unlockedName in after) {
                if (unlockedName in before) continue
                val unlocked = catalogoPorNome.getValue(unlockedName)
                if (unlockedName in selecionados && !permiteAquisicaoRepetida(unlocked)) continue
                total++
                // Solar/Sangue de Dragão têm categoria invariável e já normalizada
                // no catálogo preparado. Lunar pode resolver a rota pelo estado,
                // portanto mantém o callback dinâmico quando categoriaNoEstado existe.
                val unlockedCategory = if (categoriaNoEstado == null) {
                    normalizedCategoryPorNome[unlockedName] ?: categoria(unlocked).trim().lowercase()
                } else {
                    cat(unlocked, afterSelected, afterCounts)
                }
                if (unlockedCategory == candidateCategory) sameRoute++
            }
            if (metrics != null) metrics.recordMarginalUnlock(System.nanoTime() - started)
            return UnlockCounts(total, sameRoute)
        }

        fun immediateScore(
            candidato:D,
            selecionados:Set<String>,
            contagens:Map<String,Int>,
            afterSelected:Set<String>,
            afterCounts:Map<String,Int>,
            eligibleBefore:Set<String>,
            afterCompactKey:CompactEligibilityKey,
            stateCategory:String
        ):Int {
            val candidateName=nome(candidato)
            val staticScore = staticScore(candidato)
            val focus=(contagens[stateCategory]?:0).coerceAtMost(3)*2
            // Uma única leitura do estado pós-aquisição produz os dois sinais.
            // Antes sameRouteUnlocks repetia chave/cache/varredura para o mesmo estado.
            val unlocks=unlockCounts(
                candidato, selecionados, contagens,
                afterSelected, afterCounts, eligibleBefore, afterCompactKey, stateCategory
            )

            val selectedSocialTags=selectedSocialTagsCache.getOrPut(selecionados) {
                val tags = ArrayList<Set<EncounterSocialSynergy.Tag>>(selecionados.size)
                for (selectedName in selecionados) {
                    socialTagsPorNome[selectedName]?.let(tags::add)
                }
                tags
            }
            val fallbackPrepared = if (candidateName !in catalogoPorNome) preparedSynergy(candidato) else null
            val candidateCombatTags=combatTagsPorNome[candidateName] ?: fallbackPrepared!!.combat
            val candidateSocialTags=socialTagsPorNome[candidateName] ?: fallbackPrepared!!.social
            // A categoria do candidato é invariável durante esta pontuação.
            // Resolva uma única vez em vez de repetir o callback para cada Encanto já selecionado.
            val candidateCategory = categoryPorNome[candidateName] ?: categoria(candidato)
            var combatAffinity = 0
            for (selectedName in selecionados) {
                val selectedTags = combatTagsPorNome[selectedName].orEmpty()
                if (EncounterCombatSynergy.categoriesCanSynergize(
                        candidateCategory, categoryPorNome[selectedName].orEmpty(),
                        martialStyleResolver = martialStyleResolver
                    )
                ) {
                    combatAffinity += combatPairAffinity.computeIfAbsent(
                        if (candidateName <= selectedName) candidateName to selectedName
                        else selectedName to candidateName
                    ) { EncounterCombatSynergy.pairAffinityTags(candidateCombatTags, selectedTags) }
                }
            }
            val affinity=combatAffinity +
                EncounterSocialSynergy.scoreTags(candidateSocialTags,selectedSocialTags)
            return staticScore + unlocks.total*14 + unlocks.sameRoute*6 + focus + affinity
        }

        val normalizedCounts = HashMap<String, Int>((contagensCategorias.size * 4 / 3) + 1)
        for ((category, count) in contagensCategorias) {
            val normalized = category.trim().lowercase()
            normalizedCounts[normalized] = (normalizedCounts[normalized] ?: 0) + count
        }
        var frontier:List<Estado<D>> = listOf(
            Estado<D>(
                nomesSelecionados, normalizedCounts, null, 0,
                prepared.compactKey(nomesSelecionados, normalizedCounts)
            )
        )

        for(level in 0 until profundidade) {
            if (budgetExhausted) break
            val expansions=mutableListOf<Estado<D>>()
            for(state in frontier) {
                // Primeiro passo respeita a shortlist do chamador; passos futuros
                // examinam o catálogo completo e deixam a legalidade filtrar.
                val pool=if(state.primeiro==null) candidatos else catalogoCompleto
                val eligibleNamesForState = eligibleNames(state.selecionados,state.contagens)
                data class CandidateEvaluation<D>(
                    val candidate:D,
                    val gain:Int,
                    val selected:Set<String>,
                    val counts:Map<String,Int>,
                    val compactKey:CompactEligibilityKey
                )
                val evaluationComparator =
                    compareByDescending<CandidateEvaluation<D>> { it.gain }
                        .thenBy { nome(it.candidate) }
                // O beam conserva somente os melhores N candidatos (N=6 por
                // padrão). Ordenar todos os elegíveis criava uma lista O(C)
                // e fazia O(C log C) comparações para descartar quase tudo.
                // Uma fila limitada mantém exatamente o mesmo top-N e desempate
                // com O(C log N), sem alterar score, orçamento ou ordem final.
                val worstFirst = java.util.PriorityQueue<CandidateEvaluation<D>>(
                    beamWidth,
                    evaluationComparator.reversed()
                )
                for (candidate in pool) {
                    val candidateName = nome(candidate)
                    if (candidateName !in eligibleNamesForState) continue
                    if (!consumeWork()) continue
                    // O estado pós-aquisição era montado uma vez para calcular
                    // marginalUnlocks e novamente ao expandir o beam. Ele é
                    // mecânico e idêntico nos dois usos, então calculamos uma vez.
                    val after=stateAfter(candidate,state.selecionados,state.contagens)
                    val selected=after.selected
                    val counts=after.counts
                    val category=after.category
                    val compactKey = prepared.compactKeyAfter(state.compactKey, candidateName, category)
                    val evaluation = CandidateEvaluation(
                        candidate = candidate,
                        gain = immediateScore(
                            candidate, state.selecionados, state.contagens,
                            selected, counts, eligibleNamesForState, compactKey, category
                        ),
                        selected = selected,
                        counts = counts,
                        compactKey = compactKey
                    )
                    if (worstFirst.size < beamWidth) {
                        worstFirst += evaluation
                    } else if (evaluationComparator.compare(evaluation, worstFirst.peek()) < 0) {
                        worstFirst.poll()
                        worstFirst += evaluation
                    }
                }
                val eligible = worstFirst.toMutableList().apply { sortWith(evaluationComparator) }

                for(evaluation in eligible) {
                    metrics?.recordCandidateEvaluated()
                    expansions+=Estado(
                        evaluation.selected,
                        evaluation.counts,
                        state.primeiro?:evaluation.candidate,
                        state.score+evaluation.gain,
                        evaluation.compactKey
                    )
                }
            }
            if(expansions.isEmpty()) break
            // Caminhos diferentes podem convergir ao mesmo estado mecânico.
            // Mantemos apenas a melhor representação antes do corte do beam;
            // isso não remove alternativas semanticamente distintas, pois a chave
            // contém todo o estado que o callback de elegibilidade observa.
            val deduplicated = LinkedHashMap<CompactEligibilityKey, Estado<D>>()
            for (candidateState in expansions) {
                val key = candidateState.compactKey
                val previous = deduplicated[key]
                if (previous == null || candidateState.score > previous.score ||
                    (candidateState.score == previous.score &&
                        (candidateState.primeiro?.let(nome) ?: "") < (previous.primeiro?.let(nome) ?: ""))
                ) {
                    if (previous != null) metrics?.recordStateDeduplicated()
                    deduplicated[key] = candidateState
                } else {
                    metrics?.recordStateDeduplicated()
                }
            }
            metrics?.recordStatesExpanded(expansions.size)
            // O beam conserva apenas beamWidth estados. Evite ordenar toda a
            // fronteira deduplicada: mantenha os N melhores com a mesma ordem
            // score desc / primeiro nome asc e ordene somente esses N no final.
            val stateComparator =
                compareByDescending<Estado<D>> { it.score }
                    .thenBy { it.primeiro?.let(nome) ?: "" }
            val worstStates = java.util.PriorityQueue<Estado<D>>(
                beamWidth,
                stateComparator.reversed()
            )
            for (candidateState in deduplicated.values) {
                if (worstStates.size < beamWidth) {
                    worstStates += candidateState
                } else if (stateComparator.compare(candidateState, worstStates.peek()) < 0) {
                    worstStates.poll()
                    worstStates += candidateState
                }
            }
            frontier=worstStates.toMutableList().apply { sortWith(stateComparator) }
        }

        if (budgetExhausted) metrics?.recordBudgetExhaustion()
        return frontier.maxWithOrNull(
            compareBy<Estado<D>> { it.score }
                .thenByDescending { it.primeiro?.let(nome) ?: "" }
        )?.primeiro
    }

    private fun <D : Any> minEssenciaAproximada(candidato:D):Int = when(candidato) {
        is EncantoSolarDefinition -> candidato.minEssencia
        is EncantoLunarDefinition -> candidato.minEssencia
        is EncantoSangueDeDragaoDefinition -> candidato.minEssencia
        else -> 0
    }
}
