package com.example.data

import android.content.SharedPreferences
import com.example.iniciativas.IniciativasJsonCodec
import com.example.iniciativas.IniciativasState

/** Persistence boundary for the independent conflict/iniziatives state. */
class InitiativesStore(private val prefs: SharedPreferences) {
    fun save(state: IniciativasState) {
        prefs.edit().putString(KEY, IniciativasJsonCodec.encode(state)).apply()
    }

    fun load(onError: (Exception) -> Unit): IniciativasState {
        val raw = prefs.getString(KEY, null) ?: return IniciativasState()
        return try {
            IniciativasJsonCodec.decode(raw)
        } catch (e: Exception) {
            onError(e)
            IniciativasState()
        }
    }

    private companion object { const val KEY = "exalted_iniciativas_aba13" }
}
