package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals

class EncounterSelectionTelemetryTest {
    @Test
    fun `telemetria observa fallback sem participar da selecao`() {
        EncounterSelectionTelemetry.resetForTests()
        EncounterSelectionTelemetry.recordExactSearchBudgetExhausted()
        EncounterSelectionTelemetry.recordCompletionFallbackActivated()

        val snapshot = EncounterSelectionTelemetry.snapshot()
        assertEquals(1L, snapshot.exactSearchBudgetExhausted)
        assertEquals(1L, snapshot.completionFallbackActivated)
    }

    @Test
    fun `exact search outcomes are counted independently`() {
        EncounterSelectionTelemetry.resetForTests()
        EncounterSelectionTelemetry.recordExactSearchFound()
        EncounterSelectionTelemetry.recordExactSearchProvenImpossible()
        EncounterSelectionTelemetry.recordExactSearchBudgetExhausted()
        val snapshot = EncounterSelectionTelemetry.snapshot()
        assertEquals(1L, snapshot.exactSearchFound)
        assertEquals(1L, snapshot.exactSearchProvenImpossible)
        assertEquals(1L, snapshot.exactSearchBudgetExhausted)
        EncounterSelectionTelemetry.resetForTests()
    }

}
