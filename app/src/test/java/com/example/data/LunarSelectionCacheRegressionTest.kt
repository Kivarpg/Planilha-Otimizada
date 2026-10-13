package com.example.data

import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class LunarSelectionCacheRegressionTest {
    private fun charm(id: String, attribute: String, minimum: Int, routes: List<LunarCharmArchetypeRoute> = emptyList()) =
        EncantoLunarDefinition(id = id, atributo = attribute, subdivisao = null, nome = id,
            nomeIngles = id, custo = "", minsTexto = "", minAtributo = minimum, minEssencia = 1,
            tipo = "Reflexivo", palavrasChave = "", duracao = "", preRequisitos = "Nenhum",
            descricao = "", rotasArquetipo = routes)

    @Test fun `previous spirit form cannot hide legal priority route in next NPC`() {
        val catalog = listOf(
            charm("A fallback", "Inteligência", 1),
            charm("Z focus", "Destreza", 5, listOf(
                LunarCharmArchetypeRoute("Percepção", "VISAO_NOTURNA", 3, "Nenhum")))
        )
        fun select(traits: Set<LunarSpiritTrait>) = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = catalog, attributes = mapOf("Percepção" to 5, "Inteligência" to 3, "Destreza" to 1),
            essencia = 1, ordemAtributos = listOf("Percepção", "Inteligência"), quantidade = 1,
            random = Random(7), arquetipo = ArquetipoEncontro.MENTAL,
            atributoFocoUsuario = "Percepção", spiritTraits = traits)
        assertEquals(listOf("A fallback"), select(emptySet()).charms.map { it.nome })
        val nightVision = select(setOf(LunarSpiritTrait.VISAO_NOTURNA))
        assertEquals(listOf("Z focus"), nightVision.charms.map { it.nome })
        assertEquals("Percepção", nightVision.acquisitionAttributes["Z focus"])
        assertEquals(listOf("A fallback"), select(emptySet()).charms.map { it.nome })
    }
}
