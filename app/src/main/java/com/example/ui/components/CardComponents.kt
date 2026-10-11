package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.theme.ExaltedGold


// ============================================================================
// ATENÇÃO: arquivo MISTO — componentes puramente visuais (GildedCard,
// SectionHeader, botões estilizados) convivem aqui com componentes que têm
// comportamento de verdade (RatingControl, CompactAutoSizeField, campos de
// texto com validação). Cada composable abaixo marcado com "// SKIN:" é
// seguro editar/reestilizar sem risco; qualquer coisa SEM essa marcação
// pode ter lógica própria — confira o corpo da função antes de mexer.
// ============================================================================

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedSubtleGoldBorderBrush
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMetalGoldDeep
import com.example.ui.theme.ExaltedStructuralMetalDeep
import com.example.ui.theme.ExaltedStructuralMetal
import com.example.ui.theme.ExaltedStructuralMetalShine
import com.example.ui.theme.ExaltVisualTemplate
import com.example.ui.theme.ExaltedActiveMotif
import com.example.ui.theme.ExaltedVisualMotif

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongPressCard(
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            // APPROVED VISUAL CUSTOMIZATION
            // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
            // Mesmo tratamento estrutural do GildedCard para cartões de interação.
            .drawWithContent {
                drawContent()
                val accent = ExaltedAccentBright
                val deep = ExaltedStructuralMetalDeep
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, deep, accent, Color.Transparent)),
                    start = Offset(8.dp.toPx(), 2.dp.toPx()),
                    end = Offset(size.width - 8.dp.toPx(), 2.dp.toPx()),
                    strokeWidth = 1.8f
                )
                // Mesma leitura de metal das placas ornamentadas, mantendo a
                // área de toque e a semântica de clique longo inalteradas.
                val sideWidth = 2.dp.toPx()
                drawLine(
                    brush = Brush.verticalGradient(listOf(accent.copy(alpha = .65f), deep.copy(alpha = .22f), deep.copy(alpha = .55f))),
                    start = Offset(sideWidth / 2f, 0f),
                    end = Offset(sideWidth / 2f, size.height),
                    strokeWidth = sideWidth
                )
                drawLine(
                    brush = Brush.verticalGradient(listOf(accent.copy(alpha = .65f), deep.copy(alpha = .22f), deep.copy(alpha = .55f))),
                    start = Offset(size.width - sideWidth / 2f, 0f),
                    end = Offset(size.width - sideWidth / 2f, size.height),
                    strokeWidth = sideWidth
                )
                drawLine(
                    color = accent.copy(alpha = 0.35f),
                    start = Offset(8.dp.toPx(), size.height - 2.dp.toPx()),
                    end = Offset(size.width - 8.dp.toPx(), size.height - 2.dp.toPx()),
                    strokeWidth = 1f
                )
            }
            .feedbackCombinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant),
        border = BorderStroke(
            1.25.dp,
            ExaltedSubtleGoldBorderBrush
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

// SKIN: card com moldura dourada dupla e filigrana ornamentada nos 4 cantos.
// Os "botões" de ajuste ficam em Dimens.kt (CardBorderWidth, CardCornerFlourishLength,
// CardCornerDiamondOffset/Size) e Color.kt (tons do degradê) — mude ali, não aqui.
@Composable
fun GildedCard(
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant.copy(alpha = 0.96f)),
    border: BorderStroke? = null,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    visualTemplate: ExaltVisualTemplate? = null,
    content: @Composable () -> Unit
) {
    val visual = visualTemplate
    val accent = visual?.accentBright ?: ExaltedAccentBright
    val deep = visual?.metalDeep ?: ExaltedStructuralMetalDeep
    val surface = visual?.surfaceVariant ?: ExaltedDarkSurfaceVariant
    val motif = if (visualTemplate == null) ExaltedActiveMotif else null
    Card(
        // Conteúdo e decoração não compartilham mais a mesma fase de desenho.
        // A identidade fica no container/borda; nada é pintado depois do conteúdo.
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surface.copy(alpha=.98f)),
        border = border ?: BorderStroke(1.25.dp, Brush.linearGradient(listOf(deep.copy(alpha=.82f),accent.copy(alpha=.86f),deep.copy(alpha=.82f)))),
        shape = shape,
        elevation = elevation
    ) { content() }
}

/**
 * Campo de texto compacto, pensado para caber lado a lado com outros campos
 * semelhantes (ex: Nome / Jogador / Conceito). Reduz automaticamente o
 * tamanho da fonte (rótulo e valor) quando o texto digitado não cabe na
 * largura disponível, até um limite de metade (proporção 2:1) do tamanho base.
 */
