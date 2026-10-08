package com.example.iniciativas

import kotlinx.coroutines.flow.update

// Extraído de IniciativasController.kt (refatoração de organização —
// continuação do roteiro de refatoração agressiva, sem mudança de
// comportamento). Seção "Adicionar / remover / editar" original, mantida
// como funções de extensão sobre IniciativasController pra preservar o
// acesso ao estado compartilhado (_state, nextInsertionOrder, limparNome
// — agora internal, visíveis a qualquer arquivo do mesmo pacote).
// Mesmo padrão já usado em IniciativasControllerClash.kt e
// IniciativasControllerTermino.kt.

fun IniciativasController.adicionarParticipante(nome: String, iniciativa: Int): Boolean {
    val nomeLimpo = IniciativasController.limparNome(nome)
    if (nomeLimpo.isBlank()) return false
    val ordem = nextInsertionOrder.incrementAndGet()
    _state.update { current ->
        current.copy(
            participantes = current.participantes + ParticipanteIniciativa(
                nome = nomeLimpo,
                iniciativa = iniciativa,
                ordemInsercao = ordem
            )
        )
    }
    return true
}

fun IniciativasController.adicionarOuAtualizarNpc(nome: String, iniciativa: Int, origemNpcId: String): Boolean {
    val nomeLimpo = IniciativasController.limparNome(nome)
    if (nomeLimpo.isBlank() || origemNpcId.isBlank()) return false
    _state.update { current ->
        val indiceExistente = current.participantes.indexOfFirst { it.origemNpcId == origemNpcId }
        val novaLista = if (indiceExistente >= 0) {
            // PERFORMANCE: uma única busca resolve existência e posição.
            val existente = current.participantes[indiceExistente]
            if (existente.nome == nomeLimpo && existente.iniciativa == iniciativa) {
                return@update current
            }
            ArrayList(current.participantes).also { lista ->
                lista[indiceExistente] = existente.copy(nome = nomeLimpo, iniciativa = iniciativa)
            }
        } else {
            val ordem = nextInsertionOrder.incrementAndGet()
            current.participantes + ParticipanteIniciativa(
                nome = nomeLimpo,
                iniciativa = iniciativa,
                origemNpcId = origemNpcId,
                ordemInsercao = ordem
            )
        }
        current.copy(participantes = novaLista)
    }
    return true
}

fun IniciativasController.adicionarOuAtualizarBattleGroup(nome: String, iniciativa: Int, origemBattleGroupId: String): Boolean {
    val nomeLimpo = IniciativasController.limparNome(nome)
    if (nomeLimpo.isBlank() || origemBattleGroupId.isBlank()) return false
    _state.update { current ->
        val indiceExistente = current.participantes.indexOfFirst { it.origemBattleGroupId == origemBattleGroupId }
        val novaLista = if (indiceExistente >= 0) {
            val existente = current.participantes[indiceExistente]
            if (existente.nome == nomeLimpo && existente.iniciativa == iniciativa) return@update current
            ArrayList(current.participantes).also { it[indiceExistente] = existente.copy(nome = nomeLimpo, iniciativa = iniciativa) }
        } else {
            val ordem = nextInsertionOrder.incrementAndGet()
            current.participantes + ParticipanteIniciativa(nome = nomeLimpo, iniciativa = iniciativa, origemBattleGroupId = origemBattleGroupId, ordemInsercao = ordem)
        }
        current.copy(participantes = novaLista)
    }
    return true
}

fun IniciativasController.removerPorOrigemBattleGroupId(origemBattleGroupId: String) {
    if (origemBattleGroupId.isBlank()) return
    _state.update { current ->
        val indice = current.participantes.indexOfFirst { it.origemBattleGroupId == origemBattleGroupId }
        if (indice < 0) return@update current
        val id = current.participantes[indice].id
        val novaLista = ArrayList(current.participantes).also { it.removeAt(indice) }
        val (novasDecl, declaracaoRemovida) = removerDeclaracoesEnvolvendo(current.declaracoes, id)
        val novasDecl2 = current.copy(participantes = novaLista, declaracoes = novasDecl, vencedorClashId = current.vencedorClashId?.takeIf { it != id }, ataqueTravado = if (declaracaoRemovida) false else current.ataqueTravado, contadorTransferencia = 0)
        limparMultiPorRemocao(novasDecl2, id)
    }
}

fun IniciativasController.removerParticipante(id: String) {
    _state.update { current ->
        val indice = current.participantes.indexOfFirst { it.id == id }
        if (indice < 0) return@update current
        val novaLista = ArrayList(current.participantes).also { it.removeAt(indice) }
        val (novasDecl, declaracaoRemovida) = removerDeclaracoesEnvolvendo(current.declaracoes, id)
        val resultado = current.copy(
            participantes = novaLista,
            declaracoes = novasDecl,
            vencedorClashId = current.vencedorClashId?.takeIf { it != id },
            ataqueTravado = if (declaracaoRemovida) false else current.ataqueTravado,
            contadorTransferencia = 0
        )
        limparMultiPorRemocao(resultado, id)
    }
}

fun IniciativasController.removerPorOrigemNpcId(origemNpcId: String) {
    if (origemNpcId.isBlank()) return
    // PERFORMANCE: a versão anterior fazia duas mutações/leituras: uma
    // busca linear para descobrir o id e outra busca linear dentro de
    // removerParticipante(). A remoção pode ser resolvida atomicamente em
    // uma única passagem pela lista de participantes.
    _state.update { current ->
        val indice = current.participantes.indexOfFirst { it.origemNpcId == origemNpcId }
        if (indice < 0) return@update current
        val id = current.participantes[indice].id
        val novaLista = ArrayList(current.participantes).also { it.removeAt(indice) }
        val (novasDecl, declaracaoRemovida) = removerDeclaracoesEnvolvendo(current.declaracoes, id)
        val resultadoNpc = current.copy(
            participantes = novaLista,
            declaracoes = novasDecl,
            vencedorClashId = current.vencedorClashId?.takeIf { it != id },
            ataqueTravado = if (declaracaoRemovida) false else current.ataqueTravado,
            contadorTransferencia = 0
        )
        limparMultiPorRemocao(resultadoNpc, id)
    }
}

/**
 * Limpa referências a [idRemovido] em declaracoesMulti/filaResolucaoMulti/
 * alvoResolucaoAtual/atacanteMultiId — usado pelas 3 funções de remoção
 * de participante (removerParticipante, removerPorOrigemNpcId,
 * removerPorOrigemBattleGroupId). Se o id removido era o atacante,
 * remove a entrada inteira; se era um dos alvos, remove só ele da
 * lista de alvos daquele atacante.
 */
internal fun IniciativasController.limparMultiPorRemocao(current: IniciativasState, idRemovido: String): IniciativasState {
    val novasMulti = current.declaracoesMulti.mapNotNull { (atacante, alvos) ->
        if (atacante == idRemovido) null
        else alvos.filterNot { it == idRemovido }.takeIf { it.isNotEmpty() }?.let { atacante to it }
    }.toMap()
    val afetaFilaAtual = current.atacanteMultiId == idRemovido ||
        current.alvoResolucaoAtual == idRemovido ||
        idRemovido in current.filaResolucaoMulti
    if (novasMulti == current.declaracoesMulti && !afetaFilaAtual) return current
    return current.copy(
        declaracoesMulti = novasMulti,
        filaResolucaoMulti = if (afetaFilaAtual) emptyList() else current.filaResolucaoMulti,
        alvoResolucaoAtual = if (afetaFilaAtual) null else current.alvoResolucaoAtual,
        atacanteMultiId = if (afetaFilaAtual) null else current.atacanteMultiId
    )
}

/**
 * Remove declarações que envolvem [id] sem alocar um novo mapa quando
 * nenhuma declaração aponta para o participante removido.
 *
 * Remoções fora de uma declaração são comuns quando combatentes são
 * retirados antes de qualquer ataque. Nesse caso, reutilizar o mapa
 * imutável evita uma alocação e uma cópia completa desnecessárias.
 */
internal fun IniciativasController.removerDeclaracoesEnvolvendo(
    declaracoes: Map<String, String>,
    id: String
): Pair<Map<String, String>, Boolean> {
    if (declaracoes.isEmpty()) return emptyMap<String, String>() to false

    // PERFORMANCE: cópia tardia. Percorre o mapa apenas uma vez e só
    // materializa uma nova estrutura quando encontra a primeira
    // declaração envolvendo o participante. Isso preserva o caminho
    // comum sem remoção (zero alocação) e elimina a segunda varredura
    // do caso de remoção. LinkedHashMap preserva a ordem de iteração
    // do mapa original, que é relevante para consumidores da UI.
    var filtradas: LinkedHashMap<String, String>? = null
    for ((atacante, alvo) in declaracoes) {
        if (atacante == id || alvo == id) {
            if (filtradas == null) {
                filtradas = LinkedHashMap(declaracoes.size)
                for ((anteriorAtacante, anteriorAlvo) in declaracoes) {
                    if (anteriorAtacante == atacante && anteriorAlvo == alvo) break
                    filtradas[anteriorAtacante] = anteriorAlvo
                }
            }
            continue
        }
        filtradas?.set(atacante, alvo)
    }
    return (filtradas ?: return declaracoes to false) to true
}

fun IniciativasController.atualizarPorOrigemNpcId(origemNpcId: String, nome: String, iniciativa: Int): Boolean {
    if (origemNpcId.isBlank()) return false
    val nomeLimpo = IniciativasController.limparNome(nome)
    if (nomeLimpo.isBlank()) return false
    var encontrado = false
    _state.update { current ->
        val indice = current.participantes.indexOfFirst { it.origemNpcId == origemNpcId }
        if (indice < 0) return@update current
        encontrado = true
        val participante = current.participantes[indice]
        if (participante.nome == nomeLimpo && participante.iniciativa == iniciativa) {
            // Sem mudança: preserva a mesma instância de lista (identidade).
            return@update current
        }
        current.copy(
            participantes = ArrayList(current.participantes).also { lista ->
                lista[indice] = participante.copy(nome = nomeLimpo, iniciativa = iniciativa)
            }
        )
    }
    return encontrado
}

fun IniciativasController.editarNome(id: String, novoNome: String) {
    // PDF §2: após travar o ataque, não se volta à etapa anterior / não altera o quadro.
    if (_state.value.ataqueTravado) return
    val limpo = IniciativasController.limparNome(novoNome)
    if (limpo.isBlank()) return
    _state.update { current ->
        if (current.ataqueTravado) return@update current
        val indice = current.participantes.indexOfFirst { it.id == id }
        if (indice < 0) return@update current
        val participante = current.participantes[indice]
        if (participante.nome == limpo) return@update current
        current.copy(participantes = ArrayList(current.participantes).also { lista ->
            lista[indice] = participante.copy(nome = limpo)
        })
    }
}

fun IniciativasController.definirIniciativa(id: String, valor: Int) {
    _state.update { current ->
        // PDF §2 / §5.2: após travar, só a caixa do perdedor (transferirIniciativa) ajusta.
        if (current.ataqueTravado) return@update current
        val indice = current.participantes.indexOfFirst { it.id == id }
        if (indice < 0) return@update current
        val participante = current.participantes[indice]
        if (participante.iniciativa == valor) return@update current
        val participantes = ArrayList(current.participantes).also { lista ->
            lista[indice] = participante.copy(iniciativa = valor)
        }
        current.copy(
            participantes = participantes,
            declaracoes = emptyMap(),
            vencedorClashId = null,
            declaracoesMulti = emptyMap(),
            filaResolucaoMulti = emptyList(),
            alvoResolucaoAtual = null,
            atacanteMultiId = null
        )
    }
}

fun IniciativasController.ajustarIniciativa(id: String, delta: Int) {
    if (delta == 0) return
    _state.update { current ->
        // PDF §2: após travar, ajuste só via caixa do perdedor (transferirIniciativa).
        if (current.ataqueTravado) return@update current
        val indice = current.participantes.indexOfFirst { it.id == id }
        if (indice < 0) return@update current

        val participantes = ArrayList(current.participantes)
        val atual = participantes[indice].iniciativa
        participantes[indice] = participantes[indice].copy(iniciativa = atual + delta)

        current.copy(
            participantes = participantes,
            declaracoes = emptyMap(),
            vencedorClashId = null,
            declaracoesMulti = emptyMap(),
            filaResolucaoMulti = emptyList(),
            alvoResolucaoAtual = null,
            atacanteMultiId = null
        )
    }
}

