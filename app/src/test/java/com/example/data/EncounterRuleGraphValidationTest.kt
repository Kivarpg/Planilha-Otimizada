package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterRuleGraphValidationTest {
 
 @Test fun `relacao dangling e invalida`() {
  val a=EncounterRuleResolutionEngine.RuleId("a"); val missing=EncounterRuleResolutionEngine.RuleId("missing")
  val r=EncounterRuleResolutionEngine.resolve(listOf(EncounterRuleResolutionEngine.Rule(a)),listOf(EncounterRuleResolutionEngine.RuleRelation(a,missing,EncounterRuleResolutionEngine.Relation.OVERRIDES)))
  assertTrue(r.invalid)
 }
 @Test fun `ciclo applies after e ambiguo`() {
  val a=EncounterRuleResolutionEngine.RuleId("a"); val b=EncounterRuleResolutionEngine.RuleId("b")
  val r=EncounterRuleResolutionEngine.resolve(listOf(EncounterRuleResolutionEngine.Rule(a),EncounterRuleResolutionEngine.Rule(b)),listOf(
   EncounterRuleResolutionEngine.RuleRelation(a,b,EncounterRuleResolutionEngine.Relation.APPLIES_AFTER),EncounterRuleResolutionEngine.RuleRelation(b,a,EncounterRuleResolutionEngine.Relation.APPLIES_AFTER)))
  assertTrue(r.ambiguous)
 }
}