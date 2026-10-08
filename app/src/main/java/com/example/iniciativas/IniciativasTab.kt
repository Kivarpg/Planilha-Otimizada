package com.example.iniciativas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.InkButtonVariant
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.InkButtonSize
import com.example.ui.components.feedbackOnPress

import com.example.ui.components.InkButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.GildedDialogButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedTextStroke

private val GutterWidth = 28.dp
internal val CorFundoCombatente = Color.Black
internal val CorAtacado = Color(0xFF800000)
internal val CorContornoAtacante = Color(0xFFE00000)
internal val CorContornoPendente = Color(0xFFE8C84A)
internal val CorNome = Color.White
internal val CorContornoNome = ExaltedTextStroke
internal val CorEtiquetaPreta = Color.Black
internal val CorEtiquetaDarkRed = Color(0xFF800000)
internal val CorEtiquetaTexto = Color.White
internal val CorPlacaMetalica = Color(0xFF4A4D50)
internal val CorPlacaMetalicaBorda = Color(0xFF8A8F94)
internal val CorClash = Color(0xFF9C27B0)

/**
 * Etapas exclusivas do fluxo de combate (PDF §2–§8).
 * Só os controles da etapa atual são exibidos — exceto Término, Log e Adicionar.
 */
private enum class EtapaCombate {
    /** Sem ataque travado: declarar alvo, Ataque, Pular, Colisão. */
    DECLARACAO,
    /** Clash travado sem vencedor escolhido. */
    ESCOLHER_VENCEDOR,
    /** Escolher Fulminante ou Decisivo. */
    ESCOLHER_TIPO,
    /** Escolher Sucesso ou Falha. */
    ESCOLHER_RESULTADO,
    /** Fulminante bem-sucedido: caixa do perdedor + Próximo. */
    AJUSTAR_FULMINANTE,
    /** Decisivo ou Fulminante falho: só Próximo. */
    CONFIRMAR_PROXIMO,
    /** Resolução multi (Battle Group): sucesso/falha por alvo. */
    RESOLUCAO_MULTI
}

private fun etapaDe(
    state: IniciativasState,
    paresClash: List<Pair<String, String>>,
    emResolucaoMulti: Boolean
): EtapaCombate {
    if (!state.ataqueTravado) return EtapaCombate.DECLARACAO
    if (emResolucaoMulti) return EtapaCombate.RESOLUCAO_MULTI
    if (paresClash.isNotEmpty() && state.vencedorClashId == null) return EtapaCombate.ESCOLHER_VENCEDOR
    if (state.tipoAtaquePendente == null) return EtapaCombate.ESCOLHER_TIPO
    // Em Colisão, o resultado é sempre sucesso e a escolha já é registrada
    // junto com o tipo de ataque. Portanto não exibir Sucesso/Falha.
    if (!state.colisaoAtiva && state.ataqueBemSucedido == null) return EtapaCombate.ESCOLHER_RESULTADO
    return if (state.tipoAtaquePendente == TipoAtaque.FULMINANTE && state.ataqueBemSucedido == true) {
        EtapaCombate.AJUSTAR_FULMINANTE
    } else {
        EtapaCombate.CONFIRMAR_PROXIMO
    }
}

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IniciativasTab(
    controller: IniciativasController,
    onClashResolved: (perdedorNpcId: String?, proximosNpcIds: Set<String>) -> Unit = { _, _ -> },
    onAtaquesTravados: (npcIdsAtacados: List<String>) -> Unit = {},
    onEncerrarCombate: () -> Unit = {},
    historico: List<HistoricoCombateEntry> = emptyList(),
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val ordenados = controller.participantesOrdenados()
    val elegiveis = controller.idsElegiveis()
    val paresClash = controller.paresClashAtuais()
    val emResolucaoMulti = controller.emResolucaoMulti()
    val etapa = etapaDe(state, paresClash, emResolucaoMulti)
    val travado = state.ataqueTravado

    var mostrarCadastro by remember { mutableStateOf(false) }
    var mostrarHistorico by remember { mutableStateOf(false) }
    var mostrarTermino by remember { mutableStateOf(false) }
    var menuLongPress by remember { mutableStateOf<ParticipanteIniciativa?>(null) }
    var editarNomeDe by remember { mutableStateOf<ParticipanteIniciativa?>(null) }
    var editarInitDe by remember { mutableStateOf<ParticipanteIniciativa?>(null) }

    val mensagem = state.mensagensPendentes.firstOrNull()
    val temDeclaracao = state.declaracoes.isNotEmpty() || state.temDeclaracaoMulti()

    Column(
        modifier = modifier.fillMaxSize().exaltedTabIdentity(12).exaltedContentStage(12).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // APPROVED VISUAL CUSTOMIZATION — SectionHeader / chrome permanente
        Spacer(Modifier.height(4.dp))

        // Permanente: rodada + Término + Log
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppText("Rodada ${state.rodada}", fontWeight = FontWeight.Bold, color = ExaltedGold)
            Spacer(Modifier.width(12.dp))
            InkButton(
                label = "Término",
                onClick = { mostrarTermino = true },
                enabled = ordenados.isNotEmpty(),
                size = com.example.ui.components.InkButtonSize.Small,
                brushIndex = 0
            )
            Spacer(Modifier.width(6.dp))
            InkButton(
                onClick = { mostrarHistorico = true },
                modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.History, "Log de combate", tint = ExaltedGold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        // Lista de combatentes
        if (ordenados.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AppText(
                    "Nenhum combatente cadastrado",
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            var outerOrigin by remember { mutableStateOf(Offset.Zero) }
            val centros = remember { mutableStateMapOf<String, Offset>() }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { outerOrigin = it.positionInRoot() }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = GutterWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(ordenados, key = { _, p -> p.id }) { index, p ->
                        val ehElegivel = p.id in elegiveis
                        val atacanteAtivoId = controller.atacanteAtivoAtual()
                        val ehEmpateMaiorIniciativa = elegiveis.size > 1 && p.id in elegiveis
                        val ehAtacante = p.id == atacanteAtivoId ||
                            p.id in state.declaracoes.keys ||
                            p.id in state.declaracoesMulti.keys
                        // Em empate na maior iniciativa, antes da escolha todos ficam
                        // amarelos. Depois que um deles é escolhido, apenas o escolhido
                        // fica vermelho; os demais passam a preto/Aguardando.
                        val aguardandoEmpate = elegiveis.size > 1 &&
                            atacanteAtivoId != null &&
                            p.id in elegiveis &&
                            !ehAtacante
                        val ehAlvo = state.ehAlvoDeAlgumaDeclaracao(p.id) || p.id == state.alvoResolucaoAtual
                        val emClash = state.emClash(p.id)
                        val ehVencedor = p.id == state.vencedorClashId
                        val haAlvoSelecionado = state.declaracoes.isNotEmpty() || state.temDeclaracaoMulti()

                        ParticipanteCard(
                            participante = p,
                            ordem = index + 1,
                            ehElegivel = ehElegivel,
                            ehAtacante = ehAtacante,
                            ehEmpateMaiorIniciativa = ehEmpateMaiorIniciativa,
                            atacanteAtivoId = atacanteAtivoId,
                            ehAlvo = ehAlvo,
                            emClash = emClash,
                            ehVencedor = ehVencedor,
                            ofuscado = haAlvoSelecionado && !ehAtacante && !ehAlvo,
                            jaAgiram = p.jaAgiramNesteTurno,
                            aguardandoEmpate = aguardandoEmpate,
                            iniciativaExibida = state.iniciativasPendentes[p.id],
                            onTap = {
                                // Após travar, toques não alteram declaração (controller também bloqueia).
                                if (!travado) controller.onToqueCombatente(p.id)
                            },
                            onLongPress = {
                                // Edição de nome/iniciativa só na etapa de declaração.
                                if (!travado) menuLongPress = p
                            },
                            onPosition = { pos, size ->
                                centros[p.id] = Offset(
                                    pos.x - outerOrigin.x + size.width,
                                    pos.y - outerOrigin.y + size.height / 2f
                                )
                            }
                        )
                    }
                }

                IniciativasConnectionsCanvas(
                    declaracoes = state.declaracoes,
                    centros = centros,
                    modifier = Modifier.fillMaxSize(),
                    declaracoesMulti = state.declaracoesMulti
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- Controles exclusivos da etapa ----
        when (etapa) {
            EtapaCombate.DECLARACAO -> {
                AppText(
                    if (temDeclaracao) "Pressione [Ataque] para travar o alvo"
                    else "Toque no alvo e pressione [Ataque]",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    GildedDialogButton(
                        text = "Ataque",
                        onClick = {
                            val alvosNpc = state.declaracoes.values.mapNotNull { alvoId ->
                                state.participantes.firstOrNull { it.id == alvoId }?.origemNpcId
                            } + state.declaracoesMulti.values.flatten().mapNotNull { alvoId ->
                                state.participantes.firstOrNull { it.id == alvoId }?.origemNpcId
                            }
                            if (controller.travarAtaque()) onAtaquesTravados(alvosNpc)
                        },
                        enabled = temDeclaracao
                    )
                    if (controller.podeIniciarColisao()) {
                        GildedDialogButton(
                            text = "Colisão",
                            onClick = { controller.iniciarColisao() },
                            enabled = true
                        )
                    }
                    if (controller.podePularTurno()) {
                        GildedDialogButton(
                            text = "Pular",
                            onClick = {
                                controller.pularTurnoAtualComPenalidadesExpiradas()
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let { onClashResolved(null, it) }
                            },
                            enabled = true
                        )
                    }
                }
            }

            EtapaCombate.ESCOLHER_VENCEDOR -> {
                AppText(
                    "Escolha o vencedor da Colisão",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedAmber,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                val (idA, idB) = paresClash.first()
                val participanteA = state.participantes.firstOrNull { it.id == idA }
                val participanteB = state.participantes.firstOrNull { it.id == idB }
                if (participanteA != null && participanteB != null) {
                    SeletorVencedorClash(
                        participanteA = participanteA,
                        participanteB = participanteB,
                        onSelecionar = { vencedorId ->
                            val perdedorNpcId = controller.selecionarVencedorClash(vencedorId)
                            if (!state.colisaoAtiva && perdedorNpcId != null) onClashResolved(perdedorNpcId, emptySet())
                        }
                    )
                }
            }

            EtapaCombate.ESCOLHER_TIPO -> {
                AppText(
                    "Escolha o tipo de ataque",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedAmber,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    GildedDialogButton(
                        text = "Fulminante",
                        onClick = { controller.selecionarTipoAtaque(TipoAtaque.FULMINANTE) }
                    )
                    GildedDialogButton(
                        text = "Decisivo",
                        onClick = { controller.selecionarTipoAtaque(TipoAtaque.DECISIVO) }
                    )
                }
            }

            EtapaCombate.ESCOLHER_RESULTADO -> {
                AppText(
                    "Resultado do ataque",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedAmber,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    GildedDialogButton(
                        text = "Sucesso",
                        onClick = { controller.confirmarSucessoAtaque(true) }
                    )
                    GildedDialogButton(
                        text = "Falha",
                        onClick = { controller.confirmarSucessoAtaque(false) }
                    )
                }
            }

            EtapaCombate.AJUSTAR_FULMINANTE -> {
                val par = controller.parResolucaoAtual()
                val perdedor = par?.second
                val valorPerdedor = perdedor?.let {
                    state.iniciativasPendentes[it.id]
                        ?: state.iniciativasAntesTransferencia[it.id]
                        ?: it.iniciativa
                }
                val transferenciaBloqueada = controller.transferenciaBloqueadaPorBattleGroup()
                CaixaIniciativaPerdedor(
                    nomePerdedor = perdedor?.nome,
                    valorIniciativa = valorPerdedor,
                    quantidadeTransferida = state.contadorTransferencia,
                    ativo = true,
                    dica = if (transferenciaBloqueada) "Apenas +1 ao atacante em Próximo" else "Ajuste e pressione [Próximo] para confirmar",
                    onMais = { controller.transferirIniciativa(+1) },
                    onMenos = { controller.transferirIniciativa(-1) },
                    bloqueado = transferenciaBloqueada
                )
                Spacer(Modifier.height(6.dp))
                GildedDialogButton(
                    text = "Próximo",
                    onClick = {
                        controller.terminarAcao().takeIf { it.isNotEmpty() }?.let { onClashResolved(null, it) }
                    }
                )
            }

            EtapaCombate.CONFIRMAR_PROXIMO -> {
                val dica = when {
                    state.tipoAtaquePendente == TipoAtaque.DECISIVO && state.ataqueBemSucedido == true ->
                        "Decisivo bem-sucedido: iniciativa do atacante → 3"
                    state.tipoAtaquePendente == TipoAtaque.DECISIVO ->
                        "Decisivo falhou: iniciativa do atacante será reduzida"
                    else ->
                        "Fulminante falhou: sem transferência"
                }
                AppText(dica, style = MaterialTheme.typography.labelMedium, color = ExaltedMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                GildedDialogButton(
                    text = "Próximo",
                    onClick = {
                        controller.terminarAcao().takeIf { it.isNotEmpty() }?.let { onClashResolved(null, it) }
                    }
                )
            }

            EtapaCombate.RESOLUCAO_MULTI -> {
                AppText(
                    "Ajuste a transferência deste alvo e confirme Sucesso ou Falha",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                val transferenciaBloqueada = controller.transferenciaBloqueadaPorBattleGroup()
                ControlesTransferencia(
                    contador = state.contadorTransferencia,
                    ativo = true,
                    dica = if (transferenciaBloqueada) "Apenas +1 ao atacante ao confirmar este alvo" else null,
                    onMais = { controller.transferirIniciativa(+1) },
                    onMenos = { controller.transferirIniciativa(-1) },
                    bloqueado = transferenciaBloqueada
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    GildedDialogButton(
                        text = "Sucesso",
                        onClick = { controller.confirmarAlvoMultiEAvancar(true) }
                    )
                    GildedDialogButton(
                        text = "Falha",
                        onClick = { controller.confirmarAlvoMultiEAvancar(false) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Permanente: Adicionar combatente
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            InkButton(
                onClick = { mostrarCadastro = true },
                modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, "Adicionar combatente", tint = ExaltedGold)
            }
        }
    }

    if (mostrarCadastro) {
        DialogAdicionar(onDismiss = { mostrarCadastro = false }) { nome, init ->
            controller.adicionarParticipante(nome, init)
            mostrarCadastro = false
        }
    }

    if (mostrarHistorico) {
        DialogHistoricoCombates(historico = historico, onDismiss = { mostrarHistorico = false })
    }

    if (mostrarTermino) {
        DialogConfirmarTermino(
            resolucaoPendente = travado ||
                state.tipoAtaquePendente != null ||
                state.ataqueBemSucedido != null ||
                state.iniciativasPendentes.isNotEmpty(),
            onCancelar = { mostrarTermino = false },
            onConfirmar = {
                mostrarTermino = false
                onEncerrarCombate()
            }
        )
    }

    // Menus de edição só na declaração (não abrem se travado)
    if (!travado) {
        menuLongPress?.let { p ->
            AlertDialog(
                onDismissRequest = { menuLongPress = null },
                title = {
                    CombatNameText(
                        p.nome,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        InkButton(label = "Editar nome", onClick = { editarNomeDe = p; menuLongPress = null }, size = InkButtonSize.Small)
                        InkButton(label = "Editar iniciativa", onClick = { editarInitDe = p; menuLongPress = null }, size = InkButtonSize.Small)
                        InkButton(label = "Remover", onClick = { controller.removerParticipante(p.id); menuLongPress = null }, size = InkButtonSize.Small, variant = InkButtonVariant.Danger)
                    }
                },
                confirmButton = {
                    InkButton(label = "Fechar", onClick = { menuLongPress = null }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                }
            )
        }

        editarNomeDe?.let { p ->
            var texto by remember(p.id) { mutableStateOf(p.nome) }
            AlertDialog(
                onDismissRequest = { editarNomeDe = null },
                title = {
                    AppText("Editar nome", color = ExaltedAccentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.take(40) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ExaltedAccentBright,
                            focusedTextColor = ExaltedOnSurface,
                            unfocusedTextColor = ExaltedOnSurface
                        )
                    )
                },
                dismissButton = {
                    InkButton(label = "Salvar", onClick = { controller.editarNome(p.id, texto); editarNomeDe = null }, size = InkButtonSize.Small)
                },
                confirmButton = {
                    InkButton(label = "Cancelar", onClick = { editarNomeDe = null }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                }
            )
        }

        editarInitDe?.let { p ->
            var valor by remember(p.id) { mutableIntStateOf(p.iniciativa) }
            AlertDialog(
                onDismissRequest = { editarInitDe = null },
                title = {
                    AppText(
                        "Editar iniciativa — ${p.nome}",
                        color = ExaltedAccentBright,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InkButton(onClick = { valor -= 1 },
    modifier = Modifier
) {
                            Icon(Icons.Default.Remove, null, tint = ExaltedAmber)
                        }
                        AppText(
                            valor.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExaltedAccentBright,
                            modifier = Modifier.width(64.dp),
                            textAlign = TextAlign.Center
                        )
                        InkButton(onClick = { valor += 1 },
    modifier = Modifier
) {
                            Icon(Icons.Default.Add, null, tint = ExaltedAmber)
                        }
                    }
                },
                dismissButton = {
                    InkButton(label = "Salvar", onClick = { controller.definirIniciativa(p.id, valor); editarInitDe = null }, size = InkButtonSize.Small)
                },
                confirmButton = {
                    InkButton(label = "Cancelar", onClick = { editarInitDe = null }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                }
            )
        }
    }

    if (mensagem != null) {
        AlertDialog(
            onDismissRequest = { controller.consumirMensagem() },
            title = {
                StatusEtiqueta(texto = "Aviso", fundo = CorEtiquetaMetalica)
            },
            text = {
                AppText(
                    mensagem,
                    color = ExaltedOnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                InkButton(label = "OK", onClick = { controller.consumirMensagem() }, size = InkButtonSize.Small)
            }
        )
    }
}
