package com.example.ui.tabs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Contratos de interface das Abas 5 e 9. Evitam regressao de titulos
 * cortados e da acao de limpeza da trilha de vitalidade.
 */
class CombatVitalityUiContractTest {
    @Test
    fun `combat initiative heading can wrap instead of clipping`() {
        val source = File("src/main/java/com/example/ui/tabs/CombatTab.kt").readText()
        val heading = source.substringAfter("text = \"Contador de iniciativa\"").substringBefore("GildedCard(")
        assertTrue(heading.contains("maxLines = 2"))
        assertTrue(heading.contains("softWrap = true"))
    }

    @Test
    fun `summary health boxes share combat damage action and clear button`() {
        val source = File("src/main/java/com/example/ui/tabs/SummaryVitalitySection.kt").readText()
        assertTrue(source.contains("viewModel.cycleHealthDamage(box.id)"))
        assertTrue(source.contains("label = \"Limpar\""))
        assertTrue(source.contains("viewModel.clearHealthDamage()"))
        assertTrue(source.contains("caixas.chunked(maxPerRow)"))
    }
}
