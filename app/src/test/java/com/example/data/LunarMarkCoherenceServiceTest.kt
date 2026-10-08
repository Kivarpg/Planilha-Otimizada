package com.example.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarMarkCoherenceServiceTest {
    private fun mark(pt: String) = LunarMarksService.Mark(pt, pt)

    @Test
    fun `peixe rejeita sinal aviario`() {
        val peixe = SpiritualFormService.Animal("Peixe-zebra", "Zebrafish")
        val penas = mark("Penas brancas crescendo permanentemente no cabelo")
        assertEquals(
            LunarMarkCoherenceService.Compatibility.INCOMPATIVEL,
            LunarMarkCoherenceService.compatibilidade(peixe, penas)
        )
    }

    @Test
    fun `ave rejeita sinal aquatico`() {
        val ave = SpiritualFormService.Animal("Águia", "Eagle")
        val guelras = mark("Guelras funcionais atrás das orelhas")
        assertEquals(
            LunarMarkCoherenceService.Compatibility.INCOMPATIVEL,
            LunarMarkCoherenceService.compatibilidade(ave, guelras)
        )
    }

    @Test
    fun `felino reconhece sinal felino como forte`() {
        val tigre = SpiritualFormService.Animal("Tigre", "Tiger")
        val garras = mark("Unhas retráteis que lembram garras felinas")
        assertEquals(
            LunarMarkCoherenceService.Compatibility.FORTE,
            LunarMarkCoherenceService.compatibilidade(tigre, garras)
        )
    }

    @Test
    fun `todas as 600 formas possuem pool coerente sem sinal incompatível`() {
        val formas = SpiritualFormService.todasFormas()
        assertEquals(600, formas.size)
        formas.forEach { forma ->
            val pool = LunarMarkCoherenceService.poolCoerente(forma)
            assertTrue("${forma.portuguese} ficou sem Sinais coerentes", pool.isNotEmpty())
            pool.forEach { sinal ->
                assertNotEquals(
                    "${forma.portuguese} recebeu sinal incompatível: ${sinal.portuguese}",
                    LunarMarkCoherenceService.Compatibility.INCOMPATIVEL,
                    LunarMarkCoherenceService.compatibilidade(forma, sinal)
                )
            }
        }
    }

    @Test
    fun `catalogo continua com 300 sinais`() {
        assertEquals(300, LunarMarksService.size())
        assertTrue(LunarMarksService.todas().isNotEmpty())
    }
}
