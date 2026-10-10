package com.example.ui
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.example.ui.components.AppText
import androidx.compose.material3.MaterialTheme
import com.example.model.CharacterSheet
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
internal fun SheetTabsBar(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    tabs: List<SheetTabNavigationItem>,
    selectedTabIndex: Int,
    onSelectedTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // Passo 9/12 — a composição reage ao espaço realmente disponível e ao
    // fontScale do usuário sem alterar a ordem, quantidade ou ação das abas.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale
    val compact = screenWidthDp < 600
    val enlargedText = fontScale > 1.20f
    val tabHeight = when { enlargedText -> 68.dp; compact -> 62.dp; else -> 60.dp }
    val tabMinWidth = when {
        enlargedText -> 132.dp
        compact -> 112.dp
        else -> 112.dp
    }
    val tabMaxWidth = if (enlargedText) 178.dp else 156.dp
    LaunchedEffect(selectedTabIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        val firstVisible = visible.firstOrNull()?.index
        val lastVisible = visible.lastOrNull()?.index
        if (firstVisible == null || lastVisible == null ||
            selectedTabIndex < firstVisible || selectedTabIndex > lastVisible
        ) {
            listState.scrollToItem(selectedTabIndex)
        }
    }
    val showsProgressStrip = selectedTabIndex in 2..8
    val pbExibido = if (showsProgressStrip && !sheet.planilhaConcluida) {
        remember(sheet, viewModel) { viewModel.calculateBpBreakdown(sheet).remainingBalance.coerceAtLeast(0) }
    } else {
        0
    }
    val pbEmAlerta = pbExibido <= 5
    val pontosHabilidadeRestantes = if (selectedTabIndex == 3) {
        remember(sheet, viewModel) { viewModel.calculateAbilityPointsRemaining(sheet) }
    } else {
        0
    }
    val totalPontosHabilidade = 28

    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // IDENTIDADE VISUAL v4 — índice de capítulos. Sem cartões, sem medalhões,
    // sem caixas individuais: cada aba é uma entrada editorial contínua.
    Column(modifier.fillMaxWidth().background(ExaltedDarkBackground.copy(alpha = .98f))) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(tabHeight)
                .drawBehind {
                    val y = size.height - 1.dp.toPx()
                    drawLine(ExaltedAccentBright.copy(alpha=.62f), Offset(8.dp.toPx(), y), Offset(size.width-8.dp.toPx(), y), 1.dp.toPx())
                    drawLine(ExaltedStructuralMetalDeep.copy(alpha=.55f), Offset(14.dp.toPx(), 3.dp.toPx()), Offset(size.width-14.dp.toPx(), 3.dp.toPx()), .8.dp.toPx())
                    // Estágio B: o trilho de navegação participa da mesma gramática do shell.
                    when (ExaltedActiveMotif) {
                        ExaltedVisualMotif.SOLAR -> {
                            val cx=size.width*.5f
                            drawLine(ExaltedStructuralMetalShine.copy(.34f),Offset(cx-34.dp.toPx(),6.dp.toPx()),Offset(cx,1.dp.toPx()),.8.dp.toPx())
                            drawLine(ExaltedStructuralMetalShine.copy(.34f),Offset(cx,1.dp.toPx()),Offset(cx+34.dp.toPx(),6.dp.toPx()),.8.dp.toPx())
                        }
                        ExaltedVisualMotif.DRAGON_BLOODED -> {
                            val p=Path().apply { moveTo(18.dp.toPx(),7.dp.toPx()); lineTo(40.dp.toPx(),2.dp.toPx()); lineTo(62.dp.toPx(),7.dp.toPx()); lineTo(84.dp.toPx(),3.dp.toPx()) }
                            drawPath(p,ExaltedAccentBright.copy(.25f),style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                        }
                        ExaltedVisualMotif.LUNAR -> drawArc(ExaltedStructuralMetalShine.copy(.28f),205f,130f,false,Offset(size.width*.5f-24.dp.toPx(),-17.dp.toPx()),androidx.compose.ui.geometry.Size(48.dp.toPx(),48.dp.toPx()),style=androidx.compose.ui.graphics.drawscope.Stroke(.8.dp.toPx()))
                    }
                },
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(tabs, key = { i, _ -> i }) { index, tab ->
                val selected = index == selectedTabIndex
                Column(
                    Modifier
                        .fillParentMaxHeight()
                        // Mantém as abas visualmente equilibradas: títulos curtos não
                        // encolhem demais e títulos longos ainda recebem espaço extra.
                        .widthIn(min = tabMinWidth, max = tabMaxWidth)
                        .feedbackClickable { onSelectedTab(index) }
                        .drawBehind {
                            val edge = ExaltedStructuralMetalDeep.copy(alpha=.48f)
                            drawLine(edge, Offset(size.width-1.dp.toPx(), 9.dp.toPx()), Offset(size.width-1.dp.toPx(), size.height-9.dp.toPx()), .8.dp.toPx())
                            if (selected) {
                                drawRect(ExaltedAccentBright.copy(alpha=.085f))
                                drawLine(ExaltedAccentBright.copy(alpha=.92f), Offset(5.dp.toPx(), size.height - 3.dp.toPx()), Offset(size.width-5.dp.toPx(), size.height - 3.dp.toPx()), 3.dp.toPx())
                                drawLine(ExaltedAccentBright.copy(alpha=.28f), Offset(8.dp.toPx(), 4.dp.toPx()), Offset(size.width-8.dp.toPx(), 4.dp.toPx()), 1.dp.toPx())
                                // LEO: seleção continua sendo a mesma aba/ação, mas o
                                // marcador deixa de ser apenas uma linha recolorida.
                                val cx=size.width*.5f
                                when (ExaltedActiveMotif) {
                                    ExaltedVisualMotif.SOLAR -> {
                                        drawLine(ExaltedAccentBright.copy(.55f),Offset(cx-13.dp.toPx(),7.dp.toPx()),Offset(cx,1.dp.toPx()),1.dp.toPx())
                                        drawLine(ExaltedAccentBright.copy(.55f),Offset(cx,1.dp.toPx()),Offset(cx+13.dp.toPx(),7.dp.toPx()),1.dp.toPx())
                                    }
                                    ExaltedVisualMotif.DRAGON_BLOODED -> {
                                        val p=Path().apply { moveTo(cx-15.dp.toPx(),5.dp.toPx()); lineTo(cx-5.dp.toPx(),1.dp.toPx()); lineTo(cx+4.dp.toPx(),6.dp.toPx()); lineTo(cx+15.dp.toPx(),2.dp.toPx()) }
                                        drawPath(p,ExaltedAccentBright.copy(.52f),style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                                    }
                                    ExaltedVisualMotif.LUNAR -> {
                                        drawArc(ExaltedAccentBright.copy(.48f),205f,130f,false,Offset(cx-12.dp.toPx(),-8.dp.toPx()),androidx.compose.ui.geometry.Size(24.dp.toPx(),24.dp.toPx()),style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    AppText(
                        text = tab.title,
                        color = if (selected) ExaltedOnSurface else ExaltedOnSurface.copy(alpha = .62f),
                        fontFamily = ExaltedSymbolFont,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                        fontSize = when { enlargedText -> 14.sp; compact -> 13.sp; else -> 13.sp },
                        letterSpacing = if (compact) .15.sp else .25.sp,
                        maxLines = if (compact) 2 else 1, softWrap = compact, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (showsProgressStrip) {
            Row(
                Modifier.fillMaxWidth().height(25.dp).background(ExaltedDarkSurface.copy(alpha = .72f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(7.dp).fillMaxHeight().background(ExaltedAccentBright.copy(alpha = .72f)))
                AppText(
                    text = if (sheet.planilhaConcluida) "Experiência Restante: ${sheet.experienciaDisponivel()}" else "Pontos de Bônus: $pbExibido / 15",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = .25.sp),
                    color = if (!sheet.planilhaConcluida && pbEmAlerta) ExaltedAlertaTexto else ExaltedGold,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 9.dp)
                )
                if (selectedTabIndex == 3) {
                    // Separa visualmente os dois saldos da Aba 4. O Spacer
                    // garante uma margem real mesmo quando ambos os textos
                    // ocupam todo o peso disponível da faixa.
                    Spacer(modifier = Modifier.width(24.dp))
                    AppText(
                        text = "Pontos de Habilidade: $pontosHabilidadeRestantes / $totalPontosHabilidade",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ExaltedGold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 10.dp), textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}