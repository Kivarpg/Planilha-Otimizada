package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// SKIN: NÃO reconstruímos LocalDensity/fontScale aqui de propósito. O
// Android 14+ usa uma curva NÃO-LINEAR de escala de fonte pra
// acessibilidade (via FontScaleConverter interno) — reconstruir um
// Density(density, fontScale) manualmente com a função de 2 argumentos
// usa só interpolação linear simples, perdendo essa curva. Isso deixaria
// o texto MENOS preciso justamente pra quem mais precisa de fonte grande
// (o público que essa preocupação de acessibilidade deveria ajudar).
// LocalDensity.current, sem modificação, já vem corretamente calibrado
// pelo próprio sistema com o conversor certo — é a forma mais segura de
// respeitar a preferência do usuário sem reintroduzir esse problema.
@Composable
fun ExaltedTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // CRÍTICO: precisa ser recalculado AQUI DENTRO, a cada recomposição —
    // como val de nível de arquivo (fora da função), era computado uma
    // única vez na primeira leitura e nunca mais, então a troca de
    // paleta por template (Solar/Sangue de Dragão/Lunar) nunca refletia
    // em nada que lesse MaterialTheme.colorScheme (ficava sempre no
    // dourado do Solar, o valor inicial).
    val exaltedDarkColorScheme = darkColorScheme(
        primary = ExaltedAmber,
        onPrimary = ExaltedOnPrimary,
        primaryContainer = ExaltedDarkSurfaceVariant,
        onPrimaryContainer = ExaltedGold,
        secondary = ExaltedAmberVariant,
        onSecondary = ExaltedOnPrimary,
        secondaryContainer = ExaltedDarkSurfaceVariant,
        onSecondaryContainer = ExaltedGold,
        tertiary = ExaltedGold,
        onTertiary = ExaltedOnPrimary,
        background = ExaltedDarkBackground,
        onBackground = ExaltedOnBackground,
        surface = ExaltedDarkSurface,
        onSurface = ExaltedOnSurface,
        surfaceVariant = ExaltedDarkSurfaceVariant,
        onSurfaceVariant = ExaltedOnSurface,
        outline = ExaltedOutline,
        outlineVariant = ExaltedDivider,
        error = ExaltedError
    )
    MaterialTheme(
        colorScheme = exaltedDarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
