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
    @Test
    fun `encounter charm and spell details remain long press only`() {
        val charm = source.substringAfter("groupAccumulatedEncounterCharms(encantosDaGaveta)")
            .substringBefore("if (mostrarDetalhes)")
        assertTrue(charm.contains("detectTapGestures(onLongPress = { mostrarDetalhes = true })"))
        val spell = source.substringAfter("npc.feiticos.forEach { feitico ->")
            .substringBefore("if (mostrarDetalhesFeitico)")
        assertTrue(spell.contains("detectTapGestures(onLongPress = { mostrarDetalhesFeitico = true })"))
        // A tap must not open these dialogs. Gesture/haptic timing needs device validation.
    }

}
