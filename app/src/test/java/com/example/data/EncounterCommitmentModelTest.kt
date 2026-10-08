package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCommitmentModelTest {

    

    @Test
    fun `fixed build nao troca implicitamente`() {
        val state = EncounterCommitmentModel.State(mapOf(
            "native-domain" to EncounterCommitmentModel.Commitment("native-domain", "SOLAR", EncounterCommitmentModel.Scope.FIXED_BUILD)
        ))
        val result = EncounterCommitmentModel.apply(
            state,
            EncounterCommitmentModel.Commitment("native-domain", "LUNAR", EncounterCommitmentModel.Scope.FIXED_BUILD),
            emptyList(),
            emptySet()
        )
        assertFalse(result.allowed)
    }

    @Test
    fun `loadout diferente exige transicao explicita`() {
        val state = EncounterCommitmentModel.State(mapOf(
            "weapon" to EncounterCommitmentModel.Commitment("weapon", "SWORD", EncounterCommitmentModel.Scope.LOADOUT)
        ))
        val desired = EncounterCommitmentModel.Commitment("weapon", "BOW", EncounterCommitmentModel.Scope.LOADOUT)

        assertFalse(EncounterCommitmentModel.apply(state, desired, emptyList(), emptySet()).allowed)

        val rule = EncounterCommitmentModel.TransitionRule(
            key = "weapon",
            from = "SWORD",
            to = "BOW",
            allowed = true,
            minimumScope = EncounterCommitmentModel.Scope.LOADOUT,
            structuralCost = 2
        )
        val changed = EncounterCommitmentModel.apply(state, desired, listOf(rule), emptySet())
        assertTrue(changed.allowed)
        assertEquals(2, changed.structuralCost)
    }

    @Test
    fun `transicao condicionada nao existe sem requisito`() {
        val state = EncounterCommitmentModel.State(mapOf(
            "form" to EncounterCommitmentModel.Commitment("form", "HUMAN", EncounterCommitmentModel.Scope.TRANSIENT)
        ))
        val desired = EncounterCommitmentModel.Commitment("form", "WOLF", EncounterCommitmentModel.Scope.TRANSIENT)
        val rule = EncounterCommitmentModel.TransitionRule(
            "form", "HUMAN", "WOLF", true, EncounterCommitmentModel.Scope.TRANSIENT,
            requires = EncounterRequirementExpression.Fact("CAN_SHAPESHIFT")
        )

        assertFalse(EncounterCommitmentModel.apply(state, desired, listOf(rule), emptySet()).allowed)
        assertTrue(EncounterCommitmentModel.apply(state, desired, listOf(rule), setOf("CAN_SHAPESHIFT")).allowed)
    }
}
