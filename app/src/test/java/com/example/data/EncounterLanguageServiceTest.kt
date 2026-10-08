package com.example.data

import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EncounterLanguageServiceTest {
    @Test
    fun `idioma inicial nunca e Antigo Reino`() {
        repeat(EncounterTestSamples.count(200)) {
            val idiomaSolar = EncounterLanguageService.selecionar(TipoExaltadoEncontro.SOLAR, random = Random(it))
            val idiomaLunar = EncounterLanguageService.selecionar(TipoExaltadoEncontro.LUNAR, random = Random(it))
            assertNotEquals("Antigo Reino", idiomaSolar)
            assertNotEquals("Antigo Reino", idiomaLunar)
        }
    }

    @Test
    fun `Sangue de Dragao do Imperio sempre usa Alto Reino`() {
        repeat(20) {
            assertEquals(
                "Alto Reino",
                EncounterLanguageService.selecionar(
                    TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
                    OrigemNomeSangueDeDragao.IMPERIO,
                    Random(it)
                )
            )
        }
    }

    @Test
    fun `Sangue de Dragao de Lookshy sempre usa Dialeto dos Rios`() {
        repeat(20) {
            assertEquals(
                "Dialeto dos Rios",
                EncounterLanguageService.selecionar(
                    TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
                    OrigemNomeSangueDeDragao.LOOKSHY,
                    Random(it)
                )
            )
        }
    }

    @Test
    fun `demais idiomas iniciais pertencem ao catalogo permitido`() {
        val opcoes = EncounterLanguageService.opcoesIniciais().toSet()
        repeat(EncounterTestSamples.count(200)) {
            val idioma = EncounterLanguageService.selecionar(
                TipoExaltadoEncontro.SOLAR,
                random = Random(it)
            )
            assertTrue(idioma in opcoes)
        }
    }
}
