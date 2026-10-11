package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalExaltedVisualTemplate

/**
 * Componentes decorativos neutralizados.
 *
 * A antiga tentativa de identidade visual procedural foi removida:
 * sem nebulosas, estrelas, montanhas/telhados, molduras ornamentais,
 * medalhões desenhados, filigranas, losangos ou cantos decorativos.
 * As assinaturas permanecem para preservar chamadas existentes.
 */
@Composable
fun MedallionIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = com.example.ui.theme.Dimens.MedallionDefaultSize,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun PriorityShieldBadge(number: Int, modifier: Modifier = Modifier) {
    val visual = LocalExaltedVisualTemplate.current
    Box(
        modifier = modifier
            .size(26.dp)
            .background(visual.surface),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            number.toString(),
            color = visual.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun Starfield(modifier: Modifier = Modifier, starCount: Int = 140) {
    val visual = LocalExaltedVisualTemplate.current
    Box(modifier = modifier.background(visual.surface))
}

@Composable
fun OnyxTexturedBackground(
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.64f,
    content: @Composable () -> Unit
) {
    val visual = LocalExaltedVisualTemplate.current
    Box(modifier = modifier.background(visual.surface)) {
        content()
    }
}

fun Modifier.gildedFrame(): Modifier = this

@Composable
fun OrnateFlourish(modifier: Modifier = Modifier) {
    val visual = LocalExaltedVisualTemplate.current
    Box(
        modifier = modifier.background(
            visual.accentBright.copy(alpha = 0.10f),
            MaterialTheme.shapes.extraSmall
        )
    )
}
