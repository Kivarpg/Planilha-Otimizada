package com.example.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.Dimens
import com.example.ui.theme.ExaltedTextStroke

// Título da Aba 11 (nome do NPC e as seções Atributos, Habilidades,
// Méritos, Ações, Encantos) com o mesmo efeito visual de contorno usado em
// AppText(..., forceStroke = true) — mas respeitando de fato a cor da
// paleta do NPC (visualTemplate.accentBright), em vez de cair sempre em
// branco puro.
//
// Motivo de existir separado de AppText: no branch forceStroke = true de
// TextComponents.kt, o parâmetro `color` recebido é descartado — o
// preenchimento do texto usa sempre a constante ExaltedTextFill (branco
// fixo), então todo título que pedia `color = visualTemplate.accentBright`
// com forceStroke = true renderizava branco, não a cor da paleta. Esse
// branch também está marcado como "APPROVED VISUAL CUSTOMIZATION — DO NOT
// MODIFY WITHOUT VISUAL IMPACT REVIEW" (existe por causa de um ajuste de
// centralização nas caixas de Casta/Aspecto), então em vez de alterar o
// componente compartilhado — que é usado em todas as abas do app — este
// composable replica o mesmo efeito só para os títulos da Aba 11.
//
// De quebra, isso também resolve o bug dos botões "+ Aumentar XP"/
// "− Diminuir XP" aparecendo colados no título "Encantos" em vez de na
// borda direita da tela: o mesmo branch de AppText aplica o `modifier`
// recebido (inclusive um Modifier.weight(1f) vindo de dentro de um Row) só
// nos dois Text internos, não no Box que os envolve. Como o Row só lê
// informação de peso do seu filho direto, e o filho direto ali é o Box (sem
// o modifier), o weight() nunca chegava a valer — o título ficava do
// tamanho do próprio texto, colado nos botões, em vez de esticar e empurrá-
// los pra ponta da tela. Aqui o modifier vai direto no Box, que é o filho
// real do Row, então o weight() funciona normalmente.
@Composable
fun EncounterCardTitle(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = (MaterialTheme.typography.titleLarge.fontSize.value + 1f).sp),
    fontWeight: FontWeight? = FontWeight.Bold,
    softWrap: Boolean = true,
    textAlign: TextAlign = TextAlign.Center
) {
    val strokeWidthPx = with(LocalDensity.current) { Dimens.TextStrokeWidth.toPx() }
    // Reserva vertical própria para o título. O contorno tipográfico pode
    // ultrapassar visualmente a métrica nominal da fonte em alguns devices;
    // sem esta folga, o primeiro controle da seção parece colado/sobreposto.
    // Centralizar a proteção aqui evita depender de Spacers externos e mantém
    // todas as seções da Aba 11 consistentes.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(bottom = 4.dp)
    ) {
        Text(
            text = text,
            style = style.copy(
                color = ExaltedTextStroke,
                fontWeight = fontWeight ?: style.fontWeight,
                drawStyle = Stroke(width = strokeWidthPx)
            ),
            softWrap = softWrap,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = text,
            style = style.copy(
                color = color,
                fontWeight = fontWeight ?: style.fontWeight,
                drawStyle = Fill
            ),
            softWrap = softWrap,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
