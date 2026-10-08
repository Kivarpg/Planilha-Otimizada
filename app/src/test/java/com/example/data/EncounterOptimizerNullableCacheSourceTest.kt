package com.example.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncounterOptimizerNullableCacheSourceTest {
    @Test
    fun `memo legado nullable nunca usa acesso indexado inseguro`() {
        val source = File("src/main/java/com/example/data/EncounterCharmRouteOptimizer.kt").readText()
        assertTrue(source.contains("legacyEligibilityCache?.get(key)"))
        assertFalse(Regex("""legacyEligibilityCache\s*\[""").containsMatchIn(source))
        assertFalse(Regex("""eligibilityMemo\s*\[""").containsMatchIn(source))
    }
}
