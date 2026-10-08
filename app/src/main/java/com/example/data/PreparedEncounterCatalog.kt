package com.example.data

import com.example.model.TipoExaltadoEncontro
import java.text.Normalizer

/**
 * Etapa 5 — catálogo canônico preparado para construção de NPCs.
 *
 * Esta camada NÃO substitui os modelos de conteúdo existentes. Ela valida e
 * indexa suas identidades uma única vez e oferece uma porta de entrada comum
 * para RulesEngine/ECS/Planner. Assim a migração pode ser incremental sem
 * alterar o resultado das builds existentes.
 */
internal class PreparedEncounterCatalog private constructor(
    val solares: List<EncantoSolarDefinition>,
    val sangueDeDragao: List<EncantoSangueDeDragaoDefinition>,
    val lunares: List<EncantoLunarDefinition>,
    val feiticos: List<FeiticoDefinition>,
    val estilosMarciais: List<EstiloArteMarcialDefinition>
) {
    data class StableContentId(val namespace: String, val localId: String) {
        init {
            require(namespace.isNotBlank()) { "namespace vazio" }
            require(localId.isNotBlank()) { "localId vazio" }
        }
        val value: String = "$namespace:$localId"
        override fun toString(): String = value
    }

    enum class ContentKind { CHARM, SPELL, MARTIAL_STYLE, MARTIAL_CHARM }

    data class ContentRef(
        val id: StableContentId,
        val kind: ContentKind,
        val nome: String,
        val exaltType: TipoExaltadoEncontro? = null,
        val parentId: StableContentId? = null
    )

    private val allRefs: List<ContentRef> = buildList {
        solares.forEach {
            add(ContentRef(StableContentId(NS_SOLAR, it.id), ContentKind.CHARM, it.nome, TipoExaltadoEncontro.SOLAR))
        }
        sangueDeDragao.forEach {
            add(ContentRef(StableContentId(NS_DRAGON_BLOODED, it.id), ContentKind.CHARM, it.nome, TipoExaltadoEncontro.SANGUE_DE_DRAGAO))
        }
        lunares.forEach {
            add(ContentRef(StableContentId(NS_LUNAR, it.id), ContentKind.CHARM, it.nome, TipoExaltadoEncontro.LUNAR))
        }
        feiticos.forEach {
            add(ContentRef(StableContentId(NS_SPELL, it.id), ContentKind.SPELL, it.nome))
        }
        estilosMarciais.forEach { estilo ->
            val styleId = StableContentId(NS_MARTIAL_STYLE, estilo.id)
            add(ContentRef(styleId, ContentKind.MARTIAL_STYLE, estilo.nomePt))
            estilo.encantos.forEach { encanto ->
                add(
                    ContentRef(
                        StableContentId(NS_MARTIAL_CHARM, encanto.id),
                        ContentKind.MARTIAL_CHARM,
                        encanto.nome,
                        parentId = styleId
                    )
                )
            }
        }
    }

    /** Índice global seguro: namespaces impedem colisões entre catálogos. */
    val byId: Map<StableContentId, ContentRef> = uniqueIndex(allRefs, "ID") { it.id }

    /** Índice ordinal determinístico para estados compactos; a ordem deriva do ID canônico. */
    val compactSelectionIndex: EncounterEngineeringBaseline.CompactSelectionIndex by lazy(LazyThreadSafetyMode.PUBLICATION) {
        EncounterEngineeringBaseline.CompactSelectionIndex(byId.keys)
    }

    /** Nome é índice de conveniência, nunca identidade. Pode retornar >1 item. */
    val byNormalizedName: Map<String, List<ContentRef>> =
        allRefs.groupBy { normalize(it.nome) }

    val charmsByExaltType: Map<TipoExaltadoEncontro, List<ContentRef>> =
        allRefs.asSequence()
            .filter { it.kind == ContentKind.CHARM && it.exaltType != null }
            .groupBy { requireNotNull(it.exaltType) }

    val martialCharmsByStyle: Map<StableContentId, List<ContentRef>> =
        allRefs.asSequence()
            .filter { it.kind == ContentKind.MARTIAL_CHARM && it.parentId != null }
            .groupBy { requireNotNull(it.parentId) }

    /** Compilado uma única vez e compartilhado por todos os consumidores. */
    val requirementGraph: EncounterRequirementGraph by lazy(LazyThreadSafetyMode.PUBLICATION) {
        EncounterRequirementGraph.compile(this)
    }

    fun find(id: StableContentId): ContentRef? = byId[id]

    fun findByName(nome: String): List<ContentRef> =
        byNormalizedName[normalize(nome)].orEmpty()

    /**
     * Pré-requisitos de Encantos resolvidos para IDs canônicos quando o nome
     * é inequívoco dentro do mesmo Tipo de Exaltado. Requisitos agregados
     * (contagem, mínimos, traits) permanecem no RulesEngine.
     */
    val prerequisiteIdsByCharm: Map<StableContentId, Set<StableContentId>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        requirementGraph.nodes.mapValues { (id, node) ->
            node.routes.asSequence()
                .flatMap { it.requirements.asSequence() }
                .filterIsInstance<EncounterRequirement.Charm>()
                .mapNotNull { req ->
                    findByName(req.name)
                        .filter { it.id.namespace == id.namespace }
                        .singleOrNull()
                        ?.id
                }
                .toSet()
        }
    }

    fun prerequisitesOf(id: StableContentId): Set<StableContentId> =
        prerequisiteIdsByCharm[id].orEmpty()

    /** Índice reverso canônico: quais conteúdos citam este ID como pré-requisito direto. */
    val directDependentsByPrerequisite: Map<StableContentId, Set<StableContentId>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val reverse = linkedMapOf<StableContentId, MutableSet<StableContentId>>()
        prerequisiteIdsByCharm.forEach { (child, prerequisites) ->
            prerequisites.forEach { prerequisite ->
                reverse.getOrPut(prerequisite) { linkedSetOf() } += child
            }
        }
        reverse.mapValues { (_, dependents) -> dependents.toSet() }
    }

    fun directDependentsOf(id: StableContentId): Set<StableContentId> =
        directDependentsByPrerequisite[id].orEmpty()

    /**
     * Resolve um ID local legado somente dentro de um namespace explícito.
     * Impede o antigo padrão perigoso "procure esse texto em qualquer lista".
     */
    fun resolveLegacyId(namespace: String, localId: String): ContentRef? =
        find(StableContentId(namespace, localId))

    companion object {
        const val NS_SOLAR = "solar.charm"
        const val NS_DRAGON_BLOODED = "dragon_blooded.charm"
        const val NS_LUNAR = "lunar.charm"
        const val NS_SPELL = "spell"
        const val NS_MARTIAL_STYLE = "martial.style"
        const val NS_MARTIAL_CHARM = "martial.charm"

        fun prepare(
            solares: List<EncantoSolarDefinition> = emptyList(),
            sangueDeDragao: List<EncantoSangueDeDragaoDefinition> = emptyList(),
            lunares: List<EncantoLunarDefinition> = emptyList(),
            feiticos: List<FeiticoDefinition> = emptyList(),
            estilosMarciais: List<EstiloArteMarcialDefinition> = emptyList()
        ): PreparedEncounterCatalog {
            validateLocalIds(NS_SOLAR, solares.map { it.id })
            validateLocalIds(NS_DRAGON_BLOODED, sangueDeDragao.map { it.id })
            validateLocalIds(NS_LUNAR, lunares.map { it.id })
            validateLocalIds(NS_SPELL, feiticos.map { it.id })
            validateLocalIds(NS_MARTIAL_STYLE, estilosMarciais.map { it.id })
            validateLocalIds(NS_MARTIAL_CHARM, estilosMarciais.flatMap { e -> e.encantos.map { it.id } })

            // Um Charm MA deve pertencer ao estilo que o contém. Detectar
            // inconsistência cedo evita árvore/roadmap silenciosamente corrompidos.
            estilosMarciais.forEach { estilo ->
                estilo.encantos.forEach { encanto ->
                    require(encanto.estiloId == estilo.id) {
                        "Encanto MA ${encanto.id} aponta para ${encanto.estiloId}, mas está em ${estilo.id}"
                    }
                }
            }
            return PreparedEncounterCatalog(solares, sangueDeDragao, lunares, feiticos, estilosMarciais)
        }

        private fun validateLocalIds(namespace: String, ids: List<String>) {
            require(ids.none { it.isBlank() }) { "ID vazio em $namespace" }
            val duplicate = ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
            require(duplicate == null) { "ID duplicado em $namespace: ${duplicate?.key}" }
        }

        private fun <K> uniqueIndex(
            refs: List<ContentRef>,
            label: String,
            key: (ContentRef) -> K
        ): Map<K, ContentRef> {
            val result = LinkedHashMap<K, ContentRef>(refs.size)
            refs.forEach { ref ->
                val k = key(ref)
                require(result.put(k, ref) == null) { "$label canônico duplicado: $k" }
            }
            return result
        }

        internal fun normalize(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .lowercase()
                .trim()
                .replace(Regex("\\s+"), " ")
    }
}
