package com.example.data

/**
 * Compromissos estruturais. Não infere troca sem regra explícita.
 */
internal object EncounterCommitmentModel {
    enum class Scope { FIXED_BUILD, LOADOUT, SCENE, ROUND, ACTION, TRANSIENT }

    data class Commitment(val key:String,val value:String,val scope:Scope)
    data class TransitionRule(
        val key:String,
        val from:String?,
        val to:String,
        val allowed:Boolean,
        val minimumScope:Scope,
        val structuralCost:Int=0,
        val requires:EncounterRequirementExpression=EncounterRequirementExpression.Always
    )
    data class State(val commitments:Map<String,Commitment> = emptyMap()) {
        fun value(key:String):String?=commitments[key]?.value
    }
    data class TransitionResult(
        val allowed:Boolean,val state:State,val structuralCost:Int,val reason:String
    )

    private fun rank(scope:Scope)=when(scope) {
        Scope.FIXED_BUILD->0; Scope.LOADOUT->1; Scope.SCENE->2
        Scope.ROUND->3; Scope.ACTION->4; Scope.TRANSIENT->5
    }

    fun apply(
        state:State,
        desired:Commitment,
        rules:Collection<TransitionRule>,
        facts:Set<String>
    ):TransitionResult {
        val current=state.commitments[desired.key]
        if(current==null)
            return TransitionResult(true,State(state.commitments+(desired.key to desired)),0,
                "Novo compromisso sem conflito.")
        if(current.value==desired.value) {
            // Não permitir que repetir o mesmo valor enfraqueça silenciosamente o escopo.
            val effective=if(rank(current.scope)<=rank(desired.scope)) current else desired
            return TransitionResult(true,State(state.commitments+(desired.key to effective)),0,
                "Mesmo compromisso preservado no escopo mais restritivo.")
        }
        if(current.scope==Scope.FIXED_BUILD || desired.scope==Scope.FIXED_BUILD)
            return TransitionResult(false,state,0,"Conflito de compromisso FIXED_BUILD.")

        val applicable=rules.filter {
            it.key==desired.key && (it.from==null || it.from==current.value) &&
            it.to==desired.value && it.allowed && it.requires.isSatisfiedBy(facts)
        }
        if(applicable.isEmpty())
            return TransitionResult(false,state,0,"Nenhuma transição explícita autoriza a troca.")

        // minimumScope é uma restrição real: a configuração atual deve ser ao menos
        // tão mutável quanto a regra exige.
        val scoped=applicable.filter { rank(current.scope)>=rank(it.minimumScope) }
        if(scoped.isEmpty())
            return TransitionResult(false,state,0,"Escopo atual não permite esta transição.")

        // Regras aplicáveis conflitantes não são escolhidas por ordem da coleção.
        val signatures=scoped.map { Triple(it.from,it.structuralCost,it.minimumScope) }.distinct()
        if(signatures.size>1)
            return TransitionResult(false,state,0,"Transição ambígua: múltiplas regras aplicáveis.")

        val rule=scoped.single()
        require(rule.structuralCost>=0) { "structuralCost não pode ser negativo." }
        return TransitionResult(true,State(state.commitments+(desired.key to desired)),
            rule.structuralCost,"Transição explícita aplicada.")
    }
}
