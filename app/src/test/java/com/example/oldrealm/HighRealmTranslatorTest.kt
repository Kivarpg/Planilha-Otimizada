package com.example.oldrealm

import com.example.oldrealm.highrealm.HighRealmLayout
import com.example.oldrealm.highrealm.HighRealmTranslator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighRealmTranslatorTest {
    @Test fun longestMatchWins() {
        val result = HighRealmTranslator.translate("cha")
        assertEquals("G", result.encoded)
        assertEquals(0, result.unknownCount)
    }

    @Test fun syllablesAreProcessedSequentially() {
        val result = HighRealmTranslator.translate("chaba")
        assertEquals("GB", result.encoded)
        assertEquals(listOf(listOf(listOf("G", "B"))), result.lines)
    }

    @Test fun accentsAndPunctuationAreIgnored() {
        val result = HighRealmTranslator.translate("á, chá!")
        // "á" e "chá" são separadas por um espaço na entrada (a vírgula é
        // só pontuação removida, mas o espaço que a segue continua sendo
        // um separador de palavra de verdade) — por isso "A" e "G" saem
        // como palavras distintas, com espaço entre elas, não fundidas.
        assertEquals("A G", result.encoded)
        assertEquals(0, result.unknownCount)
    }

    @Test fun unknownCharactersNeverLeakIntoEncodedOutput() {
        val result = HighRealmTranslator.translate("b")
        assertEquals("", result.encoded)
        assertEquals(1, result.unknownCount)
        assertTrue(result.lines.all { line -> line.isEmpty() })
    }

    @Test fun explicitLinesArePreserved() {
        val result = HighRealmTranslator.translate("ba\nda")
        assertEquals(2, result.lines.size)
    }

    @Test fun wholeWordsAreNeverSplitWhenTheyFitOrMoveToNextColumn() {
        val columns = HighRealmLayout.pack(
            listOf(listOf("A", "B"), listOf("C", "D"), listOf("E")),
            maxRows = 3
        )
        assertEquals(3, columns.size)
        assertEquals(listOf(listOf("A", "B")), columns[0].words)
        assertEquals(listOf(listOf("C", "D")), columns[1].words)
    }

    @Test fun oversizedWordRemainsIntact() {
        val word = listOf("A", "B", "C", "D")
        val columns = HighRealmLayout.pack(listOf(word), maxRows = 3)
        assertEquals(1, columns.size)
        assertEquals(word, columns.single().words.single())
    }
}
