package com.example.ui

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedVisualMotif
import com.example.ui.theme.visualTemplatePorMotif
import com.example.ui.theme.corTextoContraste
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class VisualContrastAuditTest {
    private fun linear(channel: Float): Double =
        if (channel <= 0.03928f) channel.toDouble() / 12.92
        else ((channel.toDouble() + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @Test
    fun `all exalted surfaces keep readable text contrast`() {
        ExaltedVisualMotif.entries.forEach { motif ->
            val visual = visualTemplatePorMotif(motif)
            val surfaceRatio = contrast(visual.onSurface, visual.surface)
            val variantRatio = contrast(visual.onSurface, visual.surfaceVariant)
            assertTrue(surfaceRatio >= 4.5, "$motif surface contrast=$surfaceRatio")
            assertTrue(variantRatio >= 4.5, "$motif surfaceVariant contrast=$variantRatio")
        }
    }

    @Test
    fun `all exalted accents remain distinguishable from their surfaces`() {
        ExaltedVisualMotif.entries.forEach { motif ->
            val visual = visualTemplatePorMotif(motif)
            val surfaceRatio = contrast(visual.accentBright, visual.surface)
            val variantRatio = contrast(visual.accentBright, visual.surfaceVariant)
            assertTrue(surfaceRatio >= 3.0, "$motif accent/surface contrast=$surfaceRatio")
            assertTrue(variantRatio >= 3.0, "$motif accent/surfaceVariant contrast=$variantRatio")
        }
    }

    @Test
    fun `automatic button text contrast stays readable on themed fills`() {
        ExaltedVisualMotif.entries.forEach { motif ->
            val visual = visualTemplatePorMotif(motif)
            val fills = listOf(
                "accent" to visual.accent,
                "accentBright" to visual.accentBright,
                "metalShine" to visual.metalShine,
                "surface" to visual.surface,
                "surfaceVariant" to visual.surfaceVariant
            )
            fills.forEach { (name, background) ->
                val foreground = corTextoContraste(background)
                val ratio = contrast(foreground, background)
                assertTrue(ratio >= 4.5, "$motif $name automatic text contrast=$ratio")
            }
        }
    }

    @Test
    fun `dark cards declare light content color and shared texts have outline`() {
        val cards = java.io.File("src/main/java/com/example/ui/components/CardComponents.kt").readText()
        val texts = java.io.File("src/main/java/com/example/ui/components/TextComponents.kt").readText()
        val typography = java.io.File("src/main/java/com/example/ui/theme/Type.kt").readText()
        assertTrue(cards.contains("contentColor = visual.onSurface"))
        assertTrue(cards.contains("CardDefaults.cardColors(containerColor = surface.copy(alpha = .98f), contentColor = visual.onSurface)"))
        assertTrue(texts.contains("inheritedContentColor == Color.Black -> visual.onSurface"))
        assertTrue(texts.contains("shadow = style.shadow ?: ExaltedTextOutline"))
        assertTrue(typography.contains("val ExaltedTextOutline = Shadow("))
        assertTrue(typography.contains("shadow = ExaltedTextOutline"))
    }

    @Test
    fun `neutral text token itself is never transparent`() {
        assertTrue(ExaltedOnSurface.alpha == 1f)
    }
}
