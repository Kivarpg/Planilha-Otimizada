package com.example.data

import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro

/**
 * Avaliação canônica e pós-construção do NPC completo da Aba 11.
 *
 * Não altera a ficha e não participa da legalidade. Consolida, em um único
 * lugar, sinais que antes ficavam espalhados entre auditoria, foco, Encantos,
 * combate e equipamento. Isso permite comparar builds legais sem duplicar
 * regras dos geradores.
 */
internal object EncounterFinalBuildQuality {
    data class Score(
        val valid: Boolean,
        val total: Int,
        val focus: Int,
        val charmCoherence: Int,
        val offense: Int,
        val defense: Int,
        val equipment: Int,
        val validationErrors: Int,
        val validationWarnings: Int
    )

    /**
     * Ordem segura para eventual comparação futura entre builds completas.
     * Legalidade é um filtro duro: nenhuma soma de qualidade pode colocar uma
     * build inválida acima de uma válida.
     */
    val comparator: Comparator<Score> =
        compareBy<Score> { it.valid }
            .thenByDescending { it.validationErrors }
            .thenBy { it.total }

    fun evaluate(
        npc: NpcEncontro,
        catalog: PreparedEncounterCatalog,
        charmReport: EncounterBuildQualityReport? = EncounterBuildObservability.snapshot().report
    ): Score {
        val audit = EncounterNpcAuditor.audit(npc, catalog)
        val focus = scoreFocus(npc)
        val coherence = scoreCharmCoherence(npc, charmReport)
        val offense = scoreOffense(npc)
        val defense = scoreDefense(npc)
        val equipment = scoreEquipment(npc)

        // Erros tornam a build inadequada para comparação como candidata legal.
        // Warnings reduzem levemente a qualidade, sem competir com legalidade.
        val penalty = audit.errors.size * 100 + audit.warnings.size * 2
        return Score(
            valid = audit.isValid,
            total = focus + coherence + offense + defense + equipment - penalty,
            focus = focus,
            charmCoherence = coherence,
            offense = offense,
            defense = defense,
            equipment = equipment,
            validationErrors = audit.errors.size,
            validationWarnings = audit.warnings.size
        )
    }

    private fun scoreFocus(npc: NpcEncontro): Int {
        val foco = npc.focoProgressaoExplicito?.takeIf { it.isNotBlank() } ?: return 0
        return when (npc.tipoExaltado) {
            TipoExaltadoEncontro.SOLAR,
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> {
                val nivel = npc.abilities[foco] ?: 0
                val charms = npc.charms.count { it.habilidadeVinculada.equals(foco, ignoreCase = true) }
                (nivel * 2 + charms * 3).coerceAtMost(30)
            }
            TipoExaltadoEncontro.LUNAR -> {
                val nivel = npc.attributes[foco] ?: 0
                val charms = npc.charms.count { it.habilidadeVinculada.equals(foco, ignoreCase = true) }
                (nivel * 2 + charms * 3).coerceAtMost(30)
            }
        }
    }

    private fun scoreCharmCoherence(
        npc: NpcEncontro,
        report: EncounterBuildQualityReport?
    ): Int {
        if (npc.charms.isEmpty()) return 0
        if (report == null) {
            val largest = npc.charms.groupingBy { it.habilidadeVinculada.lowercase() }
                .eachCount().values.maxOrNull() ?: 0
            return (largest * 2).coerceAtMost(24)
        }
        val concentration = report.largestTreeSelectionCount * 2
        val isolatedPenalty = report.singleSelectionTrees
        val fallbackPenalty = report.fallbackSelections * 2 + report.budgetExhaustions * 2
        return (concentration - isolatedPenalty - fallbackPenalty).coerceIn(0, 24)
    }

    private fun scoreOffense(npc: NpcEncontro): Int {
        val primary = npc.acaoPrincipal.coerceAtLeast(0)
        val decisive = npc.acaoDecisiva.coerceAtLeast(0)
        val joinBattle = npc.juntarABatalha.coerceAtLeast(0)
        return (primary + decisive / 2 + joinBattle / 2).coerceAtMost(24)
    }

    private fun scoreDefense(npc: NpcEncontro): Int {
        val parry = npc.defesaPrimaria ?: 0
        val bestPassive = maxOf(parry, npc.esquiva)
        return (bestPassive * 3 + npc.absorcao + npc.dureza).coerceAtMost(24)
    }

    private fun scoreEquipment(npc: NpcEncontro): Int {
        var score = 0
        if (npc.arma != null) score += 4
        if (npc.armadura != null) score += 4
        return score
    }
}
