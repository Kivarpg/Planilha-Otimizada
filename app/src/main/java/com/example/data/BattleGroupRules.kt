package com.example.data

import com.example.model.BattleGroup
import com.example.model.BattleGroupDerivedAttack
import com.example.model.BattleGroupDerivedStats

/** Confirmed Battle Group calculations only. Unconfirmed rules are deliberately absent. */
object BattleGroupRules {
    fun calculateAttack(base: Int, size: Int, might: Int): Int = base + size + might
    fun calculateDamage(base: Int, size: Int, might: Int): Int = base + size + might
    fun calculateSoak(base: Int, size: Int): Int = base + size
    fun calculateDefense(base: Int, drillDefense: Int, mightDefense: Int): Int = base + drillDefense + mightDefense
    fun calculateMagnitude(base: Int, size: Int): Int = base + size

    fun derive(group: BattleGroup, troop: BattleGroupTroopType?): BattleGroupDerivedStats? {
        val custom = group.customStats
        val attacks = when {
            troop != null -> troop.attacks.map { attack ->
                BattleGroupDerivedAttack(
                    name = attack.name,
                    attack = calculateAttack(attack.attackBase, group.size, group.might),
                    rawDamage = calculateDamage(attack.damageBase, group.size, group.might),
                    minimumDice = attack.minimumDice
                )
            }
            custom != null -> custom.attacks.map { attack ->
                BattleGroupDerivedAttack(
                    name = attack.name,
                    attack = calculateAttack(attack.attackBase, group.size, group.might),
                    rawDamage = calculateDamage(attack.damageBase, group.size, group.might),
                    minimumDice = attack.minimumDice
                )
            }
            else -> return null
        }

        val defenseBase = troop?.defenseBase ?: custom?.defenseBase ?: return null
        val magnitudeBase = troop?.magnitudeBase ?: custom?.magnitudeBase ?: return null
        val soakBase = troop?.soakBase ?: custom?.soakBase ?: return null
        val drill = group.drill
        val mightDefense = when (group.might) { 1, 2 -> 1; 3 -> 2; else -> 0 }
        return BattleGroupDerivedStats(
            joinBattle = troop?.joinBattle ?: custom?.joinBattle ?: return null,
            attacks = attacks,
            defense = calculateDefense(defenseBase, drill.defenseModifier, mightDefense),
            magnitude = calculateMagnitude(magnitudeBase, group.size),
            soak = calculateSoak(soakBase, group.size),
            senses = troop?.senses ?: custom?.senses ?: return null,
            resolve = troop?.resolve ?: custom?.resolve ?: return null,
            resist = troop?.resist ?: custom?.resist ?: return null,
            routDifficulty = troop?.routDifficulty ?: custom?.routDifficulty ?: return null,
            perfectMorale = troop?.perfectMorale ?: custom?.perfectMorale ?: return null,
            commandModifier = drill.commandModifier,
            routRallyDifficultyModifier = drill.routRallyDifficultyModifier
        )
    }
}
