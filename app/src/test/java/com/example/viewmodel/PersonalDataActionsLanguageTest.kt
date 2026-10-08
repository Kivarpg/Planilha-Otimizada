package com.example.viewmodel

import com.example.model.CharacterSheet
import com.example.model.Merito
import com.example.model.CharacterType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalDataActionsLanguageTest {
    private fun actions(initial: CharacterSheet): Pair<MutableStateFlow<CharacterSheet>, PersonalDataActions> {
        val state = MutableStateFlow(initial)
        val errors = MutableStateFlow<String?>(null)
        val experience = ExperienceActions(errors)
        return state to PersonalDataActions(state, experience, errors)
    }

    @Test
    fun `idioma adicional cria merito automatico de nivel um`() {
        val (state, actions) = actions(CharacterSheet())

        assertTrue(actions.addLinguaAdicional("Idioma da Floresta"))

        assertEquals(listOf("Idioma da Floresta"), state.value.linguasAdicionais)
        val merito = state.value.merits.single()
        assertEquals("Idioma", merito.nome)
        assertEquals(1, merito.valor)
        assertEquals("Idioma da Floresta", merito.detalhe)
        assertEquals("Idioma", merito.origemAutomatica)
    }

    @Test
    fun `remover idioma adicional remove somente o merito automatico correspondente`() {
        val manual = Merito(nome = "Idioma", valor = 2, categoria = "Idioma")
        val (state, actions) = actions(
            CharacterSheet(
                linguasAdicionais = listOf("Idioma da Floresta", "Idioma do Fogo"),
                merits = listOf(
                    manual,
                    Merito(nome = "Idioma", valor = 1, categoria = "Idioma", detalhe = "Idioma da Floresta", origemAutomatica = "Idioma"),
                    Merito(nome = "Idioma", valor = 1, categoria = "Idioma", detalhe = "Idioma do Fogo", origemAutomatica = "Idioma")
                )
            )
        )

        actions.removeLinguaAdicional("Idioma da Floresta")

        assertEquals(listOf("Idioma do Fogo"), state.value.linguasAdicionais)
        assertEquals(2, state.value.merits.size)
        assertTrue(state.value.merits.any { it.id == manual.id })
        assertTrue(state.value.merits.any { it.detalhe == "Idioma do Fogo" && it.origemAutomatica == "Idioma" })
    }

    @Test
    fun `idioma nativo nao cria merito e remove merito do adicional quando houver conflito`() {
        val (state, actions) = actions(
            CharacterSheet(
                linguasAdicionais = listOf("Idioma da Floresta"),
                merits = listOf(Merito(nome = "Idioma", valor = 1, categoria = "Idioma", detalhe = "Idioma da Floresta", origemAutomatica = "Idioma"))
            )
        )

        actions.updateLinguaNativa("Idioma da Floresta")

        assertEquals("Idioma da Floresta", state.value.linguaNativa)
        assertTrue(state.value.linguasAdicionais.isEmpty())
        assertTrue(state.value.merits.isEmpty())
    }
    @Test
    fun `idioma adicional de sangue de dragao usa os cinco pontos adicionais de idioma`() {
        val (state, actions) = actions(
            CharacterSheet(
                tipoPersonagem = CharacterType.DRAGON_BLOODED,
                merits = (1..13).map { Merito(nome = "Mérito $it", valor = 1) }
            )
        )

        assertTrue(actions.addLinguaAdicional("Idioma do Fogo"))
        assertEquals(14, state.value.merits.sumOf { it.valor })
        assertEquals("Idioma do Fogo", state.value.merits.last().detalhe)
    }

    @Test
    fun `idioma adicional nao ultrapassa os dezoito pontos de merito de sangue de dragao`() {
        val (state, actions) = actions(
            CharacterSheet(
                tipoPersonagem = CharacterType.DRAGON_BLOODED,
                merits = (1..18).map { Merito(nome = "Mérito $it", valor = 1) }
            )
        )

        assertTrue(!actions.addLinguaAdicional("Idioma do Fogo"))
        assertEquals(18, state.value.merits.sumOf { it.valor })
        assertTrue(state.value.linguasAdicionais.isEmpty())
    }


    @Test
    fun `Antigo Reino e bloqueado sem Ocultismo ou Conhecimento`() {
        val (state, actions) = actions(CharacterSheet())
        assertTrue(!actions.addLinguaAdicional("Antigo Reino"))
        assertTrue(state.value.linguasAdicionais.isEmpty())
    }

    @Test
    fun `Antigo Reino e permitido com Ocultismo um`() {
        val (state, actions) = actions(CharacterSheet(abilities = CharacterSheet().abilities + ("Ocultismo" to 1)))
        assertTrue(actions.addLinguaAdicional("Antigo Reino"))
        assertTrue("Antigo Reino" in state.value.linguasAdicionais)
    }

    @Test
    fun `Antigo Reino e permitido com Conhecimento um`() {
        val (state, actions) = actions(CharacterSheet(abilities = CharacterSheet().abilities + ("Conhecimento" to 1)))
        assertTrue(actions.addLinguaAdicional("Antigo Reino"))
        assertTrue("Antigo Reino" in state.value.linguasAdicionais)
    }
}
