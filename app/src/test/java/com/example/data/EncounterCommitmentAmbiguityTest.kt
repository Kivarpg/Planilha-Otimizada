package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterCommitmentAmbiguityTest {
 
 @Test fun `minimum scope e aplicado`() {
  val st=EncounterCommitmentModel.State(mapOf("weapon" to EncounterCommitmentModel.Commitment("weapon","sword",EncounterCommitmentModel.Scope.SCENE)))
  val r=EncounterCommitmentModel.apply(st,EncounterCommitmentModel.Commitment("weapon","bow",EncounterCommitmentModel.Scope.SCENE),
   listOf(EncounterCommitmentModel.TransitionRule("weapon","sword","bow",true,EncounterCommitmentModel.Scope.ACTION)),emptySet())
  assertFalse(r.allowed)
 }
 @Test fun `regras conflitantes nao dependem da ordem`() {
  val st=EncounterCommitmentModel.State(mapOf("x" to EncounterCommitmentModel.Commitment("x","a",EncounterCommitmentModel.Scope.ACTION)))
  val rules=listOf(
   EncounterCommitmentModel.TransitionRule("x","a","b",true,EncounterCommitmentModel.Scope.ACTION,1),
   EncounterCommitmentModel.TransitionRule("x",null,"b",true,EncounterCommitmentModel.Scope.ACTION,2))
  assertFalse(EncounterCommitmentModel.apply(st,EncounterCommitmentModel.Commitment("x","b",EncounterCommitmentModel.Scope.ACTION),rules,emptySet()).allowed)
 }
}