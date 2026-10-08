package com.example.data

/**
 * Estrutura tipada para novas validações da Aba 11.
 *
 * A UI e o modelo persistido continuam usando List<String> nesta etapa.
 * [asLegacyMessages] é a ponte de compatibilidade para migração incremental.
 */
enum class EncounterValidationSeverity {
    ERROR,
    WARNING,
    INFO
}

data class EncounterValidationIssue(
    val code: String,
    val severity: EncounterValidationSeverity,
    val message: String
)

internal fun List<EncounterValidationIssue>.asLegacyMessages(): List<String> =
    map { it.message }
