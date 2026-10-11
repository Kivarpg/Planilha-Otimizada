package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme

/**
 * Contexto visual único para componentes compartilhados.
 *
 * A ficha fornece o template do Tipo de Exaltado ativo em ExaltedTheme.
 * Blocos independentes, como um NPC da Aba 11, podem sobrescrever apenas
 * este escopo sem alterar a identidade global da ficha.
 */
val LocalExaltedVisualTemplate = staticCompositionLocalOf {
    visualTemplatePorMotif(ExaltedVisualMotif.SOLAR)
}

@Composable
fun ProvideExaltedVisualTemplate(
    template: ExaltVisualTemplate,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExaltedVisualTemplate provides template,
        content = {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = template.accent,
                    onPrimary = corTextoContraste(template.accent),
                    primaryContainer = template.surfaceVariant,
                    onPrimaryContainer = template.onSurface,
                    secondary = template.metalShine,
                    onSecondary = corTextoContraste(template.metalShine),
                    secondaryContainer = template.surfaceVariant,
                    onSecondaryContainer = template.onSurface,
                    tertiary = template.accentBright,
                    onTertiary = corTextoContraste(template.accentBright),
                    background = template.surface,
                    onBackground = template.onSurface,
                    surface = template.surface,
                    onSurface = template.onSurface,
                    surfaceVariant = template.surfaceVariant,
                    onSurfaceVariant = template.onSurface,
                    outline = template.outline,
                    outlineVariant = template.divider
                ),
                content = content
            )
        }
    )
}
