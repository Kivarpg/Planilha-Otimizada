package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.ui.components.AppText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSheetScreen(viewModel: SheetViewModel, tipoPersonagem: String = CharacterType.SOLAR) {
    remember(tipoPersonagem){viewModel.iniciarNovaPlanilha(tipoPersonagem);aplicarPaletaPorTemplate(tipoPersonagem)}
    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner){val observer=androidx.lifecycle.LifecycleEventObserver{_,event->when(event){androidx.lifecycle.Lifecycle.Event.ON_START->viewModel.onAppForegrounded();androidx.lifecycle.Lifecycle.Event.ON_STOP->viewModel.onAppBackgrounded();else->{}}};lifecycleOwner.lifecycle.addObserver(observer);onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}}
    val sheet by viewModel.sheetState.collectAsState()
    var selectedTabIndex by rememberSaveable{mutableStateOf(0)}; var abilitiesSubTabIndex by rememberSaveable{mutableStateOf(0)}
    var encontroNomeManual by rememberSaveable{mutableStateOf("")}; var encontroArquetipo by rememberSaveable{mutableStateOf(ArquetipoEncontro.FISICO)}; var encontroGenero by rememberSaveable{mutableStateOf<com.example.data.GeneroNome?>(com.example.data.GeneroNome.MASCULINO)}; var encontroAbaSelecionadaId by rememberSaveable{mutableStateOf<String?>(null)}; var encontroMensagemLimite by rememberSaveable{mutableStateOf<String?>(null)}
    var showSheetsListDialog by remember{mutableStateOf(false)}; var buscaPlanilhasSalvas by remember{mutableStateOf("")}; var showBackupListDialog by remember{mutableStateOf(false)}; var showErrorLogDialog by remember{mutableStateOf(false)}; var showSettingsDialog by remember{mutableStateOf(false)}; var showCarregarMenu by remember{mutableStateOf(false)}; var showOpcoesMenu by remember{mutableStateOf(false)}; var showNovoConfirmDialog by remember{mutableStateOf(false)}; var showEscolhaTemplateNovaPlanilha by remember{mutableStateOf(false)}; var showCodeImportDialog by remember{mutableStateOf(false)}; var showCodeExportResultDialog by remember{mutableStateOf<String?>(null)}; var sheetToDelete by remember{mutableStateOf<CharacterSheet?>(null)}
    val scope=rememberCoroutineScope(); val snackbarHostState=remember{SnackbarHostState()}; val clipboardManager=LocalClipboardManager.current
    val tabs=createSheetTabs(sheet,viewModel,abilitiesSubTabIndex,{abilitiesSubTabIndex=it},encontroNomeManual,{encontroNomeManual=it},encontroArquetipo,{encontroArquetipo=it},encontroGenero,{encontroGenero=it},encontroAbaSelecionadaId,{encontroAbaSelecionadaId=it},encontroMensagemLimite,{encontroMensagemLimite=it})
    val navigationKey = tabs.map { it.title to it.iconRes }
    val navigationTabs = remember(navigationKey) { tabs.navigationItems() }
        Scaffold(topBar={SheetTopBar(sheet = sheet,
                viewModel = viewModel,
                tabs = navigationTabs,
                selectedTabIndex = selectedTabIndex,
                onSelectedTab = { selectedTabIndex = it },
                showCarregarMenu = showCarregarMenu,
                onShowCarregarMenu = { showCarregarMenu = it },
                showOpcoesMenu = showOpcoesMenu,
                onShowOpcoesMenu = { showOpcoesMenu = it },
                showNovoConfirmDialog = showNovoConfirmDialog,
                onShowNovoConfirmDialog = { showNovoConfirmDialog = it },
                showBackupListDialog = showBackupListDialog,
                onShowBackupListDialog = { showBackupListDialog = it },
                showSheetsListDialog = showSheetsListDialog,
                onShowSheetsListDialog = { showSheetsListDialog = it },
                showCodeImportDialog = showCodeImportDialog,
                onShowCodeImportDialog = { showCodeImportDialog = it },
                showSettingsDialog = showSettingsDialog,
                onShowSettingsDialog = { showSettingsDialog = it },
                showCodeExportResultDialog = showCodeExportResultDialog,
                onShowCodeExportResultDialog = { showCodeExportResultDialog = it },
                buscaPlanilhasSalvas = buscaPlanilhasSalvas,
                onBuscaPlanilhasSalvas = { buscaPlanilhasSalvas = it },
                scope = scope,
                snackbarHostState = snackbarHostState
            )},snackbarHost={SnackbarHost(snackbarHostState)},containerColor=Color.Transparent){innerPadding->Box(Modifier.padding(innerPadding)){SheetContentArea(tabs,selectedTabIndex)}}
    // dialogs remain centralized below; state and callbacks are unchanged.
    SaveValidationDialog(viewModel = viewModel, scope = scope, snackbarHostState = snackbarHostState, onSaveSuccessful = { buscaPlanilhasSalvas = ""; showSheetsListDialog = true })


    // Configurações: preferência global de estilo de avaliação.
    SettingsDialog(
        show = showSettingsDialog,
        onDismiss = { showSettingsDialog = false },
        viewModel = viewModel,
        onAbrirLogErros = { showErrorLogDialog = true }
    )


    // Sheets List Dialog
    SheetsListDialog(
        show = showSheetsListDialog,
        onDismiss = { showSheetsListDialog = false },
        viewModel = viewModel,
        buscaPlanilhasSalvas = buscaPlanilhasSalvas,
        onBuscaChange = { buscaPlanilhasSalvas = it },
        onDeleteRequest = { s -> sheetToDelete = s },
        onNovaPlanilha = { showNovoConfirmDialog = true }
    )
    NovoConfirmDialog(
        show = showNovoConfirmDialog,
        onDismiss = { showNovoConfirmDialog = false },
        onProceed = { salvarAntes ->
            if (salvarAntes) viewModel.validateAndSaveSheet()
            showNovoConfirmDialog = false
            showEscolhaTemplateNovaPlanilha = true
        }
    )
    EscolhaTemplateNovaPlanilhaDialog(
        show = showEscolhaTemplateNovaPlanilha,
        onDismiss = { showEscolhaTemplateNovaPlanilha = false },
        onEscolhido = { tipoPersonagem ->
            viewModel.createNewSheet(tipoPersonagem)
            showEscolhaTemplateNovaPlanilha = false
        }
    )

    DeleteSheetDialog(sheetToDelete = sheetToDelete, onDismiss = { sheetToDelete = null }, viewModel = viewModel)


    // Reversão do Modo Experiência: exige confirmação explícita, já que
    // desmarcar "Planilha concluída" reverte todos os avanços comprados com XP.
    ReversionConfirmDialog(viewModel = viewModel)


    // Alerta genérico (comitamento insuficiente, Experiência insuficiente,
    // pendências para concluir a planilha etc.) — centralizado aqui para ficar
    // visível independentemente da aba em que o usuário estiver.
    CommitmentErrorDialog(viewModel = viewModel)
    PendingMeritBreakDialog(viewModel = viewModel)



    // --- Chooser "Carregar": Local ou Código ---

    // --- Importar por Código ---
    CodeImportDialog(show = showCodeImportDialog, onDismiss = { showCodeImportDialog = false }, viewModel = viewModel, scope = scope, clipboardManager = clipboardManager, snackbarHostState = snackbarHostState)

    // --- Resultado de "Exportar Código" ---
    CodeExportResultDialog(codigo = showCodeExportResultDialog, onDismiss = { showCodeExportResultDialog = null }, clipboardManager = clipboardManager, scope = scope, snackbarHostState = snackbarHostState)


    // --- Restaurar Backup periódico ---
    BackupListDialog(show = showBackupListDialog, onDismiss = { showBackupListDialog = false }, viewModel = viewModel, scope = scope, snackbarHostState = snackbarHostState)

    if (showErrorLogDialog) {
        val contexto = androidx.compose.ui.platform.LocalContext.current
        var conteudoLog by remember { mutableStateOf("Carregando log...") }
        LaunchedEffect(showErrorLogDialog) {
            if (showErrorLogDialog) {
                conteudoLog = withContext(Dispatchers.IO) {
                    try {
                        val arquivo = java.io.File(contexto.filesDir, "ultimo_erro.txt")
                        if (arquivo.exists()) arquivo.readText() else "Nenhum erro registrado ainda."
                    } catch (e: Exception) {
                        "Não foi possível ler o log: ${e.message}"
                    }
                }
            }
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showErrorLogDialog = false },
            modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape = com.example.ui.components.dialogShape,
            title = { AppText("Log de Erros", color = ExaltedAccentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
            text = {
                AppText(
                    conteudoLog,
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedOnSurface,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                )
            },
            dismissButton = {
                com.example.ui.components.GildedDialogButton(
                    text = "Copiar",
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(conteudoLog))
                        scope.launch { snackbarHostState.showSnackbar("Log copiado.") }
                    }
                )
            },
            confirmButton = {
                com.example.ui.components.GildedDialogTextButton(text = "Fechar", onClick = { showErrorLogDialog = false })
            },
            containerColor = ExaltedDarkSurface
        )
    }

}
