package com.example.data

import android.content.SharedPreferences
import com.example.model.NpcEncontro
import org.json.JSONArray

/** Persistence boundary for NPCs produced by the encounter generator. */
class EncounterNpcStore(private val prefs: SharedPreferences) {
    fun save(npcs: List<NpcEncontro>) {
        val array = JSONArray().apply {
            npcs.forEach { put(NpcEncontroJsonCodec.encodeObject(it)) }
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun load(onError: (Exception) -> Unit): List<NpcEncontro> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                NpcEncontroJsonCodec.decode(array.getJSONObject(index).toString())
            }
        } catch (e: Exception) {
            onError(e)
            emptyList()
        }
    }

    private companion object { const val KEY = "exalted_npcs_encontro" }
}
