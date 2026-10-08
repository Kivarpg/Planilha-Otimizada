package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterRuleResolutionEngineTest {
    

    @Test
    fun `excecao suprime apenas regra alvo`() {
        val general=EncounterRuleResolutionEngine.RuleId("simple-limit")
        val other=EncounterRuleResolutionEngine.RuleId("weapon-requirement")
        val exception=EncounterRuleResolutionEngine.RuleId("specific-exception")
        val result=EncounterRuleResolutionEngine.resolve(
            listOf(EncounterRuleResolutionEngine.Rule(general),EncounterRuleResolutionEngine.Rule(other),EncounterRuleResolutionEngine.Rule(exception)),
            listOf(EncounterRuleResolutionEngine.RuleRelation(exception,general,EncounterRuleResolutionEngine.Relation.OVERRIDES))
        )
        assertFalse(general in result.active)
        assertTrue(other in result.active)
    }

    @Test
    fun `overrides mutuos ficam ambiguos`() {
        val a=EncounterRuleResolutionEngine.RuleId("a"); val b=EncounterRuleResolutionEngine.RuleId("b")
        val result=EncounterRuleResolutionEngine.resolve(
            listOf(EncounterRuleResolutionEngine.Rule(a),EncounterRuleResolutionEngine.Rule(b)),
            listOf(
                EncounterRuleResolutionEngine.RuleRelation(a,b,EncounterRuleResolutionEngine.Relation.OVERRIDES),
                EncounterRuleResolutionEngine.RuleRelation(b,a,EncounterRuleResolutionEngine.Relation.OVERRIDES)
            )
        )
        assertTrue(result.ambiguous)
    }
}
