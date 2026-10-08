package com.example.data

import com.example.model.ArquetipoEncontro

/** Lunar-specific layer: Attributes drive Charm selection, not Abilities. */
internal object LunarArchetypePolicy {
    fun attributePriority(archetype: ArquetipoEncontro, favoredOrCaste: List<String>): List<String> =
        EncounterArchetypePolicy.lunarAttributePriority(archetype, favoredOrCaste)

    fun subdivisionRank(archetype: ArquetipoEncontro, subdivision: String?): Int {
        if (subdivision == null) return 6
        return when {
            archetype == ArquetipoEncontro.FISICO && subdivision.equals("Ofensivo", true) -> 0
            subdivision.equals("Defensivo", true) -> 1
            subdivision.equals("Utilitário", true) -> 2
            subdivision.equals("Mobilidade", true) -> 3
            subdivision.equals("Ofensivo", true) -> 4
            subdivision.equals("Sangue do Coração", true) -> 5
            else -> 6
        }
    }
}
