package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SolarEncounterGeneratorRegressionTest {
    @Test
    fun `Solar possui exatamente cinco Favorecidas incluindo o Supernal`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSolaresCatalog(app).definitions
        repeat(EncounterTestSamples.count(100)) { seed ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val npc = EncounterGenerator.gerarSolar(
                    nomeManual = "",
                    arquetipo = arquetipo,
                    encantosSolares = catalogo,
                    random = kotlin.random.Random(seed * 31L + arquetipo.ordinal)
                )
                assertEquals(5, npc.habilidadesFavorecidas.size)
                assertEquals(5, npc.habilidadesFavorecidas.distinct().size)
                assertTrue(npc.habilidadeSupernal.isNotBlank() && npc.habilidadeSupernal in npc.habilidadesFavorecidas)
            }
        }
    }

    @Test
    fun `Solar Mental com Feiticaria possui quatro Encantos de Ocultismo antes da inclusao`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSolaresCatalog(app).definitions
        repeat(EncounterTestSamples.count(100)) { seed ->
            val npc = EncounterGenerator.gerarSolar(
                nomeManual = "",
                arquetipo = ArquetipoEncontro.MENTAL,
                encantosSolares = catalogo,
                random = kotlin.random.Random(seed.toLong())
            )
            if (npc.charms.any { it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE }) {
                assertTrue(npc.charms.count { it.habilidadeVinculada.equals("Ocultismo", ignoreCase = true) } >= 4)
                assertTrue(npc.charms.size >= 15)
            }
        }
    }
    @Test
    fun `Solar Mental sem Ocultismo 3 nao reserva Feiticaria e preserva 15 Encantos`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSolaresCatalog(app).definitions
        repeat(EncounterTestSamples.count(100)) { seed ->
            val npc = EncounterGenerator.gerarSolar(
                nomeManual = "",
                arquetipo = ArquetipoEncontro.MENTAL,
                encantosSolares = catalogo,
                random = kotlin.random.Random(seed.toLong())
            )
            val ocultismo = npc.abilities[EncounterGenerationRules.HABILIDADE_OCULTISMO] ?: 0
            if (ocultismo < EncounterGenerationRules.MIN_OCULTISMO_FEITICARIA) {
                assertEquals(15, npc.charms.size)
                assertTrue(npc.charms.none { it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE })
            }
        }
    }

    @Test
    fun `Solar Fisico com Ocultismo 3 ou mais recebe o projeto completo de Feiticaria`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSolaresCatalog(app).definitions

        // A pré-condição Ocultismo >= 3 é controlada diretamente aqui.
        // Não dependemos de uma seed aleatória produzir esse valor em um
        // arquétipo Físico, pois isso testa distribuição de Habilidades, não
        // a regra do projeto de Feitiçaria.
        val resultado = EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre(
            catalogo = catalogo,
            selecionados = emptyList(),
            abilities = mapOf(EncounterGenerationRules.HABILIDADE_OCULTISMO to EncounterGenerationRules.MIN_OCULTISMO_FEITICARIA),
            essencia = EncounterGenerationRules.ESSENCIA_SOLAR,
            quantidadeTotal = EncounterGenerationRules.ENCANTOS_INICIAIS,
            exigirProjeto = true
        )

        assertTrue(
            resultado.any { it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE },
            "Solar Físico com Ocultismo >= 3 deve receber Feitiçaria Terrestre"
        )
        assertTrue(
            resultado.count {
                it.habilidade.equals(EncounterGenerationRules.HABILIDADE_OCULTISMO, ignoreCase = true) &&
                    it.nome != com.example.model.NOME_FEITICARIA_TERRESTRE
            } >= 4
        )
    }

    @Test
    fun `Solar atribui 1 ponto minimo somente por exigencia de Favorecida e evita ratings 1 desnecessarios`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSolaresCatalog(app).definitions
        repeat(EncounterTestSamples.count(100)) { seed ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val npc = EncounterGenerator.gerarSolar(
                    nomeManual = "",
                    arquetipo = arquetipo,
                    encantosSolares = catalogo,
                    random = kotlin.random.Random(10_000L + seed * 31L + arquetipo.ordinal)
                )
                val casta = com.example.model.Casta.entries.first { it.displayName == npc.casta }
                assertTrue(npc.habilidadesFavorecidas.all { (npc.abilities[it] ?: 0) >= 1 })
                assertTrue(
                    npc.abilities
                        .filterKeys { it !in npc.habilidadesFavorecidas }
                        .values
                        .none { it == 1 },
                    "Habilidades não favorecidas não devem ficar em 1 quando os 28 pontos permitem concentrar a distribuição"
                )
                val castaRelevantes = casta.allowedAbilities().filter { habilidade ->
                    catalogo.any { it.habilidade == habilidade }
                }
                assertTrue(
                    castaRelevantes.all { (npc.abilities[it] ?: 0) != 1 },
                    "Habilidade de Casta/Aspecto pode permanecer em 0, mas não deve receber 1 só por ser Casta"
                )
            }
        }
    }

}
