package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.InkButtonSize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.NpcShareCodec
import com.example.model.Npc
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.InkButton
import com.example.ui.components.LongPressCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDangerCore
import com.example.ui.theme.ExaltedSuccess
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.viewmodel.SheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Ordem fixa das gavetas — independente da ordem de cadastro dos NPCs
// (ver pedido do usuário: "Aliados" sempre acima de "Inimigos", que
// sempre fica acima de "Neutros", não importa a ordem em que foram
// criados).
private val ORDEM_LEALDADES = listOf("Aliado", "Inimigo", "Neutro")
private fun rotuloGaveta(lealdade: String): String = when (lealdade) {
    "Aliado" -> "Aliados"
    "Inimigo" -> "Inimigos"
    else -> "Neutros"
}

private fun corPorLealdade(lealdade: String): androidx.compose.ui.graphics.Color = when (lealdade) {
    "Aliado" -> ExaltedSuccess
    "Inimigo" -> ExaltedDangerCore
    else -> androidx.compose.ui.graphics.Color.White
}

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NPCsTab(
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    val npcs by viewModel.npcs.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showCadastroDialog by remember { mutableStateOf(false) }
    var npcParaExcluir by remember { mutableStateOf<Npc?>(null) }

    var showShareMenu by remember { mutableStateOf(false) }
    var showExportResultDialog by remember { mutableStateOf<String?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var importLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(10).exaltedContentStage(10)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
        // O título da aba usa o mesmo SectionHeader das abas padronizadas;
        // o menu de compartilhamento permanece sobreposto à direita sem
        // alterar o formato, posicionamento ou hierarquia do título.
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                InkButton(onClick = { showShareMenu = true }) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = "Compartilhar NPCs", tint = ExaltedAccentBright)
                }
                DropdownMenu(expanded = showShareMenu, onDismissRequest = { showShareMenu = false }) {
                    DropdownMenuItem(
                        text = { AppText("Exportar Código…") },
                        onClick = {
                            showShareMenu = false
                            exportError = null
                            scope.launch {
                                val resultado = withContext(Dispatchers.Default) { viewModel.exportarNpcsComoCodigo() }
                                resultado.fold(
                                    onSuccess = { codigo -> showExportResultDialog = codigo },
                                    onFailure = { erro -> exportError = erro.message }
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { AppText("Importar Código…") },
                        onClick = {
                            showShareMenu = false
                            importText = ""
                            importError = null
                            showImportDialog = true
                        }
                    )
                }
            }
        }

        exportError?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            AppText(msg, color = ExaltedDangerCore, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Campo dinâmico: abre o popup de cadastro ao ser tocado.
        InkButton(
            label = "+ Cadastrar NPC",
            selected = true,
            onClick = { showCadastroDialog = true },
            modifier = Modifier.fillMaxWidth(),
            fillMaxWidth = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (npcs.isEmpty()) {
            AppText(
                text = "Nenhum NPC cadastrado ainda.",
                style = MaterialTheme.typography.bodyMedium,
                color = ExaltedMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        }

        // Gavetas em ordem fixa (Aliados > Inimigos > Neutros), cada uma
        // só aparece se tiver ao menos 1 NPC, ordenados alfabeticamente
        // por nome dentro da gaveta. Uma única passagem distribui os NPCs
        // nas três gavetas; a ordenação ocorre uma vez por gaveta.
        val npcsPorGaveta = remember(npcs) {
            val aliados = ArrayList<Npc>()
            val inimigos = ArrayList<Npc>()
            val neutros = ArrayList<Npc>()
            npcs.forEach { npc ->
                when (npc.lealdade) {
                    "Aliado" -> aliados += npc
                    "Inimigo" -> inimigos += npc
                    else -> neutros += npc
                }
            }
            mapOf(
                "Aliado" to aliados.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nome }),
                "Inimigo" to inimigos.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nome }),
                "Neutro" to neutros.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nome })
            )
        }
        ORDEM_LEALDADES.forEach { lealdade ->
            val itensDaGaveta = npcsPorGaveta[lealdade].orEmpty()
            if (itensDaGaveta.isNotEmpty()) {
                SectionHeader(title = rotuloGaveta(lealdade))
                Spacer(modifier = Modifier.height(6.dp))
                itensDaGaveta.forEach { npc ->
                    LongPressCard(
                        onLongClick = { npcParaExcluir = npc },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppText(npc.nome, color = ExaltedAccentBright, fontWeight = FontWeight.Bold)
                                if (npc.tipo.isNotBlank()) {
                                    AppText(npc.tipo, color = ExaltedMuted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (npc.descricao.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                com.example.ui.components.JustifiedBodyAppText(
                                    npc.descricao,
                                    color = ExaltedOnSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (showCadastroDialog) {
        var nome by remember { mutableStateOf("") }
        var lealdade by remember { mutableStateOf("Aliado") }
        var tipo by remember { mutableStateOf("") }
        var descricao by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCadastroDialog = false },
            modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape = com.example.ui.components.dialogShape,
            title = { AppText("Cadastrar NPC", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it.take(60) },
                        label = { AppText("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppText("Lealdade", style = MaterialTheme.typography.labelMedium, color = ExaltedAmber)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 2, modifier = Modifier.fillMaxWidth()) {
                            ORDEM_LEALDADES.forEach { opcao ->
                                val corLealdade = corPorLealdade(opcao)
                                InkButton(label = rotuloGaveta(opcao).dropLast(1), onClick = { lealdade = opcao }, modifier = Modifier.weight(1f), fillMaxWidth = true, selected = lealdade == opcao, size = InkButtonSize.Small)
                                }
                            }
                        }
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = { tipo = it.take(60) },
                        label = { AppText("Tipo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it.take(500) },
                        label = { AppText("Descrição") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(textAlign = TextAlign.Justify)
                    )
                }
            },
            dismissButton = {
                com.example.ui.components.GildedDialogButton(
                    text = "Cadastrar",
                    enabled = nome.isNotBlank(),
                    onClick = {
                        viewModel.addNpc(nome, lealdade, tipo, descricao)
                        showCadastroDialog = false
                    }
                )
            },
            confirmButton = {
                com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = { showCadastroDialog = false })
            },
            containerColor = ExaltedDarkSurface
        )
    }

    npcParaExcluir?.let { npc ->
        ConfirmDeleteDialog(
            itemTitle = npc.nome,
            onConfirm = {
                viewModel.removeNpc(npc.id)
                npcParaExcluir = null
            },
            onDismiss = { npcParaExcluir = null }
        )
    }

    showExportResultDialog?.let { codigo ->
        AlertDialog(
            onDismissRequest = { showExportResultDialog = null },
            modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape = com.example.ui.components.dialogShape,
            title = { AppText("Código de NPCs", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(
                        text = "Copie este código e envie para outro dispositivo. Em Importar Código, cole-o para adicionar estes NPCs à lista de lá.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        AppText(codigo, style = MaterialTheme.typography.bodySmall, color = ExaltedOnSurface)
                    }
                }
            },
            confirmButton = {
                com.example.ui.components.GildedDialogTextButton(text = "Fechar", onClick = { showExportResultDialog = null })
            },
            dismissButton = {
                com.example.ui.components.GildedDialogButton(
                    text = "Copiar",
                    onClick = {
                        clipboardManager.setText(AnnotatedString(codigo))
                    }
                )
            },
            containerColor = ExaltedDarkSurface
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { if (!importLoading) showImportDialog = false },
            modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape = com.example.ui.components.dialogShape,
            title = { AppText("Importar Código de NPCs", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it; importError = null },
                        label = { AppText("Código") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        isError = importError != null,
                        enabled = !importLoading
                    )
                    com.example.ui.components.GildedDialogTextButton(
                        text = "Colar",
                        onClick = { clipboardManager.getText()?.text?.let { importText = it; importError = null } }
                    )
                    importError?.let { erro -> AppText(erro, color = ExaltedDangerCore, style = MaterialTheme.typography.bodySmall) }
                    if (importLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ExaltedAccentBright, modifier = Modifier.height(28.dp))
                        }
                    }
                }
            },
            confirmButton = {
                com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = { if (!importLoading) showImportDialog = false })
            },
            dismissButton = {
                com.example.ui.components.GildedDialogButton(
                    text = "OK",
                    enabled = !importLoading,
                    onClick = {
                        importLoading = true
                        importError = null
                        val texto = importText
                        scope.launch {
                            val resultado = withContext(Dispatchers.Default) { NpcShareCodec.importar(texto) }
                            importLoading = false
                            when (resultado) {
                                is NpcShareCodec.ResultadoImportacao.Sucesso -> {
                                    viewModel.aplicarNpcsImportados(resultado.npcs)
                                    showImportDialog = false
                                }
                                is NpcShareCodec.ResultadoImportacao.Erro -> {
                                    importError = resultado.mensagem
                                }
                            }
                        }
                    }
                )
            },
            containerColor = ExaltedDarkSurface
        )
    }
}
