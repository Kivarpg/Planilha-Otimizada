package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterUpgradeChoiceResolverTest {
 @Test fun `upgrades exclusivos nao vazam entre opcoes`() {
  
  
  val cs=listOf(
   EncounterUpgradeChoiceResolver.ChoiceDelta(EncounterPowerProjection.Delta("fire",EncounterPowerProjection.DeltaKind.ADD), "element","fire"),
   EncounterUpgradeChoiceResolver.ChoiceDelta(EncounterPowerProjection.Delta("water",EncounterPowerProjection.DeltaKind.ADD),"element","water")
  )
  val r=EncounterUpgradeChoiceResolver.select(cs,mapOf("element" to "fire"))
  assertEquals(setOf("fire"),r.deltas.map{it.id}.toSet())
 }
}