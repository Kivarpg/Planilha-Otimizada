package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterDerivedFactResolverTest {
    

    @Test
    fun `revogacao de raiz remove cadeia derivada`() {
        val ds=listOf(
            EncounterDerivedFactResolver.Derivation("B",setOf("A")),
            EncounterDerivedFactResolver.Derivation("C",setOf("B"))
        )
        val before=EncounterDerivedFactResolver.stableFacts(setOf("A"),ds)
        assertTrue("C" in before)
        val after=EncounterDerivedFactResolver.recomputeAfterRevocation(emptySet(),ds)
        assertFalse("B" in after)
        assertFalse("C" in after)
    }

    @Test
    fun `ciclo sem raiz nao se auto sustenta`() {
        val ds=listOf(
            EncounterDerivedFactResolver.Derivation("A",setOf("B")),
            EncounterDerivedFactResolver.Derivation("B",setOf("A"))
        )
        val result=EncounterDerivedFactResolver.stableFacts(emptySet(),ds)
        assertFalse("A" in result)
        assertFalse("B" in result)
    }
}
