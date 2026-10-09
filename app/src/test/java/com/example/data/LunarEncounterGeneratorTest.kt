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
    fun `lunar fisico nao valoriza encanto alto com prerequisito inacessivel`() {
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
            encanto("Forca bloqueada", "Força", 5, "Encanto inexistente"),
            encanto("Destreza inicial", "Destreza", 1, "Nenhum"),
            encanto("Destreza avancada", "Destreza", 2, "Destreza inicial")
        )
        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = catalogo,
            attributes = mapOf("Força" to 4, "Destreza" to 4, "Vigor" to 3),
            essencia = 1,
            ordemAtributos = listOf("Força", "Destreza", "Vigor"),
            quantidade = 0,
            random = Random(42),
            arquetipo = ArquetipoEncontro.FISICO
        )
        assertEquals("Destreza", resultado.ataqueEscolhido)
    }

    @Test
    fun `foco ofensivo explicito prevalece sobre pontuacao automatica`() {
        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = emptyList(),
            attributes = mapOf("Força" to 5, "Destreza" to 3, "Vigor" to 3),
            essencia = 1,
            ordemAtributos = listOf("Força", "Destreza", "Vigor"),
            quantidade = 0,
            random = Random(7),
            arquetipo = ArquetipoEncontro.FISICO,
            atributoFocoUsuario = "Destreza"
        )
        assertEquals("Destreza", resultado.ataqueEscolhido)
    }

    @Test
    fun `projecao Lunar considera rota arquetipo somente com traco espiritual habilitado`() {
        fun encanto(nome: String, atributo: String, essencia: Int, pre: String = "Nenhum") =
            EncantoLunarDefinition(
                id = nome, atributo = atributo, subdivisao = null,
                nome = nome, nomeIngles = "", custo = "1",
                minsTexto = "$atributo 3, Essência $essencia",
                minAtributo = 3, minEssencia = essencia,
                tipo = "Reflexivo", palavrasChave = "", duracao = "",
                preRequisitos = pre, descricao = ""
            )
        val base = encanto("Base destreza", "Destreza", 1)
        val avancado = encanto("Avancado destreza", "Destreza", 3, "Base destreza")
        val alternativo = encanto("Encanto de Percepcao", "Percepção", 5).copy(
            rotasArquetipo = listOf(
                LunarCharmArchetypeRoute("Destreza", "VISAO_NOTURNA", 3, "Avancado destreza")
            )
        )
        val catalogo = listOf(encanto("Base forca", "Força", 4), base, avancado, alternativo)
        fun escolher(tracos: Set<LunarSpiritTrait>) =
            LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
                catalogo = catalogo,
                attributes = mapOf("Força" to 4, "Destreza" to 4, "Percepção" to 4, "Vigor" to 3),
                essencia = 1,
                ordemAtributos = listOf("Força", "Destreza", "Vigor"),
                quantidade = 0,
                random = Random(7),
                arquetipo = ArquetipoEncontro.FISICO,
                spiritTraits = tracos
            ).ataqueEscolhido
        assertEquals("Destreza", escolher(setOf(LunarSpiritTrait.VISAO_NOTURNA)))
        assertEquals("Força", escolher(emptySet()))
    }

    @Test
    fun `lunar fisico limita Corpo de Touro inicial a uma aquisicao`() {
        val corpo = EncantoLunarDefinition(
            id = "corpo-touro", atributo = "Vigor", subdivisao = null,
            nome = com.example.model.NOME_CORPO_DE_TOURO, nomeIngles = "",
            custo = "1", minsTexto = "Vigor 1, Essência 1",
            minAtributo = 1, minEssencia = 1, tipo = "Permanente",
            palavrasChave = "", duracao = "Permanente",
            preRequisitos = "Nenhum", descricao = ""
        )
        val selecionados = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = listOf(corpo),
            attributes = mapOf("Força" to 4, "Destreza" to 3, "Vigor" to 5, "Inteligência" to 2),
            essencia = 1,
            ordemAtributos = listOf("Vigor", "Força", "Destreza"),
            quantidade = 15,
            random = Random(17),
            arquetipo = ArquetipoEncontro.FISICO
        ).charms
        assertEquals(1, selecionados.count { it.nome == com.example.model.NOME_CORPO_DE_TOURO })
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
