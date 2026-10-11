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
        modifier = modifier.drawWithContent {
            // 315: placa de obsidiana com duas leituras de metal, em vez da caixa plana da 314.
            drawContent()
            // O desenho ornamental fica sobre a placa, sem cobrir o conteúdo com preenchimento.
            // O degradê do fundo é responsabilidade da cor da superfície do Card.
            val outer = 1.35.dp.toPx()
            val inner = 5.dp.toPx()
            val flourish = when (motif) {
                ExaltedVisualMotif.SOLAR -> 22.dp.toPx()
                ExaltedVisualMotif.DRAGON_BLOODED -> 18.dp.toPx()
                ExaltedVisualMotif.LUNAR -> 15.dp.toPx()
                null -> 18.dp.toPx()
            }
            // Arquitetura ornamental visível: faixa superior de metal e lâminas
            // verticais nas bordas, sem desenhar sobre a área de leitura.
            val rail = when (motif) {
                ExaltedVisualMotif.SOLAR -> 3.5.dp.toPx()
                ExaltedVisualMotif.DRAGON_BLOODED -> 4.dp.toPx()
                ExaltedVisualMotif.LUNAR -> 2.dp.toPx()
                null -> 3.dp.toPx()
            }
            drawRect(
                brush = Brush.horizontalGradient(listOf(deep.copy(alpha = .72f), accent.copy(alpha = .36f), deep.copy(alpha = .72f))),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, rail)
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(accent.copy(alpha = .60f), deep.copy(alpha = .18f), deep.copy(alpha = .55f))),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(rail, size.height)
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(accent.copy(alpha = .60f), deep.copy(alpha = .18f), deep.copy(alpha = .55f))),
                topLeft = Offset(size.width - rail, 0f),
                size = androidx.compose.ui.geometry.Size(rail, size.height)
            )
            // Assinatura arquitetônica exclusiva de cada Tipo de Exaltado.
            // Todos os elementos ficam confinados aos 12dp periféricos do cartão.
            val emblemInk = accent.copy(alpha = .72f)
            val emblemX = size.width / 2f
            val emblemY = 3.dp.toPx()
            when (motif) {
                ExaltedVisualMotif.SOLAR -> {
                    val span = 34.dp.toPx()
                    drawLine(emblemInk, Offset(emblemX - span, emblemY), Offset(emblemX - 12.dp.toPx(), emblemY), 2.dp.toPx())
                    drawLine(emblemInk, Offset(emblemX + 12.dp.toPx(), emblemY), Offset(emblemX + span, emblemY), 2.dp.toPx())
                    drawCircle(emblemInk, 5.dp.toPx(), Offset(emblemX, emblemY), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx()))
                }
                ExaltedVisualMotif.DRAGON_BLOODED -> {
                    val teeth = 8.dp.toPx()
                    for (step in 0..2) {
                        val x = emblemX + (step - 1) * 14.dp.toPx()
                        drawLine(emblemInk, Offset(x - teeth / 2f, emblemY), Offset(x, emblemY + teeth), 1.5.dp.toPx())
                        drawLine(emblemInk, Offset(x, emblemY + teeth), Offset(x + teeth / 2f, emblemY), 1.5.dp.toPx())
                    }
                }
                ExaltedVisualMotif.LUNAR -> {
                    val arcSize = androidx.compose.ui.geometry.Size(28.dp.toPx(), 12.dp.toPx())
                    drawArc(emblemInk, 195f, 150f, false, Offset(emblemX - arcSize.width / 2f, 0f), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(1.25.dp.toPx()))
                }
                null -> Unit
            }
            // Filetes superior/inferior em degradê metálico.
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, deep, accent, deep, Color.Transparent)), Offset(8.dp.toPx(),2.dp.toPx()), Offset(size.width-8.dp.toPx(),2.dp.toPx()), outer)
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, deep.copy(.45f), accent.copy(.48f), deep.copy(.45f), Color.Transparent)), Offset(12.dp.toPx(),size.height-2.dp.toPx()), Offset(size.width-12.dp.toPx(),size.height-2.dp.toPx()), 1.dp.toPx())
            // Moldura interna cria profundidade real sem tocar no conteúdo.
            // Cantos internos chanfrados, com silhueta inspirada em placas de armadura.
            val bevel = 14.dp.toPx()
            val bevelInk = accent.copy(alpha = .64f)
            drawLine(bevelInk, Offset(inner, inner + bevel), Offset(inner + bevel, inner), 1.5.dp.toPx())
            drawLine(bevelInk, Offset(size.width - inner - bevel, inner), Offset(size.width - inner, inner + bevel), 1.5.dp.toPx())
            drawLine(deep.copy(alpha = .78f), Offset(inner, size.height - inner - bevel), Offset(inner + bevel, size.height - inner), 1.5.dp.toPx())
            drawLine(deep.copy(alpha = .78f), Offset(size.width - inner - bevel, size.height - inner), Offset(size.width - inner, size.height - inner - bevel), 1.5.dp.toPx())
            drawLine(deep.copy(.38f), Offset(inner,inner), Offset(size.width-inner,inner), 1.dp.toPx())
            drawLine(deep.copy(.26f), Offset(inner,size.height-inner), Offset(size.width-inner,size.height-inner), 1.dp.toPx())
            drawLine(deep.copy(.28f), Offset(inner,inner), Offset(inner,size.height-inner), 1.dp.toPx())
            drawLine(deep.copy(.28f), Offset(size.width-inner,inner), Offset(size.width-inner,size.height-inner), 1.dp.toPx())
            // Quatro cantos escalonados, próximos da referência ornamental.
            // Friso interior curto acrescenta profundidade sem cobrir campos do cartão.
            val topInset = 7.dp.toPx()
            val topFlourish = 22.dp.toPx()
            drawLine(
                Brush.horizontalGradient(listOf(deep.copy(alpha = .20f), accent.copy(alpha = .48f), deep.copy(alpha = .20f))),
                Offset(topInset + topFlourish, topInset),
                Offset(size.width - topInset - topFlourish, topInset),
                .75.dp.toPx()
            )
            drawLine(accent.copy(.88f),Offset(0f,0f),Offset(flourish,0f),outer); drawLine(accent.copy(.88f),Offset(0f,0f),Offset(0f,flourish),outer)
            drawLine(accent.copy(.70f),Offset(size.width,0f),Offset(size.width-flourish,0f),outer); drawLine(accent.copy(.70f),Offset(size.width,0f),Offset(size.width,flourish),outer)
            drawLine(deep.copy(.72f),Offset(0f,size.height),Offset(flourish,size.height),outer); drawLine(deep.copy(.72f),Offset(0f,size.height),Offset(0f,size.height-flourish),outer)
            drawLine(deep.copy(.72f),Offset(size.width,size.height),Offset(size.width-flourish,size.height),outer); drawLine(deep.copy(.72f),Offset(size.width,size.height),Offset(size.width,size.height-flourish),outer)
            // Cantoneiras orientais em duas camadas: gravura angular discreta, sem imagens de fundo.
            val inset = 9.dp.toPx()
            val notch = 9.dp.toPx()
            val engraving = when (motif) {
                ExaltedVisualMotif.SOLAR -> accent.copy(alpha = .62f)
                ExaltedVisualMotif.DRAGON_BLOODED -> deep.copy(alpha = .74f)
                ExaltedVisualMotif.LUNAR -> accent.copy(alpha = .42f)
                null -> accent.copy(alpha = .48f)
            }
            val engravingWidth = when (motif) {
                ExaltedVisualMotif.DRAGON_BLOODED -> 1.2.dp.toPx()
                ExaltedVisualMotif.LUNAR -> .75.dp.toPx()
                else -> .9.dp.toPx()
            }
            drawLine(engraving, Offset(inset, inset + notch), Offset(inset + notch, inset), engravingWidth)
            drawLine(engraving, Offset(size.width - inset - notch, inset), Offset(size.width - inset, inset + notch), engravingWidth)
            drawLine(deep.copy(alpha = .62f), Offset(inset, size.height - inset - notch), Offset(inset + notch, size.height - inset), .9.dp.toPx())
            drawLine(deep.copy(alpha = .62f), Offset(size.width - inset - notch, size.height - inset), Offset(size.width - inset, size.height - inset - notch), .9.dp.toPx())
            val cx=size.width/2f
            drawCircle(accent.copy(.30f), 2.6.dp.toPx(), Offset(cx,2.5.dp.toPx()))
            // Assinatura ornamental do Exaltado no próprio componente, não só no fundo da aba.
            when (motif) {
                ExaltedVisualMotif.SOLAR -> {
                    val cy=9.dp.toPx(); val r=3.2.dp.toPx()
                    drawCircle(accent.copy(.55f),r,Offset(cx,cy),style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                    drawLine(accent.copy(.45f),Offset(cx-9.dp.toPx(),cy),Offset(cx-r*1.5f,cy),.7.dp.toPx())
                    drawLine(accent.copy(.45f),Offset(cx+r*1.5f,cy),Offset(cx+9.dp.toPx(),cy),.7.dp.toPx())
                }
                ExaltedVisualMotif.DRAGON_BLOODED -> {
                    // Rubi/metal e cantos definem a linhagem; sem zigue-zague decorativo.
                    val cy=8.dp.toPx(); val r=2.8.dp.toPx()
                    val mark=androidx.compose.ui.graphics.Path().apply { moveTo(cx,cy-r); lineTo(cx+r,cy); lineTo(cx,cy+r); lineTo(cx-r,cy); close() }
                    drawPath(mark, accent.copy(.52f), style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                }
                ExaltedVisualMotif.LUNAR -> {
                    drawArc(accent.copy(.62f),205f,130f,false,Offset(cx-7.dp.toPx(),2.dp.toPx()),androidx.compose.ui.geometry.Size(14.dp.toPx(),10.dp.toPx()),style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                }
                null -> Unit // Template de NPC: a moldura usa apenas a paleta do NPC, sem o símbolo da ficha ativa.
            }
        },
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
