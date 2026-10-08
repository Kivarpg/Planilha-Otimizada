package com.example.ui.tabs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Aba 9 — Planilha segue o padrão numérico compacto da Aba 11 — Encontros.
// Os valores continuam sendo somente leitura; esta alteração é exclusivamente
// visual e não modifica atributos nem suas regras de criação.
@Composable
internal fun SummaryAttributesSection(sheet: CharacterSheet) {
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
                "2. Atributos",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val groups = listOf(
                "Físicos" to ExaltedConstants.PHYSICAL_ATTRIBUTES,
                "Sociais" to ExaltedConstants.SOCIAL_ATTRIBUTES,
                "Mentais" to ExaltedConstants.MENTAL_ATTRIBUTES
            )

            val compactPhone = LocalConfiguration.current.screenWidthDp < 480
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compactPhone) 4.dp else 12.dp)
            ) {
                groups.forEach { (_, attributes) ->
                    Column(modifier = Modifier.weight(1f)) {
                        attributes.forEach { attribute ->
                            val rating = sheet.attributes[attribute] ?: 1
                            val marked = sheet.tipoPersonagem.isLunar() &&
                                (attribute in sheet.lunarCasteAttributesEscolhidos || attribute in sheet.favoredAttributes)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = if (compactPhone) 2.dp else 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (compactPhone) 2.dp else 5.dp)
                            ) {
                                if (sheet.tipoPersonagem.isLunar()) {
                                    SummarySpecialIndicator(marked = marked, compact = compactPhone)
                                }
                                AutoSizeAppText(
                                    text = "$attribute $rating",
                                    modifier = Modifier.weight(1f),
                                    style = if (compactPhone) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                                    color = ExaltedOnSurface,
                                    maxFontSize = if (compactPhone) 12.sp else 16.sp,
                                    minFontSize = 8.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
