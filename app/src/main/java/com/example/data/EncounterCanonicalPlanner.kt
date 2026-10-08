package com.example.data

/**
 * Liga RulesEngine -> BuildPlanner -> ECS.
 *
 * Legalidade vem exclusivamente do grafo. A pontuação continua no ECS.
 * O texto original pode continuar sendo usado pelos avaliadores maduros até
 * que todos os efeitos estejam tipados; não há mudança silenciosa de score.
 */
internal object EncounterCanonicalPlanner {
    fun planCombatCharms(
        catalog: PreparedEncounterCatalog,
        initial: EncounterRulesEngine.BuildState,
        candidateIds: List<PreparedEncounterCatalog.StableContentId>,
        selectedTexts: List<String>,
        maxSteps: Int,
        fingerprint: String
    ): EncounterBuildPlanner.Plan {
        val selectedTags = selectedTexts.map(EncounterCombatSynergy::tags)
        val uniqueCandidateIds = candidateIds.distinct()
        val candidateSet = uniqueCandidateIds.toSet()

        // Estes dados são imutáveis durante toda a busca. Antes eram
        // reconstruídos dentro de cada avaliação de cada estado do beam.
        val candidateTags = uniqueCandidateIds.associateWith { id ->
            catalog.requirementGraph.node(id)?.let {
                EncounterCombatSynergy.tagsFromCanonical(it.mechanicalTags)
            }.orEmpty()
        }
        val candidateNames = uniqueCandidateIds.associateWith { id -> catalog.find(id)?.nome }
        // O score consulta repetidamente o mesmo conjunto adquirido em vários
        // candidatos do mesmo estado do beam. Memorizar por conjunto canônico
        // evita reconstruir plannedTags N vezes sem alterar ordem ou conteúdo.
        val plannedTagsByAcquiredIds =
            HashMap<Set<PreparedEncounterCatalog.StableContentId>, List<Set<EncounterCombatSynergy.Tag>>>()

        val candidates = uniqueCandidateIds.map { id ->
            EncounterBuildPlanner.Candidate(
                id = id,
                score = { build, _ ->
                    val tags = candidateTags.getValue(id)
                    val plannedTags = plannedTagsByAcquiredIds.getOrPut(build.acquiredCharmIds) {
                        build.acquiredCharmIds.asSequence()
                            .filter { it in candidateSet }
                            .mapNotNull(candidateTags::get)
                            .filter { it.isNotEmpty() }
                            .toList()
                    }
                    EncounterCombatSynergy.scoreTags(tags, selectedTags + plannedTags)
                },
                futureScore = { build, _ ->
                    // Continuidade futura precisa simular exatamente o mesmo
                    // estado que o planner cria ao adquirir o Encanto. Alguns
                    // pré-requisitos históricos ainda são resolvidos por nome;
                    // copiar só o ID subestimava árvores A -> B nesses casos.
                    val acquiredName = candidateNames[id]
                    val after = build.copy(
                        acquiredCharmIds = build.acquiredCharmIds + id,
                        acquiredCharmNames = acquiredName?.let {
                            build.acquiredCharmNames + it
                        } ?: build.acquiredCharmNames
                    )
                    catalog.directDependentsOf(id).asSequence()
                        .filter { it in candidateSet && it !in build.acquiredCharmIds }
                        .count { dependentId ->
                            val before = EncounterRulesEngine.evaluate(
                                catalog.requirementGraph,
                                dependentId,
                                build
                            )
                            before !is EncounterRulesEngine.Eligibility.Available &&
                                EncounterRulesEngine.evaluate(
                                    catalog.requirementGraph,
                                    dependentId,
                                    after
                                ) is EncounterRulesEngine.Eligibility.Available
                        }
                }
            )
        }
        return EncounterBuildPlanner.plan(
            catalog = catalog,
            initial = initial,
            candidates = candidates,
            maxSteps = maxSteps,
            fingerprint = fingerprint
        )
    }
}
