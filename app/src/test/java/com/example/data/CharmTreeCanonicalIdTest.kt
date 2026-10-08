package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CharmTreeCanonicalIdTest {
    @Test fun `solar tree conversion preserves canonical id`() {
        val def = EncantoSolarDefinition(
            id = "solar_canonic", habilidade = "Armas Brancas", nome = "Teste", nomeIngles = "",
            custo = "", minsTexto = "Armas Brancas 1, Essência 1", minHabilidade = 1, minEssencia = 1,
            tipo = "", palavrasChave = "", duracao = "", preRequisitos = "", descricao = ""
        )
        assertEquals("solar_canonic", def.toEncanto().id)
    }

    @Test fun `lunar tree conversion preserves canonical id`() {
        val def = EncantoLunarDefinition(
            id = "lunar_canonic", atributo = "Força", subdivisao = null, nome = "Teste", nomeIngles = "",
            custo = "", minsTexto = "Força 1, Essência 1", minAtributo = 1, minEssencia = 1,
            tipo = "", palavrasChave = "", duracao = "", preRequisitos = "", descricao = ""
        )
        assertEquals("lunar_canonic", def.toEncanto().id)
    }
}
