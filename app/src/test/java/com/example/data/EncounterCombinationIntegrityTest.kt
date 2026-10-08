package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterCombinationIntegrityTest {
 @Test fun `power ids duplicados sao rejeitados`() {
  
  val p=EncounterCombinationEvaluator.Power("P",emptyList())
  val r=EncounterCombinationEvaluator.realize(listOf(p,p),EncounterCombinationEvaluator.Configuration())
  assertFalse(r.realizable); assertTrue(r.realizations.isEmpty())
 }
}