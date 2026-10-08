package com.example.data

import com.example.model.EspecialidadeEncontro
import kotlin.test.Test
import kotlin.test.assertEquals

class EncounterSpecialityPbTest {
    @Test
    fun `especialidade adicional usa maior habilidade e ignora as que ja possuem especialidade`() {
        val existentes = listOf(
            EspecialidadeEncontro("Armas Brancas"),
            EspecialidadeEncontro("Integridade")
        )
        val abilities = mapOf(
            "Armas Brancas" to 5,
            "Integridade" to 5,
            "Presença" to 4,
            "Socialização" to 5,
            "Investigação" to 3
        )

        val resultado = EncounterDistributionService.adicionarEspecialidadeAdicional(
            especialidades = existentes,
            abilities = abilities,
            ordemPrioridade = listOf("Presença", "Socialização", "Investigação"),
            necessaria = true
        )

        assertEquals(listOf("Armas Brancas", "Integridade", "Socialização"), resultado.map { it.habilidade })
    }
}
