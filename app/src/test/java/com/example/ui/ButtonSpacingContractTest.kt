package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regressão de espaçamento de controles na ficha Android.
 * Não substitui inspeção em dispositivos com fontes grandes.
 */
class ButtonSpacingContractTest {
    private val top = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
    private val navigation = File("src/main/java/com/example/ui/SheetTabsBar.kt").readText()
    private val combat = File("src/main/java/com/example/ui/tabs/CombatTab.kt").readText()
    private val encounters = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
    private val generator = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()
    private val counters = File("src/main/java/com/example/ui/components/CounterControls.kt").readText()

    @Test fun `header actions are separated without moving them from their row`() {
        assertTrue(top.contains("horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                    InkButton(onClick = { onShowSettingsDialog(true) }"))
        assertTrue(top.contains("InkButton(onClick = { onShowOpcoesMenu(true) }"))
    }

    @Test fun `each navigable tab has a nonzero gutter`() {
        assertTrue(navigation.contains("horizontalArrangement = Arrangement.spacedBy(6.dp)"))
        assertFalse(navigation.contains("horizontalArrangement = Arrangement.spacedBy(0.dp)"))
    }

    @Test fun `combat vitality action buttons wrap before colliding`() {
        val start = combat.indexOf("// Ações da Vitalidade: Adicionar")
        val end = combat.indexOf("if (showVitalitySettingsDialog)", start)
        assertTrue(start >= 0 && end > start)
        val actions = combat.substring(start, end)
        assertTrue(actions.contains("FlowRow("))
        assertTrue(actions.contains("horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)"))
        assertTrue(actions.contains("verticalArrangement = Arrangement.spacedBy(8.dp)"))
        assertTrue(actions.contains("label = \"Adicionar\""))
        assertTrue(actions.contains("label = \"Limpar\""))
    }

    @Test fun `NPC file actions retain readable spacing and callbacks`() {
        val start = encounters.indexOf("val compactActions = compactPhone")
        val end = encounters.indexOf("// Exportar / Salvar / Carregar", start)
        assertTrue(start >= 0 && end > start)
        val actions = encounters.substring(start, end)
        assertTrue(actions.contains("horizontalArrangement = Arrangement.spacedBy(8.dp)"))
        assertTrue(actions.contains("label = \"Exportar\""))
        assertTrue(actions.contains("label = \"Salvar\""))
        assertTrue(actions.contains("label = \"Carregar\""))
    }

    @Test fun `encounter selectors and shared counters keep independent gaps`() {
        assertTrue(generator.contains("horizontalArrangement = Arrangement.spacedBy(6.dp)"))
        assertTrue(counters.contains("Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)"))
    }
}
