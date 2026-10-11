package com.example.feature.charmtree

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharmTreeThemeReferenceRegressionTest {
    @Test
    fun `blocked visual status uses the scoped muted visual token`() {
        fun projectFile(relative: String): File = listOf(
            File(relative),
            File("app", relative)
        ).firstOrNull(File::isFile)
            ?: error("Project source not found: $relative (cwd=${System.getProperty("user.dir")})")

        val source = projectFile("src/main/java/com/example/feature/charmtree/CharmPrerequisiteTreeDialog.kt").readText()

        assertTrue("LocalExaltedVisualTemplate.current" in source)
        assertTrue("CharmTreeVisualStatus.BLOCKED -> visual.muted" in source)
        assertFalse("import com.example.ui.theme.ExaltedMuted" in source)
    }
}
