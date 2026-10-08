package com.example.data

/**
 * Papéis causais separados. "Quem usa o Charm" não é necessariamente
 * "quem executa a ação aprimorada", nem "quem recebe o benefício".
 */
internal object EncounterActorScope {
    enum class Actor {
        SELF, ALLY, TARGET, ENEMY, FAMILIAR, SUMMON, BATTLE_GROUP,
        CREATED_ENTITY, WEAPON_ENTITY, ANY_CHARACTER, ANY_OTHER, UNKNOWN
    }

    data class ActorRef(
        val actor:Actor,
        /** Mesmo participantId = obrigatoriamente o mesmo participante concreto. */
        val participantId:EncounterParticipantId?=null
    )

    data class ScopedMechanic(
        val mechanic:String,
        val activator:ActorRef=ActorRef(Actor.SELF),
        val actionActor:ActorRef=activator,
        val beneficiary:ActorRef=actionActor,
        val affected:ActorRef=ActorRef(Actor.UNKNOWN)
    )

    data class ScopedRequirement(
        val mechanic:String,
        val allowedActivators:Set<Actor> = setOf(Actor.SELF),
        val allowedActionActors:Set<Actor> = setOf(Actor.SELF),
        val allowedBeneficiaries:Set<Actor> = setOf(Actor.SELF),
        val allowedAffected:Set<Actor>? = null,
        /** IDs iguais entre campos exigem o mesmo participante concreto. */
        val bindingConstraints:Set<EncounterParticipantBindings.Constraint> = emptySet()
    )

    data class Match(
        val supported:Boolean,
        val externalDependency:Boolean,
        val uncertain:Boolean,
        val reason:String
    )

    private fun actorAllowed(actual:Actor,allowed:Set<Actor>):Boolean? = when {
        allowed.isEmpty() -> false
        actual==Actor.UNKNOWN -> null
        actual==Actor.ANY_CHARACTER -> if(Actor.ANY_CHARACTER in allowed) true else null
        actual==Actor.ANY_OTHER -> if(Actor.ANY_OTHER in allowed) true else null
        Actor.ANY_CHARACTER in allowed &&
            actual in setOf(Actor.SELF,Actor.ALLY,Actor.TARGET,Actor.ENEMY) -> true
        actual in allowed -> true
        else -> false
    }

    fun matches(produced:ScopedMechanic,required:ScopedRequirement):Match {
        if(produced.mechanic!=required.mechanic)
            return Match(false,false,false,"Mecânicas diferentes.")

        val checks=listOf(
            actorAllowed(produced.activator.actor,required.allowedActivators),
            actorAllowed(produced.actionActor.actor,required.allowedActionActors),
            actorAllowed(produced.beneficiary.actor,required.allowedBeneficiaries)
        ) + listOfNotNull(required.allowedAffected?.let {
            actorAllowed(produced.affected.actor,it)
        })
        if(checks.any { it==false })
            return Match(false,produced.actionActor.actor!=Actor.SELF,false,
                "Um papel causal pertence a ator incompatível.")
        if(checks.any { it==null })
            return Match(false,produced.actionActor.actor!=Actor.SELF,true,
                "Papel causal desconhecido; não promover suporte.")

        val refs=mapOf(
            "activator" to produced.activator,
            "actionActor" to produced.actionActor,
            "beneficiary" to produced.beneficiary,
            "affected" to produced.affected
        )
        val bindings=refs.mapNotNull { (role,ref) ->
            ref.participantId?.let { EncounterBindingKey(role) to it }
        }.toMap()
        for(constraint in required.bindingConstraints) {
            when(EncounterParticipantBindings.check(bindings,constraint)) {
                EncounterParticipantBindings.Check.UNKNOWN ->
                    return Match(false,produced.actionActor.actor!=Actor.SELF,true,
                        "Identidade concreta necessária não foi provada.")
                EncounterParticipantBindings.Check.VIOLATED ->
                    return Match(false,produced.actionActor.actor!=Actor.SELF,false,
                        "Restrição de identidade entre participantes foi violada.")
                EncounterParticipantBindings.Check.SATISFIED -> Unit
            }
        }

        val external=produced.actionActor.actor!=Actor.SELF ||
            produced.beneficiary.actor!=Actor.SELF
        return Match(true,external,false,
            if(external) "Suporte realizado por/para participante externo."
            else "Dependência autossustentada pelo NPC.")
    }
}

internal enum class EncounterDependencySource {
    SELF_GENERATED, BUILD_SUPPORTED, EXTERNAL_COMMON, EXTERNAL_CONDITIONAL,
    EXTERNAL_RARE, UNSUPPORTED, UNKNOWN
}
internal object EncounterDependencyClassifier {
    data class Result(val source:EncounterDependencySource,val fullSelfSustainingCredit:Boolean)
    fun classify(match:EncounterActorScope.Match):Result=when {
        match.uncertain -> Result(EncounterDependencySource.UNKNOWN,false)
        !match.supported -> Result(EncounterDependencySource.UNSUPPORTED,false)
        !match.externalDependency -> Result(EncounterDependencySource.SELF_GENERATED,true)
        else -> Result(EncounterDependencySource.EXTERNAL_CONDITIONAL,false)
    }
}
