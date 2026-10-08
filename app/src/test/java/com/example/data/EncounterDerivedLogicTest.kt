package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterDerivedLogicTest {
 
 @Test fun `derivacao or nao vira and`() {
  val d=EncounterDerivedFactResolver.Derivation("C",EncounterRequirementExpression.AnyOf(listOf(EncounterRequirementExpression.Fact("A"),EncounterRequirementExpression.Fact("B"))))
  assertTrue("C" in EncounterDerivedFactResolver.stableFacts(setOf("A"),listOf(d)))
 }
 @Test fun `derivacao not e preservada em mundo fechado`() {
  val d=EncounterDerivedFactResolver.Derivation("C",EncounterRequirementExpression.Not(EncounterRequirementExpression.Fact("BLOCKED")))
  assertTrue("C" in EncounterDerivedFactResolver.stableFacts(emptySet(),listOf(d)))
  assertFalse("C" in EncounterDerivedFactResolver.stableFacts(setOf("BLOCKED"),listOf(d)))
 }
}