package com.example.data

import com.example.model.TipoExaltadoEncontro
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterLimitCatalogTest {
    @Test fun `solar sorteia somente da tabela solar`() {
        repeat(100) {
            val limite = EncounterLimitCatalog.sortear(TipoExaltadoEncontro.SOLAR, "Alvorada", Random(it))
            assertTrue(limite in EncounterLimitCatalog.SOLAR)
            assertTrue(limite !in EncounterLimitCatalog.LUNAR)
        }
    }

    @Test fun `lunar sorteia somente da tabela lunar`() {
        repeat(100) {
            val limite = EncounterLimitCatalog.sortear(TipoExaltadoEncontro.LUNAR, "Lua Cheia", Random(it))
            assertTrue(limite in EncounterLimitCatalog.LUNAR)
            assertTrue(limite !in EncounterLimitCatalog.SOLAR)
        }
    }

    @Test fun `sangue de dragao nao possui limite`() {
        assertEquals("", EncounterLimitCatalog.sortear(TipoExaltadoEncontro.SANGUE_DE_DRAGAO, "Fogo", Random(1)))
        assertTrue(EncounterLimitCatalog.para(TipoExaltadoEncontro.SANGUE_DE_DRAGAO).isEmpty())
    }
}
