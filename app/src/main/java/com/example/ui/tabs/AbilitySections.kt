package com.example.ui.tabs
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
internal fun AbilityGridSection(
    habilidades: List<String>, martialArts: List<HabilidadeCustomizada>, sheet: CharacterSheet, viewModel: SheetViewModel,
    ratingStyle: RatingStyle, onSpecializationDelete: (Especializacao)->Unit, onMartialArtDelete: (HabilidadeCustomizada)->Unit,
    onAddMartialArt: ()->Unit, allowMartialArt: Boolean
) {
    val specializationsByAbility = remember(sheet.specializations) {
        sheet.specializations.groupBy { it.habilidade }
    }
    Box(Modifier.fillMaxWidth().background(ExaltedDarkSurface, MaterialTheme.shapes.medium).border(1.dp, ExaltedAccentBright, MaterialTheme.shapes.medium).padding(8.dp)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 620.dp) {
                val metade=(habilidades.size+1)/2
                val rows=buildAbilityGridRows(habilidades.take(metade), habilidades.drop(metade), martialArts)
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    rows.forEach { (left,right) -> Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.Top) {
                        AbilityGridSlotContent(left,sheet,viewModel,onSpecializationDelete,onMartialArtDelete,ratingStyle,Modifier.weight(1f),specializationsByAbility)
                        AbilityGridSlotContent(right,sheet,viewModel,onSpecializationDelete,onMartialArtDelete,ratingStyle,Modifier.weight(1f),specializationsByAbility)
                    }}
                    if (allowMartialArt) { Spacer(Modifier.height(4.dp)); AddMartialArtButton(onAddMartialArt) }
                }
            } else {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    habilidades.forEach { name -> AbilityCompactRowForName(name, ExaltedConstants.ABILITY_INDEX_BY_NAME[name] ?: -1, sheet, viewModel, onSpecializationDelete, ratingStyle, specializationsByAbility = specializationsByAbility) }
                    martialArts.forEach { ma -> AbilityCompactRowForName(ma.nome,0,sheet,viewModel,onSpecializationDelete,ratingStyle,ma,onMartialArtDelete, specializationsByAbility) }
                    if (allowMartialArt) { AddMartialArtButton(onAddMartialArt) }
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
internal fun SpecializationsSection(sheet: CharacterSheet) {
    val martialArtNames = remember(sheet.martialArts) { sheet.martialArts.mapTo(HashSet(sheet.martialArts.size)) { it.nome } }
    SectionHeader(title="Especialidades")
    if (sheet.specializations.isEmpty()) AppText("Nenhuma especialidade cadastrada. Toque e segure em uma habilidade para adicionar.", style=MaterialTheme.typography.bodySmall, color=ExaltedMuted, textAlign=androidx.compose.ui.text.style.TextAlign.Center, modifier=Modifier.padding(bottom=8.dp))
    else GildedCard(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=ExaltedDarkSurfaceVariant)) {
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            sheet.specializations.forEach { spec -> Surface(color=ExaltedDarkSurface, shape=MaterialTheme.shapes.small, border=BorderStroke(1.dp,ExaltedAmber.copy(alpha=.5f))) {
                AppText(if(spec.habilidade in martialArtNames) "Artes Marciais – ${spec.habilidade} – ${spec.nome}" else "${spec.habilidade} – ${spec.nome}", style=MaterialTheme.typography.bodySmall, color=ExaltedAccentBright, textAlign=androidx.compose.ui.text.style.TextAlign.Center, softWrap=false, modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp))
            }}
        }
    }
}

@Composable
internal fun MartialArtDialog(
    show: Boolean,
    draftName: String,
    blockedError: Boolean,
    viewModel: SheetViewModel,
    sheet: CharacterSheet,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onBlocked: () -> Unit,
    onCreated: () -> Unit
) {
    if (!show) return
    val estilosDisponiveis = remember(sheet.tipoPersonagem, sheet.martialArts) {
        val jaCadastrados = sheet.martialArts.mapTo(HashSet()) { it.nome.trim().lowercase() }
        viewModel.nomesEstilosArtesMarciaisParaCadastro(sheet)
            .filter { it.trim().lowercase() !in jaCadastrados }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText("Adicionar Arte Marcial", color = ExaltedAccentBright, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppText(
                    "Selecione um estilo de Arte Marcial cadastrado na Aba 8 (Encantos). A habilidade começa com o contador zerado; ao colocar pontos de 1 a 5, o personagem passa a cumprir os pré-requisitos dos Encantos daquele estilo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExaltedMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (estilosDisponiveis.isEmpty()) {
                    AppText(
                        "Não há estilos disponíveis para cadastrar (todos já foram adicionados ou o catálogo está vazio para este tipo de personagem).",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExaltedMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        estilosDisponiveis.forEach { nomeEstilo ->
                            val selecionado = draftName == nomeEstilo
                            Surface(
                                color = if (selecionado) ExaltedAccentBright.copy(alpha = 0.22f) else ExaltedDarkSurface,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(
                                    1.dp,
                                    if (selecionado) ExaltedAccentBright else ExaltedOutline.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .feedbackClickable { onDraftChange(nomeEstilo) }
                            ) {
                                AppText(
                                    nomeEstilo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selecionado) ExaltedAccentBright else ExaltedOnSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
                if (blockedError) {
                    AppText(
                        "Pré-requisitos: Briga • (pelo menos 1 ponto) e o Mérito \"Artista Marcial\" (exceto Sangue de Dragão). Verifique a Aba 4 (Habilidades) e a Aba 6 (Méritos).",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExaltedDangerCore,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        dismissButton = {
            GildedDialogButton(
                text = "Cadastrar",
                enabled = draftName.trim().isNotEmpty() && estilosDisponiveis.any { it == draftName },
                onClick = {
                    if (draftName.trim().isNotEmpty()) {
                        if (viewModel.addMartialArt(draftName.trim())) onCreated() else onBlocked()
                    }
                }
            )
        },
        confirmButton = { GildedDialogTextButton(text = "Cancelar", onClick = onDismiss) },
        containerColor = ExaltedDarkSurface
    )
}
