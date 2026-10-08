package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DialogButtonLayoutContractTest {
    private val charmDialogs = File("src/main/java/com/example/ui/tabs/CharmDetailsDialogs.kt").readText()
    private val lifecycleDialogs = File("src/main/java/com/example/ui/SheetLifecycleDialogs.kt").readText()

    @Test fun `charm tree and close actions stay side by side in that order`() {
        assertTrue(charmDialogs.windowed("text = \"Árvore\"".length).count { it == "text = \"Árvore\"" } >= 2)
        assertTrue(charmDialogs.windowed("text = \"Fechar\"".length).count { it == "text = \"Fechar\"" } >= 2)
        assertTrue(charmDialogs.contains("Row("))
        assertTrue(charmDialogs.contains("horizontalArrangement = Arrangement.spacedBy(8.dp)"))
        assertTrue(charmDialogs.contains("modifier = Modifier.weight(1f)"))
    }

    @Test fun `new sheet exalt type follows three button responsive policy`() {
        val start = lifecycleDialogs.indexOf("Regra global de grupos de três")
        val end = lifecycleDialogs.indexOf("Spacer(modifier = Modifier.height(8.dp))", start)
        val block = lifecycleDialogs.substring(start, end)
        assertTrue(block.contains("FlowRow("))
        assertTrue(block.contains("maxItemsInEachRow = if (compact) 2 else 3"))
        assertTrue(block.contains("horizontalArrangement = Arrangement.Center"))
        assertFalse(block.contains("Cada opção ocupa uma linha própria"))
    }
}
