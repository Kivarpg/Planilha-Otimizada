package com.example.data

import com.example.model.ExaltedConstants

/** Resolve a rota NORMAL ou ARQUÉTIPO usada para adquirir um Encanto Lunar.
 * Arquétipo altera aquisição (Atributo, mínimo, pré-requisitos), nunca rolagens/cálculos do Encanto.
 */
internal object LunarCharmArchetypePolicy {
    data class AcquisitionRoute(
        val atributo: String,
        val minAtributo: Int,
        val preRequisitos: String,
        val archetype: Boolean,
        /** Pré-requisitos compilados uma vez ao preparar o contexto da Forma. */
        val requisitosCompilados: List<EncounterRequirement> = EncounterPrerequisiteParser.parse(preRequisitos)
    )

    /** Rotas cuja condição de Forma Espiritual já foi resolvida uma única vez. */
    data class RouteContext(
        val routesByCharm: Map<String, List<AcquisitionRoute>>,
        val charmsByAttribute: Map<String, List<EncantoLunarDefinition>>,
        private val dependentsByCharm: Map<String, Set<String>>,
        private val dependentsByCategory: Map<String, Set<String>>,
        private val dependentsByTotalCount: Set<String>,
        val charmNamesByCategory: Map<String, Set<String>>
    ) {
        fun routesFor(def: EncantoLunarDefinition): List<AcquisitionRoute> =
            routesByCharm[def.nome].orEmpty()

        /** Quantos Encantos distintos dependem diretamente desta aquisição. */
        fun directCharmDependentCount(charmName: String): Int =
            dependentsByCharm[charmName]?.size ?: 0

        /** Alcance transitivo de dependentes nominais, com proteção contra ciclos. */
        fun transitiveCharmDependentCount(charmName: String): Int {
            val visited = mutableSetOf(charmName)
            val pending = ArrayDeque<String>()
            pending.addLast(charmName)
            while (pending.isNotEmpty()) {
                val current = pending.removeFirst()
                for (dependent in dependentsByCharm[current].orEmpty()) {
                    if (visited.add(dependent)) pending.addLast(dependent)
                }
            }
            return visited.size - 1
        }

        /** Maior Essência nominal entre descendentes; não implica rota adquirível. */
        fun highestDependentEssence(charmName: String, essenceByCharm: Map<String, Int>): Int? {
            val visited = mutableSetOf(charmName)
            val pending = ArrayDeque<String>()
            pending.addLast(charmName)
            var highest: Int? = null
            while (pending.isNotEmpty()) {
                val current = pending.removeFirst()
                for (dependent in dependentsByCharm[current].orEmpty()) {
                    if (visited.add(dependent)) {
                        essenceByCharm[dependent]?.let { essence ->
                            highest = maxOf(highest ?: essence, essence)
                        }
                        pending.addLast(dependent)
                    }
                }
            }
            return highest
        }

        /**
         * Delta exato para aquisição monotônica: somente Encantos cujo requisito
         * menciona o Encanto comprado, a categoria incrementada ou a contagem
         * total podem mudar de inelegível para elegível por esta compra.
         */
        fun affectedAfterAcquisition(charmName: String, category: String): Set<String> {
            val direct = dependentsByCharm[charmName].orEmpty()
            val byCategory = dependentsByCategory[category.trim().lowercase()].orEmpty()
            if (direct.isEmpty() && byCategory.isEmpty()) return dependentsByTotalCount
            return buildSet(direct.size + byCategory.size + dependentsByTotalCount.size) {
                addAll(direct)
                addAll(byCategory)
                addAll(dependentsByTotalCount)
            }
        }
    }

    private val knownAttributes =
        (ExaltedConstants.PHYSICAL_ATTRIBUTES + ExaltedConstants.SOCIAL_ATTRIBUTES + ExaltedConstants.MENTAL_ATTRIBUTES).toSet()

    // Preparar rotas compila pré-requisitos e índices de dependência para todo
    // o catálogo. Esses dados dependem somente do catálogo e dos traços da
    // Forma Espiritual, não dos Atributos/Essência/Encantos já escolhidos.
    // O cache fraco por catálogo permite reutilizar a preparação entre NPCs
    // sem reter catálogos antigos após uma recarga.
    private val routeContextCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<
                List<EncantoLunarDefinition>,
                MutableMap<Set<LunarSpiritTrait>, RouteContext>
            >()
        )

    private fun conditionSatisfied(condition: String, traits: Set<LunarSpiritTrait>): Boolean = when (condition) {
        "MINUSCULO" -> LunarSpiritTrait.MINUSCULO in traits
        "TAMANHO_LENDARIO" -> LunarSpiritTrait.TAMANHO_LENDARIO in traits
        "PREDATORIO" -> LunarSpiritTrait.PREDATORIO in traits
        "CACA_EM_GRUPO" -> LunarSpiritTrait.CACA_EM_GRUPO in traits
        "MIGRATORIO" -> LunarSpiritTrait.MIGRATORIO in traits
        "VISAO_APRIMORADA" -> LunarSpiritTrait.VISAO_APRIMORADA in traits
        "VISAO_NOTURNA" -> LunarSpiritTrait.VISAO_NOTURNA in traits
        "CAMUFLAGEM" -> LunarSpiritTrait.CAMUFLAGEM in traits
        "VENENOSO" -> LunarSpiritTrait.VENENOSO in traits
        "RESPIRA_AGUA" -> LunarSpiritTrait.RESPIRA_AGUA in traits
        "VIVE_EM_COLMEIA" -> LunarSpiritTrait.VIVE_EM_COLMEIA in traits
        "FURIA" -> LunarSpiritTrait.FURIA in traits
        "BESTA_DE_CARGA" -> LunarSpiritTrait.BESTA_DE_CARGA in traits
        "AMEACA_OU_INTIMIDACAO" -> LunarSpiritTrait.AMEACA_OU_INTIMIDACAO in traits
        "IMITA_APARENCIA" -> LunarSpiritTrait.IMITA_APARENCIA in traits
        "IMITA_SONS" -> LunarSpiritTrait.IMITA_SONS in traits
        "ESCALADOR_ADERENTE" -> LunarSpiritTrait.ESCALADOR_ADERENTE in traits
        "FINGE_MORTE" -> LunarSpiritTrait.FINGE_MORTE in traits
        "CARNICEIRO" -> LunarSpiritTrait.CARNICEIRO in traits
        "PRESA_E_MINUSCULO" ->
            LunarSpiritTrait.PRESA in traits && LunarSpiritTrait.MINUSCULO in traits
        else -> false // desconhecido nunca habilita uma rota silenciosamente
    }

    fun routes(
        def: EncantoLunarDefinition,
        spiritTraits: Set<LunarSpiritTrait>
    ): List<AcquisitionRoute> {
        val normal = AcquisitionRoute(def.atributo, def.minAtributo, def.preRequisitos, false)
        val alternatives = def.rotasArquetipo.asSequence()
            .filter { conditionSatisfied(it.condicao, spiritTraits) }
            .map { route ->
                // Se o texto individual não substitui os pré-requisitos, a regra geral mantém
                // "Nenhum" ou a cadeia já composta por Encantos de Arquétipo.
                val prerequisites = route.preRequisitosAlternativos
                    .takeUnless { it.isBlank() || it.equals("Nenhum", ignoreCase = true) }
                    ?: def.preRequisitos
                AcquisitionRoute(route.atributo, route.minAtributo, prerequisites, true)
            }
            .toList()
        return listOf(normal) + alternatives
    }

    fun prepare(
        catalogo: List<EncantoLunarDefinition>,
        spiritTraits: Set<LunarSpiritTrait>
    ): RouteContext {
        val traitsKey = spiritTraits.toSet()
        synchronized(routeContextCache) {
            routeContextCache[catalogo]?.get(traitsKey)?.let { return it }
        }
        val prepared = prepareUncached(catalogo, traitsKey)
        synchronized(routeContextCache) {
            val byTraits = routeContextCache.getOrPut(catalogo) { HashMap() }
            return byTraits.getOrPut(traitsKey) { prepared }
        }
    }

    private fun prepareUncached(
        catalogo: List<EncantoLunarDefinition>,
        spiritTraits: Set<LunarSpiritTrait>
    ): RouteContext {
        require(catalogo.map { it.nome }.distinct().size == catalogo.size) {
            "Catálogo Lunar contém nomes de Encanto duplicados; RouteContext exige identidade única."
        }
        val routesByCharm = LinkedHashMap<String, List<AcquisitionRoute>>(catalogo.size)
        val byAttribute = linkedMapOf<String, MutableList<EncantoLunarDefinition>>()
        val dependentsByCharm = linkedMapOf<String, MutableSet<String>>()
        val dependentsByCategory = linkedMapOf<String, MutableSet<String>>()
        val dependentsByTotalCount = linkedSetOf<String>()
        val charmNamesByCategory = linkedMapOf<String, MutableSet<String>>()

        fun indexRequirement(owner: String, requirement: EncounterRequirement) {
            when (requirement) {
                is EncounterRequirement.Charm ->
                    dependentsByCharm.getOrPut(requirement.name) { linkedSetOf() }.add(owner)
                is EncounterRequirement.CharmCount -> {
                    if (requirement.category.isBlank()) dependentsByTotalCount += owner
                    else dependentsByCategory
                        .getOrPut(requirement.category.trim().lowercase()) { linkedSetOf() }
                        .add(owner)
                }
                is EncounterRequirement.AnyOf -> requirement.alternatives.forEach { indexRequirement(owner, it) }
                else -> Unit
            }
        }

        catalogo.forEach { def ->
            val enabled = routes(def, spiritTraits)
            routesByCharm[def.nome] = enabled
            enabled.asSequence().map { it.atributo }.distinct().forEach { atributo ->
                byAttribute.getOrPut(atributo) { mutableListOf() }.add(def)
                charmNamesByCategory.getOrPut(atributo.trim().lowercase()) { linkedSetOf() }.add(def.nome)
            }
            if (def.atributo in ExaltedConstants.MENTAL_ATTRIBUTES) {
                charmNamesByCategory.getOrPut("atributo mental") { linkedSetOf() }.add(def.nome)
            }
            enabled.forEach { route -> route.requisitosCompilados.forEach { indexRequirement(def.nome, it) } }
        }
        return RouteContext(
            routesByCharm = routesByCharm,
            charmsByAttribute = byAttribute.mapValues { it.value.toList() },
            dependentsByCharm = dependentsByCharm.mapValues { it.value.toSet() },
            dependentsByCategory = dependentsByCategory.mapValues { it.value.toSet() },
            dependentsByTotalCount = dependentsByTotalCount.toSet(),
            charmNamesByCategory = charmNamesByCategory.mapValues { it.value.toSet() }
        )
    }

    /**
     * Hot path Lunar: avalia a gramática canônica já compilada sem criar uma
     * lista intermediária nem voltar ao parser em cada consulta do beam.
     */
    private fun routeEligible(
        def: EncantoLunarDefinition,
        route: AcquisitionRoute,
        attributes: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogo: List<EncantoLunarDefinition>,
        contagemCategoriaSelecionada: ((String) -> Int)?,
        categoryNames: Map<String, Set<String>>? = null
    ): Boolean {
        if ((attributes[route.atributo] ?: 0) < route.minAtributo) return false
        if (def.minEssencia > essencia) return false

        fun satisfaz(requisito: EncounterRequirement): Boolean = when (requisito) {
            is EncounterRequirement.Charm -> requisito.name in nomesSelecionados
            is EncounterRequirement.CharmCount -> {
                val contagem = if (requisito.category.isBlank()) {
                    nomesSelecionados.size
                } else {
                    if (requisito.category !in knownAttributes &&
                        !requisito.category.equals("Universal", ignoreCase = true) &&
                        !requisito.category.equals("Atributo Mental", ignoreCase = true)
                    ) return false
                    contagemCategoriaSelecionada?.invoke(requisito.category) ?: categoryNames
                        ?.get(requisito.category.trim().lowercase())
                        ?.let { names -> nomesSelecionados.count { it in names } }
                        ?: catalogo.count { lunar ->
                            val corresponde = if (requisito.category.equals("Atributo Mental", ignoreCase = true)) {
                                lunar.atributo in ExaltedConstants.MENTAL_ATTRIBUTES
                            } else {
                                lunar.atributo.equals(requisito.category, ignoreCase = true) ||
                                    lunar.rotasArquetipo.any { it.atributo.equals(requisito.category, ignoreCase = true) }
                            }
                            corresponde && lunar.nome in nomesSelecionados
                        }
                }
                contagem >= requisito.minimum
            }
            is EncounterRequirement.AnyOf -> requisito.alternatives.any(::satisfaz)
            else -> true
        }
        return route.requisitosCompilados.all(::satisfaz)
    }

    fun hasEligibleRoute(
        def: EncantoLunarDefinition,
        context: RouteContext,
        attributes: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogo: List<EncantoLunarDefinition>,
        contagemCategoriaSelecionada: ((String) -> Int)? = null
    ): Boolean = context.routesFor(def).any { route ->
        routeEligible(def, route, attributes, essencia, nomesSelecionados, catalogo, contagemCategoriaSelecionada, context.charmNamesByCategory)
    }

    private fun eligibleRoutesFrom(
        def: EncantoLunarDefinition,
        enabledRoutes: List<AcquisitionRoute>,
        attributes: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogo: List<EncantoLunarDefinition>,
        contagemCategoriaSelecionada: ((String) -> Int)? = null,
        categoryNames: Map<String, Set<String>>? = null
    ): List<AcquisitionRoute> = enabledRoutes.filter { route ->
        routeEligible(def, route, attributes, essencia, nomesSelecionados, catalogo, contagemCategoriaSelecionada, categoryNames)
    }

    fun eligibleRoutes(
        def: EncantoLunarDefinition,
        context: RouteContext,
        attributes: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogo: List<EncantoLunarDefinition>,
        contagemCategoriaSelecionada: ((String) -> Int)? = null
    ): List<AcquisitionRoute> = eligibleRoutesFrom(
        def, context.routesFor(def), attributes, essencia, nomesSelecionados, catalogo, contagemCategoriaSelecionada, context.charmNamesByCategory
    )

    fun eligibleRoutes(
        def: EncantoLunarDefinition,
        attributes: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogo: List<EncantoLunarDefinition>,
        spiritTraits: Set<LunarSpiritTrait>,
        contagemCategoriaSelecionada: ((String) -> Int)? = null
    ): List<AcquisitionRoute> = eligibleRoutesFrom(
        def, routes(def, spiritTraits), attributes, essencia, nomesSelecionados, catalogo, contagemCategoriaSelecionada
    )

    fun acquisitionRoute(
        def: EncantoLunarDefinition,
        preferredAttribute: String?,
        context: RouteContext,
        attributes: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogo: List<EncantoLunarDefinition>,
        contagemCategoriaSelecionada: ((String) -> Int)? = null,
        routeAllowed: (AcquisitionRoute) -> Boolean = { true }
    ): AcquisitionRoute? {
        var primeiraElegivel: AcquisitionRoute? = null
        for (route in context.routesFor(def)) {
            if (!routeAllowed(route)) continue
            if (!routeEligible(
                    def,
                    route,
                    attributes,
                    essencia,
                    nomesSelecionados,
                    catalogo,
                    contagemCategoriaSelecionada,
                    context.charmNamesByCategory
                )
            ) continue
            if (primeiraElegivel == null) primeiraElegivel = route
            if (preferredAttribute != null && route.atributo.equals(preferredAttribute, ignoreCase = true)) return route
        }
        return primeiraElegivel
    }

    fun canBelongToAttribute(
        def: EncantoLunarDefinition,
        attribute: String,
        spiritTraits: Set<LunarSpiritTrait>
    ): Boolean = routes(def, spiritTraits).any { it.atributo.equals(attribute, ignoreCase = true) }
}
