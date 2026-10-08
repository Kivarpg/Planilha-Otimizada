package com.example.data

/**
 * Derivação por ponto fixo preservando a expressão lógica completa.
 * Recalcular das fontes-base elimina fatos órfãos e ciclos sem raiz.
 */
internal object EncounterDerivedFactResolver {
    data class Derivation(
        val fact:String,
        val requirement:EncounterRequirementExpression
    ) {
        constructor(fact:String, requiresAll:Set<String>) :
            this(fact,EncounterRequirementExpression.AllOf(
                requiresAll.map { EncounterRequirementExpression.Fact(it) }
            ))
    }

    fun stableFacts(
        baseFacts:Set<String>,
        derivations:Collection<Derivation>
    ):Set<String> {
        val facts=baseFacts.toMutableSet()
        var changed=true
        while(changed) {
            changed=false
            for(d in derivations) {
                if(d.requirement.isSatisfiedBy(facts) && d.fact !in facts) {
                    facts+=d.fact
                    changed=true
                }
            }
        }
        return facts
    }

    fun recomputeAfterRevocation(
        remainingBaseFacts:Set<String>,
        derivations:Collection<Derivation>
    ):Set<String> = stableFacts(remainingBaseFacts,derivations)
}
