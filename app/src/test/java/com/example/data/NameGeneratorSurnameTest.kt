package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NameGeneratorSurnameTest {
    @Test fun `todas culturas geram nome completo`() {
        CulturaNome.entries.forEach { cultura ->
            GeneroNome.entries.forEach { genero ->
                repeat(50) { seed ->
                    val completo = NameGenerator.gerar(cultura, genero, Random(seed))
                    val partes = completo.trim().split(Regex("\\s+"))
                    assertTrue("$cultura -> $completo", partes.isNotEmpty() && partes.all { it.isNotBlank() })
                    // A regra da Aba 11 permite um único componente quando prenome e
                    // sobrenome sorteados seriam idênticos (ex.: "Yuan Yuan" -> "Yuan").
                    if (partes.size == 1) {
                        assertTrue(
                            "$cultura -> $completo só pode ser simples por deduplicação",
                            cultura != CulturaNome.ISLANDESA
                        )
                    }
                }
            }
        }
    }

    @Test fun `geracao com mesma seed continua deterministica`() {
        CulturaNome.entries.forEach { cultura ->
            val a = NameGenerator.gerar(cultura, GeneroNome.MASCULINO, Random(42))
            val b = NameGenerator.gerar(cultura, GeneroNome.MASCULINO, Random(42))
            assertEquals(a, b)
        }
    }

    @Test fun `categoria tribal removida nao retorna`() {
        assertTrue(CulturaNome.entries.none { it.name == "TRIBAL" })
    }

    @Test fun `islandes usa patronimico em vez de sobrenome familiar generico`() {
        val masculino = NameGenerator.gerar(CulturaNome.ISLANDESA, GeneroNome.MASCULINO, Random(7))
        val feminino = NameGenerator.gerar(CulturaNome.ISLANDESA, GeneroNome.FEMININO, Random(7))
        assertTrue(masculino.endsWith("son"))
        assertTrue(feminino.endsWith("dóttir"))
    }

    @Test fun `reduplicacao chinesa integral aparece apenas uma vez`() {
        assertEquals("Ying", NameGenerator.normalizarReduplicacaoChinesa("Yingying"))
        assertEquals("Yuan", NameGenerator.normalizarReduplicacaoChinesa("Yuanyuan"))
        assertEquals("Xiong", NameGenerator.normalizarReduplicacaoChinesa("XiongXiong"))
    }

    @Test fun `reduplicacao chinesa com espacos tambem aparece apenas uma vez`() {
        assertEquals("Ying", NameGenerator.normalizarReduplicacaoChinesa("Ying Ying"))
        assertEquals("Yuan", NameGenerator.normalizarReduplicacaoChinesa("Yuan  Yuan"))
        assertEquals("Xion", NameGenerator.normalizarReduplicacaoChinesa(" Xion Xion "))
        assertEquals("Ming Yuan", NameGenerator.normalizarReduplicacaoChinesa("Ming Yuan"))
    }

    @Test fun `nomes chineses compostos legitimos nao sao reduzidos`() {
        listOf("Qingyuan", "Guiying", "Mingyuan", "Yuanhao").forEach { nome ->
            assertEquals(nome, NameGenerator.normalizarReduplicacaoChinesa(nome))
        }
    }


}