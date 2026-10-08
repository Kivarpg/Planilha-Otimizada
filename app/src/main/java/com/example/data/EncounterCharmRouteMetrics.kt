package com.example.data

/**
 * Instrumentação transitória da busca de rota. Não participa do score nem é
 * persistida; existe para detectar explosão combinatória e regressões de custo.
 *
 * A instância pertence a uma única seleção/geração e é atualizada no mesmo
 * fluxo do otimizador. Contadores atômicos aqui só acrescentavam barreiras de
 * memória ao hot path sem oferecer correção adicional.
 */
class EncounterCharmRouteMetrics {
    private var statesExpanded = 0L
    private var statesDeduplicated = 0L
    private var candidatesEvaluated = 0L
    private var eligibilityCacheHits = 0L
    private var eligibilityCacheMisses = 0L
    private var eligibilityChecks = 0L
    private var eligibleNamesNanos = 0L
    private var marginalUnlockCalls = 0L
    private var marginalUnlockNanos = 0L
    private var optimizerCalls = 0L
    private var budgetExhaustions = 0L

    data class Snapshot(
        val statesExpanded: Long,
        val statesDeduplicated: Long,
        val candidatesEvaluated: Long,
        val eligibilityCacheHits: Long,
        val eligibilityCacheMisses: Long,
        val eligibilityChecks: Long,
        val eligibleNamesNanos: Long,
        val marginalUnlockCalls: Long,
        val marginalUnlockNanos: Long,
        val optimizerCalls: Long,
        val budgetExhaustions: Long
    ) {
        val eligibilityCacheHitRate: Double
            get() {
                val total = eligibilityCacheHits + eligibilityCacheMisses
                return if (total == 0L) 0.0 else eligibilityCacheHits.toDouble() / total.toDouble()
            }

        /** Contadores derivados são determinísticos e apropriados para regressão em CI. */
        val eligibilityCacheRequests: Long
            get() = eligibilityCacheHits + eligibilityCacheMisses

        val eligibilityCacheHitPermille: Int
            get() = if (eligibilityCacheRequests == 0L) 0
            else ((eligibilityCacheHits * 1000L) / eligibilityCacheRequests).toInt()

        /**
         * Unidade sintética de trabalho: não usa relógio e não participa do score.
         * Mantém cada operação visível separadamente no snapshot, mas fornece um
         * número compacto para comparar seeds/baselines durante profiling.
         */
        val deterministicWorkUnits: Long
            get() = eligibilityChecks + candidatesEvaluated + statesExpanded +
                statesDeduplicated + marginalUnlockCalls + optimizerCalls

        fun diagnosticLine(): String =
            "optimizerCalls=$optimizerCalls statesExpanded=$statesExpanded " +
                "statesDeduplicated=$statesDeduplicated candidatesEvaluated=$candidatesEvaluated " +
                "eligibilityChecks=$eligibilityChecks cacheHits=$eligibilityCacheHits " +
                "cacheMisses=$eligibilityCacheMisses cacheHitPermille=$eligibilityCacheHitPermille " +
                "marginalUnlockCalls=$marginalUnlockCalls budgetExhaustions=$budgetExhaustions " +
                "workUnits=$deterministicWorkUnits"
    }

    internal fun recordStatesExpanded(count: Int) { statesExpanded += count.toLong().coerceAtLeast(0L) }
    internal fun recordStateDeduplicated() { statesDeduplicated++ }
    internal fun recordCandidateEvaluated() { candidatesEvaluated++ }
    internal fun recordEligibilityCacheHit() { eligibilityCacheHits++ }
    internal fun recordEligibilityCacheMiss() { eligibilityCacheMisses++ }
    internal fun recordEligibilityChecks(count: Int) { eligibilityChecks += count.toLong().coerceAtLeast(0L) }
    internal fun recordEligibleNamesNanos(nanos: Long) { eligibleNamesNanos += nanos.coerceAtLeast(0L) }
    internal fun recordMarginalUnlock(nanos: Long) {
        marginalUnlockCalls++
        marginalUnlockNanos += nanos.coerceAtLeast(0L)
    }
    internal fun recordOptimizerCall() { optimizerCalls++ }
    internal fun recordBudgetExhaustion() { budgetExhaustions++ }

    fun snapshot() = Snapshot(
        statesExpanded, statesDeduplicated, candidatesEvaluated,
        eligibilityCacheHits, eligibilityCacheMisses,
        eligibilityChecks, eligibleNamesNanos,
        marginalUnlockCalls, marginalUnlockNanos, optimizerCalls, budgetExhaustions
    )
}
