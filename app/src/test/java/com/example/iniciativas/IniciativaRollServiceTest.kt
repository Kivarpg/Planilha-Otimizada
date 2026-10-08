package com.example.iniciativas

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class IniciativaRollServiceTest {
    @Test
    fun `7 8 9 valem um sucesso e zero vale dois`() {
        val random = FixedRandom(listOf(7, 8, 9, 0, 1, 6))
        assertEquals(8, IniciativaRollService.rolar(6, random))
    }

    @Test
    fun `quantidade de dados e o valor de juntar-se à batalha`() {
        val random = FixedRandom(List(5) { 7 })
        assertEquals(8, IniciativaRollService.rolar(5, random))
    }

    @Test
    fun `valor negativo nao gera dados mas ainda recebe tres`() {
        assertEquals(3, IniciativaRollService.rolar(-4, FixedRandom(emptyList())))
    }

    // Sobrescreve nextInt(from, until) diretamente — IniciativaRollService
    // chama esse método, não nextBits(). A stdlib do Kotlin reescalona os
    // bits internamente de um jeito não-trivial (não é um simples módulo),
    // então sobrescrever só nextBits() não dá controle confiável sobre o
    // valor final de nextInt(0, 10) — confirmado testando com valores
    // reais: nextBits(32) retornando 7 gera nextInt(0,10)=3, não 7.
    private class FixedRandom(private val values: List<Int>) : Random() {
        private var index = 0

        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(from: Int, until: Int): Int = values.getOrElse(index++) { 0 }
    }
}
