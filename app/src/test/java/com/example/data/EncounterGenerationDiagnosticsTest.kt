package com.example.data

import org.junit.Assert.*
import org.junit.Test

class EncounterGenerationDiagnosticsTest {
    @Test fun `trabalho deterministico independe dos nanos de profiling`() {
        fun snapshot(nanos: Long) = EncounterCharmRouteMetrics.Snapshot(
            statesExpanded = 4, statesDeduplicated = 2, candidatesEvaluated = 7,
            eligibilityCacheHits = 8, eligibilityCacheMisses = 2, eligibilityChecks = 11,
            eligibleNamesNanos = nanos, marginalUnlockCalls = 3, marginalUnlockNanos = nanos * 2,
            optimizerCalls = 1, budgetExhaustions = 0
        )
        assertEquals(snapshot(10).deterministicWorkUnits, snapshot(999999).deterministicWorkUnits)
        assertEquals(800, snapshot(10).eligibilityCacheHitPermille)
        assertEquals(10, snapshot(10).eligibilityCacheRequests)
    }

    @Test fun `linha diagnostica e estavel e nao inclui tempo de parede`() {
        val route = EncounterCharmRouteMetrics.Snapshot(1, 2, 3, 4, 1, 5, 999, 6, 888, 1, 0)
        val line = route.diagnosticLine()
        assertTrue(line.contains("statesExpanded=1"))
        assertTrue(line.contains("eligibilityChecks=5"))
        assertTrue(line.contains("workUnits="))
        assertFalse(line.contains("Nanos"))
        assertFalse(line.contains("999"))
        assertFalse(line.contains("888"))
    }

    @Test fun `relatorio separa profiling de trabalho deterministico`() {
        val perf = EncounterPerformanceMetrics().also { it.recordGeneration(100) }.snapshot()
        val route = EncounterCharmRouteMetrics().also {
            it.recordOptimizerCall(); it.recordEligibilityChecks(9); it.recordCandidateEvaluated()
        }.snapshot()
        val report = EncounterGenerationDiagnostics.report(perf, route)
        assertEquals(1, report.generationCount)
        assertEquals(route.deterministicWorkUnits, report.deterministicRouteWorkUnits)
        assertEquals(2, report.diagnosticLines().size)
        assertTrue(report.diagnosticLines().first().contains("generationP50Nanos=100"))
        assertTrue(report.diagnosticLines().first().contains("generationP95Nanos=100"))
        assertTrue(report.diagnosticLines().first().contains("generationP99Nanos=100"))
    }
}
