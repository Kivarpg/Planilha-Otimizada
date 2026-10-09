package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.LunarCasta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LunarEncounterGeneratorTest {
    @Test
    fun `lunar fisico prioriza cadeia ofensiva de Essencia alta sobre volume raso`() {
        fun encanto(nome: String, atributo: String, essencia: Int, requisito: String) =
            EncantoLunarDefinition(
                id = nome, atributo = atributo, subdivisao = null,
                nome = nome, nomeIngles = "", custo = "1",
                minsTexto = "$atributo 3, Essência $essencia",
                minAtributo = 3, minEssencia = essencia,
                tipo = "Reflexivo", palavrasChave = "", duracao = "",
                preRequisitos = requisito, descricao = ""
            )
        val catalogo = listOf(
            encanto("Forca inicial", "Força", 1, "Nenhum"),
            encanto("Forca intermediaria", "Força", 2, "Forca inicial"),
            encanto("Forca superior", "Força", 3, "Forca intermediaria")
        ) + (1..5).map { encanto("Destreza rasa $it", "Destreza", 1, "Nenhum") }
        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = catalogo,
            attributes = mapOf("Força" to 4, "Destreza" to 4, "Vigor" to 3),
            essencia = 1,
            ordemAtributos = listOf("Destreza", "Força", "Vigor"),
            quantidade = 0,
            random = Random(42),
            arquetipo = ArquetipoEncontro.FISICO
        )
        assertEquals("Força", resultado.ataqueEscolhido)
    }

    @Test
    fun `geracao Lunar fisica registra arvore ofensiva mesmo sem encantos disponiveis`() {
        repeat(EncounterTestSamples.count(30)) { seed ->
            val npc = EncounterGenerator.gerarLunar(
                nomeManual = "Lunar ofensivo",
                arquetipo = ArquetipoEncontro.FISICO,
                encantosLunares = emptyList(),
                random = Random(seed)
            )
            assertTrue(npc.lunarAtaqueEscolhido == "Força" || npc.lunarAtaqueEscolhido == "Destreza")
            assertEquals(null, npc.focoProgressaoExplicito)
            assertEquals(npc.lunarAtaqueEscolhido, npc.copy(xpAtual = 15).lunarAtaqueEscolhido)
        }
    }

    @Test
    fun `geracao Lunar nao fisica nao inventa arvore ofensiva`() {
        listOf(ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL).forEach { arquetipo ->
            val npc = EncounterGenerator.gerarLunar(
                nomeManual = "Lunar nao fisico",
                arquetipo = arquetipo,
                encantosLunares = emptyList(),
                random = Random(42)
            )
            assertEquals(null, npc.lunarAtaqueEscolhido)
        }
    }

    @Test
    fun `Lunar possui dois atributos de casta e dois favorecidos adicionais distintos`() {
        ArquetipoEncontro.entries.forEach { arquetipo ->
            repeat(EncounterTestSamples.count(100)) { seed ->
                val npc = EncounterGenerator.gerarLunar(
                    nomeManual = "Teste",
                    arquetipo = arquetipo,
                    encantosLunares = emptyList(),
                    random = Random(seed + arquetipo.ordinal * 1000)
                )

                assertEquals(2, npc.lunarAtributosCasta.size)
                assertEquals(2, npc.lunarAtributosCasta.distinct().size)
                assertEquals(2, npc.habilidadesFavorecidas.size)
                assertEquals(2, npc.habilidadesFavorecidas.distinct().size)
                assertTrue(npc.lunarAtributosCasta.toSet().intersect(npc.habilidadesFavorecidas.toSet()).isEmpty())
                assertEquals(4, (npc.lunarAtributosCasta + npc.habilidadesFavorecidas).distinct().size)

                val casta = LunarCasta.entries.first { it.displayName == npc.casta }
                assertTrue(npc.lunarAtributosCasta.all { it in casta.poolAtributosCasta() })
                assertTrue(npc.habilidadesFavorecidas.all { it !in npc.lunarAtributosCasta.toSet() })
            }
        }
    }
}
