package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterArchetypeMatrixTest {
    @Test
    fun `matriz 3 por 3 gera NPCs estruturais validos`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        ArquetipoEncontro.entries.forEachIndexed { a, arquetipo ->
            val solarNpc = EncounterGenerator.gerarSolar("", arquetipo, solar, random = Random(100 + a))
            assertTrue(solarNpc.motesPersonais >= 0 && solarNpc.motesPerifericos >= 0)
            assertTrue(solarNpc.especialidades.all { (solarNpc.abilities[it.habilidade] ?: 0) >= 2 })
            assertTrue(solarNpc.abilities.filterKeys { it !in solarNpc.habilidadesFavorecidas }.values.none { it == 1 })

            val dragonNpc = EncounterGenerator.gerarSangueDeDragao("", arquetipo, dragon, random = Random(200 + a))
            assertTrue(dragonNpc.habilidadesFavorecidas.size == 5)
            assertTrue(dragonNpc.especialidades.all { (dragonNpc.abilities[it.habilidade] ?: 0) >= 2 })
            assertTrue(dragonNpc.abilities.filterKeys { it !in dragonNpc.habilidadesFavorecidas }.values.none { it == 1 })

            val lunarNpc = EncounterGenerator.gerarLunar("", arquetipo, lunar, random = Random(300 + a))
            assertTrue(lunarNpc.lunarAtributosCasta.size == 2)
            assertTrue(lunarNpc.habilidadesFavorecidas.size == 2)
            assertTrue(lunarNpc.lunarAtributosCasta.intersect(lunarNpc.habilidadesFavorecidas.toSet()).isEmpty())
            assertTrue((lunarNpc.lunarAtributosCasta + lunarNpc.habilidadesFavorecidas).distinct().size == 4)
            assertTrue(lunarNpc.especialidades.all { (lunarNpc.abilities[it.habilidade] ?: 0) >= 2 })
            assertTrue(lunarNpc.abilities.values.none { it == 1 })
        }
    }
}
