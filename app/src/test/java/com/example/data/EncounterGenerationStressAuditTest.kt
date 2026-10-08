package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * Auditoria estatística deliberadamente pesada.
 * O sufixo AuditTest mantém esta classe fora de testDebugUnitTest; ela deve
 * rodar apenas em jobs de auditoria dedicados.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterGenerationStressAuditTest {
    @Test
    fun `stress de geracao dos tres tipos e tres arquetipos nao lanca excecao`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        val merits = MeritosCatalog(app).definitions
        val arquetipos = ArquetipoEncontro.entries

        repeat(EncounterTestSamples.count(300)) { i ->
            val arquetipo = arquetipos[i % arquetipos.size]
            EncounterGenerator.gerarSolar("", arquetipo, solar, random = Random(i), meritosCatalogo = merits)
            EncounterGenerator.gerarSangueDeDragao("", arquetipo, dragon, random = Random(i + 10_000), meritosCatalogo = merits)
            EncounterGenerator.gerarLunar("", arquetipo, lunar, random = Random(i + 20_000), meritosCatalogo = merits)
        }
    }
}
