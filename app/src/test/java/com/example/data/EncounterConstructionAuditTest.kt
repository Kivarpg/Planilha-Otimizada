package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import com.example.model.Aspecto
import com.example.model.Casta
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import com.example.model.ExaltedConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

private object EncounterConstructionAuditFixture {
    private val app: Application by lazy { ApplicationProvider.getApplicationContext<Application>() }
    val solar by lazy { EncantosSolaresCatalog(app).definitions }
    val dragon by lazy { EncantosSangueDosDragoesCatalog(app).definitions }
    val lunar by lazy { EncantosLunaresCatalog(app).definitions }
    val merits by lazy { MeritosCatalog(app).definitions }
    val lunarMentalCharmNames by lazy {
        lunar.asSequence()
            .filter { it.atributo in ExaltedConstants.MENTAL_ATTRIBUTES }
            .map { it.nome }
            .toHashSet()
    }
    val solarAbilities by lazy { solar.asSequence().map { it.habilidade }.toHashSet() }
    val dragonAbilities by lazy { dragon.asSequence().map { it.habilidade }.toHashSet() }
}

/** Matriz de sementes para detectar regressões de construção entre tipos e arquétipos. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterConstructionAuditTest {
    @Test
    fun `18000 construcoes cobrem identidade coerencia e determinismo nos nove cenarios`() {
        val fixture = EncounterConstructionAuditFixture

        repeat(EncounterTestSamples.count(1_000)) { seed ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val solarSeed = seed * 10_000L + arquetipo.ordinal
                val dragonSeed = seed * 20_000L + arquetipo.ordinal
                val lunarSeed = seed * 30_000L + arquetipo.ordinal

                val solarA = EncounterGenerator.gerarSolar(
                    "", arquetipo, fixture.solar, random = Random(solarSeed),
                    meritosCatalogo = fixture.merits
                )
                val solarB = EncounterGenerator.gerarSolar(
                    "", arquetipo, fixture.solar, random = Random(solarSeed),
                    meritosCatalogo = fixture.merits
                )
                auditDeterminism(solarA, solarB)
                auditIdentityAndCoherence(solarA, fixture.lunarMentalCharmNames)

                val dragonA = EncounterGenerator.gerarSangueDeDragao(
                    "", arquetipo, fixture.dragon, random = Random(dragonSeed),
                    meritosCatalogo = fixture.merits
                )
                val dragonB = EncounterGenerator.gerarSangueDeDragao(
                    "", arquetipo, fixture.dragon, random = Random(dragonSeed),
                    meritosCatalogo = fixture.merits
                )
                auditDeterminism(dragonA, dragonB)
                auditIdentityAndCoherence(dragonA, fixture.lunarMentalCharmNames)

                val lunarA = EncounterGenerator.gerarLunar(
                    "", arquetipo, fixture.lunar, random = Random(lunarSeed),
                    meritosCatalogo = fixture.merits
                )
                val lunarB = EncounterGenerator.gerarLunar(
                    "", arquetipo, fixture.lunar, random = Random(lunarSeed),
                    meritosCatalogo = fixture.merits
                )
                auditDeterminism(lunarA, lunarB)
                auditIdentityAndCoherence(lunarA, fixture.lunarMentalCharmNames)
            }
        }
    }

    @Test
    fun `regressao final preserva orcamentos de meritos e quatro especialidades nos nove cenarios`() {
        val fixture = EncounterConstructionAuditFixture
        val solar = fixture.solar
        val dragon = fixture.dragon
        val lunar = fixture.lunar
        val merits = fixture.merits

        repeat(30) { seed ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val npcs = listOf(
                    EncounterGenerator.gerarSolar(
                        "", arquetipo, solar,
                        random = Random(100_000L + seed * 100L + arquetipo.ordinal),
                        meritosCatalogo = merits
                    ),
                    EncounterGenerator.gerarSangueDeDragao(
                        "", arquetipo, dragon,
                        random = Random(200_000L + seed * 100L + arquetipo.ordinal),
                        meritosCatalogo = merits
                    ),
                    EncounterGenerator.gerarLunar(
                        "", arquetipo, lunar,
                        random = Random(300_000L + seed * 100L + arquetipo.ordinal),
                        meritosCatalogo = merits
                    )
                )

                val falhasCenario = mutableListOf<String>()
                npcs.forEach { npc ->
                    // Sangue de Dragão possui 15 Encantos normais + 5 Excelências
                    // gratuitas; Solar e Lunar permanecem com 15 Encantos iniciais.
                    val quantidadeEsperada = if (npc.tipoExaltado == TipoExaltadoEncontro.SANGUE_DE_DRAGAO) 20 else 15
                    if (npc.charms.size != quantidadeEsperada) {
                        falhasCenario += "Falha de quantidade: seed=$seed, arquetipo=$arquetipo, " +
                            "tipo=${npc.tipoExaltado}, esperado=$quantidadeEsperada, obtido=${npc.charms.size}, " +
                            "encantos=${npc.charms.map { it.nome }}, " +
                            "habilidades=${npc.charms.map { it.habilidadeVinculada }}, " +
                            "especialidades=${npc.especialidades.map { it.habilidade }}"
                    }
                    if (npc.especialidades.distinctBy { it.habilidade }.size != 4) {
                        falhasCenario += "Falha de especialidades: seed=$seed, arquetipo=$arquetipo, " +
                            "tipo=${npc.tipoExaltado}, distintas=${npc.especialidades.distinctBy { it.habilidade }.size}, " +
                            "especialidades=${npc.especialidades.map { it.habilidade }}, " +
                            "encantos=${npc.charms.map { it.nome }}"
                    }

                    val totalMeritos = npc.merits.sumOf { it.valor }
                    val totalEsperado = if (npc.tipoExaltado == TipoExaltadoEncontro.SANGUE_DE_DRAGAO) 18 else 10
                    assertEquals(totalEsperado, totalMeritos)

                    if (npc.tipoExaltado == TipoExaltadoEncontro.SANGUE_DE_DRAGAO) {
                        assertTrue(npc.merits.none { it.categoria == "sobrenatural" })
                    }
                }
                if (falhasCenario.isNotEmpty()) {
                    System.err.println(
                        "DIAGNOSTICO_CONSTRUCTION: ${falhasCenario.size} falha(s)\n" +
                            falhasCenario.joinToString("\n")
                    )
                }
                assertTrue(
                    "DIAGNOSTICO_CONSTRUCTION: ${falhasCenario.size} falha(s)\n" +
                        falhasCenario.joinToString("\n"),
                    falhasCenario.isEmpty()
                )
            }
        }
    }

    @Test
    fun `regressao final prioriza especialidade estrutural nos nove cenarios`() {
        val fixture = EncounterConstructionAuditFixture
        val solar = fixture.solar
        val dragon = fixture.dragon
        val lunar = fixture.lunar
        val solarCatalogoPorHabilidade = fixture.solarAbilities
        val dragonCatalogoPorHabilidade = fixture.dragonAbilities

        repeat(30) { seed ->
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val npcs = listOf(
                    EncounterGenerator.gerarSolar(
                        "", arquetipo, solar,
                        random = Random(400_000L + seed * 100L + arquetipo.ordinal)
                    ),
                    EncounterGenerator.gerarSangueDeDragao(
                        "", arquetipo, dragon,
                        random = Random(500_000L + seed * 100L + arquetipo.ordinal)
                    ),
                    EncounterGenerator.gerarLunar(
                        "", arquetipo, lunar,
                        random = Random(600_000L + seed * 100L + arquetipo.ordinal)
                    )
                )

                npcs.forEach { npc ->
                    val estruturais = when (npc.tipoExaltado) {
                        TipoExaltadoEncontro.SOLAR -> {
                            val casta = Casta.entries.first { it.displayName == npc.casta }
                            casta.allowedAbilities().filter { it in solarCatalogoPorHabilidade }
                        }
                        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> {
                            val aspecto = Aspecto.entries.first { it.displayName == npc.casta }
                            aspecto.allowedAbilities().filter { it in dragonCatalogoPorHabilidade }
                        }
                        TipoExaltadoEncontro.LUNAR ->
                            EncounterGenerationRules.LUNAR_ABILITY_PROFILES.getValue(arquetipo)
                    }.filter { (npc.abilities[it] ?: 0) > 0 }

                    if (estruturais.isNotEmpty()) {
                        assertTrue(
                            "${npc.tipoExaltado}/$arquetipo deve priorizar ao menos uma Especialidade estrutural",
                            npc.especialidades.any { it.habilidade in estruturais }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `politica separa regras obrigatorias de preferencias`() {
        TipoExaltadoEncontro.entries.forEach { tipo ->
            assertTrue(EncounterConstructionPolicy.identityRules(tipo).any {
                it.kind == EncounterConstructionPolicy.Kind.MANDATORY
            })
        }
        ArquetipoEncontro.entries.forEach { arquetipo ->
            assertTrue(EncounterConstructionPolicy.archetypePreferences(arquetipo).any {
                it.kind == EncounterConstructionPolicy.Kind.PREFERENCE
            })
        }
    }
    private fun auditDeterminism(a: NpcEncontro, b: NpcEncontro) {
        assertEquals(normalizeForDeterminism(a), normalizeForDeterminism(b))
    }

    private fun auditIdentityAndCoherence(npc: NpcEncontro, lunarMentalCharmNames: Set<String>) {
        EncounterValidationService.validarIdentidadeEstrutural(npc)
        if (npc.tipoExaltado == TipoExaltadoEncontro.LUNAR) {
            assertTrue(npc.attributes.values.sum() >= 27)
        } else {
            val primario = EncounterGenerationRules.ATTRIBUTE_GROUPS.getValue(npc.arquetipo)
            val somaPrimaria = primario.sumOf { npc.attributes.getValue(it) }
            assertEquals(11, somaPrimaria)
            assertEquals(16, npc.attributes.values.sum() - somaPrimaria)
            assertEquals(27, npc.attributes.values.sum())
        }

        when (npc.tipoExaltado) {
            TipoExaltadoEncontro.SOLAR -> {
                assertEquals(5, npc.habilidadesFavorecidas.distinct().size)
                assertTrue(npc.habilidadesFavorecidas.all { (npc.abilities[it] ?: 0) >= 1 })
            }
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> {
                assertTrue(npc.habilidadesFavorecidas.all { (npc.abilities[it] ?: 0) >= 1 })
                val aspecto = Aspecto.entries.first { it.displayName == npc.casta }
                assertEquals(10, (aspecto.allowedAbilities() + npc.habilidadesFavorecidas).distinct().size)
            }
            TipoExaltadoEncontro.LUNAR -> {
                assertEquals(4, (npc.lunarAtributosCasta + npc.habilidadesFavorecidas).distinct().size)
                assertEquals(15, npc.charms.size)
                if (npc.arquetipo == ArquetipoEncontro.FISICO && (npc.attributes["Inteligência"] ?: 0) >= 3) {
                    assertTrue(
                        "Lunar Físico com Inteligência >= 3 deve preservar quatro Encantos Mentais",
                        npc.charms.count { it.nome in lunarMentalCharmNames } >= 4
                    )
                    assertTrue(
                        "Lunar Físico com Inteligência >= 3 deve receber Feitiçaria Terrestre",
                        npc.charms.any { it.nome == "Feitiçaria do Círculo Terrestre" }
                    )
                }
            }
        }

        val report = EncounterValidationService.relatorioConstrucao(npc)
        assertEquals(npc.tipoExaltado, report.tipo)
        assertEquals(npc.arquetipo, report.arquetipo)
        assertTrue(report.regrasObrigatorias.isNotEmpty())
        assertTrue(report.preferencias.isNotEmpty())
    }

    /**
     * Igualdade semântica para auditoria de determinismo. IDs de runtime são
     * deliberadamente gerados com UUID e não fazem parte do contrato de uma
     * seed. O conteúdo da construção deve ser idêntico; os identificadores
     * transitórios não.
     */
    private fun normalizeForDeterminism(npc: NpcEncontro): NpcEncontro = npc.copy(
        id = "",
        merits = npc.merits.map { it.copy(id = "") },
        healthBoxes = npc.healthBoxes.map { it.copy(id = "") }
    )

}
