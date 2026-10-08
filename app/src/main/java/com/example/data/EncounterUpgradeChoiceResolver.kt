package com.example.data

/**
 * Seleciona deltas de upgrade/recompra respeitando grupos exclusivos.
 * A projeção recebe apenas deltas pertencentes à configuração escolhida.
 */
internal object EncounterUpgradeChoiceResolver {
    data class ChoiceDelta(
        val delta:EncounterPowerProjection.Delta,
        val choiceGroup:String? = null,
        val choiceOption:String? = null
    )

    data class Selection(
        val deltas:List<EncounterPowerProjection.Delta>,
        val valid:Boolean,
        val reason:String
    )

    fun select(
        candidates:Collection<ChoiceDelta>,
        choices:Map<String,String>
    ):Selection {
        val selected=mutableListOf<EncounterPowerProjection.Delta>()
        for(c in candidates) {
            val group=c.choiceGroup
            if(group==null) { selected+=c.delta; continue }
            val option=c.choiceOption
                ?: return Selection(emptyList(),false,"Delta com grupo exclusivo sem opção.")
            val chosen=choices[group] ?: continue
            if(chosen==option) selected+=c.delta
        }
        return Selection(selected,true,"Deltas filtrados pela configuração.")
    }
}
