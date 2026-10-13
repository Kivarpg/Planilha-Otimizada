package com.example.data

/** Independent traversal/evaluator: no RulesEngine.evaluate or planner calls.
 * Shares the catalog compiler, so parser correctness needs separate fixtures.
 * Proves that some legal acquisition order exists at the supplied final ratings;
 * does not prove historical XP prices or the ratings at each historical purchase.
 */
internal object EncounterAcquisitionOrderOracle {
    fun blocked(catalog: PreparedEncounterCatalog, finalState: EncounterRulesEngine.BuildState): Set<PreparedEncounterCatalog.StableContentId> {
        val pending = finalState.acquiredCharmIds.toMutableSet()
        val acquired = linkedSetOf<PreparedEncounterCatalog.StableContentId>()
        val names = linkedSetOf<String>()
        val counts = mutableMapOf<String, Int>()
        fun satisfies(requirement: EncounterRequirement): Boolean = when (requirement) {
            is EncounterRequirement.Essence -> finalState.essence >= requirement.minimum
            is EncounterRequirement.Ability -> (finalState.abilities[requirement.name] ?: 0) >= requirement.minimum
            is EncounterRequirement.Attribute -> (finalState.attributes[requirement.name] ?: 0) >= requirement.minimum
            is EncounterRequirement.Charm -> requirement.name in names
            is EncounterRequirement.CharmCount -> (if (requirement.category.isBlank()) acquired.size else
                counts.entries.firstOrNull { it.key.equals(requirement.category, true) }?.value ?: 0) >= requirement.minimum
            is EncounterRequirement.AnyOf -> requirement.alternatives.any(::satisfies)
            is EncounterRequirement.SpiritTrait -> requirement.trait in finalState.spiritTraits
            is EncounterRequirement.MartialStyle -> requirement.styleId in finalState.martialStyleIds
        }
        while (pending.isNotEmpty()) {
            val eligible = pending.filter { id ->
                catalog.requirementGraph.node(id)?.routes?.any { it.requirements.all(::satisfies) } == true
            }
            if (eligible.isEmpty()) break
            for (id in eligible) {
                pending.remove(id)
                acquired.add(id)
                catalog.find(id)?.let { names.add(it.nome) }
                for (category in catalog.countCategoriesByCharm[id].orEmpty()) counts[category] = (counts[category] ?: 0) + 1
            }
        }
        return pending
    }
}
