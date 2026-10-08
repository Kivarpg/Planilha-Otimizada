package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterActorScopeTest {
 
 @Test fun `efeito de aliado nao conta como self`() {
  val produced=EncounterActorScope.ScopedMechanic("CRASH",actionActor=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ALLY))
  val required=EncounterActorScope.ScopedRequirement("CRASH")
  assertFalse(EncounterActorScope.matches(produced,required).supported)
 }
 @Test fun `entidade arma nao conta como self`() {
  val produced=EncounterActorScope.ScopedMechanic("INITIATIVE_GAIN",actionActor=EncounterActorScope.ActorRef(EncounterActorScope.Actor.WEAPON_ENTITY))
  val required=EncounterActorScope.ScopedRequirement("INITIATIVE_GAIN")
  assertFalse(EncounterActorScope.matches(produced,required).supported)
 }
 @Test fun `dependencia externa fica condicional`() {
  val produced=EncounterActorScope.ScopedMechanic("ALLY_DEFENSE",
   actionActor=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ALLY),beneficiary=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ALLY))
  val required=EncounterActorScope.ScopedRequirement("ALLY_DEFENSE",
   allowedActionActors=setOf(EncounterActorScope.Actor.ALLY),allowedBeneficiaries=setOf(EncounterActorScope.Actor.ALLY))
  val classified=EncounterDependencyClassifier.classify(EncounterActorScope.matches(produced,required))
  assertEquals(EncounterDependencySource.EXTERNAL_CONDITIONAL,classified.source)
 }
}