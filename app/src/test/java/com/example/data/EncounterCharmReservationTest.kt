package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica a regra de reserva sem depender de sorteios do gerador completo. */
class EncounterCharmReservationTest {
    @Test
    fun `quantidade base de encantos perde exatamente as vagas reservadas`() {
        assertEquals(14, EncounterGenerationRules.ENCANTOS_INICIAIS_COM_FEITICARIA_MENTAL)
        assertEquals(15, EncounterGenerationRules.ENCANTOS_INICIAIS)
        assertTrue(EncounterGenerationRules.ENCANTOS_INICIAIS_COM_FEITICARIA_MENTAL < EncounterGenerationRules.ENCANTOS_INICIAIS)
    }
}
