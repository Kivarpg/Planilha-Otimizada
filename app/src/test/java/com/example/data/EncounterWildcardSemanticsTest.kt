package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterWildcardSemanticsTest {
 @Test fun `qualquer personagem nao prova aliado especifico`() {
  
  val m=EncounterActorScope.ScopedMechanic("X",actionActor=EncounterActorScope.ActorRef(EncounterActorScope.Actor.ANY_CHARACTER))
  val req=EncounterActorScope.ScopedRequirement("X",allowedActionActors=setOf(EncounterActorScope.Actor.ALLY))
  assertTrue(EncounterActorScope.matches(m,req).uncertain)
 }
 @Test fun `aliado satisfaz requisito personagem`() {
  
  val s=EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.ALLY),EncounterTargetShape.Selection.SINGLE,1)
  val r=EncounterTargetShape.compatible(s,EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.CHARACTER),setOf(EncounterTargetShape.Selection.SINGLE)),
   EncounterBuildQuantities(emptyMap()))
  assertEquals(true,r)
 }
 @Test fun `personagem generico nao prova inimigo`() {
  
  val s=EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.CHARACTER),EncounterTargetShape.Selection.SINGLE,1)
  val r=EncounterTargetShape.compatible(s,EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.ENEMY),setOf(EncounterTargetShape.Selection.SINGLE)),
   EncounterBuildQuantities(emptyMap()))
  assertNull(r)
 }
}