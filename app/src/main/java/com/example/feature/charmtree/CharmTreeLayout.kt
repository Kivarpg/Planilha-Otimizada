package com.example.feature.charmtree

/** A visual node: one card per Charm id, even when the DAG reaches it through several branches. */
internal data class CharmTreeLayoutNode(
    val node: CharmTreeNode,
    val key: String
)

/**
 * Creates a compact DAG layout for the prerequisite graph.
 *
 * Shared prerequisites are deliberately rendered once. The longest path from
 * the selected Charm is used as the vertical depth, keeping a shared
 * prerequisite above every dependent branch. Nodes on each level are then
 * ordered by a small barycentric pass to reduce crossings.
 */
internal fun buildCharmTreeLevels(roots: List<CharmTreeNode>): List<List<CharmTreeLayoutNode>> {
    if (roots.isEmpty()) return emptyList()

    val byId = linkedMapOf<String, CharmTreeNode>()
    val longestDepth = mutableMapOf<String, Int>()

    fun visit(node: CharmTreeNode, depth: Int, path: Set<String>) {
        if (node.charm.id in path) return
        byId.putIfAbsent(node.charm.id, node)
        if (depth > (longestDepth[node.charm.id] ?: Int.MIN_VALUE)) {
            longestDepth[node.charm.id] = depth
        }
        val nextPath = path + node.charm.id
        node.children.forEach { child -> visit(child, depth + 1, nextPath) }
    }

    roots.forEach { visit(it, 0, emptySet()) }

    val levels = mutableListOf<MutableList<CharmTreeLayoutNode>>()
    byId.values.forEach { node ->
        val depth = longestDepth[node.charm.id] ?: 0
        while (levels.size <= depth) levels.add(mutableListOf())
        levels[depth] += CharmTreeLayoutNode(node, node.charm.id)
    }

    repeat(3) {
        for (depth in 1 until levels.size) {
            val previousCenters = levels[depth - 1]
                .mapIndexed { index, item -> item.key to index.toFloat() }
                .toMap()
            val current = levels[depth]
            val fallbackOrder = current.mapIndexed { index, node -> node.key to index.toFloat() }.toMap()
            current.sortBy { layoutNode ->
                val neighbours = layoutNode.node.children.mapNotNull { previousCenters[it.charm.id] }
                neighbours.averageOrNull() ?: fallbackOrder.getValue(layoutNode.key)
            }
        }
    }

    return levels.asReversed()
}


/**
 * Layout semântico usado pelo catálogo completo da Aba 8.
 *
 * As arestas de [CharmTreeNode] apontam para pré-requisitos. Portanto todo
 * Encanto sem pré-requisito ocupa o nível visual 0; um dependente ocupa um
 * nível abaixo do pré-requisito mais profundo. Raízes independentes com o
 * mesmo requisito permanecem lado a lado, independentemente do tamanho de
 * seus ramos descendentes.
 */
internal fun buildCharmTreeLevelsByPrerequisiteDepth(
    roots: List<CharmTreeNode>
): List<List<CharmTreeLayoutNode>> {
    if (roots.isEmpty()) return emptyList()

    val byId = linkedMapOf<String, CharmTreeNode>()
    fun collect(node: CharmTreeNode, path: Set<String>) {
        if (node.charm.id in path) return
        byId.putIfAbsent(node.charm.id, node)
        val nextPath = path + node.charm.id
        node.children.forEach { collect(it, nextPath) }
    }
    roots.forEach { collect(it, emptySet()) }

    val memo = mutableMapOf<String, Int>()
    fun prerequisiteDepth(node: CharmTreeNode, path: Set<String>): Int {
        memo[node.charm.id]?.let { return it }
        if (node.charm.id in path) return 0
        val nextPath = path + node.charm.id
        val depth = if (node.children.isEmpty()) 0 else {
            1 + (node.children.maxOfOrNull { prerequisiteDepth(it, nextPath) } ?: 0)
        }
        memo[node.charm.id] = depth
        return depth
    }

    val levels = mutableListOf<MutableList<CharmTreeLayoutNode>>()
    byId.values.forEach { node ->
        val depth = prerequisiteDepth(node, emptySet())
        while (levels.size <= depth) levels.add(mutableListOf())
        levels[depth] += CharmTreeLayoutNode(node, node.charm.id)
    }

    levels.forEach { level ->
        level.sortWith(compareBy<CharmTreeLayoutNode>(
            { it.node.charm.essenceMinimum },
            { it.node.charm.abilityMinimum },
            { it.node.charm.name.lowercase() },
            { it.key.lowercase() }
        ))
    }
    return levels
}

private fun List<Float>.averageOrNull(): Float? =
    if (isEmpty()) null else average().toFloat()



/**
 * Mantém o Encanto solicitado pelo long press como primeira caixa do seu
 * nível visual. A árvore completa pode ter vários ramos independentes; sem
 * esta priorização, o nó inicial correto podia existir no nível correto mas
 * ficar fora do viewport horizontal, que abre em x=0.
 */
internal fun prioritizeCharmInVisualLevel(
    levels: List<List<CharmTreeLayoutNode>>,
    charmId: String
): List<List<CharmTreeLayoutNode>> {
    val normalized = charmId.trim()
    if (normalized.isEmpty()) return levels
    return levels.map { level ->
        val index = level.indexOfFirst { it.key.equals(normalized, ignoreCase = true) }
        if (index <= 0) level
        else buildList(level.size) {
            add(level[index])
            level.forEachIndexed { i, node -> if (i != index) add(node) }
        }
    }
}

/**
 * Returns the visual level that contains [charmId].
 *
 * Long-press trees may render several independent branches at once. Their
 * logical requested root is therefore not necessarily the first global level.
 * Keeping this lookup separate prevents callers from assuming scroll=0 means
 * "show the requested Charm".
 */
internal fun findCharmTreeVisualLevel(
    levels: List<List<CharmTreeLayoutNode>>,
    charmId: String
): Int? {
    val normalized = charmId.trim()
    if (normalized.isEmpty()) return null
    return levels.indexOfFirst { level ->
        level.any { it.key.equals(normalized, ignoreCase = true) }
    }.takeIf { it >= 0 }
}
