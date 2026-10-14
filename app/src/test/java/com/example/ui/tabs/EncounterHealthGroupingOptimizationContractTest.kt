package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncounterHealthGroupingOptimizationContractTest {
    private val npcCard = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()

    @Test fun `vitality penalty groups render in original index order without repeated lookup`() {
        assertTrue(npcCard.contains("ordemPenalidadesNpc.forEachIndexed { grupoIndex, penalidadeRotulo ->"))
        assertTrue(npcCard.contains("val caixasDoGrupo = gruposPorPenalidadeNpc[grupoIndex]"))
        assertFalse(npcCard.contains("ordemPenalidadesNpc.indexOf(penalidadeRotulo)"))
    }
}
