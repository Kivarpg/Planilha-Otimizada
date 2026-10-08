package com.example.iniciativas

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object IniciativasJsonCodec {

    private const val SCHEMA_VERSION = 4
    private const val MAX_PARTICIPANTES = 256
    private const val MAX_NOME = 120
    private const val MAX_LISTA_AUXILIAR = 256
    private const val MAX_TEXTO_AUXILIAR = 500

    fun encode(state: IniciativasState): String = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("rodada", state.rodada)
        put("ataqueTravado", state.ataqueTravado)
        put("vencedorClashId", state.vencedorClashId ?: JSONObject.NULL)
        put("colisaoAtiva", state.colisaoAtiva)
        put("contadorTransferencia", state.contadorTransferencia)
        put("declaracoes", JSONObject().apply {
            state.declaracoes.forEach { (k, v) -> put(k, v) }
        })
        put("iniciativasAntesTransferencia", JSONObject().apply {
            state.iniciativasAntesTransferencia.forEach { (k, v) -> put(k, v) }
        })
        put("mensagensPendentes", JSONArray(state.mensagensPendentes))
        put("eventosCrashAcumulados", JSONArray(state.eventosCrashAcumulados))
        // CORREÇÃO — log de combate: sem isto, matar o processo/reabrir o
        // app no meio de um combate perdia os turnos já registrados (o
        // campo voltava pro default emptyList()). Reaproveita o mesmo
        // encoder usado no histórico de combates encerrados.
        put("eventosLog", JSONArray().apply {
            state.eventosLog.forEach { log -> put(HistoricoCombateJsonCodec.encodeLog(log)) }
        })
        put("declaracoesMulti", JSONObject().apply {
            state.declaracoesMulti.forEach { (k, v) -> put(k, JSONArray(v)) }
        })
        put("filaResolucaoMulti", JSONArray(state.filaResolucaoMulti))
        put("alvoResolucaoAtual", state.alvoResolucaoAtual ?: JSONObject.NULL)
        put("atacanteMultiId", state.atacanteMultiId ?: JSONObject.NULL)
        put("tipoAtaquePendente", state.tipoAtaquePendente?.name ?: JSONObject.NULL)
        put("ataqueBemSucedido", state.ataqueBemSucedido ?: JSONObject.NULL)
        put("iniciativasPendentes", JSONObject().apply {
            state.iniciativasPendentes.forEach { (k, v) -> put(k, v) }
        })
        put("turnStatus", state.turnStatus.name)
        put("combatStatus", state.combatStatus.name)
        put("participantes", JSONArray().apply {
            state.participantes.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("nome", p.nome)
                    put("iniciativa", p.iniciativa)
                    p.origemNpcId?.let { put("origemNpcId", it) }
                    p.origemBattleGroupId?.let { put("origemBattleGroupId", it) }
                    put("ordemInsercao", p.ordemInsercao)
                    put("jaAgiramNesteTurno", p.jaAgiramNesteTurno)
                    p.reduzidoPorId?.let { put("reduzidoPorId", it) }
                    put("penalidadeClashDefesa", p.penalidadeClashDefesa.coerceAtLeast(0))
                    put("rodadasEmAtordoamento", p.rodadasEmAtordoamento.coerceIn(0, 3))
                })
            }
        })
    }.toString()

    fun decode(json: String): IniciativasState {
        return try {
            val root = JSONObject(json)
            val array = root.optJSONArray("participantes") ?: JSONArray()
            val limite = minOf(array.length(), MAX_PARTICIPANTES)
            val participantes = buildList {
                val ids = mutableSetOf<String>()
                for (i in 0 until limite) {
                    val p = array.getJSONObject(i)
                    val idOriginal = p.optString("id").trim()
                    val id = if (idOriginal.isNotBlank() && ids.add(idOriginal)) idOriginal else UUID.randomUUID().toString().also { ids.add(it) }
                    val nome = p.optString("nome").trim().take(MAX_NOME)
                    if (nome.isBlank()) continue
                    add(
                        ParticipanteIniciativa(
                            id = id,
                            nome = nome,
                            iniciativa = p.optInt("iniciativa", 0),
                            origemNpcId = p.optString("origemNpcId").ifBlank { null },
                            origemBattleGroupId = p.optString("origemBattleGroupId").ifBlank { null },
                            ordemInsercao = p.optLong("ordemInsercao", i.toLong() + 1L),
                            jaAgiramNesteTurno = p.optBoolean("jaAgiramNesteTurno", false),
                            reduzidoPorId = p.optString("reduzidoPorId").ifBlank { null },
                            penalidadeClashDefesa = p.optInt("penalidadeClashDefesa", 0).coerceIn(0, 2),
                            rodadasEmAtordoamento = p.optInt("rodadasEmAtordoamento", 0).coerceIn(0, 3)
                        )
                    )
                }
            }
            val declaracoes = mutableMapOf<String, String>()
            root.optJSONObject("declaracoes")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    declaracoes[k] = obj.optString(k)
                }
            }
            val snapshot = mutableMapOf<String, Int>()
            root.optJSONObject("iniciativasAntesTransferencia")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    snapshot[k] = obj.optInt(k)
                }
            }
            val idsValidos = participantes.map { it.id }.toSet()
            val declaracoesSeguras = declaracoes.filter { (atacante, alvo) ->
                atacante in idsValidos && alvo in idsValidos && atacante != alvo
            }
            val snapshotSeguro = snapshot.filterKeys { it in idsValidos }
            val vencedor = if (root.isNull("vencedorClashId")) null
            else root.optString("vencedorClashId").ifBlank { null }?.takeIf { it in idsValidos }
            val mensagensSeguras = root.optJSONArray("mensagensPendentes")?.let { arr ->
                (0 until minOf(arr.length(), 100)).mapNotNull {
                    arr.optString(it).trim().take(500).takeIf(String::isNotBlank)
                }
            } ?: emptyList()
            val declaracoesMulti = mutableMapOf<String, List<String>>()
            root.optJSONObject("declaracoesMulti")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = obj.optJSONArray(k) ?: JSONArray()
                    declaracoesMulti[k] = (0 until minOf(arr.length(), MAX_LISTA_AUXILIAR))
                        .mapNotNull { arr.optString(it).take(MAX_TEXTO_AUXILIAR).ifBlank { null } }
                }
            }
            val declaracoesMultiSeguras = declaracoesMulti
                .filterKeys { it in idsValidos }
                .mapValues { (_, alvos) -> alvos.filter { it in idsValidos } }
                .filterValues { it.isNotEmpty() }
            val filaMultiSegura = root.optJSONArray("filaResolucaoMulti")?.let { arr ->
                (0 until minOf(arr.length(), MAX_LISTA_AUXILIAR)).mapNotNull { arr.optString(it).ifBlank { null } }.filter { it in idsValidos }
            } ?: emptyList()
            val alvoResolucaoAtualSeguro = root.optString("alvoResolucaoAtual").ifBlank { null }?.takeIf { it in idsValidos }
            val atacanteMultiIdSeguro = root.optString("atacanteMultiId").ifBlank { null }?.takeIf { it in idsValidos }
            val tipoAtaquePendente = root.optString("tipoAtaquePendente").ifBlank { null }
                ?.let { runCatching { TipoAtaque.valueOf(it) }.getOrNull() }
            val ataqueBemSucedido = if (root.isNull("ataqueBemSucedido")) null
                else root.optBoolean("ataqueBemSucedido")
            val iniciativasPendentes = root.optJSONObject("iniciativasPendentes")?.let { obj ->
                obj.keys().asSequence().mapNotNull { key ->
                    val id = key as? String ?: return@mapNotNull null
                    if (id !in idsValidos) return@mapNotNull null
                    id to obj.optInt(id)
                }.toMap()
            } ?: emptyMap()
            val turnStatus = root.optString("turnStatus").ifBlank { null }
                ?.let { runCatching { TurnStatus.valueOf(it) }.getOrNull() }
                ?: TurnStatus.INICIADO
            val combatStatus = root.optString("combatStatus").ifBlank { null }
                ?.let { runCatching { CombatStatus.valueOf(it) }.getOrNull() }
                ?: CombatStatus.EM_ANDAMENTO
            val eventosLogSeguros = root.optJSONArray("eventosLog")?.let { arr ->
                (0 until minOf(arr.length(), MAX_LISTA_AUXILIAR)).mapNotNull { i ->
                    runCatching { HistoricoCombateJsonCodec.decodeLog(arr.getJSONObject(i)) }.getOrNull()
                }
            } ?: emptyList()
            IniciativasState(
                rodada = root.optInt("rodada", 1).coerceIn(1, 1_000_000),
                participantes = participantes,
                declaracoes = declaracoesSeguras,
                ataqueTravado = root.optBoolean("ataqueTravado", false),
                vencedorClashId = vencedor,
                colisaoAtiva = root.optBoolean("colisaoAtiva", false),
                contadorTransferencia = root.optInt("contadorTransferencia", 0).coerceIn(-10_000, 10_000),
                iniciativasAntesTransferencia = snapshotSeguro,
                mensagensPendentes = mensagensSeguras,
                eventosCrashAcumulados = root.optJSONArray("eventosCrashAcumulados")?.let { arr ->
                    (0 until minOf(arr.length(), MAX_LISTA_AUXILIAR)).mapNotNull { arr.optString(it).trim().take(120).takeIf(String::isNotBlank) }
                } ?: emptyList(),
                eventosLog = eventosLogSeguros,
                declaracoesMulti = declaracoesMultiSeguras,
                filaResolucaoMulti = filaMultiSegura,
                alvoResolucaoAtual = alvoResolucaoAtualSeguro,
                atacanteMultiId = atacanteMultiIdSeguro,
                tipoAtaquePendente = tipoAtaquePendente,
                ataqueBemSucedido = ataqueBemSucedido,
                iniciativasPendentes = iniciativasPendentes,
                turnStatus = turnStatus,
                combatStatus = combatStatus
            )
        } catch (_: Exception) {
            IniciativasState()
        }
    }
}
