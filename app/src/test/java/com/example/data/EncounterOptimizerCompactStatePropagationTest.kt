package com.example.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EncounterOptimizerCompactStatePropagationTest {
    @Test
    fun `beam reutiliza chave compacta do estado expandido`() {
        val source = File("src/main/java/com/example/data/EncounterCharmRouteOptimizer.kt").readText()
        assertTrue(source.contains("val compactKey:CompactEligibilityKey"))
        assertTrue(source.contains("val candidateName = nome(candidate)"))
        assertTrue(source.contains("val compactKey = prepared.compactKeyAfter(state.compactKey, candidateName, category)"))
        assertFalse(source.contains("compactKey = prepared.compactKey(selected, counts)"))
        assertTrue(source.contains("val key = candidateState.compactKey"))
        assertFalse(source.contains("val key = prepared.compactKey(candidateState.selecionados, candidateState.contagens)"))
    }
}
