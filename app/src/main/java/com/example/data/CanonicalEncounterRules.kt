package com.example.data

import com.example.model.TipoExaltadoEncontro

/**
 * Etapa 5 — vocabulário tipado compartilhado por catálogo, regras, ECS e planner.
 * Nesta rodada tipamos requisitos de aquisição. Efeitos mecânicos são deliberadamente
 * limitados aos conceitos que os avaliadores já usam; texto de regras continua preservado.
 */
internal sealed interface EncounterRequirement {
    data class Essence(val minimum: Int) : EncounterRequirement
    data class Ability(val name: String, val minimum: Int) : EncounterRequirement
    data class Attribute(val name: String, val minimum: Int) : EncounterRequirement
    data class Charm(val name: String) : EncounterRequirement
    data class CharmCount(val category: String, val minimum: Int) : EncounterRequirement
    /** Pelo menos uma alternativa deve ser satisfeita. */
    data class AnyOf(val alternatives: List<EncounterRequirement>) : EncounterRequirement
    data class SpiritTrait(val trait: LunarSpiritTrait) : EncounterRequirement
    data class MartialStyle(val styleId: String) : EncounterRequirement
}

internal enum class EncounterMechanicalTag {
    WITHERING, DECISIVE, CLASH, CRASH, INITIATIVE_GAIN, INITIATIVE_DRAIN,
    DEFENSE, ACCURACY, DAMAGE, REROLL, COST_REDUCTION, SOCIAL, MENTAL
}

internal data class EncounterAcquisitionRoute(
    val routeId: String,
    val requirements: List<EncounterRequirement>,
    val archetype: Boolean = false
)

internal data class EncounterRuleNode(
    val id: PreparedEncounterCatalog.StableContentId,
    val routes: List<EncounterAcquisitionRoute>,
    val mechanicalTags: Set<EncounterMechanicalTag> = emptySet()
)

/** Grafo imutável: dependências de aquisição compiladas uma vez por catálogo. */
internal class EncounterRequirementGraph private constructor(
    val nodes: Map<PreparedEncounterCatalog.StableContentId, EncounterRuleNode>
) {
    fun node(id: PreparedEncounterCatalog.StableContentId): EncounterRuleNode? = nodes[id]

    companion object {
        fun compile(catalog: PreparedEncounterCatalog): EncounterRequirementGraph {
            val nodes = LinkedHashMap<PreparedEncounterCatalog.StableContentId, EncounterRuleNode>()

            catalog.solares.forEach { def ->
                val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_SOLAR, def.id)
                nodes[id] = EncounterRuleNode(
                    id,
                    listOf(
                        EncounterAcquisitionRoute(
                            "normal",
                            baseRequirements(def.minEssencia, EncounterRequirement.Ability(def.habilidade, def.minHabilidade), def.preRequisitos)
                        )
                    ),
                    tags(def.tipo, def.palavrasChave, def.descricao)
                )
            }
            catalog.sangueDeDragao.forEach { def ->
                val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_DRAGON_BLOODED, def.id)
                nodes[id] = EncounterRuleNode(
                    id,
                    listOf(
                        EncounterAcquisitionRoute(
                            "normal",
                            baseRequirements(def.minEssencia, EncounterRequirement.Ability(def.habilidade, def.minHabilidade), def.preRequisitos)
                        )
                    ),
                    tags(def.tipo, def.palavrasChave, def.descricao)
                )
            }
            catalog.lunares.forEach { def ->
                val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_LUNAR, def.id)
                val routes = buildList {
                    add(
                        EncounterAcquisitionRoute(
                            "normal",
                            baseRequirements(def.minEssencia, EncounterRequirement.Attribute(def.atributo, def.minAtributo), def.preRequisitos)
                        )
                    )
                    def.rotasArquetipo.forEachIndexed { index, route ->
                        val traitRequirements = spiritRequirements(route.condicao)
                        // Condição desconhecida NÃO vira rota livre: a rota é omitida.
                        if (traitRequirements != null) {
                            add(
                                EncounterAcquisitionRoute(
                                    "archetype:$index",
                                    baseRequirements(
                                        def.minEssencia,
                                        EncounterRequirement.Attribute(route.atributo, route.minAtributo),
                                        route.preRequisitosAlternativos
                                            .takeUnless { it.isBlank() || it.equals("Nenhum", ignoreCase = true) }
                                            ?: def.preRequisitos
                                    ) + traitRequirements,
                                    archetype = true
                                )
                            )
                        }
                    }
                }
                nodes[id] = EncounterRuleNode(id, routes, tags(def.tipo, def.palavrasChave, def.descricao))
            }
            catalog.estilosMarciais.forEach { estilo ->
                estilo.encantos.forEach { def ->
                    val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_MARTIAL_CHARM, def.id)
                    nodes[id] = EncounterRuleNode(
                        id,
                        listOf(
                            EncounterAcquisitionRoute(
                                "martial:${estilo.id}",
                                baseRequirements(
                                    def.minEssencia,
                                    EncounterRequirement.Ability(def.habilidade, def.minHabilidade),
                                    def.preRequisitos
                                ) + EncounterRequirement.MartialStyle(estilo.id)
                            )
                        ),
                        tags(def.tipo, def.palavrasChave, def.descricao)
                    )
                }
            }
            return EncounterRequirementGraph(nodes.toMap())
        }

        private fun baseRequirements(
            essence: Int,
            rating: EncounterRequirement,
            prerequisites: String
        ): List<EncounterRequirement> =
            buildList {
                add(EncounterRequirement.Essence(essence))
                add(rating)
                addAll(EncounterPrerequisiteParser.parse(prerequisites))
            }

        private fun spiritRequirements(condition: String): List<EncounterRequirement>? {
            fun one(t: LunarSpiritTrait) = listOf(EncounterRequirement.SpiritTrait(t))
            return when (condition) {
                "MINUSCULO" -> one(LunarSpiritTrait.MINUSCULO)
                "TAMANHO_LENDARIO" -> one(LunarSpiritTrait.TAMANHO_LENDARIO)
                "PREDATORIO" -> one(LunarSpiritTrait.PREDATORIO)
                "CACA_EM_GRUPO" -> one(LunarSpiritTrait.CACA_EM_GRUPO)
                "MIGRATORIO" -> one(LunarSpiritTrait.MIGRATORIO)
                "VISAO_APRIMORADA" -> one(LunarSpiritTrait.VISAO_APRIMORADA)
                "VISAO_NOTURNA" -> one(LunarSpiritTrait.VISAO_NOTURNA)
                "CAMUFLAGEM" -> one(LunarSpiritTrait.CAMUFLAGEM)
                "VENENOSO" -> one(LunarSpiritTrait.VENENOSO)
                "RESPIRA_AGUA" -> one(LunarSpiritTrait.RESPIRA_AGUA)
                "VIVE_EM_COLMEIA" -> one(LunarSpiritTrait.VIVE_EM_COLMEIA)
                "FURIA" -> one(LunarSpiritTrait.FURIA)
                "BESTA_DE_CARGA" -> one(LunarSpiritTrait.BESTA_DE_CARGA)
                "AMEACA_OU_INTIMIDACAO" -> one(LunarSpiritTrait.AMEACA_OU_INTIMIDACAO)
                "IMITA_APARENCIA" -> one(LunarSpiritTrait.IMITA_APARENCIA)
                "IMITA_SONS" -> one(LunarSpiritTrait.IMITA_SONS)
                "ESCALADOR_ADERENTE" -> one(LunarSpiritTrait.ESCALADOR_ADERENTE)
                "FINGE_MORTE" -> one(LunarSpiritTrait.FINGE_MORTE)
                "CARNICEIRO" -> one(LunarSpiritTrait.CARNICEIRO)
                "PRESA_E_MINUSCULO" -> listOf(
                    EncounterRequirement.SpiritTrait(LunarSpiritTrait.PRESA),
                    EncounterRequirement.SpiritTrait(LunarSpiritTrait.MINUSCULO)
                )
                else -> null
            }
        }

        private fun tags(type: String, keywords: String, description: String): Set<EncounterMechanicalTag> {
            val source = "$type $keywords $description".lowercase()
            return buildSet {
                if ("fulminante" in source || "withering" in source) add(EncounterMechanicalTag.WITHERING)
                if ("decisiv" in source || "decisive" in source) add(EncounterMechanicalTag.DECISIVE)
                if ("colisão" in source || "clash" in source) add(EncounterMechanicalTag.CLASH)
                if ("atordoamento" in source || "crash" in source) add(EncounterMechanicalTag.CRASH)
                if (Regex("(?:ganha|ganhe|ganhar|recebe|receba|adiciona|aumenta|recupera)[^.!?;]{0,45}iniciativa").containsMatchIn(source) ||
                    Regex("iniciativa[^.!?;]{0,35}(?:adicional|b[oô]nus|ganha|recebe|aumenta)").containsMatchIn(source)
                ) add(EncounterMechanicalTag.INITIATIVE_GAIN)
                if (Regex("(?:perde|perca|reduz|reduza|remove|remova|drena|drene|rouba|roube|retira|subtrai)[^.!?;]{0,45}iniciativa").containsMatchIn(source) ||
                    Regex("iniciativa[^.!?;]{0,35}(?:perdida|reduzida|removida|drenada|roubada)").containsMatchIn(source)
                ) add(EncounterMechanicalTag.INITIATIVE_DRAIN)
                if ("defesa" in source || "defense" in source) add(EncounterMechanicalTag.DEFENSE)
                if ("precisão" in source || "accuracy" in source) add(EncounterMechanicalTag.ACCURACY)
                if ("dano" in source || "damage" in source) add(EncounterMechanicalTag.DAMAGE)
                if ("reroll" in source || "rerrol" in source) add(EncounterMechanicalTag.REROLL)
                if ("influência" in source || "influence" in source) add(EncounterMechanicalTag.SOCIAL)
            }
        }
    }
}

/**
 * Parser canônico compatível com a semântica histórica do
 * EncounterCharmSelectionService. Não tenta "adivinhar" requisitos desconhecidos.
 */
internal object EncounterPrerequisiteParser {
    private const val MAX_PARSED_CACHE = 2048
    private val parsedCache = object : LinkedHashMap<String, List<EncounterRequirement>>(256, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<EncounterRequirement>>?
        ): Boolean = size > MAX_PARSED_CACHE
    }
    private val orPattern = Regex("""(?i)\s+ou\s+""")
    private val countAlternativePattern = Regex("""(?i)\s+ou\s+(?=quaisquer\s+)""")
    private val countPattern = Regex(
        """(?i)^quaisquer\s+(\p{L}+|\d+)\s+encant(?:o|os|amento|amentos)(?:\s+(?:de\s+)?(.+))?$"""
    )
    private val numbers = mapOf(
        "um" to 1, "uma" to 1, "dois" to 2, "duas" to 2, "três" to 3,
        "quatro" to 4, "cinco" to 5, "seis" to 6, "sete" to 7, "oito" to 8,
        "nove" to 9, "dez" to 10, "quinze" to 15
    )

    private fun atomic(text: String): EncounterRequirement {
        val part = text.trim()
        val match = countPattern.find(part)
        if (match != null) {
            val raw = match.groupValues[1].lowercase()
            val count = raw.toIntOrNull() ?: numbers[raw]
            if (count != null) {
                // Categoria vazia significa "quaisquer N Encantos".
                return EncounterRequirement.CharmCount(match.groupValues[2].trim(), count)
            }
        }
        return EncounterRequirement.Charm(part)
    }

    private fun group(text: String): EncounterRequirement {
        val trimmed = text.trim()
        // Em "quaisquer N Encantos de A, B ou C", o "ou" pertence ao nome
        // composto da categoria. Já "Encanto X ou quaisquer N Encantos..."
        // representa alternativas de aquisição.
        val countAlternative = countAlternativePattern
        val alternatives = if (countAlternative.containsMatchIn(trimmed)) {
            trimmed.split(countAlternative)
        } else if (countPattern.matches(trimmed)) {
            listOf(trimmed)
        } else {
            trimmed.split(orPattern)
        }
        val parsed = alternatives.map(String::trim).filter(String::isNotBlank).map(::atomic)
        return if (parsed.size == 1) parsed.single() else EncounterRequirement.AnyOf(parsed)
    }

    fun parse(text: String): List<EncounterRequirement> {
        val trimmed = text.trim()
        if (trimmed.isBlank() || trimmed.equals("Nenhum", ignoreCase = true)) return emptyList()
        return synchronized(parsedCache) {
            parsedCache[trimmed] ?: parseUncached(trimmed).also { parsedCache[trimmed] = it }
        }
    }

    private fun parseUncached(trimmed: String): List<EncounterRequirement> {

        // "Nenhum ou X" preserva a semântica literal: existe uma rota sem
        // pré-requisito, portanto o requisito inteiro é opcional.
        if (trimmed.split(orPattern)
                .any { it.trim().equals("Nenhum", ignoreCase = true) }) {
            return emptyList()
        }

        // ';' é conjunção explícita entre grupos. Vírgulas históricas também
        // separam requisitos AND, exceto quando pertencem à categoria textual
        // de um CharmCount ("A, B ou C"), que deve permanecer uma categoria.
        val semicolonGroups = trimmed.split(';').map(String::trim).filter(String::isNotBlank)
        return semicolonGroups.flatMap { segment ->
            val countAlternative = countAlternativePattern
            val altMatch = countAlternative.find(segment)
            if (altMatch != null) {
                val left = segment.substring(0, altMatch.range.first).trim()
                val right = segment.substring(altMatch.range.last + 1).trim()
                val leftParts = left.split(',').map(String::trim).filter(String::isNotBlank)
                if (leftParts.size > 1) {
                    leftParts.dropLast(1).map(::group) +
                        EncounterRequirement.AnyOf(listOf(atomic(leftParts.last()), atomic(right)))
                } else {
                    listOf(group(segment))
                }
            } else if (countPattern.matches(segment.trim())) {
                listOf(atomic(segment))
            } else {
                segment.split(',').map(String::trim).filter(String::isNotBlank).map(::group)
            }
        }
    }
}

/** Uma única resposta estruturada para UI, ECS e planner. */
internal object EncounterRulesEngine {
    data class BuildState(
        val exaltType: TipoExaltadoEncontro,
        val essence: Int,
        val abilities: Map<String, Int> = emptyMap(),
        val attributes: Map<String, Int> = emptyMap(),
        val acquiredCharmNames: Set<String> = emptySet(),
        val acquiredCharmIds: Set<PreparedEncounterCatalog.StableContentId> = emptySet(),
        val spiritTraits: Set<LunarSpiritTrait> = emptySet(),
        val martialStyleIds: Set<String> = emptySet(),
        val charmCountByCategory: Map<String, Int> = emptyMap()
    )

    sealed interface Eligibility {
        data object Acquired : Eligibility
        data class Available(val routes: List<EncounterAcquisitionRoute>) : Eligibility
        data class Locked(val routes: List<RouteFailure>) : Eligibility
        data class Invalid(val reason: String) : Eligibility
    }

    data class RouteFailure(
        val routeId: String,
        val missing: List<EncounterRequirement>
    )


    /**
     * Constrói o estado compartilhado a partir dos dados que os seletores já
     * possuem. Centraliza normalização e contagem para evitar adaptadores
     * diferentes em Solar/Terrestre/Lunar durante a migração.
     */
    fun buildState(
        exaltType: TipoExaltadoEncontro,
        essence: Int,
        abilities: Map<String, Int> = emptyMap(),
        attributes: Map<String, Int> = emptyMap(),
        acquiredCharmNames: Set<String> = emptySet(),
        catalog: PreparedEncounterCatalog,
        spiritTraits: Set<LunarSpiritTrait> = emptySet(),
        martialStyleIds: Set<String> = emptySet()
    ): BuildState {
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

        return BuildState(
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

    fun evaluate(
        graph: EncounterRequirementGraph,
        id: PreparedEncounterCatalog.StableContentId,
        state: BuildState
    ): Eligibility {
        if (id in state.acquiredCharmIds) return Eligibility.Acquired
        val node = graph.node(id) ?: return Eligibility.Invalid("Conteúdo sem nó de regras: $id")
        val allowedNamespace = when (state.exaltType) {
            TipoExaltadoEncontro.SOLAR -> PreparedEncounterCatalog.NS_SOLAR
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> PreparedEncounterCatalog.NS_DRAGON_BLOODED
            TipoExaltadoEncontro.LUNAR -> PreparedEncounterCatalog.NS_LUNAR
        }
        if (id.namespace != allowedNamespace && id.namespace != PreparedEncounterCatalog.NS_MARTIAL_CHARM) {
            return Eligibility.Invalid("Conteúdo incompatível com ${state.exaltType}")
        }

        val failures = node.routes.map { route ->
            RouteFailure(route.routeId, route.requirements.filterNot { satisfied(it, state) })
        }
        val availableIds = failures.filter { it.missing.isEmpty() }.map { it.routeId }.toSet()
        val available = node.routes.filter { it.routeId in availableIds }
        return if (available.isNotEmpty()) Eligibility.Available(available) else Eligibility.Locked(failures)
    }

    private fun satisfied(requirement: EncounterRequirement, state: BuildState): Boolean = when (requirement) {
        is EncounterRequirement.Essence -> state.essence >= requirement.minimum
        is EncounterRequirement.Ability -> (state.abilities[requirement.name] ?: 0) >= requirement.minimum
        is EncounterRequirement.Attribute -> (state.attributes[requirement.name] ?: 0) >= requirement.minimum
        is EncounterRequirement.Charm -> requirement.name in state.acquiredCharmNames
        is EncounterRequirement.CharmCount -> {
            val direct = if (requirement.category.isBlank()) {
                state.acquiredCharmIds.count {
                    it.namespace != PreparedEncounterCatalog.NS_MARTIAL_STYLE
                }
            } else {
                state.charmCountByCategory[requirement.category]
                    ?: state.charmCountByCategory.entries.firstOrNull {
                        it.key.equals(requirement.category, ignoreCase = true)
                    }?.value
                    ?: 0
            }
            direct >= requirement.minimum
        }
        is EncounterRequirement.AnyOf -> requirement.alternatives.any { satisfied(it, state) }
        is EncounterRequirement.SpiritTrait -> requirement.trait in state.spiritTraits
        is EncounterRequirement.MartialStyle -> requirement.styleId in state.martialStyleIds
    }
}
