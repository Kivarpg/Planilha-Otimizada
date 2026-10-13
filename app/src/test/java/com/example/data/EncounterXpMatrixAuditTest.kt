package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import java.io.File
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterXpMatrixAuditTest {
    @Test fun `real catalog XP matrix preserves budget persistence and complete undo`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        val spells = FeiticariaCatalog(app).definitions
        val merits = MeritosCatalog(app).definitions
        val martial = ArtesMarciaisCatalog(app).definitions
        val out = File("build/reports/encounter-audit/xp-matrix.csv").apply { parentFile.mkdirs() }
        out.bufferedWriter().use { csv ->
            csv.appendLine("type,archetype,steps,cumulative_ns,charms,xp_available,xp_spent")
            for (type in TipoExaltadoEncontro.entries) for (archetype in ArquetipoEncontro.entries) {
                val base = when (type) {
                    TipoExaltadoEncontro.SOLAR -> EncounterGenerator.gerarSolar("XP Audit", archetype, solar, spells, random = Random(947), meritosCatalogo = merits, estilosArtesMarciais = martial)
                    TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.gerarSangueDeDragao("XP Audit", archetype, dragon, spells, random = Random(947), meritosCatalogo = merits, estilosArtesMarciais = martial)
                    TipoExaltadoEncontro.LUNAR -> EncounterGenerator.gerarLunar("XP Audit", archetype, lunar, spells, random = Random(947), meritosCatalogo = merits, estilosArtesMarciais = martial)
                }
                var current = base
                var nanos = 0L
                for (step in 1..100) {
                    val start = System.nanoTime()
                    current = when (type) {
                        TipoExaltadoEncontro.SOLAR -> EncounterGenerator.expandirEncantosPorExperiencia(current, solar)
                        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(current, dragon)
                        TipoExaltadoEncontro.LUNAR -> EncounterGenerator.expandirEncantosPorExperienciaLunar(current, lunar)
                    }
                    nanos += System.nanoTime() - start
                    val context = "$type $archetype step=$step"
                    assertEquals(context, step * 5, current.xpAtual + current.xpGastoTotal)
                    assertTrue(context, current.xpAtual >= 0 && current.xpGastoTotal >= 0)
                    assertEquals(context, base.feiticoInicialNome, current.feiticoInicialNome)
                    if (step in setOf(1, 10, 50, 100)) {
                        val decoded = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(current))
                        assertEquals(context, current.copy(alertasValidacao = decoded.alertasValidacao), decoded)
                        csv.appendLine("$type,$archetype,$step,$nanos,${current.charms.size},${current.xpAtual},${current.xpGastoTotal}")
                        csv.flush()
                    }
                }
                repeat(100) { current = EncounterGenerator.reduzirExperiencia(current) }
                val context = "$type $archetype undo"
                assertEquals(context, base.attributes, current.attributes)
                assertEquals(context, base.abilities, current.abilities)
                assertEquals(context, base.charms, current.charms)
                assertEquals(context, base.especialidades, current.especialidades)
                assertEquals(context, base.essencia, current.essencia)
                assertEquals(context, base.healthBoxes.map { it.penalidade }, current.healthBoxes.map { it.penalidade })
                assertEquals(context, base.feiticos, current.feiticos)
                assertEquals(context, base.arma, current.arma)
                assertEquals(context, base.armadura, current.armadura)
                assertEquals(context, 0, current.xpAtual)
                assertEquals(context, 0, current.xpGastoTotal)
                assertTrue(context, current.historicoXpBatches.isEmpty())
            }
        }
    }
}
