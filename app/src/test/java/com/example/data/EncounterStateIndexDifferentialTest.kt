package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.TipoExaltadoEncontro
import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterStateIndexDifferentialTest {
    @Test fun `indexed states equal type restricted frozen implementation for normalized names`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalog = PreparedEncounterCatalog.prepare(
            EncantosSolaresCatalog(app).definitions, EncantosSangueDosDragoesCatalog(app).definitions,
            EncantosLunaresCatalog(app).definitions, FeiticariaCatalog(app).definitions,
            ArtesMarciaisCatalog(app).definitions)
        // Legacy input must be restricted to the NPC type: its old implementation
        // incorrectly counted same-named charms from other Exalted catalogs.
        val legacyCatalog = PreparedEncounterCatalog.prepare(solares = catalog.solares,
            feiticos = catalog.feiticos, estilosMarciais = catalog.estilosMarciais)
        val names = catalog.byId.values.map { it.nome }.distinct()
        val samples = (0 until 120).map { seed ->
            names.shuffled(Random(seed)).take(seed % 60).mapIndexed { i, name ->
                if (i % 3 == 0) "  ${name.uppercase()}  " else name
            }.toSet() + "Unknown legacy charm"
        }
        fun state(selected: Set<String>, legacy: Boolean) = if (legacy)
            EncounterLegacyStateReference.legacyBuildState(TipoExaltadoEncontro.SOLAR, 3,
                acquiredCharmNames = selected, catalog = legacyCatalog)
        else EncounterRulesEngine.buildState(TipoExaltadoEncontro.SOLAR, 3,
                acquiredCharmNames = selected, catalog = catalog)
        for (selected in samples) assertEquals(state(selected, true), state(selected, false))
        // Same process/catalog/samples, alternate order to limit warmup and scheduling bias.
        repeat(2) { for (selected in samples) { state(selected, true); state(selected, false) } }
        val report = File("build/reports/encounter-audit/state-index.csv").apply { parentFile.mkdirs() }
        report.bufferedWriter().use { out ->
            out.appendLine("round,legacy_ns,indexed_ns,states")
            repeat(10) { round ->
                fun timed(legacy: Boolean): Long {
                    val start = System.nanoTime()
                    for (selected in samples) state(selected, legacy)
                    return System.nanoTime() - start
                }
                val first = timed(round % 2 == 0)
                val second = timed(round % 2 != 0)
                out.appendLine("$round,${if (round % 2 == 0) first else second},${if (round % 2 == 0) second else first},${samples.size}")
            }
        }
        // Cold per-NPC boundary: include preparation and repeated audit states.
        val coldReport = File("build/reports/encounter-audit/state-index-cold.csv")
        coldReport.bufferedWriter().use { out ->
            out.appendLine("round,legacy_ns,indexed_ns,states")
            repeat(10) { round ->
                fun timed(legacy: Boolean): Long {
                    val start = System.nanoTime()
                    val fresh = if (legacy) PreparedEncounterCatalog.prepare(solares = catalog.solares,
                        feiticos = catalog.feiticos, estilosMarciais = catalog.estilosMarciais)
                    else PreparedEncounterCatalog.prepare(catalog.solares, catalog.sangueDeDragao,
                        catalog.lunares, catalog.feiticos, catalog.estilosMarciais)
                    for (selected in samples.take(15)) {
                        if (legacy) EncounterLegacyStateReference.legacyBuildState(TipoExaltadoEncontro.SOLAR, 3,
                            acquiredCharmNames = selected, catalog = fresh)
                        else EncounterRulesEngine.buildState(TipoExaltadoEncontro.SOLAR, 3,
                            acquiredCharmNames = selected, catalog = fresh)
                    }
                    return System.nanoTime() - start
                }
                val first = timed(round % 2 == 0)
                val second = timed(round % 2 != 0)
                out.appendLine("$round,${if (round % 2 == 0) first else second},${if (round % 2 == 0) second else first},15")
            }
        }

    }
}
