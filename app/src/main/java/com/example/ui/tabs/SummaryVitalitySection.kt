package com.example.ui.tabs
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
internal fun SummaryVitalitySection(sheet: CharacterSheet, viewModel: SheetViewModel) {
// Vitalidade
GildedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        AppText("5. Trilha de vitalidade", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = ExaltedAmber, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        // Caixas interativas — mesma função do ViewModel usada na
        // Aba 5 (cycleHealthDamage), então tocar aqui ou lá afeta
        // o mesmo estado; nunca existem dois valores
        // independentes pra Trilha de Vitalidade. Agrupadas por
        // penalidade, na mesma ordem lógica da Aba 5.
        val ordemPenalidades = remember { listOf("-0", "-1", "-2", "-4", "Inc") }
        val gruposPorPenalidade = remember(sheet.healthBoxes) {
            val grupos = Array(5) { ArrayList<CaixaVitalidade>() }
            sheet.healthBoxes.forEach { caixa ->
                val rank = caixa.penaltyRank()
                if (rank in 0..4) grupos[rank].add(caixa)
            }
            grupos
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val labelWidth = 36.dp
            val boxSize = 40.dp
            val gap = 6.dp
            val availableForBoxes = (maxWidth - labelWidth).value.coerceAtLeast(boxSize.value)
            val maxPerRow = ((availableForBoxes + gap.value) / (boxSize.value + gap.value)).toInt().coerceAtLeast(1)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ordemPenalidades.forEachIndexed { penaltyIndex, penalidade ->
                    val caixas = gruposPorPenalidade[penaltyIndex]
                    if (caixas.isNotEmpty()) {
                        caixas.chunked(maxPerRow).forEachIndexed { rowIndex, rowBoxes ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (rowIndex == 0) {
                                    AppText(text = if (penalidade == "Inc") "Inc." else penalidade, style = MaterialTheme.typography.titleMedium, color = ExaltedMuted, maxLines = 1, softWrap = false, modifier = Modifier.width(labelWidth))
                                } else Spacer(modifier = Modifier.width(labelWidth))
                                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                    rowBoxes.forEach { box ->
                                        val (simbolo, cor) = when (box.tipoDano) {
                                            1 -> "/" to ExaltedDanoContundente
                                            2 -> "X" to ExaltedDanoLetal
                                            3 -> "*" to ExaltedDanoAgravado
                                            else -> "" to ExaltedMuted
                                        }
                                        Box(
                                            modifier = Modifier.size(boxSize).clip(RoundedCornerShape(6.dp)).background(ExaltedDarkSurface)
                                                .border(1.dp, ExaltedGold.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                .feedbackClickable { viewModel.cycleHealthDamage(box.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppText(text = simbolo, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = cor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            InkButton(
                label = "Limpar",
                onClick = { viewModel.clearHealthDamage() },
                size = InkButtonSize.Small,
                variant = InkButtonVariant.Secondary
            )
        }
    }
}
}
