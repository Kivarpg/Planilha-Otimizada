package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterSheet
import com.example.ui.components.AutoSizeText
import com.example.ui.components.GildedCard
import com.example.ui.components.GildedDialogButton
import com.example.ui.components.GildedDialogTextButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.dialogShape
import com.example.ui.components.gildedDialogBorder
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedOutline
import com.example.viewmodel.SheetViewModel

// Refatorado (2026-09, refatoração estrutural): as seções de Arma e
// Armadura, que antes eram ~500 linhas de código inline aqui misturado,
// viraram WeaponSection.kt e ArmorSection.kt — concerns genuinamente
// separados (nenhuma variável de estado é compartilhada entre elas, nem
// com o restante desta função), então a extração foi limpa. Este
// arquivo agora só cuida do wrapper (Column/scroll) e das seções que
// realmente pertencem a "Equipamentos" como um todo: Comitamento
// (resumo combinando arma+armadura) e Pertences. Comportamento idêntico
// ao de antes — só a organização do código mudou.
// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
fun EquipmentTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(7).exaltedContentStage(7)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW

        WeaponSection(sheet = sheet, viewModel = viewModel)

        Spacer(modifier = Modifier.height(7.dp))

        ArmorSection(sheet = sheet, viewModel = viewModel)

        Spacer(modifier = Modifier.height(7.dp))

        SectionHeader(title = "Comitamento")
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(7),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedOutline)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppText(
                    text = "Motes Comitados: ${sheet.comitamentoTotalCalculado()} / ${sheet.limiteComitamentoCalculado()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExaltedAmber,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        SectionHeader(title = "Pertences")
        OutlinedTextField(
            value = sheet.pertences,
            onValueChange = com.example.ui.components.rememberTypingFeedback { viewModel.updatePertences(it.take(1000)) },
            label = { AppText("") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(textAlign = TextAlign.Justify),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = com.example.ui.theme.ExaltedAccentBright,
                unfocusedBorderColor = ExaltedOutline.copy(alpha = 0.55f),
                focusedLabelColor = com.example.ui.theme.ExaltedAccentBright,
                unfocusedLabelColor = ExaltedMuted,
                cursorColor = ExaltedGold,
                focusedTextColor = ExaltedOnSurface,
                unfocusedTextColor = ExaltedOnSurface,
                focusedContainerColor = com.example.ui.theme.ExaltedDarkSurface,
                unfocusedContainerColor = com.example.ui.theme.ExaltedDarkSurface,
                focusedPlaceholderColor = ExaltedMuted,
                unfocusedPlaceholderColor = ExaltedMuted
            )
        )
    }
}

// Popup exibido ao comitar uma arma ou armadura Artefato: exige escolher de
// qual reserva de Essência os motes serão consumidos. Compartilhado entre
// WeaponSection e ArmorSection — por isso internal, não private.
@Composable
internal fun MoteSourceDialog(
    itemName: String,
    custoTotal: Int,
    motesPessoaisDisponiveis: Int,
    motesPerifericosDisponiveis: Int,
    onConfirm: (pessoais: Int, perifericos: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var modo by remember { mutableStateOf("Pessoais") }
    var mistoPessoaisTexto by remember { mutableStateOf("0") }
    var mistoPerifericosTexto by remember { mutableStateOf("0") }

    val pessoaisFinal = when (modo) {
        "Pessoais" -> custoTotal
        "Periféricos" -> 0
        else -> mistoPessoaisTexto.toIntOrNull() ?: 0
    }
    val perifericosFinal = when (modo) {
        "Pessoais" -> 0
        "Periféricos" -> custoTotal
        else -> mistoPerifericosTexto.toIntOrNull() ?: 0
    }
    val somaValida = pessoaisFinal + perifericosFinal == custoTotal
    val disponibilidadeValida = pessoaisFinal <= motesPessoaisDisponiveis && perifericosFinal <= motesPerifericosDisponiveis

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = {
            AppText(
                "Comitar $itemName",
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppText("Custo: $custoTotal motes de Essência", color = MaterialTheme.colorScheme.onSurface)
                listOf("Pessoais" to "Motes Pessoais", "Periféricos" to "Motes Periféricos", "Mistos" to "Motes Mistos").forEach { (chave, rotulo) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .feedbackClickable { modo = chave }
                    ) {
                        RadioButton(
                            selected = modo == chave,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = ExaltedAmber)
                        )
                        AppText(rotulo, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (modo == "Mistos") {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 360.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = mistoPessoaisTexto,
                                    onValueChange = com.example.ui.components.rememberTypingFeedback { mistoPessoaisTexto = it.filter { ch -> ch.isDigit() } },
                                    label = { AppText("Pessoais") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = mistoPerifericosTexto,
                                    onValueChange = com.example.ui.components.rememberTypingFeedback { mistoPerifericosTexto = it.filter { ch -> ch.isDigit() } },
                                    label = { AppText("Periféricos") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = mistoPessoaisTexto,
                                    onValueChange = com.example.ui.components.rememberTypingFeedback { mistoPessoaisTexto = it.filter { ch -> ch.isDigit() } },
                                    label = { AppText("Pessoais") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = mistoPerifericosTexto,
                                    onValueChange = com.example.ui.components.rememberTypingFeedback { mistoPerifericosTexto = it.filter { ch -> ch.isDigit() } },
                                    label = { AppText("Periféricos") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    if (!somaValida) {
                        AppText(
                            "A soma deve ser exatamente $custoTotal.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (!disponibilidadeValida) {
                    AppText(
                        "Motes insuficientes na reserva escolhida.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        dismissButton = {
            GildedDialogButton(
                text = "Confirmar",
                onClick = { onConfirm(pessoaisFinal, perifericosFinal) },
                enabled = somaValida && disponibilidadeValida
            )
        },
        confirmButton = {
            GildedDialogTextButton(text = "Cancelar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

// Grupo de opções com seleção única — usado nas categorias do popup de
// configuração de Arma/Armadura ("permitir apenas uma seleção por categoria").
// Compartilhado entre WeaponSection e ArmorSection — por isso internal.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryChipGroup(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                modifier = Modifier
                    .widthIn(min = 92.dp)
                    .padding(vertical = 2.dp),
                color = if (isSelected) ExaltedAmber.copy(alpha = 0.22f) else ExaltedDarkSurfaceVariant,
                shape = MaterialTheme.shapes.small,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ExaltedGold else ExaltedOutline),
                onClick = { onSelect(option) }
            ) {
                AutoSizeText(
                    text = option,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) ExaltedGold else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                    minFontSize = 8.sp,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
        }
    }
}