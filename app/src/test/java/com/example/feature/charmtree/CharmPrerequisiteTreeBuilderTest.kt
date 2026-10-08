package com.example.feature.charmtree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharmPrerequisiteTreeBuilderTest {
    @Test
    fun independentNoPrerequisiteCharmsAreNotGrouped() {
        val a = CharmTreeEntry(id = "a", name = "A", ability = "Melee", essenceMinimum = 1, abilityMinimum = 1)
        val b = CharmTreeEntry(id = "b", name = "B", ability = "Melee", essenceMinimum = 1, abilityMinimum = 1)
        val result = requireNotNull(CharmPrerequisiteTreeBuilder.build("a", listOf(a, b)))
        assertEquals(listOf("a"), result.displayRoots.map { it.charm.id })
    }

    @Test
    fun declaredPrerequisiteChainIsPreserved() {
        val base = CharmTreeEntry(id = "base", name = "Base", ability = "Melee")
        val advanced = CharmTreeEntry(id = "advanced", name = "Advanced", ability = "Melee", prerequisiteIds = listOf("base"))
        val result = requireNotNull(CharmPrerequisiteTreeBuilder.build("advanced", listOf(base, advanced)))
        assertEquals("advanced", result.displayRoots.single().charm.id)
        assertEquals("base", result.displayRoots.single().children.single().charm.id)
    }

    @Test
    fun fullCatalogKeepsEveryRealBranchWithoutArtificialDependencies() {
        val base = CharmTreeEntry(id = "base", name = "Base", ability = "Melee", essenceMinimum = 1, abilityMinimum = 1)
        val advanced = CharmTreeEntry(id = "advanced", name = "Advanced", ability = "Melee", prerequisiteIds = listOf("base"), essenceMinimum = 2, abilityMinimum = 3)
        val independent = CharmTreeEntry(id = "independent", name = "Independent", ability = "Melee", essenceMinimum = 1, abilityMinimum = 2)

        val result = requireNotNull(
            CharmPrerequisiteTreeBuilder.build(
                rootId = "base",
                charms = listOf(base, advanced, independent),
                includeFullCatalog = true
            )
        )

        assertEquals("base", result.root.charm.id)
        assertEquals(listOf("independent", "advanced"), result.displayRoots.map { it.charm.id })
        assertEquals("base", result.displayRoots.last().children.single().charm.id)
        assertEquals(emptyList<CharmTreeNode>(), result.displayRoots.first().children)
    }
    @Test
    fun cycleIsDetectedAndDepthIsPreserved() {
        val a = CharmTreeEntry(id = "a", name = "A", prerequisiteIds = listOf("b"))
        val b = CharmTreeEntry(id = "b", name = "B", prerequisiteIds = listOf("a"))
        val result = requireNotNull(CharmPrerequisiteTreeBuilder.build("a", listOf(a, b)))
        assertTrue(result.cycleDetected)
        assertEquals(0, result.root.depth)
        assertEquals(1, result.root.children.single().depth)
        assertEquals(2, result.root.children.single().children.single().depth)
    }

    @Test
    fun sharedPrerequisiteIsNotTreatedAsCycle() {
        val shared = CharmTreeEntry(id = "shared", name = "Shared")
        val a = CharmTreeEntry(id = "a", name = "A", prerequisiteIds = listOf("shared"))
        val b = CharmTreeEntry(id = "b", name = "B", prerequisiteIds = listOf("shared"))
        val result = requireNotNull(
            CharmPrerequisiteTreeBuilder.build("shared", listOf(shared, a, b), includeFullCatalog = true)
        )
        assertEquals(false, result.cycleDetected)
        assertEquals(listOf("a", "b"), result.displayRoots.map { it.charm.id })
        assertEquals(listOf("shared", "shared"), result.displayRoots.map { it.children.single().charm.id })
        assertEquals(listOf(1, 1), result.displayRoots.map { it.children.single().depth })
    }
}
