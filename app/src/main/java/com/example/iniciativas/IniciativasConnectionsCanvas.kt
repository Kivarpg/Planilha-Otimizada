package com.example.iniciativas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedAccentBright

private val CorClashConexao = Color(0xFF9C27B0)
private val CorLinhaNormalConexao: Color get() = ExaltedAccentBright
private val GutterWidthConexao = 28.dp

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Aba 12 — camada exclusivamente gráfica das conexões de declaração/Clash.
@Composable
internal fun IniciativasConnectionsCanvas(
    declaracoes: Map<String, String>,
    centros: SnapshotStateMap<String, Offset>,
    modifier: Modifier = Modifier,
    declaracoesMulti: Map<String, List<String>> = emptyMap()
) {
    Canvas(modifier) {
        val gutter = size.width - GutterWidthConexao.toPx() / 2f
        val shaftWidth = 5.dp.toPx()
        val glowWidth = 12.dp.toPx()
        val headLength = 18.dp.toPx()
        val headHalfWidth = 7.5.dp.toPx()
        val headNotch = 5.dp.toPx()
        val margemCard = 4.dp.toPx()

        fun desenharSeta(de: String, para: String, cor: Color) {
            val origem = centros[de] ?: return
            val destino = centros[para] ?: return
            val yDest = destino.y
            val tipX = destino.x - margemCard
            val baseX = tipX + headLength
            val neckX = baseX - headNotch
            val inicioX = origem.x + margemCard

            val pathShaft = Path().apply {
                moveTo(inicioX, origem.y)
                lineTo(gutter, origem.y)
                lineTo(gutter, yDest)
                lineTo(neckX, yDest)
            }
            drawPath(pathShaft, color = cor.copy(alpha = 0.18f), style = Stroke(width = glowWidth))
            drawPath(pathShaft, color = cor, style = Stroke(width = shaftWidth))

            val pathHead = Path().apply {
                moveTo(tipX, yDest)
                lineTo(baseX, yDest - headHalfWidth)
                lineTo(neckX, yDest)
                lineTo(baseX, yDest + headHalfWidth)
                close()
            }
            drawPath(pathHead, color = cor.copy(alpha = 0.22f))
            drawPath(pathHead, color = cor)
        }

        declaracoes.forEach { (de, para) ->
            val reciproco = declaracoes[para] == de
            val cor = if (reciproco) CorClashConexao else CorLinhaNormalConexao
            desenharSeta(de, para, cor)
        }

        declaracoesMulti.forEach { (de, alvos) ->
            alvos.forEach { para ->
                desenharSeta(de, para, CorLinhaNormalConexao)
            }
        }
    }
}
