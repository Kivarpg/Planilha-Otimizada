package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltVisualTemplate
import com.example.ui.theme.LocalExaltedVisualTemplate
import com.example.ui.theme.ProvideExaltedVisualTemplate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongPressCard(
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val visual = LocalExaltedVisualTemplate.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .feedbackCombinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = visual.surfaceVariant),
        border = BorderStroke(
            1.25.dp,
            Brush.linearGradient(listOf(visual.metalDeep, visual.outline, visual.metalDeep))
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
    colors: androidx.compose.material3.CardColors? = null,
    border: BorderStroke? = null,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    visualTemplate: ExaltVisualTemplate? = null,
    content: @Composable () -> Unit
) {
    val visual = visualTemplate ?: LocalExaltedVisualTemplate.current
    val accent = visual.accentBright
    val deep = visual.metalDeep
    val surface = visual.surfaceVariant
    Card(
        // Conteúdo e decoração não compartilham mais a mesma fase de desenho.
        // A identidade fica no container/borda; nada é pintado depois do conteúdo.
        modifier = modifier,
        colors = colors ?: CardDefaults.cardColors(containerColor = surface.copy(alpha = .98f)),
        border = border ?: BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    deep.copy(alpha = .96f),
                    visual.outline,
                    accent.copy(alpha = .96f),
                    visual.metalShine,
                    deep.copy(alpha = .96f)
                )
            )
        ),
        shape = shape,
        elevation = elevation
    ) {
        ProvideExaltedVisualTemplate(visual) {
            content()
        }
    }
}

/**
 * Campo de texto compacto, pensado para caber lado a lado com outros campos
 * semelhantes (ex: Nome / Jogador / Conceito). Reduz automaticamente o
 * tamanho da fonte (rótulo e valor) quando o texto digitado não cabe na
 * largura disponível, até um limite de metade (proporção 2:1) do tamanho base.
 */
