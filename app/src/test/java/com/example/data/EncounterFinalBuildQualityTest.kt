package com.example.data

import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterFinalBuildQualityTest {
    @Test
    fun explicitSolarFocusRewardsAbilityAndCharmConcentration() {
        val focused = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            focoProgressaoExplicito = "Briga",
            abilities = mapOf("Briga" to 5),
            charms = listOf(
                com.example.model.EncantoEncontro("A", "Briga", ""),
                com.example.model.EncantoEncontro("B", "Briga", "")
            )
        )
        val unfocused = focused.copy(
            abilities = mapOf("Briga" to 1),
            charms = emptyList()
        )
        val catalog = PreparedEncounterCatalog.prepare(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        val focusedScore = EncounterFinalBuildQuality.evaluate(focused, catalog)
        val unfocusedScore = EncounterFinalBuildQuality.evaluate(unfocused, catalog)

        assertTrue(focusedScore.focus > unfocusedScore.focus)
    }

    @Test
    fun structuralAuditErrorsInvalidateCandidateRegardlessOfNumericStats() {
        val impossible = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            formaEspiritual = "",
            acaoPrincipal = 99,
            acaoDecisiva = 99,
            esquiva = 99,
            absorcao = 99,
            dureza = 99
        )
        val catalog = PreparedEncounterCatalog.prepare(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        val score = EncounterFinalBuildQuality.evaluate(impossible, catalog)

        assertFalse(score.valid)
        assertTrue(score.validationErrors > 0)
    }
    @Test
    fun legalityAlwaysOutranksNumericQuality() {
        val valid = EncounterFinalBuildQuality.Score(
            valid = true,
            total = -10_000,
            focus = 0,
            charmCoherence = 0,
            offense = 0,
            defense = 0,
            equipment = 0,
            validationErrors = 0,
            validationWarnings = 0
        )
        val invalid = valid.copy(
            valid = false,
            total = 10_000,
            validationErrors = 1
        )

        assertTrue(EncounterFinalBuildQuality.comparator.compare(valid, invalid) > 0)
    }

}
