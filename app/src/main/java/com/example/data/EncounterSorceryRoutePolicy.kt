package com.example.data

import com.example.model.ArquetipoEncontro
import kotlin.random.Random

/**
 * Política única para decidir se a rota de Feitiçaria deve ser explorada.
 *
 * O sorteio automático decide exploração, não qualidade final. Focus explícito
 * exige exploração; fora do arquétipo Mental não há sorteio automático.
 */
internal object EncounterSorceryRoutePolicy {
    enum class Reason { EXPLICIT_FOCUS, MENTAL_EXPLORATION, NOT_EXPLORED }

    data class Decision(val explore: Boolean, val reason: Reason)

    /** Solar e Sangue de Dragão só podem materializar a rota terrestre com Ocultismo mínimo. */
    fun canConstructTerrestrialWithOccultism(abilities: Map<String, Int>): Boolean =
        (abilities[EncounterGenerationRules.HABILIDADE_OCULTISMO] ?: 0) >=
            EncounterGenerationRules.MIN_OCULTISMO_FEITICARIA

    /**
     * Decide se Solar/Sangue de Dragão devem materializar o projeto terrestre.
     * Exploração automática só materializa quando o catálogo contém o Círculo;
     * a exceção histórica do Físico continua estrutural quando Ocultismo é válido.
     */
    fun shouldMaterializeTerrestrialProject(
        archetype: ArquetipoEncontro,
        exploreSorcery: Boolean,
        abilities: Map<String, Int>,
        terrestrialCircleAvailable: Boolean
    ): Boolean {
        if (!canConstructTerrestrialWithOccultism(abilities)) return false
        return archetype == ArquetipoEncontro.FISICO ||
            (exploreSorcery && terrestrialCircleAvailable)
    }

    /**
     * Ocultismo só ganha prioridade de construção quando a rota realmente pode
     * ser materializada. A exceção histórica do Físico continua automática.
     */
    fun shouldPrioritizeOccultism(
        archetype: ArquetipoEncontro,
        exploreSorcery: Boolean,
        abilities: Map<String, Int>
    ): Boolean {
        if (!canConstructTerrestrialWithOccultism(abilities)) return false
        return exploreSorcery || archetype == ArquetipoEncontro.FISICO
    }

    fun decide(
        archetype: ArquetipoEncontro,
        explicitSorceryFocus: Boolean,
        random: Random
    ): Decision {
        if (explicitSorceryFocus) return Decision(true, Reason.EXPLICIT_FOCUS)
        if (archetype != ArquetipoEncontro.MENTAL) return Decision(false, Reason.NOT_EXPLORED)
        return if (EncounterGenerationRules.sortearFeiticariaMental(random)) {
            Decision(true, Reason.MENTAL_EXPLORATION)
        } else {
            Decision(false, Reason.NOT_EXPLORED)
        }
    }

    /**
     * A competição puro × Feitiçaria existe apenas no Mental automático.
     * Focus explícito e exceções estruturais obrigatórias não entram nessa
     * competição: são intenções/regras, não alternativas de qualidade.
     */
    fun shouldCompareAutomaticCandidates(
        archetype: ArquetipoEncontro,
        exploreSorcery: Boolean,
        explicitSorceryFocus: Boolean,
        sorceryConstructible: Boolean
    ): Boolean =
        archetype == ArquetipoEncontro.MENTAL &&
            exploreSorcery &&
            !explicitSorceryFocus &&
            sorceryConstructible

    /**
     * Seleciona deterministicamente a rota final depois que o sorteio decidiu
     * quais alternativas podem ser exploradas.
     *
     * Focus explícito mantém Feitiçaria como intenção obrigatória quando a rota
     * é legal. Exploração automática apenas habilita a comparação: não força
     * a rota mágica. Empates automáticos preservam a alternativa pura para que
     * o sorteio não se transforme, por si só, em preferência de qualidade.
     */
    /**
     * Mede um conjunto Solar/DB com sinais já existentes no gerador:
     * alinhamento com os níveis reais de Habilidade = utilidade presente;
     * sinergia ECS incremental = potencial de continuidade (limitado por
     * EncounterBuildQuality).
     */
    fun qualityOfTerrestrialCharmCandidate(
        charms: List<EncantoSolarDefinition>,
        abilities: Map<String, Int>
    ): EncounterBuildQuality.Score {
        val current = charms.sumOf { abilities[it.habilidade] ?: 0 }
        var future = 0
        val selectedTexts = ArrayList<String>(charms.size)
        charms.forEach { charm ->
            val text = EncounterCombatSynergy.text(charm)
            future += EncounterCombatSynergy.score(text, selectedTexts)
            selectedTexts += text
        }
        return EncounterBuildQuality.combine(current = current, future = future)
    }

    /**
     * Seed local e estável para construir a alternativa Lunar de comparação.
     * Não toca no Random autoritativo da geração; serve apenas ao candidato
     * contrafactual puro.
     */
    fun lunarComparisonSeed(
        attributes: Map<String, Int>,
        focus: String?,
        spiritTraits: Set<LunarSpiritTrait>
    ): Int {
        var hash = 17
        attributes.toSortedMap().forEach { (name, value) ->
            hash = 31 * hash + name.hashCode()
            hash = 31 * hash + value
        }
        hash = 31 * hash + (focus?.hashCode() ?: 0)
        spiritTraits.map { it.name }.sorted().forEach { hash = 31 * hash + it.hashCode() }
        return hash
    }

    /** Qualidade equivalente para o pipeline Lunar, baseado em Atributos. */
    fun qualityOfLunarCharmCandidate(
        charms: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>
    ): EncounterBuildQuality.Score {
        val current = charms.sumOf { attributes[it.atributo] ?: 0 }
        var future = 0
        val selectedTexts = ArrayList<String>(charms.size)
        charms.forEach { charm ->
            val text = EncounterCombatSynergy.text(charm)
            future += EncounterCombatSynergy.score(text, selectedTexts)
            selectedTexts += text
        }
        return EncounterBuildQuality.combine(current = current, future = future)
    }

    fun shouldSelectSorceryCandidate(
        explicitSorceryFocus: Boolean,
        exploreSorcery: Boolean,
        sorceryConstructible: Boolean,
        pureQuality: EncounterBuildQuality.Score,
        sorceryQuality: EncounterBuildQuality.Score
    ): Boolean {
        if (!exploreSorcery || !sorceryConstructible) return false
        if (explicitSorceryFocus) return true
        return sorceryQuality.total > pureQuality.total
    }

}
