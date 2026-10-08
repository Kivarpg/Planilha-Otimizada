package com.example.oldrealm.highrealm

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** Renders one already-packed High Realm column from top to bottom. */
@Composable
fun HighRealmGlyphCanvas(
    column: HighRealmLayout.Column,
    modifier: Modifier = Modifier,
    glyphColor: Color = com.example.ui.theme.ExaltedAccentBright,
    minCellDp: Float = 28f,
    maxCellDp: Float = 64f
) {
    val context = LocalContext.current
    val typeface = remember(context) {
        runCatching { Typeface.createFromAsset(context.assets, "fonts/High_Realm_Script_Ex.ttf") }
            .getOrDefault(Typeface.DEFAULT)
    }
    val rows = column.words.sumOf { it.size } +
        (column.words.size - 1).coerceAtLeast(0) * HighRealmLayout.WORD_GAP_ROWS

    BoxWithConstraints(modifier = modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val minCellPx = with(density) { minCellDp.dp.toPx() }
        val maxCellPx = with(density) { maxCellDp.dp.toPx() }
        val cellPx = (widthPx * 0.92f).coerceIn(minCellPx, maxCellPx)
        // Reserva uma margem vertical adicional dentro de cada célula para
        // que os extremos da fonte nunca encostem ou sejam cortados pela
        // célula visual seguinte.
        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
        val verticalPaddingPx = with(density) { 8.dp.toPx() }
        val heightDp = with(density) { max(1f, rows * cellPx + verticalPaddingPx * 2f).toDp() }

        Canvas(Modifier.fillMaxWidth().height(heightDp)) {
            if (rows <= 0) return@Canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = cellPx * 0.78f
                style = Paint.Style.FILL
                color = android.graphics.Color.argb(
                    (glyphColor.alpha * 255f).toInt(),
                    (glyphColor.red * 255f).toInt(),
                    (glyphColor.green * 255f).toInt(),
                    (glyphColor.blue * 255f).toInt()
                )
            }
            val fontAscent = -paint.fontMetrics.ascent
            val fontDescent = paint.fontMetrics.descent
            val baselineOffset = verticalPaddingPx + (cellPx - (fontAscent + fontDescent)) / 2f + fontAscent
            var row = 0
            column.words.forEachIndexed { wordIndex, word ->
                word.forEach { glyph ->
                    drawContext.canvas.nativeCanvas.drawText(
                        glyph,
                        size.width / 2f - paint.measureText(glyph) / 2f,
                        row * cellPx + baselineOffset,
                        paint
                    )
                    row++
                }
                if (wordIndex < column.words.lastIndex) {
                    row += HighRealmLayout.WORD_GAP_ROWS
                }
            }
        }
    }
}
