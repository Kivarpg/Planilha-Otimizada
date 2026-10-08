package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression audit for NPC creation budgets. These tests intentionally inspect
 * the pure distribution layer instead of Android/UI state.
 */
class EncounterGeneratorBudgetAuditTest {

    @Test
    fun `Solar e Sangue de Dragao mantem 28 pontos normais e PB separado`() {
        for (tipo in listOf("Solar", "Sangue de Dragão")) {
            for (arquetipo in ArquetipoEncontro.entries) {
                repeat(EncounterTestSamples.count(100)) { seed ->
                    val combate = if (seed % 2 == 0) "Armas Brancas" else "Briga"
                    val defesa = combate
                    val base = EncounterDistributionService.distribuirHabilidades(
                        arquetipo = arquetipo,
                        habilidadeCombate = combate,
                        habilidadeDefensivaObrigatoria = "Esquiva",
                        supernal = if (tipo == "Solar") combate else "",
                        habilidadesFavorecidas = listOf(combate, "Integridade"),
                        random = Random(seed.toLong())
                    )

                    assertEquals(28, base.abilities.values.sum(), "$tipo/$arquetipo: pontos normais")
                    assertTrue(base.abilities.values.all { it in 0..3 }, "$tipo/$arquetipo: PB implícito na base")

                    val (final, _) = EncounterDistributionService.distribuirPontosDeBonus(
                        arquetipo = arquetipo,
                        abilitiesBase = base.abilities,
                        habilidadesFavorecidasOuCasta = listOf(combate, "Integridade"),
                        forcaDeVontadeBase = 5,
                        ordemPrioridade = listOf(combate, "Integridade", "Esquiva")
                    )

                    var custo = 0
                    for (nome in ExaltedConstants.ALL_25_ABILITIES) {
                        val aumento = ((final[nome] ?: 0) - (base.abilities[nome] ?: 0)).coerceAtLeast(0)
                        custo += aumento * if (nome == combate || nome == "Integridade") 1 else 2
                    }
                    assertTrue(custo <= 15, "$tipo/$arquetipo: PB de Habilidades excedido: $custo")
                    assertTrue(final.values.all { it in 0..5 }, "$tipo/$arquetipo: Habilidade acima de 5")
                }
            }
        }
    }

    @Test
    fun `Lunar respeita custo real de 3 PB por Atributo de Casta ou Favorecido`() {
        val baseAbilities = ExaltedConstants.ALL_25_ABILITIES.associateWith { 3 }
        val baseAttributes = (ExaltedConstants.PHYSICAL_ATTRIBUTES + ExaltedConstants.SOCIAL_ATTRIBUTES + ExaltedConstants.MENTAL_ATTRIBUTES)
            .associateWith { 1 }

        repeat(EncounterTestSamples.count(100)) { seed ->
            val result = EncounterDistributionService.distribuirPontosDeBonusLunar(
                arquetipo = ArquetipoEncontro.FISICO,
                abilitiesBase = baseAbilities,
                attributesBase = baseAttributes,
                atributosCastaOuFavorecidos = ExaltedConstants.PHYSICAL_ATTRIBUTES,
                habilidadeCombate = "Armas Brancas",
                habilidadeDefensiva = "Esquiva",
                random = Random(seed.toLong())
            )

            val custoHabilidades = result.abilities.entries.sumOf { (nome, valor) ->
                ((valor - (baseAbilities[nome] ?: 0)).coerceAtLeast(0)) * 2
            }
            val custoAtributos = result.attributes.entries.sumOf { (nome, valor) ->
                if (nome in ExaltedConstants.PHYSICAL_ATTRIBUTES) (valor - (baseAttributes[nome] ?: 1)).coerceAtLeast(0) * 3 else 0
            }
            val custoVontade = (result.forcaDeVontade - 5).coerceAtLeast(0) * 2

            assertTrue(custoHabilidades + custoAtributos + custoVontade <= 15, "PB Lunar excedido: ${custoHabilidades + custoAtributos + custoVontade}")
            assertTrue(result.abilities.values.all { it in 0..5 })
            assertTrue(result.attributes.values.all { it in 1..5 })
            assertTrue(result.forcaDeVontade in 6..7)
        }
    }
    @Test
    fun `Solar e Sangue de Dragao consomem exatamente os 15 PB quando base permite compra normal`() {
        for (arquetipo in ArquetipoEncontro.entries) {
            val base = EncounterDistributionService.distribuirHabilidades(
                arquetipo = arquetipo,
                habilidadeCombate = "Armas Brancas",
                habilidadeDefensivaObrigatoria = "Esquiva",
                supernal = "Armas Brancas",
                habilidadesFavorecidas = listOf("Armas Brancas", "Integridade", "Esquiva", "Presença", "Conhecimento"),
                random = Random(100 + arquetipo.ordinal)
            )
            val (final, willpower) = EncounterDistributionService.distribuirPontosDeBonus(
                arquetipo = arquetipo,
                abilitiesBase = base.abilities,
                habilidadesFavorecidasOuCasta = listOf("Armas Brancas", "Integridade", "Esquiva", "Presença", "Conhecimento"),
                forcaDeVontadeBase = 5,
                ordemPrioridade = listOf("Armas Brancas", "Integridade", "Esquiva", "Presença", "Conhecimento")
            )
            var gasto = 0
            for (nome in ExaltedConstants.ALL_25_ABILITIES) {
                val aumento = ((final[nome] ?: 0) - (base.abilities[nome] ?: 0)).coerceAtLeast(0)
                gasto += aumento * if (nome in setOf("Armas Brancas", "Integridade", "Esquiva", "Presença", "Conhecimento")) 1 else 2
            }
            gasto += (willpower - 5).coerceAtLeast(0) * 2
            assertEquals(15, gasto, "$arquetipo: os 15 PB devem ser integralmente distribuídos")
        }
    }

}
