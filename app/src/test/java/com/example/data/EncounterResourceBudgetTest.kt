package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterResourceBudgetTest {
    

    @Test
    fun `motes e motes de feiticaria nao sao somados como mesmo recurso`() {
        val profile = EncounterResourceBudget.Profile(
            flows = listOf(
                EncounterResourceBudget.Flow("charm", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.SPEND, 5),
                EncounterResourceBudget.Flow("spell", EncounterResourceBudget.Resource.SORCEROUS_MOTES, EncounterResourceBudget.FlowKind.SPEND, 15)
            ),
            capacities = mapOf(
                EncounterResourceBudget.Resource.MOTES to EncounterResourceBudget.Capacity(EncounterResourceBudget.Resource.MOTES, available = 10),
                EncounterResourceBudget.Resource.SORCEROUS_MOTES to EncounterResourceBudget.Capacity(EncounterResourceBudget.Resource.SORCEROUS_MOTES, available = 20)
            )
        )
        val result = EncounterResourceBudget.assess(profile)
        assertFalse(result.impossible)
        assertFalse(EncounterResourceBudget.Resource.MOTES in result.unsupportedResources)
        assertFalse(EncounterResourceBudget.Resource.SORCEROUS_MOTES in result.unsupportedResources)
    }

    @Test
    fun `compromisso acima da capacidade e impossivel`() {
        val profile = EncounterResourceBudget.Profile(
            flows = listOf(
                EncounterResourceBudget.Flow("a", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.COMMIT, 6),
                EncounterResourceBudget.Flow("b", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.COMMIT, 5)
            ),
            capacities = mapOf(
                EncounterResourceBudget.Resource.MOTES to EncounterResourceBudget.Capacity(EncounterResourceBudget.Resource.MOTES, available = 15, reservable = 10)
            )
        )
        assertTrue(EncounterResourceBudget.assess(profile).impossible)
    }

    @Test
    fun `transferencia nao e tratada como geracao`() {
        val profile = EncounterResourceBudget.Profile(
            flows = listOf(
                EncounterResourceBudget.Flow("transfer", EncounterResourceBudget.Resource.INITIATIVE, EncounterResourceBudget.FlowKind.TRANSFER, 3),
                EncounterResourceBudget.Flow("cost", EncounterResourceBudget.Resource.INITIATIVE, EncounterResourceBudget.FlowKind.SPEND, 2)
            )
        )
        val result = EncounterResourceBudget.assess(profile)
        assertTrue(EncounterResourceBudget.Resource.INITIATIVE in result.unsupportedResources)
    }

    @Test
    fun `custo desconhecido reduz certeza sem inventar numero`() {
        val profile = EncounterResourceBudget.Profile(
            flows = listOf(
                EncounterResourceBudget.Flow("variable", EncounterResourceBudget.Resource.WILLPOWER, EncounterResourceBudget.FlowKind.SPEND, null)
            ),
            capacities = mapOf(
                EncounterResourceBudget.Resource.WILLPOWER to EncounterResourceBudget.Capacity(EncounterResourceBudget.Resource.WILLPOWER, available = 5)
            )
        )
        val result = EncounterResourceBudget.assess(profile)
        assertTrue(EncounterResourceBudget.Resource.WILLPOWER in result.unknownQuantities)
        assertFalse(result.impossible)
    }

    @Test
    fun `concentracao e tensao e nao proibicao`() {
        val profile = EncounterResourceBudget.Profile(
            flows = listOf(
                EncounterResourceBudget.Flow("a", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.SPEND, 2),
                EncounterResourceBudget.Flow("b", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.SPEND, 3),
                EncounterResourceBudget.Flow("c", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.SPEND, 4)
            ),
            capacities = mapOf(
                EncounterResourceBudget.Resource.MOTES to EncounterResourceBudget.Capacity(EncounterResourceBudget.Resource.MOTES, available = 20)
            )
        )
        val result = EncounterResourceBudget.assess(profile)
        assertTrue(EncounterResourceBudget.Resource.MOTES in result.concentratedResources)
        assertFalse(result.impossible)
    }
}
