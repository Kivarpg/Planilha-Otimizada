package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterAuditorMutationTest {
    @Test fun `auditor rejects injected violations and accepts existing XP essence progression`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        val spells = FeiticariaCatalog(app).definitions
        val merits = MeritosCatalog(app).definitions
        val martial = ArtesMarciaisCatalog(app).definitions
        val catalog = PreparedEncounterCatalog.prepare(solar, dragon, lunar, spells, martial)
        val cases = listOf(
            EncounterGenerator.gerarSolar("Mutation", ArquetipoEncontro.FISICO, solar, spells,
                random = Random(947), meritosCatalogo = merits, estilosArtesMarciais = martial),
            EncounterGenerator.gerarSangueDeDragao("Mutation", ArquetipoEncontro.FISICO, dragon, spells,
                random = Random(947), meritosCatalogo = merits, estilosArtesMarciais = martial),
            EncounterGenerator.gerarLunar("Mutation", ArquetipoEncontro.FISICO, lunar, spells,
                random = Random(947), meritosCatalogo = merits, estilosArtesMarciais = martial))
        fun rejects(npc: NpcEncontro, code: String) {
            val errors = EncounterNpcAuditor.audit(npc, catalog).errors
            assertTrue("${npc.tipoExaltado}: expected $code, got $errors", errors.any { it.code == code })
        }
        for (base in cases) {
            assertTrue(EncounterNpcAuditor.audit(base, catalog).isValid)
            rejects(base.copy(xpAtual = -1), "STRUCTURAL_INVARIANT")
            rejects(base.copy(xpGastoTotal = -1), "STRUCTURAL_INVARIANT")
            rejects(base.copy(essencia = 5), "STRUCTURAL_INVARIANT")
            rejects(base.copy(charms = base.charms + EncantoEncontro("Missing catalog charm", "Briga", "")),
                "CHARM_NOT_IN_ALLOWED_CATALOG")
            val foreign = if (base.tipoExaltado == TipoExaltadoEncontro.LUNAR) {
                solar.first { def -> lunar.none { it.nome == def.nome } }.let { EncantoEncontro(it.nome, it.habilidade, it.custo) }
            } else {
                lunar.first { def -> solar.none { it.nome == def.nome } && dragon.none { it.nome == def.nome } }
                    .let { EncantoEncontro(it.nome, it.atributo, it.custo) }
            }
            rejects(base.copy(charms = base.charms + foreign), "CHARM_NOT_IN_ALLOWED_CATALOG")
            rejects(base.copy(abilities = base.abilities.mapValues { 0 }, attributes = base.attributes.mapValues { 1 }),
                "CHARM_REQUIREMENTS_UNSATISFIED")
            val spent = 500
            val essence = EncounterExperienceService.essenciaPara(base, spent)
            val advanced = base.copy(xpGastoTotal = spent, essencia = essence,
                motesPersonais = EncounterExperienceService.motesPersonaisPara(base, essence),
                motesPerifericos = EncounterExperienceService.motesPerifericosPara(base, essence))
            assertTrue("Advanced ${base.tipoExaltado}: ${EncounterNpcAuditor.audit(advanced, catalog).errors}",
                EncounterNpcAuditor.audit(advanced, catalog).isValid)
            // This fixture isolates the audit formula; the XP matrix tests actual purchases and undo.
            assertTrue(runCatching { EncounterValidationService.validarIdentidadeEstrutural(advanced) }.isFailure)
        }
    }
}
