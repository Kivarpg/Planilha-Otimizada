package com.example.data

/**
 * Aplica uma transição estrutural atomicamente.
 * Se qualquer pré-condição não for TRUE, nenhuma mutação é persistida.
 */
internal object EncounterStateTransaction {
    data class Transaction(
        val id:String,
        val requirement:EncounterTriRequirement? = null,
        val mutations:List<EncounterProjectedFactState.Mutation>
    )
    data class Result(
        val committed:Boolean,
        val state:EncounterProjectedFactState.State,
        val truth:EncounterTruth
    )

    fun apply(
        initial:EncounterProjectedFactState.State,
        tx:Transaction,
        knownTrue:Set<String>,
        knownFalse:Set<String>
    ):Result {
        val truth=tx.requirement?.evaluate(knownTrue,knownFalse) ?: EncounterTruth.TRUE
        if(truth!=EncounterTruth.TRUE) return Result(false,initial,truth)
        return Result(true,EncounterProjectedFactState.apply(initial,tx.mutations),truth)
    }
}
