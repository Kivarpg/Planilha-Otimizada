package com.example.data

/**
 * Projeta a versão efetiva de um poder após upgrades/recompras.
 * Deltas inválidos não são aplicados silenciosamente.
 */
internal object EncounterPowerProjection {
    enum class DeltaKind { ADD, REPLACE, REMOVE }

    data class Delta(
        val id:String,
        val kind:DeltaKind,
        val targetEffectId:String? = null,
        val effect:EncounterMechanicalEffect? = null,
        val requirement:EncounterRequirementExpression =
            EncounterRequirementExpression.Always
    )

    data class Projection(
        val effects:List<EncounterMechanicalEffect>,
        val appliedDeltaIds:Set<String>,
        val rejectedDeltaIds:Set<String> = emptySet()
    )

    private fun structurallyValid(d:Delta):Boolean = when(d.kind) {
        DeltaKind.ADD -> d.effect!=null && d.targetEffectId==null
        DeltaKind.REMOVE -> d.targetEffectId!=null && d.effect==null
        DeltaKind.REPLACE -> d.targetEffectId!=null && d.effect!=null
    }

    fun project(
        base:List<EncounterMechanicalEffect>,
        deltas:List<Delta>,
        facts:Set<String>
    ):Projection {
        val effects=base.associateBy { it.id }.toMutableMap()
        val applied=mutableSetOf<String>()
        val rejected=mutableSetOf<String>()

        for(d in deltas) {
            if(!d.requirement.isSatisfiedBy(facts)) continue
            if(!structurallyValid(d)) { rejected+=d.id; continue }

            when(d.kind) {
                DeltaKind.ADD -> d.effect?.let { effects[it.id]=it }
                DeltaKind.REMOVE -> {
                    if(effects.remove(requireNotNull(d.targetEffectId))==null) { rejected+=d.id; continue }
                }
                DeltaKind.REPLACE -> {
                    if(d.targetEffectId !in effects) { rejected+=d.id; continue }
                    effects.remove(d.targetEffectId)
                    requireNotNull(d.effect).let { effects[it.id]=it }
                }
            }
            applied+=d.id
        }
        return Projection(effects.values.toList(),applied,rejected)
    }
}
