package com.example.data

/**
 * Validação de esquema para impedir que dados estruturalmente impossíveis
 * cheguem ao scorer.
 */
internal object EncounterResourceValidation {
    data class Issue(val id:String,val reason:String)

    fun validate(profile:EncounterResourceBudget.Profile):List<Issue> {
        val issues=mutableListOf<Issue>()
        for((key,cap) in profile.capacities) {
            if(key!=cap.resource) issues+=Issue("capacity:$key","Chave e resource da capacidade divergem.")
            if(cap.available!=null && cap.available<0) issues+=Issue("capacity:$key","Capacidade negativa.")
            if(cap.reservable!=null && cap.reservable<0) issues+=Issue("capacity:$key","Capacidade reservável negativa.")
            if(cap.available!=null && cap.reservable!=null && cap.reservable>cap.available)
                issues+=Issue("capacity:$key","Reservável excede disponível.")
        }
        for(f in profile.flows) {
            if(f.amount!=null && f.amount<0) issues+=Issue(f.id,"Quantidade negativa.")
            if(f.confidence !in 0.0..1.0) issues+=Issue(f.id,"Confiança fora de 0..1.")
            if(f.kind==EncounterResourceBudget.FlowKind.CONVERT && f.targetResource==null)
                issues+=Issue(f.id,"Conversão sem recurso de destino.")
            if(f.kind!=EncounterResourceBudget.FlowKind.CONVERT && f.targetResource!=null)
                issues+=Issue(f.id,"Recurso de destino em fluxo que não é conversão.")
        }
        return issues
    }
}
