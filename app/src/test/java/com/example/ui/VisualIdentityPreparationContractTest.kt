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
    fun `neutral panels use bounded dark-metal styling without decorative symbols`() {
        assertTrue(identity.contains("Brush.verticalGradient("))
        assertTrue(identity.contains("backgroundColor.copy(alpha = 0.96f)"))
        assertTrue(identity.contains("ExaltedOutline.copy(alpha = 0.32f)"))
        assertTrue(identity.contains("RoundedCornerShape(8.dp)"))
        assertTrue(!identity.contains("drawArc("))
        assertTrue(!identity.contains("drawPath("))
    }

    @Test
    fun `template selection preserves exclusive emblems and responsive cards`() {
        val selection = File("src/main/java/com/example/ui/TemplateSelectionScreen.kt").readText()
        assertTrue(selection.contains("isDragon -> R.drawable.tab_icon_dragao"))
        assertTrue(selection.contains("template.nome == \"Lunar\" -> R.drawable.tab_icon_lua"))
        assertTrue(selection.contains("else -> R.drawable.tab_icon_sol"))
        assertTrue(selection.contains("val narrowCard = maxWidth < 360.dp"))
        assertTrue(selection.contains("listOf(ExaltedDarkBackground, ExaltedDarkSurface, ExaltedDarkBackground)"))
        assertTrue(!selection.contains("ExaltedBackdropGlow.copy(alpha = 0.18f)"))
        assertTrue(!selection.contains("Offset(size.width * 0.05f, 0f)"))
        assertTrue(!selection.contains("Offset(size.width * 0.95f, 0f)"))
        assertTrue(selection.contains("val emblemSize = if (narrowCard) 74.dp else 104.dp"))
        assertTrue(selection.contains("heightIn(min = 136.dp)"))
        assertTrue(selection.contains("overflow = TextOverflow.Ellipsis"))
    }

    @Test
    fun `sheet header avoids full-height decorative side rails`() {
        val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
        assertTrue(!header.contains("Offset(7.dp.toPx(), 0f), Offset(7.dp.toPx(), size.height)"))
        assertTrue(!header.contains("Offset(12.dp.toPx(), 0f), Offset(12.dp.toPx(), size.height)"))
        assertTrue(header.contains("R.drawable.tab_icon_dragao"))
        assertTrue(header.contains("R.drawable.tab_icon_lua"))
        assertTrue(header.contains("R.drawable.tab_icon_sol"))
    }

    @Test
    fun `personal data identity fields retain callbacks in responsive layout`() {
        val tab = File("src/main/java/com/example/ui/tabs/PersonalDataTab.kt").readText()
        assertTrue(tab.contains("if (maxWidth < 390.dp)"))
        assertTrue(tab.contains("BoxNames.PersonalData.NAME, Modifier.fillMaxWidth()"))
        assertTrue(tab.contains("BoxNames.PersonalData.PLAYER, Modifier.fillMaxWidth()"))
        assertTrue(tab.contains("BoxNames.PersonalData.CONCEPT, Modifier.fillMaxWidth()"))
        assertTrue(tab.contains("BoxNames.PersonalData.NAME, Modifier.weight(1f)"))
        assertTrue(tab.contains("BoxNames.PersonalData.PLAYER, Modifier.weight(1f)"))
        assertTrue(tab.contains("BoxNames.PersonalData.CONCEPT, Modifier.weight(1f)"))
    }

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
    fun `saved sheet picker handles empty state and clear search`() {
        assertTrue(lifecycleDialogs.contains("if (savedSheets.isEmpty()) {"))
        assertTrue(lifecycleDialogs.contains("text = \"Nenhuma planilha salva.\""))
        assertTrue(lifecycleDialogs.contains("trailingIcon = if (buscaPlanilhasSalvas.isNotEmpty())"))
        assertTrue(lifecycleDialogs.contains("onClick = { onBuscaChange(\"\") }"))
        assertTrue(lifecycleDialogs.contains("contentDescription = \"Limpar busca\""))
        assertTrue(lifecycleDialogs.contains("AppText(text = \"×\", color = MaterialTheme.colorScheme.onSurface)"))
    }

    @Test
    fun `qr bitmap uses one bulk pixel transfer`() {
        assertTrue(codeDialogs.contains("codigo.isNotEmpty() && codigo.length <= LIMITE_CARACTERES_PARA_QR"))
        assertTrue(codeDialogs.contains("val pixels = IntArray(tamanhoPx * tamanhoPx)"))
        assertTrue(codeDialogs.contains("pixels[rowOffset + x] = if (matrix.get(x, y))"))
        assertTrue(codeDialogs.contains("bitmap.setPixels(pixels, 0, tamanhoPx, 0, 0, tamanhoPx, tamanhoPx)"))
        assertTrue(!codeDialogs.contains("bitmap.setPixel(x, y,"))
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
    fun `settings rows reserve remaining width for labels`() {
        assertTrue(dialogs.contains("AppText(label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))"))
        assertTrue(dialogs.contains("AppText(\"Português\", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))"))
        assertTrue(dialogs.contains("AppText(\"English\", color = ExaltedMuted, modifier = Modifier.weight(1f))"))
        assertTrue(dialogs.contains("AppText(\"Vibração ao tocar\", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(start = 8.dp))"))
        assertTrue(dialogs.contains("AppText(\"Som ao tocar\", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(start = 8.dp))"))
        assertTrue(dialogs.contains("AppText(\"Modo Livre\", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))"))
        assertTrue(dialogs.contains("AppText(\"Ver Log de Erros\", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))"))
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
