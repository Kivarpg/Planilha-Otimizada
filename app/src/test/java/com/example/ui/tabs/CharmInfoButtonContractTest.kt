package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharmInfoButtonContractTest {
    private val source = File("src/main/java/com/example/ui/tabs/CharmListComponents.kt").readText()

    @Test fun `charm row name and lock do not open details or tree`() {
        val start = source.indexOf("internal fun CharmEligibilityRow")
        val end = source.indexOf("internal fun CharmAbilityButton", start)
        val row = source.substring(start, end)
        assertFalse(row.contains("detectTapGestures"))
        assertFalse(row.contains("onDoubleTap"))
    }

    @Test fun `info button remains the detail and tree entry inside charm row`() {
        val start = source.indexOf("internal fun CharmEligibilityRow")
        val end = source.indexOf("internal fun CharmAbilityButton", start)
        val row = source.substring(start, end)
        assertTrue(row.contains("InkButton(onClick = onShowDetail"))
        assertTrue(row.contains("Icons.Outlined.Info"))
        assertTrue(row.contains("contentDescription = \"Ver detalhes\""))
    }
}
