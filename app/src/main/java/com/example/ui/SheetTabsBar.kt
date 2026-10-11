package com.example.ui
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val visual = LocalExaltedVisualTemplate.current
    val listState = rememberLazyListState()
    // Passo 9/12 — a composição reage ao espaço realmente disponível e ao
    // fontScale do usuário sem alterar a ordem, quantidade ou ação das abas.
    val fontScale = LocalDensity.current.fontScale
    val enlargedText = fontScale > 1.20f
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
        viewModel.calculateBpBreakdown(sheet).remainingBalance.coerceAtLeast(0)
    } else {
        0
    }
    val pbEmAlerta = pbExibido <= 5
    val pontosHabilidadeRestantes = if (selectedTabIndex == 3) {
        viewModel.calculateAbilityPointsRemaining(sheet)
    } else {
        0
    }
    val totalPontosHabilidade = 28

    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // IDENTIDADE VISUAL v4 — índice de capítulos. Sem cartões, sem medalhões,
    // sem caixas individuais: cada aba é uma entrada editorial contínua.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        val tabHeight = when { enlargedText -> 68.dp; compact -> 62.dp; else -> 60.dp }
        val tabMinWidth = if (enlargedText) 132.dp else 112.dp
        val tabMaxWidth = if (enlargedText) 178.dp else 156.dp
    Column(Modifier.fillMaxWidth().background(visual.surface)) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(tabHeight)
                .background(visual.surfaceVariant.copy(alpha = .72f))
                .border(
                    width = 1.dp,
                    color = visual.divider.copy(alpha = .58f)
                ),
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
                        .background(
                            if (selected) visual.accentBright.copy(alpha = .10f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) visual.accentBright.copy(alpha = .52f)
                            else visual.divider.copy(alpha = .28f)
                        )
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    AppText(
                        text = tab.title,
                        color = if (selected) visual.onSurface else visual.onSurface.copy(alpha = .62f),
                        fontFamily = ExaltedSymbolFont,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                        fontSize = when { enlargedText -> 14.sp; compact -> 13.sp; else -> 13.sp },
                        letterSpacing = if (compact) .15.sp else .25.sp,
                        maxLines = if (compact || enlargedText) 2 else 1, softWrap = compact || enlargedText, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (showsProgressStrip && selectedTabIndex == 3 && (compact || enlargedText)) {
            // Em telas estreitas, dois saldos lado a lado truncavam o texto.
            // Mantemos os mesmos valores e apenas distribuimos em duas linhas.
            Column(
                Modifier.fillMaxWidth().background(visual.surface.copy(alpha = .72f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                AppText(
                    text = if (sheet.planilhaConcluida) "Experiência Restante: ${sheet.experienciaDisponivel()}" else "Pontos de Bônus: $pbExibido / 15",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = if (!sheet.planilhaConcluida && pbEmAlerta) ExaltedAlertaTexto else visual.accentBright,
                    maxLines = 2, softWrap = true
                )
                AppText(
                    text = "Pontos de Habilidade: $pontosHabilidadeRestantes / $totalPontosHabilidade",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = visual.accentBright,
                    maxLines = 2, softWrap = true
                )
            }
        } else if (showsProgressStrip) {
            Row(
                Modifier.fillMaxWidth().height(if (enlargedText) 34.dp else 25.dp).background(visual.surface.copy(alpha = .72f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = if (sheet.planilhaConcluida) "Experiência Restante: ${sheet.experienciaDisponivel()}" else "Pontos de Bônus: $pbExibido / 15",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = .25.sp),
                    color = if (!sheet.planilhaConcluida && pbEmAlerta) ExaltedAlertaTexto else visual.accentBright,
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
                        color = visual.accentBright, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 10.dp), textAlign = TextAlign.End
                    )
                }
            }
        }
    }
    }
}
