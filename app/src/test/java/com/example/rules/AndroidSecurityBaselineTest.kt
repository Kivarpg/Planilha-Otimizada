package com.example.rules

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSecurityBaselineTest {
    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile ?: return@repeat
        }
        error("Raiz do projeto não encontrada.")
    }

    @Test
    fun `manifest mantem superficie Android minima`() {
        val manifest = File(projectRoot(), "app/src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertFalse(manifest.contains("MANAGE_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("REQUEST_INSTALL_PACKAGES"))
        assertFalse(manifest.contains("SYSTEM_ALERT_WINDOW"))
        assertTrue(manifest.contains("""android:allowBackup="false""""))
        assertTrue(manifest.contains("""android:usesCleartextTraffic="false""""))
        assertTrue(manifest.contains("""android:maxSdkVersion="28""""))
        assertTrue(Regex("""android:exported="true"""").findAll(manifest).count() == 1)
    }

    @Test
    fun `release permanece nao depuravel minificado e encolhido`() {
        val gradle = File(projectRoot(), "app/build.gradle.kts").readText()
        val release = gradle.substringAfter("release {").substringBefore("debug {")
        assertTrue(release.contains("isDebuggable = false"))
        assertTrue(release.contains("isJniDebuggable = false"))
        assertTrue(release.contains("isMinifyEnabled = true"))
        assertTrue(release.contains("isShrinkResources = true"))
    }

    @Test
    fun `logs de excecao conhecidos nao vazam para logcat em release`() {
        val root = projectRoot()
        val repository = File(root, "app/src/main/java/com/example/data/SheetRepository.kt").readText()
        val pdf = File(root, "app/src/main/java/com/example/ui/tabs/EncounterCombatEquations.kt").readText()
        assertTrue(repository.contains("if (BuildConfig.DEBUG)"))
        assertTrue(pdf.contains("if (com.example.BuildConfig.DEBUG)"))
    }
}
