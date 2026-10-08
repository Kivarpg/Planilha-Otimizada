package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterTargetShapeValidationTest {
 @Test fun `minimo declarado pode provar requisito sem maximo`() {
  
  val r=EncounterTargetShape.compatible(EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.CHARACTER),EncounterTargetShape.Selection.MULTIPLE_DISTINCT,minApplications=3),
   EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.CHARACTER),setOf(EncounterTargetShape.Selection.MULTIPLE_DISTINCT),2),EncounterBuildQuantities(emptyMap()))
  assertEquals(true,r)
 }
 @Test fun `maximo menor que minimo e inconsistente`() {
  
  val r=EncounterTargetShape.compatible(EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.CHARACTER),EncounterTargetShape.Selection.MULTIPLE_DISTINCT,3,EncounterQuantityExpression.Constant(2)),
   EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.CHARACTER),setOf(EncounterTargetShape.Selection.MULTIPLE_DISTINCT),4),EncounterBuildQuantities(emptyMap()))
  assertNull(r)
 }
}