package com.example.data

import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterMoteServiceTest {
    @Test fun solarEssence1() {
        val r = EncounterMoteService.totais(TipoExaltadoEncontro.SOLAR, 1, null, null)
        assertEquals(13, r.pessoaisMax)
        assertEquals(33, r.perifericosMax)
        assertEquals(33, r.perifericosDisponiveis)
    }

    @Test fun dragonBloodedEssence2() {
        val r = EncounterMoteService.totais(TipoExaltadoEncontro.SANGUE_DE_DRAGAO, 2, null, null)
        assertEquals(13, r.pessoaisMax)
        assertEquals(31, r.perifericosMax)
        assertEquals(31, r.perifericosDisponiveis)
    }

    @Test fun lunarEssence1() {
        val r = EncounterMoteService.totais(TipoExaltadoEncontro.LUNAR, 1, null, null)
        assertEquals(16, r.pessoaisMax)
        assertEquals(38, r.perifericosMax)
        assertEquals(38, r.perifericosDisponiveis)
    }

    @Test fun dragonBloodedEssence2CommitmentIsSubtractedOnce() {
        val armadura = com.example.model.ArmaduraEncontro(
            nome = "Teste", peso = "Leve", absorcao = 0, dureza = 0,
            penalidadeMobilidade = 0, motesComitados = 5
        )
        val r = EncounterMoteService.totais(TipoExaltadoEncontro.SANGUE_DE_DRAGAO, 2, null, armadura)
        assertEquals(31, r.perifericosMax)
        assertEquals(5, r.comitados)
        assertEquals(26, r.perifericosDisponiveis)
    }
}
