package com.example.ui
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.example.R
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetTopBar(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    tabs: List<SheetTabNavigationItem>,
    selectedTabIndex: Int,
    onSelectedTab: (Int) -> Unit,
    showCarregarMenu: Boolean,
    onShowCarregarMenu: (Boolean) -> Unit,
    showOpcoesMenu: Boolean,
    onShowOpcoesMenu: (Boolean) -> Unit,
    showNovoConfirmDialog: Boolean,
    onShowNovoConfirmDialog: (Boolean) -> Unit,
    showBackupListDialog: Boolean,
    onShowBackupListDialog: (Boolean) -> Unit,
    showSheetsListDialog: Boolean,
    onShowSheetsListDialog: (Boolean) -> Unit,
    showCodeImportDialog: Boolean,
    onShowCodeImportDialog: (Boolean) -> Unit,
    showSettingsDialog: Boolean,
    onShowSettingsDialog: (Boolean) -> Unit,
    showCodeExportResultDialog: String?,
    onShowCodeExportResultDialog: (String?) -> Unit,
    buscaPlanilhasSalvas: String,
    onBuscaPlanilhasSalvas: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Passo 9/12 — composição adaptativa sem criar um fluxo Compact paralelo.
    // O mesmo cabeçalho preserva conteúdo e ações; apenas redistribui espaço.
    val visual = LocalExaltedVisualTemplate.current

    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // IDENTIDADE VISUAL v4 — composição editorial agressiva. O topo agora é
    // uma peça de identidade + índice de capítulo; não é uma barra de app.
    // Estados, ações, tabs e callbacks permanecem inalterados.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val compactHeader = maxWidth < 600.dp
    val identityWidth = if (compactHeader) 68.dp else 84.dp
    // Dois controles de 48.dp exigem ao menos 96.dp, mesmo no celular.
    val identityIconSize = if (compactHeader) 36.dp else 42.dp
    // Usa a mesma arte do Scaffold; somente a faixa de leitura e local.
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        visual.surface.copy(alpha = .79f),
                        visual.surfaceVariant.copy(alpha = .73f),
                        visual.surface.copy(alpha = .79f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = visual.divider.copy(alpha = .72f),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        val identidadeIcon = when {
            sheet.tipoPersonagem.isDragonBlooded() -> R.drawable.tab_icon_dragao
            sheet.tipoPersonagem.isLunar() -> R.drawable.tab_icon_lua
            else -> R.drawable.tab_icon_sol
        }
        // Ações em linha independente acima do nome para todos os tipos de Exaltado.
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InkButton(onClick = { onShowSettingsDialog(true) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Settings, "Configurações", tint = visual.accentBright, modifier = Modifier.size(20.dp))
                    }
                    Box {
                        InkButton(onClick = { onShowOpcoesMenu(true) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.MoreVert, "Opções", tint = visual.accentBright, modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(expanded = showOpcoesMenu, onDismissRequest = { onShowOpcoesMenu(false) }) {
                            DropdownMenuItem(text = { AppText("Novo") }, onClick = { onShowOpcoesMenu(false); onShowNovoConfirmDialog(true) },
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                            DropdownMenuItem(text = { AppText("Salvar") }, onClick = { onShowOpcoesMenu(false); viewModel.validateAndSaveSheet() },
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                            DropdownMenuItem(text = { AppText("Carregar") }, onClick = { onShowOpcoesMenu(false); onShowCarregarMenu(true) },
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                            DropdownMenuItem(text = { AppText("Exportar") }, onClick = { onShowOpcoesMenu(false); scope.launch { val resultado = withContext(Dispatchers.Default) { viewModel.exportarComoCodigo() }; resultado.fold({ codigo -> onShowCodeExportResultDialog(codigo) }, { erro -> snackbarHostState.showSnackbar(erro.message ?: "Não foi possível gerar o código.") }) } },
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                            DropdownMenuItem(text = { AppText("Reverter Conclusão…") }, enabled = sheet.planilhaConcluida, onClick = { onShowOpcoesMenu(false); viewModel.solicitarDesmarcarPlanilhaConcluida() },
    modifier = Modifier.feedbackOnPress(enabled = sheet.planilhaConcluida)
)
                            DropdownMenuItem(text = { AppText("Restaurar Backup…") }, onClick = { onShowOpcoesMenu(false); onShowBackupListDialog(true) },
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                        }
                    }
                }
            }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .padding(start = if (compactHeader) 14.dp else 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(identityWidth)
                    // Nunca preencher a altura do viewport: o Row do cabeçalho
                    // é medido com altura flexível pelo Scaffold. fillMaxHeight()
                    // aqui fazia o emblema ocupar quase toda a tela e escondia
                    // a barra de abas e os campos da ficha.
                    .height(104.dp)
                    .padding(vertical = 10.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                visual.surfaceVariant,
                                visual.surface,
                                visual.surfaceVariant
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.25.dp,
                        color = visual.accentBright.copy(alpha = .72f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                TabIconDisplay(
                    iconRes = identidadeIcon,
                    tint = visual.accentBright,
                    size = identityIconSize,
                    isDragonBlooded = sheet.tipoPersonagem.isDragonBlooded(),
                    isLunar = sheet.tipoPersonagem.isLunar()
                )
            }
            Spacer(Modifier.width(if (compactHeader) 8.dp else 14.dp))
            Column(
                Modifier.weight(1f).padding(top = if (compactHeader) 34.dp else 28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                AppText(
                    sheet.nome.ifBlank { "Nome" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-.35).sp
                    ),
                    color = visual.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                // O estado da planilha nao deve disputar uma unica linha com
                // nomes longos de Casta/Aspecto em celulares.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppText(
                        when {
                            sheet.tipoPersonagem.isDragonBlooded() -> sheet.aspecto.ifBlank { "Aspecto" }
                            sheet.tipoPersonagem.isLunar() -> sheet.lunarCasta.displayName
                            else -> sheet.casta.displayName
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                        color = visual.accentBright,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                    val completa = viewModel.isSheetComplete(sheet)
                    val cor = if (completa) ExaltedStatusCompleto else ExaltedStatusIncompleto
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(cor))
                        Spacer(Modifier.width(6.dp))
                        AppText(
                            if (completa) "Planilha completa e válida" else "Planilha em criação",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = cor, maxLines = 2, softWrap = true, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        // Índice de capítulos: somente tipografia, ícone mínimo e linha ativa.
        // A navegação deixa de parecer uma fileira de botões.
        SheetTabsBar(sheet, viewModel, tabs, selectedTabIndex, onSelectedTab)
    }
    }

    if(showCarregarMenu){
        AlertDialog(
            onDismissRequest={onShowCarregarMenu(false)},
            modifier=Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape=com.example.ui.components.dialogShape,
            title={AppText("Carregar",color=visual.accentBright,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth(), forceStroke = true)},
            text={Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                DropdownMenuItem(text={AppText("Local")},onClick={onShowCarregarMenu(false);onBuscaPlanilhasSalvas("");onShowSheetsListDialog(true)},
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                DropdownMenuItem(text={AppText("Por código")},onClick={onShowCarregarMenu(false);onShowCodeImportDialog(true)},
    modifier = Modifier.feedbackOnPress(enabled = true)
)
            }},
            dismissButton={},
            confirmButton={com.example.ui.components.GildedDialogTextButton(text="Cancelar",onClick={onShowCarregarMenu(false)})},
            containerColor=visual.surface,
            titleContentColor=visual.onSurface,
            textContentColor=visual.onSurface
        )
    }

}
