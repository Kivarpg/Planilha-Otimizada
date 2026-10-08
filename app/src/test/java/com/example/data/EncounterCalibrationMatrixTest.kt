package com.example.data

import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterCalibrationMatrixTest {
    private fun vector(
        trees: Int,
        largest: Int,
        singles: Int,
        mainShare: Double,
        martial: Int = 0,
        fallback: Int = 0,
        budget: Long = 0,
        completionFallback: Long = 0
    ) = EncounterBuildObservability.CalibrationVector(
        totalCharms = 15,
        treesOpened = trees,
        deepestTreeSize = largest,
        singleCharmTrees = singles,
        mainTreeShare = mainShare,
        averageDepthProxy = 15.0 / trees,
        martialStylesOpened = if (martial > 0) 1 else 0,
        martialCharms = martial,
        fallbackSelections = fallback,
        exactSearchFound = 1,
        exactSearchProvenImpossible = 0,
        exactSearchBudgetExhausted = budget,
        completionFallbackActivated = completionFallback,
        largestTreeSelectionCount = largest,
        singleSelectionTrees = singles
    )

    @Test fun `resumo agrega matriz sem alterar selecao`() {
        val summary = EncounterCalibrationAnalyzer.summarize(listOf(
            vector(3, 8, 1, 0.6, martial = 5),
            vector(5, 5, 3, 0.4, fallback = 1, budget = 1, completionFallback = 1)
        ))
        assertEquals(2, summary.samples)
        assertEquals(4.0, summary.averageTreesOpened, 0.0001)
        assertEquals(6.5, summary.averageLargestTreeSelectionCount, 0.0001)
        assertEquals(2.0, summary.averageSingleSelectionTrees, 0.0001)
        assertEquals(0.5, summary.averageMainTreeShare, 0.0001)
        assertEquals(1, summary.totalFallbackSelections)
        assertEquals(1, summary.totalBudgetExhaustions)
        assertEquals(1L, summary.totalCompletionFallbackActivations)
    }

    @Test fun `matriz e ordenada deterministicamente e segmenta por tipo e rota`() {
        val lunar = EncounterCalibrationCase(
            TipoExaltadoEncontro.LUNAR, "MENTAL", "Inteligência", false, 20,
            vector(4, 6, 2, 0.5)
        )
        val solar = EncounterCalibrationCase(
            TipoExaltadoEncontro.SOLAR, "FISICO", "Briga", true, 10,
            vector(3, 8, 1, 0.7, martial = 5)
        )
        val matrix = EncounterCalibrationMatrixBuilder.build(listOf(lunar, solar))
        assertEquals(TipoExaltadoEncontro.LUNAR, matrix.cases.first().exaltedType)
        assertEquals(2, matrix.byExaltedType().size)
        assertEquals(2, matrix.byMartialRoute().size)
    }
}
