package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeSortingAllocationRegressionTest {
    private fun projectFile(relative: String): File {
        val direct = File(relative)
        if (direct.isFile) return direct
        val underApp = File("app", relative)
        if (underApp.isFile) return underApp
        error("Project file not found: $relative")
    }

    @Test
    fun `ui sorting avoids lowercase allocation inside sort selectors`() {
        val npcSource = projectFile("src/main/java/com/example/ui/tabs/NPCsTab.kt").readText()
        val martialSource = projectFile("src/main/java/com/example/ui/tabs/MartialArtsCharmsPopup.kt").readText()

        assertFalse("sortedBy { it.nome.lowercase() }" in npcSource)
        assertFalse("sortedBy { it.nomePt.lowercase(java.util.Locale.ROOT) }" in martialSource)
        assertTrue("compareBy(String.CASE_INSENSITIVE_ORDER) { it.nome }" in npcSource)
        assertTrue("compareBy(String.CASE_INSENSITIVE_ORDER) { it.nomePt }" in martialSource)
    }

    @Test
    fun `personal data avoids flow layouts around weighted material controls`() {
        val tabSource = projectFile("src/main/java/com/example/ui/tabs/PersonalDataTab.kt").readText()
        val dialogsSource = projectFile("src/main/java/com/example/ui/tabs/PersonalDataDialogs.kt").readText()

        assertFalse("FlowRow(" in tabSource)
        assertFalse("FlowRow(" in dialogsSource)
        assertTrue("Row(" in tabSource)
        assertTrue("Row(" in dialogsSource)
    }
}
