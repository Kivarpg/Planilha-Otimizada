package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterRequirementExpressionTest {

    

    @Test
    fun `AND exige todos os requisitos`() {
        val requirement = EncounterRequirementExpression.AllOf(listOf(EncounterRequirementExpression.Fact("A"), EncounterRequirementExpression.Fact("B"), EncounterRequirementExpression.Fact("C")))

        assertFalse(requirement.isSatisfiedBy(setOf("A", "B")))
        assertTrue(requirement.isSatisfiedBy(setOf("A", "B", "C")))
    }

    @Test
    fun `OR exige apenas uma alternativa`() {
        val requirement = EncounterRequirementExpression.AnyOf(listOf(EncounterRequirementExpression.Fact("A"), EncounterRequirementExpression.Fact("B")))

        assertTrue(requirement.isSatisfiedBy(setOf("A")))
        assertTrue(requirement.isSatisfiedBy(setOf("B")))
        assertFalse(requirement.isSatisfiedBy(emptySet()))
    }

    @Test
    fun `NOT bloqueia fato presente`() {
        val requirement = EncounterRequirementExpression.Not(EncounterRequirementExpression.Fact("ARMORED"))

        assertTrue(requirement.isSatisfiedBy(emptySet()))
        assertFalse(requirement.isSatisfiedBy(setOf("ARMORED")))
    }

    @Test
    fun `expressao composta preserva parenteses`() {
        // A AND (B OR C)
        val requirement = EncounterRequirementExpression.AllOf(
            listOf(
                EncounterRequirementExpression.Fact("A"),
                EncounterRequirementExpression.AnyOf(listOf(EncounterRequirementExpression.Fact("B"), EncounterRequirementExpression.Fact("C")))
            )
        )

        assertTrue(requirement.isSatisfiedBy(setOf("A", "C")))
        assertFalse(requirement.isSatisfiedBy(setOf("B", "C")))
    }
}
