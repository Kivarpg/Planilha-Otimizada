package com.example.feature.charmtree

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CharmTreeThemeReferenceRegressionTest {
    @Test
    fun `blocked visual status imports the existing muted theme token`() {
        fun projectFile(relative: String): File = listOf(
            File(relative),
            File("app", relative)
        ).firstOrNull(File::isFile)
            ?: error("Project source not found: $relative (cwd=${System.getProperty("user.dir")})")

        val source = projectFile("src/main/java/com/example/feature/charmtree/CharmPrerequisiteTreeDialog.kt").readText()
        val colors = projectFile("src/main/java/com/example/ui/theme/Color.kt").readText()

        assertTrue("val ExaltedMuted" in colors)
        assertTrue("import com.example.ui.theme.ExaltedMuted" in source)
        assertTrue("CharmTreeVisualStatus.BLOCKED -> ExaltedMuted" in source)
    }
}
