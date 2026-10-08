package com.example.ui.tabs

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contratos de invalidação dos caches derivados da Aba 11. */
class EncounterNpcCacheContractTest {
    @Test
    fun `charm drawers follow view model and exalt type changes`() {
        val source = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
        assertTrue(source.contains(
            "val encantosPorHabilidade = remember(viewModel, npc.charms, npc.tipoExaltado)"
        ))
    }

    @Test
    fun `available spells follow view model and exalt type changes`() {
        val source = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
        assertTrue(source.contains(
            "val feiticosDisponiveis = remember(viewModel, npc.id, npc.tipoExaltado, npc.charms, npc.feiticos, npc.primeiroXpRecebido)"
        ))
    }
    @Test
    fun `charm and spell detail dialogs are scoped to their NPC`() {
        val source = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
        assertTrue(source.contains(
            "androidx.compose.runtime.remember(npc.id, c.nome)"
        ))
        assertTrue(source.contains(
            "remember(npc.id, feitico.nome)"
        ))
        assertTrue(source.contains(".pointerInput(npc.id, c.nome)"))
        assertTrue(source.contains(".pointerInput(npc.id, feitico.nome)"))
    }

    @Test
    fun `encounter npc card stays inside the scrollable encounter tab`() {
        val source = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()
        val scroll = source.indexOf(".verticalScroll(rememberScrollState())", source.indexOf("fun EncounterGeneratorTab"))
        val card = source.indexOf("NpcEncontroCard(", scroll)
        assertTrue("Aba 11 must scroll before rendering the NPC card", scroll >= 0 && card > scroll)
    }

    @Test
    fun `npc action stripes continue after astucia into motes and equipment`() {
        val source = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
        val labels = listOf("Astúcia", "Motes Pessoais", "Motes Periféricos", "Equipamento")
        val positions = labels.map { source.indexOf("\"$it\"", source.indexOf("fun NpcEncontroCard(")) }
        assertTrue("All post-action rows must exist in order", positions.all { it >= 0 } && positions.zipWithNext().all { (a, b) -> a < b })
        for (position in positions) {
            val row = source.substring(position, (position + 160).coerceAtMost(source.length))
            assertTrue("Each post-action row must advance the zebra index", row.contains("zebra++"))
        }
    }

    @Test
    fun `npc row gestures retain current callback without restarting for lambda identity`() {
        val source = File("src/main/java/com/example/ui/tabs/EncounterNpcCardComponents.kt").readText()
        assertTrue(source.contains("rememberUpdatedState(onTapOverride)"))
        assertTrue(source.contains("Modifier.pointerInput(calculoLongPress, onTapOverride != null)"))
        assertTrue(source.contains("currentOnTapOverride?.invoke()"))
    }

}
