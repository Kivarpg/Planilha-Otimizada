package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterSafeResourceModifierResolverTest {
    
    
    

    @Test
    fun `reduce e cap sem precedencia nao inventam resultado`() {
        val flow=EncounterResourceBudget.Flow("x",EncounterResourceBudget.Resource.MOTES,EncounterResourceBudget.FlowKind.SPEND,10)
        val mods=listOf(
            EncounterSafeResourceModifierResolver.OrderedModifier(EncounterResourceModifierResolver.Modifier("reduce","x",EncounterResourceModifierResolver.Kind.REDUCE_COST,amount=4)),
            EncounterSafeResourceModifierResolver.OrderedModifier(EncounterResourceModifierResolver.Modifier("cap","x",EncounterResourceModifierResolver.Kind.CAP_COST,amount=5))
        )
        assertTrue(EncounterSafeResourceModifierResolver.resolve(listOf(flow),mods,emptySet()) is EncounterSafeResourceModifierResolver.Result.Unresolved)
    }

    @Test
    fun `ordem explicita e aplicada`() {
        val flow=EncounterResourceBudget.Flow("x",EncounterResourceBudget.Resource.MOTES,EncounterResourceBudget.FlowKind.SPEND,10)
        val mods=listOf(
            EncounterSafeResourceModifierResolver.OrderedModifier(
                EncounterResourceModifierResolver.Modifier("cap","x",EncounterResourceModifierResolver.Kind.CAP_COST,amount=5),
                before=setOf("reduce")
            ),
            EncounterSafeResourceModifierResolver.OrderedModifier(
                EncounterResourceModifierResolver.Modifier("reduce","x",EncounterResourceModifierResolver.Kind.REDUCE_COST,amount=2),
                after=setOf("cap")
            )
        )
        val result=EncounterSafeResourceModifierResolver.resolve(listOf(flow),mods,emptySet())
        assertTrue(result is EncounterSafeResourceModifierResolver.Result.Resolved)
        result as EncounterSafeResourceModifierResolver.Result.Resolved
        assertEquals(3,result.flows.single().amount)
    }
}
