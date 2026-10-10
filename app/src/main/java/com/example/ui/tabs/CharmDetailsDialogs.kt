package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.example.ui.components.AppText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.EncantoSolarDefinition
import com.example.model.Encanto
import com.example.feature.charmtree.paraArvoreDePreRequisitos
import com.example.ui.components.EncantoQuadroBox
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.viewmodel.SheetViewModel

@Composable
fun CatalogDetailsDialog(def: EncantoSolarDefinition, onDismiss: () -> Unit, viewModel: SheetViewModel? = null, charmsParaArvore: List<Encanto>? = null, titleColor: androidx.compose.ui.graphics.Color = com.example.ui.theme.ExaltedAccentBright) {
    val detailsHeightLimit = (LocalConfiguration.current.screenHeightDp - 240).coerceIn(160, 500).dp
    var mostrarArvore by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (mostrarArvore && viewModel != null) {
        val charmsDaArvore = remember(def.id, def.preRequisitos, charmsParaArvore) {
            if (def.id.contains("::")) {
                viewModel.encantosDaArteMarcialParaArvore(def.id.substringBefore("::"))
            } else {
                charmsParaArvore ?: viewModel.todosOsEncantosParaArvore()
            }
        }
        val rootEntry = remember(def.id, def.nome, def.preRequisitos, charmsDaArvore) {
            charmsDaArvore.paraArvoreDePreRequisitos()
                .firstOrNull { it.id == def.id || it.name.equals(def.nome, ignoreCase = true) }
        }
        val possuiEncantoPrerequisito = !rootEntry?.prerequisiteIds.isNullOrEmpty()
        if (possuiEncantoPrerequisito) {
            com.example.feature.charmtree.CharmPrerequisiteTreeDialog(
                // Use o ID da entrada do MESMO catálogo enviado à árvore.
                // O Encanto exibido no diálogo pode ter sido materializado
                // em outra instância e, portanto, possuir um ID diferente.
                rootCharmId = rootEntry?.id ?: def.id,
                charms = charmsDaArvore,
                onDismiss = { mostrarArvore = false }
            )
        } else {
            CharmListTreeDialog(
                title = def.habilidade,
                charms = charmsDaArvore.filter {
                    it.habilidadeVinculada.equals(def.habilidade, ignoreCase = true)
                },
                onDismiss = { mostrarArvore = false }
            )
        }
    } else {
        AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { com.example.ui.components.ChamTitleTwoLines(nomePt = def.nome, nomeEn = def.nomeIngles, titleColor = titleColor) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = detailsHeightLimit).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CampoNegrito("Custo", def.custo)
                CampoNegrito("Mins", def.minsTexto.ifBlank { "${def.habilidade} ${def.minHabilidade}, Essência ${def.minEssencia}" })
                CampoNegrito("Tipo", def.tipo)
                CampoNegrito("Palavras-chave", def.palavrasChave)
                CampoNegrito("Duração", def.duracao)
                CampoNegrito("Pré-requisitos", def.preRequisitos)
                Spacer(Modifier.height(6.dp))
                com.example.ui.components.JustifiedBodyAppText(def.descricao)
                def.quadros.forEach { quadro ->
                    Spacer(Modifier.height(8.dp))
                    EncantoQuadroBox(quadro)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel != null) {
                    com.example.ui.components.GildedDialogButton(
                        text = "Árvore",
                        onClick = { mostrarArvore = true },
                        modifier = Modifier.weight(1f),
                        fillMaxWidth = true
                    )
                }
                com.example.ui.components.GildedDialogButton(
                    text = "Fechar",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = true
                )
            }
        },
        containerColor = ExaltedDarkSurfaceVariant
        )
    }
}


@Composable
fun CharmDetailsDialog(
    charm: Encanto,
    onDismiss: () -> Unit,
    viewModel: SheetViewModel? = null,
    dragonBlooded: Boolean = false,
    tipoPersonagem: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = com.example.ui.theme.ExaltedAccentBright
) {
    val detailsHeightLimit = (LocalConfiguration.current.screenHeightDp - 240).coerceIn(160, 500).dp
    var mostrarArvore by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (mostrarArvore && viewModel != null) {
        val charmsDaArvore = remember(charm.id, charm.preRequisitos, tipoPersonagem, dragonBlooded) {
            tipoPersonagem?.let(viewModel::todosOsEncantosParaArvore)
                ?: viewModel.todosOsEncantosParaArvore(dragonBlooded)
        }
        val rootEntry = remember(charm.id, charm.nome, charm.preRequisitos, charmsDaArvore) {
            charmsDaArvore.paraArvoreDePreRequisitos()
                .firstOrNull { it.id == charm.id || it.name.equals(charm.nome, ignoreCase = true) }
        }
        val possuiEncantoPrerequisito = !rootEntry?.prerequisiteIds.isNullOrEmpty()
        if (possuiEncantoPrerequisito) {
            com.example.feature.charmtree.CharmPrerequisiteTreeDialog(
                // Use o ID da entrada do MESMO catálogo enviado à árvore.
                // O Encanto exibido no diálogo pode ter sido materializado
                // em outra instância e, portanto, possuir um ID diferente.
                rootCharmId = rootEntry?.id ?: charm.id,
                charms = charmsDaArvore,
                onDismiss = { mostrarArvore = false }
            )
        } else {
            CharmListTreeDialog(
                title = charm.habilidadeVinculada,
                charms = charmsDaArvore.filter {
                    it.habilidadeVinculada.equals(charm.habilidadeVinculada, ignoreCase = true)
                },
                onDismiss = { mostrarArvore = false }
            )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { com.example.ui.components.ChamTitleTwoLines(nomePt = charm.nome, nomeEn = charm.nomeIngles, titleColor = titleColor) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = detailsHeightLimit)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CampoNegrito("Custo", charm.custo)
                if (charm.circulo.isNotBlank()) {
                    CampoNegrito("Círculo", charm.circulo)
                } else {
                    CampoNegrito("Mins", charm.mins.ifBlank { "${charm.habilidadeVinculada} ${charm.minHabilidade}, Essência ${charm.minEssencia}" })
                }
                CampoNegrito("Tipo", charm.tipo)
                CampoNegrito("Palavras-chave", charm.palavrasChave)
                CampoNegrito("Duração", charm.duracao)
                CampoNegrito("Pré-requisitos", charm.preRequisitos)
                Spacer(Modifier.height(6.dp))
                com.example.ui.components.JustifiedBodyAppText(charm.descricao)
                charm.quadros.forEach { quadro ->
                    Spacer(Modifier.height(8.dp))
                    EncantoQuadroBox(quadro)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel != null) {
                    com.example.ui.components.GildedDialogButton(
                        text = "Árvore",
                        onClick = { mostrarArvore = true },
                        modifier = Modifier.weight(1f),
                        fillMaxWidth = true
                    )
                }
                com.example.ui.components.GildedDialogButton(
                    text = "Fechar",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = true
                )
            }
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

/** Campo "Rótulo: valor" com o rótulo em negrito — confirmado contra os
 * documentos de referência do usuário (docx dos Encantos), onde todo
 * rótulo de campo (Custo, Mins, Tipo, etc.) vem em negrito, seguido do
 * valor em peso normal. Antes, essa formatação tinha sumido — os campos
 * eram todos AppText() simples, sem negrito nenhum. */
@Composable
internal fun CampoNegrito(rotulo: String, valor: String) {
    AppText(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$rotulo: ") }
            append(valor)
        }
    )
}