package com.example.ui.components



import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable

/*
 * InkButton — primitivo estrutural seguro.
 *
 * A geometria funcional é um Box simples com gradiente, borda, texto e
 * feedback de toque. SVG/Path/Canvas não participam mais do layout nem da
 * pintura principal; a decoração nunca pode recortar, cobrir ou reposicionar
 * o conteúdo clicável. A identidade visual vem do contexto composicional,
 * com override explícito para blocos independentes como NPCs da Aba 11.
 */

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.corTextoContraste
import com.example.ui.theme.ExaltVisualTemplate
import com.example.ui.theme.LocalExaltedVisualTemplate

// ---------------------------------------------------------------------
// Cores do primitivo. A identidade vem do template resolvido no escopo.
// ---------------------------------------------------------------------

private data class InkGradient(val start: Color, val mid: Color, val end: Color)

private object InkColors {
    // Perigo permanece vermelho fixo por semântica, independentemente do Tipo.
    val danger = InkGradient(Color(0xFF4C150D), Color(0xFF7A241A), Color(0xFFA6503F))
    val onDark = InkGradient(Color(0xFFFFFAF0), Color(0xFFF2ECE0), Color(0xFFC9C0AC))
    val textOnLight = ExaltedOnSurface
    val textOnDark = Color(0xFF221D16)
    val disabledGray = Color(0xFF8A8578)
}

// ---------------------------------------------------------------------
// 3. API pública do componente
// ---------------------------------------------------------------------

enum class InkButtonVariant { Primary, Secondary, Danger, ThematicDanger, OnDark, Ghost }

/**
 * Optional visual overrides. Null values preserve the existing template-aware palette.
 * Does not change the brush paths, sizing, click handling or interaction feedback.
 */
data class InkButtonStyle(
    val gradientStart: Color? = null,
    val gradientMiddle: Color? = null,
    val gradientEnd: Color? = null,
    val labelColor: Color? = null,
    val outlineColor: Color? = null,
)

enum class InkButtonSize(val width: Dp, val height: Dp, val fontSize: TextUnit) {
    // Tamanhos nominais preservados para não alterar os layouts existentes.
    Small(160.dp, 56.dp, 14.sp),
    Medium(236.dp, 68.dp, 16.sp),
    Large(300.dp, 84.dp, 18.sp),
}

/**
 * POLÍTICA GLOBAL DE BOTÕES
 *
 * Os tamanhos abaixo são deliberadamente generosos para impedir truncamento
 * visual e manter uma área de toque confortável. Telas que usam muitos botões
 * devem reorganizar seus containers (Row/Flow/Column) em vez de reduzir os
 * botões abaixo destes limites.
 *
 * Botão com fundo em gradiente temático e rótulo centralizado. A geometria
 * clicável é retangular/arredondada e independente de qualquer decoração.
 *
 * @param brushIndex parâmetro legado mantido para compatibilidade de chamadas.
 *   A versão estrutural segura não usa mais SVG/Canvas e, portanto, não varia
 *   geometria por índice.
 * @param label rótulo curto (1–3 palavras); textos longos truncam com "…".
 * @param selected destaca o botão com a cor temática do Exaltado ativo — usado nas pílulas de
 *   seleção (Habilidade/Atributo, Méritos etc.), equivalente ao
 *   isSelected=true do ErgonomicMultiWordButton antigo. Ignorado quando
 *   variant = Ghost (o traço fino não tem estado selecionado).
 * @param customWidth / @param customHeight sobrescrevem o tamanho fixo de
 *   [size] — usado nas grades de pílulas pequenas (Dimens.PillMinWidth/
 *   Height). O conteúdo sempre permanece dentro das constraints do próprio Box.
 */
@Composable
fun InkButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: InkButtonVariant = InkButtonVariant.Primary,
    size: InkButtonSize = InkButtonSize.Medium,
    enabled: Boolean = true,
    selected: Boolean = false,
    brushIndex: Int = 0,
    fillMaxWidth: Boolean = false,
    customWidth: Dp? = null,
    customHeight: Dp? = null,
    visualTemplate: ExaltVisualTemplate? = null,
    styleOverride: InkButtonStyle = InkButtonStyle(),
    content: (@Composable () -> Unit)? = null,
) {
    val resolvedVisual = visualTemplate ?: LocalExaltedVisualTemplate.current
    if (variant == InkButtonVariant.Ghost) {
        InkGhostButton(label, onClick, modifier, enabled, styleOverride, resolvedVisual)
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Equivalente ao :active do CSS (escala pra baixo ao pressionar).
    // Pra hover de mouse/trackpad (tablets, Chromebook), troque por
    // Modifier.hoverable(interactionSource) + collectIsHoveredAsState()
    // e anime pra 1.015f em vez de 1f no estado "não pressionado".
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = tween(120),
        label = "inkButtonScale",
    )

    // Uma única fonte de identidade: contexto da ficha ou override local.
    val ownerPrimary = InkGradient(resolvedVisual.surface, resolvedVisual.metalDeep, resolvedVisual.accent)
    val ownerSecondary = InkGradient(resolvedVisual.surfaceVariant, resolvedVisual.surface, resolvedVisual.metalDeep)
    val ownerSelected = InkGradient(resolvedVisual.accent, resolvedVisual.metalShine, resolvedVisual.metalFlash)
    val gradient = when {
        selected -> ownerSelected
        variant == InkButtonVariant.Primary -> ownerPrimary
        variant == InkButtonVariant.Secondary -> ownerSecondary
        variant == InkButtonVariant.Danger -> InkColors.danger
        variant == InkButtonVariant.ThematicDanger -> ownerPrimary
        variant == InkButtonVariant.OnDark -> InkColors.onDark
        else -> ownerPrimary // Ghost não chega aqui (early return acima)
    }
    val resolvedGradient = InkGradient(
        styleOverride.gradientStart ?: gradient.start,
        styleOverride.gradientMiddle ?: gradient.mid,
        styleOverride.gradientEnd ?: gradient.end,
    )
    // A cor precisa acompanhar o preenchimento realmente exibido, inclusive
    // overrides e estados selecionados. Uma cor fixa pode sumir sobre metais
    // claros de Solar/Lunar ou sobre um gradiente customizado.
    val renderedMid = if (enabled) resolvedGradient.mid else InkColors.disabledGray
    val textColor = styleOverride.labelColor
        ?: corTextoContraste(renderedMid)
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // Área de toque ampliada para impedir cortes de texto e facilitar interação.
    // Mantemos tamanhos customizados quando necessários, mas nunca permitimos
    // botões menores que uma área confortável de toque.
    // Respeita dimensões compactas explícitas (ícones, +/- e controles numéricos).
    // O piso global anterior de 96.dp transformava botões pedidos com 40–76.dp
    // em caixas de 96.dp e fazia Rows de celular disputarem espaço desnecessariamente.
    // Para rótulos comuns o tamanho nominal continua vindo de InkButtonSize; apenas
    // customizações explícitas podem descer até a área de toque mínima de 48.dp.
    val width = customWidth?.coerceAtLeast(48.dp) ?: size.width
    val height = customHeight?.coerceAtLeast(48.dp) ?: size.height

    Box(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth().height(height) else Modifier.widthIn(min = 48.dp, max = width).height(height))
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // Fundo de segurança: o controle continua visível mesmo quando
            // a pincelada SVG é parcialmente recortada em layouts compactos.
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (enabled) resolvedGradient.start else InkColors.disabledGray,
                        if (enabled) resolvedGradient.mid else InkColors.disabledGray,
                        if (enabled) resolvedGradient.end else InkColors.disabledGray
                    )
                ),
                androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                brush = styleOverride.outlineColor?.let { outline ->
                    Brush.horizontalGradient(listOf(outline, outline, outline))
                } ?: Brush.horizontalGradient(
                    if (enabled) {
                        listOf(resolvedGradient.start, resolvedGradient.mid, resolvedGradient.end)
                    } else {
                        listOf(resolvedVisual.outline, resolvedVisual.accent, resolvedVisual.outline)
                    }
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .feedbackClickable(
                interactionSource = interactionSource,
                indication = null, // o próprio scale + gradiente já dá o feedback; ripple padrão ficaria estranho por cima da pincelada
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // A geometria primária do botão é o Box com gradiente/borda acima.
        // O antigo Canvas/SVG foi removido deste caminho principal porque sua
        // a antiga cadeia de recorte e transformações já havia causado interferência entre
        // controles estreitos. A decoração não participa mais de constraints,
        // hit-testing ou posicionamento do rótulo.

        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
        // Acabamento padrão: contorno + preenchimento, igual à tipografia
        // temática usada no restante do aplicativo. O contorno fica abaixo
        // do preenchimento para manter a leitura sobre qualquer pincelada.
        val effectiveTextColor = textColor
        // O InkButton é usado em larguras muito diferentes (inclusive em Rows com weight).
        // O padding antigo de 28.dp por lado consumia quase toda a largura dos botões
        // compactos e cortava rótulos como "Automático". Mantemos uma margem segura e
        // reduzimos a fonte somente quando o layout realmente informa overflow.
        var measuredTextWidthPx by remember { mutableStateOf(0) }
        // Small controls frequently share a narrow Row on phones. Reserve
        // less horizontal padding for those labels while keeping the larger
        // button sizes comfortably inset from the painted brush edge.
        val labelHorizontalPadding = if (size == InkButtonSize.Small || customWidth != null) 4.dp else 8.dp
        val textModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = labelHorizontalPadding, vertical = 2.dp)
            .onSizeChanged { measuredTextWidthPx = it.width }
        val baseFontSize = size.fontSize
        // Reset the fit when the measured width changes (e.g. a weighted Row
        // expands), rather than only when the requested nominal size changes.
        var fittedFontSize by remember(label, baseFontSize, measuredTextWidthPx) {
            mutableStateOf(baseFontSize)
        }
        val textStyle = androidx.compose.ui.text.TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = fittedFontSize,
            lineHeight = fittedFontSize * 1.08f,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (content != null) {
            CompositionLocalProvider(LocalContentColor provides effectiveTextColor) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
            }
        } else Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = effectiveTextColor,
                style = textStyle.copy(drawStyle = Fill),
                maxLines = 2,
                overflow = TextOverflow.Clip,
                softWrap = true,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow && fittedFontSize.value > 10f) {
                        fittedFontSize = (fittedFontSize.value - 1f).coerceAtLeast(10f).sp
                    }
                },
                modifier = textModifier,
            )
        }
    }
}

/**
 * Sobrecarga temática para antigos IconButton do Material 3.
 * Mantém o conteúdo/ícone e a semântica do clique, mas usa a pincelada InkButton.
 */
@Composable
fun InkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    InkButton(
        label = "",
        onClick = onClick,
        modifier = modifier,
        variant = InkButtonVariant.Secondary,
        size = InkButtonSize.Small,
        enabled = enabled,
        customWidth = 52.dp,
        customHeight = 52.dp,
        content = content,
    )
}

/** Variante "ghost": texto + traço geométrico simples; nenhum Canvas/SVG. */
@Composable
private fun InkGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    styleOverride: InkButtonStyle = InkButtonStyle(),
    visualTemplate: ExaltVisualTemplate = LocalExaltedVisualTemplate.current,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textColor = styleOverride.labelColor ?: visualTemplate.onSurface
    val underlineMid = styleOverride.gradientMiddle ?: visualTemplate.accent

    Column(
        modifier = modifier
            .wrapContentSize()
            .feedbackClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 74.dp, height = 2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            underlineMid.copy(alpha = if (enabled) 0.85f else 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

