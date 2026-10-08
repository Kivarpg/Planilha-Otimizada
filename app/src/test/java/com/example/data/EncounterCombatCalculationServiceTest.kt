package com.example.data

import com.example.model.ArmaEncontro
import com.example.model.ArmaduraEncontro
import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterCombatCalculationServiceTest {
    private val armadura = ArmaduraEncontro("Teste", "Leve", absorcao = 2, dureza = 1, penalidadeMobilidade = 2, motesComitados = 0)
    private val arma = ArmaEncontro("Teste", "Leve", "Artefato", precisao = 2, dano = 14, defesa = 1)

    @Test
    fun fisicoArmasBrancasSomaForcaEDanoDaArma() {
        val resultado = EncounterCombatCalculationService.calcular(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            attributes = mapOf("Destreza" to 5, "Força" to 4),
            abilities = mapOf("Armas Brancas" to 5, "Esquiva" to 3),
            arma = arma,
            armadura = armadura
        )

        assertEquals(13, resultado.acaoPrincipal)
        assertEquals(11, resultado.acaoDecisiva)
        assertEquals(7, resultado.defesaPrimaria)
        assertEquals(3, resultado.esquiva)
        assertEquals("18L", resultado.dano)
    }

    @Test
    fun fisicoArremessoPreencheApararComBrigaZeroECalculaEvasao() {
        val resultado = EncounterCombatCalculationService.calcular(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Arremesso",
            attributes = mapOf("Destreza" to 5, "Força" to 4),
            abilities = mapOf("Arremesso" to 4, "Esquiva" to 3),
            arma = arma,
            armadura = armadura
        )

        assertEquals(10, resultado.acaoPrincipal)
        assertEquals(10, resultado.acaoDecisiva)
        // Aparar = ceil((Destreza 5 + Briga 0 + Especialização 1) / 2) + Defesa da Arma 1 = 4.
        assertEquals(4, resultado.defesaPrimaria)
        assertEquals(3, resultado.esquiva)
        assertEquals("14L", resultado.dano)
    }

    @Test
    fun socialPreencheApararComBrigaMesmoZeroEEvasao() {
        val resultado = EncounterCombatCalculationService.calcular(
            arquetipo = ArquetipoEncontro.SOCIAL,
            habilidadeCombate = "Presença",
            attributes = mapOf("Destreza" to 4),
            abilities = mapOf("Presença" to 3, "Esquiva" to 2, "Briga" to 0),
            arma = null,
            armadura = armadura
        )

        assertEquals(3, resultado.defesaPrimaria)
        // Esquiva = ceil((Destreza 4 + Esquiva 2 + Especialização 1) / 2) - Penalidade de Mobilidade 2 = 4 - 2 = 2.
        assertEquals(2, resultado.esquiva)
    }

    @Test
    fun mentalPreencheApararComBrigaMesmoZeroEEvasao() {
        val resultado = EncounterCombatCalculationService.calcular(
            arquetipo = ArquetipoEncontro.MENTAL,
            habilidadeCombate = "Ocultismo",
            attributes = mapOf("Destreza" to 5),
            abilities = mapOf("Ocultismo" to 3, "Esquiva" to 0, "Briga" to 0),
            arma = null,
            armadura = armadura
        )

        assertEquals(3, resultado.defesaPrimaria)
        assertEquals(1, resultado.esquiva)
    }
}
