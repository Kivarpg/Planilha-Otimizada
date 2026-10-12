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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExaltedBlack
import com.example.R
import com.example.model.Casta
import com.example.ui.theme.corTextoContraste
import com.example.ui.theme.LocalExaltedVisualTemplate

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
    val visual = LocalExaltedVisualTemplate.current
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
                        visual.surface,
                        if (isSelected) visual.accentBright.copy(alpha = 0.20f) else visual.surface,
                        visual.surface
                    )
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) Brush.linearGradient(
                    listOf(visual.metalDeep, visual.metalShine, visual.accentBright, visual.metalShine, visual.metalDeep)
                ) else Brush.linearGradient(
                    listOf(visual.outline, visual.metalShine, visual.outline)
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
            Box(
                modifier = Modifier
                    .size(iconSize + 12.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                visual.accentBright.copy(alpha = if (isSelected) 0.28f else 0.10f),
                                Color.Transparent
                            )
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
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
        }
        Spacer(modifier = Modifier.height(7.dp))
        AppText(
            text = caste.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = visual.accentBright,
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
    val visual = LocalExaltedVisualTemplate.current
    val cleanLines = text.trim().split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    // Geometria única e previsível. A identidade muda por tokens, não por
    // transforms/desenho manual que possam interferir em constraints.
    val sentinelShape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 58.dp)
            .clip(sentinelShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (isSelected) visual.accentBright.copy(alpha = 0.96f) else if (matchCharmPaletteWhenUnselected) ExaltedBlack else visual.surfaceVariant,
                        if (isSelected) visual.accentBright.copy(alpha = 0.92f) else if (matchCharmPaletteWhenUnselected) ExaltedBlack else visual.surface.copy(alpha = 0.98f),
                        if (isSelected) visual.accentBright.copy(alpha = 0.96f) else if (matchCharmPaletteWhenUnselected) ExaltedBlack else visual.surfaceVariant
                    )
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    if (!isSelected && matchCharmPaletteWhenUnselected) {
                        listOf(
                            visual.metalShine.copy(alpha = 0.45f),
                            visual.metalShine.copy(alpha = 0.45f)
                        )
                    } else {
                        listOf(visual.metalDeep, visual.accentBright, visual.metalShine, visual.accentBright, visual.metalDeep)
                    }
                ),
                shape = sentinelShape
            )
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
                        .background(if (enabled) visual.accentBright else visual.muted)
                )
                Spacer(modifier = Modifier.width(9.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val corDeFundo = if (isSelected) visual.accentBright else if (matchCharmPaletteWhenUnselected) ExaltedBlack else visual.surfaceVariant
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
