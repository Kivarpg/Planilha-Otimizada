package com.example.data

/**
 * Avalia se uma sequência de configurações é estruturalmente alcançável.
 * Não escolhe ações de combate: recebe uma sequência hipotética produzida
 * pelo avaliador de construção e verifica somente consistência/transições.
 */
internal object EncounterConfigurationPathEvaluator {

    data class Step(
        val id: String,
        val desiredCommitments: Set<EncounterCommitmentModel.Commitment>,
        val facts: Set<String> = emptySet()
    )

    data class Result(
        val reachable: Boolean,
        val transitionCost: Int,
        val finalState: EncounterCommitmentModel.State,
        val failedStepId: String? = null,
        val reason: String
    )

    fun evaluate(
        initial: EncounterCommitmentModel.State,
        steps: List<Step>,
        transitionRules: Collection<EncounterCommitmentModel.TransitionRule>
    ): Result {
        var state = initial
        var cost = 0

        for (step in steps) {
            for (desired in step.desiredCommitments) {
                val result = EncounterCommitmentModel.apply(
                    state, desired, transitionRules, step.facts
                )
                if (!result.allowed) {
                    return Result(
                        false, cost, state, step.id, result.reason
                    )
                }
                state = result.state
                cost += result.structuralCost
            }
        }

        return Result(
            true, cost, state, null,
            "Todas as configurações são alcançáveis por transições explícitas."
        )
    }
}
