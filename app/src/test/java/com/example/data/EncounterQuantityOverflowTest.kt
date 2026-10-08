package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterQuantityOverflowTest {
 
 @Test fun `soma overflow vira desconhecido`() {
  assertNull(EncounterQuantityExpression.Add(listOf(EncounterQuantityExpression.Constant(Int.MAX_VALUE),EncounterQuantityExpression.Constant(1))).resolve(emptyMap()))
 }
 @Test fun `multiplicacao overflow vira desconhecido`() {
  assertNull(EncounterQuantityExpression.Multiply(EncounterQuantityExpression.Constant(Int.MAX_VALUE),EncounterQuantityExpression.Constant(2)).resolve(emptyMap()))
 }
}