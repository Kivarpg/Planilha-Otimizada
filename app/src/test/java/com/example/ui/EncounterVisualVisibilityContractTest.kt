package com.example.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Source-level regression checks; real device visual testing remains necessary. */
class EncounterVisualVisibilityContractTest {
    private val generator = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()
    private val npcCard = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
    private val tabs = File("src/main/java/com/example/ui/SheetTabs.kt").readText()

    @Test
    fun `generate npc labels remain present in both layouts`() {
        assertEquals(2, generator.split("text = \"Gerar NPC\"").size - 1)
        assertEquals(2, generator.split(".background(ExaltedAccentBright, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))").size - 1)
        assertTrue(generator.contains("enabled = !gerando, role = Role.Button"))
    }

    @Test
    fun `charm drawer heading remains visible and interactive`() {
        assertTrue(npcCard.contains("text = \"\$habilidade (\${encantosDaGaveta.size})\""))
        assertTrue(npcCard.contains(".border(2.dp, visualTemplate.accentBright, RoundedCornerShape(8.dp))"))
        assertTrue(npcCard.contains("gavetasExpandidas = if (expandida)"))
    }

    @Test
    fun `fifteen tab slots remain defined`() {
        for (number in 1..15) {
            assertTrue(tabs.contains("\"$number. ") || (number == 2 && tabs.contains("2. Casta")), "Missing tab $number")
        }
    }
}
