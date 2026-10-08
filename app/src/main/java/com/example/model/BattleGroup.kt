package com.example.model

import java.util.UUID

/** A configured Battle Group. Catalog data remains external and immutable. */
data class BattleGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val troopTypeName: String? = null,
    val size: Int = 1,
    val drill: BattleGroupDrill = BattleGroupDrill.POOR,
    val might: Int = 0,
    val customStats: BattleGroupCustomStats? = null,
    /** Magnitude atual em combate — pedido explícito do usuário (Parte C).
     * null = ainda não reduzida; usar o valor derivado
     * (BattleGroupDerivedStats.magnitude) como base de exibição. */
    val currentMagnitude: Int? = null
)

enum class BattleGroupDrill(
    val commandModifier: Int,
    val defenseModifier: Int,
    val routRallyDifficultyModifier: Int
) {
    POOR(-2, 0, 1),
    AVERAGE(0, 1, 0),
    ELITE(2, 2, 0)
}

data class BattleGroupCustomStats(
    val joinBattle: Int,
    val attacks: List<BattleGroupCustomAttack>,
    val defenseBase: Int,
    val magnitudeBase: Int,
    val soakBase: Int,
    val senses: Int,
    val resolve: Int,
    val resist: String,
    val routDifficulty: Int? = null,
    val perfectMorale: Boolean = false
)

data class BattleGroupCustomAttack(
    val name: String,
    val attackBase: Int,
    val damageBase: Int,
    val minimumDice: Int? = null
)

data class BattleGroupDerivedAttack(
    val name: String,
    val attack: Int,
    val rawDamage: Int,
    val minimumDice: Int?
)

data class BattleGroupDerivedStats(
    val joinBattle: Int,
    val attacks: List<BattleGroupDerivedAttack>,
    val defense: Int,
    val magnitude: Int,
    val soak: Int,
    val senses: Int,
    val resolve: Int,
    val resist: String,
    val routDifficulty: Int?,
    val perfectMorale: Boolean,
    val commandModifier: Int,
    val routRallyDifficultyModifier: Int
)
