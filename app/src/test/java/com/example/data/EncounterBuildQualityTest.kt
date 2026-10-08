package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterBuildQualityTest {
    @Test
    fun `crescimento futuro e limitado para nao dominar utilidade atual`() {
        val score = EncounterBuildQuality.combine(current = 7, future = 99)
        assertEquals(7, score.current)
        assertEquals(EncounterBuildQuality.MAX_FUTURE_GROWTH_BONUS, score.future)
        assertEquals(10, score.total)
    }

    @Test
    fun `crescimento negativo nao pune valor atual`() {
        val score = EncounterBuildQuality.combine(current = 6, future = -5)
        assertEquals(0, score.future)
        assertEquals(6, score.total)
    }

    @Test
    fun `crescimento coerente participa do score`() {
        val score = EncounterBuildQuality.combine(current = 5, future = 2)
        assertEquals(7, score.total)
    }
    @Test
    fun `vantagem atual relevante supera apenas potencial futuro limitado`() {
        val forteAgora = EncounterBuildQuality.combine(current = 9, future = 0)
        val apenasPotencial = EncounterBuildQuality.combine(current = 5, future = 20)
        assertEquals(9, forteAgora.total)
        assertEquals(8, apenasPotencial.total)
    }

    @Test
    fun `potencial futuro pode desempatar opcoes atuais equivalentes`() {
        val continuidade = EncounterBuildQuality.combine(current = 6, future = 2)
        val isolado = EncounterBuildQuality.combine(current = 6, future = 0)
        assertEquals(8, continuidade.total)
        assertEquals(6, isolado.total)
    }


}
