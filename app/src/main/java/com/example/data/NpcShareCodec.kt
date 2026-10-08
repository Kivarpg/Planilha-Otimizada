package com.example.data

import com.example.model.Npc
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Sistema de código de compartilhamento de NPCs — independente do código
// de planilha (ShareCodeCodec): prefixo próprio ("APPNPCS"), então um código
// de NPCs colado na tela de importar planilha (ou vice-versa) é rejeitado
// com uma mensagem clara, não interpretado incorretamente. Reaproveita o
// mesmo núcleo de compressão (GenericShareCodec) — só o payload
// (lista de NPCs, não uma planilha inteira) e o dicionário mudam.
object NpcShareCodec {
    private const val PREFIXO = "APPNPCS"
    private const val VERSAO_ATUAL = 1
    private const val MAX_NPCS_IMPORTADOS = 256
    const val TAMANHO_MAXIMO_CODIGO = 100_000

    // Vocabulário bem menor que o de planilha — só os campos de Npc e os 3
    // valores possíveis de lealdade.
    private val DICIONARIO_COMPRESSAO: ByteArray =
        "\"id\":\"nome\":\"lealdade\":\"tipo\":\"descricao\":\"Aliado\",\"Inimigo\",\"Neutro\""
            .toByteArray(Charsets.UTF_8)

    sealed class ResultadoImportacao {
        data class Sucesso(val npcs: List<Npc>) : ResultadoImportacao()
        data class Erro(val mensagem: String) : ResultadoImportacao()
    }

    private fun npcsParaJson(npcs: List<Npc>): String {
        val arr = JSONArray()
        npcs.forEach { arr.put(JSONObject(ModelJsonCodecs.npcToMap(it))) }
        return arr.toString()
    }

    private fun jsonParaNpcs(json: String): List<Npc> {
        val arr = JSONArray(json)
        require(arr.length() <= MAX_NPCS_IMPORTADOS) { "A lista de NPCs excede o limite permitido." }
        return (0 until arr.length()).map { i -> ModelJsonCodecs.npcFromJson(arr.getJSONObject(i)) }
    }

    fun exportar(npcs: List<Npc>): Result<String> {
        if (npcs.isEmpty()) {
            return Result.failure(IllegalStateException("Cadastre ao menos um NPC antes de compartilhar."))
        }
        return GenericShareCodec.codificar(PREFIXO, VERSAO_ATUAL, npcsParaJson(npcs), DICIONARIO_COMPRESSAO, TAMANHO_MAXIMO_CODIGO)
    }

    // Retorna a lista de NPCs decodificada — quem chama decide se
    // substitui a lista local ou faz merge com os NPCs já existentes
    // (a mesclagem por nome/id fica na camada de ViewModel, não aqui).
    fun importar(codigoBruto: String): ResultadoImportacao {
        val resultado = GenericShareCodec.decodificar(codigoBruto, PREFIXO, VERSAO_ATUAL, DICIONARIO_COMPRESSAO, TAMANHO_MAXIMO_CODIGO)
        val json = when (resultado) {
            is GenericShareCodec.Resultado.Erro -> return ResultadoImportacao.Erro(resultado.mensagem)
            is GenericShareCodec.Resultado.Sucesso -> resultado.payload
        }

        val npcsImportados = try {
            jsonParaNpcs(json)
        } catch (e: Exception) {
            return ResultadoImportacao.Erro("A lista de NPCs contém dados inválidos.")
        }

        // Mesma regra do código de planilha: nunca reaproveita o id de
        // origem, pra não colidir com um NPC já existente localmente que
        // por acaso tenha o mesmo id (ex.: o próprio usuário importando
        // de volta um código que ele mesmo gerou antes).
        val npcsProntos = npcsImportados.map { it.copy(id = UUID.randomUUID().toString()) }
        return ResultadoImportacao.Sucesso(npcsProntos)
    }
}
