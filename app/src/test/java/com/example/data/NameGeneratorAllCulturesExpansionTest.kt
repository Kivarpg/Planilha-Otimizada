package com.example.data
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NameGeneratorAllCulturesExpansionTest {
    @Test fun `todas origens possuem variedade ampla sem depender do banco japones`() {
        CulturaNome.entries.forEach { cultura ->
            val amostra = (0 until 2000).map { seed ->
                NameGenerator.gerar(
                    cultura,
                    if (seed % 2 == 0) GeneroNome.MASCULINO else GeneroNome.FEMININO,
                    Random(seed)
                )
            }.toSet()
            assertTrue("$cultura teve pouca variedade: ${amostra.size}", amostra.size > 500)
        }
    }
}
