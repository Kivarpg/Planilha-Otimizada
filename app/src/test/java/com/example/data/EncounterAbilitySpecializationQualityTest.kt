package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.EspecialidadeEncontro
import com.example.model.ExaltedConstants
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncounterAbilitySpecializationQualityTest {
    @Test
    fun `Lunar evita rating 1 quando 28 pontos podem ser distribuidos como blocos 3 2`() {
        repeat(64) { seed ->
            val r = EncounterDistributionService.distribuirHabilidades(
                arquetipo = ArquetipoEncontro.FISICO,
                habilidadeCombate = "Armas Brancas",
                habilidadeDefensivaObrigatoria = "Esquiva",
                supernal = null,
                habilidadesFavorecidas = emptyList(),
                random = Random(seed)
            )
            assertEquals(28, r.abilities.values.sum())
            assertTrue(r.abilities.values.none { it == 1 }, "seed=$seed criou rating 1 evitável")
        }
    }

    @Test
    fun `especialidades nunca usam habilidade com apenas um circulo`() {
        val abilities = ExaltedConstants.ALL_25_ABILITIES.associateWith { 0 }.toMutableMap().apply {
            this["Armas Brancas"] = 3
            this["Esquiva"] = 2
            this["Presença"] = 1
            this["Socialização"] = 3
            this["Integridade"] = 1
            this["Ocultismo"] = 2
        }
        val specs = EncounterDistributionService.distribuirEspecialidades(
            arquetipo = ArquetipoEncontro.SOCIAL,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            habilidadeSocialOuMental = "Presença",
            abilities = abilities,
            random = Random(42),
            habilidadesEstruturaisRelevantes = listOf("Ocultismo")
        )
        assertTrue(specs.all { (abilities[it.habilidade] ?: 0) >= 2 })
        assertTrue(specs.none { it.habilidade == "Presença" || it.habilidade == "Integridade" })
    }

    @Test
    fun `especialidade adicional ignora habilidade rating 1`() {
        val result = EncounterDistributionService.adicionarEspecialidadeAdicional(
            especialidades = listOf(EspecialidadeEncontro("Armas Brancas")),
            abilities = mapOf("Armas Brancas" to 3, "Presença" to 1, "Ocultismo" to 2),
            ordemPrioridade = listOf("Presença", "Ocultismo"),
            necessaria = true
        )
        assertEquals(listOf("Armas Brancas", "Ocultismo"), result.map { it.habilidade })
    }
}
