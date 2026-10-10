package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable

// ============================================================================
// ATENÇÃO: arquivo MISTO — componentes puramente visuais (GildedCard,
// SectionHeader, botões estilizados) convivem aqui com componentes que têm
// comportamento de verdade (RatingControl, CompactAutoSizeField, campos de
// texto com validação). Cada composable abaixo marcado com "// SKIN:" é
// seguro editar/reestilizar sem risco; qualquer coisa SEM essa marcação
// pode ter lógica própria — confira o corpo da função antes de mexer.
// ============================================================================

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExaltedDarkBackground
import com.example.ui.theme.ExaltedBlack
import com.example.R
import com.example.model.Casta
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGoldBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedMetalGoldCore
import com.example.ui.theme.ExaltedMetalGoldDeep
import com.example.ui.theme.ExaltedMetalGoldShine
import com.example.ui.theme.ExaltedStructuralMetal
import com.example.ui.theme.ExaltedStructuralMetalDeep
import com.example.ui.theme.ExaltedStructuralMetalShine
import com.example.ui.theme.ExaltedOutline
import com.example.ui.theme.corTextoContraste
import com.example.ui.theme.ExaltedActiveMotif
import com.example.ui.theme.ExaltedVisualMotif

// SKIN: moldura/gradiente/cores do emblema de Casta são editáveis à vontade
// (Color.kt). A ARTE em si (R.drawable.caste_*) é INTOCÁVEL — só redimensionar,
// nunca redesenhar ou trocar o arquivo (regra do cliente, ver comentário abaixo).
@Composable
fun CasteEmblemButton(
    caste: Casta,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = com.example.ui.theme.Dimens.CasteAspectIconSize
) {
    val emblem = when (caste) {
        Casta.Dawn -> R.drawable.caste_dawn
        Casta.Zenith -> R.drawable.caste_zenith
        Casta.Twilight -> R.drawable.caste_twilight
        Casta.Night -> R.drawable.caste_night
        Casta.Eclipse -> R.drawable.caste_eclipse
    }

    Column(
        modifier = modifier
            .width(if (iconSize > com.example.ui.theme.Dimens.CasteAspectIconSize) 166.dp else 102.dp)
            .padding(5.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.verticalGradient(
                    listOf(
                        ExaltedDarkSurface,
                        if (isSelected) ExaltedAccentBright.copy(alpha = 0.10f) else ExaltedDarkBackground,
                        ExaltedDarkSurface
                    )
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                brush = if (isSelected) Brush.linearGradient(
                    listOf(ExaltedStructuralMetalDeep, ExaltedStructuralMetalShine, ExaltedAccentBright, ExaltedStructuralMetalShine, ExaltedStructuralMetalDeep)
                ) else Brush.linearGradient(
                    listOf(ExaltedOutline, ExaltedStructuralMetal, ExaltedOutline)
                ),
                shape = MaterialTheme.shapes.medium
            )
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // As imagens originais das castas nunca são redesenhadas/recoloridas
        // — só a moldura de exibição usa Crop pra garantir tamanho visual
        // uniforme entre os 5 emblemas (pedido explícito do usuário).
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(iconSize + 12.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            ExaltedGoldBright.copy(alpha = if (isSelected) 0.28f else 0.10f),
                            Color.Transparent
                        ),
                        radius = this.size.minDimension / 2f
                    )
                )
            }
            Image(
                painter = painterResource(id = emblem),
                contentDescription = caste.displayName,
                modifier = Modifier.size(iconSize),
                // ContentScale.Crop — pedido explícito do usuário: sem
                // isso (o padrão é Fit), emblemas com proporções internas
                // diferentes entre si (Dawn/Zenith/Twilight/Night/Eclipse)
                // apareciam em tamanhos visuais diferentes dentro da mesma
                // caixa, mesmo a caixa em si tendo o tamanho igual.
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        AppText(
            text = caste.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = ExaltedAccentBright,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// SKIN: pílula de seleção usada nas listas de Habilidades de Casta/Favorecidas
// e na grade de Encantos — esses três locais passam o tamanho explícito
// Dimens.PillMinWidth/PillMinHeight. Sem esse parâmetro (uso avulso do botão),
// a função cai no tamanho mínimo padrão definido logo abaixo (defaultMinSize).
// Cores do estado selecionado/não-selecionado ficam em Color.kt.
@Composable
fun ErgonomicMultiWordButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    mostrarIndicador: Boolean = false,
    matchCharmPaletteWhenUnselected: Boolean = false
) {
    val cleanLines = text.trim().split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    // Passo 6/12 — crítica da Sentinela A: a seleção não pode depender
    // somente de cor/glow. A mesma função preserva conteúdo e callback, mas
    // a silhueta responde ao Tipo ativo: régia/axial, facetada, ou lunar suave.
    val sentinelShape = when (ExaltedActiveMotif) {
        ExaltedVisualMotif.SOLAR -> RoundedCornerShape(4.dp)
        ExaltedVisualMotif.DRAGON_BLOODED -> CutCornerShape(7.dp)
        ExaltedVisualMotif.LUNAR -> RoundedCornerShape(14.dp)
    }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 58.dp)
            .clip(sentinelShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (isSelected) ExaltedAccentBright.copy(alpha = 0.96f) else if (matchCharmPaletteWhenUnselected) ExaltedBlack else ExaltedDarkSurfaceVariant,
                        if (isSelected) ExaltedAccentBright.copy(alpha = 0.92f) else if (matchCharmPaletteWhenUnselected) ExaltedBlack else ExaltedDarkSurface,
                        if (isSelected) ExaltedAccentBright.copy(alpha = 0.96f) else if (matchCharmPaletteWhenUnselected) ExaltedBlack else ExaltedDarkSurfaceVariant
                    )
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    if (!isSelected && matchCharmPaletteWhenUnselected) {
                        listOf(
                            ExaltedStructuralMetal.copy(alpha = 0.45f),
                            ExaltedStructuralMetal.copy(alpha = 0.45f)
                        )
                    } else {
                        listOf(ExaltedStructuralMetalDeep, ExaltedAccentBright, ExaltedStructuralMetalShine, ExaltedAccentBright, ExaltedStructuralMetalDeep)
                    }
                ),
                shape = sentinelShape
            )
            .drawBehind {
                val a = ExaltedAccentBright.copy(alpha = if (isSelected) .75f else .34f)
                when (ExaltedActiveMotif) {
                    ExaltedVisualMotif.SOLAR -> {
                        drawLine(a, androidx.compose.ui.geometry.Offset(size.width*.40f, 2f), androidx.compose.ui.geometry.Offset(size.width*.60f, 2f), 1.4f)
                        drawCircle(a.copy(alpha=.45f), 2.2.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width*.5f, 2.5.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(.9f))
                    }
                    ExaltedVisualMotif.DRAGON_BLOODED -> {
                        val y = 3.dp.toPx()
                        drawLine(a, androidx.compose.ui.geometry.Offset(size.width*.36f,y+2.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width*.44f,y),1.2f)
                        drawLine(a, androidx.compose.ui.geometry.Offset(size.width*.44f,y), androidx.compose.ui.geometry.Offset(size.width*.52f,y+3.dp.toPx()),1.2f)
                        drawLine(a, androidx.compose.ui.geometry.Offset(size.width*.52f,y+3.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width*.64f,y),1.2f)
                    }
                    ExaltedVisualMotif.LUNAR -> {
                        drawArc(a, 205f, 130f, false, topLeft = androidx.compose.ui.geometry.Offset(size.width*.44f, -3.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width*.12f, 10.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.1f))
                    }
                }
            }
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Indicador circular desativado por padrão — pedido explícito
            // do usuário, confirmado em múltiplos contextos (Atributos,
            // Habilidades, sugestões de catálogo em Armas/Equipamentos):
            // o marcador dificultava a leitura e não agregava informação
            // além do próprio texto do botão. Parâmetro mantido (em vez de
            // remover o bloco inteiro) só pra alguma futura tela que
            // precise reativá-lo explicitamente.
            if (mostrarIndicador) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (enabled) ExaltedAccentBright else ExaltedMuted)
                )
                Spacer(modifier = Modifier.width(9.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val corDeFundo = if (isSelected) ExaltedAccentBright else if (matchCharmPaletteWhenUnselected) ExaltedBlack else ExaltedDarkSurfaceVariant
                cleanLines.forEach { line ->
                    AutoSizeText(
                        text = line,
                        maxFontSize = 13.sp,
                        minFontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = corTextoContraste(corDeFundo),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
