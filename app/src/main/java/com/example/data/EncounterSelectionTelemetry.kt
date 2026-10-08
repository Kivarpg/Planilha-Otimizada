package com.example.data

import java.util.concurrent.atomic.AtomicLong

/**
 * Observabilidade da seleção de Encantos.
 *
 * Estes contadores NÃO participam de score, legalidade, desempate ou seleção.
 * Servem apenas para medir quanto o gerador depende de recuperação/fallback e
 * quantas vezes uma busca limitada termina por orçamento em vez de provar que
 * não há solução. Isso permite calibrar heurísticas com evidência posteriormente.
 */
object EncounterSelectionTelemetry {
    private val exactSearchBudgetExhausted = AtomicLong()
    private val completionFallbackActivated = AtomicLong()
    private val exactSearchFound = AtomicLong()
    private val exactSearchProvenImpossible = AtomicLong()

    data class Snapshot(
        val exactSearchBudgetExhausted: Long,
        val completionFallbackActivated: Long,
        val exactSearchFound: Long = 0,
        val exactSearchProvenImpossible: Long = 0,
    )

    internal fun recordExactSearchBudgetExhausted() {
        exactSearchBudgetExhausted.incrementAndGet()
    }

    internal fun recordCompletionFallbackActivated() {
        completionFallbackActivated.incrementAndGet()
    }

    internal fun recordExactSearchFound() {
        exactSearchFound.incrementAndGet()
    }

    internal fun recordExactSearchProvenImpossible() {
        exactSearchProvenImpossible.incrementAndGet()
    }

    fun snapshot(): Snapshot = Snapshot(
        exactSearchBudgetExhausted = exactSearchBudgetExhausted.get(),
        completionFallbackActivated = completionFallbackActivated.get(),
        exactSearchFound = exactSearchFound.get(),
        exactSearchProvenImpossible = exactSearchProvenImpossible.get(),
    )

    internal fun resetForTests() {
        exactSearchBudgetExhausted.set(0)
        completionFallbackActivated.set(0)
        exactSearchFound.set(0)
        exactSearchProvenImpossible.set(0)
    }
}
