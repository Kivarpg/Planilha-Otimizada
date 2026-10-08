package com.example.data

import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCpuSafetyBudgetTest {
    private fun encanto(nome: String) = EncantoSolarDefinition(
        id = nome, habilidade = "Armas Brancas", nome = nome, nomeIngles = nome,
        custo = "1", minsTexto = "Armas Brancas 1, Essência 1",
        minHabilidade = 1, minEssencia = 1, tipo = "Encanto",
        palavrasChave = "", duracao = "Permanente", preRequisitos = "", descricao = ""
    )

    @Test
    fun `route optimizer encerra com orcamento artificialmente pequeno`() {
        val catalogo = (1..40).map { encanto("E$it") }
        var callbacks = 0
        val escolhido = EncounterCharmRouteOptimizer.escolher(
            candidatos = catalogo,
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> callbacks++; true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            beamWidth = 6,
            profundidade = 3,
            maxWorkUnits = 12
        )
        assertTrue(callbacks <= 12)
        assertTrue(escolhido == null || escolhido in catalogo)
    }

    @Test
    fun `planejadores possuem tetos deterministas positivos`() {
        assertTrue(EncounterBuildPlanner.DEFAULT_MAX_CANDIDATE_EVALUATIONS > 0)
        assertTrue(EncounterCharmRouteOptimizer.DEFAULT_MAX_WORK_UNITS > 0)
    }
}
