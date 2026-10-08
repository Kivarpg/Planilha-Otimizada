package com.example.viewmodel

import com.example.model.CharacterSheet
import com.example.model.CharacterType
import com.example.model.LunarCasta
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CasteActionsLunarTest {
    private fun actions(initial: CharacterSheet = CharacterSheet(tipoPersonagem = CharacterType.LUNAR, lunarCasta = LunarCasta.FullMoon, lunarCastaEscolhida = true)): Pair<MutableStateFlow<CharacterSheet>, CasteActions> {
        val state = MutableStateFlow(initial)
        return state to CasteActions(state)
    }

    @Test
    fun `Lunar seleciona exatamente dois atributos de casta`() {
        val (state, actions) = actions()
        actions.toggleLunarCasteAttribute("Força")
        actions.toggleLunarCasteAttribute("Destreza")
        actions.toggleLunarCasteAttribute("Vigor")

        assertEquals(listOf("Força", "Destreza"), state.value.lunarCasteAttributesEscolhidos)
    }

    @Test
    fun `Lunar impede favorecido sobre atributo de casta`() {
        val (state, actions) = actions()
        actions.toggleLunarCasteAttribute("Força")
        actions.toggleFavoredAttribute("Força")

        assertEquals(listOf("Força"), state.value.lunarCasteAttributesEscolhidos)
        assertTrue(state.value.favoredAttributes.isEmpty())
    }

    @Test
    fun `Lunar permite dois favorecidos adicionais fora da casta`() {
        val (state, actions) = actions()
        actions.toggleLunarCasteAttribute("Força")
        actions.toggleLunarCasteAttribute("Destreza")
        actions.toggleFavoredAttribute("Vigor")
        actions.toggleFavoredAttribute("Percepção")
        actions.toggleFavoredAttribute("Inteligência")

        assertEquals(listOf("Força", "Destreza"), state.value.lunarCasteAttributesEscolhidos)
        assertEquals(listOf("Vigor", "Percepção"), state.value.favoredAttributes)
        assertFalse("Inteligência" in state.value.favoredAttributes)
    }

    @Test
    fun `Lunar permite remover favorecido mesmo se estado antigo tiver duplicacao`() {
        val initial = CharacterSheet(
            tipoPersonagem = CharacterType.LUNAR,
            lunarCasta = LunarCasta.FullMoon,
            lunarCastaEscolhida = true,
            lunarCasteAttributesEscolhidos = listOf("Força"),
            favoredAttributes = listOf("Força", "Percepção")
        )
        val (state, actions) = actions(initial)

        actions.toggleFavoredAttribute("Força")

        assertEquals(listOf("Percepção"), state.value.favoredAttributes)
    }

    @Test
    fun `Lunar permite remover casta mesmo se estado antigo tiver duplicacao`() {
        val initial = CharacterSheet(
            tipoPersonagem = CharacterType.LUNAR,
            lunarCasta = LunarCasta.FullMoon,
            lunarCastaEscolhida = true,
            lunarCasteAttributesEscolhidos = listOf("Força"),
            favoredAttributes = listOf("Força", "Percepção")
        )
        val (state, actions) = actions(initial)

        actions.toggleLunarCasteAttribute("Força")

        assertTrue(state.value.lunarCasteAttributesEscolhidos.isEmpty())
    }
}
