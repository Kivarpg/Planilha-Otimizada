package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCharmActivationCompatibilityTest {

    

    @Test
    fun `dois simples nunca recebem sinergia de mesma rodada pela regra geral`() {
        val a = EncounterCharmActivationCompatibility.Profile("A", EncounterCharmActivationCompatibility.CharmType.SIMPLE, EncounterCharmActivationCompatibility.DurationClass.INSTANT)
        val b = EncounterCharmActivationCompatibility.Profile("B", EncounterCharmActivationCompatibility.CharmType.SIMPLE, EncounterCharmActivationCompatibility.DurationClass.INSTANT)

        val result = EncounterCharmActivationCompatibility.evaluate(a, b)

        assertEquals(EncounterCharmActivationCompatibility.Compatibility.BUILD_ONLY, result.compatibility)
        assertFalse(result.realizableNow)
    }

    @Test
    fun `dois simples podem formar relacao sequencial se estado persistir`() {
        val a = EncounterCharmActivationCompatibility.Profile(
            charmId = "A",
            type = EncounterCharmActivationCompatibility.CharmType.SIMPLE,
            duration = EncounterCharmActivationCompatibility.DurationClass.ONE_SCENE,
            effects = listOf(
                EncounterMechanicalEffect(
                    id = "state-x",
                    mechanic = "STATE_X",
                    relation = EncounterMechanicalEffect.Relation.PRODUCES,
                    lifetime = EncounterMechanicalEffect.EffectLifetime.OneScene
                )
            )
        )
        val b = EncounterCharmActivationCompatibility.Profile("B", EncounterCharmActivationCompatibility.CharmType.SIMPLE)

        val result = EncounterCharmActivationCompatibility.evaluate(a, b, requiredStateFromA = "STATE_X")

        assertEquals(EncounterCharmActivationCompatibility.Compatibility.CROSS_ROUND, result.compatibility)
        assertFalse(result.realizableNow)
    }

    @Test
    fun `suplementares diferentes podem compartilhar uma acao valida`() {
        val a = EncounterCharmActivationCompatibility.Profile(
            "A",
            EncounterCharmActivationCompatibility.CharmType.SUPPLEMENTAL,
            supplements = setOf(EncounterCharmActivationCompatibility.ActionKind.WITHERING_ATTACK)
        )
        val b = EncounterCharmActivationCompatibility.Profile(
            "B",
            EncounterCharmActivationCompatibility.CharmType.SUPPLEMENTAL,
            supplements = setOf(EncounterCharmActivationCompatibility.ActionKind.WITHERING_ATTACK)
        )

        val result = EncounterCharmActivationCompatibility.evaluate(a, b)

        assertEquals(EncounterCharmActivationCompatibility.Compatibility.SAME_ACTION, result.compatibility)
        assertTrue(result.realizableNow)
    }

    @Test
    fun `mesmo suplementar nao empilha consigo mesmo`() {
        val a = EncounterCharmActivationCompatibility.Profile(
            "A",
            EncounterCharmActivationCompatibility.CharmType.SUPPLEMENTAL,
            supplements = setOf(EncounterCharmActivationCompatibility.ActionKind.ATTACK),
            stackingRule = EncounterCharmActivationCompatibility.StackingRule.NO_SELF_STACK
        )

        val result = EncounterCharmActivationCompatibility.evaluate(a, a)

        assertFalse(result.realizableNow)
    }

    @Test
    fun `permanente fornece suporte passivo`() {
        val permanent = EncounterCharmActivationCompatibility.Profile("P", EncounterCharmActivationCompatibility.CharmType.PERMANENT, EncounterCharmActivationCompatibility.DurationClass.PERMANENT)
        val simple = EncounterCharmActivationCompatibility.Profile("S", EncounterCharmActivationCompatibility.CharmType.SIMPLE)

        val result = EncounterCharmActivationCompatibility.evaluate(permanent, simple)

        assertEquals(EncounterCharmActivationCompatibility.Compatibility.PASSIVE_SUPPORT, result.compatibility)
        assertTrue(result.realizableNow)
    }

    @Test
    fun `excecao explicita prevalece sobre regra geral`() {
        val a = EncounterCharmActivationCompatibility.Profile(
            "A",
            EncounterCharmActivationCompatibility.CharmType.SIMPLE,
            explicitAllowedWith = setOf("B"),
            explicitAllowedWindow = mapOf("B" to EncounterCharmActivationCompatibility.Window.SAME_ACTION)
        )
        val b = EncounterCharmActivationCompatibility.Profile("B", EncounterCharmActivationCompatibility.CharmType.SIMPLE)

        val result = EncounterCharmActivationCompatibility.evaluate(a, b)

        assertEquals(EncounterCharmActivationCompatibility.Compatibility.SAME_ACTION, result.compatibility)
        assertTrue(result.realizableNow)
    }
}
