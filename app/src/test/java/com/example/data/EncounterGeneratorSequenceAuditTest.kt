package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterGeneratorSequenceAuditTest {
    private lateinit var catalogo: List<EncantoSolarDefinition>
    private lateinit var catalogoMeritos: List<MeritoDefinition>

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        catalogo = EncantosSolaresCatalog(app).definitions
        catalogoMeritos = MeritosCatalog(app).definitions
    }

    @Test
    fun `cem geracoes com sementes diferentes nao lancam excecao, em todos os arquetipos`() {
        val arquetipos = ArquetipoEncontro.entries
        val falhas = mutableListOf<String>()
        repeat(EncounterTestSamples.count(100)) { i ->
            val arquetipo = arquetipos[i % arquetipos.size]
            val seed = i.toLong()
            val npc = EncounterGenerator.gerarSolar(
                nomeManual = "",
                arquetipo = arquetipo, encantosSolares = catalogo, meritosCatalogo = catalogoMeritos, random = Random(seed)
            )
            if (npc.essencia != 1 || npc.charms.size < 15) {
                falhas += "seed=$seed, arquetipo=$arquetipo, tipo=${npc.tipoExaltado}, " +
                    "essencia=${npc.essencia}, charms=${npc.charms.size}, " +
                    "encantos=${npc.charms.map { it.nome }}, " +
                    "habilidades=${npc.charms.map { it.habilidadeVinculada }}"
            }
        }
        assertTrue(
            "DIAGNOSTICO_SEQUENCE: ${falhas.size} falha(s)\n" + falhas.joinToString("\n"),
            falhas.isEmpty()
        )
    }
}
