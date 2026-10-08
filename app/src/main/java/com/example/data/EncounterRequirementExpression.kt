package com.example.data

/**
 * Expressão lógica única para requisitos.
 * `evaluate` preserva UNKNOWN; `isSatisfiedBy` é apenas o adaptador CLOSED_WORLD.
 */
internal sealed interface EncounterRequirementExpression {
    fun evaluate(knownTrue:Set<String>, knownFalse:Set<String>):EncounterTruth

    /**
     * Adaptador legado de mundo fechado: fatos ausentes são FALSE.
     * A avaliação tri-state deve usar `evaluate` diretamente.
     */
    fun isSatisfiedBy(facts:Set<String>):Boolean = when(this) {
        Always -> true
        is Fact -> id in facts
        is AllOf -> requirements.all { it.isSatisfiedBy(facts) }
        is AnyOf -> requirements.any { it.isSatisfiedBy(facts) }
        is OneOf -> requirements.count { it.isSatisfiedBy(facts) }==1
        is Not -> !requirement.isSatisfiedBy(facts)
    }

    data object Always:EncounterRequirementExpression {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>)=EncounterTruth.TRUE
    }
    data class Fact(val id:String):EncounterRequirementExpression {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>)=when {
            id in knownTrue -> EncounterTruth.TRUE
            id in knownFalse -> EncounterTruth.FALSE
            else -> EncounterTruth.UNKNOWN
        }
    }
    data class AllOf(val requirements:List<EncounterRequirementExpression>):EncounterRequirementExpression {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>) =
            requirements.fold(EncounterTruth.TRUE){a,x->a and x.evaluate(knownTrue,knownFalse)}
    }
    data class AnyOf(val requirements:List<EncounterRequirementExpression>):EncounterRequirementExpression {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>) =
            requirements.fold(EncounterTruth.FALSE){a,x->a or x.evaluate(knownTrue,knownFalse)}
    }
    data class OneOf(val requirements:List<EncounterRequirementExpression>):EncounterRequirementExpression {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>):EncounterTruth {
            var trueCount=0; var unknownCount=0
            for(r in requirements) when(r.evaluate(knownTrue,knownFalse)) {
                EncounterTruth.TRUE -> trueCount++
                EncounterTruth.UNKNOWN -> unknownCount++
                EncounterTruth.FALSE -> Unit
            }
            return when {
                trueCount>1 -> EncounterTruth.FALSE
                trueCount==1 && unknownCount==0 -> EncounterTruth.TRUE
                trueCount==0 && unknownCount==0 -> EncounterTruth.FALSE
                else -> EncounterTruth.UNKNOWN
            }
        }
    }
    data class Not(val requirement:EncounterRequirementExpression):EncounterRequirementExpression {
        override fun evaluate(knownTrue:Set<String>,knownFalse:Set<String>)=requirement.evaluate(knownTrue,knownFalse).not()
    }
}
