package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage

import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import com.example.model.isDragonBlooded
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterSheet
import com.example.model.Merito
import com.example.ui.components.InkButton
import com.example.ui.components.LongPressCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.GildedCard
import com.example.ui.components.GildedDialogButton
import com.example.ui.components.GildedDialogTextButton
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.viewmodel.SheetViewModel

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
fun MeritsTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    var meritMenu by remember { mutableStateOf<Merito?>(null) }
    var meritToEdit by remember { mutableStateOf<Merito?>(null) }
    val ratingStyle by viewModel.ratingStyle.collectAsState()

    val totalMeritPoints = sheet.merits.sumOf { it.valor }
    val orcamentoMeritos = if (sheet.tipoPersonagem.isDragonBlooded()) 18 else 10
    val freeMeritsRemaining = (orcamentoMeritos - totalMeritPoints).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(6).exaltedContentStage(6)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Surface(
            color = ExaltedDarkSurfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = "Total de Pontos de Mérito: $totalMeritPoints / $orcamentoMeritos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExaltedAmber
                )
                if (totalMeritPoints > orcamentoMeritos) {
                    val excess = totalMeritPoints - orcamentoMeritos
                    AppText(
                        text = "Excedente: $excess ponto(s) = $excess Pontos de Bônus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    AppText(
                        text = "Pontos Restantes: $freeMeritsRemaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExaltedGold
                    )
                }
            }
        }

        if (sheet.tipoPersonagem.isDragonBlooded()) {
            AppText(
                text = "Além disso: 5 pontos de Méritos Adicionais para distribuir entre Apoio, " +
                    "Comando, Contatos, Seguidores, Influência, Idioma, Recursos e Vassalos.",
                style = MaterialTheme.typography.bodySmall,
                color = ExaltedMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        var showSelecaoDialog by remember { mutableStateOf(false) }

        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(6),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InkButton(
                    label = "Cadastrar Mérito",
                    selected = true,
                    onClick = { showSelecaoDialog = true }
                )
            }
        }

        if (showSelecaoDialog) {
            MeritSelectionDialog(
                viewModel = viewModel,
                sheet = sheet,
                ratingStyle = ratingStyle,
                onDismiss = { showSelecaoDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        if (sheet.merits.isNotEmpty()) {
            sheet.merits.forEach { m ->
                val cardContent: @Composable () -> Unit = {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        val compactMeritCard = maxWidth < 360.dp
                        val meritIdentity: @Composable () -> Unit = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                com.example.ui.components.MedallionIcon(size = 32.dp) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = ExaltedAccentBright,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    com.example.ui.components.AutoSizeText(
                                        text = m.nome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                                        minFontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (m.detalhe.isNotBlank() && m.origemAutomatica != "Idioma") {
                                        AppText(
                                            text = "– ${m.detalhe}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = ExaltedMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (m.origemAutomatica == "Idioma" && m.detalhe.isNotBlank()) {
                                        AppText(
                                            text = "(${m.detalhe})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = ExaltedMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (compactMeritCard) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                meritIdentity()
                                com.example.ui.components.RatingDisplay(value = m.valor, maxValue = 5, style = ratingStyle)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) { meritIdentity() }
                                com.example.ui.components.RatingDisplay(value = m.valor, maxValue = 5, style = ratingStyle)
                            }
                        }
                    }
                }
                if (m.origemAutomatica == "Idioma") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = ExaltedDarkSurfaceVariant
                    ) { cardContent() }
                } else {
                    LongPressCard(
                        onLongClick = { meritMenu = m },
                        modifier = Modifier.fillMaxWidth()
                    ) { cardContent() }
                }
            }
        }
    }

    meritMenu?.let { m ->
        AlertDialog(
            onDismissRequest = { meritMenu = null },
            modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape = com.example.ui.components.dialogShape,
            title = {
                AppText(
                    m.nome,
                    color = ExaltedAmber,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    forceStroke = true
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GildedDialogButton(text = "Editar", onClick = { meritToEdit = m; meritMenu = null })
                    GildedDialogButton(text = "Remover", onClick = { meritToEdit = null; meritMenu = null; viewModel.removeMerit(m.id) })
                    GildedDialogTextButton(text = "Cancelar", onClick = { meritMenu = null })
                }
            },
            confirmButton = {},
            containerColor = ExaltedDarkSurfaceVariant
        )
    }

    meritToEdit?.let { m ->
        var detalhe by remember(m.id) { mutableStateOf(m.detalhe) }
        AlertDialog(
            onDismissRequest = { meritToEdit = null },
            modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape = com.example.ui.components.dialogShape,
            title = {
                AppText(
                    "Editar Mérito",
                    color = ExaltedAmber,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    forceStroke = true
                )
            },
            text = {
                OutlinedTextField(
                    value = detalhe,
                    onValueChange = { detalhe = it.take(80) },
                    label = { AppText("Detalhe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = com.example.ui.components.exaltedTextFieldColors()
                )
            },
            dismissButton = {
                GildedDialogButton(
                    text = "Salvar",
                    onClick = { viewModel.updateMeritDetalhe(m.id, detalhe); meritToEdit = null }
                )
            },
            confirmButton = {
                GildedDialogTextButton(text = "Cancelar", onClick = { meritToEdit = null })
            },
            containerColor = ExaltedDarkSurfaceVariant
        )
    }
}

// Popup de cadastro por catálogo: passo 1 mostra as duas gavetas (normais
// e sobrenaturais) SEMPRE abertas — nunca precisam ser expandidas
// manualmente, conforme pedido. Tocar num nome avança pro passo 2, com
// descrição, pré-requisitos (checados automaticamente quando seguem o
// padrão simples "Atributo/Habilidade ••", texto original sempre visível)
// e o custo restrito às opções permitidas por aquele mérito específico.
