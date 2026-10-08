package com.example.ui.tabs

import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButtonVariant
import androidx.compose.foundation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.CharacterSheet
import com.example.model.Especializacao
import com.example.model.ExaltedConstants
import com.example.model.HabilidadeCustomizada
import com.example.model.isLunar
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AbilitiesTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    subTabIndex: Int,
    onSubTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var itemToDelete by remember { mutableStateOf<Especializacao?>(null) }
    var martialArtToDelete by remember { mutableStateOf<HabilidadeCustomizada?>(null) }
    var showMartialArtDialog by remember { mutableStateOf(false) }
    var martialArtBlockedError by remember { mutableStateOf(false) }
    var martialArtDraftName by remember { mutableStateOf("") }
    val ratingStyle by viewModel.ratingStyle.collectAsState()
    val habilidadesFiltradas = remember(
        subTabIndex,
        sheet.casteAbilities,
        sheet.favoredAbilities,
        sheet.abilities
    ) {
        when (subTabIndex) {
            1 -> ExaltedConstants.ALL_25_ABILITIES.filter { it in sheet.casteAbilities || it in sheet.favoredAbilities }
            2 -> ExaltedConstants.ALL_25_ABILITIES.filter { (sheet.abilities[it] ?: 0) > 0 }
            else -> ExaltedConstants.ALL_25_ABILITIES
        }
    }
    val martialArtsFiltradas = remember(sheet.martialArts, subTabIndex) {
        if (subTabIndex == 1) emptyList() else sheet.martialArts
    }

    Column(
        modifier = modifier.fillMaxSize().exaltedTabIdentity(4).exaltedContentStage(4).padding(horizontal = 10.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AbilitySubTabSelector(sheet, subTabIndex, onSubTabChange)
        Spacer(Modifier.height(6.dp))
        AbilityGridSection(
            habilidades = habilidadesFiltradas,
            martialArts = martialArtsFiltradas,
            sheet = sheet,
            viewModel = viewModel,
            ratingStyle = ratingStyle,
            onSpecializationDelete = { itemToDelete = it },
            onMartialArtDelete = { martialArtToDelete = it },
            onAddMartialArt = {
                martialArtDraftName = ""
                martialArtBlockedError = false
                showMartialArtDialog = true
            },
            allowMartialArt = when (subTabIndex) {
                0 -> true
                1 -> viewModel.possuiMeritoArtistaMarcial(sheet)
                2 -> sheet.martialArts.any { it.valor > 0 }
                else -> true
            }
        )
        Spacer(Modifier.height(20.dp))
        SpecializationsSection(sheet)
        Spacer(Modifier.height(5.dp))
    }
    MartialArtDialog(
        show = showMartialArtDialog,
        draftName = martialArtDraftName,
        blockedError = martialArtBlockedError,
        viewModel = viewModel,
        sheet = sheet,
        onDraftChange = { martialArtDraftName = it },
        onDismiss = { showMartialArtDialog = false },
        onBlocked = { martialArtBlockedError = true },
        onCreated = { showMartialArtDialog = false }
    )
    itemToDelete?.let { spec ->
        ConfirmDeleteDialog(itemTitle = spec.nome.ifBlank { "Especialidade" }, onConfirm = { viewModel.removeSpecialization(spec.id); itemToDelete = null }, onDismiss = { itemToDelete = null })
    }
    martialArtToDelete?.let { ma ->
        ConfirmDeleteDialog(itemTitle = ma.nome, onConfirm = { viewModel.removeMartialArt(ma.id); martialArtToDelete = null }, onDismiss = { martialArtToDelete = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilitySubTabSelector(sheet: CharacterSheet, selected: Int, onSelected: (Int) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 2) {
        val ehLunar = sheet.tipoPersonagem.isLunar()
        val labelsComIndice = if (ehLunar) {
            // Lunar não tem Habilidade de Casta nem Habilidade Favorecida
            // (só Atributo Favorecido) — essa sub-aba nunca teria nada pra
            // mostrar, então some pra esse template.
            listOf(0 to "Todas", 2 to "Com Pontos")
        } else {
            listOf(
                0 to "Todas",
                1 to if (sheet.tipoPersonagem == "SangueDeDragao") "Aspecto e Favorecidas" else "Casta e Favorecidas",
                2 to "Com Pontos"
            )
        }
        labelsComIndice.forEach { (i, label) ->
            val active = selected == i
            InkButton(label = label, onClick = { onSelected(i) }, modifier = Modifier.weight(1f), size = InkButtonSize.Small, fillMaxWidth = true, variant = if (active) InkButtonVariant.Primary else InkButtonVariant.Secondary)
        }
    }
}
