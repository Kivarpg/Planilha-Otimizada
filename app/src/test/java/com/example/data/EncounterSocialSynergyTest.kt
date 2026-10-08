package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterSocialSynergyTest {
    @Test fun resolveDebuffPreparaAtaqueContraResolve() {
        val setup="reduz a Perseverança do alvo em dois pontos"
        val payoff="teste contra a Perseverança do alvo"
        assertTrue(EncounterSocialSynergy.pairAffinity(setup,payoff)>=24)
    }
    @Test fun appearanceBuffCombinaComEscalaDeAppearance() {
        assertTrue(EncounterSocialSynergy.pairAffinity("aumenta sua Aparência em um", "Aparência sobre a Perseverança concede sucessos automáticos")>=22)
    }
    @Test fun composicaoDeAcoesEhSinergiaForte() {
        val a="influência social é feita com um único teste, compartilhando os resultados do teste"
        val b="complementa uma ação de instilar ou persuadir de influência social"
        assertTrue(EncounterSocialSynergy.pairAffinity(a,b)>=24)
    }
    @Test fun scorePreClassificadoPreservaExatamenteScoreTextual() {
        val candidate="reduz a Perseverança do alvo e melhora influência social"
        val selected=listOf("teste contra a Perseverança do alvo", "dados de bônus em influência social")
        assertEquals(
            EncounterSocialSynergy.score(candidate,selected),
            EncounterSocialSynergy.scoreTags(EncounterSocialSynergy.tags(candidate),selected.map(EncounterSocialSynergy::tags))
        )
    }
}
