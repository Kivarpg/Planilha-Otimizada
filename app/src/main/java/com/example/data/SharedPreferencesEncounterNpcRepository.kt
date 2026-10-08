package com.example.data

import android.content.SharedPreferences
import com.example.domain.encounter.EncounterNpcRepository
import com.example.model.NpcEncontro

/** SharedPreferences-backed persistence for the Encounters feature. */
class SharedPreferencesEncounterNpcRepository(
    prefs: SharedPreferences,
    private val onFailure: (Exception) -> Unit,
) : EncounterNpcRepository {
    private val store = EncounterNpcStore(prefs)
    private val persistence = EncounterNpcPersistenceCoordinator(
        write = store::save,
        onFailure = onFailure,
    )

    override fun load(): List<NpcEncontro> =
        persistence.latestPending() ?: store.load(onFailure)

    override fun save(npcs: List<NpcEncontro>) {
        persistence.submit(npcs)
    }

    override fun close() {
        persistence.close()
    }
}
