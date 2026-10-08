package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterOpenWorldCombinationTest {
 
 @Test fun `not de fato desconhecido nao prova variante`() {
  val p=EncounterCombinationEvaluator.Power("P",listOf(EncounterCombinationEvaluator.PowerVariant("v",EncounterRequirementExpression.Not(EncounterRequirementExpression.Fact("BLOCKED")),
   listOf(EncounterCombinationEvaluator.EffectBranch("b",effects=emptyList())))))
  assertFalse(EncounterCombinationEvaluator.realize(listOf(p),EncounterCombinationEvaluator.Configuration()).realizable)
 }
 @Test fun `not de falso conhecido prova variante`() {
  val p=EncounterCombinationEvaluator.Power("P",listOf(EncounterCombinationEvaluator.PowerVariant("v",EncounterRequirementExpression.Not(EncounterRequirementExpression.Fact("BLOCKED")),
   listOf(EncounterCombinationEvaluator.EffectBranch("b",effects=emptyList())))))
  assertTrue(EncounterCombinationEvaluator.realize(listOf(p),EncounterCombinationEvaluator.Configuration(knownFalseFacts=setOf("BLOCKED"))).realizable)
 }
}