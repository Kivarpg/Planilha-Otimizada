package com.example.ui.tabs

import com.example.model.Encanto
import com.example.model.EncantoEncontro
import org.junit.Assert.assertEquals
import org.junit.Test

class CharmDisplayGroupingTest {
    @Test
    fun groupsRepeatedAcquisitionsOfAnAccumulatingCharm() {
        val bodyOfBull = Encanto(
            id = "a1",
            nome = "Técnica do Corpo de Touro",
            habilidadeVinculada = "Resistência"
        )
        val bodyOfBullAgain = bodyOfBull.copy(id = "a2")
        val sameNameOtherAbility = bodyOfBull.copy(id = "a3", habilidadeVinculada = "Atletismo")

        val grouped = groupAccumulatedCharms(listOf(bodyOfBull, bodyOfBullAgain, sameNameOtherAbility))

        assertEquals(2, grouped.size)
        assertEquals("Técnica do Corpo de Touro", grouped[0].charm.nome)
        assertEquals(2, grouped[0].quantity)
        assertEquals("Resistência", grouped[0].charm.habilidadeVinculada)
        assertEquals(1, grouped[1].quantity)
        assertEquals("Atletismo", grouped[1].charm.habilidadeVinculada)
    }

    @Test
    fun doesNotMergeDuplicateNonAccumulatingCharms() {
        val first = Encanto(
            id = "a1",
            nome = "Encanto Comum",
            habilidadeVinculada = "Presença"
        )
        val second = first.copy(id = "a2")

        val grouped = groupAccumulatedCharms(listOf(first, second))

        assertEquals(2, grouped.size)
        assertEquals(1, grouped[0].quantity)
        assertEquals(1, grouped[1].quantity)
    }
    @Test
    fun groupsRepeatedEncounterAcquisitionsOfBodyOfBull() {
        val first = EncantoEncontro(
            nome = "Técnica do Corpo de Touro",
            habilidadeVinculada = "Resistência",
            custo = "—"
        )
        val repeated = listOf(first, first.copy(), first.copy(), first.copy())
        val other = first.copy(habilidadeVinculada = "Atletismo")

        val grouped = groupAccumulatedEncounterCharms(repeated + other)

        assertEquals(2, grouped.size)
        assertEquals(4, grouped[0].quantity)
        assertEquals("Resistência", grouped[0].charm.habilidadeVinculada)
        assertEquals("Técnica do Corpo de Touro (x4)", formatGroupedEncounterCharmName(grouped[0]))
        assertEquals(1, grouped[1].quantity)
        assertEquals("Técnica do Corpo de Touro", formatGroupedEncounterCharmName(grouped[1]))
    }

}
