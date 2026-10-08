package com.example.data

import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterPostCycleConvergenceTest {
    @Test
    fun `empate automatico de feiticaria permanece na construcao pura`() {
        val pure = EncounterBuildQuality.combine(current = 8, future = 2)
        val sorcery = EncounterBuildQuality.combine(current = 8, future = 2)
        assertTrue(
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                archetype = ArquetipoEncontro.MENTAL,
                exploreSorcery = true,
                explicitSorceryFocus = false,
                sorceryConstructible = true
            )
        )
        assertFalse(
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = false,
                exploreSorcery = true,
                sorceryConstructible = true,
                pureQuality = pure,
                sorceryQuality = sorcery
            )
        )
    }

    @Test
    fun `bonus futuro permanece limitado no contrato global de qualidade`() {
        val score = EncounterBuildQuality.combine(current = 4, future = Int.MAX_VALUE)
        assertEquals(EncounterBuildQuality.MAX_FUTURE_GROWTH_BONUS, score.future)
        assertEquals(4 + EncounterBuildQuality.MAX_FUTURE_GROWTH_BONUS, score.total)
    }
}
