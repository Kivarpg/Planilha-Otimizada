package com.example.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Owns only the periodic-backup persistence concerns. */
class BackupStore(private val prefs: SharedPreferences) {
    data class Snapshot(val timestamp: String, val json: String)

    fun save(json: String) {
        val current = load()
        if (current.firstOrNull()?.json == json) return
        val timestamp = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        val snapshots = (listOf(Snapshot(timestamp, json)) + current).take(MAX_BACKUPS)
        val array = JSONArray().apply {
            snapshots.forEach { snapshot ->
                put(JSONObject().apply {
                    put("timestamp", snapshot.timestamp)
                    put("json", snapshot.json)
                })
            }
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun load(): List<Snapshot> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    runCatching {
                        val item = array.getJSONObject(index)
                        add(Snapshot(item.getString("timestamp"), item.getString("json")))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY = "exalted_backup_slots"
        const val MAX_BACKUPS = 5
    }
}
