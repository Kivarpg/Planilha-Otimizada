package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterActionSignatureTest {
 
 @Test fun `simple nao enhanceable rejeita supplemental`() {
  val s=EncounterActionSignature.Signature(EncounterCharmActivationCompatibility.ActionKind.WITHERING_ATTACK,governingTrait="Melee",charmEnhanceable=false)
  assertEquals(EncounterActionSignature.Compatibility.INCOMPATIBLE,EncounterActionSignature.compatible(s,EncounterActionSignature.Requirement(setOf(EncounterCharmActivationCompatibility.ActionKind.WITHERING_ATTACK))))
 }
 @Test fun `trait desconhecido nao vira compativel`() {
  val s=EncounterActionSignature.Signature(EncounterCharmActivationCompatibility.ActionKind.ATTACK,charmEnhanceable=true)
  assertEquals(EncounterActionSignature.Compatibility.UNKNOWN,EncounterActionSignature.compatible(s,EncounterActionSignature.Requirement(setOf(EncounterCharmActivationCompatibility.ActionKind.ATTACK),governingTraits=setOf("Archery"))))
 }
 @Test fun `range incompatível bloqueia`() {
  val s=EncounterActionSignature.Signature(EncounterCharmActivationCompatibility.ActionKind.ATTACK,ranges=setOf("close"),charmEnhanceable=true)
  assertEquals(EncounterActionSignature.Compatibility.INCOMPATIBLE,EncounterActionSignature.compatible(s,EncounterActionSignature.Requirement(setOf(EncounterCharmActivationCompatibility.ActionKind.ATTACK),ranges=setOf("long"))))
 }
}