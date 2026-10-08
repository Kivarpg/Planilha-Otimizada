package com.example.data

import com.example.model.CaixaVitalidade
import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterExperienceVitalityTest {
    @Test
    fun aoAumentarTrilhaDanoExistenteECompactadoParaInicio() {
        val antigas = listOf(
            CaixaVitalidade(id = "a", penalidade = "-0", tipoDano = 0),
            CaixaVitalidade(id = "b", penalidade = "-1", tipoDano = 1),
            CaixaVitalidade(id = "c", penalidade = "-2", tipoDano = 2)
        )
        val novas = listOf(
            CaixaVitalidade(id = "n1", penalidade = "-0"),
            CaixaVitalidade(id = "n2", penalidade = "-1"),
            CaixaVitalidade(id = "n3", penalidade = "-1"),
            CaixaVitalidade(id = "n4", penalidade = "-2")
        )

        val resultado = EncounterExperienceService.recalcularVitalidadePreservandoDano(antigas, novas)

        assertEquals(listOf(1, 2, 0, 0), resultado.map { it.tipoDano })
        assertEquals(listOf("a", "b", "c", "n4"), resultado.map { it.id })
    }
}
