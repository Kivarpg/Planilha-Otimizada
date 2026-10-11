package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WholeAppVisualIdentityContractTest {
    private val sources = listOf(
        "src/main/java/com/example/ui/tabs/PersonalDataTab.kt",
        "src/main/java/com/example/ui/tabs/AspectoTab.kt",
        "src/main/java/com/example/ui/tabs/CasteTabSolar.kt",
        "src/main/java/com/example/ui/tabs/CasteTabLunar.kt",
        "src/main/java/com/example/ui/tabs/AttributesTab.kt",
        "src/main/java/com/example/ui/tabs/AbilitiesTab.kt",
        "src/main/java/com/example/ui/tabs/CombatTab.kt",
        "src/main/java/com/example/ui/tabs/MeritsTab.kt",
        "src/main/java/com/example/ui/tabs/EquipmentTab.kt",
        "src/main/java/com/example/ui/tabs/CharmsTab.kt",
        "src/main/java/com/example/ui/tabs/SummaryComponents.kt",
        "src/main/java/com/example/ui/tabs/NPCsTab.kt",
        "src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt",
        "src/main/java/com/example/iniciativas/IniciativasTab.kt",
        "src/main/java/com/example/ui/tabs/BattleGroupsTab.kt",
        "src/main/java/com/example/ui/tabs/MapTab.kt",
        "src/main/java/com/example/oldrealm/TranslatorTab.kt"
    ).map(::File)

    @Test
    fun `all fifteen tab families remain connected to the scoped visual foundation`() {
        sources.forEach { file ->
            val source = file.readText()
            assertTrue(
                source.contains("LocalExaltedVisualTemplate") ||
                    source.contains("exaltedTabIdentity(") ||
                    source.contains("exaltedContentStage("),
                "Missing scoped visual foundation in ${file.name}"
            )
        }
    }

    @Test
    fun `main ui sources never import synthetic visual token packages`() {
        val roots = listOf(
            File("src/main/java/com/example/ui"),
            File("src/main/java/com/example/iniciativas"),
            File("src/main/java/com/example/oldrealm")
        )
        roots.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .forEach { file ->
                assertFalse(
                    file.readText().contains("import com.example.ui.theme.visual."),
                    "Invalid generated token import in ${file.path}"
                )
            }
    }

    @Test
    fun `critical shared controls stay free of decorative canvas overlays`() {
        val ink = File("src/main/java/com/example/ui/components/InkButton.kt").readText()
        val buttons = File("src/main/java/com/example/ui/components/ButtonComponents.kt").readText()
        val lunarCaste = File("src/main/java/com/example/ui/components/LunarCasteEmblemButton.kt").readText()
        val ratings = File("src/main/java/com/example/ui/components/RatingDisplayControls.kt").readText()

        assertFalse(ink.contains("Canvas("))
        assertFalse(buttons.contains("Canvas("))
        assertFalse(lunarCaste.contains("Canvas("))
        assertFalse(ratings.contains("Canvas("))
    }

    @Test
    fun `shared dialogs searches and charm panels consume scoped tokens`() {
        val targets = listOf(
            "src/main/java/com/example/ui/components/SearchComponents.kt",
            "src/main/java/com/example/ui/components/DialogComponents.kt",
            "src/main/java/com/example/ui/tabs/FeiticosTab.kt",
            "src/main/java/com/example/ui/tabs/WeaponSection.kt",
            "src/main/java/com/example/ui/tabs/ArmorSection.kt",
            "src/main/java/com/example/ui/tabs/CharmListComponents.kt",
            "src/main/java/com/example/ui/tabs/CharmDetailsDialogs.kt"
        ).map(::File)

        targets.forEach { file ->
            assertTrue(file.readText().contains("LocalExaltedVisualTemplate"), file.name)
        }
    }

    @Test
    fun `button labels derive contrast from actual painted gradient`() {
        val ink = File("src/main/java/com/example/ui/components/InkButton.kt").readText()
        assertTrue(ink.contains("corTextoContraste(resolvedGradient.mid)"))
        assertTrue(ink.contains("styleOverride.gradientMiddle ?: gradient.mid"))
        assertTrue(ink.contains("styleOverride.labelColor"))
        assertTrue(ink.contains("LocalExaltedVisualTemplate.current"))
        assertFalse(ink.contains("selected -> corTextoContraste(ownerSelected.mid)"))
        assertFalse(ink.contains("variant == InkButtonVariant.OnDark -> InkColors.textOnDark"))
    }

    @Test
    fun `dragon blooded browser owns a dragon blooded visual scope`() {
        val browser = File("src/main/java/com/example/ui/components/SangueDeDragaoBrowser.kt").readText()
        assertTrue(browser.contains("ExaltedVisualMotif.DRAGON_BLOODED"))
        assertTrue(browser.contains("ProvideExaltedVisualTemplate(visual)"))
    }
}
