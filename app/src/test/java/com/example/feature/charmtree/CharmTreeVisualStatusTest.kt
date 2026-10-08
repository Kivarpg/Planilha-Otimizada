package com.example.feature.charmtree

import kotlin.test.Test
import kotlin.test.assertEquals

class CharmTreeVisualStatusTest {
    private val root = CharmTreeEntry(id = "root", name = "Raiz")
    private val child = CharmTreeEntry(id = "child", name = "Filho", prerequisiteIds = listOf("root"))
    private val names = mapOf("root" to "Raiz", "child" to "Filho")

    @Test fun `focal status has priority`() {
        assertEquals(CharmTreeVisualStatus.FOCAL, charmTreeVisualStatus(root, "root", emptySet(), names))
    }

    @Test fun `owned charm is acquired`() {
        assertEquals(CharmTreeVisualStatus.ACQUIRED, charmTreeVisualStatus(root, "other", setOf("raiz"), names))
    }

    @Test fun `root without prerequisites is available`() {
        assertEquals(CharmTreeVisualStatus.AVAILABLE, charmTreeVisualStatus(root, "other", emptySet(), names))
    }

    @Test fun `dependent charm is available only when direct prerequisite is acquired`() {
        assertEquals(CharmTreeVisualStatus.BLOCKED, charmTreeVisualStatus(child, "other", emptySet(), names))
        assertEquals(CharmTreeVisualStatus.AVAILABLE, charmTreeVisualStatus(child, "other", setOf("raiz"), names))
    }
}
