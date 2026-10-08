package com.example.iniciativas

import org.json.JSONArray
import org.json.JSONObject

/** Codec JSON pro log de histórico de combates (até 3). Estendido com
 * turnStatus, combatStatus e logTurno (PDF §10–§11). */
object HistoricoCombateJsonCodec {

    private const val MAX_REGISTROS = 3
    private const val MAX_PARTICIPANTES = 256
    private const val MAX_EVENTOS = 100
    private const val MAX_TAMANHO_TEXTO = 500

    fun encode(lista: List<HistoricoCombateEntry>): String =
        JSONArray().apply {
            lista.forEach { entry ->
                put(JSONObject().apply {
                    put("id", entry.id)
                    put("dataHora", entry.dataHora)
                    put("rodadasTotais", entry.rodadasTotais)
                    put("participantes", JSONArray().apply {
                        entry.participantes.forEach { p ->
                            put(JSONObject().apply {
                                p.id?.let { put("id", it) }
                                put("nome", p.nome)
                                put("iniciativaFinal", p.iniciativaFinal)
                            })
                        }
                    })
                    put("eventosCrash", JSONArray(entry.eventosCrash))
                    put("turnStatus", entry.turnStatus.name)
                    put("combatStatus", entry.combatStatus.name)
                    // CORREÇÃO — era um único objeto opcional "logTurno"
                    // (só o último turno); agora um array com todos os
                    // turnos do combate, na ordem em que aconteceram.
                    put("eventosLog", JSONArray().apply {
                        entry.eventosLog.forEach { log -> put(encodeLog(log)) }
                    })
                })
            }
        }.toString()

    fun decode(json: String): List<HistoricoCombateEntry> = try {
        val arr = JSONArray(json)
        (0 until minOf(arr.length(), MAX_REGISTROS)).map { i ->
            val obj = arr.getJSONObject(i)
            val participantesArr = obj.optJSONArray("participantes")
            val participantes = if (participantesArr == null) emptyList() else
                (0 until minOf(participantesArr.length(), MAX_PARTICIPANTES)).map { j ->
                    val p = participantesArr.getJSONObject(j)
                    ParticipanteHistorico(
                        id = p.optString("id").takeIf { it.isNotBlank() }?.take(MAX_TAMANHO_TEXTO),
                        nome = p.optString("nome", "").take(MAX_TAMANHO_TEXTO),
                        iniciativaFinal = p.optInt("iniciativaFinal", 0)
                    )
                }
            val eventosArr = obj.optJSONArray("eventosCrash")
            val eventos = if (eventosArr == null) emptyList() else
                (0 until minOf(eventosArr.length(), MAX_EVENTOS))
                    .map { eventosArr.optString(it, "").take(MAX_TAMANHO_TEXTO) }
                    .filter { it.isNotBlank() }
            val turnStatus = obj.optString("turnStatus").ifBlank { null }
                ?.let { runCatching { TurnStatus.valueOf(it) }.getOrNull() }
                ?: TurnStatus.ENCERRADO
            val combatStatus = obj.optString("combatStatus").ifBlank { null }
                ?.let { runCatching { CombatStatus.valueOf(it) }.getOrNull() }
                ?: CombatStatus.ENCERRADO
            // Compatível com registros antigos salvos com o campo único
            // "logTurno" (objeto) — migra pra lista de 1 item. Registros
            // novos usam "eventosLog" (array).
            val eventosLog = obj.optJSONArray("eventosLog")?.let { arr2 ->
                (0 until minOf(arr2.length(), MAX_EVENTOS)).mapNotNull { j ->
                    runCatching { decodeLog(arr2.getJSONObject(j)) }.getOrNull()
                }
            } ?: obj.optJSONObject("logTurno")?.let { listOf(decodeLog(it)) }
            ?: emptyList()
            HistoricoCombateEntry(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                dataHora = obj.optLong("dataHora", 0L),
                participantes = participantes,
                rodadasTotais = obj.optInt("rodadasTotais", 1),
                eventosCrash = eventos,
                turnStatus = turnStatus,
                combatStatus = combatStatus,
                eventosLog = eventosLog
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    internal fun encodeLog(log: LogTurnoSnapshot): JSONObject = JSONObject().apply {
        put("rodada", log.rodada)
        put("combatenteAtivoId", log.combatenteAtivoId ?: JSONObject.NULL)
        put("combatenteAtivoNome", log.combatenteAtivoNome ?: JSONObject.NULL)
        put("alvoId", log.alvoId ?: JSONObject.NULL)
        put("alvoNome", log.alvoNome ?: JSONObject.NULL)
        put("tipoAtaque", log.tipoAtaque?.name ?: JSONObject.NULL)
        put("resultadoSucesso", log.resultadoSucesso ?: JSONObject.NULL)
        put("vencedorId", log.vencedorId ?: JSONObject.NULL)
        put("vencedorNome", log.vencedorNome ?: JSONObject.NULL)
        put("perdedorId", log.perdedorId ?: JSONObject.NULL)
        put("perdedorNome", log.perdedorNome ?: JSONObject.NULL)
        put("iniciativasAnteriores", JSONObject().apply {
            log.iniciativasAnteriores.forEach { (k, v) -> put(k, v) }
        })
        put("iniciativasAjustadas", JSONObject().apply {
            log.iniciativasAjustadas.forEach { (k, v) -> put(k, v) }
        })
        put("iniciativaPerdidaPorId", JSONObject().apply {
            log.iniciativaPerdidaPorId.forEach { (k, v) -> put(k, v) }
        })
        put("quantidadeTransferida", log.quantidadeTransferida)
        put("pontoFulminanteConcedido", log.pontoFulminanteConcedido)
        put("turnStatus", log.turnStatus.name)
        put("motivoEncerramento", log.motivoEncerramento ?: JSONObject.NULL)
    }

    internal fun decodeLog(obj: JSONObject): LogTurnoSnapshot {
        fun mapInt(key: String): Map<String, Int> {
            val o = obj.optJSONObject(key) ?: return emptyMap()
            return o.keys().asSequence().mapNotNull { k ->
                val id = k as? String ?: return@mapNotNull null
                id to o.optInt(id)
            }.toMap()
        }
        fun str(key: String): String? =
            if (obj.isNull(key)) null else obj.optString(key).take(MAX_TAMANHO_TEXTO).ifBlank { null }
        fun bool(key: String): Boolean? =
            if (obj.isNull(key)) null else obj.optBoolean(key)
        return LogTurnoSnapshot(
            rodada = obj.optInt("rodada", 1).coerceAtLeast(1),
            combatenteAtivoId = str("combatenteAtivoId"),
            combatenteAtivoNome = str("combatenteAtivoNome"),
            alvoId = str("alvoId"),
            alvoNome = str("alvoNome"),
            tipoAtaque = str("tipoAtaque")?.let { runCatching { TipoAtaque.valueOf(it) }.getOrNull() },
            resultadoSucesso = bool("resultadoSucesso"),
            vencedorId = str("vencedorId"),
            vencedorNome = str("vencedorNome"),
            perdedorId = str("perdedorId"),
            perdedorNome = str("perdedorNome"),
            iniciativasAnteriores = mapInt("iniciativasAnteriores"),
            iniciativasAjustadas = mapInt("iniciativasAjustadas"),
            iniciativaPerdidaPorId = mapInt("iniciativaPerdidaPorId"),
            quantidadeTransferida = obj.optInt("quantidadeTransferida", 0),
            pontoFulminanteConcedido = obj.optBoolean("pontoFulminanteConcedido", false),
            turnStatus = str("turnStatus")?.let { runCatching { TurnStatus.valueOf(it) }.getOrNull() }
                ?: TurnStatus.ENCERRADO,
            motivoEncerramento = str("motivoEncerramento")
        )
    }
}
