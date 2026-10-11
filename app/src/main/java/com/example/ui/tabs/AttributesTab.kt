package com.example.ui.tabs

import androidx.compose.runtime.getValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterSheet
import com.example.model.ExaltedConstants
import com.example.model.isLunar
import com.example.ui.components.GildedCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalExaltedVisualTemplate
import com.example.viewmodel.SheetViewModel

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
private fun PriorityBadge(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = LocalExaltedVisualTemplate.current
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) visual.accentBright.copy(alpha = 0.46f) else visual.surfaceVariant)
            .border(
                1.dp,
                if (isSelected) visual.accentBright else visual.outline.copy(alpha = 0.72f),
                MaterialTheme.shapes.small
            )
            .feedbackClickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) visual.accentBright else visual.muted,
            maxLines = 1
        )
    }
}

@Composable
private fun AttributeRow(
    name: String,
    value: Int,
    ratingStyle: com.example.model.RatingStyle,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    minVal: Int = 1,
    maxVal: Int = 5,
    // Indicadores separados do Lunar: quadrado = Atributo de Casta;
    // estrela = Atributo Favorecido adicional. A separação evita que a
    // interface sugira que os 2 + 2 são uma lista única.
    favoritoInfo: FavoritoAtributoInfo? = null
) {
    val visual = LocalExaltedVisualTemplate.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (favoritoInfo != null) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(18.dp)
                    .background(if (favoritoInfo.ehCasta) visual.accentBright else Color.Transparent)
                    .border(1.5.dp, if (favoritoInfo.ehCasta) visual.accentBright else visual.outline)
                    .feedbackClickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                if (favoritoInfo.ehCasta) {
                    AppText("C", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = visual.onSurface)
                }
            }
            AppText(
                text = if (favoritoInfo.ehFavorecido) "★" else "☆",
                color = if (favoritoInfo.ehFavorecido) visual.accentBright else visual.muted,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .feedbackClickable(enabled = !favoritoInfo.ehCasta) { favoritoInfo.onToggle() }
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            AppText(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.4.sp
                ),
                color = visual.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                maxLines = 2,
                softWrap = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        com.example.ui.components.RatingControl(
            value = value,
            maxValue = maxVal,
            minValue = minVal,
            style = ratingStyle,
            stepperSize = 36.dp,
            onValueChange = { novo -> if (novo > value) onIncrement() else if (novo < value) onDecrement() }
        )
    }
}

private data class FavoritoAtributoInfo(val ehCasta: Boolean, val ehFavorecido: Boolean, val onToggle: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttributeGroupCard(
    groupName: String,
    attributes: List<String>,
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    priorityLabel: String,
    ratingStyle: com.example.model.RatingStyle
) {
    val visual = LocalExaltedVisualTemplate.current
    val currentPriority = sheet.attributePriorities[groupName] ?: "1º"
    val totalAdded = attributes.sumOf { (sheet.attributes[it] ?: 1) - 1 }
    val baseGranted = when (currentPriority) {
        "1º" -> 8
        "2º" -> 6
        else -> 4
    }
    // Quanto ainda resta do orçamento do grupo — diminui conforme o usuário
    // distribui pontos nos atributos, chegando a 0 quando tudo foi gasto.
    val restante = (baseGranted - totalAdded).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .padding(vertical = 4.dp)
            .padding(top = 7.dp)
    ) {
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(3)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .padding(top = 4.dp)
            ) {
                // Cabeçalho da seção (prioridade) + total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    com.example.ui.components.PriorityShieldBadge(number = currentPriority.firstOrNull()?.digitToIntOrNull() ?: 1)
                    Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(visual.surface)
                        .border(1.dp, visual.accentBright.copy(alpha = 0.7f), MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppText(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                            color = visual.accentBright
                        )
                        AppText(
                            text = "$restante",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = visual.accentBright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Seletor de prioridade
            // Prioridade em linha propria: evita que o rotulo comprima
            // os tres botoes nas menores larguras de tela.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppText(
                    text = priorityLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = visual.muted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1º", "2º", "3º").forEach { prio ->
                        PriorityBadge(
                            text = prio,
                            isSelected = currentPriority == prio,
                            onClick = { viewModel.setGroupPriority(groupName, prio) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Linhas de atributos
            val ehLunar = sheet.tipoPersonagem.isLunar()
            attributes.forEach { attrName ->
                val rating = sheet.attributes[attrName] ?: 1
                AttributeRow(
                    name = attrName,
                    value = rating,
                    ratingStyle = ratingStyle,
                    onDecrement = { viewModel.setAttributeRating(attrName, rating - 1) },
                    onIncrement = { viewModel.setAttributeRating(attrName, rating + 1) },
                    favoritoInfo = if (ehLunar) FavoritoAtributoInfo(
                        ehCasta = attrName in sheet.lunarCasteAttributesEscolhidos,
                        ehFavorecido = attrName in sheet.favoredAttributes,
                        onToggle = { viewModel.toggleFavoredAttribute(attrName) }
                    ) else null
                )
            }
        }
        }

        // Faixa estrutural com o nome do grupo, sobreposta à borda superior.
        // Sem símbolos ou ornamentos desenhados manualmente.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-11).dp)
                .background(visual.surface)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppText(
                text = groupName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = visual.accentBright
            )
        }
    }
}

@Composable
fun AttributesTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    val ratingStyle by viewModel.ratingStyle.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(3).exaltedContentStage(3)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AttributeGroupCard(
            groupName = "Físicos",
            attributes = ExaltedConstants.PHYSICAL_ATTRIBUTES,
            sheet = sheet,
            viewModel = viewModel,
            priorityLabel = "Prioridade:",
            ratingStyle = ratingStyle
        )

        AttributeGroupCard(
            groupName = "Sociais",
            attributes = ExaltedConstants.SOCIAL_ATTRIBUTES,
            sheet = sheet,
            viewModel = viewModel,
            priorityLabel = "Prioridade:",
            ratingStyle = ratingStyle
        )

        AttributeGroupCard(
            groupName = "Mentais",
            attributes = ExaltedConstants.MENTAL_ATTRIBUTES,
            sheet = sheet,
            viewModel = viewModel,
            priorityLabel = "Prioridade:",
            ratingStyle = ratingStyle
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
