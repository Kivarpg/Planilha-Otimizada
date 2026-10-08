package com.example.data

import com.example.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExaltedCharacterExchangeTest {
    @Test fun exportacaoResolveIdCanonicoEPreservaFeiticoInicial() {
        val charm = EncantoSolarDefinition(
            id = "solar-1", habilidade = "Briga", nome = "Punho Solar", nomeIngles = "",
            custo = "3m", minsTexto = "", minHabilidade = 1, minEssencia = 1,
            tipo = "Suplementar", palavrasChave = "", duracao = "",
            preRequisitos = "", descricao = ""
        )
        val spell = FeiticoDefinition(
            id = "spell-1", circulo = "Terrestre", nome = "Feitiço Teste",
            nomeIngles = "", custo = "", palavrasChave = "", duracao = "",
            livro = "", descricao = ""
        )
        val catalog = PreparedEncounterCatalog.prepare(
            solares = listOf(charm), feiticos = listOf(spell)
        )
        val npc = NpcEncontro(
            nome = "Exportado",
            charms = listOf(EncantoEncontro("Punho Solar", "Briga", "3m")),
            feiticos = listOf(FeiticoEncontro("Feitiço Teste", "Terrestre", "")),
            feiticoInicialNome = "Feitiço Teste",
            primeiroXpRecebido = true
        )
        val exchange = ExaltedCharacterExchangeMapper.fromNpc(npc, catalog)
        assertEquals("solar.charm:solar-1", exchange.charms.single().canonicalId)
        assertEquals("spell:spell-1", exchange.spells.single().canonicalId)
        assertTrue(exchange.spells.single().initial)

        val json = JSONObject(ExaltedCharacterExchangeJson.encode(exchange))
        assertEquals(ExaltedCharacterExchange.CURRENT_SCHEMA_VERSION, json.getInt("schemaVersion"))
        assertEquals("Exportado", json.getString("name"))
    }

    @Test fun nomeAmbiguoNaoProduzIdCanonicoArbitrario() {
        val a = EncantoSolarDefinition(
            id = "a", habilidade = "Briga", nome = "Mesmo Nome", nomeIngles = "",
            custo = "", minsTexto = "", minHabilidade = 1, minEssencia = 1,
            tipo = "", palavrasChave = "", duracao = "", preRequisitos = "", descricao = ""
        )
        val b = a.copy(id = "b")
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(a, b))
        val npc = NpcEncontro(charms = listOf(EncantoEncontro("Mesmo Nome", "Briga", "")))
        assertNull(ExaltedCharacterExchangeMapper.fromNpc(npc, catalog).charms.single().canonicalId)
    }
}
