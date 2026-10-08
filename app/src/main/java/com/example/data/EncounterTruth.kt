package com.example.data

/**
 * Avaliação em três valores.
 *
 * Ausência de prova não equivale automaticamente a FALSE quando o parser
 * ou a normalização ainda não conhecem a informação.
 */
internal enum class EncounterTruth {
    TRUE,
    FALSE,
    UNKNOWN;

    infix fun and(other: EncounterTruth): EncounterTruth = when {
        this == FALSE || other == FALSE -> FALSE
        this == UNKNOWN || other == UNKNOWN -> UNKNOWN
        else -> TRUE
    }

    infix fun or(other: EncounterTruth): EncounterTruth = when {
        this == TRUE || other == TRUE -> TRUE
        this == UNKNOWN || other == UNKNOWN -> UNKNOWN
        else -> FALSE
    }

    fun not(): EncounterTruth = when(this) {
        TRUE -> FALSE
        FALSE -> TRUE
        UNKNOWN -> UNKNOWN
    }
}

/**
 * Requisito tri-state para impedir que "não extraído" vire "não existe"
 * e que NOT(UNKNOWN) vire TRUE.
 */
internal sealed interface EncounterTriRequirement {
    fun evaluate(knownTrue:Set<String>, knownFalse:Set<String>): EncounterTruth

    data class Fact(val id:String):EncounterTriRequirement {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>)=when {
            id in knownTrue -> EncounterTruth.TRUE
            id in knownFalse -> EncounterTruth.FALSE
            else -> EncounterTruth.UNKNOWN
        }
    }
    data class AllOf(val xs:List<EncounterTriRequirement>):EncounterTriRequirement {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>) =
            xs.fold(EncounterTruth.TRUE){a,x -> a and x.evaluate(knownTrue,knownFalse)}
    }
    data class AnyOf(val xs:List<EncounterTriRequirement>):EncounterTriRequirement {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>) =
            xs.fold(EncounterTruth.FALSE){a,x -> a or x.evaluate(knownTrue,knownFalse)}
    }
    data class Not(val x:EncounterTriRequirement):EncounterTriRequirement {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>)=x.evaluate(knownTrue,knownFalse).not()
    }
}
