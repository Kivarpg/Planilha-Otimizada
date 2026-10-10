package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Baseline estático dos controles compactos. Não substitui testes instrumentados
 * de bounds, hit targets, fonte ampliada ou capturas de tela.
 *
 * Mantém o contrato atual documentado antes de qualquer remodelação visual.
 */
class CompactInkButtonContractTest {
    private val inkButton = File("src/main/java/com/example/ui/components/InkButton.kt").readText()
    private val charmList = File("src/main/java/com/example/ui/tabs/CharmListComponents.kt").readText()
    private val dialogButtons = File("src/main/java/com/example/ui/components/DialogComponents.kt").readText()

    @Test
    fun `compact icon overload delegates to shared ink button`() {
        assertTrue(inkButton.contains("customWidth = 52.dp"))
        assertTrue(inkButton.contains("customHeight = 52.dp"))
        assertTrue(inkButton.contains("content = content"))
    }

    @Test
    fun `shared ink button retains explicit minimum dimensions`() {
        assertTrue(inkButton.contains("customWidth?.coerceAtLeast(48.dp)"))
        assertTrue(inkButton.contains("customHeight?.coerceAtLeast(48.dp)"))
        assertTrue(inkButton.contains("Modifier.widthIn(min = 48.dp, max = width).height(height)"))
    }

    @Test
    fun `charm stepper and detail actions still use shared button`() {
        listOf("onDecrement", "onIncrement", "onShowDetail").forEach { action ->
            val actionStart = charmList.indexOf("onClick = $action")
            assertTrue(actionStart >= 0, "Missing compact action: $action")
            val nextLines = charmList.substring(actionStart).lineSequence().take(4).joinToString("\n")
            assertTrue(nextLines.contains("Modifier.size(40.dp)"), "Expected 40.dp request for $action")
        }
    }

    @Test
    fun `dialog buttons delegate to shared ink button`() {
        assertTrue(dialogButtons.contains("fun GildedDialogButton("))
        assertTrue(dialogButtons.contains("fun GildedDialogTextButton("))
        val primary = dialogButtons.substringAfter("fun GildedDialogButton(").substringBefore("fun GildedDialogTextButton(")
        val secondary = dialogButtons.substringAfter("fun GildedDialogTextButton(").substringBefore("// SKIN: forma")
        assertTrue(primary.contains("InkButton("))
        assertTrue(secondary.contains("InkButton("))
    }
}
