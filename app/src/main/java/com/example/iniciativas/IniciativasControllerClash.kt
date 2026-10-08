package com.example.iniciativas

import kotlinx.coroutines.flow.update

// Extraído de IniciativasController.kt (refatoração de organização —
// pedido explícito do usuário, sem mudança de comportamento). Seção
// "[Ataque] — trava" original, mantida como funções de extensão sobre
// IniciativasController pra preservar o acesso ao estado compartilhado
// (_state, pendenteDeclaranteId, paresClashCache — agora internal,
// visíveis a qualquer arquivo do mesmo pacote).
//
// Corrigido no processo: dois comentários de documentação estavam
// associados às funções erradas no arquivo original (um bug
// pré-existente, não introduzido por esta refatoração) — reassociados
// aqui às funções que de fato descrevem.

/** Retorna os pares de Clash do estado atual, reutilizando o cache por identidade. */
internal fun IniciativasController.paresClashAtuais(): List<Pair<String, String>> = paresClashDaState(_state.value)

internal fun IniciativasController.paresClashDaState(state: IniciativasState): List<Pair<String, String>> {
    val declaracoes = state.declaracoes
    paresClashCache?.let { cache ->
        if (cache.origem === declaracoes) return cache.resultado
    }
    val resultado = IniciativasRules.paresClash(declaracoes)
    paresClashCache = IniciativasController.ParesClashCache(declaracoes, resultado)
    return resultado
}

internal fun IniciativasController.encontrarParElegivelParaColisao(state: IniciativasState): Pair<String, String>? {
    // Colisão entre dois elegíveis (mesma iniciativa): basta um ter o outro
    // como alvo — não exige [Ataque] prévio. Empate de iniciativa = equivalentes;
    // a posição na tabela não importa.
    if (paresClashDaState(state).isNotEmpty()) return null

    val elegiveis = idsElegiveis()
    if (elegiveis.size < 2) return null

    return state.declaracoes.entries.firstOrNull { (atacanteId, alvoId) ->
        atacanteId != alvoId &&
            atacanteId in elegiveis &&
            alvoId in elegiveis
    }?.let { it.key to it.value }
}

/**
 * Trava as declarações atuais.
 * Exige pelo menos uma declaração válida.
 * Em Clash, ainda não há vencedor — usuário toca nele depois.
 */
internal fun IniciativasController.travarAtaque(): Boolean {
    val current = _state.value
    if (current.ataqueTravado) return true
    val temSimples = current.declaracoes.isNotEmpty()
    val temMulti = current.temDeclaracaoMulti()
    if (!temSimples && !temMulti) return false

    // Snapshot para detectar Crash no [Término]. Construção manual evita
    // a lambda/transformação intermediária de associate() no hot path.
    val snapshot = HashMap<String, Int>(current.participantes.size)
    for (participante in current.participantes) {
        snapshot[participante.id] = participante.iniciativa
    }

    if (temMulti) {
        val (atacanteId, alvos) = current.declaracoesMulti.entries.first()
        val porId = current.participantes.associateBy { it.id }
        val alvosOrdenados = alvos.sortedWith(
            compareByDescending<String> { porId[it]?.iniciativa ?: Int.MIN_VALUE }
                .thenBy { porId[it]?.ordemInsercao ?: Long.MAX_VALUE }
        )
        _state.update {
            it.copy(
                ataqueTravado = true,
                contadorTransferencia = 0,
                vencedorClashId = null,
                iniciativasAntesTransferencia = snapshot,
                filaResolucaoMulti = alvosOrdenados,
                alvoResolucaoAtual = alvosOrdenados.firstOrNull(),
                atacanteMultiId = atacanteId,
                tipoAtaquePendente = null,
                ataqueBemSucedido = null,
                iniciativasPendentes = emptyMap(),
                turnStatus = TurnStatus.TRAVADO
            )
        }
    } else {
        _state.update {
            it.copy(
                ataqueTravado = true,
                contadorTransferencia = 0,
                vencedorClashId = null,
                iniciativasAntesTransferencia = snapshot,
                tipoAtaquePendente = null,
                ataqueBemSucedido = null,
                iniciativasPendentes = emptyMap(),
                turnStatus = TurnStatus.TRAVADO
            )
        }
    }
    pendenteDeclaranteId = null
    return true
}

/**
 * Inicia manualmente uma Colisão a partir de um ataque simples entre
 * dois combatentes que ainda não agiram e possuem a mesma iniciativa.
 *
 * A Colisão transforma a declaração A → B em declarações mútuas A ↔ B
 * e marca imediatamente os dois combatentes como já tendo agido neste
 * turno. A seleção do vencedor continua sendo feita pelo fluxo normal
 * de Clash exibido pela interface.
 */
internal fun IniciativasController.iniciarColisao(): Boolean {
    var iniciada = false
    _state.update { current ->
        val declaracao = encontrarParElegivelParaColisao(current)
            ?: return@update current

        val (atacanteId, alvoId) = declaracao
        val participantes = current.participantes.toMutableList()
        var atacanteIndice = -1
        var alvoIndice = -1
        for (i in participantes.indices) {
            when (participantes[i].id) {
                atacanteId -> atacanteIndice = i
                alvoId -> alvoIndice = i
            }
            if (atacanteIndice >= 0 && alvoIndice >= 0) break
        }
        if (atacanteIndice < 0 || alvoIndice < 0) return@update current
        iniciada = true
        participantes[atacanteIndice] = participantes[atacanteIndice].copy(jaAgiramNesteTurno = true)
        participantes[alvoIndice] = participantes[alvoIndice].copy(jaAgiramNesteTurno = true)
        // Trava o ataque e cria declaração mútua (Clash) para o seletor de vencedor.
        current.copy(
            declaracoes = current.declaracoes + (alvoId to atacanteId),
            participantes = participantes,
            ataqueTravado = true,
            vencedorClashId = null,
            colisaoAtiva = true,
            contadorTransferencia = 0,
            iniciativasPendentes = emptyMap(),
            turnStatus = TurnStatus.TRAVADO
        )
    }
    return iniciada
}

/**
 * Seleciona explicitamente o vencedor de um Clash. Retorna o ID do NPC
 * perdedor quando o combatente pertence a um NPC de encontro; caso
 * contrário retorna null. A penalidade de Defesa é registrada como 2
 * pontos positivos para ser subtraída pelos cálculos de Defesa.
 */
internal fun IniciativasController.selecionarVencedorClash(vencedorId: String): String? {
    var perdedorNpcId: String? = null
    _state.update { current ->
        if (!current.ataqueTravado || current.vencedorClashId != null) return@update current
        val par = paresClashDaState(current).firstOrNull { vencedorId == it.first || vencedorId == it.second }
            ?: return@update current
        val perdedorId = if (vencedorId == par.first) par.second else par.first
        val participantes = current.participantes.toMutableList()
        var perdedorIndice = -1
        var vencedorIndice = -1
        for (i in participantes.indices) {
            when (participantes[i].id) {
                perdedorId -> perdedorIndice = i
                vencedorId -> vencedorIndice = i
            }
            if (perdedorIndice >= 0 && vencedorIndice >= 0) break
        }
        if (perdedorIndice < 0 || vencedorIndice < 0) return@update current
        perdedorNpcId = participantes[perdedorIndice].origemNpcId
        if (!current.colisaoAtiva) {
            participantes[perdedorIndice] = participantes[perdedorIndice].copy(penalidadeClashDefesa = 2)
            participantes[vencedorIndice] = participantes[vencedorIndice].copy(penalidadeClashDefesa = 0)
        }
        current.copy(
            participantes = participantes,
            vencedorClashId = vencedorId,
            contadorTransferencia = 0
        )
    }
    return perdedorNpcId
}
