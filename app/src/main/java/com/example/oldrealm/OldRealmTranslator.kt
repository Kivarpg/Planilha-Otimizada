package com.example.oldrealm

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic, fully offline Old Realm transliterator.
 * Supported input languages: Portuguese and English.
 */
object OldRealmTranslator {
    enum class TranslationState { TRANSLATED, DEGRADED, UNKNOWN }

    data class Translation(
        val originalInput: String,
        val normalizedInput: String,
        val lines: List<List<List<String>>>,
        val warnings: List<String>,
        val state: TranslationState,
        val language: SupportedLanguage
    ) {
        /** Flattened syllables, derived from the canonical line/word structure. */
        val syllables: List<String> get() = lines.flatten().flatten()
        /** Flattened words, derived without storing a duplicate collection. */
        val words: List<List<String>> get() = lines.flatten()
        /** Encoded Old Realm glyph string, derived from the canonical line/word structure. */
        val glyphs: String
            get() = buildString {
                lines.forEachIndexed { lineIndex, line ->
                    if (lineIndex > 0) append('\n')
                    line.forEachIndexed { wordIndex, word ->
                        if (wordIndex > 0) append(' ')
                        word.forEach { OldRealmSyllableMap.glyphString(it)?.let(::append) }
                    }
                }
            }
    }

    enum class SupportedLanguage { PORTUGUESE, ENGLISH }

    private data class WordTranslation(val syllables: List<String>, val warnings: List<String>)

    private val wordCache = object : LinkedHashMap<String, WordTranslation>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WordTranslation>?) = size > 512
    }
    private val wordCacheLock = Any()

    private val vowels = setOf('A', 'E', 'I', 'O', 'U')
    private val consonants = setOf('B','C','D','F','G','H','K','L','M','N','P','Q','R','S','T','V','W','X','Y','Z')

    private val officialExceptions = mapOf(
        "EXALTED" to listOf("E", "XA", "LA", "TE", "DE"),
        "JADE" to listOf("GA", "DE"),
        "STRIP" to listOf("SI", "TI", "RI", "PI")
    )

    private const val MAX_INPUT_LENGTH = 20_000

    private val numberWordsPt = mapOf('1' to "UM", '2' to "DOIS", '3' to "TRES", '4' to "QUATRO", '5' to "CINCO")
    private val numberWordsEn = mapOf('1' to "ONE", '2' to "TWO", '3' to "THREE", '4' to "FOUR", '5' to "FIVE")

    fun detectLanguage(locale: Locale = Locale.getDefault()): SupportedLanguage =
        if (locale.language.equals("pt", ignoreCase = true)) SupportedLanguage.PORTUGUESE else SupportedLanguage.ENGLISH

    fun translate(input: String, locale: Locale = Locale.getDefault()): Translation =
        translate(input, detectLanguage(locale))

    fun translate(input: String, language: SupportedLanguage): Translation {
        if (input.isEmpty()) return emptyTranslation(input, language)

        // Defesa contra colagem/digitação de texto arbitrariamente grande.
        // O limite é alto para o uso normal do tradutor, mas impede que uma
        // entrada maliciosa ou acidental gere dezenas de milhares de objetos
        // de UI/sílabas e pressione a memória do aparelho.
        val limitado = input.take(MAX_INPUT_LENGTH)
        val foiLimitado = input.length > MAX_INPUT_LENGTH

        val normalized = normalize(limitado, language)
        if (normalized.isBlank()) return emptyTranslation(input, language, normalized)

        val lines = mutableListOf<List<List<String>>>()
        val warnings = mutableListOf<String>()
        if (foiLimitado) warnings += "Entrada limitada a $MAX_INPUT_LENGTH caracteres para preservar o desempenho."
        val normalizedLines = normalized.split('\n')

        normalizedLines.forEach { line ->
            val lineWords = tokenizeWords(line)
            val translatedLine = lineWords.map { rawWord ->
                val wordTranslation = translateWordCached(rawWord, language)
                warnings += wordTranslation.warnings
                wordTranslation.syllables
            }
            lines += translatedLine
        }

        val glyphsAvailable = lines.any { line -> line.any { word -> word.any { OldRealmSyllableMap.glyphString(it) != null } } }
        val state = when {
            warnings.isEmpty() -> TranslationState.TRANSLATED
            glyphsAvailable -> TranslationState.DEGRADED
            else -> TranslationState.UNKNOWN
        }
        return Translation(input, normalized, lines, warnings, state, language)
    }

    fun clearCache() = synchronized(wordCacheLock) { wordCache.clear() }

    private fun emptyTranslation(input: String, language: SupportedLanguage, normalized: String = "") =
        Translation(input, normalized, emptyList(), emptyList(), TranslationState.TRANSLATED, language)

    private fun normalize(input: String, language: SupportedLanguage): String {
        val numberWords = if (language == SupportedLanguage.PORTUGUESE) numberWordsPt else numberWordsEn
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        val out = StringBuilder(nfd.length)
        var previousWasSpace = false
        for (c0 in nfd) {
            if (Character.getType(c0) == Character.NON_SPACING_MARK.toInt()) continue
            val c = when (c0) {
                'ç', 'Ç' -> 'C'
                '\r' -> continue
                else -> c0
            }
            when {
                c in 'a'..'z' || c in 'A'..'Z' -> {
                    out.append(c.uppercaseChar())
                    previousWasSpace = false
                }
                c in '1'..'5' -> {
                    if (out.isNotEmpty() && !previousWasSpace && out.last() != '\n') out.append(' ')
                    out.append(numberWords.getValue(c))
                    out.append(' ')
                    previousWasSpace = true
                }
                c == '\n' -> {
                    while (out.lastOrNull() == ' ') out.deleteCharAt(out.lastIndex)
                    out.append('\n')
                    previousWasSpace = false
                }
                c.isWhitespace() -> {
                    if (!previousWasSpace && out.lastOrNull() != '\n') {
                        out.append(' ')
                        previousWasSpace = true
                    }
                }
                else -> Unit // punctuation and unsupported symbols are intentionally ignored
            }
        }
        return collapseSpacesPerLine(out.toString()).trim('\n')
    }

    private fun collapseSpacesPerLine(value: String): String = value.lineSequence().joinToString("\n") { line ->
        val out = StringBuilder(line.length)
        var spaced = false
        for (c in line.trim()) {
            if (c == ' ') {
                if (!spaced) out.append(c)
                spaced = true
            } else {
                out.append(c)
                spaced = false
            }
        }
        out.toString()
    }

    private fun tokenizeWords(line: String): List<String> = line.split(' ').filter(String::isNotBlank)


    private fun translateWordCached(raw: String, language: SupportedLanguage): WordTranslation {
        val key = language.name + '\u0000' + raw
        synchronized(wordCacheLock) { wordCache[key]?.let { return it } }
        val warnings = ArrayList<String>(2)
        val result = WordTranslation(translateWord(raw, language, warnings), warnings.toList())
        synchronized(wordCacheLock) { wordCache[key] = result }
        return result
    }

    private fun translateWord(raw: String, language: SupportedLanguage, warnings: MutableList<String>): List<String> {
        officialExceptions[raw]?.let { return it }
        var s = raw
            .replace("PH", "F")
            .replace("SH", "CH")
            .replace("TH", "D")
            .replace("QU", "KW")
            .replace("NH", "N")
            .replace("LH", "L")
            .replace("RR", "R")
            .replace("SS", "S")
            .replace("CC", "C")
            .replace("VV", "V")

        if (language == SupportedLanguage.PORTUGUESE) {
            s = applyPortugueseRules(s)
        } else {
            s = applyEnglishRules(s)
        }

        s = s.replace('V', 'F').replace('Z', 'X').replace('J', 'G').replace('Q', 'K').replace('Y', 'W')
        return syllabify(s, warnings)
    }

    private fun applyPortugueseRules(s: String): String {
        val out = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            when (c) {
                'C' -> out.append(if (i + 1 < s.length && s[i + 1] in "EI") 'S' else 'K')
                'G' -> out.append(if (i + 1 < s.length && s[i + 1] in "EI") 'J' else 'G')
                else -> out.append(c)
            }
        }
        // S between vowels is voiced; Z already maps to Old Realm X.
        for (i in 1 until out.length - 1) {
            if (out[i] == 'S' && out[i - 1] in vowels && out[i + 1] in vowels) out.setCharAt(i, 'Z')
        }
        return out.toString()
    }

    private fun applyEnglishRules(s: String): String {
        val out = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            when (c) {
                'C' -> out.append(if (i + 1 < s.length && s[i + 1] in "EIY") 'S' else 'K')
                'G' -> out.append(if (i + 1 < s.length && s[i + 1] in "EIY") 'J' else 'G')
                else -> out.append(c)
            }
        }
        return out.toString()
    }

    private fun syllabify(s: String, warnings: MutableList<String>): List<String> {
        if (s.isBlank()) return emptyList()
        val result = ArrayList<String>(s.length)
        val pending = ArrayList<String>(4)
        var lastVowel = 'A'
        var i = 0

        fun emit(consonant: String?, vowel: Char) {
            val c = canonicalConsonant(consonant)
            val v = canonicalVowel(vowel)
            val key = (c ?: "") + v
            if (OldRealmSyllableMap.keys.contains(key)) {
                result += key
                return
            }
            val rescued = (c ?: "") + 'A'
            if (OldRealmSyllableMap.keys.contains(rescued)) {
                result += rescued
                warnings += "Resgate CV aplicado a $key"
            } else if (OldRealmSyllableMap.keys.contains(v.toString())) {
                result += v.toString()
                warnings += "Vogal isolada aplicada a $key"
            } else {
                warnings += "Sem glifo para $key"
            }
        }

        while (i < s.length) {
            val token: String
            if (i + 1 < s.length) {
                val pair = s.substring(i, i + 2)
                token = when (pair) {
                    "CH", "KW", "GW" -> pair
                    else -> s[i].toString()
                }
            } else token = s[i].toString()
            i += token.length

            if (token.length == 1 && token[0] in vowels) {
                when (pending.size) {
                    0 -> emit(null, token[0])
                    1 -> emit(pending[0], token[0])
                    else -> {
                        // Maximal onset: keep the final consonant with the current vowel;
                        // rescue earlier consonants against the preceding vowel.
                        val onset = pending.removeAt(pending.lastIndex)
                        for (consonant in pending) emit(consonant, lastVowel)
                        emit(onset, token[0])
                    }
                }
                pending.clear()
                lastVowel = token[0]
            } else {
                when (token) {
                    "KW" -> { pending += "K"; pending += "W" }
                    "GW" -> { pending += "G"; pending += "W" }
                    else -> pending += token
                }
            }
        }

        for (consonant in pending) emit(consonant, lastVowel)
        if (result.isEmpty() && s.isNotEmpty()) {
            warnings += "Nenhuma vogal encontrada; aplicada vogal A de suporte"
            for (c in s) if (c in consonants) emit(c.toString(), 'A')
        }
        return result
    }

    private fun canonicalConsonant(c: String?): String? = c?.let {
        when (it) { "C" -> "K"; "J" -> "G"; "Q" -> "K"; "V" -> "F"; "Z" -> "X"; "Y" -> "W"; else -> it }
    }
    private fun canonicalVowel(v: Char): Char = if (v in vowels) v else 'A'
}
