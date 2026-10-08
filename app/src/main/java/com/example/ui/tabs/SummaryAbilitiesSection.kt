package com.example.ui.tabs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Aba 9 — Planilha usa a mesma composição numérica e os mesmos marcadores
// quadrados da Aba 11 — Encontros. Nenhuma regra de habilidade é alterada.
@Composable
internal fun SummaryAbilitiesSection(sheet: CharacterSheet) {
    GildedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            AppText(
                "3. Habilidades",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val casteOrAspectAbilities = remember(sheet.casteAbilities, sheet.tipoPersonagem, sheet.aspecto) {
                habilidadesDeCastaOuAspectoParaMarcador(sheet)
            }
            val favoredAbilities = remember(sheet.favoredAbilities) { sheet.favoredAbilities.toSet() }
            val compactPhone = LocalConfiguration.current.screenWidthDp < 480
            val visibleAbilities = remember(sheet.abilities, casteOrAspectAbilities, favoredAbilities) {
                ExaltedConstants.ALL_25_ABILITIES.filter { ability ->
                    val rating = sheet.abilities[ability] ?: 0
                    rating > 0 || ability in casteOrAspectAbilities || ability in favoredAbilities
                }
            }
            val columns = remember(visibleAbilities) {
                val columnSize = kotlin.math.ceil(visibleAbilities.size / 3.0).toInt().coerceAtLeast(1)
                visibleAbilities.chunked(columnSize)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                columns.forEach { columnAbilities ->
                    Column(modifier = Modifier.weight(1f)) {
                        columnAbilities.forEach { ability ->
                            val rating = sheet.abilities[ability] ?: 0
                            val isSpecial = ability in casteOrAspectAbilities || ability in favoredAbilities
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                SummarySpecialIndicator(marked = isSpecial)
                                AutoSizeAppText(
                                    text = "$ability $rating",
                                    modifier = Modifier.weight(1f),
                                    style = if (compactPhone) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                                    color = ExaltedOnSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxFontSize = if (compactPhone) 12.sp else 16.sp,
                                    minFontSize = 8.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                                if (sheet.supernalAbility == ability) {
                                    AppText(
                                        "Supernal",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ExaltedAccentBright,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Habilidades que devem receber o marcador quadrado de Casta/Aspecto na Aba 9.
 *
 * Solar usa [CharacterSheet.casteAbilities]. Sangue de Dragão também mantém
 * essa lista preenchida no modelo, mas a regra visual é resolvida explicitamente
 * pelo Aspecto para não depender de estado derivado eventualmente desatualizado.
 */
internal fun habilidadesDeCastaOuAspectoParaMarcador(sheet: CharacterSheet): Set<String> {
    val habilidadesDoAspecto = if (sheet.tipoPersonagem.isDragonBlooded()) {
        Aspecto.entries
            .firstOrNull { it.displayName == sheet.aspecto }
            ?.allowedAbilities()
            .orEmpty()
    } else {
        emptyList()
    }

    return (sheet.casteAbilities + habilidadesDoAspecto).toSet()
}
