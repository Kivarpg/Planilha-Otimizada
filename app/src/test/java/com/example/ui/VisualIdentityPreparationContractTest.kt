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
}
