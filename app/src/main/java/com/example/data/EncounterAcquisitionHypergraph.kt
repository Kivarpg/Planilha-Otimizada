package com.example.data

/**
 * Grafo de aquisição por hiperarcos lógicos.
 * Um requisito composto é uma única condição; seus átomos não viram
 * "unlocks" independentes para pontuação.
 */
internal object EncounterAcquisitionHypergraph {
    data class Node(
        val id:String,
        val requirement:EncounterRequirementExpression =
            EncounterRequirementExpression.Always,
        val repeatKey:String? = null,
        val maxPurchases:Int? = 1
    )

    data class State(
        val facts:Set<String>,
        val purchaseCounts:Map<String,Int> = emptyMap()
    )

    data class Eligibility(val eligible:Boolean,val reason:String)

    fun eligibility(node:Node,state:State):Eligibility {
        if(node.id.isBlank()) return Eligibility(false,"ID de aquisição vazio.")
        if(node.repeatKey!=null && node.repeatKey.isBlank())
            return Eligibility(false,"repeatKey vazio.")
        if(node.maxPurchases!=null && node.maxPurchases<=0)
            return Eligibility(false,"maxPurchases inválido.")
        if(state.purchaseCounts.values.any { it<0 })
            return Eligibility(false,"Contagem de aquisição negativa.")
        if(!node.requirement.isSatisfiedBy(state.facts))
            return Eligibility(false,"Expressão completa de aquisição não satisfeita.")
        val key=node.repeatKey ?: node.id
        val count=state.purchaseCounts[key] ?: 0
        val max=node.maxPurchases
        if(max!=null && count>=max)
            return Eligibility(false,"Limite de compras atingido.")
        return Eligibility(true,"Aquisição legal no estado fornecido.")
    }

    fun marginalUnlocks(
        candidateFact:String,
        before:Set<String>,
        nodes:Collection<Node>
    ):Set<String> {
        val after=before+candidateFact
        return nodes.filter {
            !it.requirement.isSatisfiedBy(before) &&
             it.requirement.isSatisfiedBy(after)
        }.map { it.id }.toSet()
    }
}
