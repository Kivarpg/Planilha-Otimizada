package com.example.data

/** Independent traversal/evaluator: no RulesEngine.evaluate or planner calls.
 * Shares the catalog compiler, so parser correctness needs separate fixtures.
 * Proves that some legal acquisition order exists at the supplied final ratings;
 * does not prove historical XP prices or the ratings at each historical purchase.
 */
internal object EncounterAcquisitionOrderOracle {
    fun blocked(npc: com.example.model.NpcEncontro, catalog: PreparedEncounterCatalog, depths: MutableMap<String, Int>? = null): Set<PreparedEncounterCatalog.StableContentId> {
        val namespace = when (npc.tipoExaltado) {
            com.example.model.TipoExaltadoEncontro.SOLAR -> PreparedEncounterCatalog.NS_SOLAR
            com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> PreparedEncounterCatalog.NS_DRAGON_BLOODED
            com.example.model.TipoExaltadoEncontro.LUNAR -> PreparedEncounterCatalog.NS_LUNAR
        }
        val names = npc.charms.map { it.nome }.toSet()
        val ids = names.flatMap { catalog.findByName(it) }.filter {
            it.id.namespace == namespace || it.id.namespace == PreparedEncounterCatalog.NS_MARTIAL_CHARM
        }.map { it.id }.toSet()
        val traits = npc.lunarArchetypeTraits.mapNotNull { runCatching { LunarSpiritTrait.valueOf(it) }.getOrNull() }
            .toSet().ifEmpty { (LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritual) +
                LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritualSecundaria)).toSet() }
        val styles = (listOf(npc.estiloArtesMarciais) + npc.estilosArtesMarciaisAdicionais).toSet()
        return blocked(catalog, EncounterRulesEngine.BuildState(npc.tipoExaltado, npc.essencia,
            abilities = npc.abilities, attributes = npc.attributes, acquiredCharmNames = names,
            acquiredCharmIds = ids, spiritTraits = traits,
            martialStyleIds = catalog.estilosMarciais.filter { it.nomePt in styles || it.nomeEn in styles }.map { it.id }.toSet()), depths)
    }

    fun blocked(catalog: PreparedEncounterCatalog, finalState: EncounterRulesEngine.BuildState, depths: MutableMap<String, Int>? = null): Set<PreparedEncounterCatalog.StableContentId> {
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
        fun prerequisiteDepth(req: EncounterRequirement): Int = when (req) {
            is EncounterRequirement.Charm -> depths?.get(req.name) ?: 0
            is EncounterRequirement.AnyOf -> req.alternatives.filter(::satisfies).minOfOrNull(::prerequisiteDepth) ?: 0
            else -> 0 // Aggregate counts do not identify a unique named chain.
        }
        while (pending.isNotEmpty()) {
            val eligible = pending.filter { id ->
                catalog.requirementGraph.node(id)?.routes?.any { it.requirements.all(::satisfies) } == true
            }
            if (eligible.isEmpty()) break
            for (id in eligible) {
                if (depths != null) {
                    val level = catalog.requirementGraph.node(id)!!.routes.filter { it.requirements.all(::satisfies) }
                        .minOf { route -> (route.requirements.maxOfOrNull(::prerequisiteDepth) ?: 0) + 1 }
                    catalog.find(id)?.let { depths[it.nome] = level }
                }
                pending.remove(id)
                acquired.add(id)
                catalog.find(id)?.let { names.add(it.nome) }
                for (category in catalog.countCategoriesFor(id)) counts[category] = (counts[category] ?: 0) + 1
            }
        }
        return pending
    }
}
