package com.example.ui.tabs

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.feature.charmtree.CharmPrerequisiteTreeDialog
import com.example.feature.charmtree.CharmTreeEntry
import com.example.feature.charmtree.CharmTreeResult
import com.example.model.Encanto
import com.example.ui.components.AppText
import com.example.ui.components.GildedDialogButton
import com.example.ui.components.gildedDialogBorder
import com.example.ui.components.dialogShape
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedMuted
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

internal data class PreparedCharmTree(
    val entries: List<CharmTreeEntry>,
    val result: CharmTreeResult
)

internal class CharmTreeOpenCache(private val maxEntries: Int = 8) {
    // LinkedHashMap em ordem de acesso implementa LRU sem remover e
    // reinserir cada entrada toda vez que uma árvore é reaberta.
    private val values = LinkedHashMap<String, PreparedCharmTree>(16, 0.75f, true)

    operator fun get(key: String): PreparedCharmTree? {
        val normalized = normalize(key)
        return values[normalized]
    }

    fun put(key: String, value: PreparedCharmTree) {
        if (maxEntries <= 0) return
        val normalized = normalize(key)
        values[normalized] = value
        while (values.size > maxEntries) {
            val eldestKey = values.keys.firstOrNull() ?: break
            values.remove(eldestKey)
        }
    }

    internal fun size(): Int = values.size

    private fun normalize(key: String): String = key.trim().lowercase()
}

/**
 * Árvore visual de Encantos da Aba 8.
 *
 * Todas as entradas de árvore passam pelo mesmo renderizador de
 * pré-requisitos: [CharmPrerequisiteTreeDialog]. Isso evita que a Aba 8,
 * árvores abertas por detalhes e árvores de Artes Marciais usem estratégias
 * diferentes para desenhar as conexões.
 */
@Composable
internal fun AbilityCharmTreeDialog(
    ability: String,
    charms: List<Encanto>,
    onDismiss: () -> Unit,
    canonicalPrerequisites: ((String) -> Set<String>)? = null,
    acquiredCharmNames: Set<String> = emptySet(),
    preparedTree: PreparedCharmTree? = null,
    onTreePrepared: ((PreparedCharmTree) -> Unit)? = null
) {
    val charmsDaArvore = remember(ability, charms) {
        // Artes Marciais são abertas pelo seletor de estilos dedicado.
        // Esta árvore recebe somente Habilidade/Atributo comum, impedindo que
        // uma chamada acidental volte a fundir estilos marciais distintos.
        if (ability.equals("Artes Marciais", ignoreCase = true)) {
            emptyList()
        } else {
            charms.filter { it.habilidadeVinculada.equals(ability, ignoreCase = true) }
        }
    }
    CharmListTreeDialog(
        title = ability,
        charms = charmsDaArvore,
        onDismiss = onDismiss,
        canonicalPrerequisites = canonicalPrerequisites,
        acquiredCharmNames = acquiredCharmNames,
        preparedTree = preparedTree,
        onTreePrepared = onTreePrepared
    )
}

/**
 * Compatibilidade para as entradas que anteriormente usavam o renderizador
 * separado. Agora esta entrada delega diretamente ao renderizador
 * único da árvore de pré-requisitos.
 *
 * O Encanto inicial é escolhido pelo menor requisito. A árvore da
 * Habilidade/Atributo permanece completa: todos os ramos reais são exibidos,
 * sem criar dependências artificiais entre raízes independentes.
 */


/**
 * Início determinístico solicitado pelo long press da Aba 8.
 *
 * Um Encanto sem descendentes não deve tomar o foco inicial de uma árvore só
 * porque empata nos Mins com uma raiz que realmente desbloqueia progressão.
 * Quando o grafo canônico está disponível, restringimos a escolha às raízes
 * úteis (Encantos usados como pré-requisito por pelo menos outro Encanto).
 * Se o grupo não possui nenhuma ligação, preservamos o fallback histórico.
 */
internal fun lowestRequirementCharm(
    charms: List<Encanto>,
    canonicalPrerequisites: ((String) -> Set<String>)? = null
): Encanto? {
    if (charms.isEmpty()) return null

    // A raiz visual segue estritamente a regra da Aba 8: menor requisito
    // dentro da gaveta inteira. O grafo canônico continua sendo usado pelo
    // renderizador para desenhar todos os ramos, mas não pode excluir um
    // Encanto mínimo apenas porque ele não possui descendentes.
    return charms.minWithOrNull(
        compareBy<Encanto>(
            { it.minEssencia },
            { it.minHabilidade },
            { it.nome.lowercase() },
            { it.id.lowercase() }
        )
    )
}

@Composable
internal fun CharmListTreeDialog(
    title: String,
    charms: List<Encanto>,
    onDismiss: () -> Unit,
    canonicalPrerequisites: ((String) -> Set<String>)? = null,
    acquiredCharmNames: Set<String> = emptySet(),
    preparedTree: PreparedCharmTree? = null,
    onTreePrepared: ((PreparedCharmTree) -> Unit)? = null
) {
    // Long press em Habilidade/Atributo sempre abre a árvore a partir do
    // menor requisito disponível no grupo. A ordenação é lexicográfica:
    // primeiro Essência mínima e, em caso de empate, Habilidade/Atributo mínimo.
    // O nome/ID apenas torna o desempate final determinístico.
    val rootCharm = remember(charms) {
        lowestRequirementCharm(charms)
    }

    if (rootCharm == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.then(gildedDialogBorder()),
            shape = dialogShape,
            title = {
                AppText(
                    forceStroke = true,
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ExaltedAccentBright,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                AppText(
                    "Nenhum Encanto encontrado para esta opção.",
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                GildedDialogButton(text = "Fechar", onClick = onDismiss)
            },
            containerColor = ExaltedDarkSurfaceVariant
        )
        return
    }

    CharmPrerequisiteTreeDialog(
        rootCharmId = rootCharm.id.trim().ifBlank { rootCharm.nome },
        charms = charms,
        onDismiss = onDismiss,
        dialogTitle = title,
        includeFullCatalog = true,
        canonicalPrerequisites = canonicalPrerequisites,
        acquiredCharmNames = acquiredCharmNames,
        preparedEntries = preparedTree?.entries,
        preparedResult = preparedTree?.result,
        onTreePrepared = { entries, result ->
            onTreePrepared?.invoke(PreparedCharmTree(entries, result))
        }
    )
}
