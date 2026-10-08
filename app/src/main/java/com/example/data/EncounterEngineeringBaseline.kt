package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.TipoExaltadoEncontro
import java.security.MessageDigest
import java.util.BitSet

/**
 * Baseline determinístico de engenharia da Aba 11.
 * Não participa de seleção, score ou legalidade: fornece casos canônicos,
 * fingerprints e orçamentos de trabalho para detectar regressões.
 */
internal object EncounterEngineeringBaseline {
    data class Case(
        val id: String,
        val exaltedType: TipoExaltadoEncontro,
        val archetype: ArquetipoEncontro,
        val focus: String? = null,
        val seed: Int,
        val expectsSorcery: Boolean = false,
        val expectsChimera: Boolean = false,
        val expectsMartialArts: Boolean = false
    )

    val cases: List<Case> = listOf(
        Case("solar-fisico", TipoExaltadoEncontro.SOLAR, ArquetipoEncontro.FISICO, seed = 1001),
        Case("dragon-fisico", TipoExaltadoEncontro.SANGUE_DE_DRAGAO, ArquetipoEncontro.FISICO, seed = 1101),
        Case("lunar-fisico", TipoExaltadoEncontro.LUNAR, ArquetipoEncontro.FISICO, seed = 2001),
        Case("lunar-social", TipoExaltadoEncontro.LUNAR, ArquetipoEncontro.SOCIAL, seed = 2002),
        Case("lunar-mental-feiticaria", TipoExaltadoEncontro.LUNAR, ArquetipoEncontro.MENTAL, focus = "Inteligência", seed = 2003, expectsSorcery = true),
        Case("lunar-quimera", TipoExaltadoEncontro.LUNAR, ArquetipoEncontro.FISICO, seed = 2004, expectsChimera = true),
        Case("lunar-artes-marciais", TipoExaltadoEncontro.LUNAR, ArquetipoEncontro.FISICO, focus = "Destreza", seed = 2005, expectsMartialArts = true),
        Case("lunar-pior-caso", TipoExaltadoEncontro.LUNAR, ArquetipoEncontro.MENTAL, seed = 2999)
    )

    /**
     * Golden versionado. Os hashes são preenchidos somente a partir de um
     * checkpoint compilado/aceito e depois permanecem imutáveis até uma
     * mudança mecânica deliberadamente aprovada.
     */
    val acceptedMechanicalFingerprints: Map<String, String> = emptyMap()

    fun expectedMechanicalFingerprint(caseId: String): String? =
        acceptedMechanicalFingerprints[caseId]

    /** Métricas de parede são observadas; estes limites determinísticos podem bloquear CI. */
    data class WorkBudget(
        val maxEligibilityChecks: Long,
        val maxStatesExpanded: Long,
        val maxCandidatesEvaluated: Long,
        val maxEligibilityCacheMisses: Long
    )

    fun withinBudget(snapshot: EncounterCharmRouteMetrics.Snapshot, budget: WorkBudget): Boolean =
        snapshot.eligibilityChecks <= budget.maxEligibilityChecks &&
            snapshot.statesExpanded <= budget.maxStatesExpanded &&
            snapshot.candidatesEvaluated <= budget.maxCandidatesEvaluated &&
            snapshot.eligibilityCacheMisses <= budget.maxEligibilityCacheMisses

    /**
     * Política relativa para comparar uma execução candidata com uma baseline conhecida.
     * Evita limites de milissegundos dependentes do runner e bloqueia explosões grandes
     * de trabalho determinístico. Os multiplicadores usam inteiros para manter o gate
     * reproduzível e sem arredondamento de ponto flutuante.
     */
    data class RelativeWorkBudget(
        val maxWorkMultiplier: Long = 2L,
        val maxEligibilityChecksMultiplier: Long = 2L,
        val maxStatesExpandedMultiplier: Long = 2L,
        val maxCandidatesEvaluatedMultiplier: Long = 2L,
        val maxEligibilityCacheMissesMultiplier: Long = 3L
    )

    fun withinRelativeBudget(
        baseline: EncounterCharmRouteMetrics.Snapshot,
        candidate: EncounterCharmRouteMetrics.Snapshot,
        budget: RelativeWorkBudget = RelativeWorkBudget()
    ): Boolean =
        withinMultiplier(candidate.deterministicWorkUnits, baseline.deterministicWorkUnits, budget.maxWorkMultiplier) &&
            withinMultiplier(candidate.eligibilityChecks, baseline.eligibilityChecks, budget.maxEligibilityChecksMultiplier) &&
            withinMultiplier(candidate.statesExpanded, baseline.statesExpanded, budget.maxStatesExpandedMultiplier) &&
            withinMultiplier(candidate.candidatesEvaluated, baseline.candidatesEvaluated, budget.maxCandidatesEvaluatedMultiplier) &&
            withinMultiplier(candidate.eligibilityCacheMisses, baseline.eligibilityCacheMisses, budget.maxEligibilityCacheMissesMultiplier)

    private fun withinMultiplier(candidate: Long, baseline: Long, multiplier: Long): Boolean {
        require(multiplier >= 1L) { "Performance multiplier must be at least 1" }
        if (baseline == 0L) return candidate == 0L
        return candidate <= baseline * multiplier
    }

    /** Snapshot mecânico estável: exclui UUID e ordem incidental de Map/Set. */
    fun mechanicalFingerprint(parts: Iterable<String>): String {
        val canonical = parts.map(String::trim).sorted().joinToString("\u001f")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /** Estado compacto para migração protegida do hot path. */
    internal class CompactSelectionIndex(ids: Collection<PreparedEncounterCatalog.StableContentId>) {
        private val orderedIds = ids.distinct().sortedBy { it.value }
        private val ordinalById = orderedIds.withIndex().associate { it.value to it.index }

        fun ordinalOf(id: PreparedEncounterCatalog.StableContentId): Int? = ordinalById[id]
        fun encode(selected: Collection<PreparedEncounterCatalog.StableContentId>): BitSet = BitSet(orderedIds.size).also { bits ->
            selected.forEach { id -> ordinalById[id]?.let(bits::set) }
        }
        fun decode(bits: BitSet): Set<PreparedEncounterCatalog.StableContentId> = buildSet {
            var i = bits.nextSetBit(0)
            while (i >= 0) {
                add(orderedIds[i])
                i = bits.nextSetBit(i + 1)
            }
        }
        val size: Int get() = orderedIds.size
    }
}
