package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `neutral panels use scoped bounded styling without decorative drawing`() {
        assertTrue(identity.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(identity.contains("Brush.verticalGradient("))
        assertTrue(identity.contains("background.copy(alpha = 0.98f)"))
        assertTrue(identity.contains("brush = Brush.linearGradient("))
        assertTrue(identity.contains("visual.metalDeep.copy(alpha = 0.92f)"))
        assertTrue(identity.contains("visual.metalShine.copy(alpha = 0.82f)"))
        assertTrue(identity.contains("visual.accentBright.copy(alpha = 0.88f)"))
        assertTrue(identity.contains("RoundedCornerShape(10.dp)"))
        assertFalse(identity.contains("drawBehind"))
        assertFalse(identity.contains("drawArc("))
        assertFalse(identity.contains("drawPath("))
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
    fun `sheet header uses safe geometry and keeps only the real exalt emblem`() {
        val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
        assertFalse(header.contains("drawBehind"))
        assertFalse(header.contains("drawArc("))
        assertFalse(header.contains("drawPath("))
        assertTrue(header.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(header.contains("R.drawable.tab_icon_dragao"))
        assertTrue(header.contains("R.drawable.tab_icon_lua"))
        assertTrue(header.contains("R.drawable.tab_icon_sol"))
    }

    @Test
    fun `shared search and text controls do not force Solar accent`() {
        val search = File("src/main/java/com/example/ui/components/SearchComponents.kt").readText()
        val textControls = File("src/main/java/com/example/ui/components/TextComponents.kt").readText()
        val combatTrack = File("src/main/java/com/example/ui/components/CombatTrackControls.kt").readText()
        assertTrue(search.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(search.contains("visual.accentBright"))
        assertTrue(search.contains("exaltedTextFieldColors()"))
        assertTrue(textControls.contains("LocalExaltedVisualTemplate.current"))
        assertFalse(search.contains("ExaltedGold"))
        assertFalse(search.contains("ExaltedAmber"))
        assertTrue(combatTrack.contains("visualTemplate ?: LocalExaltedVisualTemplate.current"))
        assertTrue(combatTrack.contains("resolvedVisual.accent"))
        assertTrue(combatTrack.contains("resolvedVisual.metalShine"))
    }

    @Test
    fun `encounter NPC tab removal has a readable accessible touch target`() {
        val encounter = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()
        assertTrue(encounter.contains(".size(40.dp)"))
        assertTrue(encounter.contains("contentDescription = \"Remover NPC"))
        assertTrue(encounter.contains("viewModel.removeNpcEncontro(npc.id)"))
        assertTrue(encounter.contains("onAbaSelecionadaChange(null)"))
    }

    @Test
    fun `navigation progress strip uses scoped palette and accessible header height`() {
        val tabs = File("src/main/java/com/example/ui/SheetTabsBar.kt").readText()
        val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
        assertTrue(tabs.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(tabs.contains("visual.accentBright"))
        assertFalse(tabs.contains("drawBehind"))
        assertFalse(tabs.contains("drawPath("))
        assertFalse(tabs.contains("Modifier.width(7.dp).fillMaxHeight()"))
        assertTrue(tabs.contains("maxLines = 2, softWrap = true"))
        assertTrue(header.contains("val headerHeight = if (enlargedHeaderText) 156.dp else 132.dp"))
    }

    @Test
    fun `header action icons follow scoped Exalt palette`() {
        val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
        assertTrue(header.contains("Icon(Icons.Outlined.Settings, \"Configurações\", tint = visual.accentBright"))
        assertTrue(header.contains("Icon(Icons.Outlined.MoreVert, \"Opções\", tint = visual.accentBright"))
        assertTrue(header.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(header.contains("val actionsWidth = if (compactHeader) 96.dp else 104.dp"))
    }

    @Test
    fun `sheet settings and lifecycle dialogs use the selected Exalt palette`() {
        val settings = File("src/main/java/com/example/ui/SheetScreenDialogs.kt").readText()
        val lifecycle = File("src/main/java/com/example/ui/SheetLifecycleDialogs.kt").readText()
        for (source in listOf(settings, lifecycle)) {
            assertTrue(source.contains("LocalExaltedVisualTemplate.current"))
            assertTrue(source.contains("visual.accentBright"))
            assertFalse(source.contains("ExaltedAmber"))
            assertFalse(source.contains("ExaltedGold"))
            assertTrue(source.contains("TextAlign.Center"))
        }
        assertTrue(settings.contains("InteractionFeedback.setHapticEnabled"))
        assertTrue(lifecycle.contains("viewModel.loadSheet(s)"))
    }

    @Test
    fun `shared dialogs and counters stay responsive and type aware`() {
        val dialogs = File("src/main/java/com/example/ui/components/DialogComponents.kt").readText()
        val counters = File("src/main/java/com/example/ui/components/CounterControls.kt").readText()
        assertFalse(dialogs.contains("ExaltedAmber"))
        assertTrue(dialogs.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(dialogs.contains("visual.accentBright"))
        assertTrue(dialogs.contains("verticalScroll(rememberScrollState())"))
        assertTrue(counters.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(counters.contains("modifier = Modifier.fillMaxWidth(),"))
        assertTrue(counters.contains("Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)"))
    }

    @Test
    fun `custom dialog containers always define readable content colors`() {
        val searchDialogs = File("src/main/java/com/example/ui/components/SearchComponents.kt").readText()
        val combatDialogs = File("src/main/java/com/example/ui/tabs/CombatTab.kt").readText()
        val sources = listOf(commonDialogs, searchDialogs, dialogs, lifecycleDialogs, combatDialogs)
        for (source in sources) {
            if (source.contains("containerColor = visual.surface") || source.contains("containerColor = visual.surfaceVariant")) {
                assertTrue(source.contains("titleContentColor = visual.onSurface"))
                assertTrue(source.contains("textContentColor = visual.onSurface"))
            }
        }
    }

    @Test
    fun `visual diagnostics keep build traceability and twelve preview cases`() {
        val gradle = File("build.gradle.kts").readText()
        val settingsDialogs = File("src/main/java/com/example/ui/SheetScreenDialogs.kt").readText()
        val preview = File("src/debug/java/com/example/ui/debug/VisualDiagnosticsPreview.kt").readText()

        assertTrue(gradle.contains("buildConfigField(\"String\", \"BUILD_COMMIT\""))
        assertTrue(settingsDialogs.contains("BuildConfig.VERSION_NAME"))
        assertTrue(settingsDialogs.contains("BuildConfig.BUILD_COMMIT"))
        assertEquals(12, Regex("""@Preview\(""").findAll(preview).count())
        assertTrue(preview.contains("TipoExaltadoEncontro.SOLAR"))
        assertTrue(preview.contains("TipoExaltadoEncontro.SANGUE_DE_DRAGAO"))
        assertTrue(preview.contains("TipoExaltadoEncontro.LUNAR"))
        assertTrue(preview.contains("widthDp = 320"))
        assertTrue(preview.contains("widthDp = 411"))
        assertTrue(preview.contains("fontScale = 1.5f"))
    }

    @Test
    fun `shared visual primitives consume one scoped identity source`() {
        val context = File("src/main/java/com/example/ui/theme/VisualContext.kt").readText()
        val theme = File("src/main/java/com/example/ui/theme/Theme.kt").readText()
        val colors = File("src/main/java/com/example/ui/theme/Color.kt").readText()
        val ink = File("src/main/java/com/example/ui/components/InkButton.kt").readText()
        val cards = File("src/main/java/com/example/ui/components/CardComponents.kt").readText()
        val text = File("src/main/java/com/example/ui/components/TextComponents.kt").readText()

        assertTrue(context.contains("LocalExaltedVisualTemplate"))
        assertTrue(context.contains("CompositionLocalProvider"))
        assertTrue(theme.contains("LocalExaltedVisualTemplate provides activeVisualTemplate"))
        assertTrue(colors.contains("fun visualTemplatePorMotif"))
        assertTrue(ink.contains("visualTemplate ?: LocalExaltedVisualTemplate.current"))
        assertTrue(cards.contains("visualTemplate ?: LocalExaltedVisualTemplate.current"))
        assertTrue(cards.contains("ProvideExaltedVisualTemplate(visual)"))
        assertTrue(text.contains("LocalExaltedVisualTemplate.current"))
        assertFalse(text.contains("ExaltedActiveMotif"))
    }

    @Test
    fun `ink button rendering path remains free of canvas svg and clipping`() {
        val ink = File("src/main/java/com/example/ui/components/InkButton.kt").readText()
        assertFalse(ink.contains("Canvas("))
        assertFalse(ink.contains("PathParser"))
        assertFalse(ink.contains("clipRect"))
        assertFalse(ink.contains("drawPath("))
        assertTrue(ink.contains("contentAlignment = Alignment.Center"))
        assertTrue(ink.contains("corTextoContraste(resolvedGradient.mid)"))
    }

    @Test
    fun `shared selection buttons avoid manual canvas ornaments`() {
        val buttons = File("src/main/java/com/example/ui/components/ButtonComponents.kt").readText()
        assertTrue(buttons.contains("LocalExaltedVisualTemplate.current"))
        assertFalse(buttons.contains("drawBehind"))
        assertFalse(buttons.contains("Canvas("))
        assertFalse(buttons.contains("ExaltedActiveMotif"))
        assertTrue(buttons.contains("corTextoContraste(corDeFundo)"))
    }

    @Test
    fun `numeric selectors consume scoped visual tokens`() {
        val source = File("src/main/java/com/example/ui/components/NumericRatingControls.kt").readText()
        assertTrue(source.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(source.contains("corTextoContraste(corPreenchimento)"))
        assertFalse(source.contains("ExaltedAmber"))
        assertFalse(source.contains("ExaltedGold"))
        assertTrue(source.contains("val decrementButton: @Composable (Dp) -> Unit"))
        assertTrue(source.contains("val incrementButton: @Composable (Dp) -> Unit"))
    }

    @Test
    fun `header status and caste remain in separate text rows`() {
        val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
        assertTrue(header.contains("O estado da planilha nao deve disputar uma unica linha"))
        assertTrue(header.contains("Column(verticalArrangement = Arrangement.spacedBy(4.dp))"))
        assertTrue(header.contains("color = cor, maxLines = 2, softWrap = true"))
    }

    @Test
    fun `shared counter follows scoped exalt identity`() {
        val counter = File("src/main/java/com/example/ui/components/CounterControls.kt").readText()
        assertTrue(counter.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(counter.contains("visual.accentBright"))
        assertTrue(counter.contains("corTextoContraste(corPreenchimento)"))
        assertFalse(counter.contains("drawBehind"))
    }

    @Test
    fun `header reserves space for two accessible action targets`() {
        val header = File("src/main/java/com/example/ui/SheetTopBar.kt").readText()
        assertTrue(header.contains("val actionsWidth = if (compactHeader) 96.dp else 104.dp"))
        assertTrue(header.contains("modifier = Modifier.size(48.dp)"))
    }

    @Test
    fun `ability balance strip stacks labels on compact screens`() {
        assertTrue(navigation.contains("selectedTabIndex == 3 && (compact || enlargedText)"))
        assertTrue(navigation.contains("verticalArrangement = Arrangement.spacedBy(3.dp)"))
        assertTrue(navigation.contains("maxLines = 2, softWrap = true"))
        assertTrue(navigation.contains("else if (showsProgressStrip)"))
    }

    @Test
    fun `troop and npc labels use their scoped exalt palette`() {
        val troops = File("src/main/java/com/example/ui/tabs/BattleGroupsTab.kt").readText()
        val npcs = File("src/main/java/com/example/ui/tabs/NPCsTab.kt").readText()
        assertTrue(troops.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(troops.contains("val coresCampoTexto = exaltedTextFieldColors()"))
        assertTrue(troops.contains("AppText(\"Tipo de tropa\", style = MaterialTheme.typography.labelMedium, color = visual.accentBright)"))
        assertTrue(npcs.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(npcs.contains("AppText(\"Lealdade\", style = MaterialTheme.typography.labelMedium, color = visual.accentBright)"))
        assertFalse(troops.contains("ExaltedGold"))
        assertFalse(npcs.contains("ExaltedAmber"))
    }

    @Test
    fun `encounter header inputs follow scoped exalt palette in both layouts`() {
        val encounter = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()
        assertTrue(encounter.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(encounter.split("colors = exaltedTextFieldColors()").size >= 3)
        assertTrue(encounter.split("checkedColor = visual.accentBright,").size >= 3)
        assertFalse(encounter.contains("cursorColor = ExaltedGold,"))
        assertFalse(encounter.contains("checkedColor = ExaltedAmber,"))
    }

    @Test
    fun `vitality settings and attribute totals retain scoped type accents`() {
        val combat = File("src/main/java/com/example/ui/tabs/CombatTab.kt").readText()
        val attributes = File("src/main/java/com/example/ui/tabs/AttributesTab.kt").readText()
        assertTrue(combat.contains("cursorColor = visual.accentBright"))
        assertTrue(combat.contains("if (ehExtra) visual.accentBright"))
        assertTrue(combat.contains("AppText(\"Caixas extras atuais\", style = MaterialTheme.typography.labelMedium, color = visual.accentBright)"))
        assertTrue(combat.contains("AppText(\"Adicionar novo nível\", style = MaterialTheme.typography.labelMedium, color = visual.accentBright)"))
        assertTrue(attributes.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(attributes.contains("color = visual.accentBright"))
    }

    @Test
    fun `combat attributes and anima controls follow scoped exalt palette`() {
        val combat = File("src/main/java/com/example/ui/tabs/CombatTab.kt").readText()
        val attributes = File("src/main/java/com/example/ui/tabs/AttributesTab.kt").readText()
        val personal = File("src/main/java/com/example/ui/tabs/PersonalDataTab.kt").readText()
        assertTrue(combat.contains("color = visual.accentBright.copy(alpha = 0.12f)"))
        assertTrue(combat.contains("if (ativo) visual.accentBright else visual.outline"))
        assertTrue(combat.contains("corTextoContraste(essenciaGradientColor(nivel))"))
        assertTrue(attributes.contains("visual.accentBright.copy(alpha = 0.46f) else visual.surfaceVariant"))
        assertTrue(attributes.contains("if (favoritoInfo.ehCasta) visual.accentBright else visual.outline"))
        assertTrue(personal.contains("colors = exaltedTextFieldColors()"))
    }

    @Test
    fun `type palette covers remaining sorcery merits and equipment controls`() {
        val sorcery = File("src/main/java/com/example/ui/tabs/FeiticosTab.kt").readText()
        val merits = File("src/main/java/com/example/ui/tabs/MeritsTab.kt").readText()
        val equipment = File("src/main/java/com/example/ui/tabs/EquipmentTab.kt").readText()
        assertTrue(sorcery.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(sorcery.contains("tint = visual.accentBright, modifier = Modifier.size(18.dp)"))
        assertTrue(merits.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(merits.contains("color = visual.accentBright"))
        assertTrue(equipment.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(equipment.contains("selectedColor = visual.accentBright"))
        assertTrue(equipment.contains("colors = exaltedTextFieldColors()"))
        assertTrue(equipment.contains("if (isSelected) visual.accentBright else visual.outline"))
    }

    @Test
    fun `sorcery circle controls and commitment dialog use active palette`() {
        val sorcery = File("src/main/java/com/example/ui/tabs/FeiticosTab.kt").readText()
        val equipment = File("src/main/java/com/example/ui/tabs/EquipmentTab.kt").readText()
        assertTrue(sorcery.contains("color = if (desbloqueado) visual.accentBright else visual.muted"))
        assertTrue(sorcery.contains("checkedColor = visual.accentBright"))
        assertTrue(sorcery.contains("border = BorderStroke(1.dp, if (desbloqueado) visual.accentBright"))
        assertTrue(equipment.contains("color = visual.accentBright"))
        assertTrue(equipment.contains("titleContentColor = visual.onSurface"))
        assertTrue(equipment.contains("textContentColor = visual.onSurface"))
    }

    @Test
    fun `merits and equipment totals wrap with scoped active palette`() {
        val merits = File("src/main/java/com/example/ui/tabs/MeritsTab.kt").readText()
        val equipment = File("src/main/java/com/example/ui/tabs/EquipmentTab.kt").readText()
        assertTrue(merits.contains("text = \"Total de Pontos de Mérito:"))
        assertTrue(merits.contains("color = visual.accentBright,\n                    textAlign = TextAlign.Center,\n                    maxLines = 3"))
        assertTrue(equipment.contains("text = \"Motes Comitados:"))
        assertTrue(equipment.contains("color = visual.accentBright,\n                    textAlign = TextAlign.Center,\n                    maxLines = 3"))
    }

    @Test
    fun `tabs six through eleven use the safe scoped visual foundation`() {
        val merits = File("src/main/java/com/example/ui/tabs/MeritsTab.kt").readText()
        val equipment = File("src/main/java/com/example/ui/tabs/EquipmentTab.kt").readText()
        val charms = File("src/main/java/com/example/ui/tabs/CharmsTab.kt").readText()
        val summary = File("src/main/java/com/example/ui/tabs/SummaryComponents.kt").readText()
        val npcs = File("src/main/java/com/example/ui/tabs/NPCsTab.kt").readText()
        val encounter = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()

        listOf(merits, equipment, charms, summary, npcs, encounter).forEach { source ->
            assertTrue(source.contains("LocalExaltedVisualTemplate.current"))
        }
        assertTrue(equipment.contains("colors = exaltedTextFieldColors()"))
        assertTrue(encounter.contains("colors = exaltedTextFieldColors()"))
        assertTrue(encounter.contains("onClick = gerarNpcDaInterface"))
        assertFalse(encounter.contains(".background(ExaltedAccentBright, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))"))
    }

    @Test
    fun `first five tabs are wired to the safe visual foundation`() {
        val personal = File("src/main/java/com/example/ui/tabs/PersonalDataTab.kt").readText()
        val aspect = File("src/main/java/com/example/ui/tabs/AspectoTab.kt").readText()
        val solar = File("src/main/java/com/example/ui/tabs/CasteTabSolar.kt").readText()
        val lunar = File("src/main/java/com/example/ui/tabs/CasteTabLunar.kt").readText()
        val attributes = File("src/main/java/com/example/ui/tabs/AttributesTab.kt").readText()
        val abilities = File("src/main/java/com/example/ui/tabs/AbilitiesTab.kt").readText()
        val combat = File("src/main/java/com/example/ui/tabs/CombatTab.kt").readText()

        assertTrue(personal.contains("colors = exaltedTextFieldColors()"))
        assertTrue(aspect.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(solar.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(lunar.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(attributes.contains("LocalExaltedVisualTemplate.current"))
        assertTrue(abilities.contains("exaltedTabIdentity(4).exaltedContentStage(4)"))
        assertTrue(combat.contains("LocalExaltedVisualTemplate.current"))
    }

    @Test
    fun `attribute priorities retain three equal width controls`() {
        val tab = File("src/main/java/com/example/ui/tabs/AttributesTab.kt").readText()
        assertTrue(tab.contains("listOf(\"1º\", \"2º\", \"3º\").forEach"))
        assertTrue(tab.contains("viewModel.setGroupPriority(groupName, prio)"))
        assertTrue(tab.contains("modifier = Modifier.weight(1f)"))
        assertTrue(tab.contains("text = priorityLabel"))
    }

    @Test
    fun `ability filter remains type specific on narrow screens`() {
        val tab = File("src/main/java/com/example/ui/tabs/AbilitiesTab.kt").readText()
        assertTrue(tab.contains("val columns = if (maxWidth < 390.dp) 1 else 2"))
        assertTrue(tab.contains("maxItemsInEachRow = columns"))
        assertTrue(tab.contains("sheet.tipoPersonagem.isLunar()"))
        assertTrue(tab.contains("0 to \"Todas\", 2 to \"Com Pontos\""))
        assertTrue(tab.contains("onSelected(i)"))
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
    fun `tabs twelve through fifteen consume scoped identity without replacing functional canvases`() {
        val conflict = File("src/main/java/com/example/iniciativas/IniciativasTab.kt").readText()
        val conflictComponents = File("src/main/java/com/example/iniciativas/IniciativasTabComponents.kt").readText()
        val connections = File("src/main/java/com/example/iniciativas/IniciativasConnectionsCanvas.kt").readText()
        val troops = File("src/main/java/com/example/ui/tabs/BattleGroupsTab.kt").readText()
        val map = File("src/main/java/com/example/ui/tabs/MapTab.kt").readText()
        val translator = File("src/main/java/com/example/oldrealm/TranslatorTab.kt").readText()

        listOf(conflict, conflictComponents, connections, troops, map, translator).forEach { source ->
            assertTrue(source.contains("LocalExaltedVisualTemplate"))
        }
        assertTrue(conflict.contains("colors = exaltedTextFieldColors()"))
        assertTrue(troops.contains("val coresCampoTexto = exaltedTextFieldColors()"))
        assertTrue(map.contains("val mapAccent = if (gold == ExaltedAccentBright) visual.accentBright else gold"))
        assertTrue(translator.contains("val resolvedGold = if (gold == com.example.ui.theme.ExaltedAccentBright) visual.accentBright else gold"))
        assertTrue(connections.contains("Canvas(modifier)"))
        assertTrue(map.contains("androidx.compose.foundation.Canvas"))
        assertTrue(translator.contains("drawWithContent"))
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
    fun `identity surface is scoped and keeps safe primitives only`() {
        assertTrue(identity.contains("@Composable\nfun Modifier.exaltedTabIdentity"))
        assertTrue(identity.contains("@Composable\nfun Modifier.exaltedContentStage"))
        assertTrue(identity.contains("@Composable\nfun Modifier.exaltedExistingPanel"))
        assertTrue(identity.contains("LocalExaltedVisualTemplate.current"))
        assertFalse(identity.contains("Canvas("))
        assertFalse(identity.contains("drawBehind"))
        assertFalse(identity.contains("drawWithContent"))
    }

    @Test
    fun `saved sheets list displays correct type specific caste or aspect`() {
        assertTrue(lifecycleDialogs.contains("s.tipoPersonagem.isDragonBlooded() -> \"Aspecto:"))
        assertTrue(lifecycleDialogs.contains("s.tipoPersonagem.isLunar() -> \"Casta:"))
        assertTrue(lifecycleDialogs.contains("s.lunarCasta.displayName"))
        assertTrue(lifecycleDialogs.contains("s.casta.displayName"))
        assertTrue(lifecycleDialogs.contains("text = s.nomeArquivo(),"))
        assertTrue(lifecycleDialogs.contains("maxLines = 2,\n                                    overflow = TextOverflow.Ellipsis"))
        assertTrue(lifecycleDialogs.contains("color = visual.muted,\n                                    maxLines = 2,\n                                    overflow = TextOverflow.Ellipsis"))
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
        assertTrue(lifecycleDialogs.contains("AppText(text = \"×\", color = visual.onSurface)"))
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
        assertTrue(dialogs.contains("AppText(\"English\", color = visual.muted, modifier = Modifier.weight(1f))"))
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
