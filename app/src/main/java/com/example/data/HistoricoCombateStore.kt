package com.example.data

import android.content.SharedPreferences
import com.example.iniciativas.HistoricoCombateEntry
import com.example.iniciativas.HistoricoCombateJsonCodec

/** Persistência do log de histórico de combates — guarda até 3 registros
 * (pedido explícito do usuário), o mais recente primeiro. */
class HistoricoCombateStore(private val prefs: SharedPreferences) {

    private companion object {
        const val KEY = "exalted_historico_combates_aba12"
        const val MAX_REGISTROS = 3
    }

    fun carregar(): List<HistoricoCombateEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return HistoricoCombateJsonCodec.decode(raw)
    }

    /** Adiciona um novo registro ao topo do log, descartando o mais antigo
     * se já houver 3. Retorna a lista atualizada, já persistida. */
    fun adicionar(entry: HistoricoCombateEntry): List<HistoricoCombateEntry> {
        val atualizada = (listOf(entry) + carregar()).take(MAX_REGISTROS)
        prefs.edit().putString(KEY, HistoricoCombateJsonCodec.encode(atualizada)).apply()
        return atualizada
    }
}
