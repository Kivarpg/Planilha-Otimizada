package com.example.data

import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro

/**
 * Auditor não mutante para a migração do roadmap. Ele não bloqueia o caminho
 * estável: detecta divergências entre o resultado produzido e o RulesEngine.
 */
internal object EncounterCanonicalRoadmapAudit {
    /**
     * Identidade mínima do conteúdo que a auditoria realmente consulta.
     * Permite ao runtime evitar repetir a mesma varredura diagnóstica quando
     * um +XP não alterou Encantos, requisitos ou contexto Lunar/Marcial.
     */
    fun fingerprint(npc: NpcEncontro): String = buildString {
        append(npc.tipoExaltado.name).append('|')
        append(npc.essencia).append('|')
        append(npc.abilities.toSortedMap()).append('|')
        append(npc.attributes.toSortedMap()).append('|')
        append(npc.charms.map { it.nome }.sorted()).append('|')
        append(npc.formaEspiritual).append('|')
        append(npc.formaEspiritualSecundaria).append('|')
        append(npc.estiloArtesMarciais).append('|')
        append(npc.estilosArtesMarciaisAdicionais.filter(String::isNotBlank).sorted())
    }

    data class Finding(
        val charmName: String,
        val contentId: String?,
        val eligibility: EncounterRulesEngine.Eligibility
    )

    fun auditFinalBuild(
        npc: NpcEncontro,
        catalog: PreparedEncounterCatalog
    ): List<Finding> {
        val spiritTraits = if (npc.tipoExaltado == TipoExaltadoEncontro.LUNAR) {
            LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritual) +
                LunarSpiritShapeArchetypeTraits.forDisplayName(npc.formaEspiritualSecundaria)
        } else emptySet()
        return auditFinalBuild(npc, catalog, spiritTraits)
    }

    internal fun auditFinalBuild(
        npc: NpcEncontro,
        catalog: PreparedEncounterCatalog,
        spiritTraits: Set<LunarSpiritTrait>
    ): List<Finding> {
        val state = EncounterRulesEngine.buildState(
            exaltType = npc.tipoExaltado,
            essence = npc.essencia,
            abilities = npc.abilities,
            attributes = npc.attributes,
            acquiredCharmNames = npc.charms.mapTo(linkedSetOf()) { it.nome },
            catalog = catalog,
            spiritTraits = spiritTraits,
            martialStyleIds = buildSet {
                npc.estiloArtesMarciais.takeIf(String::isNotBlank)?.let(::add)
                addAll(npc.estilosArtesMarciaisAdicionais.filter(String::isNotBlank))
            }
        )
        val nameMultiplicity = npc.charms.groupingBy { it.nome }.eachCount()
        return npc.charms.mapNotNull { charm ->
            // Aquisições repetíveis (ex.: Corpo de Touro) exigem estado
            // multiconjunto; não geramos falso positivo enquanto BuildState
            // ainda representa identidade adquirida como Set.
            if ((nameMultiplicity[charm.nome] ?: 0) > 1) return@mapNotNull null
            val refs = catalog.findByName(charm.nome).filter {
                when (npc.tipoExaltado) {
                    TipoExaltadoEncontro.SOLAR -> it.id.namespace == PreparedEncounterCatalog.NS_SOLAR
                    TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> it.id.namespace == PreparedEncounterCatalog.NS_DRAGON_BLOODED
                    TipoExaltadoEncontro.LUNAR -> it.id.namespace == PreparedEncounterCatalog.NS_LUNAR
                } || it.id.namespace == PreparedEncounterCatalog.NS_MARTIAL_CHARM
            }
            val ref = refs.singleOrNull() ?: return@mapNotNull null
            // Remova o próprio Encanto do estado para perguntar se a aquisição
            // seria legal naquele build, em vez de receber apenas Acquired.
            val before = state.copy(
                acquiredCharmIds = state.acquiredCharmIds - ref.id,
                acquiredCharmNames = state.acquiredCharmNames - charm.nome
            )
            val result = EncounterRulesEngine.evaluate(catalog.requirementGraph, ref.id, before)
            if (result is EncounterRulesEngine.Eligibility.Available) null
            else Finding(charm.nome, ref.id.value, result)
        }
    }
}
