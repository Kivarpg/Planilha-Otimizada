package com.example.iniciativas

/**
 * Resolve o fechamento de uma ação da Aba 12 sem depender de UI ou StateFlow.
 *
 * Esta separação é intencionalmente agressiva: regras de Clash, Crash, Shift,
 * recuperação e avanço de rodada ficam em uma função pura/testável.
 */
object IniciativasTurnResolver {

    data class Resultado(
        val state: IniciativasState,
        val penalidadesExpiradasNpcIds: Set<String>
    )

    /**
     * Aplica [IniciativasState.iniciativasPendentes] sobre a lista de
     * participantes (PDF §5.3 / §7): só o Próximo grava definitivamente.
     */
    private fun aplicarPendentes(participantes: List<ParticipanteIniciativa>, pendentes: Map<String, Int>): List<ParticipanteIniciativa> {
        if (pendentes.isEmpty()) return participantes
        return participantes.map { p ->
            val nova = pendentes[p.id]
            if (nova != null && nova != p.iniciativa) p.copy(iniciativa = nova) else p
        }
    }

    fun resolver(current: IniciativasState): Resultado {
        // Decisivo (sucesso ou fracasso) e Fulminante malsucedido não
        // passam por Clash bonus/Crash/Initiative Shift — pedido explícito
        // do usuário (Parte C): a iniciativa do Decisivo já foi definida
        // na hora da resposta (selecionarTipoAtaque/confirmarSucessoAtaque);
        // Fulminante malsucedido não aplica dano algum.
        if (current.tipoAtaquePendente == TipoAtaque.DECISIVO) {
            return resolverDecisivoOuFulminanteMalsucedido(current)
        }
        if (current.tipoAtaquePendente == TipoAtaque.FULMINANTE && current.ataqueBemSucedido == false) {
            return resolverDecisivoOuFulminanteMalsucedido(current)
        }

        val penalidadesExpiradasNpcIds = linkedSetOf<String>()

            if (current.declaracoes.isEmpty() && !current.ataqueTravado) {
                // Permite pular ação de elegível sem declaração? Não — exige algo.
                return Resultado(current, penalidadesExpiradasNpcIds)
            }

            // PDF §7: grava iniciativas pendentes só agora (botão Próximo).
            var lista = aplicarPendentes(current.participantes, current.iniciativasPendentes)
            val snapshot = current.iniciativasAntesTransferencia
            val mensagens = current.mensagensPendentes.toMutableList()
            // Cache das relações do ataque: o mesmo resultado era recalculado
            // várias vezes durante o fechamento da ação.
            val paresClash = current.paresClash()
            // PERFORMANCE: o causador de Crash era recalculado para cada vítima,
            // incluindo busca linear no par/declaracao. Materializamos a relação
            // uma única vez para o fechamento desta ação.
            val causadorPorVitima = HashMap<String, String>(2)
            val vencedorClash = current.vencedorClashId
            if (vencedorClash != null && paresClash.isNotEmpty()) {
                paresClash.firstOrNull { vencedorClash == it.first || vencedorClash == it.second }?.let { (a, b) ->
                    if (vencedorClash == a) causadorPorVitima[b] = a
                    else if (vencedorClash == b) causadorPorVitima[a] = b
                }
            } else {
                current.declaracoes.entries.firstOrNull()?.let {
                    causadorPorVitima[it.value] = it.key
                    causadorPorVitima[it.key] = it.value
                }
            }

            // --- Crash (+5) e Initiative Shift ---
            // Identifica os crashados em uma única passagem. O causador já está
            // materializado por vítima, então não precisamos de um segundo Map
            // apenas para guardar reduzidoPorId.
            val crashadoresParaBonus = HashSet<String>()
            val novosEventosCrash = ArrayList<String>()
            // O segundo passe precisa localizar o participante causador. Em vez
            // de repetir uma busca linear na lista para cada vítima de Crash,
            // materializamos o índice por id uma única vez durante o primeiro
            // passe. O custo adicional é O(n) de memória temporária, enquanto
            // o caminho anterior podia chegar a O(n * vítimasDeCrash).
            val participantePorId = HashMap<String, ParticipanteIniciativa>(lista.size)
            for (p in lista) {
                participantePorId[p.id] = p
                val antes = snapshot[p.id] ?: p.iniciativa
                if (antes > 0 && p.iniciativa <= 0) {
                    novosEventosCrash += p.nome
                    causadorPorVitima[p.id]?.let { crashadoresParaBonus += it }
                }
            }

            // --- Initiative Shift ---
            // Se B coloca A em Crash e A era quem tinha colocado B em Crash,
            // B se recupera (volta para no mínimo 3).
            val recuperados = HashSet<String>()
            for (p in lista) {
                val antes = snapshot[p.id] ?: p.iniciativa
                if (antes > 0 && p.iniciativa <= 0) {
                    val causadorId = causadorPorVitima[p.id] ?: continue
                    val causador = participantePorId[causadorId] ?: continue
                    if (causador.reduzidoPorId == p.id && causador.iniciativa <= 0) {
                        recuperados += causadorId
                    }
                }
            }

            // Quem agiu: todos os declarantes (e, em Clash, ambos do par).
            val quemAgiram = HashSet<String>(current.declaracoes.size + paresClash.size * 2)
            quemAgiram.addAll(current.declaracoes.keys)
            for ((a, b) in paresClash) {
                quemAgiram += a
                quemAgiram += b
            }

            // Aplica Crash, Initiative Shift, bônus do Clash e marcação de ação
            // em uma única passagem. Antes eram três map() sucessivos sobre a
            // lista de participantes.
            val bonusClash = if (!current.colisaoAtiva && vencedorClash != null && paresClash.isNotEmpty()) {
                3 + if (current.contadorTransferencia != 0) 1 else 0
            } else {
                0
            }
            // Fulminante bem-sucedido: +1 fixo pro atacante (seção 7.1/4.2,
            // pedido explícito do usuário — Parte C). Atacante = vencedor
            // do Clash quando há um; senão, o declarante da primeira (e
            // única) declaração simples deste slot.
            val atacanteFulminanteId: String? =
                if (!current.colisaoAtiva && current.tipoAtaquePendente == TipoAtaque.FULMINANTE && current.ataqueBemSucedido == true) {
                    vencedorClash ?: current.declaracoes.entries.firstOrNull()?.key
                } else null
            // Aviso manual (Parte C.3, opção escolhida pelo usuário): Battle
            // Group como alvo não tem a iniciativa reduzida (regra 2.2) —
            // o jogador aplica o dano na Magnitude manualmente, na Aba 13.
            if (atacanteFulminanteId != null && current.contadorTransferencia != 0) {
                val alvoFulminanteId = if (vencedorClash != null && paresClash.isNotEmpty()) {
                    paresClash.firstOrNull { vencedorClash == it.first || vencedorClash == it.second }
                        ?.let { (a, b) -> if (vencedorClash == a) b else a }
                } else {
                    current.declaracoes.entries.firstOrNull()?.value
                }
                val alvoFulminante = lista.firstOrNull { it.id == alvoFulminanteId }
                if (alvoFulminante?.origemBattleGroupId != null) {
                    mensagens += "${alvoFulminante.nome} sofreu ${current.contadorTransferencia} de dano. Aplique manualmente na Magnitude, na Aba 13."
                }
            }
            lista = lista.map { p ->
                var ini = p.iniciativa
                val antes = snapshot[p.id] ?: p.iniciativa
                val crashado = antes > 0 && p.iniciativa <= 0
                var redPor = if (crashado) {
                    causadorPorVitima[p.id] ?: p.reduzidoPorId
                } else {
                    p.reduzidoPorId
                }
                if (p.id in crashadoresParaBonus) ini += 5
                if (p.id in recuperados) {
                    ini = maxOf(ini, 3)
                    redPor = null
                    mensagens += "Initiative Shift: ${p.nome} recupera-se do Crash."
                }
                if (p.id == vencedorClash) ini += bonusClash
                if (p.id == atacanteFulminanteId) ini += 1
                val agiu = p.id in quemAgiram
                val agiuNesteTurno = if (agiu) true else p.jaAgiramNesteTurno
                p.copy(iniciativa = ini, reduzidoPorId = redPor, jaAgiramNesteTurno = agiuNesteTurno)
            }

            val aindaPendentes = lista.any { !it.jaAgiramNesteTurno }
            if (!aindaPendentes) {
                mensagens += "Fim do turno. Todos recuperam 5 motes de essência."
            }

            // A penalidade termina no início do próximo turno do perdedor,
            // não simplesmente no fim da rodada. Quando a rodada vira, todos
            // os combatentes tornam-se elegíveis novamente, sem precisar criar
            // uma segunda lista apenas para resetar jaAgiramNesteTurno.
            var maxProximaIniciativa: Int? = null
            for (p in lista) {
                val elegivelProximoSlot = !aindaPendentes || !p.jaAgiramNesteTurno
                if (elegivelProximoSlot &&
                    (maxProximaIniciativa == null || p.iniciativa > maxProximaIniciativa)) {
                    maxProximaIniciativa = p.iniciativa
                }
            }

            // Aplica expiração da penalidade, reset de ação e Atordoamento na
            // mesma passagem. Isso elimina o map() intermediário da virada de
            // rodada e mantém todas as mutações de fechamento em um único loop.
            val participantesFinais = ArrayList<ParticipanteIniciativa>(lista.size)
            for (p in lista) {
                var atual = if (!aindaPendentes && p.jaAgiramNesteTurno) {
                    p.copy(jaAgiramNesteTurno = false)
                } else {
                    p
                }
                val elegivelProximoSlot = !aindaPendentes || !p.jaAgiramNesteTurno
                val penalidadeExpira = p.penalidadeClashDefesa > 0 &&
                    elegivelProximoSlot &&
                    maxProximaIniciativa == p.iniciativa
                if (penalidadeExpira) {
                    atual.origemNpcId?.let(penalidadesExpiradasNpcIds::add)
                    atual = atual.copy(penalidadeClashDefesa = 0)
                }
                if (!aindaPendentes) {
                    if (atual.iniciativa <= 0) {
                        val rodadasNovas = atual.rodadasEmAtordoamento + 1
                        atual = if (rodadasNovas >= 3) {
                            mensagens += "${atual.nome} recupera-se do Atordoamento de Iniciativa, voltando à Iniciativa Base."
                            atual.copy(iniciativa = 3, rodadasEmAtordoamento = 0, reduzidoPorId = null)
                        } else {
                            atual.copy(rodadasEmAtordoamento = rodadasNovas)
                        }
                    } else if (atual.rodadasEmAtordoamento != 0) {
                        atual = atual.copy(rodadasEmAtordoamento = 0)
                    }
                }
                participantesFinais += atual
            }

            val participantesComAtordoamento = participantesFinais
            return Resultado(
                current.copy(
                    rodada = if (aindaPendentes) current.rodada else current.rodada + 1,
                    participantes = participantesComAtordoamento,
                    declaracoes = emptyMap(),
                    ataqueTravado = false,
                    vencedorClashId = null,
                    contadorTransferencia = 0,
                    iniciativasAntesTransferencia = emptyMap(),
                    mensagensPendentes = mensagens,
                    eventosCrashAcumulados = current.eventosCrashAcumulados + novosEventosCrash,
                    declaracoesMulti = emptyMap(),
                    filaResolucaoMulti = emptyList(),
                    alvoResolucaoAtual = null,
                    atacanteMultiId = null,
                    tipoAtaquePendente = null,
                    ataqueBemSucedido = null,
                    iniciativasPendentes = emptyMap(),
                    turnStatus = TurnStatus.RESOLVIDO
                ),
                penalidadesExpiradasNpcIds = penalidadesExpiradasNpcIds
            )
    }

    /**
     * Caminho de resolução pra Decisivo (sucesso ou fracasso) e Fulminante
     * malsucedido — pedido explícito do usuário (Parte C). Diferente do
     * caminho normal de [resolver]: não aplica Crash (+5), Initiative
     * Shift nem bônus de Clash — a iniciativa do Decisivo já foi definida
     * na hora da resposta, e Fulminante malsucedido não causa dano algum.
     * Só marca quem agiu e avança a rodada se ninguém mais pendente
     * (mesma lógica de fechamento de turno de [resolver], sem os blocos
     * de Crash/Shift/bônus).
     */
    private fun resolverDecisivoOuFulminanteMalsucedido(current: IniciativasState): Resultado {
        val penalidadesExpiradasNpcIds = linkedSetOf<String>()
        // PDF §6 / §7: aplica iniciativa pendente do Decisivo (ou nenhuma no Fulminante falho).
        var lista = aplicarPendentes(current.participantes, current.iniciativasPendentes)
        val mensagens = current.mensagensPendentes.toMutableList()

        val paresClash = current.paresClash()
        val quemAgiram = HashSet<String>(current.declaracoes.size + paresClash.size * 2)
        quemAgiram.addAll(current.declaracoes.keys)
        for ((a, b) in paresClash) {
            quemAgiram += a
            quemAgiram += b
        }

        lista = lista.map { p ->
            val agiu = p.id in quemAgiram
            val agiuNesteTurno = if (agiu) true else p.jaAgiramNesteTurno
            p.copy(jaAgiramNesteTurno = agiuNesteTurno)
        }

        val aindaPendentes = lista.any { !it.jaAgiramNesteTurno }
        if (!aindaPendentes) {
            mensagens += "Fim do turno. Todos recuperam 5 motes de essência."
        }

        var maxProximaIniciativa: Int? = null
        for (p in lista) {
            val elegivelProximoSlot = !aindaPendentes || !p.jaAgiramNesteTurno
            if (elegivelProximoSlot &&
                (maxProximaIniciativa == null || p.iniciativa > maxProximaIniciativa)) {
                maxProximaIniciativa = p.iniciativa
            }
        }

        val participantesFinais = ArrayList<ParticipanteIniciativa>(lista.size)
        for (p in lista) {
            var atual = if (!aindaPendentes && p.jaAgiramNesteTurno) {
                p.copy(jaAgiramNesteTurno = false)
            } else {
                p
            }
            val elegivelProximoSlot = !aindaPendentes || !p.jaAgiramNesteTurno
            val penalidadeExpira = p.penalidadeClashDefesa > 0 &&
                elegivelProximoSlot &&
                maxProximaIniciativa == p.iniciativa
            if (penalidadeExpira) {
                atual.origemNpcId?.let(penalidadesExpiradasNpcIds::add)
                atual = atual.copy(penalidadeClashDefesa = 0)
            }
            if (!aindaPendentes) {
                if (atual.iniciativa <= 0) {
                    val rodadasNovas = atual.rodadasEmAtordoamento + 1
                    atual = if (rodadasNovas >= 3) {
                        mensagens += "${atual.nome} recupera-se do Atordoamento de Iniciativa, voltando à Iniciativa Base."
                        atual.copy(iniciativa = 3, rodadasEmAtordoamento = 0, reduzidoPorId = null)
                    } else {
                        atual.copy(rodadasEmAtordoamento = rodadasNovas)
                    }
                } else if (atual.rodadasEmAtordoamento != 0) {
                    atual = atual.copy(rodadasEmAtordoamento = 0)
                }
            }
            participantesFinais += atual
        }

        return Resultado(
            current.copy(
                rodada = if (aindaPendentes) current.rodada else current.rodada + 1,
                participantes = participantesFinais,
                declaracoes = emptyMap(),
                ataqueTravado = false,
                vencedorClashId = null,
                colisaoAtiva = false,
                contadorTransferencia = 0,
                iniciativasAntesTransferencia = emptyMap(),
                mensagensPendentes = mensagens,
                declaracoesMulti = emptyMap(),
                filaResolucaoMulti = emptyList(),
                alvoResolucaoAtual = null,
                atacanteMultiId = null,
                tipoAtaquePendente = null,
                ataqueBemSucedido = null,
                iniciativasPendentes = emptyMap(),
                turnStatus = TurnStatus.RESOLVIDO
            ),
            penalidadesExpiradasNpcIds = penalidadesExpiradasNpcIds
        )
    }

}
