package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EncounterQuantityExpressionTest {
    

    @Test
    fun `cap derivado da ficha e resolvido sem simulacao`() {
        val expr = EncounterQuantityExpression.Add(listOf(EncounterQuantityExpression.Trait("DEXTERITY"), EncounterQuantityExpression.Trait("ESSENCE")))
        assertEquals(7, expr.resolve(mapOf("DEXTERITY" to 5, "ESSENCE" to 2)))
    }

    @Test
    fun `trait ausente permanece desconhecido`() {
        assertNull(EncounterQuantityExpression.Trait("UNKNOWN_TRAIT").resolve(emptyMap()))
    }

    @Test
    fun `arredondamento e explicito`() {
        assertEquals(3, EncounterQuantityExpression.HalfRoundedUp(EncounterQuantityExpression.Constant(5)).resolve(emptyMap()))
    }
}
