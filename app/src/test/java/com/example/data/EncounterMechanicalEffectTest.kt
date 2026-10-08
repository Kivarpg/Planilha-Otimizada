package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterMechanicalEffectTest {

    

    @Test
    fun `duracao do Charm nao promove efeito instantaneo a persistente`() {
        val effect = EncounterMechanicalEffect(
            id = "instant-crash",
            mechanic = "CRASH",
            relation = EncounterMechanicalEffect.Relation.PRODUCES,
            lifetime = EncounterMechanicalEffect.EffectLifetime.Instant
        )

        val result = EncounterEffectRealizability.supports(
            requiredMechanic = "CRASH",
            effects = listOf(effect),
            facts = emptySet(),
            requireCrossRound = true
        )

        assertFalse(result.supported)
    }

    @Test
    fun `efeito de cena sustenta dependencia entre rodadas`() {
        val effect = EncounterMechanicalEffect(
            id = "scene-state",
            mechanic = "STATE_X",
            relation = EncounterMechanicalEffect.Relation.PRODUCES,
            lifetime = EncounterMechanicalEffect.EffectLifetime.OneScene
        )

        val result = EncounterEffectRealizability.supports(
            requiredMechanic = "STATE_X",
            effects = listOf(effect),
            facts = emptySet(),
            requireCrossRound = true
        )

        assertTrue(result.supported)
        assertTrue(result.crossRound)
    }

    @Test
    fun `efeito condicionado a AND nao existe com requisito parcial`() {
        val requirement = EncounterRequirementExpression.AllOf(
            listOf(
                EncounterRequirementExpression.Fact("A"),
                EncounterRequirementExpression.Fact("B")
            )
        )
        val effect = EncounterMechanicalEffect(
            id = "conditional",
            mechanic = "X",
            relation = EncounterMechanicalEffect.Relation.PRODUCES,
            lifetime = EncounterMechanicalEffect.EffectLifetime.OneScene,
            requirement = requirement
        )

        val partial = EncounterEffectRealizability.supports(
            "X", listOf(effect), setOf("A"), false
        )
        val complete = EncounterEffectRealizability.supports(
            "X", listOf(effect), setOf("A", "B"), false
        )

        assertFalse(partial.supported)
        assertTrue(complete.supported)
    }
}
