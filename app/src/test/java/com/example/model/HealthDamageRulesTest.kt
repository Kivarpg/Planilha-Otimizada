package com.example.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthDamageRulesTest {
    @Test
    fun naoPermitePularCaixaNaMesmaLinha() {
        val primeira = CaixaVitalidade(id = "1", penalidade = "-1")
        val segunda = CaixaVitalidade(id = "2", penalidade = "-1")
        val trilha = listOf(primeira, segunda)

        assertTrue(HealthDamageRules.podeMarcar(primeira, trilha))
        assertFalse(HealthDamageRules.podeMarcar(segunda, trilha))
    }

    @Test
    fun permiteProximaCaixaSomenteDepoisDaAnterior() {
        val primeira = CaixaVitalidade(id = "1", penalidade = "-1", tipoDano = 1)
        val segunda = CaixaVitalidade(id = "2", penalidade = "-1")
        val terceira = CaixaVitalidade(id = "3", penalidade = "-2")
        val trilha = listOf(primeira, segunda, terceira)

        assertTrue(HealthDamageRules.podeMarcar(segunda, trilha))
        assertFalse(HealthDamageRules.podeMarcar(terceira, trilha))
    }
}
