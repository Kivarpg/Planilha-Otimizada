package com.example.data

import com.example.model.EncantoEncontro
import com.example.model.NOME_FEITICARIA_TERRESTRE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterSorceryAccessTest {
    private fun charm(nome: String) = EncantoEncontro(nome, "Ocultismo", "")

    @Test
    fun `detecta cada circulo uma unica vez a partir dos encantos`() {
        val acesso = EncounterSorceryAccess.from(listOf(
            charm(NOME_FEITICARIA_TERRESTRE),
            charm("Feitiçaria do Círculo Celestial"),
            charm("Feitiçaria do Círculo Solar")
        ))
        assertTrue(acesso.terrestre)
        assertTrue(acesso.celestial)
        assertTrue(acesso.solar)
    }

    @Test
    fun `sem encantos de feiticaria nao libera circulos`() {
        val acesso = EncounterSorceryAccess.from(listOf(charm("Outro Encanto")))
        assertFalse(acesso.terrestre)
        assertFalse(acesso.celestial)
        assertFalse(acesso.solar)
    }
}
