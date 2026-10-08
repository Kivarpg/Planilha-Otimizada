package com.example.domain.encounter

import com.example.model.NpcEncontro

/** Persistence contract for NPCs created by the Encounters feature. */
interface EncounterNpcRepository : AutoCloseable {
    fun load(): List<NpcEncontro>
    fun save(npcs: List<NpcEncontro>)
    override fun close()
}
