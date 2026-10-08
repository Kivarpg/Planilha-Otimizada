package com.example.rules

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EngineeringGateConfigurationTest {
    @Test
    fun `workflow points engineering gate at an existing non excluded test class`() {
        val workflow = File("../.github/workflows/build_apk.yml").readText()
        assertTrue(
            workflow.contains("testDebugUnitTest") &&
                workflow.contains("EncounterEngineeringBaselineTest")
        )
        assertFalse(workflow.contains("CompactSelectionIndexTest"))
        assertFalse(workflow.contains("EngineeringBaseline494Test"))
        assertFalse(workflow.contains("engineeringPerformanceGate"))
    }
    @Test
    fun `engineering gate protects canonical invariants without excluded audit classes`() {
        val workflow = java.io.File("../.github/workflows/build_apk.yml").readText()
        listOf(
            "EncounterEngineeringBaselineTest",
            "CanonicalEncounterRulesTest",
            "EncounterCustomizationRegressionTest",
            "EncounterGenerationRulesRegressionTest",
            "EncounterRouteEligibilityRegressionTest",
            "LunarSpiritFormPreparedContextCacheTest",
            "EncounterOptimizerCompactStatePropagationTest",
            "EncounterNpcAuditorContractTest",
            "EncounterMechanicalGoldenSnapshotTest",
            "EncounterArchetypeMatrixTest",
            "EncounterSorceryRoutePolicyTest",
            "EncounterSpecializationBoundaryTest",
            "EncounterPostCycleConvergenceTest",
            "EncounterCpuSafetyBudgetTest",
            "AndroidSecurityBaselineTest"
        ).forEach { name ->
            assertTrue(workflow.contains(name), "engineering gate must include $name")
        }
        assertFalse(workflow.contains("engineeringPerformanceGate"))
    }

}
