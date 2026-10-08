package com.example.data
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
class NameGeneratorJapaneseExpansionTest {
    @Test fun `banco japones expandido produz grande variedade deterministica`() {
        val nomes = (0 until 10000).map {
            NameGenerator.gerar(CulturaNome.JAPONESA,
                if (it % 2 == 0) GeneroNome.MASCULINO else GeneroNome.FEMININO, Random(it))
        }.toSet()
        assertTrue(nomes.size > 9000)
    }
}
