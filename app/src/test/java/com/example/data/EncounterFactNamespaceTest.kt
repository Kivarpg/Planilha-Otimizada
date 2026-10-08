package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterFactNamespaceTest {
 @Test fun `owned e active nao colidem`() {
  
  val ks=EncounterFactNamespace.keys(listOf(EncounterFactNamespace.Fact(EncounterFactNamespace.Kind.OWNED_POWER,"X"),EncounterFactNamespace.Fact(EncounterFactNamespace.Kind.ACTIVE_POWER,"X")))
  assertEquals(2,ks.size)
 }
}