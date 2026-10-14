package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterHistoricalSnapshotTest {
    @Test fun `all frozen pre-refactor NPCs remain loadable and migration is idempotent`() {
        val root = File("../docs/audit/encounters-20261013/baseline")
        val hashes = JSONObject(root.resolve("snapshot-sha256.json").readText())
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalog = PreparedEncounterCatalog.prepare(EncantosSolaresCatalog(app).definitions,
            EncantosSangueDosDragoesCatalog(app).definitions, EncantosLunaresCatalog(app).definitions,
            FeiticariaCatalog(app).definitions, ArtesMarciaisCatalog(app).definitions)
        val quality = mutableListOf("scenario,named_chain_depth,blocked")
        var count = 0
        ZipFile(root.resolve("snapshots.zip")).use { archive ->
            archive.entries().asSequence().forEach { entry ->
                val bytes = archive.getInputStream(entry).use { it.readBytes() }
                val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
                assertEquals(entry.name, hashes.getString(entry.name), sha)
                val npc = NpcEncontroJsonCodec.decode(bytes.toString(Charsets.UTF_8))
                val migrated = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(npc))
                assertEquals(entry.name, npc, migrated)
                EncounterValidationService.validarIdentidadeEstrutural(migrated)
                val depths = mutableMapOf<String, Int>()
                val blocked = EncounterAcquisitionOrderOracle.blocked(npc, catalog, depths)
                quality += "${entry.name},${depths.values.maxOrNull() ?: 0},${blocked.size}"
                count++
            }
        }
        File("build/reports/encounter-audit/historical-quality.csv").apply { parentFile.mkdirs() }
            .writeText(quality.joinToString("\n") + "\n")
        assertEquals(540, count)
    }
}
