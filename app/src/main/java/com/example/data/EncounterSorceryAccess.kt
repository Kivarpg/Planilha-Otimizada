package com.example.data

import com.example.model.EncantoEncontro
import com.example.model.NOME_FEITICARIA_TERRESTRE

/** Fonte única para detectar os círculos de Feitiçaria presentes nos Encantos do NPC. */
internal data class EncounterSorceryAccess(
    val terrestre: Boolean,
    val celestial: Boolean,
    val solar: Boolean
) {
    companion object {
        private const val CELESTIAL = "Feitiçaria do Círculo Celestial"
        private const val SOLAR = "Feitiçaria do Círculo Solar"

        fun from(charms: List<EncantoEncontro>): EncounterSorceryAccess =
            fromNames(charms.asSequence().map { it.nome })

        fun fromNames(nomes: Sequence<String>): EncounterSorceryAccess {
            var terrestre = false
            var celestial = false
            var solar = false
            nomes.forEach { nome ->
                when (nome) {
                    NOME_FEITICARIA_TERRESTRE -> terrestre = true
                    CELESTIAL -> celestial = true
                    SOLAR -> solar = true
                }
            }
            return EncounterSorceryAccess(terrestre, celestial, solar)
        }
    }
}
