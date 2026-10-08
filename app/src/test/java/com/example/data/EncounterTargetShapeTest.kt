package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterTargetShapeTest {
 
 @Test fun `cardinalidade insuficiente bloqueia relacao estrutural`() {
  val shape=EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.CHARACTER),EncounterTargetShape.Selection.MULTIPLE_DISTINCT,maxApplications=EncounterQuantityExpression.Constant(2))
  val req=EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.CHARACTER),setOf(EncounterTargetShape.Selection.MULTIPLE_DISTINCT),minimumApplications=3)
  assertEquals(false,EncounterTargetShape.compatible(shape,req,EncounterBuildQuantities(emptyMap())))
 }
 @Test fun `cardinalidade desconhecida nao e promovida a compativel`() {
  val shape=EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.CHARACTER),EncounterTargetShape.Selection.MULTIPLE_DISTINCT,maxApplications=EncounterQuantityExpression.Trait("ESSENCE"))
  val req=EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.CHARACTER),setOf(EncounterTargetShape.Selection.MULTIPLE_DISTINCT),minimumApplications=2)
  assertNull(EncounterTargetShape.compatible(shape,req,EncounterBuildQuantities(emptyMap())))
 }
}