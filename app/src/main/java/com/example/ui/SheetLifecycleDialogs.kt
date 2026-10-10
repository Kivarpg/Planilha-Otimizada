package com.example.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.LongPressCard
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.viewmodel.SheetViewModel
import kotlinx.coroutines.launch


@Composable
fun DeleteSheetDialog(sheetToDelete: com.example.model.CharacterSheet?, onDismiss: () -> Unit, viewModel: SheetViewModel, modifier: Modifier = Modifier) {
    sheetToDelete?.let { s ->
        ConfirmDeleteDialog(
            itemTitle = s.nome,
            onConfirm = {
                viewModel.deleteSheet(s.id)
                onDismiss()
            },
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}


@Composable
fun SheetsListDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: SheetViewModel,
    buscaPlanilhasSalvas: String,
    onBuscaChange: (String) -> Unit,
    onDeleteRequest: (com.example.model.CharacterSheet) -> Unit,
    onNovaPlanilha: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return
    val sheet by viewModel.sheetState.collectAsState()
    val savedSheets by viewModel.savedSheets.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText(
                forceStroke = true,
                text = "Planilhas de Personagem Salvas",
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = "Selecione uma planilha para carregar ou mantenha pressionado para excluir:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (savedSheets.size > 4) {
                    OutlinedTextField(
                        value = buscaPlanilhasSalvas,
                        onValueChange = onBuscaChange,
                        label = { AppText("Buscar por nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val termoBusca = buscaPlanilhasSalvas.trim()
                val planilhasFiltradas = if (termoBusca.isEmpty()) {
                    savedSheets
                } else {
                    savedSheets.filter {
                        it.nome.contains(termoBusca, ignoreCase = true) ||
                            it.nomeArquivo().contains(termoBusca, ignoreCase = true)
                    }
                }
                if (planilhasFiltradas.isEmpty() && termoBusca.isNotEmpty()) {
                    AppText(
                        text = "Nenhuma planilha encontrada para \"$termoBusca\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExaltedMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                planilhasFiltradas.forEach { s ->
                    val isCurrent = s.id == sheet.id
                    LongPressCard(
                        onLongClick = {
                            if (savedSheets.size > 1) {
                                onDeleteRequest(s)
                            }
                        },
                        onClick = {
                            viewModel.loadSheet(s)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                AppText(
                                    text = s.nomeArquivo(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) ExaltedAmber else MaterialTheme.colorScheme.onSurface
                                )
                                AppText(
                                    text = "Casta: ${s.casta.displayName} | Jogador: ${s.jogador.ifBlank { "-" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExaltedMuted
                                )
                            }
                            if (isCurrent) {
                                Surface(
                                    color = ExaltedAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    AppText(
                                        text = "Ativa",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ExaltedAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(
                text = "Nova Planilha",
                onClick = {
                    onDismiss()
                    onNovaPlanilha()
                }
            )
        },
        confirmButton = {
            com.example.ui.components.GildedDialogTextButton(text = "Fechar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurface
    )
}


@Composable
fun BackupListDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: SheetViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    modifier: Modifier = Modifier
) {
    if (!show) return
    val backups = remember(show, viewModel) { viewModel.listarBackupsPeriodicos() }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText("Restaurar Backup", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(
                    text = "Cópias automáticas salvas a cada 5 minutos, independentes do salvamento manual. Restaurar substitui a planilha atual na tela — a planilha em si só é sobrescrita de vez se você apertar Salvar depois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedMuted
                )
                if (backups.isEmpty()) {
                    AppText(
                        text = "Nenhum backup automático ainda. Eles aparecem aqui depois de alguns minutos de uso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExaltedMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                backups.forEach { snapshot ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppText(snapshot.timestamp, color = MaterialTheme.colorScheme.onSurface)
                        com.example.ui.components.GildedDialogTextButton(
                            text = "Restaurar",
                            onClick = {
                                val sucesso = viewModel.restaurarBackupPeriodico(snapshot)
                                onDismiss()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (sucesso) "Backup de ${snapshot.timestamp} restaurado." else "Não foi possível restaurar esse backup."
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(text = "Fechar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurface
    )
}

// Ao clicar em "Novo", pergunta se o usuário quer salvar a planilha atual
// antes de apagar tudo da tela — evita perda acidental de dados não
// salvos (mesmo cuidado já aplicado em "Restaurar Backup"/"Carregar").

@Composable
fun NovoConfirmDialog(show: Boolean, onDismiss: () -> Unit, onProceed: (salvarAntes: Boolean) -> Unit, modifier: Modifier = Modifier) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText(
                forceStroke = true,
                text = "Criar Nova Planilha",
                color = ExaltedAccentBright,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            AppText(
                text = "Deseja salvar a planilha atual antes de criar uma nova? Alterações não salvas serão perdidas.",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            // Empilhados na vertical: 3 botões lado a lado (216dp cada,
            // tamanho mínimo de InkButtonSize.Small) não cabem na largura
            // do AlertDialog na maioria das telas — "Cancelar" ficava
            // cortado. Empilhar garante que os 3 fiquem sempre visíveis,
            // independente da largura da tela.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.ui.components.GildedDialogButton(
                    text = "Salvar",
                    onClick = { onProceed(true) },
                    modifier = Modifier.fillMaxWidth(),
                    fillMaxWidth = true
                )
                com.example.ui.components.GildedDialogTextButton(
                    text = "Não Salvar",
                    onClick = { onProceed(false) },
                    modifier = Modifier.fillMaxWidth(),
                    fillMaxWidth = true
                )
                com.example.ui.components.GildedDialogTextButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    fillMaxWidth = true
                )
            }
        },
        containerColor = ExaltedDarkSurface
    )
}

// Segunda etapa do fluxo de "Novo" — pedido explícito do usuário: a planilha
// nova deve perguntar Solar ou Sangue de Dragão antes de ser criada, com
// a interface (cor, Aspecto x Casta, etc.) mudando de acordo. Diálogo
// separado do de confirmação acima (que só decide se salva a planilha atual
// antes) — cada um cuida de uma decisão.

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EscolhaTemplateNovaPlanilhaDialog(show: Boolean, onDismiss: () -> Unit, onEscolhido: (tipoPersonagem: String) -> Unit, modifier: Modifier = Modifier) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText(
                forceStroke = true,
                text = "Nova Planilha",
                color = ExaltedAccentBright,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            AppText(
                text = "Qual o tipo de Exaltado da nova planilha?",
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Regra global de grupos de três: lado a lado quando couber;
                // em largura compacta, 2 em cima e o terceiro centralizado.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 420.dp
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = if (compact) 2 else 3,
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Solar" to "Solar",
                            "Sangue de Dragão" to "SangueDeDragao",
                            "Lunar" to "Lunar"
                        ).forEach { (rotulo, valor) ->
                            com.example.ui.components.GildedDialogButton(
                                text = rotulo,
                                onClick = { onEscolhido(valor) },
                                modifier = Modifier.fillMaxWidth(if (compact) 0.49f else 0.32f),
                                fillMaxWidth = true
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd))
                }
            }
        },
        containerColor = ExaltedDarkSurface
    )
}
