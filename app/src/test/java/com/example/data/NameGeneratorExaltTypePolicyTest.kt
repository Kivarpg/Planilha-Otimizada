package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NameGeneratorExaltTypePolicyTest {
    @Test fun `dragon blooded names use 70 percent one additional component`() {
        for (origem in listOf(OrigemNomeSangueDeDragao.IMPERIO, OrigemNomeSangueDeDragao.LOOKSHY)) {
            val nomes = (0 until 2000).map { seed ->
                NomesSangueDeDragao.gerar(origem, GeneroNome.MASCULINO, Random(seed))
            }
            val simples = nomes.count { it.trim().split(Regex("\\s+")).size == 2 }
            assertTrue("$origem: $simples/2000", simples in 1280..1520)
            assertTrue(nomes.all { it.isNotBlank() })
        }
    }

    @Test fun `solar and lunar names use 60 percent one component`() {
        val nomes = (0 until 2000).map { seed ->
            NameGenerator.gerarSolarOuLunar(CulturaNome.CHINESA, GeneroNome.FEMININO, Random(seed))
        }
        val simples = nomes.count { it.trim().split(Regex("\\s+")).size == 1 }
        assertTrue("Solar/Lunar: $simples/2000", simples in 1080..1320)
    }

    @Test fun `explicit culture and seed remain deterministic`() {
        repeat(200) { seed ->
            val a = NameGenerator.gerarSolarOuLunar(CulturaNome.CHINESA, GeneroNome.MASCULINO, Random(seed))
            val b = NameGenerator.gerarSolarOuLunar(CulturaNome.CHINESA, GeneroNome.MASCULINO, Random(seed))
            assertEquals(a, b)
        }
    }

    @Test fun `dragon blooded names preserve family and never repeat components`() {
        for (origem in listOf(OrigemNomeSangueDeDragao.IMPERIO, OrigemNomeSangueDeDragao.LOOKSHY)) {
            repeat(1000) { seed ->
                val nome = NomesSangueDeDragao.gerar(origem, GeneroNome.FEMININO, Random(seed))
                val partes = nome.split(Regex("\\s+"))
                assertTrue(nome, partes.size in 2..4)
                assertEquals(nome, partes.size, partes.map { it.lowercase() }.distinct().size)
                assertEquals(nome, nome, NomesSangueDeDragao.gerar(origem, GeneroNome.FEMININO, Random(seed)))
            }
        }
    }
}
