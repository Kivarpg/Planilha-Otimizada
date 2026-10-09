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
        val targetId: String
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
            val normalizedText = normalize(text)
            if (normalizedText.isBlank()) return Resolution(emptyList())
            if (candidates.isEmpty()) {
                return if (isGenericRequirementNormalized(normalizedText)) Resolution(emptyList())
                else Resolution(emptyList(), text)
            }

            val matches = candidates
                .asSequence()
                // Nomes maiores que o texto não podem corresponder. Como os
                // candidatos estão ordenados por tamanho, interromper cedo.
                .dropWhile { candidate -> candidate.normalized.length > normalizedText.length }
                .flatMap { candidate ->
                    allOccurrences(normalizedText, candidate.normalized).mapNotNull { start ->
                        val end = start + candidate.normalized.length
                        if (isBoundaryMatch(normalizedText, start, end)) Match(start, end, candidate) else null
                    }
                }
                .sortedWith(compareBy<Match> { it.start }.thenByDescending { it.end - it.start })
                .toList()

            // A seleção de intervalos e a deduplicação dos IDs compartilham
            // uma única passagem. A ordem de ocorrência permanece intacta.
            val seenIds = HashSet<String>()
            val ids = ArrayList<String>()
            var selectedEnd = -1
            for (match in matches) {
                if (match.start < selectedEnd) continue
                selectedEnd = match.end
                if (seenIds.add(match.candidate.targetId)) {
                    ids.add(match.candidate.targetId)
                }
            }

            if (ids.isNotEmpty()) return Resolution(ids)
            return if (isGenericRequirementNormalized(normalizedText)) Resolution(emptyList())
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
        // Normalizar cada nome/ID apenas uma vez por construção do catálogo.
        data class NormalizedEntry(
            val id: String,
            val name: String,
            val normalizedId: String,
            val normalizedName: String
        )
        val entries = catalog.mapNotNull { entry ->
            val id = entry.id.trim()
            // Entradas sem ID não representam um nó endereçável na árvore.
            // Filtrar antes da normalização evita trabalho em registros inválidos.
            if (id.isBlank()) return@mapNotNull null
            val name = entry.name.trim()
            NormalizedEntry(id, name, normalize(id), normalize(name))
        }
        // Apenas a unicidade importa: dispensar listas intermediárias por nome.
        val idsByNormalizedName = HashMap<String, MutableSet<String>>()
        for (entry in entries) {
            idsByNormalizedName.getOrPut(entry.normalizedName) { HashSet() }.add(entry.id)
        }

        // Construir diretamente a lista final, sem listas temporárias por Encanto.
        val seenCandidates = HashSet<Pair<String, String>>()
        val candidates = ArrayList<Candidate>(entries.size * 2)
        fun addCandidate(normalized: String, id: String) {
            if (normalized.isNotBlank() && seenCandidates.add(normalized to id)) {
                candidates.add(Candidate(normalized, id))
            }
        }
        for (entry in entries) {
            if (entry.name.isNotBlank() && idsByNormalizedName[entry.normalizedName]?.size == 1) {
                addCandidate(entry.normalizedName, entry.id)
            }
            if (entry.normalizedId != entry.normalizedName) {
                addCandidate(entry.normalizedId, entry.id)
            }
        }
        return candidates.sortedByDescending { it.normalized.length }
    }

    private fun allOccurrences(text: String, needle: String): Sequence<Int> = sequence {
        if (needle.isBlank()) return@sequence
        var from = 0
        while (from <= text.length - needle.length) {
            val index = text.indexOf(needle, from)
            if (index < 0) break
            yield(index)
            from = index + 1
        }
    }

    private fun isBoundaryMatch(text: String, start: Int, end: Int): Boolean {
        fun isWord(c: Char): Boolean = c.isLetterOrDigit()
        val before = text.getOrNull(start - 1)
        val after = text.getOrNull(end)
        return (before == null || !isWord(before)) && (after == null || !isWord(after))
    }

    private fun isGenericRequirementNormalized(normalized: String): Boolean =
        GENERIC_MARKERS.any { marker -> normalized.contains(marker) }

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
