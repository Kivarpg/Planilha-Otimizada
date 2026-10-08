package com.example.data

/**
 * Constituição de regras da Aba 11.
 *
 * Mantém separadas três naturezas que não podem ser confundidas:
 * LEGALITY: regra mecânica; nunca pode ser vencida por Foco/Arquétipo.
 * STRUCTURAL: contrato do gerador/produto; obrigatório durante construção automática.
 * QUALITY: heurística; influencia ranking, mas não torna uma ficha ilegal.
 */
internal object EncounterRulePolicy {
    enum class Nature { LEGALITY, STRUCTURAL, QUALITY }

    enum class Precedence(val rank: Int) {
        LEGALITY(0),
        EXALT_TYPE_STRUCTURE(1),
        EXPLICIT_USER_INTENT(2),
        ARCHETYPE(3),
        CURRENT_EFFICIENCY(4),
        SYNERGY(5),
        FUTURE_GROWTH(6),
        VARIETY(7)
    }

    data class RuleDescriptor(
        val id: String,
        val nature: Nature,
        val precedence: Precedence,
        val description: String
    )

    /** Registro mínimo das invariantes/políticas globais atualmente materializadas no domínio. */
    val registry: List<RuleDescriptor> = listOf(
        RuleDescriptor("L-CHARM-REQ", Nature.LEGALITY, Precedence.LEGALITY, "Requisitos de aquisição de Encantos"),
        RuleDescriptor("P-SPECIALTY-2", Nature.STRUCTURAL, Precedence.EXALT_TYPE_STRUCTURE, "Especialização exige Habilidade 2+"),
        RuleDescriptor("P-OLD-REALM", Nature.STRUCTURAL, Precedence.EXALT_TYPE_STRUCTURE, "Antigo Reino exige Ocultismo 1+ ou Conhecimento 1+"),
        RuleDescriptor("P-LING-MENTAL", Nature.STRUCTURAL, Precedence.EXALT_TYPE_STRUCTURE, "Arquétipo Mental recebe Linguística 1+"),
        RuleDescriptor("P-LING-DB-ORIGIN", Nature.STRUCTURAL, Precedence.EXALT_TYPE_STRUCTURE, "DB do Império/Lookshy recebe Linguística 1+"),
        RuleDescriptor("H-NO-ACCIDENTAL-ONE", Nature.QUALITY, Precedence.CURRENT_EFFICIENCY, "Evitar Habilidade 1 acidental"),
        RuleDescriptor("H-COMBAT-ATTR", Nature.QUALITY, Precedence.CURRENT_EFFICIENCY, "Ajustar Atributos ao método de combate"),
        RuleDescriptor("H-TREE-CONTINUITY", Nature.QUALITY, Precedence.FUTURE_GROWTH, "Preferir continuidade útil da árvore")
    )


    /**
     * Restrições LEGALITY/STRUCTURAL não participam do desempate: elas filtram
     * candidatos antes do ranking. Este helper torna esse contrato verificável
     * sem acoplar o policy engine a um tipo concreto de candidato.
     */
    inline fun <T> filterRequired(
        candidates: Iterable<T>,
        crossinline isAllowed: (T) -> Boolean
    ): List<T> = candidates.filter { isAllowed(it) }

    /**
     * Combina preferências sem depender de concatenações ad-hoc nos geradores.
     * Restrições estruturais/legais já devem ter filtrado o espaço de opções; aqui,
     * Foco explícito lidera o ranking, seguido do Arquétipo e das preferências do Tipo de Exaltado.
     * A primeira ocorrência vence; vazios são descartados.
     */
    fun resolvePriority(
        explicitIntent: List<String> = emptyList(),
        exaltStructure: List<String> = emptyList(), // preferências já legais; não restrições obrigatórias
        archetype: List<String> = emptyList(),
        efficiency: List<String> = emptyList(),
        synergy: List<String> = emptyList(),
        growth: List<String> = emptyList(),
        fallback: List<String> = emptyList()
    ): List<String> = (
        explicitIntent + archetype + exaltStructure + efficiency + synergy + growth + fallback
    ).asSequence().filter { it.isNotBlank() }.distinct().toList()
    /** Prioridade de Habilidades derivada de uma ficha já construída.
     *
     * Foco explícito permanece soberano. Depois dele, a progressão considera
     * a intenção do Arquétipo e só então sinais da própria ficha: função de
     * combate/defesa, árvores já desenvolvidas e níveis atuais. Isso evita que
     * o roadmap abandone uma linha coerente apenas porque a criação terminou.
     */
    fun abilityPriorityFor(npc: com.example.model.NpcEncontro): List<String> {
        val concentracaoEncantos = npc.charms
            .groupingBy { it.habilidadeVinculada }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        val eficienciaAtual = listOfNotNull(
            npc.habilidadePrincipal.takeIf { it.isNotBlank() },
            npc.habilidadeDefensiva?.takeIf { it.isNotBlank() },
            npc.habilidadeSuporte.takeIf { it.isNotBlank() }
        )
        val crescimento = npc.abilities.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        return resolvePriority(
            explicitIntent = listOfNotNull(npc.focoProgressaoExplicito),
            archetype = EncounterArchetypePolicy.abilityPriority(
                archetype = npc.arquetipo,
                combat = npc.habilidadePrincipal,
                support = npc.habilidadeSuporte,
                favored = npc.habilidadesFavorecidas,
                supernal = npc.habilidadeSupernal.takeIf { it.isNotBlank() }
            ),
            efficiency = eficienciaAtual,
            synergy = concentracaoEncantos,
            growth = crescimento
        )
    }

    /** Prioridade Lunar de Atributos derivada de uma ficha existente. */
    fun lunarAttributePriorityFor(npc: com.example.model.NpcEncontro): List<String> {
        val especiais = resolvePriority(
            exaltStructure = npc.lunarAtributosCasta + npc.habilidadesFavorecidas
        )
        return resolvePriority(
            explicitIntent = listOfNotNull(npc.focoProgressaoExplicito),
            archetype = EncounterArchetypePolicy.lunarAttributePriority(npc.arquetipo, especiais)
        )
    }


}
