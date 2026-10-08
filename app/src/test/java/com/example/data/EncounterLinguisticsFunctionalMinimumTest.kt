package com.example.data

import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EncounterLinguisticsFunctionalMinimumTest {
    @Test fun `minimo funcional garante Linguistica um preservando 28 pontos`() {
        val r = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            supernal = null,
            habilidadesFavorecidas = emptyList(),
            random = Random(527),
            habilidadesMinimoUm = setOf("Linguística")
        )
        assertTrue((r.abilities["Linguística"] ?: 0) >= 1)
        assertEquals(28, r.abilities.values.sum())
    }
}
