package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterStackingIntegrityTest {
 
 @Test fun `id duplicado nao e resolvido silenciosamente`() {
  val r=EncounterStackingSemantics.resolve(listOf(EncounterStackingSemantics.Contribution("x","D",1,EncounterStackingSemantics.Mode.ADDITIVE),
   EncounterStackingSemantics.Contribution("x","D",2,EncounterStackingSemantics.Mode.ADDITIVE)))
  assertTrue(r.uncertain); assertNull(r.knownTotal)
 }
 @Test fun `replace dangling e invalido`() {
  val r=EncounterStackingSemantics.resolve(listOf(EncounterStackingSemantics.Contribution("x","D",1,EncounterStackingSemantics.Mode.REPLACES,replacesId="missing")))
  assertTrue(r.uncertain)
 }
}