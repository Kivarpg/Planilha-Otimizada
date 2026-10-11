package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalExaltedVisualTemplate
import com.example.ui.theme.corTextoContraste

@Composable
fun SyncedCounterColumn(
    label: String,
    value: Int,
    step: Int,
    minVal: Int,
    maxVal: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = LocalExaltedVisualTemplate.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppText(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            InkButton(
                onClick = { if (value - step >= minVal) onValueChange(value - step) },
                enabled = value - step >= minVal
            ) {
                AppText("-", style = MaterialTheme.typography.titleLarge, color = visual.accentBright, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = visual.accentBright.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, visual.accentBright.copy(alpha = 0.5f))
            ) {
                AppText(
                    text = "$value",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = visual.accentBright,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
            InkButton(
                onClick = { if (value + step <= maxVal) onValueChange(value + step) },
                enabled = value + step <= maxVal
            
            ) {
                AppText("+", style = MaterialTheme.typography.titleLarge, color = visual.accentBright, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// SKIN: pequeno botão −/+ em anel, usado por RatingControl (modo Stepper).
// Cores de "+"/"−" e do estado desativado vêm de Color.kt.

@Composable
internal fun RingStepSymbol(symbol: String, onClick: () -> Unit, size: Dp, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val visual = LocalExaltedVisualTemplate.current
    val corPreenchimento = if (enabled) visual.accent else visual.muted.copy(alpha = 0.20f)
    val corBorda = if (enabled) visual.accentBright else visual.muted.copy(alpha = 0.45f)
    val corSimbolo = if (enabled) corTextoContraste(corPreenchimento) else visual.muted.copy(alpha = 0.6f)
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(corPreenchimento)
            .border(
                width = com.example.ui.theme.Dimens.StepperRingStroke,
                color = corBorda,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .feedbackClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = symbol,
            fontFamily = com.example.ui.theme.ExaltedSymbolFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = corSimbolo
        )
    }
}

// Controle de avaliação unificado: renderiza como trilha de círculos ou
// como stepper −/número/+, conforme a preferência global escolhida em
// Configurações. Usado em Atributos, Habilidades e Méritos.
