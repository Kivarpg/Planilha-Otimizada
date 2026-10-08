package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NameGeneratorCulturalSafetyTest {
    @Test fun `gerador nao oferece categoria cultural monolitica tribal`() {
        assertFalse(CulturaNome.entries.any { it.name == "TRIBAL" })
    }

    @Test fun `todas culturas publicadas possuem nomes para ambos os generos`() {
        CulturaNome.entries.forEach { cultura ->
            GeneroNome.entries.forEach { genero ->
                repeat(20) {
                    assertTrue(NameGenerator.gerar(cultura, genero, Random(it)).isNotBlank())
                }
            }
        }
    }

    @Test fun `nomes sagrados removidos nao aparecem no banco mesoamericano`() {
        val proibidos = setOf(
            "Kukulkan","Itzamna","Quetzalcoatl","Xochipilli","Yacatecuhtli",
            "Ixchel","Chalchiuhtlicue","Coyolxauhqui","Mayahuel","Coatlicue",
            "Xochiquetzal","Tlazolteotl","Chantico","Cihuacoatl","Tlaloc"
        )
        repeat(5000) { seed ->
            val genero = if (seed % 2 == 0) GeneroNome.MASCULINO else GeneroNome.FEMININO
            assertFalse(NameGenerator.gerar(CulturaNome.MESOAMERICANA, genero, Random(seed)) in proibidos)
        }
    }

    @Test fun `origem europeia generica foi substituida por origens fantasticas especificas`() {
        val nomes = CulturaNome.entries.map { it.name }.toSet()
        assertFalse("EUROPEIA" in nomes)
        assertTrue("ISLANDESA" in nomes)
        assertTrue("IRLANDESA_GAELICA" in nomes)
        assertTrue("HUNGARA" in nomes)
    }

    @Test fun `intrusos modernos religiosos e entidades removidos nao reaparecem`() {
        val proibidos = setOf(
            "Peter","Vernon","Kaitlyn","Gladys","Genghis","Guanyin","Vivi",
            "Xibalba","Xipetotec","Kinich","Ahau","Toci","Xilonen",
            "Omar","Yousef","Ahmed","Ibrahim","Mariam","Fatima"
        )
        CulturaNome.entries.forEach { cultura ->
            GeneroNome.entries.forEach { genero ->
                repeat(1000) { seed ->
                    val completo = NameGenerator.gerar(cultura, genero, Random(seed))
                    val tokens = completo.replace("Ó ","").replace("Mac ","").split(" ").toSet()
                    assertTrue("$cultura gerou entrada bloqueada: $completo", tokens.intersect(proibidos).isEmpty())
                }
            }
        }
    }

}
