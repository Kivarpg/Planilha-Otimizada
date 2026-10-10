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
    private val mainSheet = File("src/main/java/com/example/ui/SheetScreen.kt").readText()
    private val codeDialogs = File("src/main/java/com/example/ui/CodeShareDialogs.kt").readText()
    private val charmDialogs = File("src/main/java/com/example/ui/tabs/CharmDetailsDialogs.kt").readText()
    private val commonDialogs = File("src/main/java/com/example/ui/components/DialogComponents.kt").readText()

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
        assertTrue(lifecycleDialogs.contains("text = s.nomeArquivo(),"))
        assertTrue(lifecycleDialogs.contains("maxLines = 2,\n                                    overflow = TextOverflow.Ellipsis"))
        assertTrue(lifecycleDialogs.contains("color = ExaltedMuted,\n                                    maxLines = 2,\n                                    overflow = TextOverflow.Ellipsis"))
        assertTrue(lifecycleDialogs.contains("viewModel.loadSheet(s)"))
        assertTrue(lifecycleDialogs.contains("onDeleteRequest(s)"))
    }

    @Test
    fun `saved sheet search stays editable when a filter reduces the list`() {
        assertTrue(lifecycleDialogs.contains("if (savedSheets.size > 4 || buscaPlanilhasSalvas.isNotBlank()) {"))
        assertTrue(lifecycleDialogs.contains("onValueChange = onBuscaChange"))
        assertTrue(lifecycleDialogs.contains("val termoBusca = buscaPlanilhasSalvas.trim()"))
    }

    @Test
    fun `backup restore rows reserve width for the action`() {
        assertTrue(lifecycleDialogs.contains("text = snapshot.timestamp,"))
        assertTrue(lifecycleDialogs.contains("modifier = Modifier.weight(1f),"))
        assertTrue(lifecycleDialogs.contains("overflow = TextOverflow.Ellipsis"))
        assertTrue(lifecycleDialogs.contains("text = \"Restaurar\""))
        assertTrue(lifecycleDialogs.contains("viewModel.restaurarBackupPeriodico(snapshot)"))
    }

    @Test
    fun `sharing dialogs preserve QR and paste control on compact screens`() {
        assertTrue(codeDialogs.contains("Modifier.fillMaxWidth().widthIn(max = 240.dp).aspectRatio(1f)"))
        assertTrue(codeDialogs.contains("modifier = Modifier.weight(1f),"))
        assertTrue(codeDialogs.contains("text = \"Colar\""))
        assertTrue(codeDialogs.contains("text = \"Copiar\""))
        assertTrue(codeDialogs.contains("heightIn(max = importContentHeight).verticalScroll(rememberScrollState())"))
    }

    @Test
    fun `charm details hide while prerequisite tree dialog is open`() {
        val start = charmDialogs.indexOf("fun CharmDetailsDialog(")
        assertTrue(start >= 0)
        val charmSection = charmDialogs.substring(start)
        assertTrue(charmSection.contains("if (mostrarArvore && viewModel != null) {"))
        assertTrue(charmSection.contains("    } else {\n    AlertDialog("))
        assertTrue(charmSection.contains("onDismiss = { mostrarArvore = false }"))
    }

    @Test
    fun `charm tree modal state is scoped to the selected charm`() {
        assertTrue(charmDialogs.contains("var mostrarArvore by remember(def.id) { mutableStateOf(false) }"))
        assertTrue(charmDialogs.contains("var mostrarArvore by remember(charm.id) { mutableStateOf(false) }"))
    }

    @Test
    fun `charm tree cache tracks the active view model`() {
        assertTrue(charmDialogs.contains("remember(viewModel, def.id, def.preRequisitos, charmsParaArvore)"))
        assertTrue(charmDialogs.contains("remember(viewModel, charm.id, charm.preRequisitos, tipoPersonagem, dragonBlooded)"))
    }

    @Test
    fun `delete confirmation keeps long names scrollable`() {
        assertTrue(commonDialogs.contains("fun ConfirmDeleteDialog("))
        assertTrue(commonDialogs.contains("val confirmationHeightLimit = (LocalConfiguration.current.screenHeightDp * 0.35f).coerceAtMost(280f).dp"))
        assertTrue(commonDialogs.contains("heightIn(max = confirmationHeightLimit).verticalScroll(rememberScrollState())"))
        assertTrue(commonDialogs.contains("text = \"Remover\""))
        assertTrue(commonDialogs.contains("text = \"Cancelar\""))
    }

    @Test
    fun `error log dialog keeps large reports scrollable within viewport`() {
        assertTrue(mainSheet.contains("if (showErrorLogDialog)"))
        assertTrue(mainSheet.contains("screenHeightDp * 0.45f"))
        assertTrue(mainSheet.contains(".heightIn(max ="))
        assertTrue(mainSheet.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(mainSheet.contains("text = \"Copiar\""))
        assertTrue(mainSheet.contains("text = \"Fechar\""))
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
        assertTrue(navigation.contains("softWrap = compact || enlargedText"))
        assertTrue(navigation.contains("height(if (enlargedText) 34.dp else 25.dp)"))
        assertTrue(navigation.contains("maxLines = if (compact || enlargedText) 2 else 1"))
    }
}
