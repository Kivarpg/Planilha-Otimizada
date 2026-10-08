package com.example.data

/** Regras globais para aquisição de idiomas. */
object LanguageAcquisitionRules {
    const val OLD_REALM = "Antigo Reino"

    fun podeAdquirir(idioma: String, abilities: Map<String, Int>): Boolean {
        if (!idioma.trim().equals(OLD_REALM, ignoreCase = true)) return true
        return (abilities["Ocultismo"] ?: 0) >= 1 || (abilities["Conhecimento"] ?: 0) >= 1
    }
}
