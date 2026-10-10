package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Baseline estático dos controles compactos. Não substitui testes instrumentados
 * de bounds, hit targets, fonte ampliada ou capturas de tela.
 *
 * Protege as ações sem congelar tamanhos locais que precisam de validação visual.
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
            val pattern = Regex("InkButton\\s*\\(\\s*onClick\\s*=\\s*" + action + "\\b")
            assertTrue(pattern.containsMatchIn(charmList), "Missing InkButton action: $action")
        }
    }

    @Test
    fun `dialog buttons delegate to shared ink button`() {
        assertTrue(dialogButtons.contains("fun GildedDialogButton("))
        assertTrue(dialogButtons.contains("fun GildedDialogTextButton("))
        val primary = dialogButtons.substringAfter("fun GildedDialogButton(").substringBefore("fun GildedDialogTextButton(")
        val secondary = dialogButtons.substringAfter("fun GildedDialogTextButton(")
        assertTrue(primary.contains("InkButton("))
        assertTrue(secondary.contains("InkButton("))
    }
    @Test
    fun `charm InkButtons do not duplicate press feedback`() {
        listOf("onDecrement", "onIncrement", "onShowDetail").forEach { action ->
            val start = charmList.indexOf("onClick = $action")
            assertTrue(start >= 0, "Missing action: $action")
            val snippet = charmList.substring(start).lineSequence().take(4).joinToString("\n")
            assertTrue(!snippet.contains("feedbackOnPress("), "Duplicate feedback on $action")
        }
        val pin = charmList.substringAfter("onClick = onTogglePin").substringBefore("val corCoracao")
        assertTrue(!pin.contains("feedbackOnPress("), "Duplicate feedback on pin action")
    }

    @Test
    fun `acquired charm preserves card tap long press and independent pin action`() {
        val acquired = charmList.substringAfter("internal fun AcquiredCharmCard(")
        assertTrue(acquired.contains(".feedbackCombinedClickable("))
        assertTrue(acquired.contains("onClick = onClick,"))
        assertTrue(acquired.contains("onLongClick = onLongPress,"))
        assertTrue(!acquired.substringBefore("color = ExaltedDarkSurfaceVariant").contains(".feedbackOnPress("))
        assertTrue(acquired.contains("onClick = onTogglePin"))
        assertTrue(acquired.contains("contentDescription = if (isPinned)"))
        // This is a structural contract only; nested gesture dispatch needs device testing.
    }

}
