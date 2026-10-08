package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class TabRedundantTitleContractTest {
    private fun source(path: String) = File("src/main/java/$path").readText()

    @Test fun `tabs do not repeat navigation title as content header`() {
        val forbidden = mapOf(
            "com/example/ui/tabs/PersonalDataTab.kt" to listOf("SectionHeader(BoxNames.PersonalData.TAB_TITLE)"),
            "com/example/ui/tabs/AttributesTab.kt" to listOf("SectionHeader(title = \"Atributos\")"),
            "com/example/ui/tabs/AbilitiesTab.kt" to listOf("SectionHeader(title = \"Habilidades\")"),
            "com/example/ui/tabs/MeritsTab.kt" to listOf("SectionHeader(title = \"Méritos\")"),
            "com/example/ui/tabs/EquipmentTab.kt" to listOf("SectionHeader(title = \"Equipamentos\")"),
            "com/example/ui/tabs/CharmsTab.kt" to listOf("SectionHeader(title = \"Encantos\")"),
            "com/example/ui/tabs/SummaryTab.kt" to listOf("SectionHeader(title = \"Planilha\")"),
            "com/example/ui/tabs/NPCsTab.kt" to listOf("SectionHeader(title = \"NPCs\")"),
            "com/example/ui/tabs/EncounterGeneratorTab.kt" to listOf("SectionHeader(title = \"Gerador de encontros\")"),
            "com/example/iniciativas/IniciativasTab.kt" to listOf("SectionHeader(title = \"Conflito\")"),
            "com/example/ui/tabs/BattleGroupsTab.kt" to listOf("SectionHeader(\"Grupos de Batalha\")"),
            "com/example/ui/tabs/MapTab.kt" to listOf("SectionHeader(title = \"Mapa\")"),
            "com/example/oldrealm/TranslatorTab.kt" to listOf("SectionHeader(title = \"Tradutor\")")
        )
        forbidden.forEach { (path, patterns) ->
            val text = source(path)
            patterns.forEach { pattern ->
                assertFalse(text.contains(pattern), "Redundant tab header returned in $path: $pattern")
            }
        }
    }
}
