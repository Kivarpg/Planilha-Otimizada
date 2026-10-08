package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NameGeneratorExaltTypePolicyTest {
    @Test
    fun `imperial and lookshy names approach 95 percent one given name in seeded draws`() {
        for (origem in listOf(OrigemNomeSangueDeDragao.IMPERIO, OrigemNomeSangueDeDragao.LOOKSHY)) {
            val nomes = (0 until 2000).map { seed ->
                NomesSangueDeDragao.gerar(origem, GeneroNome.MASCULINO, Random(seed))
            }
            val simples = nomes.count { it.trim().split(Regex("\\s+")).size == 2 }
            // A probabilidade configurada e 95%; uma amostra finita oscila.
            assertTrue("$origem: $simples/2000", simples in 1840..1960)
            assertTrue(nomes.all { it.trim().split(Regex("\\s+")).size in 2..3 })
        }
    }

    @Test
    fun `solar and lunar default pools exclude east asian names`() {
        val naoAsiaticas = listOf(
            CulturaNome.ISLANDESA, CulturaNome.IRLANDESA_GAELICA,
            CulturaNome.HUNGARA, CulturaNome.AFRICANA,
            CulturaNome.ARABE, CulturaNome.MESOAMERICANA
        )
        repeat(200) { seed ->
            val rng = Random(seed)
            val cultura = naoAsiaticas.random(rng)
            val esperado = NameGenerator.gerar(cultura, GeneroNome.FEMININO, rng)
            assertEquals(
                esperado,
                NameGenerator.gerarSolarOuLunar(genero = GeneroNome.FEMININO, random = Random(seed))
            )
        }
    }

    @Test
    fun `explicit asian culture is preserved for solar and lunar`() {
        val seed = 123
        assertEquals(
            NameGenerator.gerar(CulturaNome.CHINESA, GeneroNome.MASCULINO, Random(seed)),
            NameGenerator.gerarSolarOuLunar(CulturaNome.CHINESA, GeneroNome.MASCULINO, Random(seed))
        )
    }
    @Test
    fun `rare second given name never duplicates the first`() {
        for (origem in listOf(OrigemNomeSangueDeDragao.IMPERIO, OrigemNomeSangueDeDragao.LOOKSHY)) {
            var encontrados = 0
            repeat(3000) { seed ->
                val nome = NomesSangueDeDragao.gerar(origem, GeneroNome.FEMININO, Random(seed))
                val partes = nome.split(Regex("\\s+"))
                if (partes.size == 3) {
                    encontrados++
                    assertTrue("$origem: $nome", !partes[1].equals(partes[2], ignoreCase = true))
                }
            }
            assertTrue("$origem deve permitir a excecao de 5%", encontrados > 0)
        }
    }

    @Test
    fun `unspecified gender keeps seeded Dragon Blooded names deterministic`() {
        for (origem in listOf(OrigemNomeSangueDeDragao.IMPERIO, OrigemNomeSangueDeDragao.LOOKSHY)) {
            repeat(1000) { seed ->
                val primeiro = NomesSangueDeDragao.gerar(origem, null, Random(seed))
                val segundo = NomesSangueDeDragao.gerar(origem, null, Random(seed))
                assertEquals("$origem seed=$seed", primeiro, segundo)
                assertTrue(primeiro.split(Regex("\\s+")).size in 2..3)
            }
        }
    }

}
