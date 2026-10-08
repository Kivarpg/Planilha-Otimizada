package com.example.ui.tabs

import com.example.model.ArquetipoEncontro
import com.example.model.NpcEncontro
import kotlin.test.Test
import kotlin.test.assertEquals

class EncounterCombatEquationsTest {
    @Test
    fun `social e mental preservam habilidades padrao distintas`() {
        val base = NpcEncontro(
            attributes = mapOf(
                "Destreza" to 3, "Raciocínio" to 3, "Manipulação" to 3,
                "Vigor" to 3, "Força" to 3
            ),
            abilities = mapOf(
                "Prontidão" to 0, "Socialização" to 4, "Investigação" to 2,
                "Briga" to 0, "Esquiva" to 0, "Integridade" to 0,
                "Dissimulação" to 0, "Atletismo" to 0
            ),
            habilidadeSuporte = ""
        )

        val social = EncounterCombatEquations.gerarCampos(base.copy(arquetipo = ArquetipoEncontro.SOCIAL))
        val mental = EncounterCombatEquations.gerarCampos(base.copy(arquetipo = ArquetipoEncontro.MENTAL))

        assertEquals("7", social.first { it.rotulo == "Ataque Fulminante" }.valorExibido)
        assertEquals("5", mental.first { it.rotulo == "Ataque Fulminante" }.valorExibido)
    }
}
