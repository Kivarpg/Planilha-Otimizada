package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalExaltedVisualTemplate

/**
 * Base visual segura das abas.
 *
 * A identidade vem exclusivamente do template composicional atual. Não há
 * Canvas, Path, clip manual nem símbolo decorativo de fundo: a estrutura usa
 * somente background/border para nunca interferir em conteúdo ou hit-testing.
 */
@Composable
fun Modifier.exaltedTabIdentity(chapter: Int = 0): Modifier {
    val visual = LocalExaltedVisualTemplate.current
    // Painel sutil, nunca uma chapa opaca sobre a imagem global.
    return this.background(visual.surface.copy(alpha = 0.10f))
}

@Composable
fun Modifier.exaltedContentStage(chapter: Int): Modifier {
    val visual = LocalExaltedVisualTemplate.current
    // O fundo permanece continuo em todas as abas; superficie de leitura
    // semitransparente protege texto sem esconder a paisagem.
    return this.background(
        Brush.verticalGradient(
            listOf(
                visual.surface.copy(alpha = 0.42f),
                visual.surfaceVariant.copy(alpha = 0.30f),
                visual.surface.copy(alpha = 0.42f)
            )
        )
    )
}

@Composable
fun Modifier.exaltedSectionPanel(
    chapter: Int = 0,
    backgroundColor: Color? = null
): Modifier {
    val visual = LocalExaltedVisualTemplate.current
    val background = backgroundColor ?: visual.surfaceVariant
    return this
        .background(
            Brush.verticalGradient(
                listOf(
                    visual.metalDeep.copy(alpha = 0.52f),
                    background,
                    background.copy(alpha = 0.98f),
                    visual.surface
                )
            ),
            shape = RoundedCornerShape(10.dp)
        )
        .border(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                listOf(
                    visual.metalDeep.copy(alpha = 0.92f),
                    visual.accentBright.copy(alpha = 0.88f),
                    visual.metalShine.copy(alpha = 0.82f),
                    visual.metalDeep.copy(alpha = 0.92f)
                )
            ),
            shape = RoundedCornerShape(10.dp)
        )
}

@Composable
fun Modifier.exaltedControlBand(
    surfaceColor: Color? = null,
    accentColor: Color? = null,
    accentAlpha: Float = 0.12f
): Modifier {
    val visual = LocalExaltedVisualTemplate.current
    val surface = surfaceColor ?: visual.surface
    val accent = accentColor ?: visual.accentBright
    return this.background(
        Brush.horizontalGradient(
            listOf(
                surface,
                accent.copy(alpha = accentAlpha.coerceIn(0f, 1f)),
                surface
            )
        )
    )
}

@Composable
fun Modifier.exaltedExistingPanel(): Modifier = exaltedSectionPanel()
