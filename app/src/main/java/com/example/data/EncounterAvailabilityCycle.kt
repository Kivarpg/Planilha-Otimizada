package com.example.data

/**
 * Disponibilidade limitada/resetável sem inferir frequência de uso.
 */
internal object EncounterAvailabilityCycle {
    enum class UseLimit { UNLIMITED, ONCE_PER_SCENE, ONCE_PER_FIGHT, ONCE_PER_DAY, OTHER, UNKNOWN }

    data class Availability(
        val id:String,
        val limit:UseLimit,
        val resetRequirement:EncounterTriRequirement? = null
    )

    data class Assessment(
        val reusableInPrinciple:Boolean?,
        val bounded:Boolean,
        val reason:String
    )

    fun assess(
        availability:Availability,
        knownTrue:Set<String>,
        knownFalse:Set<String>
    ):Assessment {
        if(availability.limit==UseLimit.UNLIMITED)
            return Assessment(true,false,"Sem limite estrutural declarado.")
        val reset=availability.resetRequirement
            ?: return Assessment(false,true,"Uso limitado sem reset representado.")
        return when(reset.evaluate(knownTrue,knownFalse)) {
            EncounterTruth.TRUE ->
                Assessment(true,true,"Reset demonstrado em princípio; frequência não inferida.")
            EncounterTruth.FALSE ->
                Assessment(false,true,"Reset não disponível no estado conhecido.")
            EncounterTruth.UNKNOWN ->
                Assessment(null,true,"Reset incerto; não promover disponibilidade.")
        }
    }
}
