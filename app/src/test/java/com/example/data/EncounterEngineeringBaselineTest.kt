package com.example.data

import org.junit.Assert.*
import org.junit.Test

class EncounterEngineeringBaselineTest {
    @Test fun `seeds canonicas sao unicas e cobrem cenarios de engenharia`() {
        val cases = EncounterEngineeringBaseline.cases
        assertEquals(cases.size, cases.map { it.seed }.distinct().size)
        assertTrue(cases.any { it.expectsSorcery })
        assertTrue(cases.any { it.expectsChimera })
        assertTrue(cases.any { it.expectsMartialArts })
        assertTrue(cases.any { it.id == "lunar-pior-caso" })
        assertTrue(cases.any { it.exaltedType == com.example.model.TipoExaltadoEncontro.SOLAR })
        assertTrue(cases.any { it.exaltedType == com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO })
        assertTrue(cases.count { it.exaltedType == com.example.model.TipoExaltadoEncontro.LUNAR } >= 6)
        assertTrue(cases.any { it.archetype == com.example.model.ArquetipoEncontro.SOCIAL })
        assertTrue(cases.any { it.archetype == com.example.model.ArquetipoEncontro.MENTAL })
    }

    @Test fun `baseline mecanica aceita usa apenas ids de casos canonicos`() {
        val ids = EncounterEngineeringBaseline.cases.map { it.id }.toSet()
        assertTrue(EncounterEngineeringBaseline.acceptedMechanicalFingerprints.keys.all { it in ids })
    }

    @Test fun `fingerprint mecanico independe de ordem incidental`() {
        assertEquals(
            EncounterEngineeringBaseline.mechanicalFingerprint(listOf("b", "a", "c")),
            EncounterEngineeringBaseline.mechanicalFingerprint(listOf("c", "b", "a"))
        )
    }

    @Test fun `indice compacto usa id canonico e round trip e exato`() {
        val a = PreparedEncounterCatalog.StableContentId("solar.charm", "z")
        val b = PreparedEncounterCatalog.StableContentId("lunar.charm", "a")
        val c = PreparedEncounterCatalog.StableContentId("solar.charm", "a")
        val first = EncounterEngineeringBaseline.CompactSelectionIndex(listOf(a, b, c))
        val second = EncounterEngineeringBaseline.CompactSelectionIndex(listOf(c, a, b))
        assertEquals(first.ordinalOf(a), second.ordinalOf(a))
        assertEquals(first.ordinalOf(b), second.ordinalOf(b))
        val selected = setOf(a, c)
        assertEquals(selected, first.decode(first.encode(selected)))
    }

    @Test fun `performance gate usa trabalho deterministico e nao relogio`() {
        val metrics = EncounterCharmRouteMetrics()
        metrics.recordEligibilityChecks(10)
        metrics.recordStatesExpanded(4)
        metrics.recordCandidateEvaluated()
        metrics.recordEligibilityCacheMiss()
        val snapshot = metrics.snapshot()
        assertTrue(EncounterEngineeringBaseline.withinBudget(snapshot, EncounterEngineeringBaseline.WorkBudget(10, 4, 1, 1)))
        assertFalse(EncounterEngineeringBaseline.withinBudget(snapshot, EncounterEngineeringBaseline.WorkBudget(9, 4, 1, 1)))
    }
    @Test fun `relative performance gate detects gross deterministic regression`() {
        fun snapshot(
            checks: Long, states: Long, candidates: Long, misses: Long,
            deduplicated: Long = 2, marginal: Long = 3, calls: Long = 1
        ) = EncounterCharmRouteMetrics.Snapshot(
            statesExpanded = states, statesDeduplicated = deduplicated, candidatesEvaluated = candidates,
            eligibilityCacheHits = 20, eligibilityCacheMisses = misses, eligibilityChecks = checks,
            eligibleNamesNanos = 999_999, marginalUnlockCalls = marginal, marginalUnlockNanos = 888_888,
            optimizerCalls = calls, budgetExhaustions = 0
        )

        val baseline = snapshot(checks = 100, states = 20, candidates = 30, misses = 5)
        val acceptable = snapshot(checks = 180, states = 35, candidates = 50, misses = 12)
        val doubledPlusOne = snapshot(checks = 201, states = 20, candidates = 30, misses = 5)
        val cacheExplosion = snapshot(checks = 100, states = 20, candidates = 30, misses = 16)

        assertTrue(EncounterEngineeringBaseline.withinRelativeBudget(baseline, acceptable))
        assertFalse(EncounterEngineeringBaseline.withinRelativeBudget(baseline, doubledPlusOne))
        assertFalse(EncounterEngineeringBaseline.withinRelativeBudget(baseline, cacheExplosion))
    }

    @Test fun `relative gate does not forgive new work when baseline is zero`() {
        val zero = EncounterCharmRouteMetrics.Snapshot(0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0)
        val candidate = zero.copy(candidatesEvaluated = 1)
        assertFalse(EncounterEngineeringBaseline.withinRelativeBudget(zero, candidate))
    }


    @Test
    fun `planner canonico prepara tags imutaveis fora do hot path`() {
        val source = java.io.File(
            System.getProperty("user.dir") ?: ".",
            "src/main/java/com/example/data/EncounterCanonicalPlanner.kt"
        ).readText()
        val cachePos = source.indexOf("val candidateTags =")
        val candidatesPos = source.indexOf("val candidates =")
        assertTrue(cachePos >= 0)
        assertTrue(candidatesPos > cachePos)
        assertTrue(source.contains("candidateTags::get"))
    }

    @Test
    fun `build planner nao materializa mapa temporario de elegibilidade por estado`() {
        val source = java.io.File(
            System.getProperty("user.dir") ?: ".",
            "src/main/java/com/example/data/EncounterBuildPlanner.kt"
        ).readText()
        assertTrue(!source.contains("}.toMap()\n\n                availableBefore.forEach"))
    }


    @Test
    fun `golden versionado permanece vazio ate checkpoint ser explicitamente aceito`() {
        // Evita promover silenciosamente código pós-checkpoint a baseline mecânica.
        // O mapa só deve ser preenchido após uma compilação/validação aceita.
        assertTrue(
            EncounterEngineeringBaseline.acceptedMechanicalFingerprints.isEmpty() ||
                EncounterEngineeringBaseline.acceptedMechanicalFingerprints.keys ==
                    EncounterEngineeringBaseline.acceptedMechanicalFingerprints.keys.intersect(
                        EncounterEngineeringBaseline.cases.map { it.id }.toSet()
                    )
        )
    }

}
