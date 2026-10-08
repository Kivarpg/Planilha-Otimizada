package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class EncounterFocusUiContractTest {
    private val source = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()

    @Test fun `focus menu uses short labels while null remains automatic default`() {
        listOf(
            "Triple(customFoco ?: \"Foco\", \"Foco\", 0)",
            "Triple(customSecundaria ?: \"Foco Secundário\", \"Secundária\", 1)",
            "Triple(customAtaque ?: \"Ofensivo\", \"Ataque\", 2)",
            "Triple(customDefesa ?: \"Defensivo\", \"Defesa\", 3)"
        ).forEach { assertTrue(source.contains(it), "Missing short label contract: $it") }

        assertTrue(!source.contains("Foco — \${valorVisivel"))
        assertTrue(!source.contains("Foco Secundário — \${valorVisivel"))
        assertTrue(source.contains("null -> com.example.data.EncounterCustomizationMode.AUTOMATICO"))
        assertTrue(source.contains("label = \"Automático\""))
        assertTrue(source.contains("FOCUS_OPTION_NONE"))
        assertTrue(source.contains("EncounterCustomizationMode.NENHUM"))
    }

    @Test fun `apply is left of clear in same row`() {
        val actions = source.substring(source.indexOf("dismissButton = {}"), source.indexOf("if (mostrarCatalogoArmas)"))
        assertTrue(actions.contains("Row("))
        assertTrue(actions.indexOf("label = \"Aplicar\"") < actions.indexOf("label = \"Limpar\""))
        assertTrue(actions.count { it == 'w' } >= 2)
    }
    @Test fun `archetypes and focus use two columns instead of a compressed four button row`() {
        val start = source.indexOf("AppText(\"3. Arquétipo\"")
        val end = source.indexOf("if (tipoSangueDeDragao)", start)
        val section = source.substring(start, end)
        assertTrue(section.contains("FlowRow("))
        assertTrue(section.contains("maxItemsInEachRow = 2"))
        assertTrue(section.contains("ARQUETIPO_OPCOES.forEach"))
        assertTrue(section.contains("label = \"Foco\""))
        assertTrue(!section.contains("archetypeButtonWidth"))
    }

    @Test fun `focus interface cannot disable automatic equipment`() {
        assertTrue(source.contains("val equipamentoAutomatico = true"))
        assertTrue(!source.contains("AppText(\"Equipamento automático\""))
        assertTrue(!source.contains("onCheckedChange = { equipamentoAutomatico = it }"))
    }

}
