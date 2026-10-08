package com.example.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lê `pre_requisitos` de JSONs novos e antigos.
 *
 * O formato histórico usa String. JSONs futuros podem usar uma lista de
 * strings sem exigir mudança no restante da árvore; a informação é
 * preservada como texto separado por `;`, e o resolvedor da árvore encontra
 * cada nome pelo conteúdo, não pelo separador.
 */
internal fun JSONObject.optPrerequisitosJson(chave: String = "pre_requisitos"): String {
    val chaveEncontrada = listOf(chave, "preRequisitos", "prerequisitos")
        .firstOrNull { has(it) && !isNull(it) }
        ?: return ""
    return when (val value = opt(chaveEncontrada)) {
        is JSONArray -> (0 until value.length())
            .map { value.optString(it).trim() }
            .filter { it.isNotBlank() }
            .joinToString("; ")
        else -> optString(chaveEncontrada, "").trim()
    }
}
