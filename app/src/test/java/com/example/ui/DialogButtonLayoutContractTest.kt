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

    @Test fun `attribute and ability labels wrap in compact layouts`() {
        val attributes = File("src/main/java/com/example/ui/tabs/AttributesTab.kt").readText()
        val abilityRows = File("src/main/java/com/example/ui/tabs/AbilityRowComponents.kt").readText()
        val abilitySections = File("src/main/java/com/example/ui/tabs/AbilitySections.kt").readText()
        val ratingControls = File("src/main/java/com/example/ui/components/RatingDisplayControls.kt").readText()

        assertTrue(Regex("""maxLines\s*=\s*2,\s*softWrap\s*=\s*true""").containsMatchIn(attributes))
        assertTrue(Regex("""maxLines\s*=\s*2,\s*softWrap\s*=\s*true""").findAll(abilityRows).count() >= 2)
        assertTrue(abilitySections.contains("softWrap=true, maxLines=2"))
        assertTrue(ratingControls.contains("Arrangement.spacedBy(if (stepperSize <= 34.dp) 4.dp else 10.dp)"))
    }

    @Test fun `new sheet exalt selection avoids intrinsic measurement`() {
        val start = lifecycleDialogs.indexOf("fun EscolhaTemplateNovaPlanilhaDialog(")
        val block = lifecycleDialogs.substring(start)
        assertTrue(block.contains("FlowRow("))
        assertTrue(block.contains("maxItemsInEachRow = 2"))
        assertTrue(block.contains("modifier = Modifier.widthIn(min = 96.dp, max = 120.dp)"))
        assertTrue(block.contains("Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)"))
        assertFalse(block.contains("BoxWithConstraints("))
        assertTrue(block.contains("\"Solar\" to \"Solar\""))
        assertTrue(block.contains("\"Sangue de Dragão\" to \"SangueDeDragao\""))
        assertTrue(block.contains("\"Lunar\" to \"Lunar\""))
    }
}
