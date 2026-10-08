package com.example.oldrealm

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class OldRealmTranslatorTest {
    @Test fun inventoryContains90Syllables() {
        assertEquals(90, OldRealmSyllableMap.keys.size)
        assertTrue(OldRealmSyllableMap.keys.containsAll(listOf("A", "BA", "CHA", "DE", "FU", "YA", "YU")))
    }

    @Test fun canonicalExceptionsRemainStable() {
        assertEquals(listOf("E", "XA", "LA", "TE", "DE"), OldRealmTranslator.translate("EXALTED", Locale.ENGLISH).syllables)
        assertEquals(listOf("GA", "DE"), OldRealmTranslator.translate("JADE", Locale.ENGLISH).syllables)
        assertEquals(listOf("KA", "WA", "RA", "TA", "XA"), OldRealmTranslator.translate("QUARTZ", Locale.ENGLISH).syllables)
    }

    @Test fun arbitraryWordIsNotRejected() {
        val result = OldRealmTranslator.translate("ceboreia", Locale.forLanguageTag("pt-BR"))
        assertTrue(result.syllables.isNotEmpty())
        assertTrue(result.glyphs.isNotEmpty())
    }

    @Test fun portugueseCUsesKOrSContext() {
        val k = OldRealmTranslator.translate("KASACO", Locale.forLanguageTag("pt-BR"))
        val s = OldRealmTranslator.translate("SIDADE", Locale.forLanguageTag("pt-BR"))
        assertTrue(k.syllables.isNotEmpty())
        assertTrue(s.syllables.isNotEmpty())
    }

    @Test fun digitsOneToFiveBecomeWords() {
        assertEquals("UM", OldRealmTranslator.translate("1", Locale.forLanguageTag("pt-BR")).normalizedInput)
        assertEquals("FIVE", OldRealmTranslator.translate("5", Locale.ENGLISH).normalizedInput)
    }

    @Test fun punctuationIsIgnored() {
        val result = OldRealmTranslator.translate("CASA!", Locale.forLanguageTag("pt-BR"))
        assertFalse(result.normalizedInput.contains('!'))
    }

    @Test fun newlinesRemainSeparateLines() {
        val result = OldRealmTranslator.translate("CASA\nBATATA", Locale.forLanguageTag("pt-BR"))
        assertEquals(2, result.lines.size)
        assertEquals(1, result.lines[0].size)
        assertEquals(1, result.lines[1].size)
    }

    @Test fun manyWordsAreKeptAsSeparateLogicalWords() {
        val result = OldRealmTranslator.translate((1..500).joinToString(" ") { "ceboreia" }, Locale.forLanguageTag("pt-BR"))
        assertEquals(500, result.words.size)
    }

    @Test fun longWordIsNotArtificiallyChunked() {
        val result = OldRealmTranslator.translate("abcdefghijklmnopqrstuvwx", Locale.ENGLISH)
        assertEquals(1, result.words.size)
        assertTrue(result.words.first().size > 5)
    }

    @Test fun digitsAreSeparatedFromAdjacentLetters() {
        assertEquals("A UM B", OldRealmTranslator.translate("A1B", Locale.forLanguageTag("pt-BR")).normalizedInput)
    }

    @Test fun accentsAreIgnored() {
        assertEquals("TRES", OldRealmTranslator.translate("três", Locale.forLanguageTag("pt-BR")).normalizedInput)
    }

    @Test fun unsupportedLanguageFallsBackToEnglish() {
        assertEquals(OldRealmTranslator.SupportedLanguage.ENGLISH,
            OldRealmTranslator.detectLanguage(Locale.FRENCH))
    }

}
