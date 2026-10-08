package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

private object EncounterGoldenFixture {
    private val app: Application by lazy { ApplicationProvider.getApplicationContext<Application>() }
    val solar by lazy { EncantosSolaresCatalog(app).definitions }
    val dragon by lazy { EncantosSangueDosDragoesCatalog(app).definitions }
    val lunar by lazy { EncantosLunaresCatalog(app).definitions }
    val spells by lazy { FeiticariaCatalog(app).definitions }
    val merits by lazy { MeritosCatalog(app).definitions }
    val martial by lazy { ArtesMarciaisCatalog(app).definitions }
}

/** Golden mecânico: seeds canônicas + projeção estável da ficha completa. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterMechanicalGoldenSnapshotTest {
    private fun generate(case: EncounterEngineeringBaseline.Case): NpcEncontro {
        val f = EncounterGoldenFixture
        val random = Random(case.seed)
        return when (case.exaltedType) {
            TipoExaltadoEncontro.SOLAR -> EncounterGenerator.gerarSolar(
                "Golden ${case.id}", case.archetype, f.solar, f.spells,
                random = random, meritosCatalogo = f.merits, focoPersonalizado = case.focus,
                estilosArtesMarciais = f.martial
            )
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> EncounterGenerator.gerarSangueDeDragao(
                "Golden ${case.id}", case.archetype, f.dragon, f.spells,
                random = random, meritosCatalogo = f.merits, focoPersonalizado = case.focus,
                estilosArtesMarciais = f.martial
            )
            TipoExaltadoEncontro.LUNAR -> EncounterGenerator.gerarLunar(
                "Golden ${case.id}", case.archetype, f.lunar, f.spells,
                random = random, meritosCatalogo = f.merits, focoPersonalizado = case.focus,
                estilosArtesMarciais = f.martial
            )
        }
    }

    @Test fun `seeds canonicas produzem golden mecanico deterministico`() {
        EncounterEngineeringBaseline.cases.forEach { case ->
            val a = generate(case)
            val b = generate(case)
            assertEquals(case.id, EncounterMechanicalGoldenSnapshot.fingerprint(a), EncounterMechanicalGoldenSnapshot.fingerprint(b))
            assertEquals(case.id, case.exaltedType, a.tipoExaltado)
            assertEquals(case.id, case.archetype, a.arquetipo)
        }
    }

    @Test fun `golden aceito entre versoes e aplicado quando baseline existe`() {
        EncounterEngineeringBaseline.cases.forEach { case ->
            val expected = EncounterEngineeringBaseline.expectedMechanicalFingerprint(case.id)
            if (expected != null) {
                assertEquals(case.id, expected, EncounterMechanicalGoldenSnapshot.fingerprint(generate(case)))
            }
        }
    }

    @Test fun `golden ignora identidade efemera e ordem incidental`() {
        val a = NpcEncontro(id = "uuid-a", attributes = linkedMapOf("Força" to 3, "Destreza" to 4))
        val b = a.copy(id = "uuid-b", attributes = linkedMapOf("Destreza" to 4, "Força" to 3), alertasValidacao = listOf("diagnostico efemero"))
        assertEquals(EncounterMechanicalGoldenSnapshot.fingerprint(a), EncounterMechanicalGoldenSnapshot.fingerprint(b))
        assertFalse(EncounterMechanicalGoldenSnapshot.parts(a).any { it.contains("uuid-a") })
        assertTrue(EncounterMechanicalGoldenSnapshot.parts(a).any { it == "attr:Força=3" })
    }
}
