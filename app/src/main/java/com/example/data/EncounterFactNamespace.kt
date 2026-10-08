package com.example.data

/**
 * Impede colisão entre "possui Charm X", "Charm X está ativo",
 * "estado X existe" e "regra X foi satisfeita".
 */
internal object EncounterFactNamespace {
    enum class Kind { OWNED_POWER, ACTIVE_POWER, BUILD_TRAIT, EQUIPMENT, STATE, DERIVED, RULE }

    data class Fact(val kind:Kind,val id:String) {
        fun key():String="${kind.name}:$id"
    }

    fun keys(facts:Collection<Fact>):Set<String> = facts.map { it.key() }.toSet()
}
