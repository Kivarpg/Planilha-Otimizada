package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class EncounterPdfExportContractTest {
    private val source = File("src/main/java/com/example/ui/tabs/EncounterCombatEquations.kt").readText()

    @Test fun `npc pdf remains in public Documents on modern Android`() {
        assertTrue(source.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q"))
        assertTrue(source.contains("MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS"))
        assertTrue(source.contains("MediaStore.VOLUME_EXTERNAL_PRIMARY"))
        assertTrue(source.contains("resolver.openOutputStream(uri, \"w\")"))
    }

    @Test fun `legacy npc pdf remains in public Documents`() {
        assertTrue(source.contains("Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)"))
        assertTrue(source.contains("FileOutputStream(File(publicDir, nomeArquivo))"))
    }

    @Test fun `encounter export tells the user the public destination`() {
        val card = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
        assertTrue(card.contains("PDF salvo em Documentos: \${arquivo.name}"))
    }
}
