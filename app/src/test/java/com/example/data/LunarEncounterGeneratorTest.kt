package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.LunarCasta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LunarEncounterGeneratorTest {
    @Test
    fun `Lunar possui dois atributos de casta e dois favorecidos adicionais distintos`() {
        ArquetipoEncontro.entries.forEach { arquetipo ->
            repeat(EncounterTestSamples.count(100)) { seed ->
                val npc = EncounterGenerator.gerarLunar(
                    nomeManual = "Teste",
                    arquetipo = arquetipo,
                    encantosLunares = emptyList(),
                    random = Random(seed + arquetipo.ordinal * 1000)
                )

                assertEquals(2, npc.lunarAtributosCasta.size)
                assertEquals(2, npc.lunarAtributosCasta.distinct().size)
                assertEquals(2, npc.habilidadesFavorecidas.size)
                assertEquals(2, npc.habilidadesFavorecidas.distinct().size)
                assertTrue(npc.lunarAtributosCasta.toSet().intersect(npc.habilidadesFavorecidas.toSet()).isEmpty())
                assertEquals(4, (npc.lunarAtributosCasta + npc.habilidadesFavorecidas).distinct().size)

                val casta = LunarCasta.entries.first { it.displayName == npc.casta }
                assertTrue(npc.lunarAtributosCasta.all { it in casta.poolAtributosCasta() })
                assertTrue(npc.habilidadesFavorecidas.all { it !in npc.lunarAtributosCasta.toSet() })
            }
        }
    }
}
