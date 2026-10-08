package com.example.data
import org.junit.Assert.*; import org.junit.Test
class EncounterModifierDuplicateIdTest {
 @Test fun `ids duplicados nao sao colapsados silenciosamente`() {
  
  val r=EncounterModifierPrecedence.resolve(listOf(EncounterModifierPrecedence.ModifierNode("x"),EncounterModifierPrecedence.ModifierNode("x")))
  assertTrue(r is EncounterModifierPrecedence.Result.Invalid)
 }
}