package com.example.ui.tabs
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.CharacterSheet
import com.example.model.Encanto
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
internal fun MartialArtsCharmsPopup(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit,
    onShowDetail: (com.example.data.EncantoSolarDefinition) -> Unit
) {
    // A Aba 8 exibe o catálogo global de estilos. A qualificação para
    // aquisição é avaliada somente no segundo popup, encanto por encanto.
    val estilos = remember(viewModel) {
        viewModel.estilosArtesMarciaisDisponiveis()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nomePt })
    }
    var estiloSelecionado by remember { mutableStateOf<com.example.data.EstiloArteMarcialDefinition?>(null) }
    var estiloArvore by remember { mutableStateOf<com.example.data.EstiloArteMarcialDefinition?>(null) }

    if (estiloArvore == null) {
        AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText("Artes Marciais", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (estilos.isEmpty()) {
                    AppText(
                        "Nenhum estilo de Arte Marcial cadastrado no catálogo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExaltedMuted,
                        textAlign = TextAlign.Center
                    )
                }
                estilos.forEach { estilo ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(estilo.id) {
                                detectTapGestures(
                                    onTap = { estiloSelecionado = estilo },
                                    onLongPress = { estiloArvore = estilo }
                                )
                            }
                            .feedbackOnPress()
                            .padding(vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppText(estilo.nomePt, color = ExaltedAccentBright, fontWeight = FontWeight.Bold)
                        if (!estilo.armaduraTexto.isNullOrBlank()) AppText("Armadura: ${estilo.armaduraTexto}", style = MaterialTheme.typography.labelSmall, color = ExaltedMuted)
                        CharmAbilityButton(
                            text = "Árvore",
                            selected = false,
                            enabled = estilo.encantos.isNotEmpty(),
                            onClick = { estiloArvore = estilo },
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { GildedDialogButton(text = "Fechar", onClick = onDismiss) },
        containerColor = ExaltedDarkSurfaceVariant
        )
    }

    // Ao selecionar um estilo, abre um popup dedicado com todos os Encantos
    // daquele estilo — mesmo comportamento (mesma checkbox de seleção) já
    // usado para os Encantos de Habilidades comuns (ver HabilidadeCharmsPopup).
    estiloSelecionado?.let { estilo ->
        EstiloArteMarcialCharmsPopup(
            estilo = estilo,
            sheet = sheet,
            viewModel = viewModel,
            onDismiss = { estiloSelecionado = null },
            onShowDetail = onShowDetail
        )
    }

    estiloArvore?.let { estilo ->
        val charmsDaArvore = remember(estilo.id) { estilo.encantos.map { it.toEncanto() } }
        CharmListTreeDialog(
            title = "Árvore — ${estilo.nomePt}",
            charms = charmsDaArvore,
            onDismiss = { estiloArvore = null }
        )
    }
}

@Composable
internal fun EstiloArteMarcialCharmsPopup(
    estilo: com.example.data.EstiloArteMarcialDefinition,
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit,
    onShowDetail: (com.example.data.EncantoSolarDefinition) -> Unit
) {
    val itens = remember(sheet, estilo.id) { viewModel.encantosDaArteMarcialComElegibilidade(estilo.id, sheet) }
    val adquiridoPorNome = remember(sheet.charms) {
        buildMap<String, Encanto> {
            sheet.charms.forEach { putIfAbsent(com.example.data.EncantosSolaresCatalog.normalize(it.nome), it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText(estilo.nomePt, color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            if (itens.isEmpty()) {
                AppText(
                    "Nenhum Encanto cadastrado para este estilo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itens.forEach { item ->
                        CharmEligibilityRow(
                            item = item,
                            onToggle = { marcado ->
                                if (marcado) viewModel.addCharmFromDefinition(item.definicao)
                                else adquiridoPorNome[com.example.data.EncantosSolaresCatalog.normalize(item.definicao.nome)]?.let { viewModel.removeCharm(it.id) }
                            },
                            onShowDetail = { onShowDetail(item.definicao) }
                        )
                    }
                }
            }
        },
        confirmButton = { GildedDialogButton(text = "Fechar", onClick = onDismiss) },
        containerColor = ExaltedDarkSurfaceVariant
    )
}