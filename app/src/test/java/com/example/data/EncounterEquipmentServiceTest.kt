package com.example.data

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Regression tests for the equipment invariants used by Aba 11: Encontros.
 * The generator currently requests two artefacts for every exalted type.
 */
class EncounterEquipmentServiceTest {

    @Test
    fun `encontro sempre gera arma e armadura artefato quando dois artefatos sao permitidos`() {
        val habilidades = listOf("Armas Brancas", "Arqueirismo", "Arremesso", "Briga")

        repeat(EncounterTestSamples.count(200)) { seed ->
            habilidades.forEachIndexed { index, habilidade ->
                val (arma, armadura) = EncounterEquipmentService.selecionarEquipamentoParaEncontro(
                    habilidadeCombate = habilidade,
                    random = Random(seed.toLong() * 10 + index),
                    permitirDoisArtefatos = true
                )

                assertNotNull(arma, "$habilidade: arma ausente")
                assertNotNull(armadura, "$habilidade: armadura ausente")
                assertEquals("Artefato", arma!!.tipo, "$habilidade: arma deveria ser Artefato")
                assertEquals("Artefato", armadura!!.tipo, "$habilidade: armadura deveria ser Artefato")
            }
        }
    }

    @Test
    fun `briga nunca produz peso de arma diferente de leve`() {
        repeat(EncounterTestSamples.count(500)) { seed ->
            val arma = EncounterEquipmentService.selecionarArma(
                habilidadeCombate = "Briga",
                random = Random(seed.toLong()),
                artefato = true
            )

            assertNotNull(arma)
            assertEquals("Leve", arma!!.peso)
            assertEquals("Artefato", arma!!.tipo)
        }
    }

    @Test
    fun `armas de distancia nao recebem defesa de arma`() {
        repeat(EncounterTestSamples.count(200)) { seed ->
            for (habilidade in listOf("Arqueirismo", "Arremesso")) {
                val arma = EncounterEquipmentService.selecionarArma(
                    habilidadeCombate = habilidade,
                    random = Random(seed.toLong() + habilidade.hashCode()),
                    artefato = true
                )

                assertNotNull(arma)
                assertEquals(0, arma!!.defesa)
            }
        }
    }
}
