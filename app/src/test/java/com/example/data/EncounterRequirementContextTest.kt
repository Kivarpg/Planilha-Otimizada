package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterRequirementContextTest {
 @Test fun `ausencia em mundo fechado e false`() {
  
  assertEquals(EncounterTruth.FALSE,EncounterRequirementContext.fact("X",EncounterRequirementContext.Context(emptySet(),mode=EncounterRequirementContext.KnowledgeMode.CLOSED_WORLD)))
 }
 @Test fun `ausencia em mundo aberto e unknown`() {
  
  assertEquals(EncounterTruth.UNKNOWN,EncounterRequirementContext.fact("X",EncounterRequirementContext.Context(emptySet(),mode=EncounterRequirementContext.KnowledgeMode.OPEN_WORLD)))
 }
}