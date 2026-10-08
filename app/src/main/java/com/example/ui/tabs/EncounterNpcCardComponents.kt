package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.model.ArquetipoEncontro
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltVisualTemplate
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface

internal fun corZebrada(indice: Int, visualTemplate: ExaltVisualTemplate? = null): Color =
    if (indice % 2 == 0) {
        visualTemplate?.surface?.copy(alpha = 0.96f) ?: ExaltedDarkSurfaceVariant.copy(alpha = 0.94f)
    } else {
        visualTemplate?.surfaceVariant?.copy(alpha = 0.58f) ?: Color.Transparent
    }

/**
 * CORREÇÃO FUNCIONAL — listrado errado ao inserir linhas condicionais
 * (Aparar/Evasão) que só aparecem para alguns NPCs.
 *
 * A versão anterior usava um contador mutável compartilhado (IndiceZebrado)
 * incrementado dentro de um `remember(rotulo)`, pra sobreviver a
 * recomposições parciais disparadas por uma única linha (ex.: abrir o
 * popup de cálculo, que só recompõe aquela LinhaInfo). Isso funcionava
 * enquanto o conjunto de linhas de um NPC era sempre o mesmo, mas quebrava
 * assim que uma linha passou a aparecer só condicionalmente (Aparar/Evasão
 * "não disponível" pra quem não tem a perícia): o `remember` de cada rótulo
 * é fixado a partir da primeira vez que aquela linha específica compõe, e
 * como o conjunto/ordem de linhas varia por NPC, duas linhas adjacentes
 * podiam acabar herdando o mesmo valor de paridade.
 *
 * A correção troca a memorização por um índice simples: quem chama
 * LinhaInfo já sabe a posição exata da linha na lista visível (é só um
 * contador local incrementado sequencialmente enquanto a função-pai
 * recompõe). LinhaInfo deixa de mutar qualquer estado compartilhado —
 * só pinta o fundo a partir do Int que recebeu. Isso resolve os dois
 * problemas de uma vez: o índice reflete exatamente as linhas que
 * realmente aparecem para aquele NPC (inclusive quando Aparar/Evasão
 * somem ou aparecem condicionalmente), e uma recomposição parcial de uma
 * única linha não pode mais alterar a cor de ninguém, porque não há mais
 * nada pra incrementar — o Int já chegou pronto.
 */
@Composable
internal fun LinhaInfo(
    rotulo: String,
    valor: String,
    indice: Int,
    calculo: String? = null,
    calculoLongPress: String? = null,
    sufixo: String? = null,
    prefixo: String? = null,
    onTapOverride: (() -> Unit)? = null,
    longPressTitulo: String? = null,
    longPressContent: (@Composable () -> Unit)? = null,
    visualTemplate: ExaltVisualTemplate? = null,
    valueWidth: androidx.compose.ui.unit.Dp = 42.dp,
    valueTextStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    inlineSummary: Boolean = false
) {
    var mostrarCalculo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var mostrarLongPress by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // A ação de toque pode mudar com o NPC sem reiniciar o detector de gestos
    // em toda recomposição que recria a lambda.
    val currentOnTapOverride by androidx.compose.runtime.rememberUpdatedState(onTapOverride)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(corZebrada(indice, visualTemplate))
                .padding(vertical = 7.dp, horizontal = 8.dp)
                .then(
                    if (onTapOverride != null) {
                        // CORREÇÃO: antes, passar onTapOverride desativava o long
                        // press por completo (branch exclusivo) — por isso a linha
                        // "Equipamento" não reagia a long press nenhum. Agora os
                        // dois gestos coexistem: toque continua chamando
                        // onTapOverride (abre o editor), e long press mostra
                        // calculoLongPress (resumo só da arma/armadura atuais do
                        // NPC), igual ao padrão já usado na linha de XP.
                        Modifier.pointerInput(calculoLongPress, onTapOverride != null) {
                            detectTapGestures(
                                onTap = { currentOnTapOverride?.invoke() },
                                onLongPress = { if (calculoLongPress != null) mostrarLongPress = true }
                            )
                        }
                    } else if (calculo != null || calculoLongPress != null) {
                        Modifier.pointerInput(calculo, calculoLongPress) {
                            detectTapGestures(
                                onTap = { if (calculo != null) mostrarCalculo = true },
                                onLongPress = { if (calculoLongPress != null) mostrarLongPress = true }
                            )
                        }
                    } else Modifier
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inlineSummary) {
                com.example.ui.components.AutoSizeAppText(
                    text = buildString {
                        if (prefixo != null) append(prefixo)
                        append(formatarValorPlanilha(valor))
                        if (sufixo != null) append(" ").append(sufixo)
                        append(" ").append(rotulo)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = visualTemplate?.onSurface ?: ExaltedOnSurface,
                    fontWeight = FontWeight.Bold,
                    maxFontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                    minFontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
            // Prefixo (sinal +/-) — SEMPRE reserva 12dp, em branco quando
            // não há sinal. Pedido explícito do usuário: o número não
            // pode se mover nem 1 pixel dependendo de ter sinal ou não —
            // antes esse espaço só existia condicionalmente, deslocando
            // a caixa do número (e tudo depois dela) pra direita quando
            // havia sinal, desalinhando com as linhas sem sinal.
            Box(modifier = Modifier.width(12.dp), contentAlignment = Alignment.CenterEnd) {
                if (prefixo != null) {
                    AppText(
                        prefixo,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = visualTemplate?.onSurface ?: ExaltedOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier.width(valueWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                AppText(
                    formatarValorPlanilha(valor),
                    style = valueTextStyle.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = visualTemplate?.onSurface ?: ExaltedOnSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
            // Sufixo — mesma lógica: SEMPRE reserva 20dp (com um respiro
            // de 4dp em relação ao número), em branco quando não há
            // letra, mantendo a posição do rótulo consistente também.
            Box(modifier = Modifier.width(20.dp).padding(start = 4.dp), contentAlignment = Alignment.CenterStart) {
                if (sufixo != null) {
                    AppText(
                        sufixo,
                        style = MaterialTheme.typography.bodyLarge,
                        color = visualTemplate?.onSurface ?: ExaltedOnSurface,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            AppText(
                rotulo,
                style = MaterialTheme.typography.bodyLarge,
                color = visualTemplate?.muted ?: ExaltedMuted,
                modifier = Modifier.weight(1f)
            )
            }
        }
        if (mostrarCalculo && calculo != null) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 44),
                onDismissRequest = { mostrarCalculo = false }
            ) {
                Surface(
                    color = ExaltedDarkSurfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedGold),
                    shadowElevation = 4.dp
                ) {
                    AppText(
                        calculo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = visualTemplate?.onSurface ?: ExaltedOnSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        if (mostrarLongPress && calculoLongPress != null) {
            if (longPressTitulo != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { mostrarLongPress = false },
                    title = {
                        AppText(
                            longPressTitulo,
                            color = visualTemplate?.accentBright ?: ExaltedGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Surface(
                            color = ExaltedDarkSurfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (longPressContent != null) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .widthIn(max = 420.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    longPressContent()
                                }
                            } else {
                                AppText(
                                    calculoLongPress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = visualTemplate?.onSurface ?: ExaltedOnSurface,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .widthIn(max = 420.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    },
                    confirmButton = {
                        com.example.ui.components.GildedDialogButton(
                            text = "Fechar",
                            onClick = { mostrarLongPress = false }
                        )
                    },
                    containerColor = ExaltedDarkSurfaceVariant
                )
            } else {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, 44),
                    onDismissRequest = { mostrarLongPress = false }
                ) {
                    Surface(
                        color = ExaltedDarkSurfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedGold),
                        shadowElevation = 4.dp
                    ) {
                        AppText(
                            calculoLongPress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = visualTemplate?.onSurface ?: ExaltedOnSurface,
                            modifier = Modifier.padding(12.dp).widthIn(max = 320.dp)
                        )
                    }
                }
            }
        }
    }
}

internal fun formatarValorPlanilha(valor: String): String =
    valor.toIntOrNull()?.let { formatarNumeroPlanilha(it) } ?: valor

internal fun formatarNumeroPlanilha(valor: Int): String =
    if (valor in 0..9) "0$valor" else valor.toString()

internal fun rotuloArquetipo(arquetipo: ArquetipoEncontro): String = when (arquetipo) {
    ArquetipoEncontro.FISICO -> "Físico"
    ArquetipoEncontro.SOCIAL -> "Social"
    ArquetipoEncontro.MENTAL -> "Mental"
}
