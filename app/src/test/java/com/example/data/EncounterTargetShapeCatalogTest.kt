package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterTargetShapeCatalogTest {
 
 @Test fun `um ou varios alvos satisfaz requisito single`() {
  val s=EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.ENEMY),EncounterTargetShape.Selection.SINGLE_OR_MULTIPLE,1)
  val r=EncounterTargetShape.compatible(s,EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.ENEMY),setOf(EncounterTargetShape.Selection.SINGLE)),EncounterBuildQuantities(emptyMap()))
  assertEquals(true,r)
 }
 @Test fun `repetir mesmo alvo nao equivale a alvos distintos`() {
  val s=EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.ENEMY),EncounterTargetShape.Selection.REPEATED_SAME,2)
  val r=EncounterTargetShape.compatible(s,EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.ENEMY),setOf(EncounterTargetShape.Selection.MULTIPLE_DISTINCT),2),EncounterBuildQuantities(emptyMap()))
  assertEquals(false,r)
 }
 @Test fun `area indiscriminada falha requisito sem fogo amigo`() {
  val s=EncounterTargetShape.Shape(setOf(EncounterTargetShape.Kind.CHARACTER),EncounterTargetShape.Selection.ALL_IN_AREA,1,inclusion=EncounterTargetShape.Inclusion.INDISCRIMINATE)
  val r=EncounterTargetShape.compatible(s,EncounterTargetShape.Requirement(setOf(EncounterTargetShape.Kind.CHARACTER),setOf(EncounterTargetShape.Selection.ALL_IN_AREA),forbidsFriendlyFire=true),EncounterBuildQuantities(emptyMap()))
  assertEquals(false,r)
 }
}