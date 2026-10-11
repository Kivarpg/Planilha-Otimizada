package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

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
        content = content
    )
}
