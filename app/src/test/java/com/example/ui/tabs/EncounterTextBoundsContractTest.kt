package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Protege a geometria da Aba 11 em celulares com tela estreita e textos longos.
 * Testes estruturais não substituem uma inspeção em aparelho/emulador.
 */
class EncounterTextBoundsContractTest {
    private val generator = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()
    private val card = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
    private val sections = File("src/main/java/com/example/ui/tabs/EncounterNpcCardSections.kt").readText()
    private val components = File("src/main/java/com/example/ui/tabs/EncounterNpcCardComponents.kt").readText()

    @Test fun `NPC tab labels are bounded and ellipsized rather than overflowing`() {
        val start = generator.indexOf("rotulosAba[npc.id] ?: npc.nome")
        assertTrue(start > 0)
        val label = generator.substring((start - 220).coerceAtLeast(0), (start + 460).coerceAtMost(generator.length))
        assertTrue(label.contains("widthIn(max = 152.dp)"))
        assertTrue(label.contains("TextOverflow.Ellipsis"))
        assertTrue(label.contains("maxLines = 1"))
        assertTrue(generator.contains("horizontalScroll(npcBotoesScrollState)"))
    }

    @Test fun `NPC skill rows are constrained and merit titles wrap`() {
        val skills = sections.substring(sections.indexOf("internal fun NpcCardSkills("))
        assertTrue(skills.contains("modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)"))
        assertTrue(skills.contains("modifier = Modifier.weight(1f)"))
        val meritStart = sections.indexOf("merito.linhaExibicao()")
        val meritLine = sections.substring(meritStart, (meritStart + 420).coerceAtMost(sections.length))
        assertTrue(meritLine.contains("softWrap = true"))
        assertTrue(meritLine.contains(".fillMaxWidth()"))
        assertFalse(meritLine.contains("softWrap = false"))
    }

    @Test fun `charms and spell descriptions stay within NPC card width`() {
        val charmStart = card.indexOf("val nomeExibicao =")
        val charm = card.substring(charmStart, (charmStart + 650).coerceAtMost(card.length))
        assertTrue(charm.contains(".fillMaxWidth()"))
        val spellStart = card.indexOf("feitico.nome} — Círculo")
        val spell = card.substring(spellStart, (spellStart + 600).coerceAtMost(card.length))
        assertTrue(spell.contains(".fillMaxWidth()"))
    }

    @Test fun `calculation popup is bounded by phone screen and wraps text`() {
        val start = components.indexOf("if (mostrarCalculo && calculo != null)")
        val end = components.indexOf("if (mostrarLongPress", start)
        assertTrue(start >= 0 && end > start)
        val popup = components.substring(start, end)
        assertTrue(popup.contains("LocalConfiguration.current.screenWidthDp - 48"))
        assertTrue(popup.contains("modifier = Modifier.fillMaxWidth().padding(12.dp)"))
        assertTrue(popup.contains("softWrap = true"))
    }
}
