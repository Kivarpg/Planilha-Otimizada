package com.example.data

/**
 * Modificadores explícitos de custo.
 * Aplicados somente quando seus requisitos estão satisfeitos.
 * Não inferem geração/recuperação temporal.
 */
internal object EncounterResourceModifierResolver {

    enum class Kind {
        REPLACE_COST,
        REDUCE_COST,
        REFUND,
        WAIVE,
        CAP_COST,
        CHANGE_RESOURCE
    }

    data class Modifier(
        val id: String,
        val targetFlowId: String,
        val kind: Kind,
        val amount: Int? = null,
        val replacementAmount: Int? = null,
        val replacementResource: EncounterResourceBudget.Resource? = null,
        val requirement: EncounterRequirementExpression =
            EncounterRequirementExpression.Always
    )

    fun resolve(
        flows: List<EncounterResourceBudget.Flow>,
        modifiers: List<Modifier>,
        facts: Set<String>
    ): List<EncounterResourceBudget.Flow> {
        return flows.map { original ->
            var flow = original
            val active = modifiers.filter {
                it.targetFlowId == original.id &&
                    it.requirement.isSatisfiedBy(facts)
            }

            for (m in active) {
                flow = when (m.kind) {
                    Kind.WAIVE -> flow.copy(amount = 0)
                    Kind.REPLACE_COST -> flow.copy(amount = m.replacementAmount)
                    Kind.REDUCE_COST -> {
                        val a = flow.amount
                        flow.copy(amount = if (a != null && m.amount != null)
                            (a - m.amount).coerceAtLeast(0) else null)
                    }
                    Kind.CAP_COST -> {
                        val a = flow.amount
                        flow.copy(amount = if (a != null && m.amount != null)
                            minOf(a, m.amount) else null)
                    }
                    Kind.CHANGE_RESOURCE -> flow.copy(
                        resource = m.replacementResource ?: flow.resource
                    )
                    Kind.REFUND -> flow // refund remains a separate causal effect; do not net statically.
                }
            }
            flow
        }
    }
}
