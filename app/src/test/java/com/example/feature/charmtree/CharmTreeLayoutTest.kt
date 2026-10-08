package com.example.feature.charmtree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharmTreeLayoutTest {
    @Test
    fun sharedPrerequisiteIsRenderedOnlyOnce() {
        val result = CharmPrerequisiteTreeBuilder.build(
            "root",
            listOf(
                CharmTreeEntry("root", "Raiz", prerequisiteIds = listOf("a", "b")),
                CharmTreeEntry("a", "A", prerequisiteIds = listOf("shared")),
                CharmTreeEntry("b", "B", prerequisiteIds = listOf("shared")),
                CharmTreeEntry("shared", "Compartilhado")
            )
        )!!

        val levels = buildCharmTreeLevels(listOf(result.root))
        val ids = levels.flatten().map { it.key }

        assertEquals(setOf("root", "a", "b", "shared"), ids.toSet())
        assertEquals(4, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun sharedPrerequisiteStaysAboveAllDependants() {
        val result = CharmPrerequisiteTreeBuilder.build(
            "root",
            listOf(
                CharmTreeEntry("root", "Raiz", prerequisiteIds = listOf("a", "b")),
                CharmTreeEntry("a", "A", prerequisiteIds = listOf("shared")),
                CharmTreeEntry("b", "B", prerequisiteIds = listOf("shared", "deep")),
                CharmTreeEntry("shared", "Compartilhado"),
                CharmTreeEntry("deep", "Profundo", prerequisiteIds = listOf("shared"))
            )
        )!!

        val levels = buildCharmTreeLevels(listOf(result.root))
        val depthById = levels
            .flatMapIndexed { index, level -> level.map { it.key to index } }
            .toMap()

        assertTrue(depthById.getValue("shared") < depthById.getValue("a"))
        assertTrue(depthById.getValue("shared") < depthById.getValue("b"))
        assertTrue(depthById.getValue("shared") < depthById.getValue("deep"))
        assertTrue(depthById.getValue("a") < depthById.getValue("root"))
        assertTrue(depthById.getValue("b") < depthById.getValue("root"))
    }

    @Test
    fun requestedLowestRequirementRootCanBeLocatedInsideFullCatalogLayout() {
        val base = CharmTreeEntry("base", "Base", ability = "Melee", essenceMinimum = 1, abilityMinimum = 1)
        val advanced = CharmTreeEntry("advanced", "Advanced", ability = "Melee", prerequisiteIds = listOf("base"), essenceMinimum = 2, abilityMinimum = 3)
        val independent = CharmTreeEntry("independent", "Independent", ability = "Melee", essenceMinimum = 1, abilityMinimum = 2)

        val result = requireNotNull(
            CharmPrerequisiteTreeBuilder.build(
                rootId = "base",
                charms = listOf(base, advanced, independent),
                includeFullCatalog = true
            )
        )
        val levels = buildCharmTreeLevels(result.displayRoots)
        val level = findCharmTreeVisualLevel(levels, result.root.charm.id)

        assertTrue(level != null)
        assertEquals("base", result.root.charm.id)
        assertTrue(levels[level!!].any { it.key == "base" })
    }
    @Test
    fun fullCatalogKeepsIndependentMinimumRootsOnFirstLevel() {
        val minimumA = CharmTreeEntry("min-a", "Alfa", essenceMinimum = 1, abilityMinimum = 2)
        val minimumB = CharmTreeEntry("min-b", "Beta", essenceMinimum = 1, abilityMinimum = 2)
        val child = CharmTreeEntry(
            "child", "Filho",
            prerequisiteIds = listOf("min-b"),
            essenceMinimum = 2,
            abilityMinimum = 3
        )

        val result = requireNotNull(
            CharmPrerequisiteTreeBuilder.build(
                rootId = "min-a",
                charms = listOf(minimumA, minimumB, child),
                includeFullCatalog = true
            )
        )
        val levels = buildCharmTreeLevelsByPrerequisiteDepth(result.displayRoots)

        assertEquals(listOf("min-a", "min-b"), levels.first().map { it.key })
        assertEquals(0, findCharmTreeVisualLevel(levels, "min-a"))
        assertEquals(0, findCharmTreeVisualLevel(levels, "min-b"))
        assertTrue(findCharmTreeVisualLevel(levels, "child")!! > 0)
    }


    @Test
    fun memoizedSharedSubtreeKeepsDepthRelativeToEachParent() {
        val result = requireNotNull(
            CharmPrerequisiteTreeBuilder.build(
                rootId = "root",
                charms = listOf(
                    CharmTreeEntry("root", "Raiz", prerequisiteIds = listOf("short", "long")),
                    CharmTreeEntry("short", "Curto", prerequisiteIds = listOf("shared")),
                    CharmTreeEntry("long", "Longo", prerequisiteIds = listOf("middle")),
                    CharmTreeEntry("middle", "Meio", prerequisiteIds = listOf("shared")),
                    CharmTreeEntry("shared", "Compartilhado", prerequisiteIds = listOf("base")),
                    CharmTreeEntry("base", "Base")
                )
            )
        )

        val shortShared = result.root.children.first { it.charm.id == "short" }.children.single()
        val longShared = result.root.children.first { it.charm.id == "long" }.children.single().children.single()

        assertEquals(2, shortShared.depth)
        assertEquals(3, shortShared.children.single().depth)
        assertEquals(3, longShared.depth)
        assertEquals(4, longShared.children.single().depth)
    }

    @Test
    fun cycleDetectionStillWorksWhenAnotherBranchCanBeMemoized() {
        val result = requireNotNull(
            CharmPrerequisiteTreeBuilder.build(
                rootId = "root",
                charms = listOf(
                    CharmTreeEntry("root", "Raiz", prerequisiteIds = listOf("safe", "cycle-a")),
                    CharmTreeEntry("safe", "Seguro", prerequisiteIds = listOf("base")),
                    CharmTreeEntry("base", "Base"),
                    CharmTreeEntry("cycle-a", "Ciclo A", prerequisiteIds = listOf("cycle-b")),
                    CharmTreeEntry("cycle-b", "Ciclo B", prerequisiteIds = listOf("cycle-a"))
                )
            )
        )

        assertTrue(result.cycleDetected)
        val safe = result.root.children.first { it.charm.id == "safe" }
        assertEquals("base", safe.children.single().charm.id)
    }

}
