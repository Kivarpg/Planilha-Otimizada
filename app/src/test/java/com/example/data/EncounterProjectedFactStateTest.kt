package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterProjectedFactStateTest {
    

    @Test
    fun `troca de estado exclusivo remove fato anterior`() {
        var state=EncounterProjectedFactState.State()
        state=state.add(EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("AURA_FIRE"),EncounterProjectedFactState.SourceId("aura"),EncounterProjectedFactState.Lifetime.TRANSIENT,"AURA"))
        state=state.add(EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("AURA_WATER"),EncounterProjectedFactState.SourceId("aura"),EncounterProjectedFactState.Lifetime.TRANSIENT,"AURA"))
        assertFalse(state.has(EncounterProjectedFactState.FactId("AURA_FIRE")))
        assertTrue(state.has(EncounterProjectedFactState.FactId("AURA_WATER")))
    }

    @Test
    fun `revogar fonte remove fatos derivados`() {
        var state=EncounterProjectedFactState.State(setOf(
            EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("FORM_BONUS"),EncounterProjectedFactState.SourceId("wolf-form"),EncounterProjectedFactState.Lifetime.TRANSIENT)
        ))
        state=state.revokeSource(EncounterProjectedFactState.SourceId("wolf-form"))
        assertFalse(state.has(EncounterProjectedFactState.FactId("FORM_BONUS")))
    }

    @Test
    fun `consumir estado impede fato fantasma`() {
        var state=EncounterProjectedFactState.State(setOf(
            EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("AURA"),EncounterProjectedFactState.SourceId("aura"),EncounterProjectedFactState.Lifetime.TRANSIENT)
        ))
        state=state.consume(EncounterProjectedFactState.FactId("AURA"))
        assertFalse(state.has(EncounterProjectedFactState.FactId("AURA")))
    }

    @Test
    fun `expiracao respeita lifetime`() {
        var state=EncounterProjectedFactState.State(setOf(
            EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("ROUND_BUFF"),EncounterProjectedFactState.SourceId("x"),EncounterProjectedFactState.Lifetime.ROUND),
            EncounterProjectedFactState.Fact(EncounterProjectedFactState.FactId("BUILD_FACT"),EncounterProjectedFactState.SourceId("y"),EncounterProjectedFactState.Lifetime.BUILD)
        ))
        state=state.expire(EncounterProjectedFactState.Lifetime.ROUND)
        assertFalse(state.has(EncounterProjectedFactState.FactId("ROUND_BUFF")))
        assertTrue(state.has(EncounterProjectedFactState.FactId("BUILD_FACT")))
    }
}
