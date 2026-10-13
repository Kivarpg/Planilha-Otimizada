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
        val totalScore: Int,
        val candidateEvaluations: Int = 0,
        val budgetExhausted: Boolean = false
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
        val initiallyAvailable = hashSetOf<PreparedEncounterCatalog.StableContentId>()

        var candidateEvaluations = 0
        var budgetExhausted = false
        for (depth in 0 until maxSteps.coerceAtMost(32)) {
            if (budgetExhausted) break
            // Score can depend on acquisition order. Deduplicate only after scoring,
            // retaining the best path for the complete mechanical state.
            val nextByBuild = LinkedHashMap<EncounterRulesEngine.BuildState, SearchState>()
            expansion@ for (state in frontier) {
                for ((id, candidate) in candidateEntries) {
                    if (id in state.build.acquiredCharmIds) continue
                    if (candidateEvaluations >= maxCandidateEvaluations) {
                        budgetExhausted = true
                        break@expansion
                    }
                    candidateEvaluations++
                    val eligibility = EncounterRulesEngine.evaluate(graph, id, state.build)
                        as? EncounterRulesEngine.Eligibility.Available ?: continue
                    if (depth == 0) initiallyAvailable += id
                    val node = graph.node(id) ?: continue
                    val route = eligibility.routes.minByOrNull { it.routeId } ?: continue
                    val quality = EncounterBuildQuality.combine(
                        current = candidate.score(state.build, node),
                        future = candidate.futureScore(state.build, node)
                    )
                    val currentValue = quality.current
                    val futureValue = quality.future
                    val value = quality.total
                    val nextBuild = EncounterRulesEngine.afterAcquisition(state.build, id, catalog)
                    val step = PlannedAcquisition(
                        id = id,
                        routeId = route.routeId,
                        score = value,
                        unlockedByPreviousSteps = state.steps.isNotEmpty() && id !in initiallyAvailable,
                        currentValue = currentValue,
                        futureValue = futureValue
                    )
                    val nextTieKey = if (state.tieKey.isEmpty()) id.value else state.tieKey + "|" + id.value
                    val proposed = SearchState(nextBuild, state.steps + step, state.score + value, nextTieKey)
                    val previous = nextByBuild[nextBuild]
                    if (previous == null || proposed.score > previous.score ||
                        (proposed.score == previous.score && proposed.tieKey < previous.tieKey)) {
                        nextByBuild[nextBuild] = proposed
                    }
                }
            }
            val next = nextByBuild.values
            if (next.isEmpty()) break
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
        return Plan(fingerprint, best.steps, best.score, candidateEvaluations, budgetExhausted)
    }
}
