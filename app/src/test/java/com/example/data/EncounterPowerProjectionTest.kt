package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterPowerProjectionTest {
 
 private fun e(id:String)=EncounterMechanicalEffect(id,id,EncounterMechanicalEffect.Relation.PRODUCES,EncounterMechanicalEffect.EffectLifetime.Instant)
 @Test fun `replace remove efeito base`() {
  val p=EncounterPowerProjection.project(listOf(e("base")),listOf(
   EncounterPowerProjection.Delta("u",EncounterPowerProjection.DeltaKind.REPLACE,"base",e("upgrade"))
  ),emptySet())
  assertEquals(setOf("upgrade"),p.effects.map{it.id}.toSet())
 }
}