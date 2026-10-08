package com.example.iniciativas

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/**
 * Aba 12 (Conflito) — fluxo com Clash, Colisão e Initiative Shift.
 *
 * Bônus de sistema (+3 Clash, +5 Crash, recuperação de Shift)
 * são aplicados somente em [Término].
 */
class IniciativasController(initialState: IniciativasState = IniciativasState()) {

    // internal: acessado pelas extensões em IniciativasControllerMutations.kt
    internal val nextInsertionOrder = AtomicLong(
        initialState.participantes.maxOfOrNull { it.ordemInsercao } ?: 0L
    )

    internal val _state = MutableStateFlow(initialState)
    val state: StateFlow<IniciativasState> = _state.asStateFlow()

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    // PERFORMANCE: a UI da Aba 12 pode recompor várias vezes sem que a lista
    // de participantes tenha mudado. A ordenação é O(n log n); reutilizamos
    // o resultado enquanto a lista imutável de participantes for a mesma
    // instância. Qualquer mutação válida cria uma nova lista e invalida o cache.
    private data class OrdenadosCache(
        val origem: List<ParticipanteIniciativa>,
        val resultado: List<ParticipanteIniciativa>
    )

    // Um único holder volátil torna a leitura/escrita do par (origem, resultado)
    // atômica para os leitores. Dois campos voláteis independentes permitiriam
    // uma janela em que uma thread enxergasse a nova origem acompanhada do
    // resultado antigo.
    @Volatile private var participantesOrdenadosCache: OrdenadosCache? = null

    // PERFORMANCE: idsElegiveis() é consultado em cada toque de combatente e
    // também pela declaração direta. A regra percorre toda a lista para achar
    // a maior iniciativa e depois percorre novamente para materializar os IDs.
    // Como toda mutação válida substitui a lista de participantes, podemos
    // compartilhar esse resultado pelo mesmo ciclo de vida da lista.
    private data class ElegiveisCache(
        val origem: List<ParticipanteIniciativa>,
        val participantes: List<ParticipanteIniciativa>,
        val ids: Set<String>
    )

    @Volatile private var elegiveisCache: ElegiveisCache? = null

    // Cache por identidade do mapa: as declarações são imutáveis e cada mutação
    // válida cria um novo Map. Isso evita recalcular os pares de Clash várias
    // vezes durante a mesma recomposição/ação.
    internal data class ParesClashCache(
        val origem: Map<String, String>,
        val resultado: List<Pair<String, String>>
    )

    @Volatile internal var paresClashCache: ParesClashCache? = null

    fun participantesOrdenados(): List<ParticipanteIniciativa> {
        val participantes = _state.value.participantes
        participantesOrdenadosCache?.let { cache ->
            if (cache.origem === participantes) return cache.resultado
        }
        val ordenados = IniciativasRules.ordenar(participantes)
        participantesOrdenadosCache = OrdenadosCache(participantes, ordenados)
        return ordenados
    }

    /**
     * Combatentes elegíveis do slot atual: maior iniciativa entre os que
     * ainda não agiram. Empatados agem “ao mesmo tempo”.
     */
    fun elegiveisAtuais(): List<ParticipanteIniciativa> = elegiveisCacheAtual().participantes

    fun idsElegiveis(): Set<String> = elegiveisCacheAtual().ids

    private fun elegiveisCacheAtual(): ElegiveisCache {
        val participantes = _state.value.participantes
        elegiveisCache?.let { cache ->
            if (cache.origem === participantes) return cache
        }

        val resultado = IniciativasRules.elegiveisComIds(participantes)
        return ElegiveisCache(participantes, resultado.participantes, resultado.ids)
            .also { elegiveisCache = it }
    }

    /**
     * Regra única usada pela UI para habilitar [Colisão] e pelo controlador
     * para validar sua execução. Evita duplicação da regra de negócio.
     */
    fun podeIniciarColisao(): Boolean = encontrarParElegivelParaColisao(_state.value) != null

    fun isBattleGroup(participanteId: String): Boolean =
        _state.value.participantes.any {
            it.id == participanteId && it.origemBattleGroupId != null
        }

    /**
     * Alterna um alvo na declaração multi de um Battle Group — pedido
     * explícito do usuário (Parte A). Um Battle Group pode declarar vários
     * alvos no mesmo slot; cada toque em um alvo já escolhido remove-o,
     * e um toque em alvo novo adiciona-o. Personagens comuns continuam
     * usando o mapa `declaracoes` (1 alvo só), nunca este.
     */
    fun alternarAlvoMulti(atacanteId: String, alvoId: String) {
        if (_state.value.ataqueTravado) return
        if (atacanteId == alvoId) return
        if (!isBattleGroup(atacanteId)) return
        _state.update { st ->
            val atuais = st.declaracoesMulti[atacanteId].orEmpty()
            val novosAlvos = if (alvoId in atuais) atuais - alvoId else atuais + alvoId
            val novasMulti = st.declaracoesMulti.toMutableMap()
            if (novosAlvos.isEmpty()) novasMulti.remove(atacanteId) else novasMulti[atacanteId] = novosAlvos
            st.copy(
                declaracoesMulti = novasMulti,
                declaracoes = st.declaracoes - atacanteId
            )
        }
    }

    // ------------------------------------------------------------------
    // Adicionar / remover / editar — extraído para IniciativasControllerMutations.kt
    // (adicionarParticipante, adicionarOuAtualizarNpc, adicionarOuAtualizarBattleGroup,
    //  remover*, limparMultiPorRemocao, removerDeclaracoesEnvolvendo,
    //  atualizarPorOrigemNpcId, editarNome, definirIniciativa, ajustarIniciativa)
    // ------------------------------------------------------------------

    internal var pendenteDeclaranteId: String? = null

    fun atacanteAtivoAtual(): String? {
        val elegiveis = idsElegiveis()
        if (elegiveis.isEmpty()) return null
        // Único elegível: atacante automático (sem depender da ordem da tabela).
        if (elegiveis.size == 1) return elegiveis.first()
        val pendente = pendenteDeclaranteId
        return if (pendente != null && pendente in elegiveis) pendente else null
    }

    fun onToqueCombatente(id: String) {
        val current = _state.value
        if (current.ataqueTravado) {
            // A escolha do vencedor é feita exclusivamente pelos botões
            // de Colisão na interface.
            return
        }

        val elegiveis = idsElegiveis()
        if (elegiveis.isEmpty()) return

        // Toque no elegível que já declarou → cancela a declaração dele.
        if (id in elegiveis && current.declaracoes.containsKey(id)) {
            _state.update { st -> st.copy(declaracoes = st.declaracoes - id) }
            // Mantém como atacante ativo para escolher outro alvo.
            pendenteDeclaranteId = id
            return
        }

        if (id in elegiveis) {
            val ativo = atacanteAtivoAtual()
            when {
                // Ainda não há atacante ativo → este toque o elege, entre os
                // empatados. CORREÇÃO: a troca de atacante NÃO acontece mais
                // só por o atacante ativo ainda não ter declarado alvo — a
                // condição extra permitia burlar a declaração de alvo contra
                // um combatente empatado (ver doc da função).
                ativo == null -> {
                    pendenteDeclaranteId = id
                    return
                }
                // Atacante ativo toca em outro elegível (empatado) → declara
                // alvo (prepara Colisão), sem restrição — igual já acontecia
                // para alvos de iniciativa diferente.
                ativo != id -> {
                    _state.update { st ->
                        val novas = st.declaracoes.toMutableMap()
                        if (novas[ativo] == id) novas.remove(ativo) else novas[ativo] = id
                        st.copy(declaracoes = novas)
                    }
                    return
                }
                // Toque no próprio atacante ativo sem declaração → no-op.
                else -> return
            }
        }

        // Toque em não-elegível → alvo do atacante ativo (único ou já escolhido).
        val ativo = atacanteAtivoAtual() ?: return
        if (isBattleGroup(ativo)) {
            alternarAlvoMulti(ativo, id)
            return
        }
        _state.update { st ->
            val novas = st.declaracoes.toMutableMap()
            if (novas[ativo] == id) novas.remove(ativo) else novas[ativo] = id
            st.copy(declaracoes = novas)
        }
    }

    /** Atalho: define declaração direta (útil para testes / UI alternativa). */
    fun declararAtaque(atacanteId: String, alvoId: String?) {
        if (_state.value.ataqueTravado) return
        val elegiveis = idsElegiveis()
        if (atacanteId !in elegiveis) return
        if (isBattleGroup(atacanteId) && alvoId != null) {
            alternarAlvoMulti(atacanteId, alvoId)
            return
        }
        _state.update { st ->
            val atual = st.declaracoes[atacanteId]
            val novoAlvo = alvoId?.takeUnless { it == atacanteId }
            if (atual == novoAlvo) return@update st
            val novas = st.declaracoes.toMutableMap()
            if (novoAlvo == null) novas.remove(atacanteId) else novas[atacanteId] = novoAlvo
            st.copy(
                declaracoes = novas,
                turnStatus = if (novas.isEmpty()) TurnStatus.INICIADO else TurnStatus.ALVO_SELECIONADO
            )
        }
    }

    // ------------------------------------------------------------------
    // [Ataque] — trava: extraído para IniciativasControllerClash.kt
    // (travarAtaque, paresClashAtuais, iniciarColisao, selecionarVencedorClash)
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // + / – transferência ao vivo
    // ------------------------------------------------------------------

    /**
     * delta > 0: tira do “alvo” e dá à “origem”.
     * delta < 0: inverso.
     *
     * Clash com vencedor escolhido:
     *   origem = vencedor, alvo = o outro do par.
     * Sem Clash / declaração simples:
     *   origem = declarantes, alvo = declarado (primeira declaração).
     */
    fun transferirIniciativa(delta: Int) {
        if (delta == 0) return
        _state.update { current ->
            if (!current.ataqueTravado) return@update current
            // Só permite ajuste em Fulminante bem-sucedido (PDF §5.1).
            if (current.tipoAtaquePendente != TipoAtaque.FULMINANTE || current.ataqueBemSucedido != true) {
                return@update current
            }

            val (origemId, alvoId) = resolverParTransferencia(current) ?: return@update current
            val origem = current.participantes.firstOrNull { it.id == origemId } ?: return@update current
            val alvo = current.participantes.firstOrNull { it.id == alvoId } ?: return@update current
            // Battle Group contra Battle Group: não existe transferência de
            // iniciativa. O único ganho permitido ao atacante é o +1 fixo
            // aplicado no fechamento pelo botão Próximo.
            if (origem.origemBattleGroupId != null && alvo.origemBattleGroupId != null) {
                return@update current
            }

            // Base = snapshot no travamento (ou valor atual se ausente).
            val baseOrigem = current.iniciativasAntesTransferencia[origemId] ?: origem.iniciativa
            val baseAlvo = current.iniciativasAntesTransferencia[alvoId] ?: alvo.iniciativa
            val novoContador = current.contadorTransferencia + delta

            // PDF §5.2 / §7: valores ficam temporários em iniciativasPendentes.
            // A lista ordenada NÃO muda até o botão Próximo.
            // Battle Group como alvo: não reduz iniciativa (dano manual na Aba 13).
            val pendentes = current.iniciativasPendentes.toMutableMap()
            pendentes[origemId] = baseOrigem + novoContador
            if (alvo.origemBattleGroupId == null) {
                pendentes[alvoId] = baseAlvo - novoContador
            } else {
                pendentes.remove(alvoId)
            }

            current.copy(
                contadorTransferencia = novoContador,
                iniciativasPendentes = pendentes,
                turnStatus = TurnStatus.EM_RESOLUCAO
            )
        }
    }

    
    private fun resolverParTransferencia(current: IniciativasState): Pair<String, String>? {
        if (current.atacanteMultiId != null && current.alvoResolucaoAtual != null) {
            return current.atacanteMultiId to current.alvoResolucaoAtual
        }
        val pares = paresClashDaState(current)
        if (pares.isNotEmpty()) {
            val (a, b) = pares.first()
            val vencedor = current.vencedorClashId ?: return null // precisa tocar no vencedor
            val perdedor = if (vencedor == a) b else if (vencedor == b) a else return null
            return vencedor to perdedor
        }
        // Declaração simples (primeira)
        val (origem, alvo) = current.declaracoes.entries.firstOrNull() ?: return null
        return origem to alvo
    }

    /**
     * Confirma resultado do alvo corrente da fila multi e avança.
     * Avaliação de Crash (+5) e Initiative Shift POR ALVO INDIVIDUAL —
     * pedido explícito do usuário (mais fiel à regra do que avaliar só
     * uma vez no final). sucesso=false reverte o delta acumulado
     * (contadorTransferencia) só para aquele alvo, sem aplicar Crash/Shift.
     * Fila vazia ao final → grupo jaAgiramNesteTurno=true, limpa slot multi.
     */
    fun confirmarAlvoMultiEAvancar(sucesso: Boolean): Boolean {
        var avancou = false
        _state.update { current ->
            val atacanteId = current.atacanteMultiId ?: return@update current
            val alvoId = current.alvoResolucaoAtual ?: return@update current
            avancou = true

            var participantes = current.participantes
            val mensagens = current.mensagensPendentes.toMutableList()
            var eventosCrash = current.eventosCrashAcumulados

            val atacante = participantes.firstOrNull { it.id == atacanteId }
            val alvo = participantes.firstOrNull { it.id == alvoId }
            val bloqueioBattleGroup = atacante?.origemBattleGroupId != null &&
                alvo?.origemBattleGroupId != null &&
                (current.tipoAtaquePendente == null || current.tipoAtaquePendente == TipoAtaque.FULMINANTE)

            if (!sucesso) {
                if (current.contadorTransferencia != 0) {
                    // Reverte o delta aplicado a este par específico — sem
                    // sucesso, nenhum dano fica.
                    participantes = participantes.map { p ->
                        when (p.id) {
                            atacanteId -> p.copy(iniciativa = p.iniciativa - current.contadorTransferencia)
                            alvoId -> p.copy(iniciativa = p.iniciativa + current.contadorTransferencia)
                            else -> p
                        }
                    }
                }
            } else if (bloqueioBattleGroup) {
                // Battle Group contra Battle Group: nenhum ponto é drenado do
                // alvo. O atacante recebe somente o +1 fixo do Fulminante
                // bem-sucedido ao confirmar este alvo com [Próximo].
                participantes = participantes.map { p ->
                    if (p.id == atacanteId) p.copy(iniciativa = p.iniciativa + 1) else p
                }
            } else if (current.contadorTransferencia != 0) {
                // Avaliação de Crash/Shift POR ESTE ALVO — pedido explícito
                // do usuário. O snapshot original (capturado em travarAtaque)
                // ainda vale como "antes" pra este alvo, já que cada alvo é
                // visitado só uma vez na fila e só a própria transferência
                // dele altera sua iniciativa.
                val alvoAntes = current.iniciativasAntesTransferencia[alvoId]
                    ?: participantes.firstOrNull { it.id == alvoId }?.iniciativa ?: 0
                val alvoIndice = participantes.indexOfFirst { it.id == alvoId }
                if (alvoIndice >= 0) {
                    val alvoDepois = participantes[alvoIndice].iniciativa
                    val crashouAgora = alvoAntes > 0 && alvoDepois <= 0
                    if (crashouAgora) {
                        val listaMut = participantes.toMutableList()
                        val alvoAtual = listaMut[alvoIndice]
                        eventosCrash = eventosCrash + alvoAtual.nome
                        listaMut[alvoIndice] = alvoAtual.copy(
                            iniciativa = alvoAtual.iniciativa + 5,
                            reduzidoPorId = atacanteId
                        )
                        // Initiative Shift: se o atacante estava em Crash
                        // causado por este mesmo alvo, recupera-se agora.
                        val atacanteIndice = listaMut.indexOfFirst { it.id == atacanteId }
                        if (atacanteIndice >= 0) {
                            val atacanteAtual = listaMut[atacanteIndice]
                            if (atacanteAtual.reduzidoPorId == alvoId && atacanteAtual.iniciativa <= 0) {
                                listaMut[atacanteIndice] = atacanteAtual.copy(
                                    iniciativa = maxOf(atacanteAtual.iniciativa, 3),
                                    reduzidoPorId = null
                                )
                                mensagens += "Initiative Shift: ${atacanteAtual.nome} recupera-se do Crash."
                            }
                        }
                        participantes = listaMut
                    }
                }
                // Aviso manual (Parte C.3, opção escolhida pelo usuário):
                // Battle Group como alvo não tem a iniciativa reduzida por
                // este caminho (regra 2.2) — o jogador aplica o dano na
                // Magnitude manualmente, na Aba 13.
                val alvoParticipante = participantes.firstOrNull { it.id == alvoId }
                if (alvoParticipante?.origemBattleGroupId != null) {
                    mensagens += "${alvoParticipante.nome} sofreu ${current.contadorTransferencia} de dano. Aplique manualmente na Magnitude, na Aba 13."
                }
            }

            val restante = current.filaResolucaoMulti.filterNot { it == alvoId }
            val proximoAlvo = restante.firstOrNull()

            // CORREÇÃO — log de combate: mesmo princípio do terminarAcao()
            // (Próximo em ataque simples), agora aplicado à confirmação de
            // cada alvo individual de um Battle Group. Construído com
            // "current" ainda intacto (atacanteMultiId/alvoResolucaoAtual
            // desta rodada específica), antes de current.copy(...) abaixo
            // limpar/avançar esses campos.
            val snapshotAlvo = montarLogTurno(current, TurnStatus.RESOLVIDO)

            if (proximoAlvo == null) {
                // Fila vazia: grupo já agiu, limpa slot multi.
                val comAcao = participantes.map {
                    if (it.id == atacanteId) it.copy(jaAgiramNesteTurno = true) else it
                }
                current.copy(
                    participantes = comAcao,
                    declaracoesMulti = current.declaracoesMulti - atacanteId,
                    filaResolucaoMulti = emptyList(),
                    alvoResolucaoAtual = null,
                    atacanteMultiId = null,
                    ataqueTravado = false,
                    contadorTransferencia = 0,
                    iniciativasAntesTransferencia = emptyMap(),
                    mensagensPendentes = mensagens,
                    eventosCrashAcumulados = eventosCrash,
                    eventosLog = current.eventosLog + snapshotAlvo,
                    tipoAtaquePendente = null,
                    ataqueBemSucedido = null
                )
            } else {
                current.copy(
                    participantes = participantes,
                    filaResolucaoMulti = restante,
                    alvoResolucaoAtual = proximoAlvo,
                    contadorTransferencia = 0,
                    mensagensPendentes = mensagens,
                    eventosCrashAcumulados = eventosCrash,
                    eventosLog = current.eventosLog + snapshotAlvo
                )
            }
        }
        if (avancou) pendenteDeclaranteId = null
        return avancou
    }

    /**
     * Battle Group contra Battle Group em Fulminante bem-sucedido: a iniciativa
     * do alvo não pode ser drenada. Nesse caso o atacante recebe apenas o +1
     * fixo quando [Próximo] confirma o sucesso.
     */
    fun transferenciaBloqueadaPorBattleGroup(): Boolean {
        val current = _state.value
        if (current.ataqueBemSucedido != true) {
            return false
        }
        val (origemId, alvoId) = if (current.atacanteMultiId != null && current.alvoResolucaoAtual != null) {
            current.atacanteMultiId to current.alvoResolucaoAtual
        } else {
            resolverParTransferencia(current) ?: return false
        }
        val origem = current.participantes.firstOrNull { it.id == origemId } ?: return false
        val alvo = current.participantes.firstOrNull { it.id == alvoId } ?: return false
        return origem.origemBattleGroupId != null && alvo.origemBattleGroupId != null
    }

    fun emResolucaoMulti(): Boolean =
        _state.value.ataqueTravado &&
        _state.value.atacanteMultiId != null &&
        _state.value.alvoResolucaoAtual != null

    /** Escolhe Fulminante ou Decisivo pro ataque travado atual — pedido
     * explícito do usuário (Parte C). Só permitido depois de [Ataque]/
     * [Colisão] travarem, e (se houver Clash) só depois do vencedor ser
     * escolhido — a mesma dupla precisa estar resolvida antes de perguntar
     * o tipo. */
    fun selecionarTipoAtaque(tipo: TipoAtaque) {
        _state.update { current ->
            if (!current.ataqueTravado) return@update current
            if (current.tipoAtaquePendente != null) return@update current

            // Colisão: o resultado é sempre sucesso. Não existe a etapa
            // Sucesso/Falha para esse fluxo. O efeito do tipo escolhido
            // é aplicado imediatamente: Fulminante abre o contador de
            // transferência; Decisivo fixa a iniciativa do vencedor em 3.
            if (current.colisaoAtiva) {
                val vencedorId = current.vencedorClashId ?: return@update current
                if (tipo == TipoAtaque.DECISIVO) {
                    val vencedor = current.participantes.firstOrNull { it.id == vencedorId }
                        ?: return@update current
                    val base = current.iniciativasAntesTransferencia[vencedorId] ?: vencedor.iniciativa
                    return@update current.copy(
                        tipoAtaquePendente = tipo,
                        ataqueBemSucedido = true,
                        iniciativasPendentes = current.iniciativasPendentes + (vencedorId to 3),
                        turnStatus = TurnStatus.EM_RESOLUCAO
                    )
                }
                return@update current.copy(
                    tipoAtaquePendente = tipo,
                    ataqueBemSucedido = true,
                    turnStatus = TurnStatus.EM_RESOLUCAO
                )
            }

            current.copy(
                tipoAtaquePendente = tipo,
                turnStatus = TurnStatus.EM_RESOLUCAO
            )
        }
    }

    
    fun confirmarSucessoAtaque(sucesso: Boolean) {
        _state.update { current ->
            if (current.colisaoAtiva) return@update current
            val tipo = current.tipoAtaquePendente ?: return@update current
            if (current.ataqueBemSucedido != null) return@update current

            if (tipo == TipoAtaque.DECISIVO) {
                val (atacanteId, _) = resolverParTransferencia(current) ?: return@update current
                val atacante = current.participantes.firstOrNull { it.id == atacanteId } ?: return@update current
                // PDF §6.1 / §6.2: iniciativa do atacante muda; perdedor intacto.
                // PDF §7: valor permanece temporário até Próximo.
                val base = current.iniciativasAntesTransferencia[atacanteId] ?: atacante.iniciativa
                val novaIniciativa = when {
                    sucesso -> 3
                    // PDF: atual <= 10 → -2; atual >= 11 → -3
                    base <= 10 -> base - 2
                    else -> base - 3
                }
                current.copy(
                    ataqueBemSucedido = sucesso,
                    iniciativasPendentes = current.iniciativasPendentes + (atacanteId to novaIniciativa),
                    turnStatus = TurnStatus.EM_RESOLUCAO
                )
            } else {
                // Fulminante: sucesso abre a caixa de transferência; falha não transfere.
                current.copy(
                    ataqueBemSucedido = sucesso,
                    turnStatus = TurnStatus.EM_RESOLUCAO
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // [Término] — extraído para IniciativasControllerTermino.kt
    // (terminarAcao, pularTurnoAtual, podePularTurno)
    // ------------------------------------------------------------------

    internal companion object {
        const val MAX_NOME = 120

        /** Normaliza nome de participante: trim + limite de tamanho.
         *  internal: usado pelas extensões em IniciativasControllerMutations.kt */
        fun limparNome(nome: String): String = nome.trim().take(MAX_NOME)
    }

    // ------------------------------------------------------------------
    // Mensagens
    // ------------------------------------------------------------------

    /** Encerra o combate por completo — remove todos os participantes e
     * reseta rodada, declarações e clash. Diferente de terminarAcao(),
     * que só finaliza a ação/turno em andamento. Retorna o registro de
     * histórico deste combate (null se não havia ninguém em combate). */
    fun encerrarCombate(): HistoricoCombateEntry? {
        // PDF §10: única forma de encerrar; resolução pendente → INCOMPLETO.
        while (true) {
            val atual = _state.value
            if (atual.participantes.isEmpty()) {
                pendenteDeclaranteId = null
                return null
            }

            val pendente = atual.ataqueTravado ||
                atual.tipoAtaquePendente != null ||
                atual.ataqueBemSucedido != null ||
                atual.iniciativasPendentes.isNotEmpty()

            val turnFinal = if (pendente) TurnStatus.INCOMPLETO else TurnStatus.ENCERRADO
            // CORREÇÃO — log de combate sempre vazio: antes só existia este
            // ÚNICO snapshot, montado aqui em cima do estado "atual" no
            // instante do encerramento. No caso comum (jogador já resolveu
            // tudo antes de encerrar — "pendente" acima é false), esse
            // snapshot não tinha ativo/alvo nenhum pra registrar (tudo já
            // tinha sido limpo pelo último terminarAcao()) — daí o log saía
            // vazio de fato. Agora usamos eventosLog, acumulado a cada turno
            // resolvido (terminarAcao / confirmarAlvoMultiEAvancar) durante
            // TODO o combate, e só acrescentamos este snapshot extra quando
            // havia algo de fato incompleto/interrompido pelo encerramento.
            val eventosFinais = if (pendente) {
                atual.eventosLog + montarLogTurno(atual, turnFinal, motivoEncerramento = "encerrado_manualmente")
            } else {
                atual.eventosLog
            }

            val registro = HistoricoCombateEntry(
                dataHora = System.currentTimeMillis(),
                participantes = atual.participantes.map {
                    ParticipanteHistorico(id = it.id, nome = it.nome, iniciativaFinal = it.iniciativa)
                },
                rodadasTotais = atual.rodada,
                eventosCrash = atual.eventosCrashAcumulados,
                turnStatus = turnFinal,
                combatStatus = CombatStatus.ENCERRADO,
                eventosLog = eventosFinais
            )
            // Novo combate começa limpo (status padrão do IniciativasState).
            if (_state.compareAndSet(atual, IniciativasState())) {
                pendenteDeclaranteId = null
                return registro
            }
        }
    }

    /**
     * Monta o snapshot estruturado do turno para o log (PDF §11).
     */
    internal fun montarLogTurno(
        state: IniciativasState,
        turnStatus: TurnStatus,
        motivoEncerramento: String? = null
    ): LogTurnoSnapshot {
        val porId = state.participantes.associateBy { it.id }
        val ativoId = atacanteAtivoAtual()
            ?: state.declaracoes.keys.firstOrNull()
            ?: state.atacanteMultiId
        val alvoId = state.declaracoes[ativoId]
            ?: state.alvoResolucaoAtual
            ?: state.declaracoes.values.firstOrNull()

        var vencedorId = state.vencedorClashId
        var perdedorId: String? = null
        if (vencedorId != null) {
            val pares = paresClashDaState(state)
            pares.firstOrNull { vencedorId == it.first || vencedorId == it.second }?.let { (a, b) ->
                perdedorId = if (vencedorId == a) b else a
            }
        } else if (ativoId != null && alvoId != null && state.ataqueBemSucedido == true) {
            // Sem Clash: vencedor = atacante, perdedor = alvo em sucesso.
            vencedorId = ativoId
            perdedorId = alvoId
        }

        val ponto = state.tipoAtaquePendente == TipoAtaque.FULMINANTE && state.ataqueBemSucedido == true

        // A fonte canônica do "antes" já existe no estado: ela é capturada
        // quando o ataque é travado e permanece válida até a resolução.
        // Não criar um segundo snapshot paralelo: além de duplicar estado,
        // isso faria o log divergir da mesma base usada pelas regras de
        // Fulminante/Decisivo/Crash.
        val iniciativasAnteriores = state.iniciativasAntesTransferencia
        val iniciativaPerdida = if (iniciativasAnteriores.isEmpty()) {
            emptyMap()
        } else {
            buildMap {
                iniciativasAnteriores.forEach { (id, anterior) ->
                    val ajustada = state.iniciativasPendentes[id]
                        ?: porId[id]?.iniciativa
                        ?: anterior
                    val perda = anterior - ajustada
                    if (perda > 0) put(id, perda)
                }
            }
        }

        return LogTurnoSnapshot(
            rodada = state.rodada,
            combatenteAtivoId = ativoId,
            combatenteAtivoNome = ativoId?.let { porId[it]?.nome },
            alvoId = alvoId,
            alvoNome = alvoId?.let { porId[it]?.nome },
            tipoAtaque = state.tipoAtaquePendente,
            resultadoSucesso = state.ataqueBemSucedido,
            vencedorId = vencedorId,
            vencedorNome = vencedorId?.let { porId[it]?.nome },
            perdedorId = perdedorId,
            perdedorNome = perdedorId?.let { porId[it]?.nome },
            iniciativasAnteriores = iniciativasAnteriores,
            iniciativasAjustadas = state.iniciativasPendentes,
            iniciativaPerdidaPorId = iniciativaPerdida,
            quantidadeTransferida = state.contadorTransferencia,
            pontoFulminanteConcedido = ponto && turnStatus == TurnStatus.RESOLVIDO,
            turnStatus = turnStatus,
            motivoEncerramento = motivoEncerramento
        )
    }

    /**
     * Resolve o par (vencedor/origem, perdedor/alvo) para a caixa de iniciativa do perdedor.
     * Null se ainda não for possível (ex.: Clash sem vencedor escolhido).
     */
    fun parResolucaoAtual(): Pair<ParticipanteIniciativa, ParticipanteIniciativa>? {
        val current = _state.value
        val (origemId, alvoId) = resolverParTransferencia(current) ?: return null
        val origem = current.participantes.firstOrNull { it.id == origemId } ?: return null
        val alvo = current.participantes.firstOrNull { it.id == alvoId } ?: return null
        return origem to alvo
    }


    
    fun consumirMensagem() {
        _state.update { current ->
            val mensagens = current.mensagensPendentes
            when (mensagens.size) {
                0 -> current
                1 -> current.copy(mensagensPendentes = emptyList())
                else -> {
                    // drop(1) criava uma lista temporária através de uma
                    // transformação genérica. Aqui copiamos diretamente os
                    // elementos restantes, mantendo a mesma semântica.
                    val restantes = ArrayList<String>(mensagens.size - 1)
                    for (i in 1 until mensagens.size) restantes += mensagens[i]
                    current.copy(mensagensPendentes = restantes)
                }
            }
        }
    }
}
