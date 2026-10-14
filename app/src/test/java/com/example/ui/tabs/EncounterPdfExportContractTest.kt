package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class EncounterPdfExportContractTest {
    private val source = File("src/main/java/com/example/ui/tabs/EncounterCombatEquations.kt").readText()
    private val storage = File("src/main/java/com/example/data/ExaltedDocumentFolders.kt").readText()

    @Test fun `npc pdf uses shared public folder in modern Android`() {
        assertTrue(source.contains("ExaltedDocumentFolders.write("))
        assertTrue(source.contains("category = com.example.data.ExaltedDocumentFolders.NPC"))
        assertTrue(source.contains("mime = \"application/pdf\""))
        assertTrue(storage.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q"))
        assertTrue(storage.contains("MediaStore.MediaColumns.RELATIVE_PATH, relativePath(category)"))
        assertTrue(storage.contains("MediaStore.VOLUME_EXTERNAL_PRIMARY"))
        assertTrue(storage.contains("resolver.openOutputStream(uri, \"w\")"))
    }

    @Test fun `legacy pdf uses public folder on supported versions`() {
        assertTrue(storage.contains("Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)"))
        assertTrue(storage.contains("target.parentFile"))
        assertTrue(storage.contains("temp.outputStream().use(writer)"))
    }

    @Test fun `exports use installed app name and the NPC directory`() {
        assertTrue(storage.contains("const val APP = \"Gerador Harmônico\""))
        assertTrue(storage.contains("const val NPC = \"NPC's\""))
        val card = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
        assertTrue(card.contains("PDF salvo em Documentos:"))
    }
}
