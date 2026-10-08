package com.example.data

import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EncounterSpecializationBoundaryTest {
    @Test
    fun `especialidades nunca usam habilidade em 1 mesmo quando priorizada`() {
        val abilities = mapOf(
            "Armas Brancas" to 1,
            "Esquiva" to 1,
            "Atletismo" to 4,
            "Resistência" to 3,
            "Integridade" to 2,
            "Percepção" to 0
        )
        val result = EncounterDistributionService.distribuirEspecialidades(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            habilidadeSocialOuMental = "Percepção",
            abilities = abilities,
            random = Random(7),
            habilidadesEstruturaisRelevantes = listOf("Armas Brancas", "Esquiva", "Atletismo")
        )
        assertTrue(result.all { (abilities[it.habilidade] ?: 0) >= 2 })
    }
}
