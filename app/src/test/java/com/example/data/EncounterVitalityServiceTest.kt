package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterVitalityServiceTest {
    @Test
    fun solarMantemTrilhaBaseERegraDeCorpoDeTouro() {
        val caixas = EncounterVitalityService.trilhaVitalidadeSolar(vigor = 3, corpoDeTouroCount = 1)
        assertEquals(10, caixas.size)
        assertEquals(listOf("-0", "-1", "-1", "-2", "-2", "-4", "Inc", "-1", "-2", "-2"), caixas.map { it.penalidade })
    }

    @Test
    fun sangueDeDragaoUsaRegraPropriaSemDuplicacao() {
        val caixas = EncounterVitalityService.trilhaVitalidadeSangueDeDragao(vigor = 3, corpoDeTouroCount = 1)
        assertEquals(9, caixas.size)
        assertEquals(listOf("-0", "-1", "-1", "-2", "-2", "-4", "Inc", "-1", "-2"), caixas.map { it.penalidade })
    }

    @Test
    fun naoPermiteMarcarPenalidadeSeguinteEnquantoAnteriorTemCaixaLivre() {
        val trilha = EncounterVitalityService.trilhaVitalidadeSolar(vigor = 3, corpoDeTouroCount = 0)
        val menosDois = trilha.filter { it.penalidade == "-2" }
        val comUmMenosUm = trilha.map {
            if (it.penalidade == "-1" && it === trilha.first { b -> b.penalidade == "-1" }) it.copy(tipoDano = 1) else it
        }
        val alvo = menosDois.first()
        assertEquals(false, com.example.model.HealthDamageRules.podeMarcar(alvo, comUmMenosUm))
    }

    @Test
    fun permiteAvancarQuandoTodasAsCaixasDaPenalidadeAnteriorEstaoMarcadas() {
        val trilha = EncounterVitalityService.trilhaVitalidadeSolar(vigor = 3, corpoDeTouroCount = 0)
        // A progressão é cumulativa: antes de -2, inclusive a linha de -0
        // precisa estar completamente preenchida.
        val menosUmEMenosZeroMarcados = trilha.map {
            if (it.penalidade == "-0" || it.penalidade == "-1") it.copy(tipoDano = 1) else it
        }
        val alvo = menosUmEMenosZeroMarcados.first { it.penalidade == "-2" }
        assertEquals(true, com.example.model.HealthDamageRules.podeMarcar(alvo, menosUmEMenosZeroMarcados))
    }

    @Test
    fun corpoDeTouroContinuaContandoComoCaixasDaPropriaPenalidade() {
        val trilha = EncounterVitalityService.trilhaVitalidadeSolar(vigor = 3, corpoDeTouroCount = 1)
        val menosUm = trilha.filter { it.penalidade == "-1" }
        val menosDois = trilha.first { it.penalidade == "-2" }
        val marcadaParcialmente = trilha.map {
            if (it.id == menosUm.first().id) it.copy(tipoDano = 1) else it
        }
        assertEquals(false, com.example.model.HealthDamageRules.podeMarcar(menosDois, marcadaParcialmente))
        val todasMarcadas = trilha.map {
            if (it.penalidade == "-0" || it.penalidade == "-1") it.copy(tipoDano = 1) else it
        }
        assertEquals(true, com.example.model.HealthDamageRules.podeMarcar(menosDois, todasMarcadas))
    }

    @Test
    fun progressaoExigeTodasAsLinhasAnterioresComCorpoDeTouro() {
        val trilha = EncounterVitalityService.trilhaVitalidadeSolar(vigor = 3, corpoDeTouroCount = 1)
        val alvoMenosDois = trilha.first { it.penalidade == "-2" }

        // Ainda existe -0 livre: -2 não pode ser iniciado.
        val semMenosZero = trilha.map {
            if (it.penalidade == "-1") it.copy(tipoDano = 1) else it
        }
        assertEquals(false, com.example.model.HealthDamageRules.podeMarcar(alvoMenosDois, semMenosZero))

        // Preenche -0 e todos os -1, incluindo o -1 adicional de Corpo de Touro.
        val linhasAnterioresCompletas = trilha.map {
            if (it.penalidade == "-0" || it.penalidade == "-1") it.copy(tipoDano = 1) else it
        }
        assertEquals(true, com.example.model.HealthDamageRules.podeMarcar(alvoMenosDois, linhasAnterioresCompletas))
    }
    @Test
    fun lunarComVigor5E5CorposDeTouroAdicionaDezMenosDoisEDezMenosQuatro() {
        val trilha = EncounterVitalityService.trilhaVitalidadeLunar(vigor = 5, corpoDeTouroCount = 5)

        assertEquals(27, trilha.size)
        assertEquals(10, trilha.count { !it.isPermanente && it.penalidade == "-2" })
        assertEquals(10, trilha.count { !it.isPermanente && it.penalidade == "-4" })
        assertEquals(1, trilha.count { it.penalidade == "-0" })
        assertEquals(0, trilha.count { !it.isPermanente && it.penalidade == "-0" })
        assertEquals(0, trilha.count { !it.isPermanente && it.penalidade == "-1" })
    }

    @Test
    fun lunarGeradorUsaRegraDeVigor5DoCorpoDeTouro() {
        val trilha = LunarEncounterGenerator.trilhaVitalidadePorVigor(vigor = 5, corpoDeTouroCount = 5)

        assertEquals(10, trilha.count { !it.isPermanente && it.penalidade == "-2" })
        assertEquals(10, trilha.count { !it.isPermanente && it.penalidade == "-4" })
    }

}
