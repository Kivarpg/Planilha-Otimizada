package com.example.ui.components
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LunarCasta
import com.example.ui.theme.*

/**
 * Botão de emblema de Casta Lunar.
 * Usa as imagens fornecidas (011–014) convertidas para webp:
 *   - caste_lua_cheia.webp     (Full Moon)
 *   - caste_lua_minguante.webp (Changing Moon)
 *   - caste_lua_nova.webp      (No Moon)
 *   - caste_sem_casta.webp     (Casteless)
 *
 * O nome em inglês fica no modelo (LunarCasta.englishName) e não é
 * exibido na UI (preparado para futura localização).
 */
@Composable
fun LunarCasteEmblemButton(
    caste: LunarCasta,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = Dimens.CasteAspectIconSize
) {
    val emblemRes = when (caste) {
        LunarCasta.FullMoon -> R.drawable.caste_lua_cheia
        LunarCasta.ChangingMoon -> R.drawable.caste_lua_minguante
        LunarCasta.NoMoon -> R.drawable.caste_lua_nova
        LunarCasta.Casteless -> R.drawable.caste_sem_casta
    }

    Column(
        modifier = modifier
            // APPROVED VISUAL CUSTOMIZATION
            // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
            // Caixa padronizada para comportar inclusive "Lua Minguante" sem corte.
            // Largura escala com o tamanho real do ícone (halo = iconSize + 12dp,
            // mais o padding horizontal de 12dp de cada lado) — antes era um valor
            // fixo (166dp) que, com iconSize maior que o padrão (ex.: exibição em
            // dobro na Aba 2), deixava margem mínima e cortava visualmente o halo
            // circular no limite arredondado do card.
            .width(maxOf(122.dp, iconSize + 12.dp + 24.dp + 16.dp))
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
                    listOf(ExaltedStructuralMetalDeep, ExaltedAccentBright, ExaltedStructuralMetalShine, ExaltedAccentBright, ExaltedStructuralMetalDeep)
                ) else Brush.linearGradient(
                    listOf(ExaltedOutline, ExaltedStructuralMetal, ExaltedOutline)
                ),
                shape = MaterialTheme.shapes.medium
            )
            .feedbackClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Halo sutil de prata quando selecionado
            androidx.compose.foundation.Canvas(modifier = Modifier.size(iconSize + 12.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            ExaltedAccentBright.copy(alpha = if (isSelected) 0.28f else 0.10f),
                            Color.Transparent
                        )
                    )
                )
            }
            androidx.compose.foundation.Image(
                painter = painterResource(emblemRes),
                contentDescription = caste.displayName,
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(6.dp))
        AppText(
            text = caste.displayName,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = ExaltedAccentBright,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        // Nome em inglês permanece no modelo (caste.englishName) e é
        // invisível na UI — preparado para a futura opção de idioma.
    }
}
