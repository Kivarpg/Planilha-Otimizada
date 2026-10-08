package com.example.data

import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.*
import org.junit.Test

class PreparedEncounterCatalogTest {
    private fun solar(id: String, nome: String) = EncantoSolarDefinition(
        id = id,
        habilidade = "Briga",
        nome = nome,
        nomeIngles = "",
        custo = "",
        minsTexto = "Briga 1, Essência 1",
        minHabilidade = 1,
        minEssencia = 1,
        tipo = "Reflexivo",
        palavrasChave = "",
        duracao = "",
        preRequisitos = "",
        descricao = ""
    )

    private fun lunar(id: String, nome: String) = EncantoLunarDefinition(
        id = id,
        atributo = "Destreza",
        subdivisao = null,
        nome = nome,
        nomeIngles = "",
        custo = "",
        minsTexto = "Destreza 1, Essência 1",
        minAtributo = 1,
        minEssencia = 1,
        tipo = "Reflexivo",
        palavrasChave = "",
        duracao = "",
        preRequisitos = "",
        descricao = ""
    )

    @Test fun idsIguaisEmNamespacesDiferentesNaoColidem() {
        val catalog = PreparedEncounterCatalog.prepare(
            solares = listOf(solar("mesmo_id", "Solar")),
            lunares = listOf(lunar("mesmo_id", "Lunar"))
        )
        assertNotNull(catalog.find(PreparedEncounterCatalog.StableContentId("solar.charm", "mesmo_id")))
        assertNotNull(catalog.find(PreparedEncounterCatalog.StableContentId("lunar.charm", "mesmo_id")))
        assertEquals(2, catalog.byId.size)
    }

    @Test fun nomeNaoViraIdentidade() {
        val catalog = PreparedEncounterCatalog.prepare(
            solares = listOf(solar("a", "Mesmo Nome")),
            lunares = listOf(lunar("b", "Mesmo Nome"))
        )
        assertEquals(2, catalog.findByName(" mesmo   nome ").size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun idDuplicadoNoMesmoCatalogoFalhaCedo() {
        PreparedEncounterCatalog.prepare(
            solares = listOf(solar("duplicado", "A"), solar("duplicado", "B"))
        )
    }

    @Test fun indicePorTipoPreservaDominios() {
        val catalog = PreparedEncounterCatalog.prepare(
            solares = listOf(solar("s", "S")),
            lunares = listOf(lunar("l", "L"))
        )
        assertEquals(1, catalog.charmsByExaltType[TipoExaltadoEncontro.SOLAR]?.size)
        assertEquals(1, catalog.charmsByExaltType[TipoExaltadoEncontro.LUNAR]?.size)
        assertNull(catalog.charmsByExaltType[TipoExaltadoEncontro.SANGUE_DE_DRAGAO])
    }
}
