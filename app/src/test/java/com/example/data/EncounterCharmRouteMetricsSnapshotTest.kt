package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncounterCharmRouteMetricsSnapshotTest {
    @Test
    fun `hit rate e apenas metrica derivada`() {
        val metrics = EncounterCharmRouteMetrics()
        metrics.recordEligibilityCacheHit()
        metrics.recordEligibilityCacheHit()
        metrics.recordEligibilityCacheMiss()
        metrics.recordEligibilityChecks(7)
        metrics.recordEligibleNamesNanos(11)
        metrics.recordMarginalUnlock(13)
        metrics.recordOptimizerCall()
        val snapshot = metrics.snapshot()
        assertEquals(2.0 / 3.0, snapshot.eligibilityCacheHitRate, 0.000001)
        assertEquals(7, snapshot.eligibilityChecks)
        assertEquals(11, snapshot.eligibleNamesNanos)
        assertEquals(1, snapshot.marginalUnlockCalls)
        assertEquals(13, snapshot.marginalUnlockNanos)
        assertEquals(1, snapshot.optimizerCalls)
        assertEquals(0, snapshot.budgetExhaustions)
        assertTrue(snapshot.diagnosticLine().contains("budgetExhaustions=0"))
    }
}
