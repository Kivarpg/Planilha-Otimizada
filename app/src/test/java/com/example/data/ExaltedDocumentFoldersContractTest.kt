package com.example.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Contract for exported files; does not require Android storage on the JVM. */
class ExaltedDocumentFoldersContractTest {
    private val storage = File("src/main/java/com/example/data/ExaltedDocumentFolders.kt").readText()
    private val repository = File("src/main/java/com/example/data/SheetRepository.kt").readText()
    private val actions = File("src/main/java/com/example/viewmodel/FileManagementActions.kt").readText()
    private val npcSaves = File("src/main/java/com/example/data/NpcSaveLoadService.kt").readText()

    @Test fun `public documents use app label and separate player NPC folders`() {
        assertTrue(storage.contains("const val APP = \"Gerador Harmônico\""))
        assertTrue(storage.contains("const val PLAYER = \"Jogador\""))
        assertTrue(storage.contains("const val NPC = \"NPC's\""))
        assertTrue(storage.contains("Environment.DIRECTORY_DOCUMENTS"))
        assertTrue(storage.contains("MediaStore.MediaColumns.RELATIVE_PATH"))
    }

    @Test fun `manual sheet save and share export reach player folder`() {
        assertTrue(actions.contains("repository.publicarPlanilhaJogador(atualizado)"))
        assertTrue(actions.contains("repository.publicarCodigoJogador(current, codigo)"))
        assertTrue(repository.contains("ExaltedDocumentFolders.PLAYER, name, \"application/json\""))
        assertTrue(repository.contains("ExaltedDocumentFolders.PLAYER, name, \"text/plain\""))
        assertTrue(repository.contains("documentExportScope.launch"))
    }

    @Test fun `NPC save keeps private loading copy and publishes a public copy`() {
        assertTrue(npcSaves.contains("val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)"))
        assertTrue(npcSaves.contains("ExaltedDocumentFolders.write("))
        assertTrue(npcSaves.contains("ExaltedDocumentFolders.NPC, file.name"))
        assertTrue(npcSaves.contains("file.inputStream().use { it.copyTo(output) }"))
    }
}
