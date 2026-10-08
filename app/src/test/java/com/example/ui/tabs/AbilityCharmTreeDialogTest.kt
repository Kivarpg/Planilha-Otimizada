package com.example.ui.tabs

import com.example.model.Encanto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbilityCharmTreeDialogTest {
    private fun charm(
        id: String,
        nome: String,
        essencia: Int,
        habilidade: Int
    ) = Encanto(
        id = id,
        nome = nome,
        habilidadeVinculada = "Armas Brancas",
        minEssencia = essencia,
        minHabilidade = habilidade
    )

    @Test
    fun `tree cache with disabled capacity never retains entries`() {
        val entry = com.example.feature.charmtree.CharmTreeEntry(id = "test", name = "test")
        val root = com.example.feature.charmtree.CharmTreeNode(entry, 0, emptyList())
        val prepared = PreparedCharmTree(listOf(entry), com.example.feature.charmtree.CharmTreeResult(root))
        val cache = CharmTreeOpenCache(maxEntries = 0)
        cache.put("test", prepared)
        assertEquals(null, cache["test"])
        assertEquals(0, cache.size())
    }

    @Test
    fun `tree cache evicts least recently used and normalizes keys`() {
        fun prepared(id: String): PreparedCharmTree {
            val entry = com.example.feature.charmtree.CharmTreeEntry(id = id, name = id)
            val root = com.example.feature.charmtree.CharmTreeNode(entry, 0, emptyList())
            return PreparedCharmTree(listOf(entry), com.example.feature.charmtree.CharmTreeResult(root))
        }
        val cache = CharmTreeOpenCache(maxEntries = 2)
        val first = prepared("first")
        val second = prepared("second")
        val third = prepared("third")
        cache.put(" FIRST ", first)
        cache.put("second", second)
        assertEquals(first, cache["first"])
        cache.put("third", third)
        assertEquals(null, cache["SECOND"])
        assertEquals(first, cache[" First "])
        assertEquals(third, cache["THIRD"])
        assertEquals(2, cache.size())
    }

    @Test
    fun `empty ability drawer has no initial charm`() {
        assertEquals(null, lowestRequirementCharm(emptyList()))
    }

    @Test
    fun `charm id resolves exact name and requirement ties consistently`() {
        val first = charm("a", "Mesmo Encanto", 1, 1)
        val second = charm("z", "Mesmo Encanto", 1, 1)
        assertEquals("a", lowestRequirementCharm(listOf(second, first))?.id)
        assertEquals("a", lowestRequirementCharm(listOf(first, second))?.id)
    }

    @Test
    fun `long press starts at lowest essence before ability`() {
        val selected = lowestRequirementCharm(
            listOf(
                charm("skill-low", "Habilidade menor mas essência maior", 2, 1),
                charm("ess-low", "Essência menor", 1, 5)
            )
        )
        assertEquals("ess-low", selected?.id)
    }

    @Test
    fun `ability breaks ties after essence`() {
        val selected = lowestRequirementCharm(
            listOf(
                charm("high", "Habilidade alta", 1, 4),
                charm("low", "Habilidade baixa", 1, 2)
            )
        )
        assertEquals("low", selected?.id)
    }

    @Test
    fun `name then id only make exact requirement ties deterministic`() {
        val selected = lowestRequirementCharm(
            listOf(
                charm("z", "Beta", 1, 1),
                charm("a", "Alfa", 1, 1)
            )
        )
        assertEquals("a", selected?.id)
    }
    @Test
    fun `visual start offset follows selected charm level instead of global tree top`() {
        val top = com.example.feature.charmtree.initialCharmViewportOffset(
            visualLevel = 0, viewportHeight = androidx.compose.ui.unit.Dp(500f),
            nodeHeight = androidx.compose.ui.unit.Dp(104f), verticalGap = androidx.compose.ui.unit.Dp(38f),
            contentPadding = androidx.compose.ui.unit.Dp(24f)
        )
        val deep = com.example.feature.charmtree.initialCharmViewportOffset(
            visualLevel = 4, viewportHeight = androidx.compose.ui.unit.Dp(500f),
            nodeHeight = androidx.compose.ui.unit.Dp(104f), verticalGap = androidx.compose.ui.unit.Dp(38f),
            contentPadding = androidx.compose.ui.unit.Dp(24f)
        )
        assertEquals(androidx.compose.ui.unit.Dp(0f), top)
        assertTrue(deep > androidx.compose.ui.unit.Dp(0f))
    }

    @Test
    fun `requested start charm is promoted to first visible column`() {
        fun node(id: String) = com.example.feature.charmtree.CharmTreeLayoutNode(
            com.example.feature.charmtree.CharmTreeNode(
                com.example.feature.charmtree.CharmTreeEntry(id = id, name = id),
                depth = 0,
                children = emptyList()
            ),
            id
        )
        val prioritized = com.example.feature.charmtree.prioritizeCharmInVisualLevel(
            listOf(listOf(node("outro-a"), node("inicio"), node("outro-b"))),
            "inicio"
        )
        assertEquals("inicio", prioritized.single().first().key)
        assertEquals(setOf("outro-a", "inicio", "outro-b"), prioritized.single().map { it.key }.toSet())
    }

    @Test
    fun `long press uses absolute lowest requirement even when charm is isolated`() {
        val isolated = charm("isolated", "Arte Sublime da Fachada", 1, 2)
        val root = charm("root", "Técnica da Língua Ágil", 1, 2)
        val child = charm("child", "Engano de Cortar o Coração", 1, 3)

        val prerequisites = mapOf(
            "isolated" to emptySet(),
            "root" to emptySet(),
            "child" to setOf("root")
        )
        val selected = lowestRequirementCharm(
            listOf(isolated, root, child),
            canonicalPrerequisites = { prerequisites[it].orEmpty() }
        )
        assertEquals("isolated", selected?.id)
    }

    @Test
    fun `tree without linked roots falls back to lowest requirement`() {
        val selected = lowestRequirementCharm(
            listOf(
                charm("higher", "Maior", 2, 1),
                charm("lower", "Menor", 1, 2)
            ),
            canonicalPrerequisites = { emptySet() }
        )
        assertEquals("lower", selected?.id)
    }

    @Test
    fun `full catalog tree opens at global top instead of branch depth`() {
        val source = java.io.File(
            "src/main/java/com/example/feature/charmtree/CharmPrerequisiteTreeDialog.kt"
        ).readText()
        assertTrue(source.contains("CharmTreeContent(result!!, includeFullCatalog = includeFullCatalog, acquiredCharmNames = acquiredCharmNames)"))
        assertTrue(source.contains("private fun CharmTreeContent(result: CharmTreeResult, includeFullCatalog: Boolean, acquiredCharmNames: Set<String>)"))
        assertTrue(source.contains("if (includeFullCatalog) 0"))
        assertTrue(source.contains("else findCharmTreeVisualLevel(levels, result.root.charm.id)"))
    }

    @Test
    fun `prepared tree cache is invalidated when the charm catalog changes`() {
        val source = java.io.File("src/main/java/com/example/ui/tabs/CharmsTab.kt").readText()
        assertTrue(source.contains(
            "remember(viewModel, sheet.tipoPersonagem, charmsParaArvore) { CharmTreeOpenCache(maxEntries = 8) }"
        ))
        assertTrue(source.contains("val charmsParaArvorePorHabilidade = remember(sheet.tipoPersonagem, charmsParaArvore)"))
    }

    @Test
    fun `catalog and prerequisite caches are refreshed with a new view model`() {
        val source = java.io.File("src/main/java/com/example/ui/tabs/CharmsTab.kt").readText()
        assertTrue(source.contains("val charmsParaArvore = remember(viewModel, sheet.tipoPersonagem)"))
        assertTrue(source.contains("val canonicalPrerequisites = remember(viewModel, sheet.tipoPersonagem)"))
        assertTrue(source.contains("val encantosSearchable = remember(viewModel, sheet.tipoPersonagem)"))
    }

    @Test
    fun `charm tree viewport shrinks on compact phone height`() {
        assertEquals(
            androidx.compose.ui.unit.Dp(360f),
            com.example.feature.charmtree.charmTreeViewportHeight(699)
        )
        assertEquals(
            androidx.compose.ui.unit.Dp(500f),
            com.example.feature.charmtree.charmTreeViewportHeight(700)
        )
    }

}
