package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterRuleResolutionConflictTest {
 @Test fun `override mutuo nao produz estado dependente da ordem`() {
  val a=EncounterRuleResolutionEngine.RuleId("a"); val b=EncounterRuleResolutionEngine.RuleId("b")
  val r=EncounterRuleResolutionEngine.resolve(listOf(EncounterRuleResolutionEngine.Rule(a),EncounterRuleResolutionEngine.Rule(b)),listOf(
   EncounterRuleResolutionEngine.RuleRelation(a,b,EncounterRuleResolutionEngine.Relation.OVERRIDES),EncounterRuleResolutionEngine.RuleRelation(b,a,EncounterRuleResolutionEngine.Relation.OVERRIDES)))
  assertTrue(r.ambiguous); assertEquals(setOf(a,b),r.active); assertTrue(r.suppressed.isEmpty())
 }
}