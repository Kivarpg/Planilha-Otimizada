package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncounterSocialSynergyAllocationRegressionTest {
    @Test
    fun `precomputed payoff set preserves the four canonical social actions`() {
        val composition = setOf(EncounterSocialSynergy.Tag.ACTION_COMPOSITION)
        val payoffTags = listOf(
            EncounterSocialSynergy.Tag.PERSUADE,
            EncounterSocialSynergy.Tag.INSTILL,
            EncounterSocialSynergy.Tag.INSPIRE,
            EncounterSocialSynergy.Tag.READ_INTENTIONS
        )
        for (tag in payoffTags) {
            val score = EncounterSocialSynergy.scoreTags(composition + tag, emptyList())
            assertTrue(score >= 6, "expected composition payoff for $tag")
        }
    }

    @Test
    fun `unrelated tag does not receive action composition payoff`() {
        val candidate = setOf(
            EncounterSocialSynergy.Tag.ACTION_COMPOSITION,
            EncounterSocialSynergy.Tag.THREATEN
        )
        assertEquals(0, EncounterSocialSynergy.scoreTags(candidate, emptyList()))
    }
}
