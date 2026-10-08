package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncounterPersonalizedFocusTest {
    @Test
    fun `habilidades personalizadas mapeiam para um arquetipo valido`() {
        ExaltedConstants.ALL_25_ABILITIES.forEach { habilidade ->
            assertTrue(EncounterGenerationRules.arquetipoParaHabilidade(habilidade) in ArquetipoEncontro.entries)
        }
    }

    @Test
    fun `atributos personalizados mapeiam para sua categoria`() {
        ExaltedConstants.PHYSICAL_ATTRIBUTES.forEach { assertEquals(ArquetipoEncontro.FISICO, EncounterGenerationRules.arquetipoParaAtributo(it)) }
        ExaltedConstants.SOCIAL_ATTRIBUTES.forEach { assertEquals(ArquetipoEncontro.SOCIAL, EncounterGenerationRules.arquetipoParaAtributo(it)) }
        ExaltedConstants.MENTAL_ATTRIBUTES.forEach { assertEquals(ArquetipoEncontro.MENTAL, EncounterGenerationRules.arquetipoParaAtributo(it)) }
    }

    @Test
    fun `prioridade lunar eleva atributo escolhido sem alterar total`() {
        val original = mapOf("Força" to 2, "Destreza" to 5, "Vigor" to 3,
            "Carisma" to 3, "Manipulação" to 2, "Aparência" to 2,
            "Percepção" to 3, "Inteligência" to 3, "Raciocínio" to 2)
        val ajustado = EncounterGenerationRules.priorizarAtributoSelecionado(original, "Força")
        assertEquals(original.values.sum(), ajustado.values.sum())
        assertEquals(5, ajustado["Força"])
    }
}
