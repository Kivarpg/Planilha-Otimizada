package com.example.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

/**
 * Pasta pública de documentos do aplicativo (nome estável, independente da build).
 * Android 10+: MediaStore, sem acesso amplo ao armazenamento.
 * Android 9 e anteriores: diretório público Documents (permissão legada).
 */
object ExaltedDocumentFolders {
    const val APP = "Exalted"
    const val PLAYER = "Jogador"
    const val NPC = "NPC's"

    fun relativePath(category: String): String {
        require(category == PLAYER || category == NPC)
        return "${Environment.DIRECTORY_DOCUMENTS}/$APP/$category/"
    }

    /** Salva uma cópia pública sem alterar a fonte privada usada pelos carregamentos legados. */
    fun write(context: Context, category: String, name: String, mime: String, writer: (OutputStream) -> Unit): File {
        require(category == PLAYER || category == NPC)
        require(name.isNotBlank() && name == File(name).name && '/' !in name && '\\' !in name)
        val target = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$APP/$category/$name")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(category))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: throw IllegalStateException("Não foi possível criar o arquivo na pasta Exalted/$category")
            try {
                resolver.openOutputStream(uri, "w")?.use(writer)
                    ?: throw IllegalStateException("Não foi possível gravar o arquivo na pasta Exalted/$category")
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val directory = target.parentFile ?: throw IllegalStateException("Diretório inválido")
            if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                throw IllegalStateException("Não foi possível criar Exalted/$category")
            }
            val temp = File(directory, ".${name}.tmp")
            try {
                temp.outputStream().use(writer)
                if (!temp.renameTo(target)) throw IllegalStateException("Falha ao publicar o arquivo")
            } finally {
                temp.delete()
            }
        }
        return target
    }

    fun safeName(value: String): String = value
        .trim().ifBlank { "Sem Nome" }
        .replace(Regex("[\\\\/:*?\\\"<>|\\r\\n\\t]"), "_")
        .take(80).ifBlank { "Sem Nome" }
}
