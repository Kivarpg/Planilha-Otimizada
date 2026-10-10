package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Casta
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedAmberVariant
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedDivider
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedMetalGoldCore
import com.example.ui.theme.ExaltedMetalGoldDeep
import com.example.ui.theme.ExaltedMetalGoldShine

@Composable
fun GildedStepButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = com.example.ui.theme.Dimens.StepperRingSize,
    iconFontSize: TextUnit = 20.sp
) {
    // Neutro dourado — sem semântica vermelho/verde entre "+" e "−" (mesma
    // decisão de design do RingStepSymbol, Atributos/Habilidades/Méritos):
    // incrementar e decrementar são ações igualmente válidas de edição,
    // não "bom" vs "ruim".
    val corPreenchimento = if (enabled) com.example.ui.theme.ExaltedStepperNeutral else ExaltedMuted.copy(alpha = 0.25f)
    val symbolColor = if (enabled) Color.White else ExaltedMuted.copy(alpha = 0.55f)
    val borderBrush = if (enabled) {
        Brush.linearGradient(listOf(ExaltedAccentBright, ExaltedAmberVariant, ExaltedAccentBright))
    } else {
        Brush.linearGradient(listOf(ExaltedDivider, ExaltedDivider, ExaltedDivider))
    }
    Box(
        modifier = modifier
            .size(if (size < 52.dp) 52.dp else size)
            .feedbackClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size.coerceAtMost(50.dp))
                .clip(MaterialTheme.shapes.small)
                .background(corPreenchimento)
                .border(com.example.ui.theme.Dimens.StepperRingStroke, borderBrush, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = symbol,
                fontFamily = com.example.ui.theme.ExaltedSymbolFont,
                fontSize = iconFontSize,
                fontWeight = FontWeight.SemiBold,
                color = symbolColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// Regra de layout padrão do app: o texto de rótulo ocupa sempre uma linha única e
// inteira; o elemento gráfico (contador/trilha) fica posicionado na linha abaixo.

@Composable
fun ErgonomicNumericSelector(
    label: String,
    value: Int,
    minVal: Int = 0,
    maxVal: Int = 5,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = ExaltedAmber,
    enlargedDots: Boolean = false,
    invertButtons: Boolean = false,
    buttonsOnSameSide: Boolean = false,
    showSpentBelow: Boolean = false,
    showDots: Boolean = false,
    // Reduz botões/paddings para caber em colunas estreitas (ex.: 3 grupos de
    // atributos lado a lado). Não altera nenhum tamanho de fonte.
    compact: Boolean = false,
    // Centraliza o texto do rótulo (usado na aba Atributos).
    centerLabel: Boolean = false,
    // Sobrescreve o tamanho da fonte do rótulo (usado na aba Atributos, para
    // aumentar levemente a fonte dos nomes dos atributos).
    labelFontSize: TextUnit? = null,
    // Sobrescreve o tamanho do ícone e do botão +/- quando não estão do mesmo lado
    // (usado na aba Atributos, para aumentar os sinais de − e +).
    sideIconSizeOverride: Dp? = null,
    sideButtonSizeOverride: Dp? = null,
    // Exibe a badge (Casta/Favorecida/Supernal) do lado direito, na mesma linha
    // do rótulo, em vez de abaixo do seletor (usado na aba Habilidades).
    topRightBadge: Boolean = false,
    badgeFontSize: TextUnit? = null,
    // Sobrescreve apenas o número "total" mostrado no texto "X / Y" — sem
    // afetar o limite real do botão "+" (maxVal continua sendo o teto de
    // fato). Usado quando o teto do stepper precisa ser menor que o total
    // de verdade (ex.: motes já comitados em equipamento reduzem quanto
    // ainda dá pra alocar, mas o total do personagem não muda).
    displayMax: Int? = null,
    // Sobrescreve o tamanho da fonte do marcador "X / Y" (usado nos
    // contadores de Motes, pra dar mais destaque ao número sem afetar
    // outros usos deste componente, como Atributos).
    markerFontSize: TextUnit? = null,
    // Sobrescreve o tamanho dos botões +/- quando ficam do mesmo lado
    // (buttonsOnSameSide = true) — mesmo motivo: usado nos Motes pra
    // reduzir o destaque dos botões sem afetar outros usos.
    sameSideButtonSizeOverride: Dp? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(ExaltedMetalGoldDeep, ExaltedMetalGoldShine, ExaltedMetalGoldCore, ExaltedMetalGoldDeep)
            )
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 6.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Linha única: texto do rótulo (e, quando topRightBadge, a badge
            // Casta/Favorecida/Supernal alinhada à direita na mesma linha).
            if (topRightBadge && !badgeText.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = labelFontSize ?: MaterialTheme.typography.bodyMedium.fontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (centerLabel) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                    ) {
                        AppText(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = badgeFontSize ?: MaterialTheme.typography.labelSmall.fontSize
                            ),
                            color = badgeColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            } else {
                AppText(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = labelFontSize ?: MaterialTheme.typography.bodyMedium.fontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (centerLabel) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Linha abaixo: elemento gráfico (contador / trilha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val iconFontSize = sideIconSizeOverride?.let { (it.value).sp } ?: 20.sp
                val decrementButton: @Composable (Dp) -> Unit = { buttonSize ->
                    GildedStepButton(
                        symbol = "−",
                        enabled = value > minVal,
                        onClick = { onValueChange(value - 1) },
                        size = buttonSize,
                        iconFontSize = iconFontSize
                    )
                }
                val incrementButton: @Composable (Dp) -> Unit = { buttonSize ->
                    GildedStepButton(
                        symbol = "+",
                        enabled = value < maxVal,
                        onClick = { onValueChange(value + 1) },
                        size = buttonSize,
                        iconFontSize = iconFontSize
                    )
                }

                val sideButtonSize = sideButtonSizeOverride ?: if (compact) 28.dp else 40.dp
                if (!buttonsOnSameSide) {
                    if (invertButtons) incrementButton(sideButtonSize) else decrementButton(sideButtonSize)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showDots) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..maxVal) {
                                val baseDotSize = if (maxVal > 5) 10.dp else 14.dp
                                val dotSize = if (enlargedDots) baseDotSize + 8.dp else baseDotSize
                                val isFilled = i <= value
                                val isFinalDot = i == maxVal
                                val fillColor = when {
                                    isFinalDot && isFilled -> ExaltedAccentBright
                                    enlargedDots -> ExaltedGold
                                    else -> ExaltedAmber
                                }
                                val glowColor = if (isFinalDot) ExaltedAccentBright else ExaltedGold
                                val borderWidth = if (enlargedDots || isFinalDot) 2.dp else 1.dp
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .then(
                                            if (isFilled)
                                                Modifier.shadow(
                                                    elevation = if (isFinalDot) 10.dp else 6.dp,
                                                    shape = CircleShape,
                                                    ambientColor = glowColor,
                                                    spotColor = glowColor
                                                )
                                            else Modifier
                                        )
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(if (isFilled) fillColor else ExaltedDarkSurfaceVariant)
                                        .border(borderWidth, fillColor, CircleShape)
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = ExaltedAmber.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                1.dp,
                                ExaltedAmber.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.padding(horizontal = if (compact) 2.dp else 8.dp)
                        ) {
                            AppText(
                                text = if (showSpentBelow) "${displayMax ?: maxVal} / $value" else "$value / ${displayMax ?: maxVal}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = markerFontSize ?: MaterialTheme.typography.titleMedium.fontSize
                                ),
                                color = ExaltedAmber,
                                forceStroke = true,
                                modifier = Modifier.padding(horizontal = if (compact) 6.dp else 12.dp, vertical = if (compact) 2.dp else 4.dp)
                            )
                        }
                    }
                }

                if (buttonsOnSameSide) {
                    val sameSideSize = sameSideButtonSizeOverride ?: if (compact) 26.dp else 52.dp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        incrementButton(sameSideSize)
                        decrementButton(sameSideSize)
                    }
                } else {
                    if (invertButtons) decrementButton(sideButtonSize) else incrementButton(sideButtonSize)
                }
            }

            if (!topRightBadge && !badgeText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                ) {
                    AppText(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
