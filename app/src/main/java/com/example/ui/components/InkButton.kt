package com.example.ui.components

import androidx.compose.ui.draw.drawBehind

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable

/*
 * Botão "pincelada de tinta" — implementação Jetpack Compose.
 *
 * Isto é a tradução direta, em Compose, do protótipo HTML/CSS/SVG que foi
 * aprovado (mesmo path 'd', mesmos stops de gradiente, mesmos tamanhos e
 * cores). O documento HTML original serve só de referência visual — este
 * arquivo é o que de fato entra no projeto.
 *
 * Como funciona:
 *  - Cada pincelada é um path de SVG (string 'd'), igual ao usado no HTML.
 *    Em vez de converter as coordenadas na mão, usamos o PathParser do
 *    próprio Compose (androidx.compose.ui.graphics.vector.PathParser), que
 *    entende a mesma sintaxe de path do SVG/Vector Drawable — então dá pra
 *    colar o 'd' do design direto, sem risco de erro de tradução manual.
 *  - O botão desenha esse path num Canvas, escalado pro tamanho do botão,
 *    preenchido com um gradiente horizontal (Brush.horizontalGradient) que
 *    imita a tinta ficando mais clara/seca da esquerda pra direita.
 *  - O texto é um Text comum, centralizado por cima — sem trade-off de
 *    acessibilidade (leitor de tela lê o texto normalmente).
 *  - Feedback de toque: escala levemente pra baixo quando pressionado
 *    (equivalente ao :active do CSS). Compose para toque não tem ":hover"
 *    de mouse por padrão — se o app precisar de hover (ex.: uso com
 *    teclado/mouse em tablet ou Chromebook), dá pra ligar via
 *    Modifier.hoverable() e observar collectIsHoveredAsState(); deixei o
 *    gancho comentado mais abaixo pra não obrigar essa dependência.
 *
 * Requer: androidx.compose.ui:ui-graphics (PathParser já vem nela).
 * Testado apenas por leitura/revisão de API — este ambiente não tem Gradle
 * com acesso à rede pra compilar de verdade, então revisem antes de
 * mergear (nomes de API foram conferidos contra a documentação oficial do
 * Compose, mas sempre vale um build local).
 */

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedMetalGoldCore
import com.example.ui.theme.ExaltedMetalGoldDeep
import com.example.ui.theme.ExaltedMetalGoldFlash
import com.example.ui.theme.ExaltedMetalGoldShine
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedTextStroke
import com.example.ui.theme.Dimens
import com.example.ui.theme.corTextoContraste
import com.example.ui.theme.ExaltVisualTemplate

// ---------------------------------------------------------------------
// 1. Dados da pincelada — mesmos 3 paths do SVG (viewBox 0 0 340 100),
//    colados sem alteração.
// ---------------------------------------------------------------------

private const val VIEWBOX_W = 340f
private const val VIEWBOX_H = 100f

private object InkBrushPaths {
    const val A = "M8,52 C6,48 10,46 8,42 C14,38 10,34 18,32 C24,28 30,32 36,27 " +
        "C44,24 50,29 58,25 C66,22 74,26 84,23 C94,20 104,25 116,22 C128,19 140,24 154,21 " +
        "C168,19 182,23 196,20 C210,18 224,22 238,20 C250,18 262,21 272,19 C280,21 288,17 294,20 " +
        "C300,17 306,21 303,25 C309,28 305,32 310,35 C306,39 311,43 307,47 C312,51 307,55 311,58 " +
        "C307,62 312,65 307,68 C310,72 304,74 300,71 C296,76 289,73 285,77 C275,74 268,79 258,76 " +
        "C246,80 234,75 222,78 C208,80 194,76 180,79 C166,81 152,77 138,80 C124,82 110,78 96,81 " +
        "C82,83 68,79 56,81 C46,83 38,78 30,80 C24,76 17,79 14,74 C10,76 6,71 9,67 " +
        "C4,64 8,60 5,56 C9,54 6,50 8,52 Z"

    const val B = "M9,48 C7,44 11,41 8,37 C15,34 11,30 19,28 C26,25 32,29 39,25 " +
        "C48,22 55,27 63,23 C72,21 81,25 92,22 C103,19 114,24 127,21 C140,19 153,23 167,20 " +
        "C181,18 195,22 209,19 C221,18 233,21 244,19 C253,21 261,18 268,20 C275,17 282,21 288,18 " +
        "C294,21 300,25 297,29 C302,32 299,37 303,41 C299,45 303,49 300,53 C304,57 300,61 303,65 " +
        "C299,68 302,72 297,74 C300,78 294,80 290,76 C286,80 279,76 275,80 C266,76 258,81 249,77 " +
        "C238,81 226,76 214,79 C200,81 186,77 172,80 C158,82 144,77 130,80 C116,82 102,78 88,80 " +
        "C76,82 65,78 55,80 C48,82 40,78 34,80 C29,76 22,79 19,74 C14,76 9,72 12,67 " +
        "C7,64 10,60 7,56 C11,54 8,50 9,48 Z"

    const val C = "M7,55 C5,51 9,47 7,43 C13,40 9,36 17,33 C25,30 31,34 39,30 " +
        "C48,27 56,31 65,27 C75,24 85,28 97,25 C109,22 121,27 135,24 C149,21 163,25 178,22 " +
        "C193,20 208,24 222,21 C234,19 246,22 256,20 C264,22 272,19 279,21 C286,18 293,22 289,26 " +
        "C296,29 292,33 296,37 C292,41 296,45 292,49 C297,53 292,58 296,62 C291,65 295,69 290,72 " +
        "C293,76 287,78 283,74 C279,78 272,74 268,78 C258,75 249,80 239,76 C227,80 214,75 201,78 " +
        "C186,80 171,75 156,78 C141,80 126,76 111,79 C97,81 84,77 71,79 C60,81 50,77 41,79 " +
        "C35,75 28,78 24,73 C19,76 13,71 16,66 C11,63 14,58 11,54 C15,53 11,56 7,55 Z"

    /** Traço fino pra variante Ghost (viewBox 0 0 320 20). */
    const val UNDERLINE = "M0,10 C4,4 10,2 20,3 C60,5 140,6 200,7 C224,7.5 236,9 244,11 " +
        "L252,8 L246,13 L253,15 C244,18 220,16 190,17 C130,18.5 60,17 22,15 C8,14 2,15 0,10 Z"

    val all = listOf(A, B, C)
}

/** Faz cache do Path já parseado — parsear string toda recomposição seria desperdício. */
private val parsedPathCache = HashMap<String, Path>()

private fun svgPath(d: String): Path =
    parsedPathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

// ---------------------------------------------------------------------
// 2. Cores — mesmos hex do <linearGradient> do SVG (0% / 55% / 100%).
// ---------------------------------------------------------------------

private data class InkGradient(val start: Color, val mid: Color, val end: Color)

private object InkColors {
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // A pincelada acompanha a paleta do tipo de Exaltado ativo:
    // Solar = dourado, Sangue de Dragão = vermelho, Lunar = prata.
    // Getters são intencionais: a paleta pode mudar em runtime.
    val primary: InkGradient
        get() = InkGradient(
            ExaltedDarkSurface,
            ExaltedMetalGoldDeep,
            ExaltedMetalGoldCore
        )

    val secondary: InkGradient
        get() = InkGradient(
            ExaltedDarkSurfaceVariant,
            ExaltedDarkSurface,
            ExaltedMetalGoldDeep
        )

    val selected: InkGradient
        get() = InkGradient(
            ExaltedMetalGoldCore,
            ExaltedMetalGoldShine,
            ExaltedMetalGoldFlash
        )

    // Ação destrutiva tradicional: vermelho fixo, para diálogos/ações que
    // precisam manter a semântica universal de perigo.
    val danger = InkGradient(Color(0xFF4C150D), Color(0xFF7A241A), Color(0xFFA6503F))

    // Ação destrutiva temática: preserva a intenção de "Danger", mas acompanha
    // a paleta visual do Exaltado ativo (Solar = dourado, Sangue de Dragão =
    // vermelho, Lunar = prata). Usado quando a identidade visual da seção
    // deve permanecer coerente com a planilha aberta.
    val thematicDanger: InkGradient
        get() = InkGradient(
            ExaltedMetalGoldDeep,
            ExaltedMetalGoldShine,
            ExaltedMetalGoldFlash
        )
    val onDark = InkGradient(Color(0xFFFFFAF0), Color(0xFFF2ECE0), Color(0xFFC9C0AC))
    val textOnLight = ExaltedOnSurface
    val textOnDark = Color(0xFF221D16)
    val textBody = Color(0xFF2A2620)
    val disabledGray = Color(0xFF8A8578)
}

private fun InkGradient.toBrush(widthPx: Float): Brush = Brush.horizontalGradient(
    colorStops = arrayOf(0f to start, 0.55f to mid, 1f to end),
    startX = 0f,
    endX = widthPx
)

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
    // Mantém a proporção 340:100 do viewBox — não distorce a pincelada.
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
 * Botão com fundo em pincelada de tinta (gradiente esquerda→direita
 * simulando a tinta secando) e rótulo centralizado.
 *
 * @param brushIndex escolhe entre as 3 pinceladas (0, 1 ou 2). Varie esse
 *   índice quando dois botões desse estilo aparecerem juntos na mesma
 *   tela, pra não parecer o mesmo carimbo copiado — mesma lógica do
 *   protótipo HTML.
 * @param label rótulo curto (1–3 palavras); textos longos truncam com "…".
 * @param selected destaca o botão com a cor temática do Exaltado ativo — usado nas pílulas de
 *   seleção (Habilidade/Atributo, Méritos etc.), equivalente ao
 *   isSelected=true do ErgonomicMultiWordButton antigo. Ignorado quando
 *   variant = Ghost (o traço fino não tem estado selecionado).
 * @param customWidth / @param customHeight sobrescrevem o tamanho fixo de
 *   [size] — usado nas grades de pílulas pequenas (Dimens.PillMinWidth/
 *   Height), que não têm a mesma proporção 340:100 das pinceladas largas.
 *   A arte estica pra caber (preserveAspectRatio "none", igual ao SVG
 *   original) — numa caixa bem menor/mais quadrada que o desenho original,
 *   a pincelada fica mais compacta/reta, não idêntica ao botão grande.
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
    if (variant == InkButtonVariant.Ghost) {
        InkGhostButton(label, onClick, modifier, enabled, styleOverride)
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

    // Aba 11 pode fornecer a paleta do próprio NPC. Sem ela, preservamos
    // exatamente o comportamento histórico baseado no tema global da ficha.
    val ownerPrimary = visualTemplate?.let {
        InkGradient(it.surface, it.metalDeep, it.accent)
    }
    val ownerSecondary = visualTemplate?.let {
        InkGradient(it.surfaceVariant, it.surface, it.metalDeep)
    }
    val ownerSelected = visualTemplate?.let {
        InkGradient(it.accent, it.metalShine, it.metalFlash)
    }
    val gradient = when {
        selected -> ownerSelected ?: InkColors.selected
        variant == InkButtonVariant.Primary -> ownerPrimary ?: InkColors.primary
        variant == InkButtonVariant.Secondary -> ownerSecondary ?: InkColors.secondary
        variant == InkButtonVariant.Danger -> InkColors.danger
        variant == InkButtonVariant.ThematicDanger -> ownerPrimary ?: InkColors.thematicDanger
        variant == InkButtonVariant.OnDark -> InkColors.onDark
        else -> ownerPrimary ?: InkColors.primary // Ghost não chega aqui (early return acima)
    }
    val resolvedGradient = InkGradient(
        styleOverride.gradientStart ?: gradient.start,
        styleOverride.gradientMiddle ?: gradient.mid,
        styleOverride.gradientEnd ?: gradient.end,
    )
    val textColor = styleOverride.labelColor ?: when {
        visualTemplate != null && selected -> corTextoContraste(visualTemplate.metalShine)
        visualTemplate != null && variant != InkButtonVariant.Danger -> visualTemplate.onSurface
        selected -> corTextoContraste(InkColors.selected.mid)
        variant == InkButtonVariant.OnDark -> InkColors.textOnDark
        else -> InkColors.textOnLight
    }
    val path = remember(brushIndex) { svgPath(InkBrushPaths.all[brushIndex.coerceIn(0, 2)]) }
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
            .then(if (fillMaxWidth) Modifier.fillMaxWidth().height(height) else Modifier.widthIn(max = width).height(height))
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .feedbackClickable(
                interactionSource = interactionSource,
                indication = null, // o próprio scale + gradiente já dá o feedback; ripple padrão ficaria estranho por cima da pincelada
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = if (fillMaxWidth) {
                Modifier.fillMaxWidth().height(height)
            } else {
                Modifier.size(width, height)
            }
        ) {
            // Em botões estreitos (Rows com weight no telefone), não deformamos a
            // pincelada horizontalmente. Escala uniforme pela altura e recorte central
            // preservam a identidade visual mesmo quando a largura disponível diminui.
            val uniformScale = this.size.height / VIEWBOX_H
            val translatedX = (this.size.width - VIEWBOX_W * uniformScale) / 2f
            val brush = if (enabled) {
                resolvedGradient.toBrush(this.size.width)
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        InkColors.disabledGray.copy(alpha = 0.5f),
                        InkColors.disabledGray.copy(alpha = 0.35f),
                    ),
                    startX = 0f,
                    endX = this.size.width,
                )
            }
            // O desenho da pincelada pode ser mais largo que o espaço realmente
            // concedido pelo Row/FlowRow (especialmente em telas de telefone).
            // Sem recorte, o Canvas do botão desenhava sobre os irmãos e cobria
            // inclusive seus rótulos. O recorte torna o limite visual do botão
            // idêntico ao limite medido pelo layout, globalmente.
            clipRect {
                translate(left = translatedX) {
                    scale(uniformScale, uniformScale, pivot = Offset.Zero) {
                        drawPath(path = path, brush = brush)
                    }
                }
            }
        }
        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
        // Acabamento padrão: contorno + preenchimento, igual à tipografia
        // temática usada no restante do aplicativo. O contorno fica abaixo
        // do preenchimento para manter a leitura sobre qualquer pincelada.
        val effectiveTextColor = if (enabled) textColor else textColor.copy(alpha = 0.55f)
        // O InkButton é usado em larguras muito diferentes (inclusive em Rows com weight).
        // O padding antigo de 28.dp por lado consumia quase toda a largura dos botões
        // compactos e cortava rótulos como "Automático". Mantemos uma margem segura e
        // reduzimos a fonte somente quando o layout realmente informa overflow.
        var measuredTextWidthPx by remember { mutableStateOf(0) }
        val textModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .onSizeChanged { measuredTextWidthPx = it.width }
        val baseFontSize = size.fontSize
        // Reset the fit when the measured width changes (e.g. a weighted Row
        // expands), rather than only when the requested nominal size changes.
        var fittedFontSize by remember(label, baseFontSize, measuredTextWidthPx) {
            mutableStateOf(baseFontSize)
        }
        val textStyle = androidx.compose.ui.text.TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = fittedFontSize,
            lineHeight = fittedFontSize * 1.08f,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (content != null) {
            Box(contentAlignment = Alignment.Center) { content() }
        } else Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = (styleOverride.outlineColor ?: ExaltedTextStroke).copy(alpha = if (enabled) 1f else 0.55f),
                style = textStyle.copy(drawStyle = Stroke(width = Dimens.TextStrokeWidth.value)),
                maxLines = 2,
                overflow = TextOverflow.Clip,
                softWrap = true,
                modifier = textModifier,
            )
            Text(
                text = label,
                color = effectiveTextColor,
                style = textStyle.copy(drawStyle = Fill),
                maxLines = 2,
                overflow = TextOverflow.Clip,
                softWrap = true,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow && fittedFontSize.value > 8f) {
                        fittedFontSize = (fittedFontSize.value - 1f).coerceAtLeast(8f).sp
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

/** Variante "ghost": sem massa de tinta atrás, só um traço fino sob o texto. */
@Composable
private fun InkGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    styleOverride: InkButtonStyle = InkButtonStyle(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val underlinePath = remember { svgPath(InkBrushPaths.UNDERLINE) }
    val textColor = styleOverride.labelColor ?: InkColors.textBody

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
        Canvas(modifier = Modifier.size(width = 74.dp, height = 12.dp)) {
            val scaleX = this.size.width / 320f
            val scaleY = this.size.height / 20f
            scale(scaleX, scaleY, pivot = Offset.Zero) {
                drawPath(
                    path = underlinePath,
                    color = (styleOverride.gradientMiddle ?: InkColors.primary.mid).copy(alpha = if (enabled) 0.85f else 0.4f),
                )
            }
        }
    }
}

