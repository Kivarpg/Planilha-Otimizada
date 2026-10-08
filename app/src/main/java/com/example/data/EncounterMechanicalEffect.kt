package com.example.data

/**
 * Um Charm pode ter duração global e, ainda assim, produzir efeitos
 * com vidas úteis diferentes. A realizabilidade usa a duração do EFEITO,
 * nunca presume que todos os efeitos herdam a duração do Charm.
 */
internal data class EncounterMechanicalEffect(
    val id: String,
    val mechanic: String,
    val relation: Relation,
    val lifetime: EffectLifetime,
    val requirement: EncounterRequirementExpression =
        EncounterRequirementExpression.Always,
    val confidence: Double = 1.0
) {
    enum class Relation {
        PRODUCES,
        AMPLIFIES,
        EXPLOITS,
        CONVERTS,
        SUSTAINS,
        RECOVERS,
        MODIFIES,
        RESETS,
        CONFLICTS
    }

    sealed interface EffectLifetime {
        data object Instant : EffectLifetime
        data object UntilEndOfTurn : EffectLifetime
        data object UntilEndOfRound : EffectLifetime
        data object OneScene : EffectLifetime
        data object Indefinite : EffectLifetime
        data object Permanent : EffectLifetime
        data class UntilCondition(val conditionId: String) : EffectLifetime
        data class Explicit(val description: String) : EffectLifetime
        data object Unknown : EffectLifetime
    }

    fun isAvailable(facts: Set<String>): Boolean =
        requirement.isSatisfiedBy(facts)

    fun survivesToLaterRound(): Boolean = when (lifetime) {
        EffectLifetime.OneScene,
        EffectLifetime.Indefinite,
        EffectLifetime.Permanent -> true

        is EffectLifetime.UntilCondition -> false // contexto é obrigatório; usar overload tri-state

        EffectLifetime.Instant,
        EffectLifetime.UntilEndOfTurn,
        EffectLifetime.UntilEndOfRound,
        is EffectLifetime.Explicit,
        EffectLifetime.Unknown -> false
    }

    fun survivesToLaterRound(knownTrue:Set<String>, knownFalse:Set<String>):Boolean? =
        when(val life=lifetime) {
            EffectLifetime.OneScene, EffectLifetime.Indefinite, EffectLifetime.Permanent -> true
            is EffectLifetime.UntilCondition -> when {
                life.conditionId in knownFalse -> false
                life.conditionId in knownTrue -> true
                else -> null
            }
            EffectLifetime.Instant, EffectLifetime.UntilEndOfTurn,
            EffectLifetime.UntilEndOfRound -> false
            is EffectLifetime.Explicit, EffectLifetime.Unknown -> null
        }
}

/**
 * Resolve uma dependência usando efeitos concretos e satisfeitos.
 * Um efeito instantâneo não é promovido artificialmente a estado persistente
 * apenas porque o Charm que o contém dura uma cena.
 */
internal object EncounterEffectRealizability {
    data class Result(
        val supported: Boolean,
        val crossRound: Boolean,
        val supportingEffectIds: Set<String>,
        val reason: String
    )

    fun supports(
        requiredMechanic: String,
        effects: Collection<EncounterMechanicalEffect>,
        facts: Set<String>,
        requireCrossRound: Boolean,
        knownFalse: Set<String> = emptySet()
    ): Result {
        if(requiredMechanic.isBlank()) return Result(
            false,false,emptySet(),"Mecânica requerida vazia."
        )
        val validEffects=effects.filter {
            it.id.isNotBlank() && it.mechanic.isNotBlank() && it.confidence in 0.0..1.0
        }
        val matching = validEffects.filter {
            it.mechanic == requiredMechanic &&
                it.relation == EncounterMechanicalEffect.Relation.PRODUCES &&
                it.requirement.evaluate(facts,knownFalse)==EncounterTruth.TRUE
        }

        if (matching.isEmpty()) {
            return Result(
                supported = false,
                crossRound = false,
                supportingEffectIds = emptySet(),
                reason = "Nenhum efeito produtor disponível satisfaz a dependência."
            )
        }

        if (!requireCrossRound) {
            return Result(
                supported = true,
                crossRound = false,
                supportingEffectIds = matching.map { it.id }.toSet(),
                reason = "Há efeito produtor disponível na janela atual."
            )
        }

        val persistence = matching.associateWith { it.survivesToLaterRound(facts,knownFalse) }
        val persistent = persistence.filterValues { it==true }.keys
        val uncertain = persistence.filterValues { it==null }.keys
        return if (persistent.isNotEmpty()) {
            Result(
                supported = true,
                crossRound = true,
                supportingEffectIds = persistent.map { it.id }.toSet(),
                reason = "A dependência possui produtor cujo efeito persiste até rodada posterior."
            )
        } else if(uncertain.isNotEmpty()) {
            Result(
                supported=false,
                crossRound=false,
                supportingEffectIds=uncertain.map { it.id }.toSet(),
                reason="Persistência até rodada posterior é desconhecida; não promover sinergia."
            )
        } else {
            Result(
                supported = false,
                crossRound = false,
                supportingEffectIds = matching.map { it.id }.toSet(),
                reason = "Há produtor, mas nenhum efeito correspondente persiste até rodada posterior."
            )
        }
    }
}
