package com.example.oldrealm.highrealm

/** Packs whole words top-to-bottom; a word is never split across columns. */
object HighRealmLayout {
    const val WORD_GAP_ROWS = 1

    data class Column(val words: List<List<String>>)

    fun pack(words: List<List<String>>, maxRows: Int): List<Column> {
        // Defesa contra dimensões inválidas vindas da UI/configuração.
        // Um valor não positivo não deve derrubar a tela do tradutor.
        if (maxRows <= 0) return emptyList()
        val columns = mutableListOf<Column>()
        var current = mutableListOf<List<String>>()
        var usedRows = 0

        fun flush() {
            if (current.isNotEmpty()) columns += Column(current.toList())
            current = mutableListOf()
            usedRows = 0
        }

        words.forEach { glyphs ->
            if (glyphs.isEmpty()) return@forEach
            val gap = if (current.isEmpty()) 0 else WORD_GAP_ROWS
            if (current.isNotEmpty() && usedRows + gap + glyphs.size > maxRows) flush()
            current += glyphs
            usedRows += (if (current.size == 1) 0 else WORD_GAP_ROWS) + glyphs.size
        }
        flush()
        return columns
    }
}
