package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DialogButtonLayoutContractTest {
    private val charmDialogs = File("src/main/java/com/example/ui/tabs/CharmDetailsDialogs.kt").readText()
    private val lifecycleDialogs = File("src/main/java/com/example/ui/SheetLifecycleDialogs.kt").readText()

    private val inkButton = File("src/main/java/com/example/ui/components/InkButton.kt").readText()
    private val dialogComponents = File("src/main/java/com/example/ui/components/DialogComponents.kt").readText()

    @Test fun `ink brush uses the measured button bounds`() {
        assertTrue(inkButton.contains("Modifier.widthIn(min = 48.dp, max = width).height(height)"))
        assertTrue(inkButton.contains("modifier = Modifier.fillMaxSize()"))
        assertTrue(inkButton.contains("clipRect {"))
        assertFalse(inkButton.contains("Modifier.size(width, height)"))
    }

    @Test fun `validation report keeps scrolling and avoids fixed height subtraction`() {
        assertTrue(dialogComponents.contains("screenHeightDp * 0.45f"))
        assertTrue(dialogComponents.contains("verticalScroll(rememberScrollState())"))
        assertFalse(dialogComponents.contains("screenHeightDp - 260"))
    }

    @Test fun `saved sheets and backups use proportional scroll limits`() {
        assertTrue(lifecycleDialogs.contains("val savedListHeightLimit = (LocalConfiguration.current.screenHeightDp * 0.45f).coerceAtMost(480f).dp"))
        assertTrue(lifecycleDialogs.contains("val backupListHeightLimit = (LocalConfiguration.current.screenHeightDp * 0.45f).coerceAtMost(480f).dp"))
        assertFalse(lifecycleDialogs.contains("screenHeightDp - 240"))
        assertTrue(lifecycleDialogs.contains(".heightIn(max = savedListHeightLimit)"))
        assertTrue(lifecycleDialogs.contains(".heightIn(max = backupListHeightLimit)"))
    }

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
        assertTrue(block.contains("maxItemsInEachRow = columns"))
        assertTrue(block.contains("val buttonWidth = (maxWidth - 8.dp * (columns - 1)) / columns"))
        assertTrue(block.contains("modifier = Modifier.width(buttonWidth)"))
        assertTrue(block.contains("horizontalArrangement = Arrangement.Center"))
        assertFalse(block.contains("Cada opção ocupa uma linha própria"))
    }
}
