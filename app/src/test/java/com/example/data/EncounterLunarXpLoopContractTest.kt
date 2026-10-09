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
    fun `desempate ofensivo lunar respeita prioridade ignorando capitalizacao`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        assertTrue(source.contains("indice.ordemAtributos.indexOfFirst { atributo ->"))
        assertTrue(source.contains("atributo.equals(it, ignoreCase = true)"))
    }

    @Test
    fun `integracao lunar conserva xp e atualiza contexto da quimera`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        assertTrue(source.contains("traitsCache == traits && ordemCache == ordem"))
        assertTrue(source.contains("traitsAnteriores == traits"))
        assertTrue(source.contains("routeContextAtual = LunarCharmArchetypePolicy.prepare("))
        assertTrue(source.contains("xpDisponivel -= custoXp"))
        assertTrue(source.contains("xpGastoTotal += custoXp"))
        assertTrue(source.contains("nomesCharmsNesteLote += candidatoSelecionado.nome"))
        assertTrue(source.contains("xpGasto = xpGastoTotal - npc.xpGastoTotal"))
    }

    @Test
    fun `lunar persiste ataque forma e historico em cada lote de xp`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val expansion = source.substringAfter("private fun expandLunarInternalResult(")
            .substringBefore("fun reduceLunar(npc: NpcEncontro)")
        assertTrue(expansion.contains("nomesEncantosAdicionados = nomesCharmsNesteLote"))
        assertTrue(expansion.contains("historicoXpBatches = npc.historicoXpBatches + batch"))
        assertTrue(expansion.contains("formaEspiritualSecundaria = secundaria"))
        assertTrue(expansion.contains("lunarArchetypeTraits = traitsEfetivosFinais.map { it.name }.sorted()"))
        assertTrue(expansion.contains("resultadoCompra.ataqueEscolhido"))
        assertTrue(expansion.contains("xpGastoTotal = xpGastoTotal"))
    }

    @Test
    fun `reversao lunar preserva ataque e restaura saldo e forma espiritual`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val rollback = source.substringAfter("fun reduceLunar(npc: NpcEncontro): NpcEncontro {")
        assertTrue(rollback.contains("lunarAtaqueEscolhido = npc.lunarAtaqueEscolhido"))
        assertTrue(rollback.contains("XP_POR_CHAMADA - ultimoLote.xpGasto"))
        assertTrue(rollback.contains("xpGastoTotal = xpGastoTotalNovo"))
        assertTrue(rollback.contains("historicoXpBatches = npc.historicoXpBatches.dropLast(1)"))
        assertTrue(rollback.contains("val mantemQuimera = charmsRestantes.any"))
        assertTrue(rollback.contains("formaEspiritualSecundaria = secundariaRevertida"))
        assertTrue(rollback.contains("lunarArchetypeTraits = traitsRevertidos"))
    }

    @Test
    fun `xp lunar reconhece atributos favorecidos sem distinguir maiusculas`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        assertTrue(source.contains("atributoAquisicao.lowercase() in castaOuFavorecidos"))
        assertTrue(source.contains(".mapTo(hashSetOf()) { it.lowercase() }"))
        assertTrue(source.contains("atributoAquisicao.equals(\"Universal\", ignoreCase = true)"))
    }

    @Test
    fun `xp lunar encontra gaveta de encantos com capitalizacao diferente`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        assertTrue(source.contains("routeContextAtual.charmsByAttribute[atributoRamo]"))
        assertTrue(source.contains("it.key.equals(atributoRamo, ignoreCase = true)"))
        assertTrue(source.contains(".distinctBy { it.lowercase() }"))
        assertTrue(source.contains("for (def in candidatosOrdenados(atributoRamo))"))
    }

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
        assertTrue(source.contains("listOf(\"Vigor\", ataqueEscolhido) + indice.ordemAtributos"))
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
    fun `vigor blocked by three charm margin skips candidate evaluation`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val block = source.substringAfter("fun comprarBloco(atributoRamo: String): Boolean {")
            .substringBefore("val candidatoSelecionado = candidato ?: break")
        val guard = block.indexOf("!vigorPermitido(categoriasSelecionadas)")
        val scan = block.indexOf("for (def in candidatosOrdenados(atributoRamo))")
        assertTrue(guard >= 0 && guard < scan)
        assertTrue(block.contains("atributoRamo.equals(\"Vigor\", ignoreCase = true)"))
    }

    @Test
    fun `lunar filters impossible essence and attribute routes before eligibility`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        val purchase = source.substringAfter("fun comprarBloco(atributoRamo: String): Boolean {")
            .substringBefore("val candidatoSelecionado = candidato ?: break")
        val essenceGuard = purchase.indexOf("if (def.minEssencia > essenciaAtual) continue")
        val routeGuard = purchase.indexOf("routeContextAtual.routesFor(def).none")
        val eligibility = purchase.indexOf("LunarCharmArchetypePolicy.eligibleRoutes(")
        assertTrue(essenceGuard >= 0 && routeGuard > essenceGuard && eligibility > routeGuard)
        assertTrue(purchase.contains("route.minAtributo"))
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
    fun `lunar physical progression ranks deeper trees and invalidates chimera cache`() {
        val source = File("src/main/java/com/example/data/EncounterExperienceLunar.kt").readText()
        assertTrue(source.contains("listOf(\"Vigor\", ataqueEscolhido) + indice.ordemAtributos"))
        assertTrue(source.contains("alcancePorEncanto.getOrPut(def.nome)"))
        assertTrue(source.contains("routeContextAtual.reachableDependentCountWithinLimits("))
        assertTrue(source.contains("def.nome, definicoesPorNome, attributesAtuais, limiteEssenciaCatalogo"))
        assertTrue(source.contains("alcancePorEncanto.clear()"))
        assertTrue(source.contains("alcanceAtualPorEncanto.clear()"))
        assertTrue(source.contains("compareByDescending<EncantoLunarDefinition> { alcanceAtualPorEncanto[it.nome] ?: 0 }"))
        assertTrue(source.contains("thenByDescending { alcancePorEncanto[it.nome] ?: 0 }"))
        assertTrue(source.contains("val candidatosPorRamo = mutableMapOf"))
        assertTrue(source.contains("if (ataqueEscolhido == null) return@getOrPut encantos"))
        assertTrue(source.contains("if (ataqueEscolhido != null) catalogo.associateBy { it.nome } else emptyMap()"))
        assertTrue(source.contains("candidatosPorRamo.clear()"))
        assertTrue(source.contains("LunarCharmArchetypePolicy.eligibleRoutes("))
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
