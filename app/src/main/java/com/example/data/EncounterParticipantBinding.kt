package com.example.data

@JvmInline
internal value class EncounterParticipantId(val value:String) {
    init { require(value.isNotBlank()) { "ParticipantId vazio." } }
}

@JvmInline
internal value class EncounterBindingKey(val value:String) {
    init { require(value.isNotBlank()) { "BindingKey vazio." } }
}

internal object EncounterParticipantBindings {
    enum class Relation { SAME, DIFFERENT }
    data class Constraint(
        val left:EncounterBindingKey,
        val right:EncounterBindingKey,
        val relation:Relation
    )
    enum class Check { SATISFIED, VIOLATED, UNKNOWN }

    fun check(
        bindings:Map<EncounterBindingKey,EncounterParticipantId>,
        constraint:Constraint
    ):Check {
        val a=bindings[constraint.left] ?: return Check.UNKNOWN
        val b=bindings[constraint.right] ?: return Check.UNKNOWN
        return when(constraint.relation) {
            Relation.SAME -> if(a==b) Check.SATISFIED else Check.VIOLATED
            Relation.DIFFERENT -> if(a!=b) Check.SATISFIED else Check.VIOLATED
        }
    }
}
