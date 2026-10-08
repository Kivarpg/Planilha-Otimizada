package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterStackingGroupTest {
 
 @Test fun `max of nao suprime bonus aditivo de outro grupo`() {
  val r=EncounterStackingSemantics.resolve(listOf(
   EncounterStackingSemantics.Contribution("a","DEF",2,EncounterStackingSemantics.Mode.MAX_OF,"dodge"),
   EncounterStackingSemantics.Contribution("b","DEF",3,EncounterStackingSemantics.Mode.MAX_OF,"dodge"),
   EncounterStackingSemantics.Contribution("c","DEF",1,EncounterStackingSemantics.Mode.ADDITIVE,"shield")
  ))
  assertEquals(4,r.knownTotal); assertTrue("c" in r.realizedIds)
 }
 @Test fun `max of compete apenas dentro do grupo`() {
  val r=EncounterStackingSemantics.resolve(listOf(
   EncounterStackingSemantics.Contribution("a","DEF",2,EncounterStackingSemantics.Mode.MAX_OF,"g1"),
   EncounterStackingSemantics.Contribution("b","DEF",3,EncounterStackingSemantics.Mode.MAX_OF,"g1"),
   EncounterStackingSemantics.Contribution("c","DEF",4,EncounterStackingSemantics.Mode.MAX_OF,"g2")
  ))
  assertEquals(7,r.knownTotal)
 }
}