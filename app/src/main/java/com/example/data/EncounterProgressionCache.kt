package com.example.data

import com.example.model.EncounterProgressionRoadmap
import java.util.LinkedHashMap

/**
 * Cache transitório do roadmap; nunca faz parte do estado persistido do NPC.
 *
 * O limite impede que roadmaps de NPCs antigos permaneçam indefinidamente na
 * memória caso algum fluxo futuro deixe de chamar remove() explicitamente.
 * A remoção continua sendo feita pelo ciclo de vida normal; o LRU é apenas
 * uma segunda barreira de retenção.
 */
class EncounterProgressionCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val metrics: EncounterProgressionMetrics = EncounterProgressionMetrics()
) {
    init {
        require(maxEntries > 0) { "maxEntries deve ser maior que zero" }
    }

    private val byNpcId = object : LinkedHashMap<String, EncounterProgressionRoadmap>(
        maxEntries.coerceAtLeast(16),
        LOAD_FACTOR,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, EncounterProgressionRoadmap>?
        ): Boolean = size > maxEntries
    }

    @Synchronized
    fun get(npcId: String): EncounterProgressionRoadmap? = byNpcId[npcId].also {
        if (it == null) metrics.recordCacheMiss() else metrics.recordCacheHit()
    }

    /** Consulta de manutenção que não altera métricas de consumo do +XP. */
    @Synchronized
    fun peek(npcId: String): EncounterProgressionRoadmap? = byNpcId[npcId]

    @Synchronized
    fun put(npcId: String, roadmap: EncounterProgressionRoadmap) {
        byNpcId[npcId] = roadmap
    }

    @Synchronized
    fun remove(npcId: String) {
        byNpcId.remove(npcId)
    }

    @Synchronized
    fun clear() {
        byNpcId.clear()
    }

    @Synchronized
    internal fun size(): Int = byNpcId.size

    companion object {
        const val DEFAULT_MAX_ENTRIES = 64
        private const val LOAD_FACTOR = 0.75f
    }
}
