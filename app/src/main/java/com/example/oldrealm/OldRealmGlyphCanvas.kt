package com.example.oldrealm

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/** Renders one logical Old Realm input line. Words are indivisible and wrap as whole units. */
@Composable
fun OldRealmGlyphCanvas(
    words: List<List<String>>,
    modifier: Modifier = Modifier,
    targetSp: Float = 32f,
    minSp: Float = 18f,
    maxSp: Float = 48f,
    glyphColor: Color = com.example.ui.theme.ExaltedAccentBright,
    viewportWidthPx: Float = Float.NaN
) {
    val context = LocalContext.current
    val typeface = remember(context) {
        runCatching { Typeface.createFromAsset(context.assets, "OldRealm.ttf") }
            .getOrDefault(Typeface.DEFAULT)
    }
    BoxWithConstraints(modifier = modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val constrainedWidthPx = with(density) { maxWidth.toPx() }
        val widthPx = if (viewportWidthPx.isFinite() && viewportWidthPx > 0f) {
            viewportWidthPx
        } else if (constrainedWidthPx.isFinite() && constrainedWidthPx > 0f) {
            constrainedWidthPx
        } else {
            with(density) { 360.dp.toPx() }
        }
        val targetPx = with(density) { targetSp.sp.toPx() }
        val minPx = with(density) { minSp.sp.toPx() }
        val maxPx = with(density) { maxSp.sp.toPx() }
        val referenceWidthPx = with(density) { 360.dp.toPx() }
        val preferred = (targetPx * (widthPx / referenceWidthPx).coerceIn(0.78f, 1.5f)).coerceIn(minPx, maxPx)
        val paint = remember(typeface, preferred) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = preferred
            }
        }
        // CORREÇÃO — corte de caracteres na fonte Old Realm: o layout
        // dimensionava linhas/canvas somando paint.measureText() (a largura
        // de "avanço" do cursor de cada glifo), mas vários glifos dessa
        // fonte desenham tinta além da própria caixa de avanço (overhang) —
        // então o glifo mais largo de fato podia ultrapassar a borda direita
        // do Canvas e ser cortado, mesmo quando a soma das larguras "cabia".
        //
        // Aqui varremos TODO o inventário de sílabas (OldRealmSyllableMap.keys
        // — não só as sílabas do texto atual, pedido explícito do usuário:
        // "independentemente do texto traduzido"), comparando avanço
        // (measureText) contra a caixa de tinta real (getTextBounds) de cada
        // glifo, e guardamos a maior largura de tinta e as maiores sobras
        // (overhang) à direita e à esquerda de todo o alfabeto. Esses valores
        // viram a margem de segurança padrão usada no dimensionamento do
        // Canvas e nas decisões de quebra de linha abaixo — garantindo que
        // mesmo o caractere mais largo do alfabeto sempre caiba, qualquer
        // que seja o texto renderizado.
        data class MetricasGlifos(
            val larguraTintaMaxima: Float,
            val overhangDireita: Float,
            val overhangEsquerda: Float,
            val topoMinimo: Float,
            val baseMaxima: Float,
            val glifoMaisAlto: String,
            val alturaGlifoMaisAlto: Float
        )
        val metricasGlifos = remember(paint) {
            var larguraTintaMaxima = 0f
            var overhangDireita = 0f
            var overhangEsquerda = 0f
            var topoTintaMinimo = 0f
            var baseTintaMaxima = 0f
            var glifoMaisAlto = ""
            var alturaGlifoMaisAlto = 0f
            val bounds = android.graphics.Rect()
            OldRealmSyllableMap.keys.forEach { syllable ->
                val glyph = OldRealmSyllableMap.glyphString(syllable) ?: return@forEach
                val avanco = paint.measureText(glyph)
                bounds.setEmpty()
                paint.getTextBounds(glyph, 0, glyph.length, bounds)
                val larguraTinta = (bounds.right - bounds.left).toFloat()
                val alturaTinta = (bounds.bottom - bounds.top).toFloat()
                larguraTintaMaxima = max(larguraTintaMaxima, larguraTinta)
                overhangDireita = max(overhangDireita, bounds.right.toFloat() - avanco)
                overhangEsquerda = max(overhangEsquerda, -bounds.left.toFloat())
                topoTintaMinimo = minOf(topoTintaMinimo, bounds.top.toFloat())
                baseTintaMaxima = maxOf(baseTintaMaxima, bounds.bottom.toFloat())
                if (alturaTinta > alturaGlifoMaisAlto) {
                    alturaGlifoMaisAlto = alturaTinta
                    glifoMaisAlto = syllable
                }
            }
            // FontMetrics cobre a caixa vertical que a fonte declara para a
            // escala REAL usada pelo Paint. Usamos a união dela com a tinta
            // observada nos glifos: nenhum dos dois pode reduzir o outro.
            val fm = paint.fontMetrics
            val topoSeguro = minOf(topoTintaMinimo, fm.top)
            val baseSegura = maxOf(baseTintaMaxima, fm.bottom)
            MetricasGlifos(
                larguraTintaMaxima,
                overhangDireita,
                overhangEsquerda,
                topoSeguro,
                baseSegura,
                glifoMaisAlto,
                alturaGlifoMaisAlto
            )
        }

        // Folga aplicada nas bordas do conteúdo — cobre o pior caso do
        // alfabeto inteiro, com uma margem extra pequena por segurança.
        val folgaHorizontalPx = metricasGlifos.overhangDireita + 4f
        val margemEsquerdaPx = max(8f, metricasGlifos.overhangEsquerda + 4f)
        val gap = preferred * 0.85f
        fun widthOf(word: List<String>): Float =
            word.sumOf { glyph ->
                OldRealmSyllableMap.glyphString(glyph)?.let { paint.measureText(it).toDouble() } ?: 0.0
            }.toFloat() +
                (word.size - 1).coerceAtLeast(0) * preferred * 0.12f

        val layout = remember(words, widthPx, preferred) {
            val packed = mutableListOf<List<List<String>>>()
            var current = mutableListOf<List<String>>()
            var used = 0f
            words.forEach { word ->
                val w = widthOf(word)
                val extra = if (current.isEmpty()) 0f else gap
                if (current.isNotEmpty() && used + extra + w > widthPx) {
                    packed += current.toList()
                    current = mutableListOf()
                    used = 0f
                }
                current += word
                used += (if (current.size == 1) 0f else gap) + w
            }
            if (current.isNotEmpty()) packed += current.toList()
            val widths = packed.map { row ->
                row.sumOf { widthOf(it).toDouble() }.toFloat() +
                    (row.size - 1).coerceAtLeast(0) * gap + margemEsquerdaPx + folgaHorizontalPx
            }
            packed to widths
        }
        val rows = layout.first
        val rowWidths = layout.second
        // CORREÇÃO — corte vertical (embaixo): a versão anterior media
        // minGlyphTop/maxGlyphBottom varrendo só os glifos do texto ATUAL
        // ("words"), não o alfabeto inteiro. Isso fazia a altura de linha
        // ficar certa só quando o texto de teste incluía por acaso o glifo
        // mais alto/mais descendente da fonte — qualquer tradução que não
        // incluísse esse glifo específico calculava uma altura de linha
        // menor que o necessário, cortando por baixo os glifos mais altos.
        // Agora usamos os mesmos topoMinimo/baseMaxima já varridos em TODO
        // o alfabeto (metricasGlifos, acima) — mesmo procedimento da
        // correção horizontal, só que pro eixo vertical: sempre reserva
        // espaço pro pior caso do alfabeto inteiro, não só do texto atual.

        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
        // Folga física adicional acima e abaixo da tinta real do glifo.
        // A altura deixa de ter qualquer constante em pixels. Tudo é
        // calculado na escala efetiva do Paint e convertido pela densidade.
        // A margem inclui 12dp físicos + 1px de guarda para rasterização AA
        // em cada extremidade; assim ceil/arredondamentos não comem a última
        // linha de pixels do glifo em densidades fracionárias.
        val verticalPaddingPx = with(density) { 12.dp.toPx() }
        val rasterGuardPx = max(1f, preferred * 0.04f)
        val baselineOffset = verticalPaddingPx + rasterGuardPx - metricasGlifos.topoMinimo
        val requiredGlyphHeightPx = metricasGlifos.baseMaxima - metricasGlifos.topoMinimo
        val minRowHeightPx = requiredGlyphHeightPx +
            (verticalPaddingPx + rasterGuardPx) * 2f
        val rowHeight = kotlin.math.ceil(minRowHeightPx.toDouble()).toFloat()
        val contentWidthPx = max(widthPx, rowWidths.maxOrNull() ?: widthPx)
        val heightDp = with(density) { (max(1, rows.size) * rowHeight).toDp() }
        val contentWidthDp = with(density) { contentWidthPx.toDp() }

        Canvas(Modifier.width(contentWidthDp).height(heightDp)) {
            if (words.isEmpty()) return@Canvas
            val drawPaint = Paint(paint).apply {
                textSize = preferred
                color = android.graphics.Color.argb(
                    (glyphColor.alpha * 255f).toInt(),
                    (glyphColor.red * 255f).toInt(),
                    (glyphColor.green * 255f).toInt(),
                    (glyphColor.blue * 255f).toInt()
                )
            }
            rows.forEachIndexed { rowIndex, row ->
                var x = margemEsquerdaPx
                val y = rowIndex * rowHeight + baselineOffset
                row.forEach { word ->
                    word.forEach { syllable ->
                        val glyph = OldRealmSyllableMap.glyphString(syllable) ?: return@forEach
                        drawContext.canvas.nativeCanvas.drawText(glyph, x, y, drawPaint)
                        x += drawPaint.measureText(glyph) + preferred * 0.12f
                    }
                    x += gap
                }
            }
        }
    }
}
