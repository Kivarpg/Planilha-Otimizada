package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterCombinationBranchPreservationTest {
 private fun effect(id:String)=EncounterMechanicalEffect(
  id,id,EncounterMechanicalEffect.Relation.PRODUCES,EncounterMechanicalEffect.EffectLifetime.Instant)
 @Test fun `ramos com mesma configuracao mas efeitos distintos nao colapsam antes da avaliacao`() {
  
  val p=EncounterCombinationEvaluator.Power("P",listOf(EncounterCombinationEvaluator.PowerVariant("v",branches=listOf(
   EncounterCombinationEvaluator.EffectBranch("small",effects=listOf(effect("a"))),
   EncounterCombinationEvaluator.EffectBranch("large",effects=listOf(effect("a"),effect("b")))
  ))))
  val r=EncounterCombinationEvaluator.realize(listOf(p),EncounterCombinationEvaluator.Configuration())
  assertTrue(r.realizable)
  assertEquals(setOf(1,2),r.realizations.map { it.effects.size }.toSet())
 }
}