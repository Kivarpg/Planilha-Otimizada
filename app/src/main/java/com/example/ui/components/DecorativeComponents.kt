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
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedDarkBackground
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedOnSurface

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
    Box(
        modifier = modifier
            .size(26.dp)
            .background(ExaltedDarkSurface),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            number.toString(),
            color = ExaltedOnSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun Starfield(modifier: Modifier = Modifier, starCount: Int = 140) {
    Box(modifier = modifier.background(ExaltedDarkBackground))
}

@Composable
fun OnyxTexturedBackground(
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.64f,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.background(ExaltedDarkBackground)) {
        content()
    }
}

fun Modifier.gildedFrame(): Modifier = this

@Composable
fun OrnateFlourish(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            ExaltedAccentBright.copy(alpha = 0.10f),
            MaterialTheme.shapes.extraSmall
        )
    )
}
