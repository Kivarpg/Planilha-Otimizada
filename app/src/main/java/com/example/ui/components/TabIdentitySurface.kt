package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

fun Modifier.exaltedContentStage(chapter: Int): Modifier = this

fun Modifier.exaltedSectionPanel(chapter: Int = 0): Modifier = this
    .background(ExaltedDarkSurfaceVariant)

fun Modifier.exaltedControlBand(): Modifier = this
    .background(
        Brush.horizontalGradient(
            listOf(
                ExaltedDarkSurface,
                ExaltedAccentBright.copy(alpha = 0.035f),
                ExaltedDarkSurface
            )
        )
    )

fun Modifier.exaltedExistingPanel(): Modifier = exaltedSectionPanel()
