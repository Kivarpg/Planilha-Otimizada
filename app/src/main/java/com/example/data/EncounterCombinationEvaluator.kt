package com.example.data

/**
 * Modelo consolidado de realizabilidade.
 *
 * Objetivo: impedir que afinidades locais, variantes incompatíveis ou
 * configurações diferentes sejam somadas como se coexistissem.
 *
 * Não simula combate. Ele prova apenas que um conjunto de efeitos pode
 * coexistir numa configuração estrutural e janela declaradas.
 */
internal object EncounterCombinationEvaluator {

    enum class Timing { PASSIVE, SAME_ACTION, SAME_ROUND, CROSS_ROUND, BUILD_ONLY }

    data class ChoiceKey(val group: String, val option: String)

    data class Configuration(
        val facts: Set<String> = emptySet(),
        val knownFalseFacts:Set<String> = emptySet(),
        val choices: Map<String, String> = emptyMap(),
        val equipment: Set<String> = emptySet(),
        val states: Set<String> = emptySet(),
        /** Variáveis de identidade: ex. "protectedAlly" -> "ally#1". */
        val bindings: Map<EncounterBindingKey,EncounterParticipantId> = emptyMap()
    ) {
        fun withChoice(choice: ChoiceKey): Configuration? {
            val existing = choices[choice.group]
            if (existing != null && existing != choice.option) return null
            return copy(choices = choices + (choice.group to choice.option))
        }
    }

    data class EffectBranch(
        val id: String,
        val requirement: EncounterRequirementExpression =
            EncounterRequirementExpression.Always,
        val effects: List<EncounterMechanicalEffect>,
        val choices: Set<ChoiceKey> = emptySet(),
        val forbiddenFacts: Set<String> = emptySet(),
        val requiredEquipment: Set<String> = emptySet(),
        val requiredStates: Set<String> = emptySet(),
        /** Bindings concretos que este ramo produz/fixa. */
        val bindings: Map<EncounterBindingKey,EncounterParticipantId> = emptyMap(),
        /** Pares de variáveis que precisam apontar para o mesmo participante. */
        val participantConstraints:Set<EncounterParticipantBindings.Constraint> = emptySet()
    )

    data class PowerVariant(
        val id: String,
        val requirement: EncounterRequirementExpression =
            EncounterRequirementExpression.Always,
        val branches: List<EffectBranch>,
        val choices: Set<ChoiceKey> = emptySet()
    )

    data class Power(
        val id: String,
        val variants: List<PowerVariant>
    )

    data class RealizedEffect(
        val powerId: String,
        val variantId: String,
        val branchId: String,
        val effect: EncounterMechanicalEffect,
        val choices: Set<ChoiceKey>
    )

    data class Realization(
        val configuration: Configuration,
        val effects: List<RealizedEffect>,
        val inactiveConditionalEffectIds:Set<String> = emptySet()
    )

    data class Result(
        val realizable: Boolean,
        val realization: Realization?,
        val reason: String,
        val realizations: List<Realization> = listOfNotNull(realization)
    )

    /** Chave estrutural estável, usada sem serializações/toString no caminho quente. */
    private data class ConfigurationKey(
        val facts: Set<String>,
        val knownFalseFacts: Set<String>,
        val choices: Map<String, String>,
        val equipment: Set<String>,
        val states: Set<String>,
        val bindings: Map<EncounterBindingKey, EncounterParticipantId>
    )

    /** Preserva diferenças mecânicas entre ramos mesmo quando a configuração é igual. */
    private data class RealizationKey(
        val configuration: ConfigurationKey,
        val effects: List<String>,
        val inactiveConditionalEffectIds: Set<String>
    )

    private data class RequirementKey(
        val requirement: EncounterRequirementExpression,
        val facts: Set<String>,
        val knownFalseFacts: Set<String>
    )

    private fun configurationKey(c: Configuration) = ConfigurationKey(
        c.facts,
        c.knownFalseFacts,
        c.choices,
        c.equipment,
        c.states,
        c.bindings
    )

    private fun realizationStructuralKey(r: Realization) = RealizationKey(
        configurationKey(r.configuration),
        r.effects.map {
            "${it.powerId}/${it.variantId}/${it.branchId}/${it.effect.id}"
        }.sorted(),
        r.inactiveConditionalEffectIds
    )

    fun realize(
        powers: Collection<Power>,
        initial: Configuration
    ): Result {
        // Validação em passagem única, sem lista intermediária de IDs.
        val seenPowerIds = HashSet<String>()
        if (powers.any { it.id.isBlank() || !seenPowerIds.add(it.id) })
            return Result(false,null,"Poderes com ID vazio ou duplicado.",emptyList())
        for(power in powers) {
            val seenVariantIds = HashSet<String>()
            if (power.variants.any { it.id.isBlank() || !seenVariantIds.add(it.id) })
                return Result(false,null,"Variantes com ID vazio ou duplicado em ${power.id}.",emptyList())
            for(variant in power.variants) {
                val seenBranchIds = HashSet<String>()
                if (variant.branches.any { it.id.isBlank() || !seenBranchIds.add(it.id) })
                    return Result(false,null,"Ramos com ID vazio ou duplicado em ${power.id}/${variant.id}.",emptyList())
                if (variant.choices.any { it.group.isBlank() || it.option.isBlank() } ||
                    variant.branches.any { branch ->
                        branch.choices.any { it.group.isBlank() || it.option.isBlank() }
                    }
                ) return Result(false,null,"ChoiceKey vazio.",emptyList())
            }
        }
        if(initial.facts.intersect(initial.knownFalseFacts).isNotEmpty())
            return Result(false,null,"Fato simultaneamente TRUE e FALSE.",emptyList())
        if(initial.choices.any { it.key.isBlank() || it.value.isBlank() })
            return Result(false,null,"Configuração inicial contém escolha vazia.",emptyList())

        // Memoização local: uma execução não recalcula a mesma expressão para o mesmo estado factual.
        val requirementCache = HashMap<RequirementKey, EncounterTruth>()
        fun evaluate(
            requirement: EncounterRequirementExpression,
            configuration: Configuration
        ): EncounterTruth = requirementCache.getOrPut(
            RequirementKey(requirement, configuration.facts, configuration.knownFalseFacts)
        ) { requirement.evaluate(configuration.facts, configuration.knownFalseFacts) }

        var configurations = listOf(Realization(initial, emptyList()))

        for (power in powers) {
            // Deduplicação incremental: estados equivalentes nunca se acumulam numa lista intermediária.
            val next = LinkedHashMap<RealizationKey, Realization>()

            for (current in configurations) {
                for (variant in power.variants) {
                    if (evaluate(variant.requirement, current.configuration) != EncounterTruth.TRUE) continue
                    val afterVariant = applyChoices(current.configuration, variant.choices) ?: continue

                    for (branch in variant.branches) {
                        // Poda antecipada de incompatibilidades que escolhas do ramo não podem reparar.
                        if (branch.forbiddenFacts.any { it in afterVariant.facts }) continue
                        if (!afterVariant.equipment.containsAll(branch.requiredEquipment)) continue
                        if (!afterVariant.states.containsAll(branch.requiredStates)) continue
                        if (evaluate(branch.requirement, afterVariant) != EncounterTruth.TRUE) continue

                        val afterChoices = applyChoices(afterVariant, branch.choices) ?: continue
                        val afterBranch = applyBindings(
                            afterChoices, branch.bindings, branch.participantConstraints
                        ) ?: continue

                        val effects = ArrayList<RealizedEffect>(branch.effects.size)
                        val inactive = linkedSetOf<String>()
                        val realizedChoices = variant.choices + branch.choices
                        for (effect in branch.effects) {
                            if (evaluate(effect.requirement, afterBranch) == EncounterTruth.TRUE) {
                                effects += RealizedEffect(
                                    powerId = power.id,
                                    variantId = variant.id,
                                    branchId = branch.id,
                                    effect = effect,
                                    choices = realizedChoices
                                )
                            } else {
                                inactive += "${power.id}/${variant.id}/${branch.id}/${effect.id}"
                            }
                        }
                        val realization = Realization(
                            afterBranch,
                            current.effects + effects,
                            current.inactiveConditionalEffectIds + inactive
                        )
                        next.putIfAbsent(realizationStructuralKey(realization), realization)
                    }
                }
            }

            if (next.isEmpty()) {
                return Result(
                    false,
                    null,
                    "Nenhuma configuração consistente realiza todos os poderes solicitados."
                )
            }
            configurations = next.values.toList()
        }

        // Não declarar "melhor" realização por quantidade de efeitos.
        // O chamador recebe todas as alternativas; `realization` é apenas
        // compatibilidade com a API antiga e usa ordem determinística.
        val ordered = configurations.sortedBy { realizationKey(it) }
        val first = ordered.firstOrNull()
        return Result(
            first != null,
            first,
            if (first != null) "Existe configuração estrutural consistente." else "Sem realização.",
            ordered
        )
    }

    private fun realizationKey(r: Realization): String =
        listOf(
            r.configuration.choices.toSortedMap().toString(),
            r.configuration.equipment.sorted().toString(),
            r.configuration.states.sorted().toString(),
            r.configuration.facts.sorted().toString(),
            r.configuration.knownFalseFacts.sorted().toString(),
            r.configuration.bindings.entries.sortedBy { e -> e.key.value }.joinToString { e -> "${e.key.value}=${e.value.value}" },
            r.effects.map { "${it.powerId}/${it.variantId}/${it.branchId}/${it.effect.id}" }
                .sorted().toString(),
            r.inactiveConditionalEffectIds.sorted().toString()
        ).joinToString("|")

    private fun applyBindings(
        base:Configuration,
        assignments:Map<EncounterBindingKey,EncounterParticipantId>,
        constraints:Set<EncounterParticipantBindings.Constraint>
    ):Configuration? {
        if (assignments.isEmpty() && constraints.isEmpty()) return base
        val map=base.bindings.toMutableMap()
        for((key,value) in assignments) {
            val existing=map[key]
            if(existing!=null && existing!=value) return null
            map[key]=value
        }
        for(constraint in constraints) {
            if(EncounterParticipantBindings.check(map,constraint)
                != EncounterParticipantBindings.Check.SATISFIED) return null
        }
        return if (assignments.isEmpty()) base else base.copy(bindings=map)
    }

    private fun applyChoices(
        base: Configuration,
        choices: Set<ChoiceKey>
    ): Configuration? {
        if (choices.isEmpty()) return base
        val updated=base.choices.toMutableMap()
        for (choice in choices) {
            val existing=updated[choice.group]
            if (existing != null && existing != choice.option) return null
            updated[choice.group]=choice.option
        }
        return base.copy(choices=updated)
    }
}

/**
 * Evidência deduplicável de sinergia.
 * O score não deve somar repetidamente a mesma relação causal apenas
 * porque ela reaparece em par, rota, identidade e cobertura.
 */
internal data class EncounterSynergyEvidence(
    val mechanic: String,
    val producerPowerId: String?,
    val consumerPowerId: String?,
    val producerEffectId: String?,
    val consumerEffectId: String?,
    val timing: EncounterCombinationEvaluator.Timing,
    val causalKey: String,
    val value: Int
)

internal object EncounterSynergyEvidenceReducer {
    fun reduce(evidence: Collection<EncounterSynergyEvidence>): List<EncounterSynergyEvidence> =
        evidence
            .groupBy { it.causalKey }
            .values
            .map { group ->
                // A mesma causa não soma várias vezes. Preserva a evidência
                // de maior valor demonstrado.
                group.maxBy { it.value }
            }
}
