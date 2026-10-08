package com.example.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LunarSpiritFormPreparedContextCacheTest {
    private val source = File("src/main/java/com/example/data/LunarSpiritFormStrategy.kt").readText()

    @Test fun `spirit form planning reuses canonical prepared route contexts locally`() {
        assertTrue(source.contains("private class RouteContextCache"))
        assertTrue(source.contains("traits.sortedBy { it.name }"))
        assertTrue(source.contains("prepared.getOrPut(key(traits))"))
        assertTrue(source.contains("val routeContexts = RouteContextCache(catalogo)"))
        assertTrue(source.contains("val context = routeContexts.get(traits)"))
    }

    @Test fun `route context cache is not global cross npc state`() {
        assertFalse(source.contains("private val routeContexts ="))
        assertFalse(source.contains("object RouteContextCache"))
    }

    @Test fun `prepare remains centralized in cache`() {
        assertEquals(1, Regex("""LunarCharmArchetypePolicy\.prepare\(catalogo, traits\)""").findAll(source).count())
    }
}
