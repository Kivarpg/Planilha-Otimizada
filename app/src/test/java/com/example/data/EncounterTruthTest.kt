package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterTruthTest {
    

    @Test
    fun `not unknown continua unknown`() {
        assertEquals(EncounterTruth.UNKNOWN,EncounterTruth.UNKNOWN.not())
    }

    @Test
    fun `and false domina unknown`() {
        assertEquals(EncounterTruth.FALSE,EncounterTruth.UNKNOWN and EncounterTruth.FALSE)
    }

    @Test
    fun `or true domina unknown`() {
        assertEquals(EncounterTruth.TRUE,EncounterTruth.UNKNOWN or EncounterTruth.TRUE)
    }

    @Test
    fun `fato nao extraido nao vira false`() {
        val r=EncounterTriRequirement.Fact("HAS_AURA")
        assertEquals(EncounterTruth.UNKNOWN,r.evaluate(emptySet(),emptySet()))
    }
}
