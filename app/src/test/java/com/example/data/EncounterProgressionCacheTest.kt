package com.example.data

import com.example.model.EncounterProgressionRoadmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EncounterProgressionCacheTest {
    @Test
    fun `cache e transitório e separado do NPC`() {
        val cache = EncounterProgressionCache()
        val roadmap = EncounterProgressionRoadmap(proximoPasso = 2)

        assertNull(cache.get("npc-1"))
        cache.put("npc-1", roadmap)
        assertEquals(roadmap, cache.get("npc-1"))

        cache.remove("npc-1")
        assertNull(cache.get("npc-1"))
    }

    @Test
    fun `limpar remove todos os roadmaps em memoria`() {
        val cache = EncounterProgressionCache()
        cache.put("npc-1", EncounterProgressionRoadmap())
        cache.put("npc-2", EncounterProgressionRoadmap())

        cache.clear()

        assertNull(cache.get("npc-1"))
        assertNull(cache.get("npc-2"))
    }
    @Test
    fun `cache aplica limite LRU para evitar crescimento indefinido`() {
        val cache = EncounterProgressionCache(maxEntries = 2)
        val primeiro = EncounterProgressionRoadmap(proximoPasso = 1)
        val segundo = EncounterProgressionRoadmap(proximoPasso = 2)
        val terceiro = EncounterProgressionRoadmap(proximoPasso = 3)

        cache.put("npc-1", primeiro)
        cache.put("npc-2", segundo)
        assertEquals(primeiro, cache.get("npc-1"))

        cache.put("npc-3", terceiro)

        assertNull(cache.get("npc-2"))
        assertEquals(primeiro, cache.get("npc-1"))
        assertEquals(terceiro, cache.get("npc-3"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `cache remove e clear continuam liberando entradas`() {
        val cache = EncounterProgressionCache(maxEntries = 2)
        cache.put("npc-1", EncounterProgressionRoadmap())
        cache.put("npc-2", EncounterProgressionRoadmap())

        cache.remove("npc-1")
        assertNull(cache.get("npc-1"))
        assertEquals(1, cache.size())

        cache.clear()
        assertEquals(0, cache.size())
    }

    @Test
    fun `metricas distinguem acertos e faltas do cache`() {
        val metrics = EncounterProgressionMetrics()
        val cache = EncounterProgressionCache(metrics = metrics)
        cache.put("npc-1", EncounterProgressionRoadmap())

        assertEquals(null, cache.get("npc-2"))
        assertEquals(EncounterProgressionRoadmap(), cache.get("npc-1"))

        val snapshot = metrics.snapshot()
        assertEquals(1L, snapshot.cacheHits)
        assertEquals(1L, snapshot.cacheMisses)
        assertEquals(0.5, snapshot.cacheHitRate, 0.0)
    }

    @Test
    fun `peek consulta cache sem alterar metricas de consumo`() {
        val metrics = EncounterProgressionMetrics()
        val cache = EncounterProgressionCache(metrics = metrics)
        val roadmap = EncounterProgressionRoadmap(proximoPasso = 1)
        cache.put("npc-1", roadmap)

        assertEquals(roadmap, cache.peek("npc-1"))
        assertNull(cache.peek("npc-2"))

        val snapshot = metrics.snapshot()
        assertEquals(0L, snapshot.cacheHits)
        assertEquals(0L, snapshot.cacheMisses)
    }

}

