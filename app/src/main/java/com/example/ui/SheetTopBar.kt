package com.example.ui
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
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
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale
    val compactHeader = screenWidthDp < 600
    val enlargedHeaderText = fontScale > 1.20f
    val headerHeight = if (enlargedHeaderText) 144.dp else 132.dp
    val identityWidth = if (compactHeader) 68.dp else 84.dp
    val actionsWidth = if (compactHeader) 80.dp else 88.dp
    val identityIconSize = if (compactHeader) 36.dp else 42.dp

    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // IDENTIDADE VISUAL v4 — composição editorial agressiva. O topo agora é
    // uma peça de identidade + índice de capítulo; não é uma barra de app.
    // Estados, ações, tabs e callbacks permanecem inalterados.
    Column(
        modifier
            .fillMaxWidth()
            .background(ExaltedDarkBackground.copy(alpha = .985f))
            .drawBehind {
                val accent = ExaltedAccentBright
                val deep = ExaltedStructuralMetalDeep
                // Grande eixo vertical de identidade.
                drawLine(accent.copy(alpha = .75f), Offset(7.dp.toPx(), 0f), Offset(7.dp.toPx(), size.height), 3.dp.toPx())
                drawLine(deep.copy(alpha = .65f), Offset(12.dp.toPx(), 0f), Offset(12.dp.toPx(), size.height), 1.dp.toPx())
                // Linha de horizonte interrompida.
                drawLine(accent.copy(alpha = .16f), Offset(22.dp.toPx(), size.height - 1.dp.toPx()), Offset(size.width * .48f, size.height - 1.dp.toPx()), 1.dp.toPx())
                drawLine(accent.copy(alpha = .38f), Offset(size.width * .62f, size.height - 1.dp.toPx()), Offset(size.width - 12.dp.toPx(), size.height - 1.dp.toPx()), 1.dp.toPx())
                // Moldura superior e cantos escalonados, alinhados à referência.
                drawLine(accent.copy(alpha=.62f), Offset(7.dp.toPx(), 5.dp.toPx()), Offset(size.width-7.dp.toPx(), 5.dp.toPx()), 1.dp.toPx())
                drawLine(deep.copy(alpha=.52f), Offset(13.dp.toPx(), 10.dp.toPx()), Offset(size.width-13.dp.toPx(), 10.dp.toPx()), .7.dp.toPx())
                val c=20.dp.toPx(); val x=7.dp.toPx(); val y=5.dp.toPx()
                drawLine(accent.copy(.88f),Offset(x,y),Offset(x+c,y),1.6.dp.toPx())
                drawLine(accent.copy(.88f),Offset(size.width-x,y),Offset(size.width-x-c,y),1.6.dp.toPx())

                // Passo 3/12 — decomposição LEO: o cabeçalho recebe uma assinatura
                // estrutural própria por Tipo, sem adicionar conteúdo ou controles.
                val center = size.width * .5f
                when (ExaltedActiveMotif) {
                    ExaltedVisualMotif.SOLAR -> {
                        val r = 16.dp.toPx()
                        drawArc(accent.copy(.46f), 200f, 140f, false, Offset(center-r, -r*.38f), androidx.compose.ui.geometry.Size(r*2,r*2), style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        drawLine(accent.copy(.34f), Offset(center, 5.dp.toPx()), Offset(center, 23.dp.toPx()), 1.dp.toPx())
                    }
                    ExaltedVisualMotif.DRAGON_BLOODED -> {
                        val p = Path().apply {
                            moveTo(center-34.dp.toPx(),5.dp.toPx()); lineTo(center-19.dp.toPx(),13.dp.toPx())
                            lineTo(center-5.dp.toPx(),5.dp.toPx()); lineTo(center+9.dp.toPx(),14.dp.toPx())
                            lineTo(center+23.dp.toPx(),5.dp.toPx()); lineTo(center+36.dp.toPx(),11.dp.toPx())
                        }
                        drawPath(p, accent.copy(.42f), style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    }
                    ExaltedVisualMotif.LUNAR -> {
                        val r = 15.dp.toPx()
                        drawArc(accent.copy(.42f), 205f, 130f, false, Offset(center-r, -r*.30f), androidx.compose.ui.geometry.Size(r*2,r*2), style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        drawArc(deep.copy(.48f), 205f, 130f, false, Offset(center-r+5.dp.toPx(), -r*.30f), androidx.compose.ui.geometry.Size(r*2,r*2), style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                    }
                }
            }
    ) {
        val identidadeIcon = when {
            sheet.tipoPersonagem.isDragonBlooded() -> R.drawable.tab_icon_dragao
            sheet.tipoPersonagem.isLunar() -> R.drawable.tab_icon_lua
            else -> R.drawable.tab_icon_sol
        }
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(headerHeight)
                .padding(start = if (compactHeader) 14.dp else 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(identityWidth)
                    .fillMaxHeight()
                    .drawBehind {
                        val c = ExaltedAccentBright
                        val d = ExaltedStructuralMetalDeep
                        // Emblema tratado como selo arquitetônico, não medalhão.
                        drawLine(c.copy(alpha = .85f), Offset(8.dp.toPx(), 14.dp.toPx()), Offset(8.dp.toPx(), size.height - 14.dp.toPx()), 1.5f)
                        drawLine(d.copy(alpha = .8f), Offset(8.dp.toPx(), 14.dp.toPx()), Offset(size.width - 8.dp.toPx(), 14.dp.toPx()), 1f)
                        drawLine(c.copy(alpha = .42f), Offset(size.width - 8.dp.toPx(), 14.dp.toPx()), Offset(size.width - 8.dp.toPx(), size.height * .62f), 1f)
                        drawLine(c.copy(alpha = .30f), Offset(8.dp.toPx(), size.height - 14.dp.toPx()), Offset(size.width * .55f, size.height - 14.dp.toPx()), 1f)
                        // Passo 4/12 — o selo do cabeçalho deixa de compartilhar a
                        // mesma silhueta interna entre os três Tipos.
                        val cx=size.width*.5f; val cy=size.height*.5f
                        when (ExaltedActiveMotif) {
                            ExaltedVisualMotif.SOLAR -> {
                                val rr=25.dp.toPx()
                                drawArc(c.copy(.28f),0f,360f,false,Offset(cx-rr,cy-rr),androidx.compose.ui.geometry.Size(rr*2,rr*2),style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                                drawLine(c.copy(.32f),Offset(cx,cy-31.dp.toPx()),Offset(cx,cy-24.dp.toPx()),.8.dp.toPx())
                                drawLine(c.copy(.32f),Offset(cx,cy+24.dp.toPx()),Offset(cx,cy+31.dp.toPx()),.8.dp.toPx())
                            }
                            ExaltedVisualMotif.DRAGON_BLOODED -> {
                                val p=Path().apply {
                                    moveTo(cx-29.dp.toPx(),cy-18.dp.toPx()); lineTo(cx-20.dp.toPx(),cy-25.dp.toPx())
                                    lineTo(cx+19.dp.toPx(),cy-25.dp.toPx()); lineTo(cx+29.dp.toPx(),cy-16.dp.toPx())
                                    lineTo(cx+24.dp.toPx(),cy+22.dp.toPx()); lineTo(cx+12.dp.toPx(),cy+28.dp.toPx())
                                }
                                drawPath(p,c.copy(.30f),style=androidx.compose.ui.graphics.drawscope.Stroke(.85.dp.toPx()))
                            }
                            ExaltedVisualMotif.LUNAR -> {
                                val rr=27.dp.toPx()
                                drawArc(c.copy(.28f),55f,250f,false,Offset(cx-rr,cy-rr),androidx.compose.ui.geometry.Size(rr*2,rr*2),style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                                drawArc(d.copy(.34f),55f,250f,false,Offset(cx-rr+7.dp.toPx(),cy-rr),androidx.compose.ui.geometry.Size(rr*2,rr*2),style=androidx.compose.ui.graphics.drawscope.Stroke(.65.dp.toPx()))
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                TabIconDisplay(
                    iconRes = identidadeIcon,
                    tint = ExaltedAccentBright,
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
                    color = ExaltedOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        when {
                            sheet.tipoPersonagem.isDragonBlooded() -> sheet.aspecto.ifBlank { "Aspecto" }
                            sheet.tipoPersonagem.isLunar() -> sheet.lunarCasta.displayName
                            else -> sheet.casta.displayName
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                        color = ExaltedAccentBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(10.dp))
                    val completa = viewModel.isSheetComplete(sheet)
                    val cor = if (completa) ExaltedStatusCompleto else ExaltedStatusIncompleto
                    Box(Modifier.size(6.dp).background(cor))
                    Spacer(Modifier.width(6.dp))
                    AppText(
                        if (completa) "Planilha completa e válida" else "Planilha em criação",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = cor, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                Modifier.width(actionsWidth).fillMaxHeight().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InkButton(onClick = { onShowSettingsDialog(true) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Settings, "Configurações", tint = ExaltedGold, modifier = Modifier.size(20.dp))
                    }
                    Box {
                        InkButton(onClick = { onShowOpcoesMenu(true) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.MoreVert, "Opções", tint = ExaltedGold, modifier = Modifier.size(22.dp))
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
        }
        // Índice de capítulos: somente tipografia, ícone mínimo e linha ativa.
        // A navegação deixa de parecer uma fileira de botões.
        SheetTabsBar(sheet, viewModel, tabs, selectedTabIndex, onSelectedTab)
    }

    if(showCarregarMenu){
        AlertDialog(
            onDismissRequest={onShowCarregarMenu(false)},
            modifier=Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape=com.example.ui.components.dialogShape,
            title={AppText("Carregar",color=ExaltedAccentBright,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth(), forceStroke = true)},
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
            containerColor=ExaltedDarkSurface
        )
    }

}
