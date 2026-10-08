package com.example.model

/**
 * Fonte central dos nomes exibidos dentro das caixas de seleção da ficha.
 *
 * ALTERAÇÕES DE NOMENCLATURA DEVEM SER FEITAS AQUI, e não diretamente nos
 * componentes visuais. Os enums Casta, Aspecto e LunarCasta consomem esta
 * fonte para manter uma única representação dos nomes exibidos.
 */
object BoxNames {
    object SolarCaste {
        const val DAWN = "Alvorecer"
        const val ZENITH = "Zênite"
        const val TWILIGHT = "Crepúsculo"
        const val NIGHT = "Noite"
        const val ECLIPSE = "Eclipse"

        val ALL = listOf(DAWN, ZENITH, TWILIGHT, NIGHT, ECLIPSE)
    }

    object DragonBloodedAspect {
        const val AIR = "Ar"
        const val EARTH = "Terra"
        const val FIRE = "Fogo"
        const val WATER = "Água"
        const val WOOD = "Madeira"

        val ALL = listOf(AIR, EARTH, FIRE, WATER, WOOD)
    }

    object LunarCaste {
        const val FULL_MOON = "Lua Cheia"
        const val CHANGING_MOON = "Lua Minguante"
        const val NO_MOON = "Lua Nova"
        const val CASTELESS = "Sem Casta"

        val ALL = listOf(FULL_MOON, CHANGING_MOON, NO_MOON, CASTELESS)
    }

    object PersonalData {
        const val TAB_TITLE = "Dados pessoais"
        const val CASTE = "Casta"
        const val ASPECT = "Aspecto"
        const val LANGUAGE = "Idioma"
        const val INTIMACIES = "Intimidades"
        const val ANIMA_DESCRIPTION = "Descrição da Anima"
        const val NAME = "Nome"
        const val PLAYER = "Jogador"
        const val CONCEPT = "Conceito"
        const val ASPECT_ABILITY = "Habilidade do Aspecto"
    }
}
