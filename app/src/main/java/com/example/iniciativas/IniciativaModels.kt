package com.example.iniciativas

import java.util.UUID

/**
 * Glossário (mantido para comunicação entre programadores):
 *
 * - [Atacante]  → combatente elegível (maior iniciativa entre os que ainda não agiram).
 * - [Atacado]   → alvo declarado por um [Atacante].
 * - [Ataque]    → botão que trava o(s) pareamento(s) e libera +/–.
 * - [Término]   → aplica bônus de sistema (+3 Clash, +5 Crash, Shift) e encerra a ação.
 * - Clash       → A declara B e B declara A, com a mesma iniciativa.
 * - Crash       → iniciativa ≤ 0.
 * - Initiative Shift → quem estava em Crash e coloca em Crash quem o derrubou se recupera.
 * - Rodada      → ciclo em que todos já foram [Atacante] uma vez.
 */

data class ParticipanteIniciativa(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val iniciativa: Int,
    /** ID do NPC de encontro (Aba 11), quando aplicável. */
    val origemNpcId: String? = null,
    /** ID do Battle Group configurado, quando aplicável. */
    val origemBattleGroupId: String? = null,
    /** Desempate quando iniciativas são iguais e não há Clash. */
    val ordemInsercao: Long,
    /** Já recebeu a condição [Atacante] / já agiu neste turno. */
    val jaAgiramNesteTurno: Boolean = false,
    /**
     * ID de quem colocou este combatente em Crash (≤ 0) pela última vez.
     * Usado pelo Initiative Shift.
     */
    val reduzidoPorId: String? = null,
    /** Penalidade temporária de Defesa por perder um Clash. Mantida positiva e subtraída do valor-base. */
    val penalidadeClashDefesa: Int = 0,
    /** Rodadas consecutivas com iniciativa <= 0 (Atordoamento de Iniciativa
     * — "Crash" na terminologia em inglês, confirmado pelo usuário).
     * Ao atingir 3, recupera-se automaticamente voltando à Iniciativa
     * Base (3). Reseta a 0 assim que a iniciativa volta a ser > 0. */
    val rodadasEmAtordoamento: Int = 0
)

data class IniciativasState(
    val rodada: Int = 1,
    val participantes: List<ParticipanteIniciativa> = emptyList(),
    /**
     * Declarações de ataque do “slot” atual:
     * atacanteId → alvoId
     * Em iniciativa empatada, vários elegíveis podem declarar.
     * Clash = declarações mútuas (A→B e B→A).
     */
    val declaracoes: Map<String, String> = emptyMap(),
    /**
     * true depois de [Ataque]: pareamentos travados, +/– liberados.
     */
    val ataqueTravado: Boolean = false,
    /**
     * No Clash, após [Ataque], o usuário toca no vencedor.
     * Transferência + só flui perdedor → vencedor.
     */
    val vencedorClashId: String? = null,
    /** True quando o fluxo atual foi iniciado pelo botão Colisão. */
    val colisaoAtiva: Boolean = false,
    /**
     * Contador de transferência exibido (+/–). Default 0.
     * Positivo = pontos tirados do “lado alvo” e dados ao “lado origem”.
     */
    val contadorTransferencia: Int = 0,
    /**
     * Snapshot da iniciativa no momento do [Ataque], para detectar Crash no [Término].
     * id → iniciativa antes da transferência.
     */
    val iniciativasAntesTransferencia: Map<String, Int> = emptyMap(),
    /** Nomes de quem entrou em Crash (iniciativa <= 0) ao longo deste combate,
     * na ordem em que ocorreu — usado pelo log de histórico (pedido do usuário). */
    val eventosCrashAcumulados: List<String> = emptyList(),
    /** CORREÇÃO — log de combate sempre vazio / sem "quem foi atacado" nem
     * iniciativa perdida: antes só existia um único LogTurnoSnapshot? (o
     * ÚLTIMO turno, guardado direto em HistoricoCombateEntry só no
     * [Término] que encerra o combate inteiro) — qualquer turno anterior a
     * esse era perdido, e se o combate fosse encerrado sem uma resolução
     * pendente no exato momento, o snapshot saía vazio (ids nulos). Agora
     * cada turno resolvido (terminarAcao — o "Próximo" que fecha uma
     * ação/ataque — e cada alvo confirmado em confirmarAlvoMultiEAvancar)
     * acrescenta um snapshot aqui, então o combate inteiro fica registrado. */
    val eventosLog: List<LogTurnoSnapshot> = emptyList(),
    val mensagensPendentes: List<String> = emptyList(),
    /** Battle Group → lista de alvos. Personagens NÃO usam este mapa. */
    val declaracoesMulti: Map<String, List<String>> = emptyMap(),
    /** Fila após [Ataque] multi, ordenada por iniciativa desc. */
    val filaResolucaoMulti: List<String> = emptyList(),
    /** Alvo corrente na resolução multi (null = fora do fluxo). */
    val alvoResolucaoAtual: String? = null,
    /** Id do Battle Group resolvendo o ataque multi. */
    val atacanteMultiId: String? = null,
    /** Definido depois que [Ataque]/[Colisão] trava, antes de liberar +/– ou
     *  o reset de Decisivo. Null = ainda aguardando a escolha do tipo. */
    val tipoAtaquePendente: TipoAtaque? = null,
    /** Resposta à pergunta "Foi bem-sucedido?", feita sempre depois do tipo.
     *  Null = ainda aguardando resposta. */
    val ataqueBemSucedido: Boolean? = null,
    /**
     * Iniciativas em resolução temporária (id → valor).
     * A lista e a ordenação usam [ParticipanteIniciativa.iniciativa] até [Próximo].
     * Durante a resolução (Fulminante com ajuste / Decisivo), os valores
     * pendentes ficam aqui e só são gravados definitivamente no Próximo
     * (especificação consolidada §5.3, §7 e críticas finais).
     */
    val iniciativasPendentes: Map<String, Int> = emptyMap(),
    /** Estado oficial do turno atual para o log (§11). */
    val turnStatus: TurnStatus = TurnStatus.INICIADO,
    /** Estado oficial do combate para o log (§11). */
    val combatStatus: CombatStatus = CombatStatus.EM_ANDAMENTO
) {
    /** Pares em Clash (declarações mútuas), calculados pela regra central. */
    fun paresClash(): List<Pair<String, String>> = IniciativasRules.paresClash(declaracoes)

    fun emClash(id: String): Boolean =
        declaracoes[id]?.let { outro -> declaracoes[outro] == id } == true

    fun temDeclaracaoMulti(): Boolean =
        declaracoesMulti.values.any { it.isNotEmpty() }

    fun ehAlvoDeAlgumaDeclaracao(id: String): Boolean =
        declaracoes.containsValue(id) ||
        declaracoesMulti.values.any { id in it }

    fun alvosMultiDo(atacanteId: String): List<String> =
        declaracoesMulti[atacanteId].orEmpty()
}

/** Tipo de ataque escolhido explicitamente pelo usuário, fora do fluxo de
 * Clash — pedido explícito do usuário (Parte C). Battle Group só pode agir
 * como Fulminante (regra do sistema). */
enum class TipoAtaque { FULMINANTE, DECISIVO }

/**
 * Estados oficiais do turno/resolução (especificação consolidada §11).
 * Mantidos durante o fluxo para o log padronizado.
 */
enum class TurnStatus {
    INICIADO,
    ALVO_SELECIONADO,
    TRAVADO,
    EM_RESOLUCAO,
    RESOLVIDO,
    PULADO,
    INCOMPLETO,
    /** Combate encerrado sem resolução pendente (log §11). */
    ENCERRADO
}

/** Estado global do combate (especificação consolidada §11). */
enum class CombatStatus {
    EM_ANDAMENTO,
    ENCERRADO
}

/** Um combatente registrado no log de um combate encerrado — nome e
 * iniciativa final (pedido explícito do usuário). */
data class ParticipanteHistorico(
    /** ID estável do combatente; null apenas em históricos legados. */
    val id: String? = null,
    val nome: String,
    val iniciativaFinal: Int
)

/**
 * Snapshot estruturado do turno no momento do registro (PDF §11).
 * Campos opcionais: preenchidos quando disponíveis no estado.
 */
data class LogTurnoSnapshot(
    /** Rodada real em que a ação ocorreu. */
    val rodada: Int = 1,
    val combatenteAtivoId: String? = null,
    val combatenteAtivoNome: String? = null,
    val alvoId: String? = null,
    val alvoNome: String? = null,
    val tipoAtaque: TipoAtaque? = null,
    val resultadoSucesso: Boolean? = null,
    val vencedorId: String? = null,
    val vencedorNome: String? = null,
    val perdedorId: String? = null,
    val perdedorNome: String? = null,
    val iniciativasAnteriores: Map<String, Int> = emptyMap(),
    val iniciativasAjustadas: Map<String, Int> = emptyMap(),
    /** Perda efetiva de Iniciativa por personagem nesta ação (id -> pontos). */
    val iniciativaPerdidaPorId: Map<String, Int> = emptyMap(),
    val quantidadeTransferida: Int = 0,
    val pontoFulminanteConcedido: Boolean = false,
    val turnStatus: TurnStatus = TurnStatus.INICIADO,
    val motivoEncerramento: String? = null
)

/** Registro de um combate encerrado, guardado pelo log (até 3 — pedido
 * explícito do usuário: "nomes, iniciativa, rodadas, crash").
 * Estendido com estados oficiais e snapshot do turno (PDF §10–§11). */
data class HistoricoCombateEntry(
    val id: String = UUID.randomUUID().toString(),
    val dataHora: Long,
    val participantes: List<ParticipanteHistorico>,
    val rodadasTotais: Int,
    /** Nomes de quem entrou em Crash durante o combate, na ordem em que ocorreu. */
    val eventosCrash: List<String>,
    /** Estado do turno no encerramento (INCOMPLETO se resolução pendente). */
    val turnStatus: TurnStatus = TurnStatus.ENCERRADO,
    /** Sempre ENCERRADO neste registro. */
    val combatStatus: CombatStatus = CombatStatus.ENCERRADO,
    /** CORREÇÃO — era um único LogTurnoSnapshot? (só o último turno antes do
     * combate encerrar); agora guarda TODOS os turnos resolvidos durante o
     * combate, na ordem em que aconteceram. */
    val eventosLog: List<LogTurnoSnapshot> = emptyList()
)
