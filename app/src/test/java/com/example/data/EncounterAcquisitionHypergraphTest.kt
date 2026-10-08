package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterAcquisitionHypergraphTest {
 @Test fun `and nao cria unlock parcial`() {
  
  val n=EncounterAcquisitionHypergraph.Node("C",EncounterRequirementExpression.AllOf(listOf(EncounterRequirementExpression.Fact("A"),EncounterRequirementExpression.Fact("B"))))
  assertTrue(EncounterAcquisitionHypergraph.marginalUnlocks("A",emptySet(),listOf(n)).isEmpty())
  assertEquals(setOf("C"),EncounterAcquisitionHypergraph.marginalUnlocks("B",setOf("A"),listOf(n)))
 }
 @Test fun `or abre com uma alternativa`() {
  
  val n=EncounterAcquisitionHypergraph.Node("C",EncounterRequirementExpression.AnyOf(listOf(EncounterRequirementExpression.Fact("A"),EncounterRequirementExpression.Fact("B"))))
  assertEquals(setOf("C"),EncounterAcquisitionHypergraph.marginalUnlocks("A",emptySet(),listOf(n)))
 }
}