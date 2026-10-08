package com.example.oldrealm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TranslatorEngineTest {
    @Test fun facadeRoutesToSelectedEngine() {
        val old = TranslatorEngine.translate("ceboreia", TranslatorEngine.WritingSystem.OLD, Locale.forLanguageTag("pt-BR"))
        val high = TranslatorEngine.translate("chaba", TranslatorEngine.WritingSystem.HIGH, Locale.ENGLISH)
        assertTrue(old is TranslatorEngine.Result.OldRealm)
        assertTrue(high is TranslatorEngine.Result.HighRealm)
        assertEquals("GB", (high as TranslatorEngine.Result.HighRealm).value.encoded)
    }
}
