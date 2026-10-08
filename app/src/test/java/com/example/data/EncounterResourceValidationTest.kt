package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterResourceValidationTest {
 @Test fun `conversao sem destino e invalida`() {
  
  val p=EncounterResourceBudget.Profile(listOf(EncounterResourceBudget.Flow("x",EncounterResourceBudget.Resource.MOTES,EncounterResourceBudget.FlowKind.CONVERT,3)))
  assertTrue(EncounterResourceValidation.validate(p).isNotEmpty())
 }
 @Test fun `capacity map nao pode mentir namespace`() {
  
  val p=EncounterResourceBudget.Profile(emptyList(),mapOf(EncounterResourceBudget.Resource.MOTES to EncounterResourceBudget.Capacity(EncounterResourceBudget.Resource.WILLPOWER,5,5)))
  assertTrue(EncounterResourceValidation.validate(p).isNotEmpty())
 }
}