package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Contract for one shared LEO scene, behind every tab and the header. */
class ExaltedScenesIntegrationContractTest {
    private val renderer = File("src/main/java/com/example/ui/components/ExaltedSceneBackdrop.kt").readText()
    private val screen = File("src/main/java/com/example/ui/SheetScreen.kt").readText()
    private val personal = File("src/main/java/com/example/ui/tabs/PersonalDataTab.kt").readText()
    private val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
    private val identity = File("src/main/java/com/example/ui/components/TabIdentitySurface.kt").readText()
    private val content = File("src/main/java/com/example/ui/SheetContentArea.kt").readText()

    @Test fun `all three approved scenes are packaged as PNGs`() {
        listOf("solar", "dragon", "lunar").forEach { type ->
            val file = File("src/main/res/drawable-nodpi/leo_scene_$type.png")
            assertTrue(file.isFile, "Missing scene for $type")
            assertTrue(file.length() > 100_000L, "Empty artwork for $type")
            assertTrue(renderer.contains("R.drawable.leo_scene_$type"))
        }
    }

    @Test fun `selected exalt type chooses one full screen scene`() {
        assertTrue(renderer.contains("exaltedType.isDragonBlooded()"))
        assertTrue(renderer.contains("exaltedType.isLunar()"))
        assertTrue(renderer.contains("ContentScale.Crop"))
        assertTrue(renderer.contains("contentDescription = null"))
        assertTrue(screen.contains("ExaltedSceneBackdrop(sheet.tipoPersonagem, Modifier.matchParentSize())"))
        assertTrue(screen.indexOf("ExaltedSceneBackdrop(") < screen.indexOf("Scaffold("))
    }

    @Test fun `header and all fifteen tabs share the same painting`() {
        assertFalse(header.contains("ExaltedSceneBackdrop("))
        assertFalse(personal.contains("ExaltedSceneBackdrop("))
        assertFalse(content.contains("sheetBackgroundBrush()"))
        assertTrue(identity.contains("visual.surface.copy(alpha = 0.10f)"))
        assertTrue(identity.contains("visual.surfaceVariant.copy(alpha = 0.30f)"))
        assertTrue(header.contains("SheetTabsBar(sheet, viewModel, tabs, selectedTabIndex, onSelectedTab)"))
        assertTrue(header.contains(".height(104.dp)"))
        assertFalse(header.contains(".fillMaxHeight()"))
    }

    @Test fun `personal data retains shaded edit controls and callbacks`() {
        assertTrue(personal.contains("exaltedTabIdentity(1).exaltedContentStage(1)"))
        assertTrue(personal.contains("GildedCard("))
        assertTrue(personal.contains("colors = exaltedTextFieldColors()"))
        assertTrue(personal.contains("viewModel.updateNome(it)"))
        assertTrue(personal.contains("viewModel.updateJogador(it)"))
        assertTrue(personal.contains("viewModel.updateConceito(it)"))
        assertTrue(personal.contains("onValueChange = { viewModel.updateDescricaoAnima(it.take(500)) }"))
    }
}
