package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterPerformanceMetricsTest {
    @Test
    fun `snapshot calcula total media e maximo sem interferir no dominio`() {
        val metrics = EncounterPerformanceMetrics()

        metrics.recordGeneration(10)
        metrics.recordGeneration(30)
        metrics.recordXp(7)
        metrics.recordXp(13)

        val snapshot = metrics.snapshot()
        assertEquals(2L, snapshot.generationCount)
        assertEquals(40L, snapshot.generationNanos)
        assertEquals(20L, snapshot.generationAverageNanos)
        assertEquals(30L, snapshot.generationMaxNanos)
        assertEquals(10L, snapshot.generationP50Nanos)
        assertEquals(30L, snapshot.generationP95Nanos)
        assertEquals(30L, snapshot.generationP99Nanos)
        assertEquals(2L, snapshot.xpCount)
        assertEquals(20L, snapshot.xpNanos)
        assertEquals(10L, snapshot.xpAverageNanos)
        assertEquals(13L, snapshot.xpMaxNanos)
        assertEquals(7L, snapshot.xpP50Nanos)
        assertEquals(13L, snapshot.xpP95Nanos)
        assertEquals(13L, snapshot.xpP99Nanos)
    }

    @Test
    fun `snapshot vazio retorna medias zero e duracao negativa e normalizada`() {
        val metrics = EncounterPerformanceMetrics()
        assertEquals(0L, metrics.snapshot().generationAverageNanos)
        assertEquals(0L, metrics.snapshot().generationP95Nanos)
        assertEquals(0L, metrics.snapshot().xpAverageNanos)
        assertEquals(0L, metrics.snapshot().xpP95Nanos)

        metrics.recordXp(-5)
        assertEquals(0L, metrics.snapshot().xpNanos)
    }

    @Test
    fun `percentis usam janela limitada sem perder totais da sessao`() {
        val metrics = EncounterPerformanceMetrics()
        val total = EncounterPerformanceMetrics.MAX_PERCENTILE_SAMPLES + 100

        repeat(total) { index ->
            metrics.recordXp((index + 1).toLong())
        }

        val snapshot = metrics.snapshot()
        assertEquals(total.toLong(), snapshot.xpCount)
        assertEquals((total.toLong() * (total + 1L)) / 2L, snapshot.xpNanos)
        assertEquals(total.toLong(), snapshot.xpMaxNanos)
        // As 100 amostras mais antigas já saíram da janela de percentis.
        assertEquals(356L, snapshot.xpP50Nanos)
        assertEquals(587L, snapshot.xpP95Nanos)
        assertEquals(607L, snapshot.xpP99Nanos)
    }

    @Test
    fun `amostras paralelas preservam totais e janela limitada`() {
        val metrics = EncounterPerformanceMetrics()
        val workers = 8
        val porWorker = 200
        val threads = (0 until workers).map {
            Thread {
                repeat(porWorker) {
                    metrics.recordGeneration(10L)
                    metrics.recordXp(20L)
                }
            }.apply { start() }
        }
        threads.forEach { it.join() }
        val snapshot = metrics.snapshot()
        val total = (workers * porWorker).toLong()
        assertEquals(total, snapshot.generationCount)
        assertEquals(total * 10L, snapshot.generationNanos)
        assertEquals(10L, snapshot.generationP50Nanos)
        assertEquals(10L, snapshot.generationP99Nanos)
        assertEquals(total, snapshot.xpCount)
        assertEquals(total * 20L, snapshot.xpNanos)
        assertEquals(20L, snapshot.xpP50Nanos)
        assertEquals(20L, snapshot.xpP99Nanos)
    }

}
