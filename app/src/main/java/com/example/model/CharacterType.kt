package com.example.model

/** Canonical identifiers persisted in CharacterSheet.tipoPersonagem. */
object CharacterType {
    const val SOLAR = "Solar"
    const val DRAGON_BLOODED = "SangueDeDragao"
    const val LUNAR = "Lunar"

    fun isSupported(value: String): Boolean = value == SOLAR || value == DRAGON_BLOODED || value == LUNAR
}

fun String.isDragonBlooded(): Boolean = this == CharacterType.DRAGON_BLOODED
fun String.isSolar(): Boolean = this == CharacterType.SOLAR
fun String.isLunar(): Boolean = this == CharacterType.LUNAR
