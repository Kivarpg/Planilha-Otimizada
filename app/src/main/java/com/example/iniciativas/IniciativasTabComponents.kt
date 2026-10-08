package com.example.iniciativas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedTextFill
import com.example.ui.theme.ExaltedTextStroke


// APPROVED VISUAL CUSTOMIZATION — acabamento metálico cinza compartilhado
// por etiquetas de estado e avisos do sistema da Aba 12.
internal val CorEtiquetaMetalica = Color(0xFF5F6266)
internal val CorEtiquetaMetalicaBorda = Color(0xFF9EA1A5)
// Extraído de IniciativasTab.kt (refatoração de organização — pedido
// explícito do usuário, sem mudança de comportamento). As 4 funções
// abaixo já eram autocontidas (recebem tudo via parâmetro, sem estado
// compartilhado) — candidatas de baixo risco, mudadas de private pra
// internal já que são chamadas a partir de IniciativasTab (arquivo
// principal, que continua com a função de tela em si).
@Composable
internal fun ParticipanteCard(
    participante: ParticipanteIniciativa,
    ordem: Int,
    ehElegivel: Boolean,
    ehAtacante: Boolean,
    ehEmpateMaiorIniciativa: Boolean = false,
    atacanteAtivoId: String? = null,
    ehAlvo: Boolean,
    emClash: Boolean,
    ehVencedor: Boolean,
    ofuscado: Boolean,
    jaAgiram: Boolean,
    aguardandoEmpate: Boolean = false,
    /** Valor de iniciativa em resolução temporária (PDF §5.2 / §7). Null = usa o valor gravado. */
    iniciativaExibida: Int? = null,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onPosition: (Offset, androidx.compose.ui.geometry.Size) -> Unit
) {
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // Aba 12 — contornos da caixa:
    //   vermelho  → turno ativo / Atacante (elegível ou já declarou alvo)
    //   amarelo   → Aguardando (ainda não agiu, mas não é o turno dele)
    //   nenhum    → já agiu neste turno
    // Fundo Dark Red do Atacado e ofuscamento dos demais após a seleção
    // do alvo permanecem inalterados.
    val alpha = if (ofuscado) 0.28f else 1f
    val corFundo = CorFundoCombatente
    val (contornoCor, contornoLargura) = when {
        ehAtacante -> CorContornoAtacante to 2.dp
        aguardandoEmpate -> Color.Transparent to 0.dp
        !jaAgiram -> CorContornoPendente to 2.dp
        else -> Color.Transparent to 0.dp
    }
    val temContornoAtacante = ehAtacante
    val temContornoPendente = !ehAtacante && !aguardandoEmpate && !jaAgiram

    val baseModifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned {
            onPosition(
                it.positionInRoot(),
                androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat())
            )
        }
        .pointerInput(participante.id) {
            detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
        }
        .feedbackOnPress()

    Surface(
        color = when {
            ehAtacante -> CorEtiquetaDarkRed
            ehAlvo -> CorAtacado
            else -> corFundo
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(contornoLargura, contornoCor),
        modifier = when {
            temContornoAtacante -> baseModifier.shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = CorContornoAtacante.copy(alpha = 0.30f),
                spotColor = CorContornoAtacante.copy(alpha = 0.55f)
            )
            temContornoPendente -> baseModifier.shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = CorContornoPendente.copy(alpha = 0.20f),
                spotColor = CorContornoPendente.copy(alpha = 0.35f)
            )
            else -> baseModifier
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                "$ordem.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CorNome.copy(alpha = 0.70f),
                modifier = Modifier.width(32.dp)
            )

            // O nome ocupa a área principal. O status fica na mesma linha,
            // a partir do meio da área disponível, e nunca abaixo do nome.
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    // Camada de contorno: stroke fino e uniforme seguindo cada
                    // caractere. A camada branca acima preserva o preenchimento.
                    //
                    // Correção: a cor do contorno/preenchimento precisa ir no
                    // parâmetro `color` do AppText, não dentro de `style` — com
                    // headlineSmall (fonte "de título"), o AppText usa só o
                    // parâmetro `color` para essas duas camadas, então a cor
                    // embutida em style.copy(color=...) era descartada e o
                    // contorno nunca aparecia de fato na tela. Largura do
                    // traço trocada pelo valor padrão do app (Dimens.TextStrokeWidth).
                    val tamanhoNome = when {
                        participante.nome.length > 42 -> 18.sp
                        participante.nome.length > 30 -> 20.sp
                        participante.nome.length > 22 -> 22.sp
                        else -> 24.sp
                    }
                    val densidadeNome = androidx.compose.ui.platform.LocalDensity.current
                    AppText(
                        participante.nome,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = tamanhoNome,
                            fontWeight = FontWeight.Bold,
                            drawStyle = Stroke(width = with(densidadeNome) { com.example.ui.theme.Dimens.TextStrokeWidth.toPx() })
                        ),
                        color = CorContornoNome,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppText(
                        participante.nome,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = tamanhoNome,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CorNome,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // A etiqueta deve ocupar o espaço real necessário para o texto.
                // A largura anterior de 176.dp com padding(end = 144.dp) deixava
                // somente 32.dp de área útil para o conteúdo, fazendo nomes como
                // "Atacante" e "Aguardando" aparecerem cortados em um caractere.
                // Mantemos a etiqueta na mesma linha, mas deixamos sua largura
                // intrínseca acompanhar a palavra/frase exibida.
                // Reserva uma coluna fixa para as etiquetas, evitando que o
                // início varie conforme o comprimento de cada texto. A coluna
                // é deslocada 15% para a direita dentro da área de conteúdo.
                BoxWithConstraints(
                    modifier = Modifier.width(190.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val deslocamento = maxWidth * 0.15f
                    Box(
                        modifier = Modifier.padding(start = deslocamento),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        StatusEtiqueta(
                            texto = when {
                            ehVencedor -> "Vencedor da Colisão"
                            emClash -> "Colisão"
                            ehAtacante -> "Atacante"
                            ehAlvo -> "Atacado"
                            participante.rodadasEmAtordoamento > 0 -> "Atordoamento de Iniciativa"
                            jaAgiram -> "Concluído"
                            else -> "Aguardando"
                        },
                            fundo = CorEtiquetaMetalica
                        )
                    }
                }
            }

            // Mesma correção: contorno padrão do app na etiqueta de Iniciativa
            // (parâmetro `color`, não style.copy) — antes aparecia sem contorno
            // algum, difícil de ler sobre os fundos coloridos do card.
            Box {
                AppText(
                    (iniciativaExibida ?: participante.iniciativa).toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        drawStyle = Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { com.example.ui.theme.Dimens.TextStrokeWidth.toPx() })
                    ),
                    fontWeight = FontWeight.Bold,
                    color = ExaltedTextStroke
                )
                AppText(
                    (iniciativaExibida ?: participante.iniciativa).toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (iniciativaExibida != null && iniciativaExibida != participante.iniciativa) ExaltedAmber else CorNome
                )
            }
        }
    }
}

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Nomes de combatentes usam exatamente o acabamento tipográfico padrão do
// restante do aplicativo: preenchimento claro + contorno escuro por caractere.
@Composable
internal fun CombatNameText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    fontWeight: FontWeight? = FontWeight.Bold,
    fillColor: Color = ExaltedTextFill,
    maxLines: Int = 1
) {
    val baseStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center
    )
    Box(modifier = modifier) {
        AppText(
            text,
            style = baseStyle.copy(
                color = ExaltedTextStroke,
                drawStyle = Stroke(width = 2.0f)
            ),
            maxLines = maxLines,
            softWrap = true,
            modifier = Modifier.fillMaxWidth()
        )
        AppText(
            text,
            style = baseStyle.copy(color = fillColor),
            maxLines = maxLines,
            softWrap = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// APPROVED VISUAL CUSTOMIZATION — etiqueta de estado da Aba 12.
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW.
// Componente único para manter todas as etiquetas com a mesma caixa sólida,
// contraste e espaçamento. Alterações visuais devem ser feitas aqui.
@Composable
internal fun StatusEtiqueta(
    texto: String,
    fundo: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = fundo,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, CorEtiquetaMetalicaBorda),
        modifier = modifier.padding(start = 8.dp)
    ) {
        AppText(
            texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = CorEtiquetaTexto,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
internal fun SeletorVencedorClash(
    participanteA: ParticipanteIniciativa,
    participanteB: ParticipanteIniciativa,
    onSelecionar: (String) -> Unit
) {
    Surface(
        color = ExaltedDarkSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CorClash.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText(
                "Selecione o Vencedor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ExaltedAccentBright
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InkButton(label = participanteA.nome, onClick = { onSelecionar(participanteA.id) }, modifier = Modifier.weight(1f), size = InkButtonSize.Small, fillMaxWidth = true)
                InkButton(label = participanteB.nome, onClick = { onSelecionar(participanteB.id) }, modifier = Modifier.weight(1f), size = InkButtonSize.Small, fillMaxWidth = true)
            }
        }
    }
}

/**
 * Caixa de iniciativa do perdedor (PDF §5.2):
 *   [Nome do perdedor]
 *   [−] [valor] [+]
 * Toda iniciativa removida do perdedor vai automaticamente ao vencedor
 * (controlado por [transferirIniciativa] no controller).
 */
@Composable
internal fun CaixaIniciativaPerdedor(
    nomePerdedor: String?,
    valorIniciativa: Int?,
    quantidadeTransferida: Int,
    ativo: Boolean,
    dica: String?,
    onMais: () -> Unit,
    onMenos: () -> Unit,
    bloqueado: Boolean = false
) {
    Surface(
        color = ExaltedDarkSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ExaltedGold.copy(alpha = if (ativo) 0.7f else 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CombatNameText(
                nomePerdedor?.let { it } ?: "Perdedor",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fillColor = if (ativo) ExaltedTextFill else ExaltedMuted,
                modifier = Modifier.fillMaxWidth()
            )
            AppText(
                "Iniciativa (temporária até Próximo)",
                style = MaterialTheme.typography.labelSmall,
                color = ExaltedMuted
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                // [−] remove iniciativa do perdedor (= transferir +1 ao vencedor)
                InkButton(onClick = onMais, enabled = ativo && !bloqueado, modifier = (Modifier.size(48.dp)).feedbackOnPress(enabled = ativo && !bloqueado)) {
                    Icon(Icons.Default.Remove, "−", tint = if (ativo) ExaltedAmber else ExaltedMuted)
                }
                AppText(
                    (valorIniciativa ?: 0).toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (ativo) ExaltedAccentBright else ExaltedMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(64.dp)
                )
                // [+] devolve iniciativa ao perdedor (= transferir −1)
                InkButton(onClick = onMenos, enabled = ativo && !bloqueado && quantidadeTransferida > 0, modifier = (Modifier.size(48.dp)).feedbackOnPress(enabled = ativo && !bloqueado && quantidadeTransferida > 0)) {
                    Icon(Icons.Default.Add, "+", tint = if (ativo) ExaltedAmber else ExaltedMuted)
                }
            }
            if (bloqueado && ativo) {
                AppText(
                    "Battle Group não reduz iniciativa de outro Battle Group. +1 ao atacante em Próximo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExaltedGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (quantidadeTransferida != 0 && ativo) {
                AppText(
                    "Transferido: $quantidadeTransferida",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExaltedGold
                )
            }
            if (dica != null) {
                AppText(dica, style = MaterialTheme.typography.labelSmall, color = ExaltedMuted)
            }
        }
    }
}

/** Compatibilidade: antigo contador global (multi-alvo / fallback). */
@Composable
internal fun ControlesTransferencia(
    contador: Int,
    ativo: Boolean,
    dica: String?,
    onMais: () -> Unit,
    onMenos: () -> Unit,
    bloqueado: Boolean = false
) {
    CaixaIniciativaPerdedor(
        nomePerdedor = null,
        valorIniciativa = contador,
        quantidadeTransferida = contador,
        ativo = ativo,
        dica = dica,
        onMais = onMais,
        onMenos = onMenos,
        bloqueado = bloqueado
    )
}
