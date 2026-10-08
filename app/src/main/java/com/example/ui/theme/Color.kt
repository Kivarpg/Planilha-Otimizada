package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.example.model.isDragonBlooded
import com.example.model.isLunar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// SKIN — Color.kt
// ----------------------------------------------------------------------------
// TODA cor visível no app está definida aqui — nenhum componente deveria ter
// uma cor "solta" (hex direto) fora deste arquivo. Para mudar a paleta
// (deixar o dourado mais claro/escuro, trocar o tom do void, etc.), edite
// só estas constantes; nada nos arquivos de components/ (DialogComponents,
// RatingControls, ButtonComponents, CardComponents, TextComponents,
// DecorativeComponents) ou nas Abas precisa mudar, já que todos referenciam
// estes nomes.
// ----------------------------------------------------------------------------
// ExaltedSkin — identidade visual reconstruída: void arquitetônico, metal nobre e acentos específicos por Tipo de Exaltado.
// Paleta oficial: void + onyx + ouro metálico épico
// (sem motivo de sol nas abas — apenas metal dourado)
// Tons calibrados por amostragem de pixel da referência visual
// "10_mockup_pdnva.jpg" (moldura ondulada dourada + título com flourish) —
// ênfase no caráter metálico, tom mais quente/âmbar que a rodada anterior.
// ============================================================================

// --- Fundos (void / onyx) ---
val ExaltedDarkBackground: Color get() = exaltedBackdropCoreState
val ExaltedOnyxDeep: Color get() = exaltedBackdropCoreState
val ExaltedDarkSurface: Color get() = exaltedSurfaceState
val ExaltedDarkSurfaceVariant: Color get() = exaltedSurfaceVariantState
private var exaltedSurfaceState by mutableStateOf(Color(0xFF080706))
private var exaltedSurfaceVariantState by mutableStateOf(Color(0xFF100E0A))
val ExaltedBlack              = Color(0xFF020201)

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Texto normal e texto de botões: preenchimento branco + contorno preto.
val ExaltedTextFill            = Color.White
val ExaltedTextStroke          = Color.Black


// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Template visual independente do contexto da planilha. Usado pela Aba 11 para
// que cada NPC carregue a paleta do seu próprio tipo de Exaltado.
enum class ExaltedVisualMotif { SOLAR, DRAGON_BLOODED, LUNAR }

val ExaltedActiveMotif: ExaltedVisualMotif get() = exaltedActiveMotifState
private var exaltedActiveMotifState by mutableStateOf(ExaltedVisualMotif.SOLAR)

// Metal estrutural segue a mesma fonte de verdade dos blueprints oficiais.
// Isso impede que componentes estruturais reintroduzam cores antigas (como
// dourado em Sangue de Dragão) fora da identidade aprovada de cada tipo.
private val activeBlueprintPalette: ExaltVisualPalette
    get() = when (ExaltedActiveMotif) {
        ExaltedVisualMotif.SOLAR -> ExaltedVisualBlueprints.Solar.palette
        ExaltedVisualMotif.DRAGON_BLOODED -> ExaltedVisualBlueprints.SangueDeDragao.palette
        ExaltedVisualMotif.LUNAR -> ExaltedVisualBlueprints.Lunar.palette
    }

val ExaltedStructuralMetalDeep: Color get() = activeBlueprintPalette.metalDeep
val ExaltedStructuralMetal: Color get() = activeBlueprintPalette.metalCore
val ExaltedStructuralMetalShine: Color get() = activeBlueprintPalette.metalShine

data class ExaltVisualTemplate(
    val accent: Color,
    val accentBright: Color,
    val gold: Color,
    val muted: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val surface: Color,
    val metalDeep: Color,
    val metalShine: Color,
    val metalFlash: Color,
    val outline: Color,
    val divider: Color
)

fun visualTemplateParaExaltado(tipo: com.example.model.TipoExaltadoEncontro): ExaltVisualTemplate {
    // A Aba 11 usa exatamente a mesma fonte de verdade visual do restante do app.
    // Isso evita uma segunda paleta manual que fazia NPCs parecerem pertencer a
    // uma skin diferente da ficha ativa.
    val palette = when (tipo) {
        com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> ExaltedVisualBlueprints.SangueDeDragao.palette
        com.example.model.TipoExaltadoEncontro.LUNAR -> ExaltedVisualBlueprints.Lunar.palette
        else -> ExaltedVisualBlueprints.Solar.palette
    }
    return ExaltVisualTemplate(
        accent = palette.primary,
        accentBright = palette.primaryBright,
        gold = palette.primary,
        muted = palette.primaryPale,
        onSurface = ExaltedOnSurface,
        surfaceVariant = palette.surfaceVariant,
        surface = palette.surface,
        metalDeep = palette.metalDeep,
        metalShine = palette.metalShine,
        metalFlash = palette.metalFlash,
        outline = palette.outline,
        divider = palette.divider
    )
}

// --- Ouros (metálico + solar) — degradê de bronze profundo a brilho quente.
// Calibrado para remeter a OURO METÁLICO (mais saturado, mais quente,
// menos "amarelo pastel") — sem perder contraste de leitura sobre o void. ---
val ExaltedAmber: Color get() = exaltedAmberState
val ExaltedAmberVariant: Color get() = exaltedAmberVariantState
val ExaltedGold: Color get() = exaltedGoldState
// Acompanha a paleta ativa (Solar=dourado, Lunar=prata, DB=vermelho).
// Antes era constante dourada fixa — em planilha Lunar vários botões/ícones
// continuavam dourados mesmo com aplicarPaletaPorTemplate("Lunar").
val ExaltedGoldBright: Color get() = exaltedGoldBrightState
val ExaltedAccentBright: Color get() = exaltedGoldBrightState
private var exaltedAmberState by mutableStateOf(Color(0xFFF0C75E))
private var exaltedAmberVariantState by mutableStateOf(Color(0xFF5C4A28))
private var exaltedGoldState by mutableStateOf(Color(0xFFF0C75E))
private var exaltedGoldBrightState by mutableStateOf(Color(0xFFFFD978))
private var exaltedGoldPaleState by mutableStateOf(Color(0xFFF3D79A))

// ExaltedGoldBright é reservado ao Solar. A interface compartilhada usa ExaltedAccentBright.
// Paleta padrão (dourado metálico, Solar) vs. Sangue de Dragão (vermelho
// sangue, tom médio "padrão da indústria" — nem muito escuro nem muito
// vibrante, mantendo o mesmo contraste de leitura sobre o void que o
// dourado já tinha). Chamada uma vez em MainSheetScreen (LaunchedEffect),
// ao entrar com o template escolhido — cosmético apenas: nenhum ícone,
// comportamento ou funcionalidade muda, só estes 5 tons.
fun aplicarPaletaPorTemplate(tipoPersonagem: String) {
    // Fonte única da identidade gráfica: o blueprint do Tipo de Exaltado.
    // MaterialTheme e os tokens Exalted* passam a consumir a mesma paleta.
    val palette = when {
        tipoPersonagem.isDragonBlooded() -> {
            exaltedActiveMotifState = ExaltedVisualMotif.DRAGON_BLOODED
            ExaltedVisualBlueprints.SangueDeDragao.palette
        }
        tipoPersonagem.isLunar() -> {
            exaltedActiveMotifState = ExaltedVisualMotif.LUNAR
            ExaltedVisualBlueprints.Lunar.palette
        }
        else -> {
            exaltedActiveMotifState = ExaltedVisualMotif.SOLAR
            ExaltedVisualBlueprints.Solar.palette
        }
    }
    exaltedAmberState = palette.primary
    exaltedAmberVariantState = palette.metalDeep
    exaltedGoldState = palette.primary
    exaltedGoldBrightState = palette.primaryBright
    exaltedGoldPaleState = palette.primaryPale
    exaltedBackdropCoreState = palette.background
    exaltedBackdropGlowState = palette.backgroundGlow
    exaltedSurfaceRaisedState = palette.surfaceRaised
    exaltedSurfaceState = palette.surface
    exaltedSurfaceVariantState = palette.surfaceVariant
    exaltedOutlineState = palette.outline
    exaltedDividerState = palette.divider
    exaltedMetalRedDeepState = palette.metalDeep
    exaltedMetalRedCoreState = palette.metalCore
    exaltedMetalRedShineState = palette.metalShine
    exaltedMetalRedFlashState = palette.metalFlash
}
val ExaltedMetalGoldDeep: Color get() = exaltedMetalRedDeepState
val ExaltedMetalGoldCore: Color get() = exaltedMetalRedCoreState
val ExaltedMetalGoldShine: Color get() = exaltedMetalRedShineState
val ExaltedMetalGoldFlash: Color get() = exaltedMetalRedFlashState
private var exaltedMetalRedDeepState by mutableStateOf(Color(0xFF4A3D22))
private var exaltedMetalRedCoreState by mutableStateOf(Color(0xFF97793A))
private var exaltedMetalRedShineState by mutableStateOf(Color(0xFFFFD978))
private var exaltedMetalRedFlashState by mutableStateOf(Color(0xFFEDC873))

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Camadas de fundo dinâmicas: a identidade Solar/Sangue de Dragão/Lunar agora
// percorre a tela inteira, sem criar nenhum elemento funcional novo.
val ExaltedBackdropCore: Color get() = exaltedBackdropCoreState
val ExaltedBackdropGlow: Color get() = exaltedBackdropGlowState
private var exaltedBackdropCoreState by mutableStateOf(Color(0xFF030303))
private var exaltedBackdropGlowState by mutableStateOf(Color(0xFF6D5217))
private var exaltedSurfaceRaisedState by mutableStateOf(Color(0xFF11100D))

// --- Neutro dourado para botões +/- (sem semântica vermelho/verde) ---
val ExaltedStepperNeutral: Color get() = ExaltedAmber

// --- Texto e ícones ---
// ExaltedOnPrimary: usado como onPrimary/onSecondary/onTertiary no
// MaterialTheme (Theme.kt) — texto/ícone padrão de qualquer botão ou
// superfície Material3 que não sobrescreva contentColor explicitamente.
// primary/secondary/tertiary são todos ExaltedAmber (a cor da área ativa),
// então o texto por cima não pode ser outro tom da MESMA área — ficaria
// parecido demais com o próprio fundo (ver corTextoContraste acima).
// Usa ExaltedOnSurface, a cor neutra dos menus/barra de rolagem: nenhuma
// cor de fonte do app pode ser preta, e ela tem que ser visivelmente
// diferente do botão em que está escrita.
val ExaltedOnSurface          = Color(0xFFFFFBF2)
val ExaltedOnPrimary: Color get() = ExaltedOnSurface
val ExaltedOnBackground       = Color(0xFFFFFDF8)
val ExaltedMuted              = Color(0xFFC8C1B5)
val ExaltedOutline: Color get() = exaltedOutlineState
private var exaltedOutlineState by mutableStateOf(Color(0xFF97793A))
val ExaltedDivider: Color get() = exaltedDividerState
private var exaltedDividerState by mutableStateOf(Color(0xFF3D2E10))

// --- Estados (mantidos para uso pontual fora dos botões +/-, ex.: mensagens
// de erro/sucesso) ---
val ExaltedError              = Color(0xFFFF756B)
// Família "perigo" — mesmo padrão de 3 tons (sombra/núcleo/brilho) da
// família metálica dourada, usada nos botões destrutivos dos pop-ups
// (REMOVER, EXCLUIR etc.) pra ter o mesmo acabamento gradiente/borda, não
// uma cor sólida lisa.
val ExaltedDangerCore         = Color(0xFFFF756B)
val ExaltedSuccess            = Color(0xFF70D69A)

// --- Willpower / destaques especiais ---
// Mesmos valores de ExaltedGoldBright/ExaltedMetalGoldFlash — viram
// alias em vez de constantes fixas próprias, pra também acompanhar a
// paleta ativa (achado ao revisar "toda cor amarela deve virar
// vermelho" — Força de Vontade é universal, aparece nos dois templates).

// --- Consolidadas aqui em 2026-08: estavam soltas (hex direto) em 6
// arquivos de aba/componente diferentes, violando a regra deste arquivo
// ("nenhum componente deveria ter cor solta fora daqui"). Valores
// preservados exatamente como estavam — só a localização mudou, pra
// facilitar ajuste de paleta num lugar só.
val ExaltedStatusCompleto      = Color(0xFF66D17A) // "Planilha completa" (ícone de status)
val ExaltedStatusIncompleto    = Color(0xFFFF6B66) // "Planilha incompleta" (ícone de status)
val ExaltedAlertaFundo         = Color(0xFF3A1716) // Fundo de estado de alerta (PB negativo, tag Atordoado)
val ExaltedAlertaTexto         = Color(0xFFFF8A80) // Texto/borda de estado de alerta (mesmos 2 usos acima)
val ExaltedFavorecidaLaranja   = Color(0xFFFFC66D) // Indicador de Habilidade Favorecida (Aba 4)
val ExaltedLimiteClaro         = Color(0xFFF0C868) // Degradê do contador de Limite (Aba 1) — ponta clara
val ExaltedLimiteEscuro        = Color(0xFF6B4E1E) // Degradê do contador de Limite (Aba 1) — ponta escura
val ExaltedEssenciaClaro       = Color(0xFFFFF9C4) // Degradê de Essência Permanente (Aba 5) — ponta clara
val ExaltedEssenciaEscuro      = Color(0xFFF9A825) // Degradê de Essência Permanente (Aba 5) — ponta escura
val ExaltedDanoContundente     = Color(0xFFFF9800) // Símbolo "/" na Trilha de Vitalidade
val ExaltedDanoLetal           = Color(0xFFF44336) // Símbolo "X" na Trilha de Vitalidade
val ExaltedDanoAgravado        = Color(0xFF9C27B0) // Símbolo "*" na Trilha de Vitalidade
val ExaltedNebulosaFundo1      = Color(0xFF2A1F4D) // Nebulosa decorativa de fundo (canto inferior direito)
val ExaltedNebulosaFundo2      = Color(0xFF241833) // Nebulosa decorativa de fundo (canto inferior esquerdo)

// --- Brushes reutilizáveis: consolidam dois padrões de gradiente que já
// apareciam repetidos, inline, em vários componentes (ButtonComponents,
// CardComponents, DecorativeComponents) — mudar o efeito num lugar só
// agora reflete em todo canto que usa esse brush, em vez de precisar
// editar cada ocorrência separadamente. Sem parâmetros de posição
// explícitos (start/end), então se ajustam automaticamente ao tamanho de
// qualquer componente que os use. ---
// CRÍTICO: precisa ser get() (recalculado a cada leitura), não um val
// estático — mesmo usando cores reativas por dentro (ExaltedMetalGold*),
// um val de nível de arquivo captura o valor delas só na primeira
// leitura e nunca mais, então o brush ficava travado na paleta inicial
// (dourado do Solar) mesmo depois de trocar pra Sangue de Dragão/Lunar.
// Usado pesadamente em ErgonomicMultiWordButton — provável causa raiz de
// vários relatos de "ainda está dourado" nas Abas 2/3/4/8 do Lunar.
val ExaltedGoldGradientBrush: Brush get() = Brush.linearGradient(
    listOf(ExaltedMetalGoldShine, ExaltedMetalGoldFlash, ExaltedMetalGoldShine)
)
val ExaltedSubtleGoldBorderBrush: Brush get() = Brush.linearGradient(
    listOf(ExaltedMetalGoldDeep, ExaltedOutline, ExaltedMetalGoldDeep)
)

// Padrão de contraste automático — pedido explícito do usuário: fundo
// claro exige fonte escura, fundo escuro exige fonte clara, calculado a
// partir da própria cor de fundo (em vez de decidir manualmente cor por
// cor, local por local). Usa a fórmula de contraste relativo (WCAG), a
// mesma base usada por ferramentas de acessibilidade de contraste.
//
// Nenhuma cor de fonte do app pode ser preta, e a fonte nunca pode ser da
// mesma cor do botão em que está escrita. Por isso as únicas duas opções
// candidatas são ExaltedGold (a cor da área ativa — dourado no Solar,
// rubi no Sangue de Dragão, prata no Lunar) e ExaltedOnSurface (a cor
// neutra dos menus/barra de rolagem). Em vez de decidir qual delas usar
// por um limiar fixo de luminância do fundo (o que já se mostrou frágil:
// bastou uma cor de área ficar um pouco mais escura pra cair do lado
// errado do limiar e ficar parecida demais com a outra opção do mesmo
// tom), calcula o contraste real contra as duas e escolhe a que de fato
// contrasta melhor com o fundo recebido — continua correto qualquer que
// seja o tom exato calibrado pra cada paleta.
private fun luminanciaRelativa(cor: Color): Float {
    fun canalLinear(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * canalLinear(cor.red) + 0.7152f * canalLinear(cor.green) + 0.0722f * canalLinear(cor.blue)
}

private fun razaoDeContraste(a: Color, b: Color): Float {
    val l1 = luminanciaRelativa(a)
    val l2 = luminanciaRelativa(b)
    val claro = maxOf(l1, l2)
    val escuro = minOf(l1, l2)
    return (claro + 0.05f) / (escuro + 0.05f)
}

fun corTextoContraste(fundo: Color): Color {
    return if (razaoDeContraste(fundo, ExaltedOnSurface) >= razaoDeContraste(fundo, ExaltedGold)) {
        ExaltedOnSurface
    } else {
        ExaltedGold
    }
}
