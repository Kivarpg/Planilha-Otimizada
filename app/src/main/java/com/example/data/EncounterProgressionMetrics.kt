package com.example.data

import java.util.concurrent.atomic.AtomicLong

/**
 * Métricas transitórias do roadmap. Não são persistidas e não alteram a
 * decisão do algoritmo; servem apenas para medir se cache e prefetch estão
 * realmente reduzindo trabalho no fluxo de +XP.
 */
class EncounterProgressionMetrics {
    private val cacheHits = AtomicLong()
    private val cacheMisses = AtomicLong()
    private val roadmapFallbacks = AtomicLong()
    private val prefetchScheduled = AtomicLong()
    private val prefetchSkippedActive = AtomicLong()
    private val prefetchCompleted = AtomicLong()
    private val prefetchDiscarded = AtomicLong()
    private val prefetchInvalidated = AtomicLong()
    private val prefetchFailed = AtomicLong()
    private val prefetchBuildNanos = AtomicLong()
    private val canonicalAudits = AtomicLong()
    private val canonicalAuditFindings = AtomicLong()

    data class Snapshot(
        val cacheHits: Long,
        val cacheMisses: Long,
        val roadmapFallbacks: Long,
        val prefetchScheduled: Long,
        val prefetchSkippedActive: Long,
        val prefetchCompleted: Long,
        val prefetchDiscarded: Long,
        val prefetchInvalidated: Long,
        val prefetchFailed: Long,
        val prefetchBuildNanos: Long,
        val canonicalAudits: Long,
        val canonicalAuditFindings: Long
    ) {
        val cacheHitRate: Double
            get() {
                val total = cacheHits + cacheMisses
                return if (total == 0L) 0.0 else cacheHits.toDouble() / total.toDouble()
            }
    }

    internal fun recordCacheHit() = cacheHits.incrementAndGet()
    internal fun recordCacheMiss() = cacheMisses.incrementAndGet()
    internal fun recordRoadmapFallback() = roadmapFallbacks.incrementAndGet()
    internal fun recordPrefetchScheduled() = prefetchScheduled.incrementAndGet()
    internal fun recordPrefetchSkippedActive() = prefetchSkippedActive.incrementAndGet()
    internal fun recordPrefetchCompleted() = prefetchCompleted.incrementAndGet()
    internal fun recordPrefetchDiscarded() = prefetchDiscarded.incrementAndGet()
    internal fun recordPrefetchInvalidated() = prefetchInvalidated.incrementAndGet()
    internal fun recordPrefetchFailed() = prefetchFailed.incrementAndGet()
    internal fun recordPrefetchBuildNanos(nanos: Long) = prefetchBuildNanos.addAndGet(nanos.coerceAtLeast(0L))
    internal fun recordCanonicalAudit(findings: Int) {
        canonicalAudits.incrementAndGet()
        canonicalAuditFindings.addAndGet(findings.coerceAtLeast(0).toLong())
    }

    fun snapshot(): Snapshot = Snapshot(
        cacheHits = cacheHits.get(),
        cacheMisses = cacheMisses.get(),
        roadmapFallbacks = roadmapFallbacks.get(),
        prefetchScheduled = prefetchScheduled.get(),
        prefetchSkippedActive = prefetchSkippedActive.get(),
        prefetchCompleted = prefetchCompleted.get(),
        prefetchDiscarded = prefetchDiscarded.get(),
        prefetchInvalidated = prefetchInvalidated.get(),
        prefetchFailed = prefetchFailed.get(),
        prefetchBuildNanos = prefetchBuildNanos.get(),
        canonicalAudits = canonicalAudits.get(),
        canonicalAuditFindings = canonicalAuditFindings.get()
    )
}
