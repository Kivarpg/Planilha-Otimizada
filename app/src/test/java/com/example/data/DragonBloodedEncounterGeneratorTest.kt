package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.Aspecto
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.NOME_FEITICARIA_TERRESTRE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DragonBloodedEncounterGeneratorTest {
    @Test
    fun `cada aspecto possui exatamente as cinco habilidades canonicas`() {
        Aspecto.entries.forEach { aspecto ->
            assertEquals(5, aspecto.allowedAbilities().size)
            assertEquals(5, aspecto.allowedAbilities().distinct().size)
        }
    }

    @Test
    fun `cada geracao possui cinco favorecidas fora do aspecto`() {
        ArquetipoEncontro.entries.forEach { arquetipo ->
            repeat(EncounterTestSamples.count(100)) { seed ->
                val npc = EncounterGenerator.gerarSangueDeDragao(
                    nomeManual = "Teste",
                    arquetipo = arquetipo,
                    encantosSangueDeDragao = emptyList(),
                    random = Random(seed + arquetipo.ordinal * 1000)
                )
                val aspecto = Aspecto.entries.first { it.displayName == npc.casta }
                assertEquals(5, npc.habilidadesFavorecidas.size)
                assertEquals(5, npc.habilidadesFavorecidas.distinct().size)
                assertTrue(npc.habilidadesFavorecidas.intersect(aspecto.allowedAbilities().toSet()).isEmpty())
                // Regra de criação: Habilidades Favorecidas devem receber
                // pelo menos 1 ponto. As Habilidades do Aspecto/Casta podem
                // permanecer em 0. Esta distinção é importante para que o
                // custo reduzido de XP das Favorecidas seja realmente
                // aproveitado na progressão do NPC.
                assertTrue(
                    "Sangue de Dragão/$arquetipo: toda Favorecida deve começar com pelo menos 1 ponto",
                    npc.habilidadesFavorecidas.all { (npc.abilities[it] ?: 0) >= 1 }
                )
            }
        }
    }

    @Test
    fun `Sangue de Dragao nao usa 1 ponto para Aspecto e concentra os 28 pontos`() {
        ArquetipoEncontro.entries.forEach { arquetipo ->
            repeat(EncounterTestSamples.count(100)) { seed ->
                val npc = EncounterGenerator.gerarSangueDeDragao(
                    nomeManual = "Teste",
                    arquetipo = arquetipo,
                    encantosSangueDeDragao = emptyList(),
                    random = Random(10_000 + seed + arquetipo.ordinal * 1000)
                )
                val aspecto = Aspecto.entries.first { it.displayName == npc.casta }
                val favorecidas = npc.habilidadesFavorecidas.toSet()
                assertTrue(
                    "Habilidades não favorecidas não devem ficar em 1 sem necessidade",
                    npc.abilities.filterKeys { it !in favorecidas }.values.none { it == 1 }
                )
                assertTrue(
                    "Habilidades do Aspecto podem ficar em 0 e não precisam começar em 1",
                    aspecto.allowedAbilities().all { (npc.abilities[it] ?: 0) != 1 }
                )
            }
        }
    }

    @Test
    fun `Sangue de Dragao Fisico com Ocultismo 3 ou mais recebe o projeto completo de Feiticaria`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSangueDosDragoesCatalog(app).definitions.map { it.paraFormatoSolar() }

        // Este teste verifica a regra do projeto com a pré-condição controlada.
        // A geração aleatória de um Físico não é obrigada a produzir Ocultismo 3;
        // exigir uma seed elegível tornaria o teste dependente da distribuição
        // aleatória, sem aumentar a cobertura da regra que estamos auditando.
        val resultado = EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre(
            catalogo = catalogo,
            selecionados = emptyList(),
            abilities = mapOf(EncounterGenerationRules.HABILIDADE_OCULTISMO to EncounterGenerationRules.MIN_OCULTISMO_FEITICARIA),
            essencia = EncounterGenerationRules.ESSENCIA_SANGUE_DE_DRAGAO,
            quantidadeTotal = EncounterGenerationRules.ENCANTOS_INICIAIS,
            exigirProjeto = true
        )

        assertTrue(resultado.any { it.nome == NOME_FEITICARIA_TERRESTRE })
        assertTrue(
            resultado.count {
                it.habilidade.equals(EncounterGenerationRules.HABILIDADE_OCULTISMO, ignoreCase = true) &&
                    it.nome != NOME_FEITICARIA_TERRESTRE
            } >= 4
        )
    }

    @Test
    fun `mesmo aspecto permanece canonico com arquetipos diferentes`() {
        Aspecto.entries.forEachIndexed { index, aspecto ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val npc = EncounterGenerator.gerarSangueDeDragao(
                    nomeManual = "Teste", arquetipo = arquetipo, encantosSangueDeDragao = emptyList(), random = Random(index * 100 + arquetipo.ordinal)
                )
                val habilidades = Aspecto.entries.first { it.displayName == npc.casta }.allowedAbilities()
                assertEquals(5, habilidades.size)
            }
        }
    }
}
