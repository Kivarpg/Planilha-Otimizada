package com.example.data

import com.example.model.HistoricoXpBatch
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro

/**
 * Abstração da expansão de um único clique de +XP.
 *
 * O roadmap conhece apenas este contrato; as implementações concretas dos
 * geradores permanecem fora dele. Isso reduz o acoplamento sem alterar as
 * regras de compra de XP existentes.
 */
data class ExpansionResult(
    val npcResultante: NpcEncontro,
    val batchAplicado: HistoricoXpBatch
)

fun interface EncounterXpExpander {
    fun expand(npc: NpcEncontro): ExpansionResult
}

object EncounterXpExpanderFactory {
    fun from(
        tipo: TipoExaltadoEncontro,
        solarCatalogo: List<EncantoSolarDefinition>,
        dragonCatalogo: List<EncantoSangueDeDragaoDefinition>,
        lunarCatalogo: List<EncantoLunarDefinition>
    ): EncounterXpExpander = when (tipo) {
        TipoExaltadoEncontro.SOLAR -> EncounterXpExpander { npc ->
            SolarEncounterGenerator.expandirEncantosPorExperienciaComBatch(npc, solarCatalogo)
        }
        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterXpExpander { npc ->
            DragonBloodedEncounterGenerator.expandirEncantosPorExperienciaSangueDeDragaoComBatch(npc, dragonCatalogo)
        }
        TipoExaltadoEncontro.LUNAR -> EncounterXpExpander { npc ->
            EncounterExperienceService.expandLunarWithBatch(
                npc,
                lunarCatalogo,
                EncounterRulePolicy.lunarAttributePriorityFor(npc)
            )
        }
    }
}
