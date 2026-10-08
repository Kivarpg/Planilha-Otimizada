package com.example.ui.theme

// ============================================================================
// SKIN — Type.kt
// ----------------------------------------------------------------------------
// Centraliza toda a tipografia do app: qual fonte usa cada estilo de texto
// (título principal vs. subtítulos vs. texto normal) e os tamanhos/pesos/
// espaçamentos de cada nível. Qualquer Text/AutoSizeText do app que use
// `MaterialTheme.typography.X` lê daqui automaticamente — para trocar a
// fonte ou o tamanho de algum estilo, mude só aqui.
//
// HIERARQUIA DE 4 FONTES (para delimitar visualmente título/subtítulo/corpo/
// símbolos técnicos):
//   1. PRINCIPAL — Cinzel Decorative — a mais rebuscada, reservada para o
//      título grande no topo de cada aba (headlineSmall, usado uma vez por
//      tela via SectionHeader). Justamente por ser tão ornamentada, fica
//      restrita a esse único uso — em tamanhos menores ou com mais volume de
//      texto ela perde legibilidade.
//   2. SECUNDÁRIA — Cinzel — ainda de caráter "épico"/monumental, mas mais
//      limpa que a Decorative, então aguenta ser usada com mais frequência
//      nos subtítulos de seção (titleLarge/Medium/Small).
//   3. TERCIÁRIA — Inter — fonte humanista desenhada especificamente para
//      legibilidade em telas pequenas, sem nenhum traço decorativo/
//      deformidade. Usada em todo texto operacional: corpo, rótulos, valores
//      numéricos (bodyLarge a labelSmall).
//   4. SÍMBOLOS — Rajdhani — geométrica/condensada, caráter técnico-mecânico
//      (não narrativa como as outras três). Não faz parte de nenhum nível de
//      MaterialTheme.typography — é aplicada manualmente (via constante
//      ExaltedSymbolFont, exportada abaixo) nos poucos elementos que são
//      símbolos/números soltos, não texto de leitura: os símbolos "+"/"−" dos
//      botões de contador (RingStepSymbol/GildedStepButton), o texto das
//      abas nas duas barras de navegação, e o número de nível na trilha de
//      Quebra de Limite. Esses pontos usavam a serifada padrão do sistema
//      (FontFamily.Serif) antes — Rajdhani unifica todos no mesmo caráter
//      "técnico", mais coerente com o tom metálico do app do que uma
//      serifada genérica de sistema que varia entre aparelhos.
// ============================================================================

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Todas as três fontes abaixo são SIL Open Font License (google/fonts) e
// foram instanciadas ESTATICAMENTE onde a fonte original é variável — Cinzel
// Decorative já é distribuída como estática (sem eixo de peso), mas Inter é
// variável (eixos wght/opsz) e precisou ser instanciada em wght=400, opsz=14
// antes de entrar no projeto. Carregar uma fonte variável como .ttf bruto
// sem um XML de font-family já causou falha real de renderização neste
// projeto (ver histórico da Cinzel) — instanciar estaticamente elimina essa
// classe inteira de instabilidade em qualquer versão/fabricante de Android.

// 1. PRINCIPAL — título grande, único por tela.
val ExaltedPrimaryFont = FontFamily(Font(R.font.cinzel_decorative))

// 2. SECUNDÁRIA — subtítulos de seção.
val ExaltedSecondaryFont = FontFamily(Font(R.font.cinzel))

// 3. TERCIÁRIA — texto normal, rótulos, valores.
private val ExaltedBodyFont = FontFamily(Font(R.font.inter))

// 4. SÍMBOLOS — não faz parte de Typography (não é um nível de leitura),
// por isso é pública: aplicada manualmente onde é usada (ver lista acima).
val ExaltedSymbolFont = FontFamily(Font(R.font.rajdhani))

val Typography = Typography(
    // Usado no cabeçalho principal da ficha. Antes não era sobrescrito e, por
    // isso, recaía na tipografia Material padrão, quebrando a linguagem editorial.
    headlineMedium = TextStyle(
        fontFamily = ExaltedPrimaryFont, fontWeight = FontWeight.Bold,
        fontSize = 31.sp, lineHeight = 37.sp, letterSpacing = 1.55.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ExaltedPrimaryFont, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 1.65.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ExaltedSecondaryFont, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = 1.05.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ExaltedSecondaryFont, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.8.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ExaltedSecondaryFont, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.75.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ExaltedBodyFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ExaltedBodyFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ExaltedBodyFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.05.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ExaltedBodyFont, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.65.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ExaltedBodyFont, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 0.45.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ExaltedBodyFont, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.2.sp
    )
)
