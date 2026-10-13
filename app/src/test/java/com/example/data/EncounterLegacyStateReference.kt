package com.example.data

import com.example.model.TipoExaltadoEncontro

/** Frozen pre-refactor implementation for differential testing; never used in production. */
internal object EncounterLegacyStateReference {
    fun legacyBuildState(
        exaltType: TipoExaltadoEncontro,
        essence: Int,
        abilities: Map<String, Int> = emptyMap(),
        attributes: Map<String, Int> = emptyMap(),
        acquiredCharmNames: Set<String> = emptySet(),
        catalog: PreparedEncounterCatalog,
        spiritTraits: Set<LunarSpiritTrait> = emptySet(),
        martialStyleIds: Set<String> = emptySet()
    ): EncounterRulesEngine.BuildState {
        val ids = acquiredCharmNames.asSequence()
            .flatMap { catalog.findByName(it).asSequence() }
            .filter {
                it.kind == PreparedEncounterCatalog.ContentKind.CHARM ||
                    it.kind == PreparedEncounterCatalog.ContentKind.MARTIAL_CHARM
            }
            .map { it.id }
            .toSet()

        val acquiredRefs = acquiredCharmNames.asSequence()
            .flatMap { catalog.findByName(it).asSequence() }
            .filter {
                it.kind == PreparedEncounterCatalog.ContentKind.CHARM ||
                    it.kind == PreparedEncounterCatalog.ContentKind.MARTIAL_CHARM
            }
            .distinctBy { it.id }
            .toList()

        val countKeys = mutableListOf<String>()
        acquiredRefs.forEach { ref ->
            when (ref.id.namespace) {
                PreparedEncounterCatalog.NS_SOLAR -> {
                    catalog.solares.firstOrNull { it.id == ref.id.localId }?.let { def ->
                        countKeys += def.habilidade
                        if (def.minEssencia >= 2) countKeys += "${def.habilidade} de Essência 2+"
                        if (def.minEssencia >= 3) countKeys += "Essência 3+ de ${def.habilidade}"
                        val compositeCategories = listOf(
                            setOf("Performance", "Presença", "Socialização") to "Performance, Presença ou Socialização",
                            setOf("Burocracia", "Presença", "Guerra") to "Burocracia, Presença ou Guerra",
                            setOf("Armas Brancas", "Arqueirismo", "Arremesso", "Briga", "Guerra") to
                                "Armas brancas, Arqueirismo, Arremesso, Briga ou Guerra",
                            setOf("Burocracia", "Performance", "Socialização") to "Burocracia, Performance ou Socialização",
                            setOf("Conhecimento", "Presença", "Socialização") to "Conhecimento, Presença ou Socialização",
                            setOf("Atletismo", "Resistência", "Sobrevivência") to "Atletismo, Resistência ou Sobrevivência"
                        )
                        compositeCategories.forEach { (members, label) ->
                            if (def.habilidade in members) countKeys += label
                        }
                        if (def.habilidade in setOf("Burocracia", "Integridade", "Performance", "Presença", "Socialização")) {
                            countKeys += "sociais"
                        }
                    }
                }
                PreparedEncounterCatalog.NS_DRAGON_BLOODED ->
                    catalog.sangueDeDragao.firstOrNull { it.id == ref.id.localId }?.let { countKeys += it.habilidade }
                PreparedEncounterCatalog.NS_LUNAR ->
                    catalog.lunares.firstOrNull { it.id == ref.id.localId }?.let { def ->
                        countKeys += def.atributo
                        if (def.atributo in com.example.model.ExaltedConstants.MENTAL_ATTRIBUTES) {
                            countKeys += "Atributo Mental"
                        }
                    }
            }
        }
        val counts = countKeys.groupingBy { it }.eachCount()

        return EncounterRulesEngine.BuildState(
            exaltType = exaltType,
            essence = essence,
            abilities = abilities,
            attributes = attributes,
            acquiredCharmNames = acquiredCharmNames,
            acquiredCharmIds = ids,
            spiritTraits = spiritTraits,
            martialStyleIds = martialStyleIds,
            charmCountByCategory = counts
        )
    }

}
