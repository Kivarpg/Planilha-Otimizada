package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterCombinationActorBindingTest {
 
 @Test fun `mesmo aliado precisa ser provado`() {
  val p=EncounterCombinationEvaluator.Power("P",listOf(EncounterCombinationEvaluator.PowerVariant("v",branches=listOf(
   EncounterCombinationEvaluator.EffectBranch("b",effects=emptyList(),
    bindings=mapOf(EncounterBindingKey("producerAlly") to EncounterParticipantId("ally#1"),
     EncounterBindingKey("consumerAlly") to EncounterParticipantId("ally#2")),
    participantConstraints=setOf(EncounterParticipantBindings.Constraint(
     EncounterBindingKey("producerAlly"),EncounterBindingKey("consumerAlly"),
     EncounterParticipantBindings.Relation.SAME)))
  ))))
  assertFalse(EncounterCombinationEvaluator.realize(listOf(p),EncounterCombinationEvaluator.Configuration()).realizable)
 }
 @Test fun `binding consistente e realizavel`() {
  val p=EncounterCombinationEvaluator.Power("P",listOf(EncounterCombinationEvaluator.PowerVariant("v",branches=listOf(
   EncounterCombinationEvaluator.EffectBranch("b",effects=emptyList(),
    bindings=mapOf(EncounterBindingKey("producerAlly") to EncounterParticipantId("ally#1"),
     EncounterBindingKey("consumerAlly") to EncounterParticipantId("ally#1")),
    participantConstraints=setOf(EncounterParticipantBindings.Constraint(
     EncounterBindingKey("producerAlly"),EncounterBindingKey("consumerAlly"),
     EncounterParticipantBindings.Relation.SAME)))
  ))))
  assertTrue(EncounterCombinationEvaluator.realize(listOf(p),EncounterCombinationEvaluator.Configuration()).realizable)
 }
 @Test fun `binding conflitante com configuracao inicial e rejeitado`() {
  val key=EncounterBindingKey("ally")
  val p=EncounterCombinationEvaluator.Power("P",listOf(EncounterCombinationEvaluator.PowerVariant("v",branches=listOf(
   EncounterCombinationEvaluator.EffectBranch("b",effects=emptyList(),
    bindings=mapOf(key to EncounterParticipantId("ally#2")))
  ))))
  val initial=EncounterCombinationEvaluator.Configuration(
   bindings=mapOf(key to EncounterParticipantId("ally#1"))
  )
  assertFalse(EncounterCombinationEvaluator.realize(listOf(p),initial).realizable)
 }
 @Test fun `constraint sem todos os bindings continua desconhecida e rejeitada`() {
  val left=EncounterBindingKey("left")
  val right=EncounterBindingKey("right")
  val p=EncounterCombinationEvaluator.Power("P",listOf(EncounterCombinationEvaluator.PowerVariant("v",branches=listOf(
   EncounterCombinationEvaluator.EffectBranch("b",effects=emptyList(),
    bindings=mapOf(left to EncounterParticipantId("ally#1")),
    participantConstraints=setOf(EncounterParticipantBindings.Constraint(
     left,right,EncounterParticipantBindings.Relation.SAME)))
  ))))
  assertFalse(EncounterCombinationEvaluator.realize(listOf(p),EncounterCombinationEvaluator.Configuration()).realizable)
 }
}
