package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterRouteValidationTest {
 @Test fun `duplicata nao vira shared prefix gratuito`() {
  
  val e=EncounterRouteState.evaluate(emptySet(),listOf(EncounterRouteState.Step("A",8,1),EncounterRouteState.Step("A",8,1)),EncounterRouteState.Horizon(20,2,3))
  assertFalse(e.reachable); assertNull(e.marginalXp)
 }
}