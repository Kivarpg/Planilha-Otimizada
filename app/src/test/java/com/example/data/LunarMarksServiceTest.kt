package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale
import kotlin.random.Random

class LunarMarksServiceTest {
    @Test
    fun listContainsExactly300Marks() {
        assertEquals(300, LunarMarksService.size())
    }

    @Test
    fun portugueseAndEnglishAreBothPresent() {
        val pt = LunarMarksService.sortearExibicao(Locale.forLanguageTag("pt-BR"), Random(1))
        val en = LunarMarksService.sortearExibicao(Locale.ENGLISH, Random(1))
        assertFalse(pt.isBlank())
        assertFalse(en.isBlank())
        assertFalse(pt == en)
    }
}
