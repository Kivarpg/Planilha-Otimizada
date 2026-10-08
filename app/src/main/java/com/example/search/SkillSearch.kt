package com.example.search

import com.example.data.EncantoSolarDefinition
import com.example.data.FeiticoDefinition

// Representação normalizada e agnóstica de categoria usada pelo motor de
// busca. Qualquer catálogo (Encantos, Feitiços, Necromancia, Artes
// Marciais) pode ser adaptado para este formato sem que o motor precise
// mudar — basta escrever uma função de conversão para o novo catálogo.
// Campos não aplicáveis à categoria (ex.: Mins/Essência para Feitiços)
// ficam nulos e são ignorados pelo filtro correspondente.
data class SearchableSkill(
    val id: String,
    val namePt: String,
    val nameEn: String,
    val habilidade: String?,
    val minHabilidade: Int?,
    val essencia: Int?,
    val tipo: String?,
    val keywords: List<String>,
    val duracao: String
) {
    // Pré-computado uma vez na criação, não a cada tecla digitada na busca
    // — namePt/nameEn não mudam depois de criado, então recalcular o
    // normalize() (NFD + regex) em cada keystroke pros ~750+ itens do
    // catálogo era trabalho redundante.
    val namePtNormalized: String by lazy { SkillSearchEngine.normalize(namePt) }
    val nameEnNormalized: String by lazy { SkillSearchEngine.normalize(nameEn) }
}

enum class NumericMatchMode { EXACT, AT_LEAST }
enum class KeywordMode { ANY, ALL }

data class SkillFilter(
    val query: String = "",
    val selectedSkillIds: Set<String> = emptySet(),
    val minHabilidade: Int? = null,
    val minHabilidadeMode: NumericMatchMode = NumericMatchMode.AT_LEAST,
    val essencia: Int? = null,
    val essenciaMode: NumericMatchMode = NumericMatchMode.AT_LEAST,
    val types: Set<String> = emptySet(),
    val keywords: Set<String> = emptySet(),
    val keywordMode: KeywordMode = KeywordMode.ANY,
    val durations: Set<String> = emptySet()
) {
    // Quantidade de filtros ativos — usada no badge do botão "Busca".
    val activeCount: Int
        get() {
            var n = 0
            if (query.isNotBlank()) n++
            if (selectedSkillIds.isNotEmpty()) n++
            if (minHabilidade != null) n++
            if (essencia != null) n++
            if (types.isNotEmpty()) n++
            if (keywords.isNotEmpty()) n++
            if (durations.isNotEmpty()) n++
            return n
        }

    val isEmpty: Boolean get() = activeCount == 0
}

// Motor de busca puro, independente da interface: recebe uma lista já
// carregada em memória e o filtro atual, devolve a lista filtrada.
// Critérios de grupos diferentes usam lógica E; valores dentro do mesmo
// grupo usam lógica OU (exceto Palavras-chave, que respeita keywordMode).
object SkillSearchEngine {

    // Delega para EncantosSolaresCatalog.normalize (mesma lógica: NFD + remoção
    // de diacríticos + minúsculas + trim) — evita duplicar o Regex de
    // diacríticos e a normalização Unicode em dois lugares do código.
    fun normalize(text: String): String =
        com.example.data.EncantosSolaresCatalog.normalize(text)

    fun filterSkills(skills: List<SearchableSkill>, filter: SkillFilter): List<SearchableSkill> {
        if (filter.isEmpty) return skills
        val queryNormalized = normalize(filter.query)
        return skills.filter { skill -> matches(skill, filter, queryNormalized) }
    }

    private fun matches(skill: SearchableSkill, filter: SkillFilter, queryNormalized: String): Boolean {
        if (queryNormalized.isNotBlank()) {
            val matchPt = skill.namePtNormalized.contains(queryNormalized)
            val matchEn = skill.nameEnNormalized.contains(queryNormalized)
            if (!matchPt && !matchEn) return false
        }
        if (filter.selectedSkillIds.isNotEmpty()) {
            if (skill.habilidade == null || skill.habilidade !in filter.selectedSkillIds) return false
        }
        if (filter.minHabilidade != null && skill.minHabilidade != null) {
            if (!matchNumeric(skill.minHabilidade, filter.minHabilidade, filter.minHabilidadeMode)) return false
        }
        if (filter.essencia != null && skill.essencia != null) {
            if (!matchNumeric(skill.essencia, filter.essencia, filter.essenciaMode)) return false
        }
        if (filter.types.isNotEmpty()) {
            if (skill.tipo == null || skill.tipo !in filter.types) return false
        }
        if (filter.keywords.isNotEmpty()) {
            if (!matchKeywords(skill.keywords, filter.keywords, filter.keywordMode)) return false
        }
        if (filter.durations.isNotEmpty()) {
            if (skill.duracao !in filter.durations) return false
        }
        return true
    }

    private fun matchNumeric(value: Int, target: Int, mode: NumericMatchMode): Boolean =
        if (mode == NumericMatchMode.EXACT) value == target else value >= target

    private fun matchKeywords(skillKeywords: List<String>, selected: Set<String>, mode: KeywordMode): Boolean {
        if (skillKeywords.isEmpty()) return false
        val skillSet = skillKeywords.toSet()
        return if (mode == KeywordMode.ANY) selected.any { it in skillSet } else selected.all { it in skillSet }
    }
}

// --- Adaptadores por categoria: convertem cada catálogo para o formato
// agnóstico do motor, sem que o motor precise conhecer a estrutura de
// nenhuma categoria específica. Uma nova categoria (Necromancia, Artes
// Marciais) só precisa de um adaptador equivalente a estes.

fun splitKeywords(texto: String): List<String> =
    texto.split(",", ";").map { it.trim() }.filter { it.isNotBlank() && !SkillSearchEngine.normalize(it).let { n -> n == "nenhuma" || n == "nenhum" } }

fun EncantoSolarDefinition.toSearchableSkill(): SearchableSkill = SearchableSkill(
    id = id,
    namePt = nome,
    nameEn = nomeIngles,
    habilidade = habilidade,
    minHabilidade = minHabilidade,
    essencia = minEssencia,
    tipo = tipo.trim().ifBlank { null },
    keywords = splitKeywords(palavrasChave),
    duracao = duracao.trim()
)

fun FeiticoDefinition.toSearchableSkill(): SearchableSkill = SearchableSkill(
    id = id,
    namePt = nome,
    nameEn = nomeIngles,
    habilidade = circulo.trim().ifBlank { null },
    minHabilidade = null,
    essencia = null,
    tipo = null,
    keywords = splitKeywords(palavrasChave),
    duracao = duracao.trim()
)
