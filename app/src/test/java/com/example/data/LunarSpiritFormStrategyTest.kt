package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LunarSpiritFormStrategyTest {
    private fun encanto(
        nome: String,
        atributo: String,
        min: Int,
        rota: LunarCharmArchetypeRoute? = null
    ) = EncantoLunarDefinition(
        id = nome,
        atributo = atributo,
        subdivisao = null,
        nome = nome,
        nomeIngles = nome,
        custo = "—",
        minsTexto = "",
        minAtributo = min,
        minEssencia = 1,
        tipo = "Simples",
        palavrasChave = if (rota == null) "Nenhuma" else "Arquétipo (${rota.atributo})",
        duracao = "Instantânea",
        preRequisitos = "Nenhum",
        descricao = "",
        rotasArquetipo = listOfNotNull(rota)
    )

    @Test
    fun `porte do catalogo nao cria trait mecanico`() {
        val semTrait = SpiritualFormService.todasFormas().first {
            LunarSpiritShapeArchetypeTraits.forAnimal(it).isEmpty()
        }
        val traits = LunarSpiritShapeArchetypeTraits.forAnimal(semTrait)
        assertFalse(LunarSpiritTrait.MINUSCULO in traits)
        assertFalse(LunarSpiritTrait.TAMANHO_LENDARIO in traits)
    }

    @Test
    fun `encanto estrutural minusculo exige trait e foco de raciocinio`() {
        val grasshopper = encanto(
            "Forma do Gafanhoto Esmeralda", "Destreza", 4,
            LunarCharmArchetypeRoute("Raciocínio", "MINUSCULO", 4, "Nenhum")
        )
        val attrs = mapOf("Destreza" to 1, "Raciocínio" to 5)
        val sim = LunarSpiritFormStrategy.encantosEstruturais(
            setOf(LunarSpiritTrait.MINUSCULO), listOf(grasshopper), attrs, 1, "Raciocínio"
        )
        val nao = LunarSpiritFormStrategy.encantosEstruturais(
            emptySet(), listOf(grasshopper), attrs, 1, "Raciocínio"
        )
        assertEquals(listOf("Forma do Gafanhoto Esmeralda"), sim.map { it.nome })
        assertTrue(nao.isEmpty())
    }
}
