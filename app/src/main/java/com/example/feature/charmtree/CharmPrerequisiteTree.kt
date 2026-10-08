package com.example.feature.charmtree

/**
 * UI-independent data contract for the Charm prerequisite tree.
 *
 * [prerequisiteIds] contains IDs from the same catalog. [essenceMinimum] and
 * [abilityMinimum] drive the five-dot indicators shown by the UI.
 */
data class CharmTreeEntry(
    val id: String,
    val name: String,
    val description: String = "",
    val ability: String = "",
    val prerequisiteIds: List<String> = emptyList(),
    /** Texto de um pré-requisito explícito que não pôde ser resolvido no catálogo. */
    val unresolvedPrerequisiteText: String? = null,
    val essenceMinimum: Int = 0,
    val abilityMinimum: Int = 0
)

data class CharmTreeNode(
    val charm: CharmTreeEntry,
    val depth: Int,
    val children: List<CharmTreeNode>
)

data class CharmTreeResult(
    val root: CharmTreeNode,
    val missingPrerequisiteIds: Set<String> = emptySet(),
    val cycleDetected: Boolean = false,
    /** True when the defensive node-expansion limit was reached. */
    val expansionLimitReached: Boolean = false,
    /**
     * When the selected Charm has no prerequisite, this contains the complete
     * tree for its linked ability (or Lunar attribute), including prerequisite
     * ancestors needed to keep the tree visually connected. Otherwise it is
     * just the selected Charm root.
     */
    val displayRoots: List<CharmTreeNode> = listOf(root)
)


/** Builds the prerequisite graph rooted at [rootId]. */
object CharmPrerequisiteTreeBuilder {
    private const val MAX_EXPANDED_NODES = 10_000

    fun build(
        rootId: String,
        charms: List<CharmTreeEntry>,
        includeFullCatalog: Boolean = false
    ): CharmTreeResult? {
        val byId = HashMap<String, CharmTreeEntry>(charms.size)
        charms.forEach { charm ->
            val id = charm.id.trim()
            if (id.isNotEmpty() && id !in byId) {
                byId[id] = charm.copy(
                    id = id,
                    essenceMinimum = charm.essenceMinimum.coerceIn(0, 5),
                    abilityMinimum = charm.abilityMinimum.coerceIn(0, 5),
                    prerequisiteIds = charm.prerequisiteIds
                        .asSequence()
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .distinct()
                        .toList()
                )
            }
        }

        val requestedRootId = rootId.trim()
        val root = byId[requestedRootId]
            ?: byId.entries.firstOrNull { it.key.equals(requestedRootId, ignoreCase = true) }?.value
            ?: byId.values.firstOrNull { it.name.equals(requestedRootId, ignoreCase = true) }
            ?: return null
        val missing = linkedSetOf<String>()
        var cycleDetected = false
        var expansionLimitReached = false
        var expandedNodes = 0

        // A árvore completa é um DAG na maioria dos catálogos. Sem memoização,
        // um pré-requisito compartilhado era reconstruído integralmente para
        // cada raiz terminal, multiplicando trabalho e alocações na abertura.
        // Guardamos a subárvore em profundidade relativa zero e reaplicamos a
        // profundidade visual ao reutilizá-la. Caminhos que já contêm o nó não
        // usam o cache, preservando exatamente a detecção de ciclos.
        val subtreeCache = HashMap<String, CharmTreeNode>(byId.size)
        fun withDepth(node: CharmTreeNode, depth: Int): CharmTreeNode =
            CharmTreeNode(
                charm = node.charm,
                depth = depth,
                children = node.children.map { child -> withDepth(child, depth + 1) }
            )

        fun visit(charm: CharmTreeEntry, depth: Int, activePath: MutableSet<String>): CharmTreeNode {
            if (charm.id !in activePath) {
                subtreeCache[charm.id]?.let { cached -> return withDepth(cached, depth) }
            }
            if (expandedNodes >= MAX_EXPANDED_NODES) {
                expansionLimitReached = true
                return CharmTreeNode(charm, depth, emptyList())
            }
            expandedNodes++

            charm.unresolvedPrerequisiteText?.trim()?.takeIf { it.isNotBlank() }?.let {
                missing += it
            }

            if (charm.id in activePath) {
                cycleDetected = true
                return CharmTreeNode(charm, depth, emptyList())
            }

            activePath += charm.id
            val children = ArrayList<CharmTreeNode>(charm.prerequisiteIds.size)
            for (prerequisiteId in charm.prerequisiteIds) {
                val prerequisite = byId[prerequisiteId]
                if (prerequisite == null) {
                    missing += prerequisiteId
                } else {
                    children += visit(prerequisite, depth + 1, activePath)
                }
            }
            activePath.remove(charm.id)
            val built = CharmTreeNode(charm, depth, children)
            if (!cycleDetected) {
                subtreeCache[charm.id] = withDepth(built, 0)
            }
            return built
        }

        val primaryTree = visit(root, depth = 0, activePath = HashSet())

        val displayRoots = if (includeFullCatalog) {
            // Árvore completa da Habilidade/Atributo. Como as arestas deste
            // modelo apontam do Encanto para seus pré-requisitos, as raízes
            // visuais do catálogo completo são os Encantos que não são
            // pré-requisito de nenhum outro. A partir deles alcançamos todos
            // os ramos reais sem inventar ligações entre Encantos independentes.
            val prerequisiteIds = byId.values
                .asSequence()
                .flatMap { it.prerequisiteIds.asSequence() }
                .toSet()
            val terminalCharms = byId.values
                .filter { it.id !in prerequisiteIds }
                .sortedWith(compareBy<CharmTreeEntry>(
                    { it.essenceMinimum },
                    { it.abilityMinimum },
                    { it.name.lowercase() },
                    { it.id.lowercase() }
                ))

            // Catálogos malformados podem conter somente ciclos. Nesse caso,
            // mantém-se ao menos o ramo selecionado para a janela continuar
            // navegável e para o diagnóstico de ciclo permanecer disponível.
            terminalCharms.map { visit(it, depth = 0, activePath = HashSet()) }
                .ifEmpty { listOf(primaryTree) }
        } else {
            // Detalhes de um Encanto continuam exibindo apenas sua cadeia real.
            listOf(primaryTree)
        }

        return CharmTreeResult(
            root = primaryTree,
            missingPrerequisiteIds = missing,
            cycleDetected = cycleDetected,
            expansionLimitReached = expansionLimitReached,
            displayRoots = displayRoots
        )
    }
}
