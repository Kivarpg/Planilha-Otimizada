package com.example.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncounterNpcAuditorContractTest {
    private val source = File("src/main/java/com/example/data/EncounterNpcAuditor.kt").readText()

    @Test fun auditorIsIndependentAndDoesNotMutateNpc() {
        assertTrue(source.contains("internal object EncounterNpcAuditor"))
        assertTrue(source.contains("fun audit(npc: NpcEncontro, catalog: PreparedEncounterCatalog)"))
        assertTrue(source.contains("EncounterRulesEngine.evaluate"))
        assertTrue(source.contains("validarIdentidadeEstrutural(npc)"))
        assertFalse(source.contains("npc.copy("))
    }

    @Test fun auditorCoversCoreIndependentInvariants() {
        listOf(
            "STRUCTURAL_INVARIANT",
            "CHARM_NOT_IN_ALLOWED_CATALOG",
            "CHARM_REQUIREMENTS_UNSATISFIED",
            "MARTIAL_STYLE_EXALT_TYPE",
            "SPELL_NOT_IN_CATALOG",
            "SPELL_CIRCLE_MISMATCH",
            "INITIAL_SPELL_MISSING",
            "LUNAR_PRIMARY_SPIRIT_FORM_MISSING",
            "LUNAR_UNKNOWN_ARCHETYPE_TRAIT"
        ).forEach { code -> assertTrue(source.contains(code), "auditor must cover $code") }
    }
}
