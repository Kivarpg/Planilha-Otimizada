package com.example.viewmodel

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncounterPreparedCatalogSharingRegressionTest {
    private fun source(relative: String): String {
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(8) {
            val candidate = File(dir, relative)
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile ?: return@repeat
        }
        error("Source not found: $relative")
    }

    @Test
    fun `encounter actions must receive the shared prepared catalog instead of rebuilding it`() {
        val actions = source("app/src/main/java/com/example/viewmodel/EncounterNpcActions.kt")
        val viewModel = source("app/src/main/java/com/example/viewmodel/SheetViewModel.kt")

        assertTrue(actions.contains("private val preparedEncounterCatalog: PreparedEncounterCatalog,"))
        assertFalse(actions.contains("preparedEncounterCatalog: PreparedEncounterCatalog = PreparedEncounterCatalog.prepare("))
        assertTrue(viewModel.contains("preparedEncounterCatalog = preparedEncounterCatalog"))
    }
}
