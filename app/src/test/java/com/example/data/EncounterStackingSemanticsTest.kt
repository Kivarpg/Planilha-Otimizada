package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterStackingSemanticsTest {
    

    @Test
    fun `max of nao soma efeitos`() {
        val result = EncounterStackingSemantics.resolve(listOf(
            EncounterStackingSemantics.Contribution("a","DEFENSE",2,EncounterStackingSemantics.Mode.MAX_OF),
            EncounterStackingSemantics.Contribution("b","DEFENSE",3,EncounterStackingSemantics.Mode.MAX_OF)
        ))
        assertEquals(3, result.knownTotal)
        assertEquals(setOf("b"), result.realizedIds)
    }

    @Test
    fun `shared cap limita soma`() {
        val result = EncounterStackingSemantics.resolve(listOf(
            EncounterStackingSemantics.Contribution("a","BONUS_DICE",3,EncounterStackingSemantics.Mode.SHARED_CAP,cap=5),
            EncounterStackingSemantics.Contribution("b","BONUS_DICE",4,EncounterStackingSemantics.Mode.SHARED_CAP,cap=5)
        ))
        assertEquals(5, result.knownTotal)
    }

    @Test
    fun `regra desconhecida nao inventa stacking`() {
        val result = EncounterStackingSemantics.resolve(listOf(
            EncounterStackingSemantics.Contribution("a","SOAK",2,EncounterStackingSemantics.Mode.UNKNOWN)
        ))
        assertTrue(result.uncertain)
        assertEquals(null, result.knownTotal)
    }
}
