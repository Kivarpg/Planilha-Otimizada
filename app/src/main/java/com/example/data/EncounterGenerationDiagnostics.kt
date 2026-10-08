package com.example.data

/**
 * Projeção somente-leitura para diagnóstico de desempenho da geração.
 * Separa métricas de parede (profiling humano) de trabalho determinístico
 * (comparável entre execuções e seguro para gates de CI).
 */
object EncounterGenerationDiagnostics {
    data class Report(
        val generationCount: Long,
        val generationAverageNanos: Long,
        val generationMaxNanos: Long,
        val generationP50Nanos: Long,
        val generationP95Nanos: Long,
        val generationP99Nanos: Long,
        val route: EncounterCharmRouteMetrics.Snapshot? = null
    ) {
        val deterministicRouteWorkUnits: Long get() = route?.deterministicWorkUnits ?: 0L
        fun diagnosticLines(): List<String> = buildList {
            add("generationCount=$generationCount generationAverageNanos=$generationAverageNanos generationMaxNanos=$generationMaxNanos generationP50Nanos=$generationP50Nanos generationP95Nanos=$generationP95Nanos generationP99Nanos=$generationP99Nanos")
            route?.let { add(it.diagnosticLine()) }
        }
    }

    fun report(
        performance: EncounterPerformanceMetrics.Snapshot,
        route: EncounterCharmRouteMetrics.Snapshot? = null
    ): Report = Report(
        generationCount = performance.generationCount,
        generationAverageNanos = performance.generationAverageNanos,
        generationMaxNanos = performance.generationMaxNanos,
        generationP50Nanos = performance.generationP50Nanos,
        generationP95Nanos = performance.generationP95Nanos,
        generationP99Nanos = performance.generationP99Nanos,
        route = route
    )
}
