package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedVisualBlueprints

/**
 * Ícone de identidade (emblema no topo da planilha, em SheetTopBar.kt). Aplica
 * a cor da paleta ativa:
 * - Solar: Modulate sobre o PNG dourado (já é a cor certa).
 * - Sangue de Dragão: matriz vermelha.
 * - Lunar: SrcIn com tint prateado — Modulate deixava o ouro do arquivo
 *   visível mesmo com a paleta prata ativa.
 *
 * A caixa externa (com o halo de brilho) é sempre Dimens.TabIconGlowMultiplier
 * vezes maior que [size], e o raio do halo é derivado DESSE MESMO
 * multiplicador (não de um valor fixo) — garante margem de segurança
 * proporcional mesmo que o multiplicador mude no futuro. Histórico: antes
 * o raio usava um número fixo que só tinha folga por coincidência com o
 * multiplicador da época; quando o redesenho visual (ExaltedSkin) reduziu
 * o multiplicador sem tocar aqui, a margem zerou e o halo voltou a ser
 * cortado visivelmente.
 */
@Composable
internal fun TabIconDisplay(
    iconRes: Int,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    isDragonBlooded: Boolean = false,
    isLunar: Boolean = false,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val selecionada = tint != ExaltedMuted
    Box(
        modifier = modifier
            .size(size * com.example.ui.theme.Dimens.TabIconGlowMultiplier)
            .drawBehind {
                if (selecionada) {
                    val glow = when {
                        isDragonBlooded -> ExaltedVisualBlueprints.SangueDeDragao.palette.primaryBright
                        isLunar -> ExaltedAccentBright
                        else -> ExaltedAccentBright
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0f))
                        ),
                    // Raio derivado do multiplicador real da caixa (não de um
                    // número fixo) — histórico: a versão anterior usava
                    // "size * 0.55f", um valor que só tinha folga porque na
                    // época TabIconGlowMultiplier era 1.35 (diâmetro 1.1x
                    // cabendo com folga numa caixa de 1.35x). Quando o
                    // redesenho visual (ExaltedSkin) reduziu o multiplicador
                    // pra 1.10 sem tocar neste arquivo, a margem de segurança
                    // zerou (diâmetro = caixa, corte visível de novo — log
                    // confirmado pelo usuário). Esta fórmula usa 40% do
                    // espaço que o multiplicador reserva ALÉM do tamanho
                    // base, mantendo uma margem PERCENTUAL constante — segura
                    // não importa qual valor o multiplicador venha a ter.
                    radius = size.toPx() * com.example.ui.theme.Dimens.TabIconGlowMultiplier * 0.4f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val filtroVermelho = remember(selecionada, isDragonBlooded) {
            val fatorEsmaecimento = if (selecionada) 1f else 0.55f
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.05f * fatorEsmaecimento, 0f, 0f, 0f, 0f,
                        0f, 0.42f * fatorEsmaecimento, 0f, 0f, 0f,
                        0f, 0f, 0.35f * fatorEsmaecimento, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        // Lunar: desatura e força prata via SrcIn — remove o matiz dourado
        // embutido no asset. Solar mantém Modulate (asset já é dourado).
        val filtro = when {
            isDragonBlooded -> filtroVermelho
            isLunar -> ColorFilter.tint(tint, BlendMode.SrcIn)
            else -> ColorFilter.tint(tint, BlendMode.Modulate)
        }
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(size).padding(2.dp),
            contentScale = ContentScale.Fit,
            colorFilter = filtro
        )
    }
}
