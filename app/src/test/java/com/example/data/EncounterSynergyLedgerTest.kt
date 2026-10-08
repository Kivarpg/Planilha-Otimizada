package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterSynergyLedgerTest {
 @Test fun `mesma causa em varias camadas conta uma vez`() {
  
  val a=EncounterSynergyLedger.Evidence("A->B:X",EncounterSynergyLedger.Layer.PAIR,10,1.0,setOf("A","B"))
  val b=EncounterSynergyLedger.Evidence("A->B:X",EncounterSynergyLedger.Layer.PATH,15,1.0,setOf("A","B"))
  val r=EncounterSynergyLedger.aggregate(listOf(a,b))
  assertEquals(15,r.value); assertEquals(1,r.retained.size); assertEquals(1,r.discarded.size)
 }
}