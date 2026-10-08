package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterBuildCoherenceTest {
 @Test fun `hub generico nao define foco`() {
  
  val r=EncounterBuildCoherence.evaluate(listOf(
   EncounterBuildCoherence.Contribution("core",EncounterBuildCoherence.Role.CORE_ENGINE,setOf("CRASH"),10),
   EncounterBuildCoherence.Contribution("hub",EncounterBuildCoherence.Role.UTILITY,setOf("A","B","C","D","E"),10)
  ))
  assertEquals(1,r.coreFocus); assertTrue((r.genericHubPenalty ?: 0)>0)
 }
}