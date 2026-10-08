package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCombinationEvaluatorOptimizationTest {
    @Test
    fun `caminhos estrutural e mecanicamente identicos sao deduplicados durante expansao`() {
        val effect = EncounterMechanicalEffect(
            id = "same",
            mechanic = "same",
            relation = EncounterMechanicalEffect.Relation.PRODUCES,
            lifetime = EncounterMechanicalEffect.EffectLifetime.Instant
        )
        val duplicated = EncounterCombinationEvaluator.Power(
            id = "p",
            variants = listOf(
                EncounterCombinationEvaluator.PowerVariant(
                    id = "v",
                    branches = listOf(
                        EncounterCombinationEvaluator.EffectBranch("b1", effects = listOf(effect)),
                        EncounterCombinationEvaluator.EffectBranch("b2", effects = listOf(effect))
                    )
                )
            )
        )
        val result = EncounterCombinationEvaluator.realize(
            listOf(duplicated), EncounterCombinationEvaluator.Configuration()
        )
        assertTrue(result.realizable)
        // Ramos distintos continuam distintos porque fazem parte da identidade mecânica.
        assertEquals(2, result.realizations.size)
    }
}
