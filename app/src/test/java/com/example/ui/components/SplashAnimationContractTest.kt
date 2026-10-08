package com.example.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SplashAnimationContractTest {
    private val source = File("src/main/java/com/example/ui/components/SplashScreen.kt").readText()

    @Test fun `symbols are not activated before their stagger delay`() {
        val start = source.indexOf("repeat(N) { index ->")
        val delay = source.indexOf("if (index > 0) delay(ATRASO_ENTRE_ICONES_MS)", start)
        val activate = source.indexOf("iconeAtivo = index", start)
        assertTrue(delay >= 0)
        assertTrue(activate > delay)
    }

    @Test fun `inactive symbols are not composed`() {
        assertTrue(source.contains("if (index <= iconeAtivo && progress < 1f)"))
    }
}
