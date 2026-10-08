package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterAvailabilityCycleTest {
 @Test fun `reset desconhecido nao vira disponibilidade infinita`() {
  val a=EncounterAvailabilityCycle.Availability("x",EncounterAvailabilityCycle.UseLimit.ONCE_PER_SCENE,
   EncounterTriRequirement.Fact("RESET"))
  val r=EncounterAvailabilityCycle.assess(a,emptySet(),emptySet())
  assertNull(r.reusableInPrinciple); assertTrue(r.bounded)
 }
}