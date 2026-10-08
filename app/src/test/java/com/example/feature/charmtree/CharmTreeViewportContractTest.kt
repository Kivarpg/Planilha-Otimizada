package com.example.feature.charmtree

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CharmTreeViewportContractTest {
    private val source = File("src/main/java/com/example/feature/charmtree/CharmPrerequisiteTreeDialog.kt").readText()
    private val abilityDialog = File("src/main/java/com/example/ui/tabs/AbilityCharmTreeDialog.kt").readText()
    private val details = File("src/main/java/com/example/ui/tabs/CharmDetailsDialogs.kt").readText()

    @Test fun `full catalog long press starts at top and horizontally centered`() {
        assertTrue(source.contains("if (includeFullCatalog) 0"))
        assertTrue(source.contains("horizontalState.scrollTo(horizontalState.maxValue / 2)"))
        assertTrue(!source.contains("horizontalState.scrollTo(0)"))
    }
    @Test fun `tree popup title cannot be covered by close action`() {
        assertTrue(source.contains("Spacer(Modifier.width(48.dp))"))
        assertTrue(source.contains("modifier = Modifier.weight(1f)"))
        assertTrue(source.contains("Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd)"))
    }

    @Test fun `tree titles use only ability or attribute name`() {
        assertTrue(abilityDialog.contains("title = ability"))
        assertTrue(!abilityDialog.contains("Árvore —"))
        assertTrue(!details.contains("Árvore —"))
    }
}
