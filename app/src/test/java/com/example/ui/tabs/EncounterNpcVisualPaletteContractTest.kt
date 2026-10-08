package com.example.ui.tabs

import com.example.model.TipoExaltadoEncontro
import com.example.ui.theme.visualTemplateParaExaltado
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterNpcVisualPaletteContractTest {
    @Test
    fun `cada tipo de exaltado possui identidade visual propria na aba 11`() {
        val solar = visualTemplateParaExaltado(TipoExaltadoEncontro.SOLAR)
        val dragao = visualTemplateParaExaltado(TipoExaltadoEncontro.SANGUE_DE_DRAGAO)
        val lunar = visualTemplateParaExaltado(TipoExaltadoEncontro.LUNAR)

        assertNotEquals(solar.accent, dragao.accent)
        assertNotEquals(solar.accent, lunar.accent)
        assertNotEquals(dragao.accent, lunar.accent)
        assertNotEquals(dragao.surfaceVariant, lunar.surfaceVariant)
    }
    @Test
    fun `listrado de acoes usa a paleta do proprio npc e mantem contraste`() {
        listOf(
            TipoExaltadoEncontro.SOLAR,
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            TipoExaltadoEncontro.LUNAR
        ).forEach { tipo ->
            val template = visualTemplateParaExaltado(tipo)
            val linhaPar = corZebrada(0, template)
            val linhaImpar = corZebrada(1, template)

            assertNotEquals(linhaPar, linhaImpar)
            assertEquals(template.surface.copy(alpha = 0.96f), linhaPar)
            assertEquals(template.surfaceVariant.copy(alpha = 0.58f), linhaImpar)
        }
    }
}

