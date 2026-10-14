package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Prevents approved 947 backgrounds from silently disappearing again.
 * Structural verification only; physical-device visual inspection is still needed.
 */
class ExaltedScenesIntegrationContractTest {
    private val renderer = File("src/main/java/com/example/ui/components/ExaltedSceneBackdrop.kt").readText()
    private val personal = File("src/main/java/com/example/ui/tabs/PersonalDataTab.kt").readText()
    private val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()

    @Test fun `all three approved scenes are packaged as PNGs`() {
        val scenes = listOf("solar", "dragon", "lunar")
        scenes.forEach { type ->
            val file = File("src/main/res/drawable-nodpi/leo_scene_$type.png")
            assertTrue(file.isFile, "Missing approved LEO art for $type")
            assertTrue(file.length() > 100_000L, "Empty or undersized scene for $type")
            assertTrue(renderer.contains("R.drawable.leo_scene_$type"))
        }
    }

    @Test fun `scene mapping follows exalt type and preserves the complete painting`() {
        assertTrue(renderer.contains("exaltedType.isDragonBlooded()"))
        assertTrue(renderer.contains("exaltedType.isLunar()"))
        assertTrue(renderer.contains("ContentScale.Fit"))
        assertTrue(renderer.contains("contentDescription = null"))
    }

    @Test fun `art is behind personal fields without changing field callbacks`() {
        assertTrue(personal.contains("ExaltedSceneBackdrop(sheet.tipoPersonagem, Modifier.matchParentSize())"))
        assertTrue(personal.indexOf("ExaltedSceneBackdrop(") < personal.indexOf("PersonalIdentitySection(sheet,viewModel)"))
        assertTrue(personal.contains("onValueChange = { viewModel.updateDescricaoAnima(it.take(500)) }"))
        assertFalse(personal.contains(".exaltedContentStage(1)"))
    }

    @Test fun `header renders background without filling phone height`() {
        assertTrue(header.contains("ExaltedSceneBackdrop("))
        assertTrue(header.contains("modifier = Modifier.matchParentSize()"))
        assertTrue(header.contains(".height(104.dp)"))
        assertFalse(header.contains(".fillMaxHeight()"))
        assertTrue(header.contains("SheetTabsBar(sheet, viewModel, tabs, selectedTabIndex, onSelectedTab)"))
    }
}
