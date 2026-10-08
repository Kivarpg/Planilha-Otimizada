package com.example.data

import com.example.model.NpcEncontro

/**
 * Ponto único e conservador para o pós-processamento de mutações da Aba 11.
 *
 * Nesta etapa centraliza somente o recálculo que já era obrigatório após
 * alterações de equipamento. Outras normalizações e validações serão
 * incorporadas gradualmente, protegidas por testes.
 */
object EncounterMutationPipeline {
    fun recalcular(npc: NpcEncontro): NpcEncontro {
        val armadura = npc.armadura
        val motes = EncounterMoteService.totais(
            npc.tipoExaltado, npc.essencia, npc.arma, armadura
        )
        val normalizado = npc.copy(
            absorcaoArmadura = armadura?.absorcao ?: 0,
            absorcao = npc.absorcaoNatural + (armadura?.absorcao ?: 0),
            dureza = armadura?.dureza ?: 0,
            motesPersonais = motes.pessoaisMax,
            motesPerifericos = motes.perifericosDisponiveis
        )
        return EncounterExperienceService.recalcularDerivados(normalizado)
    }
}
