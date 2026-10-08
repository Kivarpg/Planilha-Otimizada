package com.example.data

import com.example.model.Aspecto

/**
 * Regra exclusiva dos Sangue de Dragão para Encantos com etiqueta Assinatura.
 * Uma Assinatura por Habilidade normalmente; em Essência 5, uma segunda pode
 * ser adquirida nas Habilidades de Aspecto/Favorecidas. Se a primeira não for
 * do Aspecto do Exaltado, a segunda deve sê-lo.
 */
internal object DragonBloodedSignaturePolicy {
    private val signatureRegex = Regex("""(?i)\bAssinatura\s*\((Ar|Terra|Fogo|Água|Agua|Madeira)\)""")
    private val signatureElementCache = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<EncantoSolarDefinition, String?>()
    )
    private val catalogByNameCache = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<List<EncantoSolarDefinition>, Map<String, EncantoSolarDefinition>>()
    )
    private data class PreparedSignatureCandidate(
        val definition: EncantoSolarDefinition,
        val abilityNormalized: String,
        val element: String?
    )
    private val preparedSignatureCatalogCache = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<List<EncantoSolarDefinition>, List<PreparedSignatureCandidate>>()
    )

    private fun catalogByName(catalog: List<EncantoSolarDefinition>): Map<String, EncantoSolarDefinition> =
        synchronized(catalogByNameCache) {
            catalogByNameCache[catalog] ?: catalog.associateBy { it.nome }
                .also { catalogByNameCache[catalog] = it }
        }

    private fun signatureElement(def: EncantoSolarDefinition): String? = synchronized(signatureElementCache) {
        if (signatureElementCache.containsKey(def)) {
            signatureElementCache[def]
        } else {
            signatureRegex.find(def.palavrasChave)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::normalizeElement)
                .also { signatureElementCache[def] = it }
        }
    }

    private fun preparedSignatureCatalog(
        catalog: List<EncantoSolarDefinition>
    ): List<PreparedSignatureCandidate> = synchronized(preparedSignatureCatalogCache) {
        preparedSignatureCatalogCache[catalog] ?: catalog.map { definition ->
            PreparedSignatureCandidate(
                definition = definition,
                abilityNormalized = definition.habilidade.lowercase(),
                element = signatureElement(definition)
            )
        }.also { preparedSignatureCatalogCache[catalog] = it }
    }

    fun isSignature(def: EncantoSolarDefinition): Boolean = signatureElement(def) != null

    fun element(def: EncantoSolarDefinition): String? = signatureElement(def)

    /**
     * Projeção mínima do estado que pode alterar o filtro de progressão.
     * Encantos comuns adquiridos não modificam a política de Assinatura e,
     * portanto, não devem invalidar o catálogo preparado.
     */
    internal fun selectedSignatureNames(
        catalog: List<EncantoSolarDefinition>,
        selectedNames: Set<String>
    ): Set<String> {
        val byName = catalogByName(catalog)
        return selectedNames.asSequence()
            .mapNotNull(byName::get)
            .filter(::isSignature)
            .mapTo(LinkedHashSet()) { it.nome }
    }

    private fun normalizeElement(value: String): String =
        when (value.lowercase()) {
            "ar" -> "Ar"
            "terra" -> "Terra"
            "fogo" -> "Fogo"
            "água", "agua" -> "Água"
            "madeira" -> "Madeira"
            else -> value
        }

    fun abilityEligibleForSecond(habilidade: String, aspecto: String, favorecidas: Collection<String>): Boolean {
        val aspectoEnum = Aspecto.entries.firstOrNull { it.displayName.equals(aspecto, ignoreCase = true) }
        return favorecidas.any { it.equals(habilidade, ignoreCase = true) } ||
            aspectoEnum?.allowedAbilities()?.any { it.equals(habilidade, ignoreCase = true) } == true
    }

    fun canSelect(
        candidate: EncantoSolarDefinition,
        selectedNames: Set<String>,
        catalog: List<EncantoSolarDefinition>,
        essence: Int,
        aspecto: String,
        favorecidas: Collection<String>
    ): Boolean {
        if (!isSignature(candidate)) return true
        // A seleção possuída costuma ser muito menor que o catálogo completo.
        // Resolva somente esses nomes pelo índice já preparado em vez de varrer
        // todos os Encantos para descobrir quais deles estão selecionados.
        val byName = catalogByName(catalog)
        val selected = selectedNames.asSequence()
            .mapNotNull(byName::get)
            .filter { isSignature(it) && it.habilidade.equals(candidate.habilidade, ignoreCase = true) }
            .toList()
        if (selected.isEmpty()) return true
        if (selected.size >= 2 || essence < 5 || !abilityEligibleForSecond(candidate.habilidade, aspecto, favorecidas)) return false

        val ownElement = normalizeElement(aspecto)
        val alreadyHasOwn = selected.any { element(it)?.equals(ownElement, ignoreCase = true) == true }
        return alreadyHasOwn || element(candidate)?.equals(ownElement, ignoreCase = true) == true
    }

    fun filterForProgression(
        catalog: List<EncantoSolarDefinition>,
        selectedNames: Set<String>,
        essence: Int,
        aspecto: String,
        favorecidas: Collection<String>,
        favoredNormalizedPrepared: Set<String>? = null,
        aspectAbilitiesNormalizedPrepared: Set<String>? = null
    ): List<EncantoSolarDefinition> {
        // Caminho quente do +XP: indexa as Assinaturas já possuídas uma vez.
        // Evita revarrer o catálogo inteiro para cada candidato.
        val byName = catalogByName(catalog)
        val selectedSignaturesByAbility = HashMap<String, MutableList<EncantoSolarDefinition>>()
        selectedNames.forEach { nome ->
            val selected = byName[nome] ?: return@forEach
            if (!isSignature(selected)) return@forEach
            selectedSignaturesByAbility
                .getOrPut(selected.habilidade.lowercase()) { mutableListOf() }
                .add(selected)
        }
        val ownElement = normalizeElement(aspecto)
        val favoredNormalized = favoredNormalizedPrepared
            ?: favorecidas.asSequence().map { it.lowercase() }.toHashSet()
        val aspectAbilitiesNormalized = aspectAbilitiesNormalizedPrepared
            ?: Aspecto.entries
                .firstOrNull { it.displayName.equals(aspecto, ignoreCase = true) }
                ?.allowedAbilities()
                .orEmpty()
                .asSequence()
                .map { it.lowercase() }
                .toHashSet()
        return preparedSignatureCatalog(catalog).mapNotNull { prepared ->
            val candidate = prepared.definition
            if (prepared.element == null) return@mapNotNull candidate
            val habilidadeNormalizada = prepared.abilityNormalized
            val selected = selectedSignaturesByAbility[habilidadeNormalizada].orEmpty()
            val elegivel = when {
                selected.isEmpty() -> true
                selected.size >= 2 -> false
                essence < 5 -> false
                habilidadeNormalizada !in favoredNormalized &&
                    habilidadeNormalizada !in aspectAbilitiesNormalized -> false
                selected.any { element(it)?.equals(ownElement, ignoreCase = true) == true } -> true
                else -> prepared.element.equals(ownElement, ignoreCase = true)
            }
            candidate.takeIf { elegivel }
        }
    }
}
