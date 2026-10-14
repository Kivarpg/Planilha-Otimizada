package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterMandatoryResistanceTest {
    @Test fun `mandatory Ox Body has legal resistance on previously failing real seeds`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val spells = FeiticariaCatalog(app).definitions
        val merits = MeritosCatalog(app).definitions
        val martial = ArtesMarciaisCatalog(app).definitions
        val cases = listOf(
            EncounterGenerator.gerarSolar("Audit", ArquetipoEncontro.SOCIAL, solar, spells,
                random = Random(13), meritosCatalogo = merits, estilosArtesMarciais = martial),
            EncounterGenerator.gerarSangueDeDragao("Audit", ArquetipoEncontro.MENTAL, dragon, spells,
                random = Random(12), meritosCatalogo = merits, focoPersonalizado = "Ocultismo", estilosArtesMarciais = martial))
        val catalog = PreparedEncounterCatalog.prepare(solares = solar, sangueDeDragao = dragon, feiticos = spells, estilosMarciais = martial)
        for (npc in cases) {
            assertTrue(npc.corpoDeTouroCount > 0)
            assertTrue(npc.corpoDeTouroCount <= npc.abilities.getValue("Resistência"))
            assertEquals(emptyList<EncounterValidationIssue>(), EncounterNpcAuditor.audit(npc, catalog).errors)
        }
    }

    @Test fun `future preference preserves the established Solar defensive chain`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val npc = EncounterGenerator.gerarSolar("Audit", ArquetipoEncontro.FISICO,
            EncantosSolaresCatalog(app).definitions, FeiticariaCatalog(app).definitions,
            random = Random(16), meritosCatalogo = MeritosCatalog(app).definitions,
            estilosArtesMarciais = ArtesMarciaisCatalog(app).definitions)
        assertTrue(npc.charms.any { it.nome == "Concentração de Pele de Ferro" })
        assertTrue(npc.charms.any { it.nome == "Espírito Fortalece a Pele" })
        assertTrue(npc.charms.any { it.nome == "Meditação da Durabilidade do Carvalho" })
    }

    @Test fun `functional resistance and literacy use normal budget rather than free dots`() {
        for (archetype in ArquetipoEncontro.entries) for (seed in 0..99) {
            val distribution = EncounterDistributionService.distribuirHabilidades(archetype,
                "Arremesso", "Esquiva", "Performance",
                listOf("Performance", "Sobrevivência", "Conhecimento", "Integridade", "Presença"),
                Random(seed), habilidadesMinimoUm = setOf("Resistência", "Linguística"))
            assertEquals(28, distribution.abilities.values.sum())
            assertTrue(distribution.abilities.getValue("Resistência") >= 1)
            assertTrue(distribution.abilities.getValue("Linguística") >= 1)
            assertTrue(distribution.abilities.values.all { it in 0..3 })
        }
    }
}
