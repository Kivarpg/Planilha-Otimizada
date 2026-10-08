package com.example.data

/**
 * Declara explicitamente se ausência significa FALSE (mundo fechado)
 * ou UNKNOWN (extração/análise parcial).
 */
internal object EncounterRequirementContext {
    enum class KnowledgeMode { CLOSED_WORLD, OPEN_WORLD }

    data class Context(
        val knownTrue:Set<String>,
        val knownFalse:Set<String> = emptySet(),
        val mode:KnowledgeMode
    )

    fun fact(id:String, context:Context):EncounterTruth = when {
        id in context.knownTrue -> EncounterTruth.TRUE
        id in context.knownFalse -> EncounterTruth.FALSE
        context.mode==KnowledgeMode.CLOSED_WORLD -> EncounterTruth.FALSE
        else -> EncounterTruth.UNKNOWN
    }

    fun requireTrue(value:EncounterTruth):Boolean = value==EncounterTruth.TRUE
}
