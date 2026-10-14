package com.example.data

import com.example.model.ArquetipoEncontro
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class EncounterFutureGrowthPlannerTest {
    private fun node(name: String, essence: Int = 1, prereq: String = "") =
        EncounterFutureGrowthPlanner.Node(name, essence, listOf(EncounterPrerequisiteParser.parse(prereq)))

    @Test fun `complete high Essence chain outranks an isolated root`() {
        val result = EncounterFutureGrowthPlanner.plan(listOf(node("A isolated"), node("Z root"),
            node("Middle", 3, "Z root"), node("Target", 5, "Middle")))
        assertFalse(result.budgetExhausted)
        assertTrue(result.scores.getValue("Z root") > result.scores.getValue("A isolated"))
    }

    @Test fun `unknown missing cyclic or statically impossible routes earn no speculative bonus`() {
        val result = EncounterFutureGrowthPlanner.plan(listOf(node("Root"), node("Missing", 5, "Unknown"),
            node("Cycle A", 5, "Cycle B"), node("Cycle B", 5, "Cycle A"),
            EncounterFutureGrowthPlanner.Node("Impossible minimum", 5, emptyList()),
            node("Aggregate", 5, "quaisquer cinco Encantos de Briga")))
        assertEquals(setOf("Root"), result.scores.keys)
    }

    @Test fun `all conjunctive prerequisites and shortest legal alternative are planned`() {
        val result = EncounterFutureGrowthPlanner.plan(listOf(node("A"), node("B"),
            node("Target", 5, "A; Unknown ou B")))
        assertTrue(result.scores.getValue("A") >= 500)
        assertTrue(result.scores.getValue("B") >= 500)
    }

    @Test fun `incomplete budget never biases scores toward earlier catalog entries`() {
        val result = EncounterFutureGrowthPlanner.plan((1..20).map { node("Root $it") }, maxWork = 3)
        assertTrue(result.budgetExhausted)
        assertTrue(result.scores.isEmpty())
    }

    @Test fun `equal future potential favors deepening a legal branch over another root`() {
        val result = EncounterFutureGrowthPlanner.plan(listOf(node("Root A"), node("Root B"),
            node("Middle", 1, "Root A"), node("Target A", 5, "Middle"), node("Target B", 5, "Root B")))
        assertTrue(result.scores.getValue("Middle") > result.scores.getValue("Root B"))
        assertEquals(result.scores.getValue("Root A"), result.scores.getValue("Root B"))
    }

    private fun solar(name: String, essence: Int = 1, pre: String = "") = EncantoSolarDefinition(
        id = name, habilidade = "Briga", nome = name, nomeIngles = name, custo = "", minsTexto = "",
        minHabilidade = 3, minEssencia = essence, tipo = "Reflexivo", palavrasChave = "", duracao = "",
        preRequisitos = pre, descricao = "")

    @Test fun `Solar and converted Dragon selectors prepare high Essence path without buying target early`() {
        val defs = listOf(solar("A isolated"), solar("Z root"), solar("Target", 5, "Z root"))
        val dragons = defs.map { EncantoSangueDeDragaoDefinition(it.id, it.habilidade, it.nome,
            it.nomeIngles, it.custo, it.minsTexto, it.minHabilidade, it.minEssencia, it.tipo,
            it.palavrasChave, it.duracao, it.preRequisitos, it.descricao) }
        for (catalog in listOf(defs, DragonBloodedEncounterGenerator.catalogoConvertidoPreparado(dragons))) {
            val selected = EncounterCharmSelectionService.selecionarIniciais(catalog, mapOf("Briga" to 5),
                1, listOf("Briga"), 1, habilidadePrincipalArquetipo = "Briga")
            assertEquals(listOf("Z root"), selected.map { it.nome })
            assertTrue(selected.all { it.minEssencia <= 1 })
        }
    }

    @Test fun `Lunar plans only routes with satisfied Attribute and spirit minima`() {
        val defs = listOf(solar("A isolated"), solar("Z root"), solar("Target", 5, "Z root")).map {
            EncantoLunarDefinition(it.id, "Destreza", null, it.nome, it.nomeIngles, it.custo,
                it.minsTexto, it.minHabilidade, it.minEssencia, it.tipo, it.palavrasChave,
                it.duracao, it.preRequisitos, it.descricao)
        }
        val selected = LunarEncounterCharmSelection.selecionarEncantosIniciais(defs,
            mapOf("Destreza" to 5), 1, listOf("Destreza"), 1, Random(7), ArquetipoEncontro.FISICO,
            atributoFocoUsuario = "Destreza")
        assertEquals(listOf("Z root"), selected.map { it.nome })
        assertTrue(selected.all { it.minEssencia <= 1 })
    }
}
