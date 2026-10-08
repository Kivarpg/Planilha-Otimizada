package com.example.data

import android.content.Context
import android.os.Environment
import com.example.model.NpcEncontro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

/** Persistência de NPCs em arquivos .save, sempre executada em Dispatchers.IO. */
object NpcSaveLoadService {
    private const val MAX_SAVE_BYTES = 4L * 1024L * 1024L
    private const val SAVE_EXTENSION = ".save"

    private fun diretorio(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
            throw IllegalStateException("Não foi possível criar o diretório de NPCs")
        }
        return dir
    }

    private fun tipoExaltadoTexto(npc: NpcEncontro): String = when (npc.tipoExaltado) {
        com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> "Sangue de Dragão"
        com.example.model.TipoExaltadoEncontro.LUNAR -> "Lunar"
        com.example.model.TipoExaltadoEncontro.SOLAR -> "Solar"
    }

    private fun nomeSeguro(valor: String): String = valor
        .ifBlank { "Sem Nome" }
        .replace(Regex("[\\/:*?\"<>|\\r\\n\\t]"), "_")
        .trim()
        .take(100)
        .ifBlank { "Sem Nome" }

    private fun nomeArquivo(numero: Int, npc: NpcEncontro, dataFormatada: String): String {
        val numeroFormatado = numero.coerceAtLeast(1).toString().padStart(3, '0')
        return "$numeroFormatado - (NPC) - ${nomeSeguro(npc.nome)} - ${tipoExaltadoTexto(npc)}.$dataFormatada.save"
    }

    private fun limparParaSalvar(npc: NpcEncontro): NpcEncontro = npc.copy(
        forcaDeVontadeUsados = emptySet(),
        healthBoxes = npc.healthBoxes.map { it.copy(tipoDano = 0) },
        iniciativaAtual = 0
    )

    suspend fun salvar(context: Context, npc: NpcEncontro, proximoNumero: Int): File = withContext(Dispatchers.IO) {
        val dataFormatada = java.text.SimpleDateFormat("dd.MM.yy", Locale.forLanguageTag("pt-BR")).format(java.util.Date())
        val limpo = limparParaSalvar(npc)
        val dir = diretorio(context)
        // Serializa a escolha do nome e a publicação do arquivo no processo.
        // Sem isso, dois salvamentos simultâneos podem observar o mesmo nome
        // livre e o segundo renameTo sobrescrever o primeiro.
        synchronized(this@NpcSaveLoadService) {
            val nomeBase = nomeArquivo(proximoNumero, limpo, dataFormatada)
            var file = File(dir, nomeBase)
            if (file.exists()) {
                val base = nomeBase.removeSuffix(SAVE_EXTENSION)
                do {
                    file = File(dir, "${base}-${UUID.randomUUID()}$SAVE_EXTENSION")
                } while (file.exists())
            }
            val temp = File(dir, ".${file.name}.tmp-${UUID.randomUUID()}")
            try {
                temp.writeText(NpcEncontroJsonCodec.encode(limpo), Charsets.UTF_8)
                if (temp.length() > MAX_SAVE_BYTES) {
                    throw IllegalArgumentException("NPC salvo excede o limite de ${MAX_SAVE_BYTES / (1024 * 1024)} MB")
                }
                if (!temp.renameTo(file)) {
                    throw IllegalStateException("Não foi possível concluir o salvamento do NPC")
                }
                file
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun listarSalvos(context: Context): List<File> = withContext(Dispatchers.IO) {
        diretorio(context).listFiles { f ->
            f.isFile && f.name.contains("(NPC)") && f.name.endsWith(SAVE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun carregar(context: Context, file: File): NpcEncontro = withContext(Dispatchers.IO) {
        val dir = diretorio(context).canonicalFile
        val alvo = file.canonicalFile
        val pertenceAoDiretorio = alvo.parentFile?.canonicalFile == dir
        if (!pertenceAoDiretorio || !alvo.isFile || !alvo.name.endsWith(SAVE_EXTENSION)) {
            throw NpcSaveCorruptedException("Arquivo de NPC inválido ou fora do diretório permitido.")
        }
        if (alvo.length() > MAX_SAVE_BYTES) {
            throw NpcSaveCorruptedException("Arquivo de NPC excede o limite permitido.")
        }
        try {
            NpcEncontroJsonCodec.decode(alvo.readText(Charsets.UTF_8)).copy(id = UUID.randomUUID().toString())
        } catch (e: NpcSaveCorruptedException) {
            throw e
        } catch (e: Exception) {
            throw NpcSaveCorruptedException("Arquivo de NPC corrompido ou inválido.", e)
        }
    }
}
