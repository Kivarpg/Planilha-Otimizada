package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterStateTransactionTest {
 @Test fun `falha nao deixa mutacao parcial`() {
  
  val initial=EncounterProjectedFactState.State()
  val tx=EncounterStateTransaction.Transaction("x",EncounterTriRequirement.Fact("ALLOW"),
   listOf(EncounterProjectedFactState.Mutation.Produce(EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("GHOST"),EncounterProjectedFactState.SourceId("x"),EncounterProjectedFactState.Lifetime.TRANSIENT))))
  val r=EncounterStateTransaction.apply(initial,tx,emptySet(),setOf("ALLOW"))
  assertFalse(r.committed); assertFalse(r.state.has(EncounterProjectedFactState.FactId("GHOST")))
 }
 @Test fun `unknown tambem nao persiste mutacao`() {
  
  val tx=EncounterStateTransaction.Transaction("x",EncounterTriRequirement.Fact("ALLOW"),
   listOf(EncounterProjectedFactState.Mutation.Produce(EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("GHOST"),EncounterProjectedFactState.SourceId("x"),EncounterProjectedFactState.Lifetime.TRANSIENT))))
  val r=EncounterStateTransaction.apply(EncounterProjectedFactState.State(),tx,emptySet(),emptySet())
  assertFalse(r.committed); assertEquals(EncounterTruth.UNKNOWN,r.truth)
 }
}