package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Baseline estrutural para a futura identidade visual.
 * Nao valida screenshots, medidas reais ou acessibilidade no dispositivo.
 */
class VisualIdentityPreparationContractTest {
    private val tabs = File("src/main/java/com/example/ui/SheetTabs.kt").readText()
    private val host = File("src/main/java/com/example/ui/SheetContentArea.kt").readText()
    private val identity = File("src/main/java/com/example/ui/components/TabIdentitySurface.kt").readText()
    private val navigation = File("src/main/java/com/example/ui/SheetTabsBar.kt").readText()
    private val dialogs = File("src/main/java/com/example/ui/SheetScreenDialogs.kt").readText()
    private val lifecycleDialogs = File("src/main/java/com/example/ui/SheetLifecycleDialogs.kt").readText()

    @Test
    fun `all fifteen existing tabs remain declared in order`() {
        val names = listOf(
            "1. Dados Pessoais", "2. Aspecto", "3. Atributos",
            "4. Habilidades", "5. Combate", "6. Méritos",
            "7. Equipamentos", "8. Encantos", "9. Planilha",
            "10. Vínculos", "11. Encontros", "12. Conflito",
            "13. Grupos de Batalha", "14. Mapa", "15. Tradutor"
        )
        val positions = names.map { name ->
            val position = tabs.indexOf("\"$name\"")
            assertTrue(position >= 0, "Missing existing tab: $name")
            position
        }
        assertEquals(15, positions.size)
        assertTrue(positions.zipWithNext().all { (left, right) -> left < right })
        assertTrue(tabs.contains("if (sheet.tipoPersonagem.isDragonBlooded()) \"2. Aspecto\" else \"2. Casta\""))
        assertTrue(tabs.contains("com.example.iniciativas.IniciativasTab("))
        assertTrue(tabs.contains("TranslatorTab(modifier = Modifier.fillMaxSize())"))
    }

    @Test
    fun `tab host retains width-aware folio and per-tab saved state`() {
        assertTrue(host.contains("BoxWithConstraints("))
        assertTrue(host.contains("val widthDp = maxWidth.value"))
        assertTrue(host.contains("widthDp >= 1000"))
        assertTrue(host.contains("widthDp >= 720"))
        assertTrue(host.contains("else -> Modifier.fillMaxWidth()"))
        assertTrue(host.contains("rememberSaveableStateHolder()"))
        assertTrue(host.contains("holder.SaveableStateProvider(selectedTabIndex)"))
    }
    @Test
    fun `identity surface remains neutral until visual pilot is approved`() {
        assertTrue(identity.contains("fun Modifier.exaltedTabIdentity(chapter: Int = 0): Modifier = this"))
        assertTrue(identity.contains("fun Modifier.exaltedContentStage(chapter: Int): Modifier = this"))
        assertTrue(identity.contains("fun Modifier.exaltedExistingPanel(): Modifier = exaltedSectionPanel()"))
    }

    @Test
    fun `saved sheets list displays correct type specific caste or aspect`() {
        assertTrue(lifecycleDialogs.contains("s.tipoPersonagem.isDragonBlooded() -> \"Aspecto:"))
        assertTrue(lifecycleDialogs.contains("s.tipoPersonagem.isLunar() -> \"Casta:"))
        assertTrue(lifecycleDialogs.contains("s.lunarCasta.displayName"))
        assertTrue(lifecycleDialogs.contains("s.casta.displayName"))
        assertTrue(lifecycleDialogs.contains("viewModel.loadSheet(s)"))
        assertTrue(lifecycleDialogs.contains("onDeleteRequest(s)"))
    }

    @Test
    fun `settings and warning dialogs retain bounded scrollable content`() {
        assertTrue(dialogs.contains("val settingsContentHeight = (LocalConfiguration.current.screenHeightDp - 240).coerceIn(160, 500).dp"))
        assertTrue(dialogs.contains("heightIn(max = settingsContentHeight).verticalScroll(rememberScrollState())"))
        assertTrue(dialogs.contains("val messageHeightLimit = (LocalConfiguration.current.screenHeightDp - 260).coerceIn(120, 420).dp"))
        assertTrue(dialogs.contains("heightIn(max = messageHeightLimit).verticalScroll(rememberScrollState())"))
        assertTrue(dialogs.contains("val affectedMeritsHeightLimit = (LocalConfiguration.current.screenHeightDp - 260).coerceIn(120, 420).dp"))
        assertTrue(dialogs.contains("heightIn(max = affectedMeritsHeightLimit).verticalScroll(rememberScrollState())"))
    }

    @Test
    fun `navigation preserves compact sizing scrolling and selected tab focus`() {
        assertTrue(navigation.contains("val compact = maxWidth < 600.dp"))
        assertTrue(navigation.contains("val enlargedText = fontScale > 1.20f"))
        assertTrue(navigation.contains("LazyRow("))
        assertTrue(navigation.contains("listState.scrollToItem(selectedTabIndex)"))
        assertTrue(navigation.contains("onSelectedTab(index)"))
        assertTrue(navigation.contains("maxLines = if (compact) 2 else 1"))
    }
}
