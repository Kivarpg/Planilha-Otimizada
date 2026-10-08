package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random

class EncounterDistributionServiceBudgetTest {
    @Test
    fun `distribuirHabilidades consome exatamente 28 pontos normais sem bonus implícito`() {
        val resultado = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            supernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Armas Brancas"),
            random = Random(1234)
        )

        assertEquals(28, resultado.abilities.values.sum())
        assertTrue(resultado.abilities.values.all { it in 0..3 })
    }

    @Test
    fun `pontos de bonus são a única etapa que pode levar habilidades acima de 3`() {
        val base = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            supernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Armas Brancas"),
            random = Random(5678)
        )
        val (final, _) = EncounterDistributionService.distribuirPontosDeBonus(
            arquetipo = ArquetipoEncontro.FISICO,
            abilitiesBase = base.abilities,
            habilidadesFavorecidasOuCasta = listOf("Armas Brancas"),
            forcaDeVontadeBase = 5,
            ordemPrioridade = listOf("Armas Brancas")
        )

        assertEquals(28, base.abilities.values.sum())
        assertTrue(final.values.all { it in 0..5 })
        assertTrue(final["Armas Brancas"]!! > base.abilities["Armas Brancas"]!!)
    }
    @Test
    fun `saldo de 3 PB compra forca de vontade e converte o 1 PB final em especialidade`() {
        val favorecidas = ExaltedConstants.ALL_25_ABILITIES.take(12)
        val base = ExaltedConstants.ALL_25_ABILITIES.associateWith {
            if (it in favorecidas) 4 else 5
        }

        val (final, willpower, precisaEspecialidadeAdicional) =
            EncounterDistributionService.distribuirPontosDeBonus(
                arquetipo = ArquetipoEncontro.SOCIAL,
                abilitiesBase = base,
                habilidadesFavorecidasOuCasta = favorecidas,
                forcaDeVontadeBase = 5,
                ordemPrioridade = favorecidas
            )

        assertTrue(precisaEspecialidadeAdicional)
        assertEquals(6, willpower)
        assertEquals(15, favorecidas.sumOf { (final[it] ?: 0) - (base[it] ?: 0) } + (willpower - 5) * 2 + 1)
    }

    @Test
    fun `habilidade estrutural relevante recebe prioridade repetida alem do ponto inicial`() {
        val estrutural = "Ocultismo"
        val resultado = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            supernal = "Armas Brancas",
            habilidadesFavorecidas = emptyList(),
            random = Random(1234),
            habilidadesEstruturaisRelevantes = listOf(estrutural)
        )

        assertTrue(
            (resultado.abilities[estrutural] ?: 0) >= 3,
            "Habilidade estrutural relevante deve ser priorizada para abrir caminhos de Encantos"
        )
    }

    @Test
    fun `habilidade estrutural relevante recebe prioridade tambem nos pontos de bonus`() {
        val estrutural = "Ocultismo"
        val favorecidas = listOf("Armas Brancas")
        val base = ExaltedConstants.ALL_25_ABILITIES.associateWith { nome ->
            when (nome) {
                "Armas Brancas" -> 3
                estrutural -> 1
                else -> 0
            }
        }

        val (final, _) = EncounterDistributionService.distribuirPontosDeBonus(
            arquetipo = ArquetipoEncontro.FISICO,
            abilitiesBase = base,
            habilidadesFavorecidasOuCasta = favorecidas,
            forcaDeVontadeBase = 5,
            ordemPrioridade = favorecidas,
            habilidadeCombate = "Armas Brancas",
            habilidadesEstruturaisRelevantes = listOf(estrutural)
        )

        assertTrue(
            (final[estrutural] ?: 0) >= 2,
            "Habilidade estrutural relevante deve receber compras de PB antes do fallback global"
        )
    }

    @Test
    fun `especialidade prioriza habilidade estrutural do perfil antes do fallback aleatorio`() {
        val resultado = EncounterDistributionService.distribuirEspecialidades(
            arquetipo = ArquetipoEncontro.SOCIAL,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            habilidadeSocialOuMental = "Presença",
            abilities = mapOf(
                "Armas Brancas" to 3,
                "Esquiva" to 3,
                "Presença" to 3,
                "Socialização" to 3,
                "Ocultismo" to 3
            ),
            random = Random(2468),
            habilidadesEstruturaisRelevantes = listOf("Ocultismo")
        )

        assertEquals(4, resultado.size)
        assertTrue(
            resultado.any { it.habilidade == "Ocultismo" },
            "A especialidade deve priorizar uma habilidade estrutural relevante"
        )
    }

    @Test
    fun `mesma seed produz a mesma ordem de prioridades de habilidades`() {
        fun gerar(seed: Int) = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            supernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Armas Brancas", "Esquiva", "Atletismo"),
            random = Random(seed),
            habilidadesEstruturaisRelevantes = listOf("Ocultismo", "Investigação")
        )

        assertEquals(gerar(2468), gerar(2468))
    }


    @Test
    fun `combatente prioriza raciocinio para juntar se a batalha sem alterar orcamento`() {
        val primarios = ExaltedConstants.PHYSICAL_ATTRIBUTES
        val secundarios = ExaltedConstants.SOCIAL_ATTRIBUTES
        val terciarios = ExaltedConstants.MENTAL_ATTRIBUTES

        val resultado = EncounterDistributionService.distribuirAtributos(
            primarios, secundarios, terciarios, Random(4815), ArquetipoEncontro.FISICO
        )

        val mentais = ExaltedConstants.MENTAL_ATTRIBUTES
        assertEquals(
            mentais.maxOf { resultado.getValue(it) },
            resultado.getValue("Raciocínio")
        )
        assertEquals(11, primarios.sumOf { resultado.getValue(it) })
        assertEquals(9, secundarios.sumOf { resultado.getValue(it) })
        assertEquals(7, terciarios.sumOf { resultado.getValue(it) })
    }

    @Test
    fun `combatente inclui prontidao entre habilidades estruturais de entrada em combate`() {
        val resultado = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.FISICO,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Armas Brancas",
            supernal = null,
            habilidadesFavorecidas = emptyList(),
            random = Random(2026)
        )

        assertTrue(
            (resultado.abilities["Prontidão"] ?: 0) >= 3,
            "Prontidão deve receber prioridade porque Raciocínio + Prontidão define Juntar-se à Batalha"
        )
    }
}
