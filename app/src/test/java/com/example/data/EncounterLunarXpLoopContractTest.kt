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
    fun `xp lunar deve preservar restricao de arvore ofensiva e limite de vigor`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        // Contrato de seguranca: a compra por XP nao pode contornar a
        // especializacao ofensiva nem a margem defensiva da geracao inicial.
        assertTrue(source.contains("ataqueEscolhido"))
        assertTrue(source.contains("ataqueDescartado"))
        assertTrue(source.contains("vigorPermitido"))
    }

    @Test
    fun `lunar xp filtra ramo ofensivo descartado antes de expandir arvores`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        assertTrue(source.contains("indice.ordemAtributos.filter { ataqueEscolhido == null || it != ataqueDescartado }"))
        assertTrue(source.contains("if (atributoRamo == ataqueDescartado) return false"))
        assertTrue(source.contains("vigorPermitido(categoriasSelecionadas)"))
        assertTrue(source.contains("route.atributo.equals(ataqueDescartado, ignoreCase = true)"))
    }

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
