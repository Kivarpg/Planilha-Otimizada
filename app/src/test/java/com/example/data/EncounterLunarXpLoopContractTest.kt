package com.example.data

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Garante que a lista de ramos Lunar so seja ampliada entre rodadas:
 * isso permite percorrer a lista diretamente sem alocacao por rodada.
 */
class EncounterLunarXpLoopContractTest {
    @Test
    fun `lunar active trees are iterated without a redundant snapshot`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        assertTrue(source.contains("for (atributo in arvoresAtivas) {"))
        assertFalse(source.contains("for (atributo in arvoresAtivas.toList())"))
        val loop = source.substringAfter("for (atributo in arvoresAtivas) {")
            .substringBefore("if (progressoRodada) continue")
        assertFalse(loop.contains("arvoresAtivas +="))
        assertTrue(source.contains("arvoresAtivas += proximaArvore"))
    }

    @Test
    fun `lunar block normalizes its category key once`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val block = source.substringAfter("fun comprarBloco(atributoRamo: String): Boolean {")
            .substringBefore("while (tentativas++")
        assertTrue(block.contains("val chaveCategoriaRamo = atributoRamo.lowercase()"))
        assertTrue(block.contains("categoriasSelecionadas[chaveCategoriaRamo]"))
        assertFalse(block.contains("categoriasSelecionadas[atributoRamo.lowercase()]"))
    }
}
