package com.example.iniciativas

// Extraído de IniciativasController.kt (refatoração de organização —
// continuação do roteiro de refatoração agressiva, sem mudança de
// comportamento). Seção "[Término]" original, mantida como funções de
// extensão sobre IniciativasController pra preservar o acesso ao estado
// compartilhado (_state, pendenteDeclaranteId — já internal, visíveis a
// qualquer arquivo do mesmo pacote). Mesmo padrão já usado em
// IniciativasControllerClash.kt.

/**
 * 1. Aplica bônus de sistema (+3 Clash, +5 Crash, Initiative Shift).
 * 2. Marca quem agiu neste slot como já agiu.
 * 3. Limpa declarações / trava.
 * 4. Se ninguém mais pendente → fim de turno + mensagem de motes.
 */
/**
 * Finaliza a ação atual. A resolução pesada de regras de turno fica
 * isolada em uma classe pura para reduzir responsabilidades do controller
 * e permitir testes determinísticos sem Compose/StateFlow.
 */
internal fun IniciativasController.terminarAcao(): Set<String> {
    // CAS explícito: resolver() trabalha sobre um snapshot. Evitamos o
    // padrão ler -> resolver -> atribuir, que poderia sobrescrever uma
    // mutação concorrente feita entre a leitura e a atribuição.
    while (true) {
        val atual = _state.value
        val resultado = IniciativasTurnResolver.resolver(atual)
        // CORREÇÃO — log de combate: registra este turno (quem atacou, quem
        // foi atacado, quantos pontos de Iniciativa foram transferidos) a
        // cada [Próximo]/[Término da ação] — não só uma vez no fim do
        // combate inteiro. Construído a partir de "atual" (o estado ANTES
        // do reset de declarações feito por resolver()), que ainda tem a
        // declaração/transferência deste turno específico.
        val snapshot = montarLogTurno(atual, resultado.state.turnStatus)
        val estadoComLog = resultado.state.copy(eventosLog = resultado.state.eventosLog + snapshot)
        if (_state.compareAndSet(atual, estadoComLog)) {
            pendenteDeclaranteId = null
            return resultado.penalidadesExpiradasNpcIds
        }
    }
}

/**
 * [Pular]: avança para o próximo combatente ignorando o(s) elegível(is)
 * atual(is) — marca quem está na maior iniciativa e ainda não agiu como
 * já tendo agido neste turno, sem aplicar bônus de Clash/Crash nem exigir
 * declaração de ataque. Se ninguém mais restar pendente, fecha a rodada
 * (mesmo critério de [Próximo]/reset de [jaAgiramNesteTurno] + rodada+1).
 *
 * Disponível a qualquer momento em que haja pelo menos um combatente
 * pendente (não exige [Ataque] travado).
 */
internal fun IniciativasController.pularTurnoAtual(): Boolean =
    pularTurnoAtualComPenalidadesExpiradas() != null

/**
 * Variante usada pela UI para propagar à Aba 11 as penalidades temporárias
 * que expiram quando [Pular] entrega o próximo slot a outro NPC.
 * null = não foi possível pular; conjunto vazio = pulou sem NPC vinculado.
 */
internal fun IniciativasController.pularTurnoAtualComPenalidadesExpiradas(): Set<String>? {
    while (true) {
        val atual = _state.value
        // PDF §9: depois de travar o ataque, Pular fica indisponível.
        if (atual.ataqueTravado) return null
        val idsElegiveis = IniciativasRules.idsElegiveis(atual.participantes)
        if (idsElegiveis.isEmpty()) return null

        val marcados = atual.participantes.map { p ->
            if (p.id in idsElegiveis) p.copy(jaAgiramNesteTurno = true) else p
        }
        val aindaPendentes = marcados.any { !it.jaAgiramNesteTurno }
        val participantesFinais = if (aindaPendentes) {
            marcados
        } else {
            // Fim de rodada: todos recuperam a chance de agir.
            marcados.map { it.copy(jaAgiramNesteTurno = false) }
        }
        val novo = atual.copy(
            participantes = participantesFinais,
            rodada = if (aindaPendentes) atual.rodada else atual.rodada + 1,
            declaracoes = emptyMap(),
            ataqueTravado = false,
            vencedorClashId = null,
            colisaoAtiva = false,
            contadorTransferencia = 0,
            iniciativasAntesTransferencia = emptyMap(),
            declaracoesMulti = emptyMap(),
            filaResolucaoMulti = emptyList(),
            alvoResolucaoAtual = null,
            atacanteMultiId = null,
            tipoAtaquePendente = null,
            ataqueBemSucedido = null,
            iniciativasPendentes = emptyMap(),
            turnStatus = TurnStatus.PULADO
        )
        if (_state.compareAndSet(atual, novo)) {
            pendenteDeclaranteId = null
            val proximosIds = IniciativasRules.idsElegiveis(participantesFinais)
            return participantesFinais.asSequence()
                .filter { it.id in proximosIds }
                .mapNotNull { it.origemNpcId }
                .toSet()
        }
    }
}

/** Há combatente pendente para [Pular] ignorar. */
internal fun IniciativasController.podePularTurno(): Boolean {
    val s = _state.value
    // PDF §9: Pular só antes do travamento do ataque.
    if (s.ataqueTravado) return false
    return IniciativasRules.idsElegiveis(s.participantes).isNotEmpty()
}
