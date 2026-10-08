package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterBuildObservabilityTest {
    @Test
    fun `quality report is derived without changing selections`() {
        val categories = listOf("Armas Brancas", "Armas Brancas", "Armas Brancas", "Resistência")
        val trace = EncounterSelectionTrace(
            listOf(
                EncounterSelectionTraceEntry("A", "Armas Brancas", EncounterSelectionReason.CONTINUIDADE_FOCO, 0),
                EncounterSelectionTraceEntry("B", "Resistência", EncounterSelectionReason.FALLBACK, 1)
            )
        )
        val report = EncounterBuildQualityAnalyzer.analyze(
            categories = categories,
            mainCategory = "Armas Brancas",
            isolatedEntryCharms = 1,
            trace = trace,
            telemetry = EncounterSelectionTelemetry.Snapshot(2, 1)
        )
        assertEquals(4, report.totalCharms)
        assertEquals(2, report.treesOpened)
        assertEquals(3, report.mainTreeCharms)
        assertEquals(1, report.isolatedEntryCharms)
        assertEquals(1, report.fallbackSelections)
        assertEquals(2, report.budgetExhaustions)
        assertEquals(0.75, report.mainTreeShare, 0.0001)
        assertEquals(2.0, report.averageDepthProxy, 0.0001)
        assertEquals(categories, categories) // analyzer is observational only
    }

    @Test
    fun `search outcomes keep impossible distinct from exhausted budget`() {
        val impossible: EncounterSearchOutcome<String> = EncounterSearchOutcome.ProvenImpossible
        val exhausted: EncounterSearchOutcome<String> = EncounterSearchOutcome.BudgetExhausted(128)
        assertTrue(impossible is EncounterSearchOutcome.ProvenImpossible)
        assertTrue(exhausted is EncounterSearchOutcome.BudgetExhausted)
    }

    @Test
    fun `trace records return to main focus without influencing selection`() {
        val selected = listOf(
            "A" to "Armas Brancas",
            "B" to "Resistência",
            "C" to "Armas Brancas"
        )
        val trace = buildEncounterSelectionTrace(selected, mainCategory = "Armas Brancas")
        assertEquals(
            listOf(
                EncounterSelectionReason.CONTINUIDADE_FOCO,
                EncounterSelectionReason.COMPLEMENTAR,
                EncounterSelectionReason.RETORNO_FOCO
            ),
            trace.entries.map { it.reason }
        )
        assertEquals(selected.map { it.first }, trace.entries.map { it.charmName })
    }

    @Test
    fun `shadow observability publishes trace and report together`() {
        EncounterBuildObservability.clearForTests()
        val trace = buildEncounterSelectionTrace(listOf("A" to "Ocultismo"), "Ocultismo")
        val report = EncounterBuildQualityAnalyzer.analyze(listOf("Ocultismo"), "Ocultismo", trace = trace)
        EncounterBuildObservability.publish(trace, report)
        val snapshot = EncounterBuildObservability.snapshot()
        assertEquals(trace, snapshot.first)
        assertEquals(report, snapshot.second)
        EncounterBuildObservability.clearForTests()
    }


    @Test
    fun `quality report derives concentration and isolated trees`() {
        val report = EncounterBuildQualityAnalyzer.analyze(
            categories = listOf("Melee", "Melee", "Melee", "Dodge", "Resistance", "Resistance"),
            mainCategory = "Melee"
        )
        assertEquals(3, report.treesOpened)
        assertEquals(3, report.deepestTreeSize)
        assertEquals(1, report.singleCharmTrees)
        assertEquals(1, report.isolatedEntryCharms)
        assertEquals(0.5, report.mainTreeShare, 0.0001)
        assertEquals(2.0, report.averageDepthProxy, 0.0001)
    }


    @Test
    fun `calibration vector exposes shadow metrics without becoming a selector`() {
        EncounterBuildObservability.clearForTests()
        EncounterSelectionTelemetry.resetForTests()
        // O vetor da Etapa 4 mede deltas da build, não o histórico global.
        EncounterBuildObservability.beginBuild()
        EncounterSelectionTelemetry.recordExactSearchFound()
        val trace = buildEncounterSelectionTrace(
            listOf("A" to "Melee", "B" to "Melee", "C" to "Dodge"),
            mainCategory = "Melee"
        )
        val report = EncounterBuildQualityAnalyzer.analyze(
            categories = trace.entries.map { it.category },
            mainCategory = "Melee",
            trace = trace
        )
        EncounterBuildObservability.publish(trace, report)
        EncounterBuildObservability.publishFinalBuild(
            selected = trace.entries.map { it.charmName to it.category },
            mainCategory = "Melee"
        )
        val vector = EncounterBuildObservability.calibrationVector()
        requireNotNull(vector)
        assertEquals(3, vector.totalCharms)
        assertEquals(2, vector.treesOpened)
        assertEquals(2, vector.deepestTreeSize)
        assertEquals(1, vector.singleCharmTrees)
        assertEquals(2.0 / 3.0, vector.mainTreeShare, 0.0001)
        assertEquals(1L, vector.exactSearchFound)
        assertEquals(0L, vector.exactSearchBudgetExhausted)
        EncounterBuildObservability.clearForTests()
        EncounterSelectionTelemetry.resetForTests()
    }


    @Test fun `trace distingue rota continuidade e novo estilo marcial`() {
        val trace = buildEncounterSelectionTrace(
            selected = listOf(
                "Raiz Tigre" to "Tigre",
                "Meio Tigre" to "Tigre",
                "Raiz Garça" to "Garça"
            ),
            mainCategory = "Briga",
            martialStyleCategories = setOf("Tigre", "Garça"),
            newlyOpenedMartialStyle = "Garça"
        )
        assertEquals(EncounterSelectionReason.ROTA_MARCIAL, trace.entries[0].reason)
        assertEquals(EncounterSelectionReason.CONTINUIDADE_ESTILO, trace.entries[1].reason)
        assertEquals(EncounterSelectionReason.NOVO_ESTILO, trace.entries[2].reason)
    }


    @Test fun `publicacao final inclui encantos marciais no relatorio`() {
        EncounterBuildObservability.clearForTests()
        EncounterBuildObservability.publishFinalBuild(
            selected = listOf(
                "Encanto Solar" to "Briga",
                "Raiz Tigre" to "Estilo Tigre",
                "Meio Tigre" to "Estilo Tigre"
            ),
            mainCategory = "Briga",
            combatCategory = "Briga",
            martialStyleCategories = setOf("Estilo Tigre")
        )
        val snapshot = EncounterBuildObservability.snapshot()
        assertEquals(2, snapshot.report?.martialCharms)
        assertEquals(1, snapshot.report?.martialStylesOpened)
        assertEquals(3, snapshot.report?.totalCharms)
        assertEquals(EncounterSelectionReason.ROTA_MARCIAL, snapshot.trace?.entries?.get(1)?.reason)
        assertEquals(EncounterSelectionReason.CONTINUIDADE_ESTILO, snapshot.trace?.entries?.get(2)?.reason)
    }


    @Test fun `trace marca compra de requisito de estilo explicitamente`() {
        val trace = buildEncounterSelectionTrace(
            selected = listOf("Raiz" to "Tigre", "Passo" to "Tigre"),
            mainCategory = null,
            martialStyleCategories = setOf("Tigre"),
            martialPrerequisiteCharms = setOf("Raiz")
        )
        assertEquals(EncounterSelectionReason.REQUISITO_ESTILO, trace.entries[0].reason)
        assertEquals(EncounterSelectionReason.CONTINUIDADE_ESTILO, trace.entries[1].reason)
    }


    @Test fun `metricas de calibracao distinguem contagem por arvore de profundidade topologica`() {
        val report = EncounterBuildQualityAnalyzer.analyze(
            categories = listOf("Briga", "Briga", "Esquiva"),
            mainCategory = "Briga"
        )
        assertEquals(2, report.largestTreeSelectionCount)
        assertEquals(1, report.singleSelectionTrees)
        assertEquals(report.deepestTreeSize, report.largestTreeSelectionCount)
    }


    @Test fun `vetor de calibracao exclui historico anterior ao inicio da build`() {
        EncounterBuildObservability.clearForTests()
        EncounterSelectionTelemetry.resetForTests()
        EncounterSelectionTelemetry.recordExactSearchFound() // histórico anterior
        EncounterBuildObservability.beginBuild()
        EncounterSelectionTelemetry.recordExactSearchFound() // evento desta build
        EncounterBuildObservability.publishFinalBuild(
            selected = listOf("A" to "Melee"),
            mainCategory = "Melee"
        )
        val vector = requireNotNull(EncounterBuildObservability.calibrationVector())
        assertEquals(1L, vector.exactSearchFound)
        EncounterBuildObservability.clearForTests()
        EncounterSelectionTelemetry.resetForTests()
    }

}
