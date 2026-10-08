package com.example.data

import kotlin.random.Random

/**
 * Planeja a Forma Espiritual a partir das rotas de Arquétipo que seus traços
 * REALMENTE habilitam. Porte do catálogo (small/medium/large) nunca é usado
 * como substituto de MINUSCULO/TAMANHO_LENDARIO.
 */
internal object LunarSpiritFormStrategy {
    data class Evaluation(
        val animal: SpiritualFormService.Animal,
        val traits: Set<LunarSpiritTrait>,
        val score: Int,
        val enabledArchetypeRoutes: Int,
        val focusRoutes: Int
    )

    private val structuralCharmByTrait = mapOf(
        LunarSpiritTrait.MINUSCULO to "Forma do Gafanhoto Esmeralda",
        LunarSpiritTrait.TAMANHO_LENDARIO to "Forma Bestial Imponente"
    )

    /**
     * Cache efêmero por planejamento de Forma Espiritual. A chave é canônica
     * para que animais/conjuntos equivalentes reutilizem exatamente o mesmo
     * RouteContext preparado, sem manter catálogos vivos entre NPCs.
     */
    private class RouteContextCache(
        private val catalogo: List<EncantoLunarDefinition>
    ) {
        private val prepared = HashMap<List<LunarSpiritTrait>, LunarCharmArchetypePolicy.RouteContext>()

        private fun key(traits: Set<LunarSpiritTrait>): List<LunarSpiritTrait> =
            traits.sortedBy { it.name }

        fun get(traits: Set<LunarSpiritTrait>): LunarCharmArchetypePolicy.RouteContext =
            prepared.getOrPut(key(traits)) {
                LunarCharmArchetypePolicy.prepare(catalogo, traits)
            }
    }

    fun escolherPrincipal(
        catalogo: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>,
        essencia: Int,
        foco: String?,
        ordemAtributos: List<String>,
        random: Random
    ): SpiritualFormService.Animal {
        val scoreByTraits = HashMap<Set<LunarSpiritTrait>, Triple<Int, Int, Int>>()
        val routeContexts = RouteContextCache(catalogo)
        val avaliacoes = SpiritualFormService.todasFormas().map { animal ->
            val traits = LunarSpiritShapeArchetypeTraits.forAnimal(animal)
            val values = scoreByTraits.getOrPut(traits) {
                val e = avaliar(animal, emptySet(), catalogo, attributes, essencia, foco, ordemAtributos, routeContexts)
                Triple(e.score, e.enabledArchetypeRoutes, e.focusRoutes)
            }
            Evaluation(animal, traits, values.first, values.second, values.third)
        }
        val melhor = avaliacoes.maxOfOrNull { it.score } ?: return SpiritualFormService.selecionarInicial(random)
        // Mantém variedade entre formas mecanicamente equivalentes.
        return avaliacoes.filter { it.score == melhor }.random(random).animal
    }

    fun escolherSecundaria(
        principal: SpiritualFormService.Animal,
        catalogo: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>,
        essencia: Int,
        foco: String?,
        ordemAtributos: List<String>,
        random: Random
    ): SpiritualFormService.Animal {
        val baseTraits = LunarSpiritShapeArchetypeTraits.forAnimal(principal)
        val routeContexts = RouteContextCache(catalogo)
        val base = valorDasRotas(baseTraits, catalogo, attributes, essencia, foco, ordemAtributos, routeContexts)
        val candidatos = SpiritualFormService.todasFormas().filterNot { it.portuguese == principal.portuguese }
        val totalByTraits = HashMap<Set<LunarSpiritTrait>, Int>()
        val avaliados = candidatos.map { animal ->
            val combined = baseTraits + LunarSpiritShapeArchetypeTraits.forAnimal(animal)
            val total = totalByTraits.getOrPut(combined) {
                valorDasRotas(combined, catalogo, attributes, essencia, foco, ordemAtributos, routeContexts)
            }
            // Quimera mede contribuição marginal, não duplica valor já dado pela principal.
            animal to (total - base)
        }
        val melhor = avaliados.maxOfOrNull { it.second } ?: return SpiritualFormService.selecionarSecundaria(principal, random)
        return avaliados.filter { it.second == melhor }.random(random).first
    }

    fun encantosEstruturais(
        traits: Set<LunarSpiritTrait>,
        catalogo: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>,
        essencia: Int,
        foco: String?
    ): List<EncantoLunarDefinition> = structuralCharmByTrait.mapNotNull { (trait, nome) ->
        if (trait !in traits) return@mapNotNull null
        val def = catalogo.firstOrNull { it.nome == nome } ?: return@mapNotNull null
        val routes = LunarCharmArchetypePolicy.routes(def, traits)
        val archetype = routes.firstOrNull { it.archetype }
        if (archetype != null &&
            (attributes[archetype.atributo] ?: 0) >= archetype.minAtributo &&
            def.minEssencia <= essencia &&
            (foco == null || archetype.atributo.equals(foco, ignoreCase = true))
        ) def else null
    }

    private fun avaliar(
        animal: SpiritualFormService.Animal,
        baseTraits: Set<LunarSpiritTrait>,
        catalogo: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>,
        essencia: Int,
        foco: String?,
        ordemAtributos: List<String>,
        routeContexts: RouteContextCache
    ): Evaluation {
        val traits = baseTraits + LunarSpiritShapeArchetypeTraits.forAnimal(animal)
        val context = routeContexts.get(traits)
        var enabled = 0
        var focusRoutes = 0
        var score = 0
        catalogo.forEach { def ->
            context.routesFor(def).filter { it.archetype }.forEach { route ->
                enabled++
                val nivel = attributes[route.atributo] ?: 0
                val reachable = nivel >= route.minAtributo && def.minEssencia <= essencia
                if (reachable) {
                    val priorityIndex = ordemAtributos.indexOfFirst { it.equals(route.atributo, ignoreCase = true) }
                    score += when {
                        foco != null && route.atributo.equals(foco, ignoreCase = true) -> 30
                        priorityIndex == 0 -> 18
                        priorityIndex in 1..2 -> 12
                        priorityIndex >= 0 -> 7
                        else -> 3
                    }
                    if (foco != null && route.atributo.equals(foco, ignoreCase = true)) focusRoutes++
                    // Uma rota com pré-requisito oferece valor, mas não é tratada como
                    // imediatamente disponível; o seletor continua responsável pela legalidade.
                    if (route.preRequisitos.isBlank() || route.preRequisitos.equals("Nenhum", true)) score += 5
                }
            }
        }
        return Evaluation(animal, traits, score, enabled, focusRoutes)
    }

    private fun valorDasRotas(
        traits: Set<LunarSpiritTrait>,
        catalogo: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>,
        essencia: Int,
        foco: String?,
        ordemAtributos: List<String>,
        routeContexts: RouteContextCache
    ): Int {
        val context = routeContexts.get(traits)
        return catalogo.sumOf { def ->
            context.routesFor(def).filter { it.archetype }.sumOf { route ->
                if ((attributes[route.atributo] ?: 0) < route.minAtributo || def.minEssencia > essencia) 0
                else when {
                    foco != null && route.atributo.equals(foco, true) -> 30
                    ordemAtributos.any { it.equals(route.atributo, true) } -> 10
                    else -> 2
                }
            }
        }
    }
}
