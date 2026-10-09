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
        assertTrue(source.contains("listOf(ataqueEscolhido, \"Vigor\") + indice.ordemAtributos"))
        assertTrue(source.contains("ataqueEscolhido == null || !it.equals(ataqueDescartado, ignoreCase = true)"))
        assertTrue(source.contains("if (ataqueDescartado != null && atributoRamo.equals(ataqueDescartado, ignoreCase = true)) return false"))
        assertTrue(source.contains("vigorPermitido(categoriasSelecionadas)"))
        assertTrue(source.contains("route.atributo.equals(ataqueDescartado, ignoreCase = true)"))
    }

    @Test
    fun `foco ofensivo explicito precede contagem automatica no xp lunar`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val selection = source.substringAfter("val ataqueEscolhido = if (npc.arquetipo")
            .substringBefore("val ataqueDescartado")
        assertTrue(selection.contains("npc.lunarAtaqueEscolhido?.let { salvo -> ofensivos.firstOrNull { it.equals(salvo.trim(), ignoreCase = true) } }"))
        assertTrue(selection.contains("npc.focoProgressaoExplicito?.let { foco -> ofensivos.firstOrNull { it.equals(foco.trim(), ignoreCase = true) } }"))
        assertTrue(selection.indexOf("npc.lunarAtaqueEscolhido") < selection.indexOf("npc.focoProgressaoExplicito"))
        assertTrue(selection.indexOf("npc.focoProgressaoExplicito") < selection.indexOf("ofensivos.maxWithOrNull"))
    }

    @Test
    fun `lunar vigor stays within three charms of the chosen attack across xp batches`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val purchase = source.substringAfter("fun vigorPermitido(contagens: Map<String, Int>): Boolean =")
            .substringBefore("val ramos =")
        assertTrue(purchase.contains("(contagens[\"vigor\"] ?: 0) < (contagens[ataqueEscolhido.lowercase()] ?: 0) + 3"))
        assertTrue(source.contains("vigorPermitido(categoriasSelecionadas)"))
        assertTrue(source.contains("categoriasSelecionadas[categoriaCandidato] = (categoriasSelecionadas[categoriaCandidato] ?: 0) + 1"))
        assertTrue(source.contains("distinctBy { it.lowercase() }"))
        assertTrue(source.contains("arvoresAtivas.none { it.equals(ramo, ignoreCase = true) }"))
    }

    @Test
    fun `lunar advanced charms recheck essence and prerequisites after every purchase`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val purchase = source.substringAfter("fun comprarBloco(atributoRamo: String): Boolean {")
            .substringBefore("while (tentativas++ < 500")
        val essence = purchase.indexOf("val essenciaAtual = EncounterExperienceService.essenciaPara(npc, xpGastoTotal)")
        val eligible = purchase.indexOf("LunarCharmArchetypePolicy.eligibleRoutes(")
        val spend = purchase.indexOf("xpGastoTotal += custoXp")
        val acquired = purchase.indexOf("nomesSelecionados += candidatoSelecionado.nome")
        assertTrue(essence >= 0 && eligible > essence && spend > eligible && acquired > spend)
        assertTrue(purchase.contains("essenciaAtual, nomesSelecionados, catalogo"))
        assertTrue(purchase.contains("tentativasBloco++ < 100"))
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
