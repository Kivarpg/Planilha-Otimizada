package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Observes the unchanged generator. Audit findings are recorded, never silently corrected. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterAuditBaselineTest {
    @Test fun `record reproducible real catalog matrix before optimization`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        val spells = FeiticariaCatalog(app).definitions
        val merits = MeritosCatalog(app).definitions
        val martial = ArtesMarciaisCatalog(app).definitions
        val catalog = PreparedEncounterCatalog.prepare(solar, dragon, lunar, spells, martial)
        val out = File("build/reports/encounter-audit").apply { mkdirs() }
        val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        fun snapshot(npc: NpcEncontro) = uuid.replace(NpcEncontroJsonCodec.encode(npc), "<UUID>")
        fun generate(type: TipoExaltadoEncontro, archetype: ArquetipoEncontro, seed: Int, focus: String?): NpcEncontro = when(type) {
            TipoExaltadoEncontro.SOLAR -> EncounterGenerator.gerarSolar("Audit", archetype, solar, spells, random = Random(seed), meritosCatalogo = merits, focoPersonalizado = focus, estilosArtesMarciais = martial)
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.gerarSangueDeDragao("Audit", archetype, dragon, spells, random = Random(seed), meritosCatalogo = merits, focoPersonalizado = focus, estilosArtesMarciais = martial)
            TipoExaltadoEncontro.LUNAR -> EncounterGenerator.gerarLunar("Audit", archetype, lunar, spells, random = Random(seed), meritosCatalogo = merits, focoPersonalizado = focus, estilosArtesMarciais = martial)
        }
        fun xp(npc: NpcEncontro) = when(npc.tipoExaltado) {
            TipoExaltadoEncontro.SOLAR -> EncounterGenerator.expandirEncantosPorExperiencia(npc, solar)
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(npc, dragon)
            TipoExaltadoEncontro.LUNAR -> EncounterGenerator.expandirEncantosPorExperienciaLunar(npc, lunar)
        }
        out.resolve("catalog-sha256.txt").writeText(
            listOf("encantos_solares.json", "encantos_sangue_dragoes.json", "encantos_lunares.json", "feiticaria.json", "estilos_artes_marciais.json", "meritos.json").joinToString("\n") { name ->
                val bytes = app.assets.open(name).use { it.readBytes() }
                name + " " + MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
            })
        var scenarios = 0
        var errors = 0
        var nondeterministic = 0
        var blockedAcquisitions = 0
        var generationFailures = 0
        out.resolve("samples.csv").bufferedWriter().use { csv ->
            csv.appendLine("type,archetype,mode,seed,operation_ns,audit_errors,charms,trees,largest_tree,xp_available,xp_spent,deterministic,named_chain_depth,allocated_bytes")
            for (type in TipoExaltadoEncontro.entries) for (archetype in ArquetipoEncontro.entries) {
                runCatching { generate(type, archetype, -1, null) } // unmeasured warmup
                for (seed in 0 until 20) for (mode in listOf("automatic", "focus", "xp")) {
                    val focus = if (mode != "focus") null else when (archetype) {
                        ArquetipoEncontro.FISICO -> if (type == TipoExaltadoEncontro.LUNAR) "Destreza" else "Briga"
                        ArquetipoEncontro.SOCIAL -> if (type == TipoExaltadoEncontro.LUNAR) "Carisma" else "Socialização"
                        ArquetipoEncontro.MENTAL -> if (type == TipoExaltadoEncontro.LUNAR) "Inteligência" else "Ocultismo"
                    }
                    val id = "${type.name}-${archetype.name}-$mode-$seed"
                    scenarios++
                    try {
                    val allocatedBefore = EncounterAuditAllocationCounter.bytes()
                    val start = System.nanoTime()
                    val generated = generate(type, archetype, seed, focus)
                    val generationNs = System.nanoTime() - start
                    val allocatedAfterGeneration = EncounterAuditAllocationCounter.bytes()
                    val xpStart = System.nanoTime()
                    val npc = if (mode == "xp") xp(generated) else generated
                    val elapsed = if (mode == "xp") System.nanoTime() - xpStart else generationNs
                    val allocated = EncounterAuditAllocationCounter.delta(
                        if (mode == "xp") allocatedAfterGeneration else allocatedBefore,
                        if (mode == "xp") EncounterAuditAllocationCounter.bytes() else allocatedAfterGeneration)
                    val report = EncounterNpcAuditor.audit(npc, catalog)
                    val depths = mutableMapOf<String, Int>()
                    val blocked = EncounterAcquisitionOrderOracle.blocked(npc, catalog, depths)
                    blockedAcquisitions += blocked.size
                    if (blocked.isNotEmpty()) out.resolve("$id.acquisition-blocked.txt").writeText(blocked.joinToString("\n"))
                    val groups = npc.charms.groupingBy { it.habilidadeVinculada }.eachCount()
                    val deterministic = if (mode == "xp") snapshot(npc) == snapshot(xp(generated)) else snapshot(npc) == snapshot(generate(type, archetype, seed, focus))
                    if (!deterministic) nondeterministic++
                    errors += report.errors.size
                    out.resolve("$id.json").writeText(snapshot(npc))
                    if (report.issues.isNotEmpty()) out.resolve("$id.issues.txt").writeText(report.issues.joinToString("\n"))
                    val decoded = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(npc))
                    if (npc.alertasValidacao != decoded.alertasValidacao) {
                        out.resolve("$id.alert-migration.txt").writeText("before=${npc.alertasValidacao}\nafter=${decoded.alertasValidacao}\n")
                    }
                    assertEquals("Mechanical JSON roundtrip $id", npc.copy(alertasValidacao = decoded.alertasValidacao), decoded)
                    csv.appendLine(listOf(type, archetype, mode, seed, elapsed, report.errors.size, npc.charms.size, groups.size, groups.values.maxOrNull() ?: 0, npc.xpAtual, npc.xpGastoTotal, deterministic, depths.values.maxOrNull() ?: 0, allocated).joinToString(","))
                    csv.flush()
                    } catch (failure: IllegalStateException) {
                        generationFailures++
                        out.resolve("$id.failure.txt").writeText(failure.stackTraceToString())
                        csv.appendLine(listOf(type, archetype, mode, seed, "", "GENERATION_FAILED", "", "", "", "", "", "").joinToString(","))
                        csv.flush()
                    }
                }
            }
        }
        out.resolve("summary.txt").writeText("scenarios=$scenarios\ngeneration_failures=$generationFailures\naudit_errors=$errors\nnondeterministic=$nondeterministic\nThese observations are not approved mechanical golden snapshots.\n")
        assertEquals(540, scenarios)
        assertEquals("Generation exceptions; inspect failure artifacts", 0, generationFailures)
        assertEquals("Illegal generated NPCs; inspect issue artifacts", 0, errors)
        assertEquals("Seed regression", 0, nondeterministic)
        assertEquals("No legal acquisition order; inspect artifacts", 0, blockedAcquisitions)
    }
}
