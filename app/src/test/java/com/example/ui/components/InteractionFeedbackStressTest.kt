package com.example.ui.components

import android.view.View
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InteractionFeedbackStressTest {
    @Test
    fun disabledFeedback_isSafe() {
        val context = RuntimeEnvironment.getApplication()
        InteractionFeedback.setHapticEnabled(context, false)
        InteractionFeedback.setSoundEnabled(context, false)

        val view = View(context)
        InteractionFeedback.perform(view)

        assertTrue(true)
    }

    @Test
    fun rapidRepeatedFeedback_doesNotPropagateExceptions() {
        val context = RuntimeEnvironment.getApplication()
        InteractionFeedback.setHapticEnabled(context, true)
        InteractionFeedback.setSoundEnabled(context, true)

        val view = View(context)
        repeat(if (System.getProperty("exalted.fullAudit") == "true") 1000 else 100) {
            InteractionFeedback.perform(view)
        }

        assertTrue(true)
    }
}
