package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterActorScopeCatalogTest {
 
 @Test fun `charm proprio pode aprimorar acao de aliado`() {
  val m=EncounterActorScope.ScopedMechanic("DAMAGE_BONUS",
   activator=EncounterActorScope.ActorRef(EncounterActorScope.Actor.SELF,EncounterParticipantId("solar")),
   actionActor=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ALLY,EncounterParticipantId("ally1")),
   beneficiary=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ALLY,EncounterParticipantId("ally1")))
  val req=EncounterActorScope.ScopedRequirement("DAMAGE_BONUS",
   allowedActionActors=setOf(EncounterActorScope.Actor.ALLY),
   allowedBeneficiaries=setOf(EncounterActorScope.Actor.ALLY),
   bindingConstraints=setOf(EncounterParticipantBindings.Constraint(
    EncounterBindingKey("actionActor"),EncounterBindingKey("beneficiary"),
    EncounterParticipantBindings.Relation.SAME)))
  assertTrue(EncounterActorScope.matches(m,req).supported)
 }
 @Test fun `aliados indefinidos nao sao unificados automaticamente`() {
  val m=EncounterActorScope.ScopedMechanic("BONUS",
   actionActor=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ALLY,null),beneficiary=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ALLY,null))
  val req=EncounterActorScope.ScopedRequirement("BONUS",allowedActionActors=setOf(EncounterActorScope.Actor.ALLY),
   allowedBeneficiaries=setOf(EncounterActorScope.Actor.ALLY),
   bindingConstraints=setOf(EncounterParticipantBindings.Constraint(
    EncounterBindingKey("actionActor"),EncounterBindingKey("beneficiary"),
    EncounterParticipantBindings.Relation.SAME)))
  assertTrue(EncounterActorScope.matches(m,req).uncertain)
 }
}