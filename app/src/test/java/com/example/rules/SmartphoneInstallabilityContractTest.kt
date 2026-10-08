package com.example.rules

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartphoneInstallabilityContractTest {
    private fun root(): File {
        var current = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile ?: return@repeat
        }
        error("Raiz")
    }

    @Test
    fun contract() {
        val build = File(root(), "app/build.gradle.kts").readText()
        val workflow = File(root(), ".github/workflows/build_apk.yml").readText()

        assertTrue(build.contains("versionCode = exaltedVersionNumber"))
        assertTrue(build.contains("versionName = exaltedVersionNumber.toString()"))
        assertFalse(build.contains("abiFilters"))
        assertTrue(workflow.contains("Smartphone installability gate"))
        assertTrue(workflow.contains("apksigner"))
        assertTrue(workflow.contains("zipalign"))
        assertTrue(workflow.contains("aapt"))
    }
}
