package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeiticosSearchPreparedIndexRegressionTest {
    private fun projectFile(relative: String): File {
        val direct = File(relative)
        if (direct.isFile) return direct
        val underApp = File("app", relative)
        if (underApp.isFile) return underApp
        error("Arquivo do projeto não encontrado: $relative")
    }

    @Test
    fun `spell search prepares normalized catalog outside query filtering`() {
        val source = projectFile("src/main/java/com/example/ui/tabs/FeiticosTab.kt").readText()
        assertTrue("feiticosIndexadosPorCirculo" in source)
        assertTrue("searchable.namePtNormalized.contains(buscaNormalizada)" in source)
        assertTrue("searchable.nameEnNormalized.contains(buscaNormalizada)" in source)
        assertFalse("SkillSearchEngine.normalize(def.nome).contains(buscaNormalizada)" in source)
        assertFalse("SkillSearchEngine.normalize(def.nomeIngles).contains(buscaNormalizada)" in source)
    }
}
