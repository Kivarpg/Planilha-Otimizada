package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterRouteStateTest {
 @Test fun `prefixo ja possuido nao e pago novamente`() {
  
  val e=EncounterRouteState.evaluate(setOf("A"),listOf(EncounterRouteState.Step("A",8,1),EncounterRouteState.Step("B",8,1)),EncounterRouteState.Horizon(8,1,1))
  assertTrue(e.reachable); assertEquals(8,e.marginalXp)
 }
 @Test fun `destino fora do horizonte nao recebe valor`() {
  
  val e=EncounterRouteState.evaluate(emptySet(),listOf(EncounterRouteState.Step("A",8,1),EncounterRouteState.Step("B",8,3)),EncounterRouteState.Horizon(20,2,3))
  assertFalse(e.reachable)
 }
}