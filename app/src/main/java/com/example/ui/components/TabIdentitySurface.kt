package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ExaltedStructuralMetalDeep
import com.example.ui.theme.ExaltedStructuralMetalShine
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedOutline

/**
 * Base visual neutra das abas.
 *
 * A antiga identidade ornamental desenhada em Compose foi removida:
 * sem sol/lua, molduras temáticas, trilhos, zigue-zagues, arcos,
 * névoa, coroas, marcas de canto ou ornamentos por Tipo de Exaltado.
 * Estes modificadores permanecem apenas para preservar as chamadas
 * existentes e evitar qualquer alteração funcional nas telas.
 */
fun Modifier.exaltedTabIdentity(chapter: Int = 0): Modifier = this

fun Modifier.exaltedContentStage(chapter: Int): Modifier = this.background(
    Brush.verticalGradient(
        listOf(
            ExaltedDarkSurface,
            ExaltedDarkSurfaceVariant.copy(alpha = 0.34f),
            ExaltedDarkSurface
        )
    )
)

fun Modifier.exaltedSectionPanel(
    chapter: Int = 0,
    backgroundColor: Color = ExaltedDarkSurfaceVariant
): Modifier = this
    .background(
        Brush.verticalGradient(
            listOf(
                ExaltedStructuralMetalDeep.copy(alpha = 0.62f),
                backgroundColor,
                backgroundColor.copy(alpha = 0.98f),
                ExaltedDarkSurface
            )
        )
    )
    .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(
            listOf(
                ExaltedStructuralMetalDeep.copy(alpha = 0.94f),
                ExaltedAccentBright.copy(alpha = 0.92f),
                ExaltedStructuralMetalShine.copy(alpha = 0.90f),
                ExaltedStructuralMetalDeep.copy(alpha = 0.94f)
            )
        ),
        shape = RoundedCornerShape(10.dp)
    )

fun Modifier.exaltedControlBand(
    surfaceColor: Color = ExaltedDarkSurface,
    accentColor: Color = ExaltedAccentBright,
    accentAlpha: Float = 0.12f
): Modifier = this.background(
    Brush.horizontalGradient(
        listOf(
            surfaceColor,
            accentColor.copy(alpha = accentAlpha.coerceIn(0f, 1f)),
            surfaceColor
        )
    )
)

fun Modifier.exaltedExistingPanel(): Modifier = exaltedSectionPanel()
