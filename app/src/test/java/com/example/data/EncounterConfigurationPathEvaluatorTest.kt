package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterConfigurationPathEvaluatorTest {

    
    

    @Test
    fun `rota nao pode colher duas configuracoes sem caminho entre elas`() {
        val initial = EncounterCommitmentModel.State(mapOf(
            "weapon" to EncounterCommitmentModel.Commitment("weapon", "SWORD", EncounterCommitmentModel.Scope.LOADOUT)
        ))
        val steps = listOf(
            EncounterConfigurationPathEvaluator.Step("sword-synergy", setOf(EncounterCommitmentModel.Commitment("weapon","SWORD",EncounterCommitmentModel.Scope.LOADOUT))),
            EncounterConfigurationPathEvaluator.Step("bow-synergy", setOf(EncounterCommitmentModel.Commitment("weapon","BOW",EncounterCommitmentModel.Scope.LOADOUT)))
        )

        val result = EncounterConfigurationPathEvaluator.evaluate(initial, steps, emptyList())
        assertFalse(result.reachable)
        assertEquals("bow-synergy", result.failedStepId)
    }

    @Test
    fun `rota pode alternar configuracao quando regra existe e paga custo`() {
        val initial = EncounterCommitmentModel.State(mapOf(
            "weapon" to EncounterCommitmentModel.Commitment("weapon", "SWORD", EncounterCommitmentModel.Scope.LOADOUT)
        ))
        val steps = listOf(
            EncounterConfigurationPathEvaluator.Step("sword", setOf(EncounterCommitmentModel.Commitment("weapon","SWORD",EncounterCommitmentModel.Scope.LOADOUT))),
            EncounterConfigurationPathEvaluator.Step("bow", setOf(EncounterCommitmentModel.Commitment("weapon","BOW",EncounterCommitmentModel.Scope.LOADOUT)))
        )
        val rules = listOf(
            EncounterCommitmentModel.TransitionRule("weapon","SWORD","BOW",true,EncounterCommitmentModel.Scope.LOADOUT,3)
        )

        val result = EncounterConfigurationPathEvaluator.evaluate(initial, steps, rules)
        assertTrue(result.reachable)
        assertEquals(3, result.transitionCost)
    }
}
