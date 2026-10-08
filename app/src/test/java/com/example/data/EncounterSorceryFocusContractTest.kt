package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterSorceryFocusContractTest {
    @Test
    fun `feiticaria explicita e uma diretiva estrutural e nao uma habilidade`() {
        val c = EncounterCustomization(
            foco = ENCOUNTER_FOCUS_SORCERY,
            focoMode = EncounterCustomizationMode.EXPLICITO
        )
        assertTrue(c.rotaFeiticaria)
        assertEquals(ENCOUNTER_FOCUS_SORCERY, c.focoExplicito)
        assertFalse(ENCOUNTER_FOCUS_SORCERY in com.example.model.ExaltedConstants.ALL_25_ABILITIES)
        assertFalse(ENCOUNTER_FOCUS_SORCERY in com.example.model.ExaltedConstants.ALL_ATTRIBUTES)
    }

    @Test
    fun `feiticaria automatica nao equivale a foco explicito`() {
        val c = EncounterCustomization()
        assertFalse(c.rotaFeiticaria)
    }

    @Test
    fun `nenhum nunca ativa rota de feiticaria`() {
        val c = EncounterCustomization(
            foco = ENCOUNTER_FOCUS_SORCERY,
            focoMode = EncounterCustomizationMode.NENHUM
        )
        assertFalse(c.rotaFeiticaria)
    }

    @Test
    fun `foco de feiticaria usa ancora mecanica sem vazar nome especial`() {
        val c = EncounterCustomization(
            foco = ENCOUNTER_FOCUS_SORCERY,
            focoMode = EncounterCustomizationMode.EXPLICITO
        )
        assertEquals("Ocultismo", c.focoMecanico(null, "Ocultismo"))
        assertEquals("Inteligência", c.focoMecanico(null, "Inteligência"))
    }

}