package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EncounterDynamicCapResolverTest {

    @Test
    fun `shared cap usa valor da ficha`() {
        val result = EncounterDynamicCapResolver.resolve(
            listOf(
                EncounterDynamicCapResolver.DynamicContribution(
                    "a","BONUS",EncounterQuantityExpression.Constant(4),EncounterStackingSemantics.Mode.SHARED_CAP, cap = EncounterQuantityExpression.Trait("ESSENCE")
                ),
                EncounterDynamicCapResolver.DynamicContribution(
                    "b","BONUS",EncounterQuantityExpression.Constant(4),EncounterStackingSemantics.Mode.SHARED_CAP, cap = EncounterQuantityExpression.Trait("ESSENCE")
                )
            ),
            EncounterBuildQuantities(mapOf("ESSENCE" to 5))
        )
        assertEquals(5, result.knownTotal)
    }

    @Test
    fun `cap nao resolvido nao vira numero inventado`() {
        val result = EncounterDynamicCapResolver.resolve(
            listOf(
                EncounterDynamicCapResolver.DynamicContribution(
                    "a","BONUS",EncounterQuantityExpression.Constant(4),EncounterStackingSemantics.Mode.SHARED_CAP, cap = EncounterQuantityExpression.Trait("MISSING")
                )
            ),
            EncounterBuildQuantities(emptyMap())
        )
        assertNull(result.knownTotal)
    }
}
