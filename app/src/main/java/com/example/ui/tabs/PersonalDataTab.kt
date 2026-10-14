package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.BoxNames
import com.example.model.CharacterSheet
import com.example.model.Intimidade
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
fun PersonalDataTab(sheet: CharacterSheet, viewModel: SheetViewModel, modifier: Modifier = Modifier) {
    var itemToDelete by remember { mutableStateOf<Intimidade?>(null) }
    var showIntimidadeDialog by remember { mutableStateOf(false) }
    var showLinguaDialog by remember { mutableStateOf(false) }
    var newIntimidadeNome by remember { mutableStateOf("") }
    var newIntimidadeTipo by remember { mutableStateOf("Laço") }
    var newIntimidadeIntensidade by remember { mutableStateOf("Menor") }
    // A arte pertence ao host global, nunca a uma aba isolada.
    Column(
        modifier.fillMaxSize()
            .exaltedTabIdentity(1).exaltedContentStage(1)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GildedCard(modifier = Modifier.fillMaxWidth().exaltedSectionPanel(1)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp)) {
                PersonalIdentitySection(sheet,viewModel)
            }
        }
        Spacer(Modifier.height(20.dp))
        AspectSelectionSection(sheet,viewModel)
        CasteSection(sheet,viewModel)
        Spacer(Modifier.height(24.dp))
        LanguageSection(sheet){showLinguaDialog=true}
        Spacer(Modifier.height(24.dp))
        IntimaciesSection(sheet,onOpenAdd={showIntimidadeDialog=true},onDelete={itemToDelete=it})
    }
    IntimidadeDialog(show=showIntimidadeDialog,nome=newIntimidadeNome,tipo=newIntimidadeTipo,intensidade=newIntimidadeIntensidade,onNome={newIntimidadeNome=it},onTipo={newIntimidadeTipo=it},onIntensidade={newIntimidadeIntensidade=it},onDismiss={showIntimidadeDialog=false},viewModel=viewModel)
    LinguaDialog(showLinguaDialog,sheet,{showLinguaDialog=false},viewModel)
    itemToDelete?.let { item -> ConfirmDeleteDialog(itemTitle=item.nome.ifBlank{"Intimidade"},onConfirm={viewModel.removeIntimidade(item.id);itemToDelete=null},onDismiss={itemToDelete=null}) }
}

/**
 * Garante que cada campo tenha largura real para seu rótulo.
 * Composição em três colunas só em áreas amplas, com escala de fonte normal.
 */
internal fun personalIdentityNeedsStacking(availableWidthDp: Float, fontScale: Float): Boolean =
    availableWidthDp < 900f || fontScale > 1.2f

@Composable
private fun PersonalIdentitySection(sheet:CharacterSheet,viewModel:SheetViewModel){
    // Os tres campos precisam de largura para o texto, rotulos e alvos de toque.
    // A regra anterior (390.dp) colocava tres OutlinedTextFields espremidos
    // em telas de celular e tablets compactos. O limite considera a area REAL
    // disponivel dentro do cartao, nao a largura fisica do aparelho.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        if (personalIdentityNeedsStacking(maxWidth.value, fontScale)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactAutoSizeField(sheet.nome, { viewModel.updateNome(it) }, BoxNames.PersonalData.NAME, Modifier.fillMaxWidth())
                CompactAutoSizeField(sheet.jogador, { viewModel.updateJogador(it) }, BoxNames.PersonalData.PLAYER, Modifier.fillMaxWidth())
                CompactAutoSizeField(sheet.conceito, { viewModel.updateConceito(it) }, BoxNames.PersonalData.CONCEPT, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactAutoSizeField(sheet.nome, { viewModel.updateNome(it) }, BoxNames.PersonalData.NAME, Modifier.weight(1f))
                CompactAutoSizeField(sheet.jogador, { viewModel.updateJogador(it) }, BoxNames.PersonalData.PLAYER, Modifier.weight(1f))
                CompactAutoSizeField(sheet.conceito, { viewModel.updateConceito(it) }, BoxNames.PersonalData.CONCEPT, Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(5.dp))
    OutlinedTextField(
        value = sheet.descricaoAnima,
        onValueChange = { viewModel.updateDescricaoAnima(it.take(500)) },
        label = { AppText(BoxNames.PersonalData.ANIMA_DESCRIPTION) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(textAlign = TextAlign.Justify),
        colors = exaltedTextFieldColors()
    )
}