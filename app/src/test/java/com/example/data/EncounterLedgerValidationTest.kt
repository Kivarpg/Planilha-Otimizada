package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterLedgerValidationTest {
 @Test fun `confidence invalida rejeita agregado`() {
  
  val r=EncounterSynergyLedger.aggregate(listOf(EncounterSynergyLedger.Evidence("x",EncounterSynergyLedger.Layer.PAIR,10,1.5,setOf("a"))))
  assertFalse(r.valid); assertNull(r.value)
 }
}