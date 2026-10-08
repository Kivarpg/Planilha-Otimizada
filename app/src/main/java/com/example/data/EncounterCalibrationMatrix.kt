package com.example.data

import com.example.model.TipoExaltadoEncontro

internal data class EncounterCalibrationCase(
    val exaltedType: TipoExaltadoEncontro,
    val archetype: String,
    val focus: String?,
    val martialRoute: Boolean,
    val seed: Int,
    val vector: EncounterBuildObservability.CalibrationVector
)

internal data class EncounterCalibrationMatrix(
    val cases: List<EncounterCalibrationCase>
) {
    fun byExaltedType(): Map<TipoExaltadoEncontro, EncounterCalibrationSummary> =
        cases.groupBy { it.exaltedType }.mapValues { (_, group) ->
            EncounterCalibrationAnalyzer.summarize(group.map { it.vector })
        }

    fun byMartialRoute(): Map<Boolean, EncounterCalibrationSummary> =
        cases.groupBy { it.martialRoute }.mapValues { (_, group) ->
            EncounterCalibrationAnalyzer.summarize(group.map { it.vector })
        }

    fun byArchetype(): Map<String, EncounterCalibrationSummary> =
        cases.groupBy { it.archetype }.mapValues { (_, group) ->
            EncounterCalibrationAnalyzer.summarize(group.map { it.vector })
        }
}

internal object EncounterCalibrationMatrixBuilder {
    fun build(cases: List<EncounterCalibrationCase>): EncounterCalibrationMatrix =
        EncounterCalibrationMatrix(
            cases.sortedWith(
                compareBy<EncounterCalibrationCase> { it.exaltedType.name }
                    .thenBy { it.archetype }
                    .thenBy { it.focus.orEmpty() }
                    .thenBy { it.martialRoute }
                    .thenBy { it.seed }
            )
        )
}
