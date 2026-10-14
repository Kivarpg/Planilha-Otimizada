package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import java.io.File
import java.util.zip.ZipFile
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Paired trajectories isolate initial selection; both use the same current XP rules. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterStrategicProgressionAuditTest {
    @Test fun `historical and new legal bases follow comparable 100 step trajectories`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        val spells = FeiticariaCatalog(app).definitions
        val merits = MeritosCatalog(app).definitions
        val martial = ArtesMarciaisCatalog(app).definitions
        val catalog = PreparedEncounterCatalog.prepare(solar, dragon, lunar, spells, martial)
        fun generate(type: TipoExaltadoEncontro, archetype: ArquetipoEncontro, seed: Int) = when (type) {
            TipoExaltadoEncontro.SOLAR -> EncounterGenerator.gerarSolar("Audit", archetype, solar, spells,
                random = Random(seed), meritosCatalogo = merits, estilosArtesMarciais = martial)
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.gerarSangueDeDragao("Audit", archetype, dragon, spells,
                random = Random(seed), meritosCatalogo = merits, estilosArtesMarciais = martial)
            TipoExaltadoEncontro.LUNAR -> EncounterGenerator.gerarLunar("Audit", archetype, lunar, spells,
                random = Random(seed), meritosCatalogo = merits, estilosArtesMarciais = martial)
        }
        fun expand(npc: NpcEncontro) = when (npc.tipoExaltado) {
            TipoExaltadoEncontro.SOLAR -> EncounterGenerator.expandirEncantosPorExperiencia(npc, solar)
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(npc, dragon)
            TipoExaltadoEncontro.LUNAR -> EncounterGenerator.expandirEncantosPorExperienciaLunar(npc, lunar)
        }
        val output = File("build/reports/encounter-audit/strategic-trajectories.csv").apply { parentFile.mkdirs() }
        output.bufferedWriter().use { csv ->
            csv.appendLine("type,archetype,seed,base,step,charms,named_chain_depth,xp_spent")
            ZipFile(File("../docs/audit/encounters-20261013/baseline/snapshots.zip")).use { archive ->
                for (type in TipoExaltadoEncontro.entries) for (archetype in ArquetipoEncontro.entries) {
                    val (seed, old) = (0..19).map { seed ->
                        val entry = archive.getEntry("$type-$archetype-automatic-$seed.json")
                        seed to NpcEncontroJsonCodec.decode(archive.getInputStream(entry).bufferedReader().use { it.readText() })
                    }.first { (_, npc) -> EncounterNpcAuditor.audit(npc, catalog).isValid &&
                        EncounterAcquisitionOrderOracle.blocked(npc, catalog).isEmpty() }
                    for ((label, base) in listOf("historical" to old, "new" to generate(type, archetype, seed))) {
                        var npc = base
                        for (step in 0..100) {
                            if (step > 0) npc = expand(npc)
                            if (step in setOf(0, 10, 50, 100)) {
                                val context = "$type $archetype $seed $label step=$step"
                                assertEquals(context, emptyList<EncounterValidationIssue>(), EncounterNpcAuditor.audit(npc, catalog).errors)
                                val depths = mutableMapOf<String, Int>()
                                assertEquals(context, emptySet<PreparedEncounterCatalog.StableContentId>(), EncounterAcquisitionOrderOracle.blocked(npc, catalog, depths))
                                assertEquals(context, step * 5, npc.xpAtual + npc.xpGastoTotal)
                                csv.appendLine("$type,$archetype,$seed,$label,$step,${npc.charms.size},${depths.values.maxOrNull() ?: 0},${npc.xpGastoTotal}")
                                csv.flush()
                            }
                        }
                    }
                }
            }
        }
    }
}
