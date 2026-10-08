package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterProjectionValidationTest {
 private fun e(id:String)=EncounterMechanicalEffect(id,id,EncounterMechanicalEffect.Relation.PRODUCES,EncounterMechanicalEffect.EffectLifetime.Instant)
 @Test fun `delta malformado e rejeitado`() {
  val d=EncounterPowerProjection.Delta("bad",EncounterPowerProjection.DeltaKind.REPLACE,targetEffectId="base",effect=null)
  val r=EncounterPowerProjection.project(listOf(e("base")),listOf(d),emptySet())
  assertTrue("bad" in r.rejectedDeltaIds); assertTrue("base" in r.effects.map{it.id})
 }
 @Test fun `replace de alvo inexistente nao cria upgrade orfao`() {
  val d=EncounterPowerProjection.Delta("bad",EncounterPowerProjection.DeltaKind.REPLACE,"missing",e("new"))
  val r=EncounterPowerProjection.project(emptyList(),listOf(d),emptySet())
  assertTrue(r.effects.isEmpty()); assertTrue("bad" in r.rejectedDeltaIds)
 }
}