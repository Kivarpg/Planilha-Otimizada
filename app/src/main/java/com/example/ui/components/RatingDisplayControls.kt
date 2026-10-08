package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RatingStyle
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedAmberVariant
import com.example.ui.theme.ExaltedAccentBright

@Composable
fun RatingControl(
    value: Int,
    maxValue: Int,
    style: RatingStyle,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 0,
    stepperSize: Dp = 40.dp
) {
    when (style) {
        RatingStyle.STEPPER -> {
            val raioAnel = stepperSize.coerceIn(18.dp, 27.dp)
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RingStepSymbol(
                    symbol = "−",
                    onClick = { onValueChange((value - 1).coerceAtLeast(minValue)) },
                    size = raioAnel,
                    enabled = value > minValue
                )
                AppText(
                    text = "$value",
                    color = ExaltedAccentBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(20.dp),
                    textAlign = TextAlign.Center
                )
                RingStepSymbol(
                    symbol = "+",
                    onClick = { onValueChange((value + 1).coerceAtMost(maxValue)) },
                    size = raioAnel,
                    enabled = value < maxValue
                )
            }
        }
        RatingStyle.DIAMOND -> {
            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..maxValue) {
                    val filled = i <= value
                    DiamondPip(
                        filled = filled,
                        onClick = {
                            // Tocar no losango já preenchido mais à direita reduz em 1;
                            // tocar em qualquer outro define o valor até ali.
                            val novo = if (value == i) i - 1 else i
                            onValueChange(novo.coerceIn(minValue, maxValue))
                        }
                    )
                }
            }
        }
    }
}

// Variante somente leitura do RatingControl — para exibir um valor já
// definido (ex.: Méritos já cadastrados), sem permitir edição direta ali.

@Composable
fun RatingDisplay(value: Int, maxValue: Int, style: RatingStyle, modifier: Modifier = Modifier) {
    when (style) {
        RatingStyle.STEPPER -> {
            Box(
                modifier = modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(ExaltedAmber.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                AppText("Nível $value", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ExaltedAmber)
            }
        }
        RatingStyle.DIAMOND -> {
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..maxValue) {
                    DiamondPip(filled = i <= value, onClick = {}, enabled = false)
                }
            }
        }
    }
}


@Composable
private fun DiamondPip(filled: Boolean, onClick: () -> Unit, size: Dp = 15.dp, enabled: Boolean = true) {
    Canvas(
        modifier = Modifier
            .size(size)
            .feedbackClickable(enabled = enabled, onClick = onClick)
    ) {
        val r = this.size.minDimension / 2f
        val centro = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)
        if (filled) {
            drawCircle(color = ExaltedAccentBright, radius = r, center = centro)
        } else {
            drawCircle(color = ExaltedAmberVariant.copy(alpha = 0.6f), radius = r, center = centro, style = Stroke(width = 1.4f))
        }
    }
}

/** Botão +/- da skin. A área de toque é preservada, mas o elemento visual
 * reproduz o pequeno marcador dourado da referência. */
// SKIN: cor de "+"/"−" vem de Color.kt (ExaltedStepperNeutral/Dim). Puramente
// visual — a lógica de incrementar/decrementar mora em quem chama onClick.
