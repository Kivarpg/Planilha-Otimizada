package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.NOME_CORPO_DE_TOURO
import com.example.model.NOME_FEITICARIA_TERRESTRE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EncounterValidationServiceTest {
    @Test
    fun `validacao da distribuicao base aceita todas as combinacoes de arquetipo`() {
        repeat(EncounterTestSamples.count(300)) { seed ->
            ArquetipoEncontro.entries.forEachIndexed { index, arquetipo ->
                val (primario, secundario, terciario) = EncounterGenerationRules.gruposDeAtributoPara(
                    arquetipo, Random(seed * 31L + index)
                )
                val atributos = EncounterDistributionService.distribuirAtributos(
                    primario, secundario, terciario, Random(seed * 97L + index)
                )
                val ajustados = EncounterDistributionService.ajustarAtributosPorHabilidadeCombate(
                    atributos, EncounterGenerationRules.COMBAT_ABILITIES[index], Random(seed * 193L + index)
                )
                EncounterValidationService.validarDistribuicaoBaseDeAtributos(ajustados, arquetipo)
            }
        }
    }
    @Test
    fun `social sem feiticaria terrestre nao alerta ausencia de feiticos`() {
        val atributos = mapOf("Carisma" to 5)
        val alertas = EncounterValidationService.validar(
            arquetipo = ArquetipoEncontro.SOCIAL,
            attributes = atributos,
            charms = listOf(NOME_CORPO_DE_TOURO),
            feiticos = emptyList<Any>()
        )
        assertFalse(alertas.any { it.contains("Feitiço") })
    }

    @Test
    fun `social com feiticaria terrestre alerta se nenhum feitico foi selecionado`() {
        val atributos = mapOf("Carisma" to 5)
        val alertas = EncounterValidationService.validar(
            arquetipo = ArquetipoEncontro.SOCIAL,
            attributes = atributos,
            charms = listOf(NOME_CORPO_DE_TOURO, NOME_FEITICARIA_TERRESTRE),
            feiticos = emptyList<Any>()
        )
        assertTrue(alertas.any { it.contains("nenhum Feitiço", ignoreCase = true) })
    }

    @Test
    fun `mental sem feiticaria terrestre nao exige quatro feiticos`() {
        val atributos = mapOf("Inteligência" to 5)
        val alertas = EncounterValidationService.validar(
            arquetipo = ArquetipoEncontro.MENTAL,
            attributes = atributos,
            charms = listOf(NOME_CORPO_DE_TOURO),
            feiticos = emptyList<Any>()
        )
        assertFalse(alertas.any { it.contains("Feitiço", ignoreCase = true) })
    }

    @Test
    fun `mental com feiticaria terrestre e poucos feiticos recebe apenas aviso de perfil`() {
        val atributos = mapOf("Inteligência" to 5)
        val alertas = EncounterValidationService.validar(
            arquetipo = ArquetipoEncontro.MENTAL,
            attributes = atributos,
            charms = listOf(NOME_FEITICARIA_TERRESTRE),
            feiticos = emptyList<Any>()
        )
        assertTrue(alertas.any { it.contains("apesar do acesso à Feitiçaria") })
    }

    @Test
    fun `social e mental sem corpo de touro nao recebem alerta de resistencia`() {
        listOf(ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL).forEach { arquetipo ->
            val atributo = if (arquetipo == ArquetipoEncontro.SOCIAL) "Carisma" else "Inteligência"
            val alertas = EncounterValidationService.validar(
                arquetipo = arquetipo, attributes = mapOf(atributo to 5), charms = emptyList(), feiticos = emptyList<Any>()
            )
            assertFalse(alertas.any { it.contains("Corpo de Touro") })
        }
    }

}
