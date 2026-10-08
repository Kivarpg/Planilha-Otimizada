package com.example.oldrealm.highrealm

import java.text.Normalizer

/** Deterministic, offline High Realm transliterator based strictly on the supplied mapping. */
object HighRealmTranslator {
    data class Translation(
        val originalInput: String,
        val normalizedInput: String,
        val lines: List<List<List<String>>>, // logical lines -> words -> glyph codepoints
        val encoded: String,
        val unknownCount: Int
    )

    // Longest-match-first is mandatory: 3, then 2, then 1 character units.
    private val mapping: Map<String, String> = mapOf(
        "cha" to "G", "che" to "H", "chi" to "I", "cho" to "J", "chu" to "K",
        "ba" to "B", "be" to "C", "bi" to "D", "bo" to "E", "bu" to "F",
        "da" to "L", "de" to "M", "di" to "N", "do" to "O", "du" to "P",
        "fa" to "R", "fe" to "S", "fi" to "T", "fo" to "U", "fu" to "V",
        "ga" to "W", "ge" to "X", "gi" to "Y", "go" to "Z", "gu" to "a",
        "ha" to "b", "he" to "c", "hi" to "d", "ho" to "e", "hu" to "f",
        "ka" to "h", "ke" to "i", "ki" to "j", "ko" to "k", "ku" to "l",
        "la" to "m", "le" to "n", "li" to "o", "lo" to "p", "lu" to "q",
        "ma" to "r", "me" to "s", "mi" to "t", "mo" to "u", "mu" to "v",
        "na" to "w", "ne" to "x", "ni" to "y", "no" to "z", "nu" to "Á",
        "pa" to "À", "pe" to "à", "pi" to "Â", "po" to "â", "pu" to "Ä",
        "ra" to "ä", "re" to "Ç", "ri" to "ç", "ro" to "É", "ru" to "é",
        "sa" to "È", "se" to "è", "si" to "Ê", "so" to "ê", "su" to "Ë",
        "ta" to "ë", "te" to "Ú", "ti" to "ú", "to" to "Ù", "tu" to "ù",
        "wa" to "û", "we" to "Ü", "wi" to "ü", "wo" to "Î", "wu" to "î",
        "xa" to "Ï", "xe" to "ï", "xi" to "Ó", "xo" to "ó", "xu" to "Ô",
        "ya" to "ô", "ye" to "Ö", "yi" to "ö", "yo" to "Ñ", "yu" to "ß",
        "a" to "A", "e" to "Q", "i" to "g", "o" to "á", "u" to "Û",
        "1" to "1", "2" to "2", "3" to "3", "4" to "4", "5" to "5",
        "6" to "6", "7" to "7", "8" to "8", "9" to "9", "0" to "0"
    )

    private const val MAX_TOKEN_LENGTH = 3
    private const val MAX_INPUT_LENGTH = 20_000

    fun translate(input: String): Translation {
        val limitado = input.take(MAX_INPUT_LENGTH)
        val normalized = normalize(limitado)
        if (normalized.isEmpty()) return Translation(input, normalized, emptyList(), "", 0)

        var unknown = 0
        val lines = normalized.split('\n').map { line ->
            line.split(' ').filter(String::isNotBlank).mapNotNull { word ->
                val glyphs = ArrayList<String>(word.length)
                var i = 0
                while (i < word.length) {
                    var matched: String? = null
                    var consumed = 0
                    for (length in MAX_TOKEN_LENGTH downTo 1) {
                        if (i + length <= word.length) {
                            val candidate = word.substring(i, i + length)
                            val glyph = mapping[candidate]
                            if (glyph != null) {
                                matched = glyph
                                consumed = length
                                break
                            }
                        }
                    }
                    if (matched != null) {
                        glyphs += matched
                        i += consumed
                    } else {
                        // Unknown source units are never emitted as ordinary text.
                        // The diagnostic count records the degradation without leaking
                        // Latin characters into the High Realm renderer.
                        unknown++
                        i++
                    }
                }
                glyphs.takeIf { it.isNotEmpty() }
            }.toList()
        }

        val encoded = buildString {
            lines.forEachIndexed { lineIndex, line ->
                if (lineIndex > 0) append('\n')
                line.forEachIndexed { wordIndex, word ->
                    if (wordIndex > 0) append(' ')
                    word.forEach(::append)
                }
            }
        }
        return Translation(input, normalized, lines, encoded, unknown)
    }

    private fun normalize(input: String): String {
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        val out = StringBuilder(nfd.length)
        var space = false
        for (raw in nfd) {
            if (Character.getType(raw) == Character.NON_SPACING_MARK.toInt()) continue
            when (raw) {
                '\r' -> Unit
                '\n' -> {
                    while (out.lastOrNull() == ' ') out.deleteCharAt(out.lastIndex)
                    out.append('\n')
                    space = false
                }
                else -> when {
                    raw.isLetterOrDigit() -> {
                        out.append(raw.lowercaseChar())
                        space = false
                    }
                    raw.isWhitespace() -> {
                        if (!space && out.lastOrNull() != '\n') {
                            out.append(' ')
                            space = true
                        }
                    }
                    else -> Unit // High Realm specification does not define punctuation.
                }
            }
        }
        while (out.lastOrNull() == ' ' || out.lastOrNull() == '\n') out.deleteCharAt(out.lastIndex)
        return out.toString()
    }
}
