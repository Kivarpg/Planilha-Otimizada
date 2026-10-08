package com.example.data

/**
 * Estado projetado com proveniência e revogação.
 *
 * Fatos derivados não são um Set<String> monotônico. Eles podem nascer,
 * ser substituídos, consumidos ou expirar quando sua fonte deixa de valer.
 * Isto continua sendo análise de construção/realizabilidade, não combate.
 */
internal object EncounterProjectedFactState {

    enum class Lifetime {
        BUILD,
        LOADOUT,
        SCENE,
        ROUND,
        ACTION,
        TRANSIENT
    }

    data class FactId(val value: String)
    data class SourceId(val value: String)

    data class Fact(
        val id: FactId,
        val source: SourceId,
        val lifetime: Lifetime,
        val exclusiveGroup: String? = null
    )

    data class State(
        val facts: Set<Fact> = emptySet()
    ) {
        fun has(id: FactId): Boolean = facts.any { it.id == id }

        fun add(fact: Fact): State {
            val withoutExclusive = fact.exclusiveGroup?.let { group ->
                facts.filterNot { it.exclusiveGroup == group }.toSet()
            } ?: facts
            return copy(facts = withoutExclusive + fact)
        }

        fun revokeSource(source: SourceId): State =
            copy(facts = facts.filterNot { it.source == source }.toSet())

        fun consume(id: FactId): State =
            copy(facts = facts.filterNot { it.id == id }.toSet())

        fun expire(scope: Lifetime): State =
            copy(facts = facts.filterNot { it.lifetime == scope }.toSet())
    }

    sealed interface Mutation {
        data class Produce(val fact: Fact) : Mutation
        data class RevokeSource(val source: SourceId) : Mutation
        data class Consume(val fact: FactId) : Mutation
        data class Expire(val lifetime: Lifetime) : Mutation
    }

    fun apply(initial: State, mutations: List<Mutation>): State {
        var state = initial
        for (m in mutations) {
            state = when (m) {
                is Mutation.Produce -> state.add(m.fact)
                is Mutation.RevokeSource -> state.revokeSource(m.source)
                is Mutation.Consume -> state.consume(m.fact)
                is Mutation.Expire -> state.expire(m.lifetime)
            }
        }
        return state
    }
}
