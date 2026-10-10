package com.example.ui.components

import androidx.compose.ui.draw.rotate

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EncantoQuadro
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedOutline
import com.example.ui.theme.ExaltedDivider
import com.example.ui.theme.ExaltedTextFill
import com.example.ui.theme.ExaltedTextStroke
import com.example.ui.theme.ExaltedStructuralMetalShine
import com.example.ui.theme.ExaltedActiveMotif
import com.example.ui.theme.ExaltedVisualMotif
import com.example.ui.theme.Dimens

// Extraído dos 12 arquivos que já copiavam este mesmo bloco de cores toda
// vez que um campo de texto novo era criado ao longo da sessão (refatoração
// de organização — pedido explícito do usuário). Paleta padrão "Exalted"
// pra qualquer OutlinedTextField do app — usar isso em vez de repetir o
// bloco inline evita que uma futura mudança de paleta precise ser feita
// em uma dúzia de lugares diferentes.
@Composable
fun exaltedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ExaltedAccentBright,
    unfocusedBorderColor = ExaltedOutline.copy(alpha = 0.55f),
    focusedLabelColor = ExaltedAccentBright,
    unfocusedLabelColor = ExaltedMuted,
    cursorColor = ExaltedAccentBright,
    selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(handleColor = ExaltedAccentBright, backgroundColor = ExaltedAccentBright.copy(alpha = 0.38f)),
    focusedTextColor = ExaltedOnSurface,
    unfocusedTextColor = ExaltedOnSurface,
    focusedContainerColor = Color(0xFF101014),
    unfocusedContainerColor = Color(0xFF17171B),
    focusedPlaceholderColor = ExaltedMuted,
    unfocusedPlaceholderColor = ExaltedMuted,
    disabledContainerColor = Color(0xFF17171B),
    errorContainerColor = Color(0xFF17171B)
)

// SKIN: cabeçalho de seção compartilhado por todas as abas. Tamanho do título,
// largura do flourish e do divisor ficam em Dimens.kt (SectionTitleMaxSize/
// MinSize, SectionFlourishWidth/Height, SectionDividerWidthFraction).
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: androidx.compose.ui.text.style.TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {},
    forceStroke: Boolean = false
) {
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // Texto normal e texto de botões: branco com contorno preto.
    // Títulos continuam usando a tipografia original, sem stroke.
    val titleFonts = setOf(
        MaterialTheme.typography.headlineSmall.fontFamily,
        MaterialTheme.typography.titleLarge.fontFamily,
        MaterialTheme.typography.titleMedium.fontFamily,
        MaterialTheme.typography.titleSmall.fontFamily
    )
    val isTitle = !forceStroke && (style.fontFamily in titleFonts ||
        fontFamily in titleFonts ||
        (fontSize.value != 0f && fontSize.value >= 18f))

    val requestedFontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize
    var fittedFontSize by remember(text, requestedFontSize, maxLines, minLines, modifier, style, fontFamily, letterSpacing, lineHeight, softWrap) { mutableStateOf(requestedFontSize) }
    val effectiveStyle = style.copy(
        color = if (isTitle) color else ExaltedTextFill,
        fontSize = fittedFontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        fontStyle = fontStyle ?: style.fontStyle,
        fontFamily = fontFamily ?: style.fontFamily,
        letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing else style.letterSpacing,
        textDecoration = textDecoration ?: style.textDecoration,
        textAlign = textAlign ?: style.textAlign,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight
    )

    if (isTitle) {
        androidx.compose.material3.Text(
            text = text,
            modifier = modifier,
            color = color,
            style = effectiveStyle,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && fittedFontSize.isSp && fittedFontSize.value > 8f) {
                    fittedFontSize = (fittedFontSize.value - 1f).coerceAtLeast(8f).sp
                }
                onTextLayout(result)
            }
        )
    } else {
        val strokeStyle = effectiveStyle.copy(
            color = ExaltedTextStroke,
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = with(LocalDensity.current) { Dimens.TextStrokeWidth.toPx() })
        )
        val fillStyle = effectiveStyle.copy(
            color = ExaltedTextFill,
            drawStyle = androidx.compose.ui.graphics.drawscope.Fill
        )
        // O Modifier externo pertence ao contêiner, não deve ser aplicado duas vezes.\n        // Os Text internos ocupam a largura medida pelo Box para preservar centralização.\n        Box(modifier = modifier) {
            androidx.compose.material3.Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = strokeStyle,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines
            )
            androidx.compose.material3.Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = fillStyle,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow && fittedFontSize.isSp && fittedFontSize.value > 8f) {
                        fittedFontSize = (fittedFontSize.value - 1f).coerceAtLeast(8f).sp
                    }
                    onTextLayout(result)
                }
            )
        }
    }
}

@Composable
fun AppText(
    text: androidx.compose.ui.text.AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {}
) {
    val effectiveStyle = style.copy(
        color = ExaltedTextFill,
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        fontFamily = fontFamily ?: style.fontFamily,
        textAlign = textAlign ?: style.textAlign
    )
    val density = LocalDensity.current
    // Mesmo ajuste do overload de String acima: o modifier vai nos BasicText
    // internos, não no Box, para que fillMaxWidth + textAlign realmente centralize.
    Box {
        BasicText(
            text = text,
            modifier = modifier,
            style = effectiveStyle.copy(
                color = ExaltedTextStroke,
                drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = with(density) { Dimens.TextStrokeWidth.toPx() }
                )
            ),
            overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines
        )
        BasicText(
            text = text,
            modifier = modifier,
            style = effectiveStyle,
            overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines,
            onTextLayout = onTextLayout
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // IDENTIDADE VISUAL v4 — marcador editorial aberto.
    // A seção não recebe uma caixa nem uma mini-moldura; ela funciona como
    // título de capítulo sobre o conteúdo.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(ExaltedStructuralMetalShine.copy(alpha=.075f), ExaltedDarkSurfaceVariant.copy(alpha=.82f), ExaltedAccentBright.copy(alpha=.045f), Color.Transparent)))
            .border(width = 1.dp, brush = Brush.horizontalGradient(listOf(ExaltedStructuralMetalShine.copy(alpha=.72f), ExaltedAccentBright.copy(alpha=.38f), ExaltedDivider.copy(alpha=.48f), Color.Transparent)), shape = RectangleShape)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // O antigo marcador temático à esquerda (incluindo o pequeno
        // zigue-zague vermelho do Sangue de Dragão) foi removido por pedido
        // explícito. Mantemos todo o restante do SectionHeader intacto.

        AutoSizeText(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            maxFontSize = 18.sp,
            minFontSize = 11.sp,
            letterSpacing = 1.35.sp,
            color = ExaltedOnSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f, fill = true)
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .widthIn(min = 12.dp, max = 44.dp)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(ExaltedDivider.copy(alpha = .7f), Color.Transparent)))
        )
        Spacer(Modifier.width(5.dp))
        Canvas(Modifier.size(18.dp)) {
            val c = center
            when (ExaltedActiveMotif) {
                ExaltedVisualMotif.SOLAR -> {
                    val r=size.minDimension*.18f
                    drawCircle(ExaltedAccentBright.copy(alpha=.68f), r, c, style=androidx.compose.ui.graphics.drawscope.Stroke(1.1f))
                    drawCircle(ExaltedAccentBright.copy(alpha=.20f), r*.34f, c)
                    drawLine(ExaltedAccentBright.copy(alpha=.48f), androidx.compose.ui.geometry.Offset(c.x-r*1.65f,c.y), androidx.compose.ui.geometry.Offset(c.x-r*1.15f,c.y), .9f)
                    drawLine(ExaltedAccentBright.copy(alpha=.48f), androidx.compose.ui.geometry.Offset(c.x+r*1.15f,c.y), androidx.compose.ui.geometry.Offset(c.x+r*1.65f,c.y), .9f)
                }
                ExaltedVisualMotif.DRAGON_BLOODED -> {
                    val r=size.minDimension*.16f
                    val p=androidx.compose.ui.graphics.Path().apply { moveTo(c.x,c.y-r); lineTo(c.x+r,c.y); lineTo(c.x,c.y+r); lineTo(c.x-r,c.y); close() }
                    drawPath(p, ExaltedAccentBright.copy(alpha=.72f), style=androidx.compose.ui.graphics.drawscope.Stroke(1.2f))
                    drawCircle(ExaltedAccentBright.copy(alpha=.24f), r*.35f, c)
                }
                ExaltedVisualMotif.LUNAR -> {
                    val r=size.minDimension*.23f
                    drawCircle(ExaltedStructuralMetalShine.copy(alpha=.52f), r, c)
                    drawCircle(ExaltedDarkSurface, r*.90f, androidx.compose.ui.geometry.Offset(c.x+r*.42f,c.y-r*.08f))
                }
            }
        }
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.sp,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 7.sp,
    textAlign: TextAlign = TextAlign.Center,
    style: androidx.compose.ui.text.TextStyle? = null,
    brush: Brush? = null
) {
    // Use the actual text container width rather than Modifier identity as a fit key.
    // This lets the font grow back when a weighted parent becomes wider.
    var availableTextWidthPx by remember { mutableStateOf(0) }
    var fontSize by remember(text, maxFontSize, minFontSize, style, letterSpacing, availableTextWidthPx) {
        mutableStateOf(maxFontSize)
    }
    val baseStyle = style ?: MaterialTheme.typography.bodyMedium
    val effectiveStyle = baseStyle.copy(
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        fontSize = fontSize,
        brush = brush,
        textAlign = textAlign
    )
    val density = LocalDensity.current
    val strokeWidth = with(density) { Dimens.TextStrokeWidth.toPx() }
    Box(modifier = modifier.onSizeChanged { availableTextWidthPx = it.width }) {
        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
        // AutoSizeText é usado em botões e rótulos que antes escapavam do
        // contorno global. Mantemos a mesma medição/auto-redução e aplicamos
        // somente o tratamento visual do contorno, sem alterar o callback.
        androidx.compose.material3.Text(
            text = text,
            color = ExaltedTextStroke,
            style = effectiveStyle.copy(brush = null, drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            // CORREÇÃO: sem fillMaxWidth aqui, o Text fica do tamanho do
            // próprio texto e "textAlign" não tem espaço sobrando pra
            // centralizar dentro de — por isso o rótulo aparecia encostado
            // à esquerda em vez de centralizado (Idioma/Intimidade na Aba 1,
            // caixas de Habilidade/Atributo na Aba 2).
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.Text(
            text = text,
            color = if (brush == null) color else Color.Unspecified,
            style = effectiveStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
            onTextLayout = { result: TextLayoutResult ->
                if ((result.didOverflowWidth || result.didOverflowHeight) && fontSize > minFontSize) {
                    val proximo = (fontSize.value * 0.92f).sp
                    fontSize = if (proximo < minFontSize) minFontSize else proximo
                }
            }
        )
    }
}

@Composable
fun AutoSizeAppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ExaltedOnSurface,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.sp,
    maxFontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 8.sp,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    AutoSizeText(
        text = text,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        maxFontSize = maxFontSize,
        minFontSize = minFontSize,
        textAlign = textAlign,
        style = style
    )
}

@Composable
fun JustifiedBodyAppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ExaltedOnSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    forceStroke: Boolean = false
) {
    AppText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = color,
        style = style.copy(textAlign = TextAlign.Justify),
        textAlign = TextAlign.Justify,
        forceStroke = forceStroke
    )
}

@Composable
fun CompactAutoSizeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    baseFontSize: TextUnit = 15.sp
) {
    val minFontSize = baseFontSize / 2f
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = MaterialTheme.typography.bodyMedium
    val view = LocalView.current
    var measuredWidthPx by remember { mutableStateOf(0) }

    val horizontalPaddingPx = with(density) { 28.dp.toPx() }
    val availableWidthPx = (measuredWidthPx.toFloat() - horizontalPaddingPx).coerceAtLeast(0f)
    val textToMeasure = value.ifEmpty { label }

    val fontSize = remember(textToMeasure, availableWidthPx, baseFontSize, minFontSize) {
        var candidate = baseFontSize
        if (availableWidthPx > 0f) {
            while (candidate > minFontSize) {
                val measured = textMeasurer.measure(
                    text = textToMeasure,
                    style = baseStyle.copy(fontSize = candidate),
                    maxLines = 1
                )
                if (measured.size.width <= availableWidthPx) break
                candidate = (candidate.value - 1f).coerceAtLeast(minFontSize.value).sp
            }
        }
        candidate
    }

    OutlinedTextField(
        value = value,
        onValueChange = { novoValor ->
            if (novoValor != value) InteractionFeedback.performTyping(view)
            onValueChange(novoValor)
        },
        label = { AppText(label, fontSize = fontSize * 0.85f, maxLines = 1) },
        singleLine = true,
        textStyle = baseStyle.copy(fontSize = fontSize),
        modifier = modifier
            .onSizeChanged { measuredWidthPx = it.width }
            .fillMaxWidth(),
        colors = exaltedTextFieldColors()
    )
}

// SKIN: componente ÚNICO pra qualquer corpo de texto longo (descrição de
// Encanto/Feitiço/Mérito/NPC, texto de quadro, etc) — SEMPRE use este em
// vez de um Text() avulso com textAlign=Justify, em QUALQUER aba nova que
// vier a existir. Duas coisas garantem que a justificação realmente
// apareça na tela (não é só "textAlign = Justify" sozinho):
//   1. Modifier.fillMaxWidth() — sem uma largura definida pra distribuir
//      o espaço, Justify não tem efeito nenhum (fica visualmente igual a
//      alinhamento à esquerda).
//   2. lineBreak = LineBreak.Paragraph — a estratégia de quebra de linha
//      "Simple" (usada por padrão em Text()) nem sempre distribui os
//      espaços de forma perceptível numa coluna estreita; "Paragraph" é
//      a mesma estratégia usada por editores de texto de verdade e
//      resolve isso de forma consistente entre aparelhos.
@Composable
fun JustifiedBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    AppText(
        text = text,
        color = color,
        style = style.copy(
            textAlign = TextAlign.Justify,
            lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// SKIN: título de Encanto/Feitiço em 2 linhas — nome em Português primeiro
// (linha 1, negrito), nome em Inglês depois (linha 2, itálico, um pouco
// menor). Separar em 2 linhas (em vez de "Nome (Nome em Inglês)" numa
// linha só) dobra a largura disponível pra cada nome individualmente,
// o que já resolve a maioria dos casos de nome cortado/quebrado sem
// precisar reduzir a fonte agressivamente. O tamanho abaixo (16sp/11sp,
// com piso de 12sp/9sp) foi calibrado contra o nome mais longo já
// cadastrado nos catálogos ("Espírito Mendicante Sobrevivente às
// Dificuldades", 48 caracteres) — reduz só esse caso extremo, mantendo
// os demais títulos consistentes entre si.
@Composable
fun ChamTitleTwoLines(
    nomePt: String,
    nomeEn: String,
    modifier: Modifier = Modifier,
    titleColor: Color = ExaltedAccentBright
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AutoSizeText(
            text = nomePt,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            maxFontSize = 16.sp,
            minFontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (nomeEn.isNotBlank()) {
            AutoSizeText(
                text = nomeEn,
                color = ExaltedOnSurface,
                fontWeight = FontWeight.Normal,
                maxFontSize = 11.sp,
                minFontSize = 9.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
@Composable
fun EncantoQuadroBox(quadro: EncantoQuadro, modifier: Modifier = Modifier) {
    // "Quadro" (caixa de destaque) do livro original: título e corpo
    // centralizados, com borda e fundo sutil
    // pra parecer uma caixa de verdade, não só um parágrafo a mais no meio
    // do texto. Movido de CharmsTab.kt pra cá (2026-09) pra ser reutilizado
    // também na navegação de Sangue de Dragão, sem duplicar o composable.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ExaltedOutline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .background(ExaltedDarkSurfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (quadro.titulo.isNotBlank()) {
            AppText(
                quadro.titulo,
                fontWeight = FontWeight.Bold,
                color = ExaltedGold,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (quadro.texto.isNotBlank()) {
            // Pedido do usuário: textos dentro de caixas centralizados.
            AppText(
                quadro.texto,
                color = ExaltedOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// SKIN: quadro expansível/retrátil pra descrições longas (Poder da Anima
// na Aba 2) — começa recolhido, toque em
// qualquer parte do card alterna entre expandido/retraído. O ícone de
// seta indica o estado atual.
@Composable
fun ExpandableTextCard(
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier
) {
    var expandido by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ExaltedOutline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .background(ExaltedDarkSurfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small)
            .feedbackClickable { expandido = !expandido }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            AppText(
                titulo,
                fontWeight = FontWeight.Bold,
                color = ExaltedGold,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            androidx.compose.material3.Icon(
                imageVector = if (expandido) androidx.compose.material.icons.Icons.Default.ArrowDropUp else androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                contentDescription = if (expandido) "Retrair" else "Expandir",
                tint = ExaltedGold
            )
        }
        androidx.compose.animation.AnimatedVisibility(visible = expandido) {
            // Pedido do usuário: texto do Poder da Anima (Aba 2) justificado.
            AppText(
                texto,
                color = ExaltedOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
