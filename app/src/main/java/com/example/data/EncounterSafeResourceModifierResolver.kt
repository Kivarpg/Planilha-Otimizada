package com.example.data

/**
 * Applies cost modifiers only when their semantic order is proven.
 * Ambiguous or cyclic precedence is returned as unresolved.
 */
internal object EncounterSafeResourceModifierResolver {

    data class OrderedModifier(
        val modifier: EncounterResourceModifierResolver.Modifier,
        val before: Set<String> = emptySet(),
        val after: Set<String> = emptySet()
    )

    sealed interface Result {
        data class Resolved(val flows: List<EncounterResourceBudget.Flow>) : Result
        data class Unresolved(val reason: String, val modifierIds: Set<String>) : Result
    }

    fun resolve(
        flows: List<EncounterResourceBudget.Flow>,
        modifiers: List<OrderedModifier>,
        facts: Set<String>
    ): Result {
        val malformed=modifiers.filter {
            EncounterResourceModifierValidation.validate(it.modifier).isNotEmpty()
        }
        if(malformed.isNotEmpty())
            return Result.Unresolved(
                "Modificador de recurso estruturalmente inválido.",
                malformed.map { it.modifier.id }.toSet()
            )

        val active=modifiers.filter {
            it.modifier.requirement.isSatisfiedBy(facts)
        }
        val byTarget=active.groupBy { it.modifier.targetFlowId }
        var current=flows

        for ((target, group) in byTarget) {
            if (group.size==1) {
                current=EncounterResourceModifierResolver.resolve(
                    current, listOf(group.single().modifier), facts
                )
                continue
            }

            val order=EncounterModifierPrecedence.resolve(group.map {
                EncounterModifierPrecedence.ModifierNode(
                    it.modifier.id,it.before,it.after
                )
            })
            when(order) {
                is EncounterModifierPrecedence.Result.Ordered -> {
                    val map=group.associateBy { it.modifier.id }
                    for (id in order.ids) {
                        current=EncounterResourceModifierResolver.resolve(
                            current,listOf(map.getValue(id).modifier),facts
                        )
                    }
                }
                is EncounterModifierPrecedence.Result.Ambiguous ->
                    return Result.Unresolved(
                        "Precedência não demonstrada para modificadores do fluxo $target.",
                        order.remainingIds
                    )
                is EncounterModifierPrecedence.Result.Cycle ->
                    return Result.Unresolved(
                        "Ciclo de precedência entre modificadores do fluxo $target.",
                        order.ids
                    )
                is EncounterModifierPrecedence.Result.Invalid ->
                    return Result.Unresolved(order.reason, order.ids)
            }
        }
        return Result.Resolved(current)
    }
}
