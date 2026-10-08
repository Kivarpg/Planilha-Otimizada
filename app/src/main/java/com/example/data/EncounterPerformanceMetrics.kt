package com.example.data

import java.util.concurrent.atomic.AtomicLong

/**
 * Medições transitórias dos dois caminhos interativos mais caros da Aba 11.
 * Não são persistidas e não participam de nenhuma decisão de geração ou XP.
 * Servem como baseline para a fase de profiling/otimização.
 */
class EncounterPerformanceMetrics {
    private val generationCount = AtomicLong()
    private val generationNanos = AtomicLong()
    private val generationMaxNanos = AtomicLong()
    private val generationCoreNanos = AtomicLong()
    private val generationQualityNanos = AtomicLong()
    private val generationSamples = java.util.concurrent.ConcurrentLinkedQueue<Long>()
    private val xpCount = AtomicLong()
    private val xpNanos = AtomicLong()
    private val xpMaxNanos = AtomicLong()
    private val xpSamples = java.util.concurrent.ConcurrentLinkedQueue<Long>()

    data class Snapshot(
        val generationCount: Long,
        val generationNanos: Long,
        val generationMaxNanos: Long,
        val generationP50Nanos: Long,
        val generationP95Nanos: Long,
        val generationP99Nanos: Long,
        val generationCoreNanos: Long,
        val generationQualityNanos: Long,
        val xpCount: Long,
        val xpNanos: Long,
        val xpMaxNanos: Long,
        val xpP50Nanos: Long,
        val xpP95Nanos: Long,
        val xpP99Nanos: Long
    ) {
        val generationAverageNanos: Long
            get() = if (generationCount == 0L) 0L else generationNanos / generationCount
        val xpAverageNanos: Long
            get() = if (xpCount == 0L) 0L else xpNanos / xpCount
    }

    internal fun recordGeneration(nanos: Long, coreNanos: Long = nanos, qualityNanos: Long = 0L) {
        val safe = nanos.coerceAtLeast(0L)
        recordSample(generationSamples, safe)
        record(safe, generationCount, generationNanos, generationMaxNanos)
        generationCoreNanos.addAndGet(coreNanos.coerceAtLeast(0L))
        generationQualityNanos.addAndGet(qualityNanos.coerceAtLeast(0L))
    }
    internal fun recordXp(nanos: Long) {
        val safe = nanos.coerceAtLeast(0L)
        recordSample(xpSamples, safe)
        record(safe, xpCount, xpNanos, xpMaxNanos)
    }

    /**
     * Percentis são diagnósticos de latência recente, não histórico persistido.
     * Uma janela limitada impede crescimento indefinido de memória e mantém o
     * custo de snapshot previsível mesmo após milhares de gerações/cliques.
     * Contagem, soma e máximo continuam cobrindo a sessão inteira.
     */
    private fun recordSample(
        samples: java.util.concurrent.ConcurrentLinkedQueue<Long>,
        nanos: Long
    ) {
        samples.add(nanos)
        while (samples.size > MAX_PERCENTILE_SAMPLES) {
            samples.poll()
        }
    }

    private fun percentile(sortedSamples: List<Long>, percentile: Int): Long {
        if (sortedSamples.isEmpty()) return 0L
        // nearest-rank: ceil(p * N) - 1. Para [10, 30], p50 = 10.
        val rank = ((percentile * sortedSamples.size) + 99) / 100
        val index = rank - 1
        return sortedSamples[index.coerceIn(0, sortedSamples.lastIndex)]
    }

    private fun record(nanos: Long, count: AtomicLong, total: AtomicLong, max: AtomicLong) {
        val safe = nanos.coerceAtLeast(0L)
        count.incrementAndGet()
        total.addAndGet(safe)
        max.accumulateAndGet(safe) { atual, novo -> maxOf(atual, novo) }
    }

    fun snapshot(): Snapshot {
        val generationSorted = generationSamples.sorted()
        val xpSorted = xpSamples.sorted()
        return Snapshot(
            generationCount = generationCount.get(),
            generationNanos = generationNanos.get(),
            generationMaxNanos = generationMaxNanos.get(),
            generationP50Nanos = percentile(generationSorted, 50),
            generationP95Nanos = percentile(generationSorted, 95),
            generationP99Nanos = percentile(generationSorted, 99),
            generationCoreNanos = generationCoreNanos.get(),
            generationQualityNanos = generationQualityNanos.get(),
            xpCount = xpCount.get(),
            xpNanos = xpNanos.get(),
            xpMaxNanos = xpMaxNanos.get(),
            xpP50Nanos = percentile(xpSorted, 50),
            xpP95Nanos = percentile(xpSorted, 95),
            xpP99Nanos = percentile(xpSorted, 99)
        )
    }

    companion object {
        internal const val MAX_PERCENTILE_SAMPLES = 512
    }
}
