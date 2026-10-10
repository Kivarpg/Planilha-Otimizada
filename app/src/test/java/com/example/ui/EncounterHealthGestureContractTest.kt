package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Contrato estático da trilha de vitalidade de NPCs.
 * A propagação de gestos e a área de toque exigem validação de interface.
 */
class EncounterHealthGestureContractTest {
    private val source = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()

    @Test
    fun `health box keeps cycling damage on tap and extra box removal on long press`() {
        val healthBox = source.substringAfter("rowBoxes.forEach { box ->")
            .substringBefore("contentAlignment = Alignment.Center")
        assertTrue(healthBox.contains(".feedbackCombinedClickable("))
        assertTrue(healthBox.contains("onClick = { onCiclarDano(box.id) }"))
        assertTrue(healthBox.contains("onLongClick = if (ehExtra) ({ boxParaRemover = box }) else null"))
        assertTrue(!healthBox.contains(".feedbackOnPress("))
        assertTrue(!healthBox.contains(".pointerInput("))
    }
}
