package com.example.model

import java.util.UUID

// Nome canônico do Encanto "Técnica do Corpo de Touro" (Ox-Body Technique),
// usado para identificar aquisições e marcar as caixas de vitalidade que ele
// gera automaticamente na Trilha de Vitalidade.
const val NOME_CORPO_DE_TOURO = "Técnica do Corpo de Touro"
const val NOME_FEITICARIA_TERRESTRE = "Feitiçaria do Círculo Terrestre"

enum class Casta(val displayName: String) {
    Dawn(BoxNames.SolarCaste.DAWN),
    Zenith(BoxNames.SolarCaste.ZENITH),
    Twilight(BoxNames.SolarCaste.TWILIGHT),
    Night(BoxNames.SolarCaste.NIGHT),
    Eclipse(BoxNames.SolarCaste.ECLIPSE);

    fun allowedAbilities(): List<String> {
        return when (this) {
            Dawn -> listOf("Armas Brancas", "Arqueirismo", "Arremesso", "Briga", "Esquiva", "Guerra", "Prontidão", "Resistência")
            Zenith -> listOf("Atletismo", "Conhecimento", "Guerra", "Integridade", "Performance", "Presença", "Resistência", "Sobrevivência")
            Twilight -> listOf("Burocracia", "Conhecimento", "Integridade", "Investigação", "Linguística", "Medicina", "Ocultismo", "Ofícios")
            Night -> listOf("Atletismo", "Cavalgar", "Crime", "Esquiva", "Furtividade", "Investigação", "Prontidão", "Socialização")
            Eclipse -> listOf("Burocracia", "Cavalgar", "Crime", "Linguística", "Navegação", "Ocultismo", "Presença", "Socialização")
        }
    }
}

object ExaltedConstants {
    val ALL_25_ABILITIES = listOf(
        "Armas Brancas", "Arqueirismo", "Arremesso", "Atletismo", "Briga",
        "Burocracia", "Cavalgar", "Conhecimento", "Crime", "Esquiva",
        "Furtividade", "Guerra", "Integridade", "Investigação", "Linguística",
        "Medicina", "Navegação", "Ocultismo", "Ofícios", "Performance",
        "Presença", "Prontidão", "Resistência", "Sobrevivência", "Socialização"
    )

    val ABILITY_INDEX_BY_NAME = ALL_25_ABILITIES.withIndex().associate { it.value to it.index }

    val PHYSICAL_ATTRIBUTES = listOf("Força", "Destreza", "Vigor")
    val SOCIAL_ATTRIBUTES = listOf("Carisma", "Manipulação", "Aparência")
    val MENTAL_ATTRIBUTES = listOf("Percepção", "Inteligência", "Raciocínio")
    val ALL_ATTRIBUTES = PHYSICAL_ATTRIBUTES + SOCIAL_ATTRIBUTES + MENTAL_ATTRIBUTES

    val POWER_CATEGORIES = listOf("Encanto", "Feitiçaria", "Necromancia")
    const val OCCULT_ABILITY = "Ocultismo"

    val DEFAULT_ATTRIBUTES = ALL_ATTRIBUTES.associateWith { 1 }
    val DEFAULT_ABILITIES = ALL_25_ABILITIES.associateWith { 0 }

    fun defaultHealthBoxes(): List<CaixaVitalidade> {
        return listOf(
            CaixaVitalidade(UUID.randomUUID().toString(), "-0", 0, isPermanente = true),
            CaixaVitalidade(UUID.randomUUID().toString(), "-1", 0, isPermanente = true),
            CaixaVitalidade(UUID.randomUUID().toString(), "-1", 0, isPermanente = true),
            CaixaVitalidade(UUID.randomUUID().toString(), "-2", 0, isPermanente = true),
            CaixaVitalidade(UUID.randomUUID().toString(), "-2", 0, isPermanente = true),
            CaixaVitalidade(UUID.randomUUID().toString(), "-4", 0, isPermanente = true),
            CaixaVitalidade(UUID.randomUUID().toString(), "Inc", 0, isPermanente = true)
        )
    }
}
