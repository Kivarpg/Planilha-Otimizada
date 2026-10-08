package com.example.ui.tabs

import com.example.model.Aspecto
import com.example.model.CharacterSheet
import com.example.model.CharacterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryAbilitiesMarkerTest {
    @Test
    fun dragonBloodedUsesAspectAbilitiesEvenWhenStoredCasteAbilitiesAreEmpty() {
        val sheet = CharacterSheet(
            tipoPersonagem = CharacterType.DRAGON_BLOODED,
            aspecto = Aspecto.Fogo.displayName,
            casteAbilities = emptyList(),
            favoredAbilities = listOf("Conhecimento")
        )

        val marked = habilidadesDeCastaOuAspectoParaMarcador(sheet)

        assertEquals(5, marked.size)
        assertTrue("Armas Brancas" in marked)
        assertTrue("Presença" in marked)
        assertTrue("Esquiva" in marked)
        assertTrue("Socialização" in marked)
        assertTrue("Atletismo" in marked)
    }
}
