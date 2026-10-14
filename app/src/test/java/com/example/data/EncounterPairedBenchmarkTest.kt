package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import java.io.File
import kotlin.random.Random
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Copied unchanged into both revisions by CI; contains no new production API. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterPairedBenchmarkTest {
    @Test fun pairedRealCatalogGenerationAndXp() {
        assumeTrue(System.getProperty("exalted.pairedBenchmark") == "true")
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        val spells = FeiticariaCatalog(app).definitions
        val merits = MeritosCatalog(app).definitions
        val martial = ArtesMarciaisCatalog(app).definitions
        val allocation: (() -> Long)? = runCatching {
            val bean = Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null)
            val extended = Class.forName("com.sun.management.ThreadMXBean")
            extended.getMethod("setThreadAllocatedMemoryEnabled", Boolean::class.javaPrimitiveType).invoke(bean, true)
            val method = extended.getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
            val read: () -> Long = { (method.invoke(bean, Thread.currentThread().id) as Number).toLong() }
            read
        }.getOrNull()
        fun generate(type: TipoExaltadoEncontro, archetype: ArquetipoEncontro, seed: Int, focus: String?) = when (type) {
            TipoExaltadoEncontro.SOLAR -> EncounterGenerator.gerarSolar("Benchmark", archetype, solar, spells,
                random = Random(seed), meritosCatalogo = merits, estilosArtesMarciais = martial, focoPersonalizado = focus)
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.gerarSangueDeDragao("Benchmark", archetype, dragon, spells,
                random = Random(seed), meritosCatalogo = merits, estilosArtesMarciais = martial, focoPersonalizado = focus)
            TipoExaltadoEncontro.LUNAR -> EncounterGenerator.gerarLunar("Benchmark", archetype, lunar, spells,
                random = Random(seed), meritosCatalogo = merits, estilosArtesMarciais = martial, focoPersonalizado = focus)
        }
        val output = File("build/reports/encounter-paired/samples.csv").apply { parentFile.mkdirs() }
        output.bufferedWriter().use { out ->
            out.appendLine("type,archetype,seed,mode,operation_ns,allocated_bytes,charms")
            for (type in TipoExaltadoEncontro.entries) for (archetype in ArquetipoEncontro.entries) {
                repeat(3) { generate(type, archetype, -1 - it, null) }
                for (seed in 0 until 10) for (mode in listOf("automatic", "focus", "xp")) {
                    val focus = if (mode != "focus") null else when (archetype) {
                        ArquetipoEncontro.FISICO -> if (type == TipoExaltadoEncontro.LUNAR) "Destreza" else "Briga"
                        ArquetipoEncontro.SOCIAL -> if (type == TipoExaltadoEncontro.LUNAR) "Carisma" else "Socialização"
                        ArquetipoEncontro.MENTAL -> if (type == TipoExaltadoEncontro.LUNAR) "Inteligência" else "Ocultismo"
                    }
                    val prepared = if (mode == "xp") generate(type, archetype, seed, null) else null
                    val before = allocation?.invoke() ?: -1L
                    val start = System.nanoTime()
                    val npc = if (prepared == null) generate(type, archetype, seed, focus) else when (type) {
                        TipoExaltadoEncontro.SOLAR -> EncounterGenerator.expandirEncantosPorExperiencia(prepared, solar)
                        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(prepared, dragon)
                        TipoExaltadoEncontro.LUNAR -> EncounterGenerator.expandirEncantosPorExperienciaLunar(prepared, lunar)
                    }
                    val elapsed = System.nanoTime() - start
                    val after = allocation?.invoke() ?: -1L
                    val bytes = if (before >= 0 && after >= before) after - before else -1L
                    out.appendLine("$type,$archetype,$seed,$mode,$elapsed,$bytes,${npc.charms.size}")
                    out.flush()
                }
            }
        }
    }
}
