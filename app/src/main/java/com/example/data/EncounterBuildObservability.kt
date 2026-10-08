package com.example.data

/**
 * Motivo semântico de uma aquisição de Encanto. Esta camada é somente de
 * observabilidade: não participa de score, legalidade ou desempate.
 */
internal enum class EncounterSelectionReason {
    CONTINUIDADE_FOCO,
    FOCO_SECUNDARIO,
    LACUNA_DEFENSIVA,
    COMBATE,
    PREREQUISITO,
    SINERGIA,
    RETORNO_FOCO,
    COMPLEMENTAR,
    ROTA_MARCIAL,
    REQUISITO_ESTILO,
    CONTINUIDADE_ESTILO,
    NOVO_ESTILO,
    FALLBACK
}

internal data class EncounterSelectionTraceEntry(
    val charmName: String,
    val category: String,
    val reason: EncounterSelectionReason,
    val sequence: Int
)


/**
 * Explicação canônica da legalidade, separada do motivo estratégico da seleção.
 * UI futura pode exibir "por que pode" e "por que foi escolhido" sem reconstruir regras.
 */
internal data class EncounterEligibilityTrace(
    val contentId: String,
    val selectedRouteId: String?,
    val available: Boolean,
    val missingRequirements: List<String> = emptyList()
)

internal fun EncounterRulesEngine.Eligibility.toEligibilityTrace(
    id: PreparedEncounterCatalog.StableContentId
): EncounterEligibilityTrace = when (this) {
    EncounterRulesEngine.Eligibility.Acquired ->
        EncounterEligibilityTrace(id.value, null, available = true)
    is EncounterRulesEngine.Eligibility.Available ->
        EncounterEligibilityTrace(id.value, routes.firstOrNull()?.routeId, available = true)
    is EncounterRulesEngine.Eligibility.Locked ->
        EncounterEligibilityTrace(
            id.value, null, available = false,
            missingRequirements = routes.flatMap { it.missing }.distinct().map { it.toString() }
        )
    is EncounterRulesEngine.Eligibility.Invalid ->
        EncounterEligibilityTrace(id.value, null, available = false, missingRequirements = listOf(reason))
}

internal data class EncounterSelectionTrace(
    val entries: List<EncounterSelectionTraceEntry>
) {
    val reasons: Map<EncounterSelectionReason, Int>
        get() = entries.groupingBy { it.reason }.eachCount()
}

/**
 * Estado explícito de uma busca limitada. Evita confundir ausência provada de
 * solução com interrupção por orçamento.
 */
internal sealed interface EncounterSearchOutcome<out T> {
    data class Found<T>(val value: T) : EncounterSearchOutcome<T>
    data object ProvenImpossible : EncounterSearchOutcome<Nothing>
    data class BudgetExhausted(val exploredStates: Long) : EncounterSearchOutcome<Nothing>
}

/**
 * Diagnóstico shadow-mode da coerência de uma build. Nunca bloqueia geração.
 */
internal data class EncounterBuildQualityReport(
    val totalCharms: Int,
    val treesOpened: Int,
    val mainTreeCharms: Int,
    val isolatedEntryCharms: Int,
    val fallbackSelections: Int,
    val budgetExhaustions: Int,
    val deepestTreeSize: Int = 0,
    val singleCharmTrees: Int = 0,
    val martialStylesOpened: Int = 0,
    val martialCharms: Int = 0
) {
    val mainTreeShare: Double
        get() = if (totalCharms == 0) 0.0 else mainTreeCharms.toDouble() / totalCharms

    /** Maior quantidade de Encantos selecionados em uma mesma árvore/categoria.
     * Não representa profundidade topológica do grafo de pré-requisitos. */
    val largestTreeSelectionCount: Int
        get() = deepestTreeSize

    /** Árvores/categorias com exatamente um Encanto selecionado.
     * Não afirma que o Encanto seja uma raiz isolada no grafo. */
    val singleSelectionTrees: Int
        get() = singleCharmTrees

    val averageDepthProxy: Double
        get() = if (treesOpened == 0) 0.0 else totalCharms.toDouble() / treesOpened
}

internal object EncounterBuildQualityAnalyzer {
    /**
     * Deriva somente métricas de dados já escolhidos. Não altera a seleção.
     * `categories` deve refletir a árvore/categoria de cada Encanto selecionado.
     */
    fun analyze(
        categories: List<String>,
        mainCategory: String?,
        isolatedEntryCharms: Int = 0,
        trace: EncounterSelectionTrace = EncounterSelectionTrace(emptyList()),
        telemetry: EncounterSelectionTelemetry.Snapshot = EncounterSelectionTelemetry.snapshot(),
        martialStyleCategories: Set<String> = emptySet()
    ): EncounterBuildQualityReport {
        val normalized = categories.map { it.trim() }.filter { it.isNotEmpty() }
        val countsByTree = normalized.groupingBy { it.lowercase() }.eachCount()
        val mainCount = mainCategory?.let { main ->
            normalized.count { it.equals(main, ignoreCase = true) }
        } ?: 0
        val derivedIsolated = countsByTree.values.count { it == 1 }
        return EncounterBuildQualityReport(
            totalCharms = normalized.size,
            treesOpened = countsByTree.size,
            mainTreeCharms = mainCount,
            isolatedEntryCharms = maxOf(isolatedEntryCharms.coerceAtLeast(0), derivedIsolated),
            fallbackSelections = trace.reasons[EncounterSelectionReason.FALLBACK] ?: 0,
            budgetExhaustions = telemetry.exactSearchBudgetExhausted.toInt(),
            deepestTreeSize = countsByTree.values.maxOrNull() ?: 0,
            singleCharmTrees = derivedIsolated,
            martialStylesOpened = martialStyleCategories.count { style ->
                normalized.any { it.equals(style, ignoreCase = true) }
            },
            martialCharms = normalized.count { category ->
                martialStyleCategories.any { it.equals(category, ignoreCase = true) }
            }
        )
    }
}



internal data class EncounterCalibrationSummary(
    val samples: Int,
    val averageTreesOpened: Double,
    val averageLargestTreeSelectionCount: Double,
    val averageSingleSelectionTrees: Double,
    val averageMainTreeShare: Double,
    val averageMartialCharms: Double,
    val totalFallbackSelections: Int,
    val totalBudgetExhaustions: Int,
    val totalCompletionFallbackActivations: Long
)

internal object EncounterCalibrationAnalyzer {
    fun summarize(vectors: List<EncounterBuildObservability.CalibrationVector>): EncounterCalibrationSummary {
        if (vectors.isEmpty()) {
            return EncounterCalibrationSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0)
        }
        fun avg(selector: (EncounterBuildObservability.CalibrationVector) -> Number): Double =
            vectors.map { selector(it).toDouble() }.average()
        return EncounterCalibrationSummary(
            samples = vectors.size,
            averageTreesOpened = avg { it.treesOpened },
            averageLargestTreeSelectionCount = avg { it.largestTreeSelectionCount },
            averageSingleSelectionTrees = avg { it.singleSelectionTrees },
            averageMainTreeShare = avg { it.mainTreeShare },
            averageMartialCharms = avg { it.martialCharms },
            totalFallbackSelections = vectors.sumOf { it.fallbackSelections },
            totalBudgetExhaustions = vectors.sumOf { it.exactSearchBudgetExhausted.toInt() },
            totalCompletionFallbackActivations = vectors.sumOf { it.completionFallbackActivated }
        )
    }
}

/**
 * Registro shadow-mode agregado por geração. Mantém apenas o último resultado
 * por thread para não acoplar o domínio à UI nem alterar persistência.
 */
internal object EncounterBuildObservability {
    internal data class Snapshot(
        val trace: EncounterSelectionTrace?,
        val report: EncounterBuildQualityReport?,
        val selectionTelemetry: EncounterSelectionTelemetry.Snapshot,
        val routeMetrics: EncounterCharmRouteMetrics.Snapshot? = null
    ) {
        // Compatibilidade com os testes/consumidores que usavam Pair.
        val first: EncounterSelectionTrace? get() = trace
        val second: EncounterBuildQualityReport? get() = report
    }

    internal data class CalibrationVector(
        val totalCharms: Int,
        val treesOpened: Int,
        val deepestTreeSize: Int,
        val singleCharmTrees: Int,
        val mainTreeShare: Double,
        val averageDepthProxy: Double,
        val martialStylesOpened: Int,
        val martialCharms: Int,
        val fallbackSelections: Int,
        val exactSearchFound: Long,
        val exactSearchProvenImpossible: Long,
        val exactSearchBudgetExhausted: Long,
        val completionFallbackActivated: Long,
        val largestTreeSelectionCount: Int = deepestTreeSize,
        val singleSelectionTrees: Int = singleCharmTrees
    )

    private val lastTrace = ThreadLocal<EncounterSelectionTrace?>()
    private val lastReport = ThreadLocal<EncounterBuildQualityReport?>()
    private val telemetryBaseline = ThreadLocal<EncounterSelectionTelemetry.Snapshot?>()
    private val lastBuildTelemetry = ThreadLocal<EncounterSelectionTelemetry.Snapshot?>()
    private val lastFinalQuality = ThreadLocal<EncounterFinalBuildQuality.Score?>()
    private val lastRouteMetrics = ThreadLocal<EncounterCharmRouteMetrics.Snapshot?>()

    fun beginBuild() {
        telemetryBaseline.set(EncounterSelectionTelemetry.snapshot())
        lastBuildTelemetry.remove()
        lastFinalQuality.remove()
        lastRouteMetrics.remove()
    }

    fun publish(
        trace: EncounterSelectionTrace,
        report: EncounterBuildQualityReport
    ) {
        lastTrace.set(trace)
        lastReport.set(report)
    }


    /**
     * Republica a observabilidade sobre a lista FINAL de Encantos do NPC.
     * É chamada depois da integração de Artes Marciais e permanece shadow-mode:
     * não participa de legalidade, score, seleção ou persistência.
     */
    fun publishFinalBuild(
        selected: List<Pair<String, String>>,
        mainCategory: String?,
        combatCategory: String? = null,
        martialStyleCategories: Set<String> = emptySet(),
        newlyOpenedMartialStyle: String? = null,
        martialPrerequisiteCharms: Set<String> = emptySet()
    ) {
        val previous = lastTrace.get()
        val fallbackActivated = previous?.entries?.any {
            it.reason == EncounterSelectionReason.FALLBACK
        } == true
        val trace = buildEncounterSelectionTrace(
            selected = selected,
            mainCategory = mainCategory,
            combatCategory = combatCategory,
            fallbackActivated = fallbackActivated,
            martialStyleCategories = martialStyleCategories,
            newlyOpenedMartialStyle = newlyOpenedMartialStyle,
            martialPrerequisiteCharms = martialPrerequisiteCharms
        )
        val report = EncounterBuildQualityAnalyzer.analyze(
            categories = selected.map { it.second },
            mainCategory = mainCategory,
            trace = trace,
            martialStyleCategories = martialStyleCategories
        )
        val now = EncounterSelectionTelemetry.snapshot()
        val before = telemetryBaseline.get()
        fun delta(nowValue: Long, beforeValue: Long?): Long =
            (nowValue - (beforeValue ?: nowValue)).coerceAtLeast(0L)
        lastBuildTelemetry.set(
            EncounterSelectionTelemetry.Snapshot(
                exactSearchBudgetExhausted = delta(now.exactSearchBudgetExhausted, before?.exactSearchBudgetExhausted),
                completionFallbackActivated = delta(now.completionFallbackActivated, before?.completionFallbackActivated),
                exactSearchFound = delta(now.exactSearchFound, before?.exactSearchFound),
                exactSearchProvenImpossible = delta(now.exactSearchProvenImpossible, before?.exactSearchProvenImpossible)
            )
        )
        telemetryBaseline.set(now)
        publish(trace, report)
    }

    fun publishFinalQuality(score: EncounterFinalBuildQuality.Score) {
        lastFinalQuality.set(score)
    }

    fun publishRouteMetrics(snapshot: EncounterCharmRouteMetrics.Snapshot) {
        lastRouteMetrics.set(snapshot)
    }

    fun finalQuality(): EncounterFinalBuildQuality.Score? = lastFinalQuality.get()

    fun snapshot(): Snapshot = Snapshot(
        trace = lastTrace.get(),
        report = lastReport.get(),
        selectionTelemetry = EncounterSelectionTelemetry.snapshot(),
        routeMetrics = lastRouteMetrics.get()
    )

    /**
     * Vetor somente-leitura preparado para a calibração estatística. Retorna
     * null antes da primeira build observada e nunca participa da seleção.
     */
    fun calibrationVector(): CalibrationVector? {
        val report = lastReport.get() ?: return null
        val telemetry = lastBuildTelemetry.get() ?: EncounterSelectionTelemetry.Snapshot(0, 0, 0, 0)
        return CalibrationVector(
            totalCharms = report.totalCharms,
            treesOpened = report.treesOpened,
            deepestTreeSize = report.deepestTreeSize,
            singleCharmTrees = report.singleCharmTrees,
            mainTreeShare = report.mainTreeShare,
            averageDepthProxy = report.averageDepthProxy,
            martialStylesOpened = report.martialStylesOpened,
            martialCharms = report.martialCharms,
            fallbackSelections = report.fallbackSelections,
            exactSearchFound = telemetry.exactSearchFound,
            exactSearchProvenImpossible = telemetry.exactSearchProvenImpossible,
            exactSearchBudgetExhausted = telemetry.exactSearchBudgetExhausted,
            completionFallbackActivated = telemetry.completionFallbackActivated,
            largestTreeSelectionCount = report.largestTreeSelectionCount,
            singleSelectionTrees = report.singleSelectionTrees
        )
    }

    internal fun clearForTests() {
        lastTrace.remove()
        lastReport.remove()
        telemetryBaseline.remove()
        lastBuildTelemetry.remove()
        lastFinalQuality.remove()
        lastRouteMetrics.remove()
    }
}

/**
 * Classificação determinística e pós-seleção. Ela explica a sequência já
 * escolhida; não é consultada pelo seletor e, portanto, não pode mudar a build.
 */
internal fun buildEncounterSelectionTrace(
    selected: List<Pair<String, String>>,
    mainCategory: String?,
    secondaryCategory: String? = null,
    combatCategory: String? = null,
    fallbackActivated: Boolean = false,
    martialStyleCategories: Set<String> = emptySet(),
    newlyOpenedMartialStyle: String? = null,
    martialPrerequisiteCharms: Set<String> = emptySet()
): EncounterSelectionTrace {
    val seenCategories = mutableSetOf<String>()
    var leftMain = false
    return EncounterSelectionTrace(
        selected.mapIndexed { index, (name, category) ->
            val isMain = mainCategory != null && category.equals(mainCategory, ignoreCase = true)
            val reason = when {
                martialPrerequisiteCharms.any { name.equals(it, ignoreCase = true) } ->
                    EncounterSelectionReason.REQUISITO_ESTILO
                isMain && leftMain -> EncounterSelectionReason.RETORNO_FOCO
                isMain -> EncounterSelectionReason.CONTINUIDADE_FOCO
                secondaryCategory != null && category.equals(secondaryCategory, ignoreCase = true) ->
                    EncounterSelectionReason.FOCO_SECUNDARIO
                newlyOpenedMartialStyle != null &&
                    category.equals(newlyOpenedMartialStyle, ignoreCase = true) ->
                    EncounterSelectionReason.NOVO_ESTILO
                martialStyleCategories.any { category.equals(it, ignoreCase = true) } &&
                    category.lowercase() in seenCategories ->
                    EncounterSelectionReason.CONTINUIDADE_ESTILO
                martialStyleCategories.any { category.equals(it, ignoreCase = true) } ->
                    EncounterSelectionReason.ROTA_MARCIAL
                combatCategory != null && category.equals(combatCategory, ignoreCase = true) ->
                    EncounterSelectionReason.COMBATE
                fallbackActivated && index == selected.lastIndex -> EncounterSelectionReason.FALLBACK
                category.lowercase() in seenCategories -> EncounterSelectionReason.SINERGIA
                else -> EncounterSelectionReason.COMPLEMENTAR
            }
            if (!isMain && mainCategory != null) leftMain = true
            seenCategories += category.lowercase()
            EncounterSelectionTraceEntry(name, category, reason, index)
        }
    )
}
