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
import androidx.compose.material3.LocalContentColor
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
import com.example.ui.theme.ExaltedDivider
import com.example.ui.theme.ExaltedTextStroke
import com.example.ui.theme.ExaltedTextOutline
import com.example.ui.theme.Dimens
import com.example.ui.theme.LocalExaltedVisualTemplate

// Extraído dos 12 arquivos que já copiavam este mesmo bloco de cores toda
// vez que um campo de texto novo era criado ao longo da sessão (refatoração
// de organização — pedido explícito do usuário). Paleta padrão "Exalted"
// pra qualquer OutlinedTextField do app — usar isso em vez de repetir o
// bloco inline evita que uma futura mudança de paleta precise ser feita
// em uma dúzia de lugares diferentes.
@Composable
fun exaltedTextFieldColors(): androidx.compose.material3.TextFieldColors {
    val visual = LocalExaltedVisualTemplate.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = visual.accentBright,
        unfocusedBorderColor = visual.outline.copy(alpha = 0.55f),
        focusedLabelColor = visual.accentBright,
        unfocusedLabelColor = visual.muted,
        cursorColor = visual.accentBright,
        selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
            handleColor = visual.accentBright,
            backgroundColor = visual.accentBright.copy(alpha = 0.38f)
        ),
        focusedTextColor = visual.onSurface,
        unfocusedTextColor = visual.onSurface,
        focusedContainerColor = visual.surface,
        unfocusedContainerColor = visual.surfaceVariant,
        focusedPlaceholderColor = visual.muted,
        unfocusedPlaceholderColor = visual.muted,
        disabledContainerColor = visual.surfaceVariant,
        errorContainerColor = visual.surfaceVariant
    )
}
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
    val visual = LocalExaltedVisualTemplate.current
    val inheritedContentColor = LocalContentColor.current
    val resolvedColor = when {
        color != Color.Unspecified -> color
        style.color != Color.Unspecified -> style.color
        // Material3 pode fornecer preto como cor herdada em cards com
        // container customizado. Na skin escura, o padrao e sempre claro.
        inheritedContentColor == Color.Black -> visual.onSurface
        inheritedContentColor != Color.Unspecified -> inheritedContentColor
        else -> visual.onSurface
    }
    val resolvedStyle = style.copy(
        color = resolvedColor,
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        fontStyle = fontStyle ?: style.fontStyle,
        fontFamily = fontFamily ?: style.fontFamily,
        letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing else style.letterSpacing,
        textDecoration = textDecoration ?: style.textDecoration,
        textAlign = textAlign ?: style.textAlign,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight,
        shadow = style.shadow ?: ExaltedTextOutline
    )
    Text(text = text, modifier = modifier, color = resolvedColor, style = resolvedStyle, overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines, onTextLayout = onTextLayout)
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
    val visual = LocalExaltedVisualTemplate.current
    val inheritedContentColor = LocalContentColor.current
    val resolvedColor = when {
        color != Color.Unspecified -> color
        style.color != Color.Unspecified -> style.color
        // Material3 pode fornecer preto como cor herdada em cards com
        // container customizado. Na skin escura, o padrao e sempre claro.
        inheritedContentColor == Color.Black -> visual.onSurface
        inheritedContentColor != Color.Unspecified -> inheritedContentColor
        else -> visual.onSurface
    }
    val effectiveStyle = style.copy(
        color = resolvedColor,
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        fontFamily = fontFamily ?: style.fontFamily,
        textAlign = textAlign ?: style.textAlign,
        shadow = style.shadow ?: ExaltedTextOutline
    )
    // Uma única camada de texto: evita divergência de medição/recorte entre
    // contorno e preenchimento em layouts estreitos. O contraste passa a ser
    // responsabilidade do token de cor resolvido, não de uma segunda pintura.
    BasicText(
        text = text,
        modifier = modifier,
        style = effectiveStyle,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val visual = LocalExaltedVisualTemplate.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        visual.metalDeep.copy(alpha = .92f),
                        visual.surfaceVariant,
                        visual.accentBright.copy(alpha = .22f),
                        visual.surfaceVariant
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        visual.metalShine.copy(alpha = .72f),
                        visual.accentBright.copy(alpha = .65f),
                        visual.outline.copy(alpha = .75f),
                        visual.metalDeep.copy(alpha = .85f)
                    )
                ),
                shape = RectangleShape
            )
            .padding(start = 8.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        visual.accentBright.copy(alpha = .34f),
                        visual.surfaceVariant.copy(alpha = .92f),
                        visual.surfaceVariant.copy(alpha = .74f)
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AutoSizeText(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            maxFontSize = 20.sp,
            minFontSize = 14.sp,
            letterSpacing = 0.7.sp,
            color = visual.accentBright,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f, fill = true)
        )
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
    Box(modifier = modifier.onSizeChanged { availableTextWidthPx = it.width }) {
        // Uma única camada mantém medição, alinhamento e pintura sincronizados.
        // A auto-redução continua idêntica; apenas removemos o contorno duplicado
        // que podia divergir do preenchimento em constraints estreitas.
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
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.sp,
    maxFontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 8.sp,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val resolvedColor = if (color != Color.Unspecified) color else LocalExaltedVisualTemplate.current.onSurface
    AutoSizeText(
        text = text,
        modifier = modifier,
        color = resolvedColor,
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
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    forceStroke: Boolean = false
) {
    val resolvedColor = if (color != Color.Unspecified) color else LocalExaltedVisualTemplate.current.onSurface
    AppText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = resolvedColor,
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
    titleColor: Color = Color.Unspecified
) {
    val visual = LocalExaltedVisualTemplate.current
    val resolvedTitleColor = if (titleColor != Color.Unspecified) titleColor else visual.accentBright
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AutoSizeText(
            text = nomePt,
            color = resolvedTitleColor,
            fontWeight = FontWeight.Bold,
            maxFontSize = 16.sp,
            minFontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (nomeEn.isNotBlank()) {
            AutoSizeText(
                text = nomeEn,
                color = visual.onSurface,
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
    val visual = LocalExaltedVisualTemplate.current
    // "Quadro" (caixa de destaque) do livro original: título e corpo
    // centralizados, com borda e fundo sutil
    // pra parecer uma caixa de verdade, não só um parágrafo a mais no meio
    // do texto. Movido de CharmsTab.kt pra cá (2026-09) pra ser reutilizado
    // também na navegação de Sangue de Dragão, sem duplicar o composable.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, visual.outline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .background(visual.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (quadro.titulo.isNotBlank()) {
            AppText(
                quadro.titulo,
                fontWeight = FontWeight.Bold,
                color = visual.accentBright,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (quadro.texto.isNotBlank()) {
            // Pedido do usuário: textos dentro de caixas centralizados.
            AppText(
                quadro.texto,
                color = visual.onSurface,
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
    val visual = LocalExaltedVisualTemplate.current
    var expandido by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, visual.outline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .background(visual.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small)
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
                color = visual.accentBright,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            androidx.compose.material3.Icon(
                imageVector = if (expandido) androidx.compose.material.icons.Icons.Default.ArrowDropUp else androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                contentDescription = if (expandido) "Retrair" else "Expandir",
                tint = visual.accentBright
            )
        }
        androidx.compose.animation.AnimatedVisibility(visible = expandido) {
            // Pedido do usuário: texto do Poder da Anima (Aba 2) justificado.
            AppText(
                texto,
                color = visual.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
