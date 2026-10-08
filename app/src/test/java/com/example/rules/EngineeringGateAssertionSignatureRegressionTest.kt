package com.example.rules

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EngineeringGateAssertionSignatureRegressionTest {
    @Test
    fun `assertTrue uses actual before message`() {
        val source = File("src/test/java/com/example/rules/EngineeringGateConfigurationTest.kt").readText()
        assertTrue(source.contains("assertTrue(workflow.contains(name), \"engineering gate must include \$name\")"))
        assertFalse(source.contains("assertTrue(\"engineering gate must include \$name\", workflow.contains(name))"))
    }
}
