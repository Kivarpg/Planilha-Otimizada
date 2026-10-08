package com.example.data

import com.example.model.ArmaduraEncontro
import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterDerivedStatsServiceTest {
    private val armadura = ArmaduraEncontro("Teste", "Leve", absorcao = 2, dureza = 1, penalidadeMobilidade = 1, motesComitados = 0)

    @Test
    fun recalculoComNovaHabilidadeAtualizaTodosOsDerivadosDependentes() {
        val base = EncounterDerivedStatsService.calcularDerivadosComuns(
            attributes = mapOf("Destreza" to 5, "Vigor" to 4, "Raciocínio" to 3, "Manipulação" to 3),
            abilities = mapOf("Prontidão" to 1, "Atletismo" to 2, "Esquiva" to 2, "Integridade" to 1, "Socialização" to 1),
            armadura = armadura,
            bonusPerseveranca = 0,
            bonusAstucia = 0,
            bonusJuntarBatalha = 1
        )
        val melhorada = EncounterDerivedStatsService.calcularDerivadosComuns(
            attributes = mapOf("Destreza" to 5, "Vigor" to 4, "Raciocínio" to 3, "Manipulação" to 3),
            abilities = mapOf("Prontidão" to 2, "Atletismo" to 3, "Esquiva" to 3, "Integridade" to 2, "Socialização" to 2),
            armadura = armadura,
            bonusPerseveranca = 1,
            bonusAstucia = 1,
            bonusJuntarBatalha = 1,
            bonusInvestida = 1,
            bonusDesengajamento = 1
        )

        assertEquals(5, base.juntarABatalha)
        assertEquals(6, melhorada.juntarABatalha)
        assertEquals(4, base.investida)
        assertEquals(5, melhorada.investida)
        assertEquals(4, base.desengajamento)
        assertEquals(5, melhorada.desengajamento)
        assertEquals(2, base.perseveranca)
        assertEquals(3, melhorada.perseveranca)
        assertEquals(2, base.astucia)
        assertEquals(3, melhorada.astucia)
    }
}
