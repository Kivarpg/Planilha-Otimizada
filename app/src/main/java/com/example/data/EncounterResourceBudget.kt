package com.example.data

/**
 * Orçamento estrutural de recursos.
 *
 * Não simula combate, frequência de uso ou ordem de ativação.
 * Mede apenas se uma seleção de poderes concentra custos incompatíveis
 * ou depende de recursos que a construção não possui/sustenta.
 */
internal object EncounterResourceBudget {

    enum class Resource {
        MOTES,
        SORCEROUS_MOTES,
        WILLPOWER,
        INITIATIVE,
        ANIMA,
        AURA,
        HEALTH,
        OTHER
    }

    enum class FlowKind {
        SPEND,          // custo pago e perdido
        COMMIT,         // indisponível enquanto comprometido
        GENERATE,       // produz recurso
        TRANSFER,       // move recurso, não cria
        CONVERT,        // transforma um recurso em outro
        REQUIRE_MINIMUM // limiar necessário, não custo
    }

    data class Flow(
        val id: String,
        val resource: Resource,
        val kind: FlowKind,
        val amount: Int? = null,
        val targetResource: Resource? = null,
        val requirement: EncounterRequirementExpression =
            EncounterRequirementExpression.Always,
        val confidence: Double = 1.0
    )

    data class Capacity(
        val resource: Resource,
        val available: Int? = null,
        val reservable: Int? = null
    )

    data class Profile(
        val flows: List<Flow>,
        val capacities: Map<Resource, Capacity> = emptyMap()
    )

    data class Assessment(
        val impossible: Boolean,
        val unsupportedResources: Set<Resource>,
        val overcommittedResources: Set<Resource>,
        val concentratedResources: Set<Resource>,
        val unknownQuantities: Set<Resource>,
        val reasons: List<String>
    )

    fun assess(profile: Profile, facts: Set<String> = emptySet()): Assessment {
        val active = profile.flows.filter {
            it.requirement.isSatisfiedBy(facts)
        }

        val unsupported = mutableSetOf<Resource>()
        val overcommitted = mutableSetOf<Resource>()
        val concentrated = mutableSetOf<Resource>()
        val unknown = mutableSetOf<Resource>()
        val reasons = mutableListOf<String>()

        for (resource in Resource.entries) {
            val flows = active.filter { it.resource == resource }
            if (flows.isEmpty()) continue

            val spend = flows.filter { it.kind == FlowKind.SPEND }
            val commit = flows.filter { it.kind == FlowKind.COMMIT }
            val generators = flows.filter { it.kind == FlowKind.GENERATE }
            val requirements = flows.filter { it.kind == FlowKind.REQUIRE_MINIMUM }

            val cap = profile.capacities[resource]

            // Não comparar recursos de namespaces diferentes.
            if ((spend.isNotEmpty() || commit.isNotEmpty() || requirements.isNotEmpty()) &&
                cap == null &&
                generators.isEmpty()
            ) {
                unsupported += resource
                reasons += "$resource possui demanda, mas nenhuma capacidade/suporte estrutural conhecido."
            }

            val commitKnown = commit.mapNotNull { it.amount }.sum()
            if (commit.any { it.amount == null }) unknown += resource
            if (cap?.reservable != null && commitKnown > cap.reservable) {
                overcommitted += resource
                reasons += "$resource está comprometido acima da capacidade reservável conhecida."
            }

            // Concentração é sinal de tensão, não impossibilidade.
            val consumers = (spend + commit).map { it.id }.distinct()
            if (consumers.size >= 3) {
                concentrated += resource
                reasons += "$resource concentra custos em múltiplas aquisições."
            }

            if (spend.any { it.amount == null } || requirements.any { it.amount == null }) {
                unknown += resource
            }
        }

        return Assessment(
            impossible = overcommitted.isNotEmpty(),
            unsupportedResources = unsupported,
            overcommittedResources = overcommitted,
            concentratedResources = concentrated,
            unknownQuantities = unknown,
            reasons = reasons
        )
    }
}
