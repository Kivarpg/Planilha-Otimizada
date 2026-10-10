package com.example.ui.tabs
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

internal fun abilityRealIconRes(index: Int): Int {
    val recursos = listOf(
        com.example.R.drawable.ability_real_armas_brancas,
        com.example.R.drawable.ability_real_arqueirismo,
        com.example.R.drawable.ability_real_arremesso,
        com.example.R.drawable.ability_real_atletismo,
        com.example.R.drawable.ability_real_briga,
        com.example.R.drawable.ability_real_burocracia,
        com.example.R.drawable.ability_real_cavalgar,
        com.example.R.drawable.ability_real_conhecimento,
        com.example.R.drawable.ability_real_crime,
        com.example.R.drawable.ability_real_esquiva,
        com.example.R.drawable.ability_real_furtividade,
        com.example.R.drawable.ability_real_guerra,
        com.example.R.drawable.ability_real_integridade,
        com.example.R.drawable.ability_real_investigacao,
        com.example.R.drawable.ability_real_linguistica,
        com.example.R.drawable.ability_real_medicina,
        com.example.R.drawable.ability_real_navegacao,
        com.example.R.drawable.ability_real_ocultismo,
        com.example.R.drawable.ability_real_oficios,
        com.example.R.drawable.ability_real_performance,
        com.example.R.drawable.ability_real_presenca,
        com.example.R.drawable.ability_real_prontidao,
        com.example.R.drawable.ability_real_resistencia,
        com.example.R.drawable.ability_real_sobrevivencia,
        com.example.R.drawable.ability_real_socializacao
    )
    return recursos[index % recursos.size]
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AbilityCompactRow(
    name: String,
    rating: Int,
    badgeText: String?,
    badgeColor: Color,
    martialArtIcon: ImageVector?,
    abilityIconRes: Int?,
    onValueChange: (Int) -> Unit,
    ratingStyle: com.example.model.RatingStyle,
    modifier: Modifier = Modifier,
    specializationText: String? = null,
    // Único caso com 3 "nomes" na mesma especialização: Arte Marcial,
    // nome dado pelo usuário à arte, e a especialização em si. Quando
    // preenchido, tem prioridade sobre specializationText e renderiza os
    // 3 segmentos lado a lado (ou cada um em linha própria, se não
    // couber) em vez de uma única linha de texto corrida.
    specializationSegments: List<String>? = null,
    onLongPress: (() -> Unit)? = null
) {
    GildedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            com.example.ui.components.MedallionIcon(size = 40.dp) {
                if (martialArtIcon != null) {
                    Icon(
                        imageVector = martialArtIcon,
                        contentDescription = null,
                        tint = ExaltedAccentBright,
                        modifier = Modifier.size(21.dp)
                    )
                } else if (abilityIconRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = abilityIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(29.dp)
                    )
                }
            }
            val nameColumnModifier = if (onLongPress != null) {
                Modifier
                    .weight(1f)
                    .pointerInput(onLongPress) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                    .feedbackOnPress()
            } else {
                Modifier.weight(1f)
            }
            Column(modifier = nameColumnModifier, verticalArrangement = Arrangement.Center) {
                AppText(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                if (!badgeText.isNullOrBlank()) {
                    AppText(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!specializationSegments.isNullOrEmpty()) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        specializationSegments.forEach { segmento ->
                            AppText(
                                text = segmento,
                                style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = ExaltedMuted,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else if (!specializationText.isNullOrBlank()) {
                    AppText(
                        text = specializationText,
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = ExaltedMuted,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            com.example.ui.components.RatingControl(
                value = rating,
                maxValue = 5,
                minValue = 0,
                style = ratingStyle,
                stepperSize = 38.dp,
                onValueChange = { novo -> onValueChange(novo) }
            )
        }
    }
}

internal sealed class AbilityGridSlot {
    data class Ability(val name: String, val globalIndex: Int) : AbilityGridSlot()
    data class MartialArt(val item: HabilidadeCustomizada) : AbilityGridSlot()
}

// Monta as linhas do grid de 2 colunas: primeiro emparelha as habilidades padrão
// linha a linha; a 1ª Arte Marcial ocupa o espaço reservado ao final da coluna
// mais curta (abaixo de "Socialização"); as demais Artes Marciais preenchem as
// linhas seguintes em zigue-zague, da esquerda para a direita.
internal fun buildAbilityGridRows(
    firstColumn: List<String>,
    secondColumn: List<String>,
    martialArts: List<HabilidadeCustomizada>
): List<Pair<AbilityGridSlot?, AbilityGridSlot?>> {
    val rows = mutableListOf<Pair<AbilityGridSlot?, AbilityGridSlot?>>()
    val commonRows = minOf(firstColumn.size, secondColumn.size)

    for (i in 0 until commonRows) {
        rows.add(
            AbilityGridSlot.Ability(firstColumn[i], ExaltedConstants.ABILITY_INDEX_BY_NAME[firstColumn[i]] ?: -1) to
                AbilityGridSlot.Ability(secondColumn[i], ExaltedConstants.ABILITY_INDEX_BY_NAME[secondColumn[i]] ?: -1)
        )
    }

    var maIndex = 0
    firstColumn.drop(commonRows).forEach { name ->
        val right = martialArts.getOrNull(maIndex)?.let { AbilityGridSlot.MartialArt(it) }
        if (right != null) maIndex++
        rows.add(AbilityGridSlot.Ability(name, ExaltedConstants.ABILITY_INDEX_BY_NAME[name] ?: -1) to right)
    }
    secondColumn.drop(commonRows).forEach { name ->
        val left = martialArts.getOrNull(maIndex)?.let { AbilityGridSlot.MartialArt(it) }
        if (left != null) maIndex++
        rows.add(left to AbilityGridSlot.Ability(name, ExaltedConstants.ABILITY_INDEX_BY_NAME[name] ?: -1))
    }

    martialArts.drop(maIndex).chunked(2).forEach { chunk ->
        val left = AbilityGridSlot.MartialArt(chunk[0])
        val right = chunk.getOrNull(1)?.let { AbilityGridSlot.MartialArt(it) }
        rows.add(left to right)
    }
    return rows
}

@Composable
internal fun AbilityGridSlotContent(
    slot: AbilityGridSlot?,
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onSpecializationDelete: (Especializacao) -> Unit,
    onMartialArtDelete: (HabilidadeCustomizada) -> Unit,
    ratingStyle: com.example.model.RatingStyle,
    modifier: Modifier = Modifier,
    specializationsByAbility: Map<String, List<Especializacao>>
) {
    Box(modifier = modifier) {
        when (slot) {
            is AbilityGridSlot.Ability -> AbilityCompactRowForName(
                abName = slot.name,
                index = slot.globalIndex,
                sheet = sheet,
                viewModel = viewModel,
                onSpecializationDelete = onSpecializationDelete,
                ratingStyle = ratingStyle,
                specializationsByAbility = specializationsByAbility
            )
            is AbilityGridSlot.MartialArt -> AbilityCompactRowForName(
                abName = slot.item.nome,
                index = 0,
                sheet = sheet,
                viewModel = viewModel,
                onSpecializationDelete = onSpecializationDelete,
                martialArt = slot.item,
                onMartialArtDelete = onMartialArtDelete,
                ratingStyle = ratingStyle,
                specializationsByAbility = specializationsByAbility
            )
            null -> {}
        }
    }
}
