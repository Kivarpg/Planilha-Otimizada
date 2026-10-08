package com.example.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.components.InkButtonVariant
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedOutline
import com.example.ui.theme.ExaltVisualTemplate

// Trilha de Força de Vontade: uma única trilha de 1 a 10 pontos, iniciando em 5.
// Os botões "−"/"+" reduzem/aumentam o valor atual em 1. Tocar diretamente em
// uma bolinha da escala define o valor naquele ponto: bolinhas até o valor
// tocado ficam preenchidas de amarelo; as demais ficam vazadas (apenas contorno).
@Composable
fun WillpowerTrack(
    valor: Int,
    usados: Set<Int>,
    planilhaConcluida: Boolean,
    onValorChange: (Int) -> Unit,
    onToggleUsado: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minValor: Int = 5,
    maxValor: Int = 10,
    visualTemplate: ExaltVisualTemplate? = null
) {
    val surfaceVariant = visualTemplate?.surfaceVariant ?: ExaltedDarkSurfaceVariant
    val outline = visualTemplate?.outline ?: ExaltedOutline
    val accent = visualTemplate?.accent ?: ExaltedAmber
    val accentBright = visualTemplate?.accentBright ?: ExaltedAccentBright
    val shine = visualTemplate?.metalShine ?: ExaltedGold
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceVariant),
        border = BorderStroke(1.dp, outline),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Trilha: as 10 bolinhas da escala (1 a 10), sem sinais de menos/mais.
            // O toque nas bolinhas depende automaticamente de "Planilha Concluída": antes dela, define o
            // nível permanente (comprado com XP), que nunca pode ficar abaixo de 5;
            // depois, marca/desmarca pontos como gastos —
            // nível permanente; depois, marca/desmarca pontos como gastos —
            // sem alternância manual, trava sozinho ao concluir a planilha.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // O índice clicado apenas dispara a operação. A regra sequencial
                    // vive em WillpowerTrackLogic: um clique em qualquer caixa vazia
                    // consome o primeiro ponto disponível; um clique em uma caixa marcada
                    // remove o último ponto marcado. Assim, tocar na última caixa vazia
                    // também preenche automaticamente a primeira caixa da sequência e,
                    // quando todas estão cheias, qualquer caixa marcada permite desfazer
                    // o último ponto gasto.

                    // Fileira de cima: os círculos (posse) — pedido explícito do
                    // usuário, círculos ficam ACIMA dos quadrados, não os
                    // englobando (não mais centralizados um dentro do outro).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..maxValor) {
                            val adquirida = i <= valor
                            val gasta = adquirida && usados.contains(i)
                            val podeInteragir = if (planilhaConcluida) adquirida else true
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (adquirida && !gasta) Modifier.shadow(
                                                elevation = 6.dp,
                                                shape = CircleShape,
                                                ambientColor = shine,
                                                spotColor = shine
                                            ) else Modifier
                                        )
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (adquirida) accentBright else Color.Black)
                                        .border(
                                            2.dp,
                                            if (adquirida) accent else outline.copy(alpha = 0.6f),
                                            CircleShape
                                        )
                                        .feedbackClickable(enabled = podeInteragir) {
                                            if (planilhaConcluida) {
                                                onToggleUsado(i)
                                            } else {
                                                onValorChange(i.coerceAtLeast(minValor))
                                            }
                                        }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    // Fileira de baixo: os quadrados (gasto) — sempre mostra os 10.
                    // Após a conclusão, qualquer caixa adquirida pode ser tocada:
                    // a lógica centralizada decide se o clique consome o primeiro
                    // ponto disponível ou devolve o último ponto gasto.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..maxValor) {
                            val adquirida = i <= valor
                            val gasta = adquirida && usados.contains(i)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(if (gasta) accentBright else Color.Black)
                                        .border(
                                            1.5.dp,
                                            when {
                                                gasta -> shine
                                                adquirida -> outline
                                                else -> outline.copy(alpha = 0.3f)
                                            }
                                        )
                                        .feedbackClickable(enabled = adquirida) {
                                            onToggleUsado(i)
                                        }
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}


// Botão compacto de ajuste de um contador de Iniciativa (-10, -5, -1, +1,
// +5, +10) — movido de CombatTab.kt (2026-09, refatoração estrutural)
// pra cá porque agora é reutilizado também pelo contador de Iniciativa
// da Aba 11 (Encontros), que passou a seguir o mesmo padrão visual e
// funcional da Aba 5 (Combate), a pedido explícito do usuário.
@Composable
fun IniciativaAjusteButton(
    delta: Int,
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp? = null,
    visualTemplate: ExaltVisualTemplate? = null
) {
    val texto = if (delta > 0) "+$delta" else "$delta"
    InkButton(
        visualTemplate = visualTemplate,
        label = texto,
        onClick = onClick,
        size = InkButtonSize.Small,
        variant = InkButtonVariant.Secondary,
        customWidth = width ?: if (kotlin.math.abs(delta) >= 10) 96.dp else 76.dp,
        customHeight = 52.dp
    )
}

// Cor do contador de Iniciativa conforme o valor (Aba 5 e Aba 11) — pedido
// explícito do usuário: verde acima de 10, laranja/amarelo em 9, vermelho
// em 5 (e abaixo), com degradê suave e proporcional entre esses pontos —
// não um salto abrupto de cor a cada limiar.
private val CorIniciativaVerde = Color(0xFF4CAF50)
private val CorIniciativaLaranja = Color(0xFFFFA726)
private val CorIniciativaVermelha = Color(0xFFE53935)

fun corIniciativaPorValor(valor: Int): Color {
    val v = valor.toFloat()
    return when {
        v >= 11f -> CorIniciativaVerde
        v >= 9f -> lerp(CorIniciativaLaranja, CorIniciativaVerde, (v - 9f) / 2f)
        v >= 5f -> lerp(CorIniciativaVermelha, CorIniciativaLaranja, (v - 5f) / 4f)
        else -> CorIniciativaVermelha
    }
}
