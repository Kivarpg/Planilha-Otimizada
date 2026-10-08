package com.example.data

import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterModifierPrecedenceTest {
    

    @Test
    fun `ordem sem regra explicita permanece ambigua`() {
        val result=EncounterModifierPrecedence.resolve(listOf(EncounterModifierPrecedence.ModifierNode("reduce"),EncounterModifierPrecedence.ModifierNode("cap")))
        assertTrue(result is EncounterModifierPrecedence.Result.Ambiguous)
    }

    @Test
    fun `precedencia explicita produz ordem`() {
        val result=EncounterModifierPrecedence.resolve(listOf(
            EncounterModifierPrecedence.ModifierNode("replace",before=setOf("reduce")),
            EncounterModifierPrecedence.ModifierNode("reduce",after=setOf("replace"))
        ))
        assertTrue(result is EncounterModifierPrecedence.Result.Ordered)
    }

    @Test
    fun `ciclo nao e resolvido por ordem incidental`() {
        val result=EncounterModifierPrecedence.resolve(listOf(
            EncounterModifierPrecedence.ModifierNode("a",before=setOf("b")),
            EncounterModifierPrecedence.ModifierNode("b",before=setOf("a"))
        ))
        assertTrue(result is EncounterModifierPrecedence.Result.Cycle)
    }
}
