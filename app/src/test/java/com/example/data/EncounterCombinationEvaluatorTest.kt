package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCombinationEvaluatorTest {

    

    private fun effect(id: String, mechanic: String) = EncounterMechanicalEffect(
        id = id,
        mechanic = mechanic,
        relation = EncounterMechanicalEffect.Relation.PRODUCES,
        lifetime = EncounterMechanicalEffect.EffectLifetime.Instant
    )

    @Test
    fun `ramos exclusivos nao sao somados`() {
        val power = EncounterCombinationEvaluator.Power(
            id = "DualPower",
            variants = listOf(
                EncounterCombinationEvaluator.PowerVariant(
                    id = "base",
                    branches = listOf(
                        EncounterCombinationEvaluator.EffectBranch(
                            id = "withering",
                            effects = listOf(effect("w", "WITHERING_DAMAGE")),
                            choices = setOf(EncounterCombinationEvaluator.ChoiceKey("attack-mode", "WITHERING"))
                        ),
                        EncounterCombinationEvaluator.EffectBranch(
                            id = "decisive",
                            effects = listOf(effect("d", "DECISIVE_DAMAGE")),
                            choices = setOf(EncounterCombinationEvaluator.ChoiceKey("attack-mode", "DECISIVE"))
                        )
                    )
                )
            )
        )

        val result = EncounterCombinationEvaluator.realize(listOf(power), EncounterCombinationEvaluator.Configuration())

        assertTrue(result.realizable)
        assertEquals(1, result.realization!!.effects.size)
    }

    @Test
    fun `configuracoes contraditorias entre poderes tornam conjunto irrealizavel`() {
        val a = EncounterCombinationEvaluator.Power("A", listOf(
            EncounterCombinationEvaluator.PowerVariant("v", branches = listOf(
                EncounterCombinationEvaluator.EffectBranch("b", effects = listOf(effect("a", "X")),
                    choices = setOf(EncounterCombinationEvaluator.ChoiceKey("weapon", "SWORD")))
            ))
        ))
        val b = EncounterCombinationEvaluator.Power("B", listOf(
            EncounterCombinationEvaluator.PowerVariant("v", branches = listOf(
                EncounterCombinationEvaluator.EffectBranch("b", effects = listOf(effect("b", "Y")),
                    choices = setOf(EncounterCombinationEvaluator.ChoiceKey("weapon", "BOW")))
            ))
        ))

        val result = EncounterCombinationEvaluator.realize(listOf(a, b), EncounterCombinationEvaluator.Configuration())

        assertFalse(result.realizable)
    }

    @Test
    fun `efeito de variante nao vaza para outra variante`() {
        val power = EncounterCombinationEvaluator.Power("MA", listOf(
            EncounterCombinationEvaluator.PowerVariant(
                "terrestrial",
                branches = listOf(EncounterCombinationEvaluator.EffectBranch("t", effects = listOf(effect("t", "T")))),
                choices = setOf(EncounterCombinationEvaluator.ChoiceKey("ma-variant", "TERRESTRIAL"))
            ),
            EncounterCombinationEvaluator.PowerVariant(
                "mastery",
                branches = listOf(EncounterCombinationEvaluator.EffectBranch("m", effects = listOf(effect("m", "M")))),
                choices = setOf(EncounterCombinationEvaluator.ChoiceKey("ma-variant", "MASTERY"))
            )
        ))

        val result = EncounterCombinationEvaluator.realize(
            listOf(power),
            EncounterCombinationEvaluator.Configuration(choices = mapOf("ma-variant" to "TERRESTRIAL"))
        )

        assertTrue(result.realizable)
        assertEquals(setOf("T"), result.realization!!.effects.map { it.effect.mechanic }.toSet())
    }

    @Test
    fun `restricao de equipamento participa da realizabilidade`() {
        val power = EncounterCombinationEvaluator.Power("A", listOf(
            EncounterCombinationEvaluator.PowerVariant("v", branches = listOf(
                EncounterCombinationEvaluator.EffectBranch(
                    "b",
                    effects = listOf(effect("e", "X")),
                    requiredEquipment = setOf("SWORD")
                )
            ))
        ))

        assertFalse(EncounterCombinationEvaluator.realize(listOf(power), EncounterCombinationEvaluator.Configuration()).realizable)
        assertTrue(
            EncounterCombinationEvaluator.realize(
                listOf(power),
                EncounterCombinationEvaluator.Configuration(equipment = setOf("SWORD"))
            ).realizable
        )
    }

    @Test
    fun `mesma evidencia causal nao soma varias vezes`() {
        val a = EncounterSynergyEvidence(
            "CRASH", "A", "B", "e1", "e2",
            EncounterCombinationEvaluator.Timing.SAME_ACTION, "A:e1->B:e2:CRASH", 12
        )
        val duplicate = a.copy(value = 20)

        val reduced = EncounterSynergyEvidenceReducer.reduce(listOf(a, duplicate))

        assertEquals(1, reduced.size)
        assertEquals(20, reduced.single().value)
    }
    @Test
    fun `multiplas escolhas do mesmo lote preservam todas as opcoes`() {
        val choices = linkedSetOf(
            EncounterCombinationEvaluator.ChoiceKey("weapon", "SWORD"),
            EncounterCombinationEvaluator.ChoiceKey("stance", "GUARD")
        )
        val power = EncounterCombinationEvaluator.Power("P", listOf(
            EncounterCombinationEvaluator.PowerVariant("v", branches = listOf(
                EncounterCombinationEvaluator.EffectBranch("b", effects = emptyList(), choices = choices)
            ))
        ))

        val result = EncounterCombinationEvaluator.realize(
            listOf(power), EncounterCombinationEvaluator.Configuration()
        )

        assertTrue(result.realizable)
        assertEquals("SWORD", result.realization!!.configuration.choices["weapon"])
        assertEquals("GUARD", result.realization!!.configuration.choices["stance"])
    }

    @Test
    fun `conflito tardio em lote de escolhas rejeita a realizacao`() {
        val choices = linkedSetOf(
            EncounterCombinationEvaluator.ChoiceKey("stance", "GUARD"),
            EncounterCombinationEvaluator.ChoiceKey("weapon", "BOW")
        )
        val power = EncounterCombinationEvaluator.Power("P", listOf(
            EncounterCombinationEvaluator.PowerVariant("v", branches = listOf(
                EncounterCombinationEvaluator.EffectBranch("b", effects = emptyList(), choices = choices)
            ))
        ))
        val initial = EncounterCombinationEvaluator.Configuration(
            choices = mapOf("weapon" to "SWORD")
        )

        assertFalse(EncounterCombinationEvaluator.realize(listOf(power), initial).realizable)
    }
}
