package com.example.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InteractionFeedbackContractTest {
    private val feedback = File("src/main/java/com/example/ui/components/InteractionFeedback.kt").readText()
    private val personalData = File("src/main/java/com/example/ui/tabs/PersonalDataSections.kt").readText()

    @Test fun `dragon blooded aspect buttons remain connected to global feedback`() {
        val start = personalData.indexOf("private fun ItemAspecto")
        val end = personalData.indexOf("@Composable\ninternal fun CasteSection", start)
        val section = personalData.substring(start, end)
        assertTrue(section.contains(".feedbackClickable { viewModel.updateAspecto(aspecto) }"))
    }

    @Test fun `global haptic uses portable vibration with view fallback`() {
        assertTrue(feedback.contains("VibrationEffect.createOneShot"))
        assertTrue(feedback.contains("val amplitude = if (typing) 185 else 230"))
        assertTrue(feedback.contains("view.performHapticFeedback"))
        assertFalse(feedback.contains("FLAG_IGNORE_GLOBAL_SETTING"))
        assertTrue(feedback.contains("if (!vibrated)"))
    }

    @Test fun `haptic preference remains enabled by default`() {
        assertTrue(feedback.contains("getBoolean(HAPTIC, true)"))
    }
}
