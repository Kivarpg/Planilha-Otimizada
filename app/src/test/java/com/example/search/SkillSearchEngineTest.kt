package com.example.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillSearchEngineTest {

    private fun sample(): List<SearchableSkill> = listOf(
        SearchableSkill(
            id = "shadow_step", namePt = "Passo das Sombras", nameEn = "Shadow Step",
            habilidade = "Furtividade", minHabilidade = 3, essencia = 2, tipo = "Suplementar",
            keywords = listOf("Uniforme"), duracao = "Instantânea"
        ),
        SearchableSkill(
            id = "shadow_sight", namePt = "Visão nas Sombras", nameEn = "Shadow Sight",
            habilidade = "Furtividade", minHabilidade = 2, essencia = 1, tipo = "Reflexivo",
            keywords = listOf("Perigoso"), duracao = "Uma cena"
        ),
        SearchableSkill(
            id = "shadow_strike", namePt = "Golpe das Sombras", nameEn = "Shadow Strike",
            habilidade = "Armas Brancas", minHabilidade = 4, essencia = 3, tipo = "Simples",
            keywords = listOf("Agravado", "Perigoso"), duracao = "Uma cena"
        ),
        SearchableSkill(
            id = "essencia_pura", namePt = "Essência Pura", nameEn = "Pure Essence",
            habilidade = "Ocultismo", minHabilidade = 5, essencia = 5, tipo = "Permanente",
            keywords = emptyList(), duracao = "Permanente"
        )
    )

    @Test fun buscaPorNomeEmPortugues() {
        val result = SkillSearchEngine.filterSkills(sample(), SkillFilter(query = "sombras"))
        assertEquals(3, result.size)
    }

    @Test fun buscaPorNomeEmIngles() {
        val result = SkillSearchEngine.filterSkills(sample(), SkillFilter(query = "shadow"))
        assertEquals(3, result.size)
        // não deve duplicar resultados mesmo casando em pt e en simultaneamente
        assertEquals(result.size, result.distinctBy { it.id }.size)
    }

    @Test fun buscaIgnoraAcentosEMaiusculas() {
        val result = SkillSearchEngine.filterSkills(sample(), SkillFilter(query = "ESSENCIA"))
        assertEquals(1, result.size)
        assertEquals("essencia_pura", result.first().id)
    }

    @Test fun buscaPorCorrespondenciaParcial() {
        val result = SkillSearchEngine.filterSkills(sample(), SkillFilter(query = "strike"))
        assertEquals(1, result.size)
        assertEquals("shadow_strike", result.first().id)
    }

    @Test fun filtroDeHabilidadeMultiplaUsaLogicaOu() {
        val result = SkillSearchEngine.filterSkills(
            sample(),
            SkillFilter(selectedSkillIds = setOf("Ocultismo", "Armas Brancas"))
        )
        assertEquals(setOf("essencia_pura", "shadow_strike"), result.map { it.id }.toSet())
    }

    @Test fun minsExatoRetornaApenasValorIgual() {
        val result = SkillSearchEngine.filterSkills(
            sample(),
            SkillFilter(minHabilidade = 3, minHabilidadeMode = NumericMatchMode.EXACT)
        )
        assertEquals(listOf("shadow_step"), result.map { it.id })
    }

    @Test fun minsAPartirDeRetornaMaiorOuIgual() {
        val result = SkillSearchEngine.filterSkills(
            sample(),
            SkillFilter(minHabilidade = 3, minHabilidadeMode = NumericMatchMode.AT_LEAST)
        )
        assertEquals(setOf("shadow_step", "shadow_strike", "essencia_pura"), result.map { it.id }.toSet())
    }

    @Test fun tipoComMultiplaSelecaoUsaLogicaOu() {
        val result = SkillSearchEngine.filterSkills(
            sample(),
            SkillFilter(types = setOf("Simples", "Reflexivo"))
        )
        assertEquals(setOf("shadow_sight", "shadow_strike"), result.map { it.id }.toSet())
    }

    @Test fun palavraChaveModoQualquerUma() {
        val result = SkillSearchEngine.filterSkills(
            sample(),
            SkillFilter(keywords = setOf("Perigoso", "Agravado"), keywordMode = KeywordMode.ANY)
        )
        assertEquals(setOf("shadow_sight", "shadow_strike"), result.map { it.id }.toSet())
    }

    @Test fun palavraChaveModoTodas() {
        val result = SkillSearchEngine.filterSkills(
            sample(),
            SkillFilter(keywords = setOf("Perigoso", "Agravado"), keywordMode = KeywordMode.ALL)
        )
        assertEquals(listOf("shadow_strike"), result.map { it.id })
    }

    @Test fun combinacaoDeGruposDiferentesUsaLogicaE() {
        // nome contém "sombras" AND mins >= 3 AND tipo IN [Suplementar, Reflexivo]
        val result = SkillSearchEngine.filterSkills(
            sample(),
            SkillFilter(
                query = "sombras",
                minHabilidade = 3,
                minHabilidadeMode = NumericMatchMode.AT_LEAST,
                types = setOf("Suplementar", "Reflexivo")
            )
        )
        assertEquals(listOf("shadow_step"), result.map { it.id })
    }

    @Test fun filtroVazioNaoAlteraResultados() {
        val result = SkillSearchEngine.filterSkills(sample(), SkillFilter())
        assertEquals(sample().size, result.size)
    }

    @Test fun nenhumResultadoQuandoFiltrosNaoCorrespondem() {
        val result = SkillSearchEngine.filterSkills(sample(), SkillFilter(query = "inexistente"))
        assertTrue(result.isEmpty())
    }

    @Test fun contagemDeFiltrosAtivos() {
        val filter = SkillFilter(query = "sombras", minHabilidade = 3, types = setOf("Simples"))
        assertEquals(3, filter.activeCount)
        assertTrue(SkillFilter().isEmpty)
    }
}
