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
        assertEquals(2, generator.split("label = \"Gerar NPC\"").size - 1)
        assertEquals(2, generator.split("onClick = gerarNpcDaInterface").size - 1)
        assertEquals(2, generator.split("enabled = !gerando").size - 1)
        assertTrue(generator.contains("val gerarNpcDaInterface: () -> Unit"))
        assertTrue(!generator.contains(".background(ExaltedAccentBright, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))"))
    }

    @Test
    fun `charm drawer heading remains visible and interactive`() {
        assertTrue(npcCard.contains("text = \"\$habilidade (\${encantosDaGaveta.size})\""))
        assertTrue(npcCard.contains("listOf(visualTemplate.metalShine, visualTemplate.accentBright, visualTemplate.metalDeep)"))
        assertTrue(npcCard.contains(".feedbackClickable {"))
        assertTrue(npcCard.contains("fontSize = 17.sp, lineHeight = 22.sp"))
        assertTrue(npcCard.contains("gavetasExpandidas = if (expandida)"))
    }

    @Test
    fun `fifteen tab slots remain defined`() {
        for (number in 1..15) {
            assertTrue(tabs.contains("\"$number. ") || (number == 2 && tabs.contains("2. Casta")), "Missing tab $number")
        }
    }
}
