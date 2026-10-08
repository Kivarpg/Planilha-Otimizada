package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterEffectPersistenceContextTest {
 @Test fun `until condition desconhecida nao prova cross round`() {
  val E=EncounterMechanicalEffect("x","M",EncounterMechanicalEffect.Relation.PRODUCES,
   EncounterMechanicalEffect.EffectLifetime.UntilCondition("COND"))
  val r=EncounterEffectRealizability.supports("M",listOf(E),emptySet(),true)
  assertFalse(r.supported)
 }
}