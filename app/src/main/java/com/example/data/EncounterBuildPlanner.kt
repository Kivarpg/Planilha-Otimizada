package com.example.data

/**
 * Planejador canônico de aquisições. Ele não compra nada e não substitui a
 * progressão de XP: produz uma sequência candidata sobre o mesmo RulesEngine.
 *
 * O score é injetado. Assim ECS continua dono de "qual opção é melhor", enquanto
 * RulesEngine é o único dono de "qual opção é legal".
 */
internal object EncounterBuildPlanner {
    internal const val DEFAULT_MAX_CANDIDATE_EVALUATIONS = 1_000_000
    data class PlannedAcquisition(
        val id: PreparedEncounterCatalog.StableContentId,
        val routeId: String,
        val score: Int,
        val unlockedByPreviousSteps: Boolean,
        val currentValue: Int = score,
        val futureValue: Int = 0
    )

    data class Plan(
        val fingerprint: String,
        val steps: List<PlannedAcquisition>,
        val totalScore: Int
    )

    data class Candidate(
        val id: PreparedEncounterCatalog.StableContentId,
        val futureScore: (EncounterRulesEngine.BuildState, EncounterRuleNode) -> Int = { _, _ -> 0 },
        val score: (EncounterRulesEngine.BuildState, EncounterRuleNode) -> Int
    )

    private data class SearchState(
        val build: EncounterRulesEngine.BuildState,
        val steps: List<PlannedAcquisition>,
        val score: Int,
        // Equivale a steps.joinToString("|") { it.id.value }, mas é mantida
        // incrementalmente para evitar reconstrução durante cada comparação.
        val tieKey: String
    )

    /**
     * Beam limitado: considera valor futuro sem explosão combinatória.
     * IDs tornam desempate determinístico; isso facilita regressão no GitHub.
     */
    fun plan(
        catalog: PreparedEncounterCatalog,
        initial: EncounterRulesEngine.BuildState,
        candidates: List<Candidate>,
        maxSteps: Int,
        beamWidth: Int = 24,
        fingerprint: String,
        maxCandidateEvaluations: Int = DEFAULT_MAX_CANDIDATE_EVALUATIONS
    ): Plan {
        if (maxSteps <= 0 || candidates.isEmpty()) return Plan(fingerprint, emptyList(), 0)
        require(maxCandidateEvaluations > 0) { "maxCandidateEvaluations deve ser positivo." }
        val graph = catalog.requirementGraph
        val candidateById = candidates.associateBy { it.id }
        val candidateEntries = candidateById.entries.toList()
        var frontier = listOf(SearchState(initial, emptyList(), 0, ""))
        var best = frontier.first()
        val initiallyAvailable = candidateEntries.asSequence()
            .map { it.key }
            .filter { it !in initial.acquiredCharmIds }
            .filter {
                EncounterRulesEngine.evaluate(graph, it, initial) is
                    EncounterRulesEngine.Eligibility.Available
            }.toSet()

        var candidateEvaluations = 0
        var budgetExhausted = false
        repeat(maxSteps.coerceAtMost(32)) {
            if (budgetExhausted) return@repeat
            val next = ArrayList<SearchState>()
            // distinctBy abaixo sempre preservava a primeira rota gerada para
            // um mesmo conjunto adquirido. Detectar esse conjunto antes do
            // score evita pontuar/materializar rotas que seriam descartadas.
            val seenAcquiredSets = HashSet<Set<PreparedEncounterCatalog.StableContentId>>()
            frontier.forEach { state ->
                if (budgetExhausted) return@forEach
                candidateEntries.asSequence()
                    .filter { (id, _) -> id !in state.build.acquiredCharmIds }
                    .mapNotNull { (id, candidate) ->
                        val eligibility = EncounterRulesEngine.evaluate(graph, id, state.build)
                            as? EncounterRulesEngine.Eligibility.Available
                        eligibility?.let { Triple(id, candidate, it) }
                    }
                    .forEach { (id, candidate, eligibility) ->
                    if (candidateEvaluations >= maxCandidateEvaluations) {
                        budgetExhausted = true
                        return@forEach
                    }
                    candidateEvaluations++
                    val nextAcquiredIds = state.build.acquiredCharmIds + id
                    if (!seenAcquiredSets.add(nextAcquiredIds)) return@forEach
                    val node = graph.node(id) ?: return@forEach
                    val route = eligibility.routes.minByOrNull { it.routeId } ?: return@forEach
                    val quality = EncounterBuildQuality.combine(
                        current = candidate.score(state.build, node),
                        future = candidate.futureScore(state.build, node)
                    )
                    val currentValue = quality.current
                    val futureValue = quality.future
                    val value = quality.total
                    val nextBuild = state.build.copy(
                        acquiredCharmIds = nextAcquiredIds,
                        acquiredCharmNames = catalog.find(id)?.nome?.let {
                            state.build.acquiredCharmNames + it
                        } ?: state.build.acquiredCharmNames
                    )
                    val step = PlannedAcquisition(
                        id = id,
                        routeId = route.routeId,
                        score = value,
                        unlockedByPreviousSteps = state.steps.isNotEmpty() && id !in initiallyAvailable,
                        currentValue = currentValue,
                        futureValue = futureValue
                    )
                    val nextTieKey = if (state.tieKey.isEmpty()) id.value else state.tieKey + "|" + id.value
                    next += SearchState(nextBuild, state.steps + step, state.score + value, nextTieKey)
                }
            }
            if (next.isEmpty()) return@repeat
            frontier = next
                .sortedWith(
                    compareByDescending<SearchState> { it.score }
                        .thenByDescending { it.steps.size }
                        .thenBy { it.tieKey }
                )
                .take(beamWidth.coerceIn(1, 256))
            val roundBest = frontier.first()
            if (roundBest.score > best.score ||
                (roundBest.score == best.score && roundBest.steps.size > best.steps.size)
            ) best = roundBest
        }
        return Plan(fingerprint, best.steps, best.score)
    }
}
