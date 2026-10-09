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
    @Test
    fun `ids duplicados e vazios continuam rejeitados em todos os niveis`() {
        val branch = EncounterCombinationEvaluator.EffectBranch("b", effects = emptyList())
        val variant = EncounterCombinationEvaluator.PowerVariant("v", branches = listOf(branch))
        val power = EncounterCombinationEvaluator.Power("p", variants = listOf(variant))
        val cases = listOf(
            listOf(power, power) to "Poderes com ID vazio ou duplicado.",
            listOf(power.copy(id = "")) to "Poderes com ID vazio ou duplicado.",
            listOf(power.copy(variants = listOf(variant, variant))) to
                "Variantes com ID vazio ou duplicado em p.",
            listOf(power.copy(variants = listOf(variant.copy(id = "")))) to
                "Variantes com ID vazio ou duplicado em p.",
            listOf(power.copy(variants = listOf(variant.copy(branches = listOf(branch, branch))))) to
                "Ramos com ID vazio ou duplicado em p/v.",
            listOf(power.copy(variants = listOf(variant.copy(branches = listOf(branch.copy(id = "")))))) to
                "Ramos com ID vazio ou duplicado em p/v."
        )
        for ((powers, expectedReason) in cases) {
            val result = EncounterCombinationEvaluator.realize(powers, EncounterCombinationEvaluator.Configuration())
            assertEquals(expectedReason, result.reason)
            assertEquals(false, result.realizable)
        }
    }

    @Test
    fun `ids validos em niveis diferentes nao sao confundidos`() {
        val first = EncounterCombinationEvaluator.Power(
            id = "shared",
            variants = listOf(
                EncounterCombinationEvaluator.PowerVariant(
                    id = "shared",
                    branches = listOf(
                        EncounterCombinationEvaluator.EffectBranch("shared", effects = emptyList())
                    )
                )
            )
        )
        val second = EncounterCombinationEvaluator.Power(
            id = "second",
            variants = listOf(
                EncounterCombinationEvaluator.PowerVariant(
                    id = "shared",
                    branches = listOf(
                        EncounterCombinationEvaluator.EffectBranch("shared", effects = emptyList())
                    )
                )
            )
        )
        val result = EncounterCombinationEvaluator.realize(
            listOf(first, second),
            EncounterCombinationEvaluator.Configuration()
        )
        assertTrue(result.reason, result.realizable)
        assertEquals(1, result.realizations.size)
    }

}
