package com.example.feature.charmtree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import com.example.model.Encanto
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TreeNodeWidth = 220.dp
private val TreeCollapsedHeight = 104.dp
private val TreeExpandedHeight = 304.dp
private val TreeHorizontalGap = 24.dp
private val TreeVerticalGap = 38.dp
private val TreeContentPadding = 24.dp
private val TreeViewportHeight = 500.dp
private val TreeCompactViewportHeight = 360.dp

internal fun charmTreeViewportHeight(screenHeightDp: Int): Dp =
    if (screenHeightDp < 700) TreeCompactViewportHeight else TreeViewportHeight
private val TreeNodeHorizontalPadding = 12.dp
private val TreeNodeVerticalPadding = 9.dp

// Cores das caixas de Encanto na árvore de pré-requisitos — decoração que
// deve seguir a paleta do template ativo (dourado/vermelho/prata), por
// isso são propriedades computadas (get()), não vals fixos.
private val CharmBoxGold: Color get() = ExaltedAccentBright
private val CharmBoxGoldOff: Color get() = ExaltedAccentBright.copy(alpha = 0.35f)
private val CharmBoxRed = Color(0xFFB73A35)
private val CharmBoxRedOff = Color(0xFF6F6662)
private val CharmConnector = Color(0xFF9A9A9A)
private const val IndicatorCount = 5

/**
 * Calcula o deslocamento vertical para focalizar um nível específico.
 * A árvore completa da Aba 8 usa nível 0 (topo); árvores focais podem usar
 * níveis posteriores. O resultado nunca é negativo.
 */
internal fun initialCharmViewportOffset(
    visualLevel: Int,
    viewportHeight: Dp,
    nodeHeight: Dp,
    verticalGap: Dp,
    contentPadding: Dp
): Dp {
    val level = visualLevel.coerceAtLeast(0)
    val nodeCenter = contentPadding + (nodeHeight + verticalGap) * level + nodeHeight / 2
    return (nodeCenter - viewportHeight / 2).coerceAtLeast(0.dp)
}

/** Public entry point for the prerequisite-tree popup. */
@Composable
fun CharmPrerequisiteTreeDialog(
    rootCharmId: String,
    charms: List<Encanto>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dialogTitle: String? = null,
    includeFullCatalog: Boolean = false,
    canonicalPrerequisites: ((String) -> Set<String>)? = null,
    acquiredCharmNames: Set<String> = emptySet(),
    preparedEntries: List<CharmTreeEntry>? = null,
    preparedResult: CharmTreeResult? = null,
    onTreePrepared: ((List<CharmTreeEntry>, CharmTreeResult) -> Unit)? = null
) {
    var entries by remember(charms, canonicalPrerequisites, preparedEntries) {
        mutableStateOf(preparedEntries)
    }

    LaunchedEffect(charms, canonicalPrerequisites, preparedEntries) {
        if (preparedEntries == null) {
            entries = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                charms.paraArvoreDePreRequisitos(canonicalPrerequisites)
            }
        }
    }

    entries?.let { resolvedEntries ->
        CharmPrerequisiteTreeDialogEntries(
            rootCharmId = rootCharmId,
            charms = resolvedEntries,
            onDismiss = onDismiss,
            modifier = modifier,
            dialogTitle = dialogTitle,
            includeFullCatalog = includeFullCatalog,
            acquiredCharmNames = acquiredCharmNames,
            preparedResult = preparedResult,
            onTreePrepared = { result -> onTreePrepared?.invoke(resolvedEntries, result) }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        modifier = modifier,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(48.dp))
                AppText(
                    text = dialogTitle?.takeIf { it.isNotBlank() } ?: "Pré-requisitos",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
                    InkButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
            }
        },
        text = {
            AppText("Carregando árvore…")
        }
    )
}

@Composable
private fun CharmPrerequisiteTreeDialogEntries(
    rootCharmId: String,
    charms: List<CharmTreeEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dialogTitle: String? = null,
    includeFullCatalog: Boolean = false,
    acquiredCharmNames: Set<String> = emptySet(),
    preparedResult: CharmTreeResult? = null,
    onTreePrepared: ((CharmTreeResult) -> Unit)? = null
) {
    var result by remember(rootCharmId, charms, includeFullCatalog, preparedResult) { mutableStateOf(preparedResult) }
    var buildFinished by remember(rootCharmId, charms, includeFullCatalog, preparedResult) { mutableStateOf(preparedResult != null) }

    LaunchedEffect(rootCharmId, charms, includeFullCatalog, preparedResult) {
        if (preparedResult == null) {
            val built = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                CharmPrerequisiteTreeBuilder.build(rootCharmId, charms, includeFullCatalog)
            }
            result = built
            built?.let { onTreePrepared?.invoke(it) }
            buildFinished = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        modifier = modifier,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reserva simétrica: o título permanece geometricamente centralizado
                // e nunca divide espaço com o botão Fechar.
                Spacer(Modifier.width(48.dp))
                AppText(
                    text = dialogTitle?.takeIf { it.isNotBlank() }
                        ?: result?.root?.charm?.name
                        ?: "Pré-requisitos",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
                    InkButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
            }
        },
        text = {
            when {
                result != null -> CharmTreeContent(result!!, includeFullCatalog = includeFullCatalog, acquiredCharmNames = acquiredCharmNames)
                buildFinished -> AppText("Encanto não encontrado no catálogo desta árvore.")
                else -> AppText("Carregando árvore…")
            }
        }
    )
}


@Composable
private fun CharmTreeContent(result: CharmTreeResult, includeFullCatalog: Boolean, acquiredCharmNames: Set<String>) {
    val horizontalState = rememberScrollState()
    val verticalState = rememberScrollState()
    val normalizedOwnedNames = remember(acquiredCharmNames) {
        acquiredCharmNames.asSequence().map { it.trim().lowercase() }.toHashSet()
    }
    var expandedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Exibição no sentido da progressão: pré-requisitos acima e o Encanto
    // selecionado abaixo. Isso permite representar visualmente as
    // ramificações quando um Encanto depende de dois ou mais Encantos.
    val levels = remember(result.displayRoots, result.root.charm.id, includeFullCatalog) {
        prioritizeCharmInVisualLevel(
            if (includeFullCatalog) {
                buildCharmTreeLevelsByPrerequisiteDepth(result.displayRoots)
            } else {
                buildCharmTreeLevels(result.displayRoots)
            },
            result.root.charm.id
        )
    }

    // A árvore completa pode conter ramos independentes; por isso o Encanto
    // escolhido pelo long press (menor Essência -> menor Habilidade/Atributo)
    // não é necessariamente o primeiro nível global. Posicionamos o viewport
    // no nível que contém explicitamente result.root, em vez de assumir que
    // scroll=0 representa o ponto inicial solicitado.
    val entryNameById = remember(levels) {
        levels.asSequence().flatten().associate { it.node.charm.id to it.node.charm.name }
    }
    val initialVisualLevel = remember(levels, result.root.charm.id, includeFullCatalog) {
        // Aba 8 abre o catálogo completo. Nesse modo, o ponto mecânico inicial
        // já foi escolhido antes de construir a árvore; deslocar verticalmente
        // até o nível global desse ramo cria grandes áreas vazias quando há
        // múltiplas raízes independentes. A visualização deve começar no menor
        // nível da árvore completa, preservando todos os ramos.
        if (includeFullCatalog) 0
        else findCharmTreeVisualLevel(levels, result.root.charm.id)
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val viewportHeight = charmTreeViewportHeight(configuration.screenHeightDp)
    LaunchedEffect(result.root.charm.id, levels, initialVisualLevel, density, viewportHeight) {
        val level = initialVisualLevel ?: 0
        val targetDp = initialCharmViewportOffset(
            visualLevel = level,
            viewportHeight = viewportHeight,
            nodeHeight = TreeCollapsedHeight,
            verticalGap = TreeVerticalGap,
            contentPadding = TreeContentPadding
        )
        val targetPx = with(density) { targetDp.roundToPx() }

        // O ScrollState começa com maxValue == 0 antes da primeira medição.
        // A implementação antiga limitava o destino nesse instante e convertia
        // qualquer Encanto inicial em scroll=0. Aguarde o layout publicar o
        // intervalo real antes de posicionar o viewport.
        androidx.compose.runtime.withFrameNanos { }
        androidx.compose.runtime.withFrameNanos { }
        verticalState.scrollTo(targetPx.coerceIn(0, verticalState.maxValue))
        // No long press da Aba 8, a árvore completa deve abrir pelo topo e
        // centralizada horizontalmente. maxValue representa exatamente o excesso
        // de largura fora do viewport; metade dele posiciona o centro do conteúdo
        // no centro da área visível, independentemente da largura da árvore.
        horizontalState.scrollTo(horizontalState.maxValue / 2)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewportHeight)
                .horizontalScroll(horizontalState)
                .verticalScroll(verticalState)
        ) {
            CharmTreeLayout(
                levels = levels,
                expandedKeys = expandedKeys,
                acquiredCharmNames = normalizedOwnedNames,
                entryNameById = entryNameById,
                focalCharmId = result.root.charm.id,
                onToggle = { key ->
                    expandedKeys = if (key in expandedKeys) expandedKeys - key else expandedKeys + key
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (result.cycleDetected || result.missingPrerequisiteIds.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            TreeWarning(result)
        }
    }
}

@Composable
private fun TreeWarning(result: CharmTreeResult) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Aviso", tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            AppText(buildWarningAppText(result), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CharmTreeLayout(
    levels: List<List<CharmTreeLayoutNode>>,
    expandedKeys: Set<String>,
    acquiredCharmNames: Set<String>,
    entryNameById: Map<String, String>,
    focalCharmId: String,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val levelHeights = levels.map { level ->
        level.maxOfOrNull { node ->
            if (node.key in expandedKeys) TreeExpandedHeight else TreeCollapsedHeight
        } ?: TreeCollapsedHeight
    }
    val maxLevelWidth = levels.maxOfOrNull { level ->
        levelWidth(level.size)
    } ?: TreeNodeWidth
    val contentWidth = maxLevelWidth + TreeContentPadding * 2
    val contentHeight = levelHeights.fold(0.dp) { total, height -> total + height } +
        TreeVerticalGap * (levels.size - 1).coerceAtLeast(0) +
        TreeContentPadding * 2

    Box(
        modifier = modifier
            .width(contentWidth)
            .height(contentHeight)
            .padding(TreeContentPadding)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawConnectors(levels, levelHeights, expandedKeys)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            levels.forEachIndexed { depth, level ->
                val levelHeight = levelHeights[depth]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(levelHeight),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    level.forEachIndexed { index, layoutNode ->
                        CharmTreeNodeCard(
                            node = layoutNode.node,
                            status = charmTreeVisualStatus(layoutNode.node.charm, focalCharmId, acquiredCharmNames, entryNameById),
                            expanded = layoutNode.key in expandedKeys,
                            onClick = { onToggle(layoutNode.key) },
                            modifier = Modifier.requiredWidth(TreeNodeWidth)
                        )
                        if (index < level.lastIndex) Spacer(Modifier.width(TreeHorizontalGap))
                    }
                }
                if (depth < levels.lastIndex) Spacer(Modifier.height(TreeVerticalGap))
            }
        }
    }
}

@Composable
private fun CharmTreeNodeCard(
    node: CharmTreeNode,
    status: CharmTreeVisualStatus,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
            .height(if (expanded) TreeExpandedHeight else TreeCollapsedHeight)
            .clip(MaterialTheme.shapes.medium)
            .feedbackClickable(onClick = onClick),
        tonalElevation = if (status == CharmTreeVisualStatus.FOCAL) 5.dp else 2.dp,
        color = when (status) {
            CharmTreeVisualStatus.FOCAL -> ExaltedAccentBright.copy(alpha = 0.20f)
            CharmTreeVisualStatus.ACQUIRED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            CharmTreeVisualStatus.AVAILABLE -> MaterialTheme.colorScheme.surfaceVariant
            CharmTreeVisualStatus.BLOCKED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TreeNodeHorizontalPadding, vertical = TreeNodeVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RequirementDots(
                value = node.charm.essenceMinimum,
                activeColor = CharmBoxGold,
                inactiveColor = CharmBoxGoldOff,
                contentDescription = "Essência mínima ${node.charm.essenceMinimum}"
            )

            Spacer(Modifier.height(5.dp))

            AppText(
                text = node.charm.name,
                style = if (status == CharmTreeVisualStatus.FOCAL) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                fontWeight = if (status == CharmTreeVisualStatus.FOCAL || status == CharmTreeVisualStatus.ACQUIRED) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    AppText(
                        text = node.charm.description.ifBlank { "Descrição não disponível." },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (node.charm.ability.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    AppText(
                        text = node.charm.ability,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            AppText(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when (status) {
                    CharmTreeVisualStatus.FOCAL -> ExaltedAccentBright
                    CharmTreeVisualStatus.ACQUIRED -> MaterialTheme.colorScheme.primary
                    CharmTreeVisualStatus.AVAILABLE -> MaterialTheme.colorScheme.onSurface
                    CharmTreeVisualStatus.BLOCKED -> ExaltedMuted
                }
            )
            Spacer(Modifier.height(3.dp))

            RequirementDots(
                value = node.charm.abilityMinimum,
                activeColor = CharmBoxRed,
                inactiveColor = CharmBoxRedOff,
                contentDescription = "Habilidade mínima ${node.charm.abilityMinimum}"
            )
        }
    }
}

internal enum class CharmTreeVisualStatus(val label: String) {
    FOCAL("Foco"),
    ACQUIRED("Adquirido"),
    AVAILABLE("Disponível"),
    BLOCKED("Bloqueado")
}

internal fun charmTreeVisualStatus(
    charm: CharmTreeEntry,
    focalCharmId: String,
    acquiredCharmNames: Set<String>,
    entryNameById: Map<String, String>
): CharmTreeVisualStatus {
    if (charm.id == focalCharmId) return CharmTreeVisualStatus.FOCAL
    if (charm.name.trim().lowercase() in acquiredCharmNames) return CharmTreeVisualStatus.ACQUIRED

    val directPrerequisites = charm.prerequisiteIds
    if (directPrerequisites.isEmpty()) return CharmTreeVisualStatus.AVAILABLE
    val prerequisitesOwned = directPrerequisites.all { prerequisiteId ->
        val prerequisiteName = entryNameById[prerequisiteId] ?: return@all false
        prerequisiteName.trim().lowercase() in acquiredCharmNames
    }
    return if (prerequisitesOwned) CharmTreeVisualStatus.AVAILABLE else CharmTreeVisualStatus.BLOCKED
}

@Composable
private fun RequirementDots(
    value: Int,
    activeColor: Color,
    inactiveColor: Color,
    contentDescription: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(IndicatorCount) { index ->
            val active = index < value.coerceIn(0, IndicatorCount)
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .height(9.dp)
                    .width(9.dp)
                    .background(if (active) activeColor else inactiveColor, CircleShape)
            )
        }
    }
}

private fun buildWarningAppText(result: CharmTreeResult): String = buildString {
    if (result.cycleDetected) append("Foi detectado um ciclo nos pré-requisitos; a ramificação foi interrompida.")
    if (result.missingPrerequisiteIds.isNotEmpty()) {
        if (isNotEmpty()) append(" ")
        append("Há pré-requisitos não encontrados no catálogo.")
    }
}

private fun levelWidth(count: Int): Dp =
    (TreeNodeWidth * count) + (TreeHorizontalGap * (count - 1).coerceAtLeast(0))

// APPROVED VISUAL CUSTOMIZATION
// Connectors are routed from the bottom-center of a source card to the
// top-center of a destination card. Edges that belong to the same connected
// branch between two levels share one trunk; only real branch points create
// separate vertical segments.
private fun DrawScope.drawConnectors(
    levels: List<List<CharmTreeLayoutNode>>,
    levelHeights: List<Dp>,
    expandedKeys: Set<String>
) {
    if (levels.size < 2) return

    val positions = mutableMapOf<String, NodeGeometry>()
    var yOffset = 0f

    levels.forEachIndexed { depth, level ->
        val width = levelWidth(level.size).toPx()
        val startX = (size.width - width) / 2f
        val rowHeight = levelHeights[depth].toPx()

        level.forEachIndexed { index, layoutNode ->
            val left = startX + index * (TreeNodeWidth.toPx() + TreeHorizontalGap.toPx())
            val nodeWidth = TreeNodeWidth.toPx()
            val height = nodeHeight(layoutNode, expandedKeys).toPx()
            val top = yOffset + (rowHeight - height) / 2f
            positions[layoutNode.key] = NodeGeometry(
                centerX = left + nodeWidth / 2f,
                centerY = top + height / 2f,
                top = top,
                bottom = top + height,
                depth = depth
            )
        }

        yOffset += rowHeight
        if (depth < levels.lastIndex) yOffset += TreeVerticalGap.toPx()
    }

    val edges = levels.flatMap { level ->
        level.flatMap { dependentLayout ->
            dependentLayout.node.children.mapNotNull { prerequisite ->
                val dependent = positions[dependentLayout.key] ?: return@mapNotNull null
                val prerequisiteGeometry = positions[prerequisite.charm.id]
                    ?: return@mapNotNull null
                if (prerequisiteGeometry.depth >= dependent.depth) return@mapNotNull null
                TreeEdge(
                    sourceKey = prerequisite.charm.id,
                    targetKey = dependentLayout.key,
                    source = prerequisiteGeometry,
                    target = dependent
                )
            }
        }
    }

    edges
        .groupBy { edge -> edge.source.depth to edge.target.depth }
        .values
        .forEach { levelEdges ->
            connectedEdgeGroups(levelEdges).forEach { group ->
                if (group.isEmpty()) return@forEach

                val sourceBottom = group.maxOf { it.source.bottom }
                val targetTop = group.minOf { it.target.top }
                val busY = if (targetTop > sourceBottom) {
                    (sourceBottom + targetTop) * 0.5f
                } else {
                    group.map { (it.source.bottom + it.target.top) * 0.5f }.average().toFloat()
                }

                val sourceXs = group.map { it.source.centerX }.distinct().sorted()
                val targetXs = group.map { it.target.centerX }.distinct().sorted()
                val endpointXs = (sourceXs + targetXs).distinct().sorted()
                if (endpointXs.isEmpty()) return@forEach

                // One horizontal trunk serves this connected branch. Separate
                // graph components get separate trunks; unrelated branches are
                // therefore never artificially joined.
                val busLeft = endpointXs.first()
                val busRight = endpointXs.last()
                if (busRight > busLeft) {
                    drawLine(
                        color = CharmConnector,
                        start = Offset(busLeft, busY),
                        end = Offset(busRight, busY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                // Every source enters the shared trunk exactly at its bottom
                // center. A source with several dependents therefore branches
                // only after this common segment.
                sourceXs.forEach { sourceX ->
                    val source = group.first { it.source.centerX == sourceX }.source
                    drawLine(
                        color = CharmConnector,
                        start = source.bottomCenter(),
                        end = Offset(sourceX, busY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                // Every destination receives the arrow at its top center. The
                // card is drawn after the Canvas, so the connector terminates
                // exactly at the card boundary without entering its content.
                group
                    .distinctBy { it.targetKey }
                    .forEach { edge ->
                        drawArrowConnector(
                            color = CharmConnector,
                            start = Offset(edge.target.centerX, busY),
                            end = edge.target.topCenter()
                        )
                    }
            }
        }
}

private fun connectedEdgeGroups(edges: List<TreeEdge>): List<List<TreeEdge>> {
    if (edges.isEmpty()) return emptyList()

    val remaining = edges.toMutableList()
    val groups = mutableListOf<List<TreeEdge>>()

    while (remaining.isNotEmpty()) {
        val group = mutableListOf<TreeEdge>()
        val queue = ArrayDeque<TreeEdge>()
        queue.addLast(remaining.removeAt(0))

        while (queue.isNotEmpty()) {
            val edge = queue.removeFirst()
            group += edge

            val related = remaining.filter { candidate ->
                candidate.sourceKey == edge.sourceKey ||
                    candidate.targetKey == edge.targetKey ||
                    candidate.sourceKey == edge.targetKey ||
                    candidate.targetKey == edge.sourceKey
            }
            related.forEach { candidate ->
                remaining.remove(candidate)
                queue.addLast(candidate)
            }
        }

        groups += group
    }

    return groups
}

private data class TreeEdge(
    val sourceKey: String,
    val targetKey: String,
    val source: NodeGeometry,
    val target: NodeGeometry
)


private fun DrawScope.drawArrowConnector(color: Color, start: Offset, end: Offset) {
    drawLine(color = color, start = start, end = end, strokeWidth = 2.dp.toPx())
    val direction = (end - start).let { vector ->
        val length = kotlin.math.hypot(vector.x.toDouble(), vector.y.toDouble()).toFloat()
        if (length == 0f) Offset.Zero else Offset(vector.x / length, vector.y / length)
    }
    if (direction == Offset.Zero) return

    val perpendicular = Offset(-direction.y, direction.x)
    val tip = end
    val arrowLength = 7.dp.toPx()
    val arrowWidth = 4.dp.toPx()
    val base = tip - direction * arrowLength
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x + perpendicular.x * arrowWidth, base.y + perpendicular.y * arrowWidth)
        lineTo(base.x - perpendicular.x * arrowWidth, base.y - perpendicular.y * arrowWidth)
        close()
    }
    drawPath(path, color = color)
}

private fun NodeGeometry.bottomCenter(): Offset = Offset(centerX, bottom)

private fun NodeGeometry.topCenter(): Offset = Offset(centerX, top)

private data class NodeGeometry(
    val centerX: Float,
    val centerY: Float,
    val top: Float,
    val bottom: Float,
    val depth: Int
)


private fun nodeHeight(
    layoutNode: CharmTreeLayoutNode,
    expandedKeys: Set<String>
): Dp = if (layoutNode.key in expandedKeys) TreeExpandedHeight else TreeCollapsedHeight
