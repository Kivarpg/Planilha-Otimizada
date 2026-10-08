package com.example.data

import com.example.model.TipoExaltadoEncontro

/** Fonte única dos marcos de Essência usados tanto pelo gerador quanto pelo roadmap. */
object EncounterExperienceProgressionRules {
    data class Milestone(val xpRequired: Int, val essence: Int)

    fun milestones(tipo: TipoExaltadoEncontro): List<Milestone> = when (tipo) {
        TipoExaltadoEncontro.SOLAR, TipoExaltadoEncontro.LUNAR -> listOf(
            Milestone(0, 1),
            Milestone(50, 2),
            Milestone(125, 3),
            Milestone(200, 4),
            Milestone(300, 5)
        )
        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> listOf(
            Milestone(0, 2),
            Milestone(75, 3),
            Milestone(250, 4),
            Milestone(325, 5)
        )
    }

    fun essenceForXp(tipo: TipoExaltadoEncontro, xpGastoTotal: Int): Int =
        milestones(tipo).lastOrNull { xpGastoTotal >= it.xpRequired }?.essence ?: milestones(tipo).first().essence

    fun nextMilestone(tipo: TipoExaltadoEncontro, xpGastoTotal: Int): Int? =
        milestones(tipo).firstOrNull { it.xpRequired > xpGastoTotal }?.xpRequired
}
