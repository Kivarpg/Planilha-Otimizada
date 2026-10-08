package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// EXALTED VISUAL BLUEPRINTS
// ----------------------------------------------------------------------------
// Contrato visual para a integração de novos Exaltados.
//
// Este arquivo é uma ESPECIFICAÇÃO DE SKIN, não contém regras de negócio nem
// cria campos, abas ou nomes. Novos Exaltados devem fornecer somente uma nova
// instância de ExaltVisualBlueprint e reutilizar os mesmos componentes.
//
// REGRAS INVARIÁVEIS
// 1. Nenhum campo existente pode ser criado, removido ou renomeado.
// 2. Nenhum nome existente pode ser alterado.
// 3. A identidade do novo Exaltado deve mudar pela skin, não pela estrutura.
// 4. Componentes interativos mantêm seu comportamento; a skin só define a
//    apresentação, estados visuais e feedback.
// ============================================================================

/** Paleta e acabamento visual de um Tipo de Exaltado. */
data class ExaltVisualPalette(
    val primary: Color,
    val primaryBright: Color,
    val primaryPale: Color,
    val background: Color,
    val backgroundGlow: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val divider: Color,
    val metalDeep: Color,
    val metalCore: Color,
    val metalShine: Color,
    val metalFlash: Color,
    val disabled: Color
)

/** Especificação de forma, escala e estados dos botões. */
data class ExaltButtonBlueprint(
    val minHeight: Dp = 48.dp,
    val horizontalPadding: Dp = 14.dp,
    val verticalPadding: Dp = 8.dp,
    val cornerRadius: Dp = 7.dp,
    val borderWidth: Dp = 1.5.dp,
    val pressedScale: Float = 0.97f,
    val normalAlpha: Float = 1f,
    val pressedAlpha: Float = 0.92f,
    val disabledAlpha: Float = 0.45f,
    val labelSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    val labelLetterSpacing: androidx.compose.ui.unit.TextUnit = 0.45.sp
)

/** Contrato dos efeitos de interação. */
data class ExaltInteractionBlueprint(
    val pressAnimationMs: Int = 110,
    val releaseAnimationMs: Int = 150,
    val selectionAnimationMs: Int = 180,
    val screenTransitionMs: Int = 220,
    val glowAlphaNormal: Float = 0.08f,
    val glowAlphaPressed: Float = 0.22f,
    val glowAlphaSelected: Float = 0.30f,
    val enableRipple: Boolean = true,
    val enableHapticOnPrimaryAction: Boolean = true
)

/** Diretrizes para qualquer arte, emblema ou ilustração futura. */
data class ExaltIllustrationBlueprint(
    val lineWidthThin: Dp = 1.dp,
    val lineWidthStructural: Dp = 1.5.dp,
    val lineWidthHero: Dp = 2.dp,
    val cornerRadius: Dp = 7.dp,
    val heroAspectRatio: Float = 1f,
    val detailLevel: Int = 4,
    val maxAccentColors: Int = 3,
    val preserveSilhouette: Boolean = true,
    val allowPhotorealism: Boolean = false,
    val useFlatBackground: Boolean = false
)

data class ExaltVisualBlueprint(
    val id: String,
    val palette: ExaltVisualPalette,
    val button: ExaltButtonBlueprint = ExaltButtonBlueprint(),
    val interaction: ExaltInteractionBlueprint = ExaltInteractionBlueprint(),
    val illustration: ExaltIllustrationBlueprint = ExaltIllustrationBlueprint()
)

object ExaltedVisualBlueprints {
    // SOLAR — ouro/âmbar, luz quente, metal nobre.
    val Solar = ExaltVisualBlueprint(
        id = "solar",
        palette = ExaltVisualPalette(
            primary = Color(0xFFF2C230),
            primaryBright = Color(0xFFFFE06A),
            primaryPale = Color(0xFFFFD978),
            background = Color(0xFF0D0B0A),
            backgroundGlow = Color(0xFF2A2115),
            surface = Color(0xFF12100F),
            surfaceRaised = Color(0xFF171412),
            surfaceVariant = Color(0xFF1C1815),
            outline = Color(0xFFC99A24),
            divider = Color(0xFF3D2E10),
            metalDeep = Color(0xFF4A3D22),
            metalCore = Color(0xFFC99A24),
            metalShine = Color(0xFFFFD34E),
            metalFlash = Color(0xFFFFED9A),
            disabled = Color(0xFF665F52)
        )
    )

    // SANGUE DE DRAGÃO — vermelho mineral/ígneo sobre obsidiana.
    val SangueDeDragao = ExaltVisualBlueprint(
        id = "sangue_de_dragao",
        palette = ExaltVisualPalette(
            primary = Color(0xFFD52D3A),
            primaryBright = Color(0xFFFF5360),
            primaryPale = Color(0xFFD96A73),
            background = Color(0xFF0D0A0A),
            backgroundGlow = Color(0xFF2B1719),
            surface = Color(0xFF121010),
            surfaceRaised = Color(0xFF171313),
            surfaceVariant = Color(0xFF1C1717),
            outline = Color(0xFF7D2F38),
            divider = Color(0xFF5C1A1D),
            metalDeep = Color(0xFF4A1416),
            metalCore = Color(0xFF7D2530),
            metalShine = Color(0xFFD13A49),
            metalFlash = Color(0xFFF05A67),
            disabled = Color(0xFF665454)
        )
    )

    // LUNAR — prata fria, azul de profundidade e contraste lunar.
    val Lunar = ExaltVisualBlueprint(
        id = "lunar",
        palette = ExaltVisualPalette(
            primary = Color(0xFFBFD9FF),
            primaryBright = Color(0xFFF2F7FF),
            primaryPale = Color(0xFFC9D8EA),
            background = Color(0xFF0A0C0E),
            backgroundGlow = Color(0xFF17222D),
            surface = Color(0xFF101316),
            surfaceRaised = Color(0xFF14181C),
            surfaceVariant = Color(0xFF181D22),
            outline = Color(0xFF779BC7),
            divider = Color(0xFF405D7E),
            metalDeep = Color(0xFF3A3A3A),
            metalCore = Color(0xFF779BC7),
            metalShine = Color(0xFFD8E8FA),
            metalFlash = Color(0xFFFFFFFF),
            disabled = Color(0xFF60646A)
        )
    )
}

