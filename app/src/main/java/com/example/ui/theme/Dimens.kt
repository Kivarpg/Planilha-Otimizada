package com.example.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// SKIN — Dimens.kt
// ----------------------------------------------------------------------------
// Centraliza TODAS as medidas visuais reutilizadas pelo app: espaçamento
// interno de cards, raio de ícones, espessura de bordas, etc.
//
// Para o próximo programador: se você quer que os cards fiquem mais
// "respirados", os ícones maiores, ou as bordas mais grossas, mexa AQUI —
// nunca precisa abrir os arquivos de components/ pra isso.
// Esses arquivos só LEEM estas constantes; a lógica funcional do app
// (ViewModel, model) nunca referencia nada deste arquivo.
// ============================================================================

object Dimens {

    // --- Texto com contorno ---
    // Espessura ajustável do stroke global para textos normais e botões.
    val TextStrokeWidth = 2.5.dp

    // --- Espaçamento interno de cards e seções ---
    val CardPaddingInner = 12.dp
    val CardPaddingInnerLarge = 14.dp
    val SectionSpacingVertical = 10.dp
    val SectionSpacingBetweenGroups = 20.dp

    // --- Bordas e molduras (GildedCard) ---
    val CardBorderWidth = 1.5.dp
    val CardBorderInnerGap = 3.dp
    val CardCornerFlourishLength = 48.dp
    val CardCornerDiamondOffset = 32.dp
    val CardCornerDiamondSize = 3.9.dp

    // --- Cabeçalho de seção (SectionHeader) ---
    val SectionTitleMaxSize = 19.sp
    val SectionTitleMinSize = 11.sp
    val SectionFlourishWidth = 42.dp
    val SectionFlourishHeight = 18.dp
    val SectionDividerWidthFraction = 0.82f

    // --- Medalhões de ícone (MedallionIcon) ---
    val MedallionDefaultSize = 40.dp
    val MedallionGlowRadiusFactor = 0.98f
    val MedallionRingStrokeFactor = 0.052f
    val MedallionInnerRingStrokeFactor = 0.018f

    // --- Ícones de Casta/Aspecto (Abas 1 e 2) ---
    // Tamanho padrão único pro app inteiro — antes, Sangue de Dragão usava
    // 56dp (seletor na Aba 1) e 72dp (ícone principal na Aba 2), diferentes
    // do Solar (64dp). Igualado a pedido explícito do usuário: 64dp (a
    // referência do Solar) vira o padrão pras duas Abas.
    val CasteAspectIconSize = 64.dp

    // --- Botões circulares +/- (GildedStepButton / RingStepSymbol) ---
    val StepperRingSize = 46.dp
    val StepperRingStroke = 1.5.dp

    // --- Pílulas de seleção (ErgonomicMultiWordButton) ---
    // Mantidas no tamanho original: aumentar reduziria quantas cabem por
    // linha na grade de seleção (Habilidades de Casta/Favorecidas/Encantos).
    val PillMinWidth = 96.dp
    val PillMinHeight = 56.dp
    val PillDoubleBorderGap = 2.dp

    // --- Tabela de abas (SheetTabsBar / TabIconDisplay) ---
    // Histórico: a altura da aba foi ajustada 3 vezes seguidas (78→100→
    // 110→120dp na LazyRow) porque cada tentativa chutava um número sem
    // somar o que o conteúdo realmente ocupa. A causa raiz: a caixa
    // externa do ícone (com o halo de brilho) é sempre TabIconGlowMultiplier
    // vezes maior que o tamanho "lógico" pedido — só que esse fator vivia
    // isolado dentro de TabIconDisplay, sem SheetTabsBar saber dele pra
    // calcular a altura corretamente. Agora os dois arquivos leem as
    // MESMAS constantes daqui, e a altura final é somada a partir do
    // conteúdo real (indicador + ícone + texto + respiro), não chutada.
    val TabIconGlowMultiplier = 1.10f
    val TabIconSelectedSize = 31.dp
    val TabIconUnselectedSize = 27.dp
    // Maior dos dois tamanhos de ícone (selecionado) — usado pro cálculo
    // de altura, já que a aba precisa caber o ícone no seu maior estado.
    val TabIconBoxSize get() = TabIconSelectedSize * TabIconGlowMultiplier

    val TabIndicatorDotAreaHeight = 9.dp
    val TabInternalSpacing = 1.dp
    val TabTitleFontSize = 10.sp
    // Estimativa de altura de uma linha de texto a TabTitleFontSize,
    // incluindo o espaço extra que o Compose reserva por padrão pra
    // acentos/descendentes (fator ~1.4x o tamanho da fonte é o típico
    // pra Material3). Pequenas variações reais de densidade/fonte são
    // absorvidas pela margem de segurança abaixo.
    val TabTitleLineHeightEstimate = 14.dp
    val TabItemVerticalPadding = 2.dp
    // Folga explícita e documentada — diferente de um número "mágico"
    // solto, esta margem existe DE PROPÓSITO pra absorver pequenas
    // variações de medição real vs. estimada, sem precisar reajustar
    // manualmente de novo cada vez que algo no conteúdo mudar de tamanho.
    val TabItemSafetyMargin = 2.dp
    val TabBarVerticalPadding = 4.dp

    val TabItemContentHeight get() = TabIndicatorDotAreaHeight + TabInternalSpacing + TabIconBoxSize + TabInternalSpacing + TabTitleLineHeightEstimate
    val TabItemHeight get() = TabItemContentHeight + TabItemVerticalPadding * 2 + TabItemSafetyMargin
    val TabBarHeight get() = TabItemHeight + TabBarVerticalPadding * 2
}
