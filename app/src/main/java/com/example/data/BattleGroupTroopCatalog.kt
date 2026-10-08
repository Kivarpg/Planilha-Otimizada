package com.example.data

// Battle Group troop catalog extracted from the supplied Size 1 / Poor / Might 0 screenshots.
// The Base fields below are normalized to Size 0 for values affected by Size.
// Join Battle, Defense, Senses, Resolve and Resist are taken directly from the supplied results.

data class BattleGroupAttackBase(
    val name: String,
    val attackBase: Int,
    val damageBase: Int,
    val minimumDice: Int? = null
)

data class BattleGroupTroopType(
    val name: String,
    val joinBattle: Int,
    val attacks: List<BattleGroupAttackBase>,
    val defenseBase: Int,
    val magnitudeBase: Int,
    val soakBase: Int,
    val senses: Int,
    val resolve: Int,
    val resist: String,
    val routDifficulty: Int? = null,
    val perfectMorale: Boolean = false
)

val battleGroupTroopTypes = listOf(
    BattleGroupTroopType(
        name = "Anhule",
        joinBattle = 7,
        attacks = listOf(
            BattleGroupAttackBase("Bite", 10, 13, null),
            BattleGroupAttackBase("Web Grapple", 10, 10, null),
        ),
        defenseBase = 4, magnitudeBase = 15, soakBase = 8,
        senses = 10, resolve = 3, resist = "0",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Automata",
        joinBattle = 8,
        attacks = listOf(
            BattleGroupAttackBase("Sword", 8, 16, 2),
            BattleGroupAttackBase("Unarmed", 10, 14, null),
        ),
        defenseBase = 4, magnitudeBase = 16, soakBase = 13,
        senses = 14, resolve = 1, resist = "immune",
        routDifficulty = null, perfectMorale = true
    ),
    BattleGroupTroopType(
        name = "Battle-Ready Troops",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Chopping sword", 7, 12, 2),
            BattleGroupAttackBase("Long spear", 5, 14, null),
            BattleGroupAttackBase("Shield bash", 6, 12, null),
        ),
        defenseBase = 4, magnitudeBase = 7, soakBase = 8,
        senses = 4, resolve = 2, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Blood Apes",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Claws", 11, 15, null),
        ),
        defenseBase = 4, magnitudeBase = 9, soakBase = 7,
        senses = 6, resolve = 3, resist = "8",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Bonesiders",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Claw", 6, 11, null),
        ),
        defenseBase = 4, magnitudeBase = 7, soakBase = 4,
        senses = 1, resolve = 2, resist = "immune",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Brides of Ahlat",
        joinBattle = 8,
        attacks = listOf(
            BattleGroupAttackBase("Short spear", 10, 11, null),
            BattleGroupAttackBase("Shield bash", 5, 13, null),
            BattleGroupAttackBase("Firewand", 9, 11, 2),
            BattleGroupAttackBase("Sling", 8, 11, null),
        ),
        defenseBase = 5, magnitudeBase = 7, soakBase = 4,
        senses = 4, resolve = 4, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Buck-Ogres",
        joinBattle = 8,
        attacks = listOf(
            BattleGroupAttackBase("Axe", 7, 18, null),
            BattleGroupAttackBase("Antlers", 8, 16, null),
            BattleGroupAttackBase("Shield", 8, 16, null),
        ),
        defenseBase = 5, magnitudeBase = 12, soakBase = 13,
        senses = 8, resolve = 4, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Conscripts",
        joinBattle = 5,
        attacks = listOf(
            BattleGroupAttackBase("Short sword", 6, 11, 2),
            BattleGroupAttackBase("Self bow", 8, 9, null),
        ),
        defenseBase = 2, magnitudeBase = 7, soakBase = 6,
        senses = 4, resolve = 1, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Crystalmoths",
        joinBattle = 5,
        attacks = listOf(
            BattleGroupAttackBase("Bite", 9, 9, null),
            BattleGroupAttackBase("Slice", 10, 13, null),
        ),
        defenseBase = 4, magnitudeBase = 8, soakBase = 2,
        senses = 7, resolve = 1, resist = "0",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Drowned",
        joinBattle = 4,
        attacks = listOf(
            BattleGroupAttackBase("Grapple", 6, 9, null),
            BattleGroupAttackBase("Knife", 8, 10, null),
        ),
        defenseBase = 3, magnitudeBase = 11, soakBase = 3,
        senses = 0, resolve = 3, resist = "immune",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Elite Troops",
        joinBattle = 10,
        attacks = listOf(
            BattleGroupAttackBase("Slashing sword", 11, 12, 2),
        ),
        defenseBase = 5, magnitudeBase = 7, soakBase = 10,
        senses = 9, resolve = 4, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Fair Folk Cataphracts",
        joinBattle = 11,
        attacks = listOf(
            BattleGroupAttackBase("Horizon Cutter", 16, 16, 2),
            BattleGroupAttackBase("Dream Curdling Bow", 14, 16, null),
        ),
        defenseBase = 7, magnitudeBase = 18, soakBase = 14,
        senses = 11, resolve = 4, resist = "12",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Fair Folk Hobgoblins",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Bite", 7, 14, null),
            BattleGroupAttackBase("Talons", 9, 12, null),
        ),
        defenseBase = 5, magnitudeBase = 7, soakBase = 5,
        senses = 4, resolve = 2, resist = "6",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Grelidaka",
        joinBattle = 8,
        attacks = listOf(
            BattleGroupAttackBase("Claw", 6, 9, null),
            BattleGroupAttackBase("Peck", 6, 5, null),
        ),
        defenseBase = 3, magnitudeBase = 5, soakBase = 2,
        senses = 6, resolve = 4, resist = "6",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Grenmaws",
        joinBattle = 8,
        attacks = listOf(
            BattleGroupAttackBase("Bite", 8, 19, 2),
            BattleGroupAttackBase("Tail Slam", 10, 17, null),
        ),
        defenseBase = 5, magnitudeBase = 15, soakBase = 10,
        senses = 9, resolve = 3, resist = "9",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Griefbees",
        joinBattle = 5,
        attacks = listOf(
            BattleGroupAttackBase("Sting", 4, 4, null),
        ),
        defenseBase = 4, magnitudeBase = 7, soakBase = 4,
        senses = 6, resolve = 4, resist = "immune",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Haunts",
        joinBattle = 4,
        attacks = listOf(
            BattleGroupAttackBase("Short sword", 6, 12, null),
            BattleGroupAttackBase("Shield bash", 5, 12, null),
        ),
        defenseBase = 3, magnitudeBase = 10, soakBase = 8,
        senses = 4, resolve = 5, resist = "immune",
        routDifficulty = null, perfectMorale = true
    ),
    BattleGroupTroopType(
        name = "Keregost's Hundred Handed Army",
        joinBattle = 11,
        attacks = listOf(
            BattleGroupAttackBase("Grand Daiklaive", 9, 19, null),
            BattleGroupAttackBase("Longbow", 8, 14, null),
            BattleGroupAttackBase("Grapple", 9, 12, null),
            BattleGroupAttackBase("Hook Sword", 9, 14, null),
        ),
        defenseBase = 4, magnitudeBase = 32, soakBase = 16,
        senses = 0, resolve = 5, resist = "immune",
        routDifficulty = null, perfectMorale = true
    ),
    BattleGroupTroopType(
        name = "Lava Moths",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Wing buffet", 9, 9, null),
        ),
        defenseBase = 5, magnitudeBase = 9, soakBase = 6,
        senses = 5, resolve = 2, resist = "0",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Lintha Reaver",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Axe", 7, 12, 2),
            BattleGroupAttackBase("Dagger", 9, 10, null),
            BattleGroupAttackBase("Javelin", 8, 12, null),
        ),
        defenseBase = 4, magnitudeBase = 7, soakBase = 8,
        senses = 5, resolve = 3, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Metody",
        joinBattle = 5,
        attacks = listOf(
            BattleGroupAttackBase("Strike", 7, 10, null),
            BattleGroupAttackBase("Grapple", 7, 6, null),
        ),
        defenseBase = 4, magnitudeBase = 10, soakBase = 8,
        senses = 5, resolve = 4, resist = "8",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Nomad Horse-Archers",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Scimitar", 9, 12, 2),
            BattleGroupAttackBase("Long spear", 8, 14, null),
            BattleGroupAttackBase("Self bow", 11, 10, null),
        ),
        defenseBase = 4, magnitudeBase = 7, soakBase = 8,
        senses = 7, resolve = 4, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Parasitic Shadows",
        joinBattle = 6,
        attacks = listOf(
            BattleGroupAttackBase("Strangle", 5, 2, null),
        ),
        defenseBase = 5, magnitudeBase = 4, soakBase = 1,
        senses = 6, resolve = 1, resist = "0",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Silverwights",
        joinBattle = 5,
        attacks = listOf(
            BattleGroupAttackBase("Bite", 5, 12, null),
            BattleGroupAttackBase("Claws", 9, 8, null),
        ),
        defenseBase = 4, magnitudeBase = 4, soakBase = 3,
        senses = 5, resolve = 1, resist = "2",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Singers of the Deep",
        joinBattle = 4,
        attacks = listOf(
            BattleGroupAttackBase("Talons", 9, 12, null),
            BattleGroupAttackBase("Grapple", 9, 9, null),
        ),
        defenseBase = 4, magnitudeBase = 9, soakBase = 5,
        senses = 5, resolve = 3, resist = "6",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Tomescu",
        joinBattle = 7,
        attacks = listOf(
            BattleGroupAttackBase("Sword", 10, 10, null),
            BattleGroupAttackBase("Axe", 10, 12, null),
            BattleGroupAttackBase("Sledge", 10, 14, null),
            BattleGroupAttackBase("Bow", 10, 10, null),
        ),
        defenseBase = 5, magnitudeBase = 9, soakBase = 8,
        senses = 7, resolve = 3, resist = "0",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Walkuren",
        joinBattle = 9,
        attacks = listOf(
            BattleGroupAttackBase("Longspear", 9, 14, null),
        ),
        defenseBase = 3, magnitudeBase = 9, soakBase = 9,
        senses = 8, resolve = 4, resist = "immune",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "War Ghosts",
        joinBattle = 5,
        attacks = listOf(
            BattleGroupAttackBase("Sword", 7, 12, 2),
        ),
        defenseBase = 4, magnitudeBase = 11, soakBase = 8,
        senses = 5, resolve = 4, resist = "immune",
        routDifficulty = 2, perfectMorale = false
    ),
    BattleGroupTroopType(
        name = "Zombies",
        joinBattle = 2,
        attacks = listOf(
            BattleGroupAttackBase("Bite", 5, 16, null),
        ),
        defenseBase = 2, magnitudeBase = 13, soakBase = 3,
        senses = 4, resolve = 1, resist = "immune",
        routDifficulty = null, perfectMorale = true
    )
)

// Custom Army is intentionally not included above: its values are user-entered rather than a fixed troop type.

