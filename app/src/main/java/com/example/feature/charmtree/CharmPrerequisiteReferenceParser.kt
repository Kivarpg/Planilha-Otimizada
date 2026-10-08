package com.example.feature.charmtree

import java.text.Normalizer

/**
 * Resolve referências de pré-requisitos escritas no JSON contra o catálogo
 * carregado, sem assumir que vírgula/`;`/`e` delimitam nomes.
 *
 * Isso é importante porque nomes de Encantos podem conter vírgulas. A fonte
 * de verdade continua sendo o campo `pre_requisitos` do JSON: o parser apenas
 * identifica, dentro desse texto, quais nomes/IDs realmente existem no mesmo
 * catálogo. Assim, novos JSONs não precisam obedecer a um separador específico.
 */
internal object CharmPrerequisiteReferenceParser {
    private data class Candidate(
        val normalized: String,
        val targetId: String,
        val sourceName: String
    )

    data class Resolution(
        val prerequisiteIds: List<String>,
        val unresolvedText: String? = null
    )

    /** Prepara os candidatos uma única vez para resolver muitos Encantos. */
    class Resolver internal constructor(catalog: List<CharmTreeEntry>) {
        private val candidates = buildCandidates(catalog)

        fun resolve(raw: String): Resolution {
            val text = raw.trim()
            if (text.isBlank() || text.equals("Nenhum", ignoreCase = true)) {
                return Resolution(emptyList())
            }
            if (candidates.isEmpty()) {
                return if (isGenericRequirement(text)) Resolution(emptyList())
                else Resolution(emptyList(), text)
            }

            val normalizedText = normalize(text)
            if (normalizedText.isBlank()) return Resolution(emptyList())

            val matches = candidates
                .asSequence()
                .flatMap { candidate ->
                    allOccurrences(normalizedText, candidate.normalized).asSequence().map { start ->
                        Match(start, start + candidate.normalized.length, candidate)
                    }
                }
                .sortedWith(compareBy<Match> { it.start }.thenByDescending { it.end - it.start })
                .toList()

            val selected = mutableListOf<Match>()
            for (match in matches) {
                if (!isBoundaryMatch(normalizedText, match.start, match.end)) continue
                if (selected.none { it.start < match.end && match.start < it.end }) {
                    selected += match
                }
            }

            val ids = selected
                .sortedBy { it.start }
                .map { it.candidate.targetId }
                .distinct()

            if (ids.isNotEmpty()) return Resolution(ids)
            return if (isGenericRequirement(text)) Resolution(emptyList())
            else Resolution(emptyList(), text)
        }
    }

    fun resolver(catalog: List<CharmTreeEntry>): Resolver = Resolver(catalog)

    fun resolve(raw: String, catalog: List<CharmTreeEntry>): Resolution =
        Resolver(catalog).resolve(raw)

    private data class Match(
        val start: Int,
        val end: Int,
        val candidate: Candidate
    )

    private fun buildCandidates(catalog: List<CharmTreeEntry>): List<Candidate> {
        val idsByNormalizedName = catalog
            .groupBy { normalize(it.name) }
            .mapValues { (_, entries) -> entries.map { it.id.trim() }.filter { it.isNotBlank() }.distinct() }

        return catalog.flatMap { entry ->
            val id = entry.id.trim()
            val name = entry.name.trim()
            buildList {
                if (name.isNotBlank() && idsByNormalizedName[normalize(name)]?.size == 1) {
                    add(Candidate(normalize(name), id, name))
                }
                if (id.isNotBlank() && !normalize(id).equals(normalize(name))) {
                    add(Candidate(normalize(id), id, id))
                }
            }
        }
            .filter { it.normalized.isNotBlank() }
            .distinctBy { it.normalized to it.targetId }
            .sortedByDescending { it.normalized.length }
    }

    private fun allOccurrences(text: String, needle: String): List<Int> {
        if (needle.isBlank()) return emptyList()
        val result = mutableListOf<Int>()
        var from = 0
        while (from <= text.length - needle.length) {
            val index = text.indexOf(needle, from)
            if (index < 0) break
            result += index
            from = index + 1
        }
        return result
    }

    private fun isBoundaryMatch(text: String, start: Int, end: Int): Boolean {
        fun isWord(c: Char): Boolean = c.isLetterOrDigit()
        val before = text.getOrNull(start - 1)
        val after = text.getOrNull(end)
        return (before == null || !isWord(before)) && (after == null || !isWord(after))
    }

    private fun isGenericRequirement(text: String): Boolean {
        val normalized = normalize(text)
        return GENERIC_MARKERS.any { marker -> normalized.contains(marker) }
    }

    private val GENERIC_MARKERS = listOf(
        "qualquer ",
        "quaisquer ",
        "any ",
        "at least ",
        "pelo menos "
    )

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .replace('’', '\'')
            .replace('–', '-')
            .replace('—', '-')
            .lowercase()
            .replace(WHITESPACE_REGEX, " ")
            .trim()

    private val DIACRITICS_REGEX = Regex("\\p{M}+")
    private val WHITESPACE_REGEX = Regex("\\s+")
}
