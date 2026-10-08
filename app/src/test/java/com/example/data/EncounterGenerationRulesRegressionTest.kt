package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncounterGenerationRulesRegressionTest {
    @Test
    fun `grupos de atributos sempre atribuem grupos distintos`() {
        repeat(EncounterTestSamples.count(500)) { seed ->
            val (primario, secundario, terciario) = EncounterGenerationRules.gruposDeAtributoPara(
                ArquetipoEncontro.FISICO,
                Random(seed)
            )
            assertTrue(secundario != terciario)
            assertTrue(secundario.toSet().intersect(terciario.toSet()).isEmpty())
            assertEquals(3, primario.size)
            assertEquals(3, secundario.size)
            assertEquals(3, terciario.size)
        }
    }

    @Test
    fun `defesa de combate e centralizada e rejeita habilidade invalida`() {
        assertEquals("Armas Brancas", EncounterGenerationRules.habilidadeDefensivaPara("Armas Brancas"))
        assertEquals("Briga", EncounterGenerationRules.habilidadeDefensivaPara("Briga"))
        assertEquals("Esquiva", EncounterGenerationRules.habilidadeDefensivaPara("Arqueirismo"))
        assertEquals("Esquiva", EncounterGenerationRules.habilidadeDefensivaPara("Arremesso"))
        assertFailsWith<IllegalStateException> {
            EncounterGenerationRules.habilidadeDefensivaPara("Habilidade inexistente")
        }
    }

    @Test
    fun `sorteio de feiticaria mental preserva regra nove em dez`() {
        val resultados = (0 until 10_000).count { index ->
            EncounterGenerationRules.sortearFeiticariaMental(Random(mixSeed(index.toLong())))
        }
        assertTrue(resultados in 8_700..9_300, "Frequência inesperada: $resultados/10000")
    }
    @Test
    fun `distribuicao de atributos preserva 11 9 7 e grupos distintos`() {
        repeat(EncounterTestSamples.count(2_000)) { seed ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val (primario, secundario, terciario) = EncounterGenerationRules.gruposDeAtributoPara(
                    arquetipo,
                    Random(seed * 17 + arquetipo.ordinal)
                )
                val atributos = EncounterDistributionService.distribuirAtributos(
                    primario,
                    secundario,
                    terciario,
                    Random(seed * 31 + arquetipo.ordinal)
                )
                assertEquals(11, primario.sumOf { atributos.getValue(it) })
                assertEquals(9, secundario.sumOf { atributos.getValue(it) })
                assertEquals(7, terciario.sumOf { atributos.getValue(it) })
                assertEquals(27, atributos.values.sum())
                assertTrue(primario.toSet().intersect(secundario.toSet()).isEmpty())
                assertTrue(primario.toSet().intersect(terciario.toSet()).isEmpty())
                assertTrue(secundario.toSet().intersect(terciario.toSet()).isEmpty())
            }
        }
    }

    @Test
    fun `ajuste de combate preserva totais das categorias de atributos`() {
        repeat(EncounterTestSamples.count(2_000)) { seed ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val (primario, secundario, terciario) = EncounterGenerationRules.gruposDeAtributoPara(
                    arquetipo,
                    Random(seed * 43 + arquetipo.ordinal)
                )
                val base = EncounterDistributionService.distribuirAtributos(
                    primario,
                    secundario,
                    terciario,
                    Random(seed * 59 + arquetipo.ordinal)
                )
                EncounterGenerationRules.COMBAT_ABILITIES.forEachIndexed { indice, combate ->
                    val ajustado = EncounterDistributionService.ajustarAtributosPorHabilidadeCombate(
                        base,
                        combate,
                        Random(seed * 71L + indice)
                    )
                    assertEquals(11, primario.sumOf { ajustado.getValue(it) })
                    assertEquals(9, secundario.sumOf { ajustado.getValue(it) })
                    assertEquals(7, terciario.sumOf { ajustado.getValue(it) })
                    assertEquals(27, ajustado.values.sum())
                    assertTrue(ajustado.values.all { it in 1..5 })
                    assertTrue(ajustado.getValue("Destreza") >= if (combate in listOf("Armas Brancas", "Briga")) 4 else 5)
                    if (combate in listOf("Arqueirismo", "Arremesso")) {
                        assertTrue(ajustado.getValue("Força") <= 2)
                    }
                }
            }
        }
    }

    private fun mixSeed(value: Long): Long {
        var z = value + 0x9E3779B97F4A7C15UL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBUL.toLong()
        return z xor (z ushr 31)
    }

}

