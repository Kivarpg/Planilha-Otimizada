package com.example.data

internal object EncounterRouteState {
    data class Horizon(val xpBudget:Int?,val maxEssence:Int?,val maxPurchases:Int?)
    data class Step(
        val id:String,val xpCost:Int,val minEssence:Int,
        val structuralFacts:Set<String> = emptySet()
    )
    data class Evaluation(
        val reachable:Boolean,val marginalXp:Int?,val newPurchases:Int,val reason:String
    )

    fun evaluate(
        alreadyOwned:Set<String>, path:List<Step>, horizon:Horizon
    ):Evaluation {
        if(horizon.xpBudget!=null && horizon.xpBudget<0)
            return Evaluation(false,null,0,"Horizonte de XP inválido.")
        if(horizon.maxEssence!=null && horizon.maxEssence<0)
            return Evaluation(false,null,0,"Horizonte de Essência inválido.")
        if(horizon.maxPurchases!=null && horizon.maxPurchases<0)
            return Evaluation(false,null,0,"Horizonte de aquisições inválido.")
        if(path.any { it.id.isBlank() || it.xpCost<0 || it.minEssence<0 })
            return Evaluation(false,null,0,"Passo de rota estruturalmente inválido.")

        val duplicateNew=path.filter { it.id !in alreadyOwned }.groupingBy { it.id }
            .eachCount().filterValues { it>1 }.keys
        if(duplicateNew.isNotEmpty())
            return Evaluation(false,null,0,"Rota contém aquisição duplicada sem semântica de recompra.")

        val new=path.filter { it.id !in alreadyOwned }
        val xp=try { new.fold(0) { acc,s -> Math.addExact(acc,s.xpCost) } }
            catch (_:ArithmeticException) {
                return Evaluation(false,null,new.size,"Overflow no custo da rota.")
            }
        if(horizon.xpBudget!=null && xp>horizon.xpBudget)
            return Evaluation(false,xp,new.size,"Fora do horizonte de XP.")
        if(horizon.maxEssence!=null && new.any { it.minEssence>horizon.maxEssence })
            return Evaluation(false,xp,new.size,"Exige Essência além do horizonte.")
        if(horizon.maxPurchases!=null && new.size>horizon.maxPurchases)
            return Evaluation(false,xp,new.size,"Rota além do horizonte de aquisições.")
        return Evaluation(true,xp,new.size,"Rota alcançável no horizonte informado.")
    }

    data class Fingerprint(
        val owned:Set<String>,val facts:Set<String>,val commitments:Map<String,String>
    )
}
