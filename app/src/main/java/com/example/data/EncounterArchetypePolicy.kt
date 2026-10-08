package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants

/**
 * Central policy for NPC archetype remodeling.
 *
 * Generation is deliberately layered: generic archetype policy first, then
 * the Exalted-specific generator applies Solar, Dragon-Blooded or Lunar rules.
 * Combat remains a single specialization: only the selected attack ability is
 * eligible for automatic PB/XP investment.
 */
internal object EncounterArchetypePolicy {
    /**
     * Chance that the NPC route prefers the Martial Artist merit over the
     * normal Brawl route. The decision is made once per generated NPC.
     */
    fun chanceArtesMarciaisPercent(archetype: ArquetipoEncontro): Int = when (archetype) {
        ArquetipoEncontro.FISICO -> 70
        ArquetipoEncontro.SOCIAL -> 50
        ArquetipoEncontro.MENTAL -> 30
    }

    fun devePreferirArtesMarciais(
        archetype: ArquetipoEncontro,
        briga: Int,
        random: kotlin.random.Random
    ): Boolean = briga >= 1 && random.nextInt(100) < chanceArtesMarciaisPercent(archetype)

    fun abilityPriority(
        archetype: ArquetipoEncontro,
        combat: String,
        support: String,
        favored: List<String>,
        supernal: String? = null
    ): List<String> {
        // A defesa nativa precisa participar da mesma política de prioridade
        // usada pela progressão de XP. Para Armas Brancas/Briga, Aparar usa a
        // própria Habilidade de combate; para Arqueirismo/Arremesso, a defesa
        // nativa é Esquiva. Não devemos tratar Esquiva como prioridade genérica
        // do arquétipo Físico quando ela não é a defesa efetivamente utilizada.
        // Alguns fluxos de teste e os arquétipos Social/Mental usam a
        // habilidade de suporte nessa posição, e ela não é necessariamente uma
        // Habilidade de combate. Só mapeamos a defesa nativa quando o valor
        // recebido é realmente uma Habilidade de combate.
        val habilidadeDefensiva = combat
            .takeIf { it in EncounterGenerationRules.COMBAT_ABILITIES }
            ?.let(EncounterGenerationRules::habilidadeDefensivaPara)
        val habilidadesCombatePermitidas = setOfNotNull(combat, habilidadeDefensiva)
        val archetypeCore = when (archetype) {
            ArquetipoEncontro.FISICO -> PhysicalArchetypePolicy.priorityAbilities
            ArquetipoEncontro.SOCIAL -> SocialArchetypePolicy.priorityAbilities
            ArquetipoEncontro.MENTAL -> MentalArchetypePolicy.priorityAbilities
        }.filter { it !in EncounterGenerationRules.COMBAT_ABILITIES || it in habilidadesCombatePermitidas }
        val core = when (archetype) {
            ArquetipoEncontro.FISICO -> listOf(combat)
            ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL -> listOf(support)
        }
        // A segunda habilidade de combate nunca entra na prioridade automática.
        // O foco de combate é único para todos os arquétipos; Social/Mental
        // priorizam suporte e especializações próprias, sem reintroduzir outro
        // foco de ataque no fim da lista.
        val favoredPermitidas = favored.filter {
            it !in EncounterGenerationRules.COMBAT_ABILITIES || it in habilidadesCombatePermitidas
        }
        // A defesa nativa fica imediatamente após o ataque. Isso faz com que
        // Aparar seja priorizado naturalmente quando o ataque é Armas Brancas
        // ou Briga, e que Esquiva seja priorizada quando a defesa nativa é
        // Esquiva (Arqueirismo/Arremesso).
        val combatePrincipal = listOf(combat)
        val defesaNativa = listOfNotNull(habilidadeDefensiva)
        return EncounterRulePolicy.resolvePriority(
            exaltStructure = listOfNotNull(supernal) + favoredPermitidas,
            archetype = core + combatePrincipal + defesaNativa + archetypeCore
        ).filter { it in ExaltedConstants.ALL_25_ABILITIES }
    }

    fun charmPriority(
        archetype: ArquetipoEncontro,
        combat: String,
        support: String,
        favored: List<String>,
        supernal: String? = null,
        priorizarOcultismo: Boolean = false
    ): List<String> {
        val prioridadeMagia = if (priorizarOcultismo) {
            listOf(EncounterGenerationRules.HABILIDADE_OCULTISMO)
        } else {
            emptyList()
        }
        val habilidadeDefensiva = combat
            .takeIf { it in EncounterGenerationRules.COMBAT_ABILITIES }
            ?.let(EncounterGenerationRules::habilidadeDefensivaPara)
        val prioridadeArquetipo = when (archetype) {
            ArquetipoEncontro.FISICO ->
                listOfNotNull(combat, habilidadeDefensiva) + listOfNotNull(supernal) + favored + listOf(support)
            ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL ->
                listOfNotNull(support, habilidadeDefensiva) + listOfNotNull(supernal) + favored + listOf(combat)
        }
        return EncounterRulePolicy.resolvePriority(
            explicitIntent = prioridadeMagia,
            exaltStructure = listOfNotNull(supernal) + favored,
            archetype = prioridadeArquetipo
        )
    }

    fun lunarAttributePriority(
        archetype: ArquetipoEncontro,
        favoredOrCaste: List<String>
    ): List<String> {
        val core = EncounterGenerationRules.ATTRIBUTE_GROUPS.getValue(archetype)
        return (core + favoredOrCaste + EncounterGenerationRules.ALL_ATTRIBUTES + "Universal")
            .distinct()
    }
}
