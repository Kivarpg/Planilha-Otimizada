package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EncounterResourceModifierResolverTest {
    
    

    @Test
    fun `substituicao explicita troca custo impresso`() {
        val flow = EncounterResourceBudget.Flow("combo", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.SPEND, 10)
        val modifier = EncounterResourceModifierResolver.Modifier(
            "replacement", "combo", EncounterResourceModifierResolver.Kind.REPLACE_COST,
            replacementAmount = 7
        )
        val resolved = EncounterResourceModifierResolver.resolve(listOf(flow), listOf(modifier), emptySet())
        assertEquals(7, resolved.single().amount)
    }

    @Test
    fun `modificador condicionado nao vaza sem requisito`() {
        val flow = EncounterResourceBudget.Flow("a", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.SPEND, 10)
        val modifier = EncounterResourceModifierResolver.Modifier(
            "reduce", "a", EncounterResourceModifierResolver.Kind.REDUCE_COST, amount = 5,
            requirement = EncounterRequirementExpression.Fact("ENABLED")
        )
        assertEquals(10, EncounterResourceModifierResolver.resolve(listOf(flow), listOf(modifier), emptySet()).single().amount)
        assertEquals(5, EncounterResourceModifierResolver.resolve(listOf(flow), listOf(modifier), setOf("ENABLED")).single().amount)
    }

    @Test
    fun `refund nao e abatido automaticamente`() {
        val flow = EncounterResourceBudget.Flow("a", EncounterResourceBudget.Resource.MOTES, EncounterResourceBudget.FlowKind.SPEND, 10)
        val refund = EncounterResourceModifierResolver.Modifier("refund", "a", EncounterResourceModifierResolver.Kind.REFUND, amount = 5)
        assertEquals(10, EncounterResourceModifierResolver.resolve(listOf(flow), listOf(refund), emptySet()).single().amount)
    }
}
