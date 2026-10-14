package com.example.ui.tabs

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A UI deve apresentar Nome, Jogador e Conceito em campos legiveis.
 * Teste da politica geometrica; nao substitui verificacao no celular.
 */
class PersonalDataResponsiveLayoutTest {
    @Test fun `phone widths always stack all three fields`() {
        listOf(280f, 320f, 360f, 390f, 412f, 480f, 600f).forEach { width ->
            assertTrue(personalIdentityNeedsStacking(width, 1f), "width=$width")
        }
    }

    @Test fun `tablet split screen also stacks identity fields`() {
        listOf(720f, 800f, 899f).forEach { width ->
            assertTrue(personalIdentityNeedsStacking(width, 1f), "width=$width")
        }
    }

    @Test fun `wide layout retains existing three column design`() {
        assertFalse(personalIdentityNeedsStacking(900f, 1f))
        assertFalse(personalIdentityNeedsStacking(1200f, 1.2f))
    }

    @Test fun `increased font scale favors readable stacked inputs`() {
        assertTrue(personalIdentityNeedsStacking(1200f, 1.3f))
        assertTrue(personalIdentityNeedsStacking(900f, 1.5f))
    }
}
