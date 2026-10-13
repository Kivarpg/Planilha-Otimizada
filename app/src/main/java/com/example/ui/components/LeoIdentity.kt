package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.isDragonBlooded
import com.example.model.isLunar
import com.example.ui.theme.ExaltVisualTemplate
import com.example.ui.theme.ExaltedSecondaryFont
import com.example.ui.theme.ExaltedVisualMotif
import com.example.ui.theme.LocalExaltedVisualTemplate

/** Pilot only. No global palette, character state or mechanical rules are changed. */
fun leoMotif(characterType: String): ExaltedVisualMotif = when {
    characterType.isDragonBlooded() -> ExaltedVisualMotif.DRAGON_BLOODED
    characterType.isLunar() -> ExaltedVisualMotif.LUNAR
    else -> ExaltedVisualMotif.SOLAR
}

/** Shared artwork mapping; all three identities have their own illustration. */
@Composable
fun LeoScene(motif: ExaltedVisualMotif, modifier: Modifier = Modifier,
             alignment: Alignment = Alignment.Center) {
    val scene = when (motif) {
        ExaltedVisualMotif.SOLAR -> com.example.R.drawable.leo_scene_solar
        ExaltedVisualMotif.DRAGON_BLOODED -> com.example.R.drawable.leo_scene_dragon
        ExaltedVisualMotif.LUNAR -> com.example.R.drawable.leo_scene_lunar
    }
    // Approved portrait artwork must remain complete at any viewport ratio.
    // Black letterboxing preserves composition without stretching or cropping.
    Image(painterResource(scene), contentDescription = null,
        modifier = modifier.background(Color.Black),
        contentScale = ContentScale.Fit, alignment = alignment)
}

/**
 * LEO frame: cached geometry painted BEFORE content. No clipping, intrinsic
 * measurement, pointer handlers, text or background emblem in this modifier.
 * The three corner silhouettes are deliberately different, not recoloured copies.
 */
fun Modifier.leoFrame(
    motif: ExaltedVisualMotif,
    visual: ExaltVisualTemplate,
    selected: Boolean = false,
    focused: Boolean = false,
    compact: Boolean = false,
    backgroundAlpha: Float = 1f,
): Modifier = drawWithCache {
    val inset = 1.dp.toPx()
    val inner = 4.dp.toPx()
    val cut = if (motif == ExaltedVisualMotif.LUNAR) 9.dp.toPx() else 6.dp.toPx()
    fun outline(padding: Float) = Path().apply {
        val right = size.width - padding
        val bottom = size.height - padding
        val bevel = cut.coerceAtMost((minOf(right, bottom) - padding).coerceAtLeast(0f) / 3f)
        moveTo(padding + bevel, padding)
        lineTo(right - bevel, padding); lineTo(right, padding + bevel)
        lineTo(right, bottom - bevel); lineTo(right - bevel, bottom)
        lineTo(padding + bevel, bottom); lineTo(padding, bottom - bevel)
        lineTo(padding, padding + bevel); close()
    }
    val outerPath = outline(inset)
    val innerPath = outline(inner)
    val corner = Path().apply {
        when (motif) {
            ExaltedVisualMotif.SOLAR -> {
                moveTo(1f, 28f); lineTo(1f, 1f); lineTo(28f, 1f)
                moveTo(5f, 24f); lineTo(5f, 5f); lineTo(24f, 5f)
                moveTo(1f, 12f); lineTo(12f, 12f); lineTo(12f, 1f)
                moveTo(5f, 18f); lineTo(18f, 18f); lineTo(18f, 5f)
                moveTo(9f, 9f); lineTo(15f, 9f); lineTo(15f, 15f); lineTo(9f, 15f); close()
            }
            ExaltedVisualMotif.DRAGON_BLOODED -> {
                moveTo(1f, 28f); lineTo(1f, 1f); lineTo(28f, 1f)
                moveTo(5f, 25f); lineTo(5f, 5f); lineTo(25f, 5f)
                moveTo(1f, 13f); lineTo(13f, 13f); lineTo(13f, 1f)
                moveTo(5f, 21f); lineTo(21f, 21f); lineTo(21f, 5f)
                moveTo(9f, 9f); lineTo(17f, 9f); lineTo(17f, 17f); lineTo(9f, 17f); close()
                moveTo(13f, 13f); lineTo(25f, 13f)
            }
            ExaltedVisualMotif.LUNAR -> {
                moveTo(1f, 28f); lineTo(1f, 12f); lineTo(12f, 1f); lineTo(28f, 1f)
                moveTo(5f, 25f); lineTo(5f, 14f); lineTo(14f, 5f); lineTo(25f, 5f)
                moveTo(1f, 1f); lineTo(21f, 21f)
                moveTo(1f, 1f); lineTo(3f, 13f); lineTo(13f, 3f); close()
                moveTo(9f, 19f); lineTo(19f, 9f); lineTo(22f, 22f); close()
            }
        }
    }
    val cornerScale = (if (compact) 17.dp else 24.dp).toPx() / 30f
    val metal = Brush.linearGradient(
        listOf(visual.metalDeep, visual.accentBright, visual.outline, visual.metalShine, visual.metalDeep),
        end = Offset(size.width, size.height)
    )
    val backdrop = Brush.linearGradient(
        listOf(visual.surface, Color(0xFF030405), visual.surface),
        end = Offset(size.width, size.height)
    )
    val glow = Brush.radialGradient(
        listOf(visual.accent.copy(alpha = .24f), Color.Transparent),
        center = Offset(size.width / 2f, size.height * .3f),
        radius = maxOf(size.width, size.height).coerceAtLeast(1f)
    )
    onDrawBehind {
        drawRect(backdrop, alpha = backgroundAlpha)
        if (selected) drawRect(glow)
        drawPath(outerPath, metal, style = Stroke(if (selected) 2.dp.toPx() else 1.dp.toPx()))
        drawPath(innerPath, visual.outline.copy(alpha = if (selected) .9f else .45f), style = Stroke(.65.dp.toPx()))
        val margin = 2.dp.toPx()
        for (right in listOf(false, true)) for (bottom in listOf(false, true)) {
            withTransform({
                translate(if (right) size.width - margin else margin, if (bottom) size.height - margin else margin)
                scale(if (right) -cornerScale else cornerScale, if (bottom) -cornerScale else cornerScale, pivot = Offset.Zero)
            }) { drawPath(corner, visual.accentBright, style = Stroke(1.2f)) }
        }
        if (selected) {
            // A shape, as well as light and stroke, communicates selection.
            val radius = 3.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height - 5.dp.toPx()
            val diamond = Path().apply {
                moveTo(cx, cy - radius); lineTo(cx + radius, cy)
                lineTo(cx, cy + radius); lineTo(cx - radius, cy); close()
            }
            drawPath(diamond, visual.accentBright)
        }
        if (focused && size.width > 14.dp.toPx() && size.height > 14.dp.toPx()) {
            drawRect(Color.White, Offset(7.dp.toPx(), 7.dp.toPx()),
                Size(size.width - 14.dp.toPx(), size.height - 14.dp.toPx()), style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
fun LeoSection(
    motif: ExaltedVisualMotif,
    modifier: Modifier = Modifier,
    title: String? = null,
    illustrated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visual = LocalExaltedVisualTemplate.current
    Box(modifier.fillMaxWidth().background(Color(0xFF030405))) {
        if (illustrated) {
            // Decorative sibling: no semantics, hit targets or intrinsic measurement.
            // The content determines height; the image never determines layout.
            LeoScene(motif, Modifier.matchParentSize())
        }
    Column(Modifier.fillMaxWidth()
        .leoFrame(motif, visual, backgroundAlpha = if (illustrated) .18f else 1f)
        .padding(horizontal = 18.dp, vertical = if (illustrated) 26.dp else 18.dp)) {
        if (title != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                AppText(title, fontFamily = ExaltedSecondaryFont, fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold, color = visual.onSurface,
                    modifier = Modifier.weight(1f), softWrap = true)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.width(48.dp).height(1.dp).background(
                    Brush.horizontalGradient(listOf(visual.accentBright, Color.Transparent))))
            }
        }
        content()
    }
    }
}

/** Responsive rows retain 3+2 or 2+2 at ordinary font scale; no fixed-width overflow. */
@Composable
fun <T> LeoEmblemGrid(items: List<T>, columns: Int, content: @Composable (T, Modifier) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val effectiveColumns = if (maxWidth < 250.dp || fontScale > 1.3f) minOf(2, columns) else columns
        val width = ((maxWidth - 8.dp * (effectiveColumns - 1)) / effectiveColumns).coerceAtMost(152.dp)
        val rows = remember(items, effectiveColumns) { items.chunked(effectiveColumns) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    row.forEach { item -> key(item) { content(item, Modifier.width(width)) } }
                }
            }
        }
    }
}

@Composable
fun LeoEmblemCard(
    label: String,
    imageRes: Int,
    motif: ExaltedVisualMotif,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = LocalExaltedVisualTemplate.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .97f else 1f, label = "leoEmblemPress")
    BoxWithConstraints(modifier) {
        val artSize = (maxWidth - 20.dp).coerceIn(40.dp, 112.dp)
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .leoFrame(motif, visual, selected = selected, focused = focused, compact = true)
                .onFocusChanged { focused = it.isFocused }
                .semantics { this.selected = selected }
                .feedbackClickable(role = Role.RadioButton, interactionSource = interaction, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(imageRes), contentDescription = null,
                modifier = Modifier.size(artSize), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(12.dp))
            AppText(label, fontFamily = ExaltedSecondaryFont, fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = visual.onSurface, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(), softWrap = true)
        }
    }
}

/** Selected tab gets a luminous baseline; neighbouring tabs have no dividing boxes. */
fun Modifier.leoTabSelection(visual: ExaltVisualTemplate, selected: Boolean): Modifier = drawWithCache {
    val fill = Brush.verticalGradient(listOf(Color.Transparent, visual.accent.copy(alpha = .24f)))
    onDrawBehind {
        if (selected) {
            drawRect(fill)
            drawLine(visual.accentBright, Offset(8.dp.toPx(), size.height - 3.dp.toPx()),
                Offset(size.width - 8.dp.toPx(), size.height - 3.dp.toPx()), strokeWidth = 2.dp.toPx())
        }
    }
}
