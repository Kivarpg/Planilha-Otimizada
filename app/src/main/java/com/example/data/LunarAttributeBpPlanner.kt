package com.example.data

import com.example.model.ArquetipoEncontro

/**
 * Planejador conservador dos Atributos que podem receber PB Lunar.
 *
 * Não compra pontos, não altera custos e não decide legalidade. Ele apenas
 * ordena os quatro Atributos de Casta/Favorecidos já escolhidos pelo gerador.
 * Nesta fase da criação a Forma Espiritual ainda não existe; por isso somente
 * as rotas normais do catálogo participam da projeção. Rotas de Forma/Quimera
 * continuam sob responsabilidade da seleção de Encantos posterior.
 */
internal object LunarAttributeBpPlanner {
    data class Projection(
        val priority: List<String>,
        val scores: Map<String, Int>
    )

    data class EssenceHorizon(
        val essence: Int,
        val xpThreshold: Int,
        val reachableNormalRoutes: Int,
        val focusedRoutes: Int
    )

    data class HorizonComparison(
        val current: List<EssenceHorizon>,
        val strategic: List<EssenceHorizon>,
        val strategicNonWorseAtEveryMilestone: Boolean,
        val strategicStrictlyBetterSomewhere: Boolean
    )

    data class Comparison(
        val currentPriority: List<String>,
        val strategicPriority: List<String>,
        val sameFirstChoice: Boolean,
        val currentFirstScore: Int,
        val strategicFirstScore: Int,
        val nonRegressive: Boolean
    )

    /**
     * Gate de shadow mode. Compara a primeira compra que o algoritmo histórico
     * faria com a primeira sugerida pelo planejador, usando a MESMA função de
     * pontuação prospectiva. Não altera a ficha.
     */
    fun compareShadow(
        archetype: ArquetipoEncontro,
        attributes: Map<String, Int>,
        casteOrFavored: List<String>,
        catalog: List<EncantoLunarDefinition>,
        explicitFocus: String?
    ): Comparison {
        val legal = casteOrFavored.distinct()
        val currentPriority = legal.sortedByDescending { attributes[it] ?: 1 }
        val projection = project(archetype, attributes, legal, catalog, explicitFocus)
        val currentFirst = currentPriority.firstOrNull()
        val strategicFirst = projection.priority.firstOrNull()
        val currentScore = currentFirst?.let { projection.scores[it] } ?: 0
        val strategicScore = strategicFirst?.let { projection.scores[it] } ?: 0
        return Comparison(
            currentPriority = currentPriority,
            strategicPriority = projection.priority,
            sameFirstChoice = currentFirst == strategicFirst,
            currentFirstScore = currentScore,
            strategicFirstScore = strategicScore,
            nonRegressive = strategicScore >= currentScore
        )
    }

    /**
     * Projeção barata E1-E5. Mede apenas a barreira estrutural Atributo +
     * Essência das rotas normais; pré-requisitos entre Encantos permanecem
     * fora desta métrica e continuam sob o seletor autoritativo.
     */
    fun compareEssenceHorizon(
        attributes: Map<String, Int>,
        catalog: List<EncantoLunarDefinition>,
        currentFirst: String?,
        strategicFirst: String?,
        explicitFocus: String?
    ): HorizonComparison {
        val milestones = listOf(1 to 0, 2 to 50, 3 to 125, 4 to 200, 5 to 300)

        fun projected(first: String?): Map<String, Int> = attributes.toMutableMap().apply {
            if (first != null) put(first, ((this[first] ?: 1) + 1).coerceAtMost(5))
        }
        // Sem Forma Espiritual nesta etapa: contexto normal deliberadamente
        // preparado com traits vazios. A expansão abaixo percorre apenas
        // Encantos realmente adquiríveis a partir do conjunto já alcançado,
        // respeitando Essência, Atributo e cadeia de pré-requisitos.
        val routeContext = LunarCharmArchetypePolicy.prepare(catalog, emptySet())
        fun horizons(projected: Map<String, Int>): List<EssenceHorizon> = milestones.map { (essence, xp) ->
            val acquired = linkedSetOf<String>()
            var changed: Boolean
            do {
                changed = false
                catalog.forEach { def ->
                    if (def.nome !in acquired && LunarCharmArchetypePolicy.hasEligibleRoute(
                            def = def,
                            context = routeContext,
                            attributes = projected,
                            essencia = essence,
                            nomesSelecionados = acquired,
                            catalogo = catalog
                        )
                    ) {
                        acquired += def.nome
                        changed = true
                    }
                }
            } while (changed)
            EssenceHorizon(
                essence = essence,
                xpThreshold = xp,
                reachableNormalRoutes = acquired.size,
                focusedRoutes = if (explicitFocus == null) 0 else catalog.count { def ->
                    def.nome in acquired && def.atributo.equals(explicitFocus, ignoreCase = true)
                }
            )
        }

        val current = horizons(projected(currentFirst))
        val strategic = horizons(projected(strategicFirst))
        fun compareValue(a: EssenceHorizon, b: EssenceHorizon): Int =
            if (a.focusedRoutes != b.focusedRoutes) {
                a.focusedRoutes.compareTo(b.focusedRoutes)
            } else {
                a.reachableNormalRoutes.compareTo(b.reachableNormalRoutes)
            }
        return HorizonComparison(
            current = current,
            strategic = strategic,
            strategicNonWorseAtEveryMilestone = strategic.zip(current).all { (s, old) -> compareValue(s, old) >= 0 },
            strategicStrictlyBetterSomewhere = strategic.zip(current).any { (s, old) -> compareValue(s, old) > 0 }
        )
    }

    fun activationPriority(
        local: Comparison,
        horizon: HorizonComparison
    ): List<String> = if (
        local.nonRegressive && horizon.strategicNonWorseAtEveryMilestone
    ) local.strategicPriority else emptyList()

    fun project(
        archetype: ArquetipoEncontro,
        attributes: Map<String, Int>,
        casteOrFavored: List<String>,
        catalog: List<EncantoLunarDefinition>,
        explicitFocus: String?
    ): Projection {
        val legal = casteOrFavored.distinct()
        if (legal.isEmpty()) return Projection(emptyList(), emptyMap())

        val archetypeOrder = LunarArchetypePolicy.attributePriority(archetype, legal)
        val archetypeRank = archetypeOrder.withIndex().associate { it.value to it.index }
        val normalByAttribute = catalog.groupBy { it.atributo }

        fun score(attribute: String): Int {
            val current = attributes[attribute] ?: 1
            val afterOneBp = (current + 1).coerceAtMost(5)
            val defs = normalByAttribute[attribute].orEmpty()

            // Ganho imediato: Encantos cuja exigência de Atributo passa a ser
            // satisfeita exatamente pela compra projetada.
            val newlyReachable = defs.count {
                it.minAtributo > current && it.minAtributo <= afterOneBp
            }
            // Continuidade: densidade de rotas normais que ficam dentro do
            // alcance do Atributo após a compra. Essência/pré-requisitos ainda
            // serão validados pelo seletor autoritativo de Encantos.
            val reachableDensity = defs.count { it.minAtributo <= afterOneBp }
            val futureDepth = defs.count { it.minAtributo in (afterOneBp + 1)..5 }

            val focusBonus = if (explicitFocus.equals(attribute, ignoreCase = true)) 100_000 else 0
            val archetypeBonus = (10 - (archetypeRank[attribute] ?: 10)).coerceAtLeast(0) * 1_000
            return focusBonus + archetypeBonus + newlyReachable * 100 + reachableDensity * 10 + futureDepth
        }

        val scores = legal.associateWith(::score)
        val priority = legal.sortedWith(
            compareByDescending<String> { scores.getValue(it) }
                // Em equivalência estratégica, mantém a preferência do
                // comportamento atual: maior rating primeiro.
                .thenByDescending { attributes[it] ?: 1 }
                .thenBy { legal.indexOf(it) }
        )
        return Projection(priority, scores)
    }
}
