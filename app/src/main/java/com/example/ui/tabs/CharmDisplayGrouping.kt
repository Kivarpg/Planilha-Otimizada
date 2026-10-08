package com.example.ui.tabs

import com.example.data.EncantosSolaresCatalog
import com.example.model.Encanto
import com.example.model.EncantoEncontro
import com.example.model.NOME_CORPO_DE_TOURO

/**
 * Representação de uma aquisição acumulável para fins exclusivamente visuais.
 * A lista original continua contendo uma entrada por aquisição; [quantity]
 * apenas consolida repetições de uma definição que pode ser adquirida mais de
 * uma vez. Encantos não acumuláveis permanecem como linhas independentes.
 */
internal data class GroupedCharmDisplay(
    val charm: Encanto,
    val quantity: Int,
    val members: List<Encanto>
)

/** Representação visual de aquisições acumuláveis na planilha de encontro. */
internal data class GroupedEncounterCharmDisplay(
    val charm: EncantoEncontro,
    val quantity: Int,
    val members: List<EncantoEncontro>
)

/** Formato compacto usado na gaveta de Encantos da Aba 11. */
internal fun formatGroupedEncounterCharmName(group: GroupedEncounterCharmDisplay): String =
    if (group.quantity > 1) "${group.charm.nome} (x${group.quantity})" else group.charm.nome

private fun isAccumulatingEncounterCharm(charm: EncantoEncontro): Boolean =
    EncantosSolaresCatalog.sameName(charm.nome, NOME_CORPO_DE_TOURO)

/**
 * Agrupa somente aquisições repetíveis da Aba 11. Encantos não acumuláveis
 * continuam como entradas independentes, exatamente como na apresentação
 * das abas jogáveis.
 */
internal fun groupAccumulatedEncounterCharms(items: List<EncantoEncontro>): List<GroupedEncounterCharmDisplay> {
    val grouped = linkedMapOf<String, MutableList<EncantoEncontro>>()
    items.forEachIndexed { index, charm ->
        val key = if (isAccumulatingEncounterCharm(charm)) {
            "acc:${charm.habilidadeVinculada}|${EncantosSolaresCatalog.normalize(charm.nome)}"
        } else {
            "single:$index:${charm.nome}"
        }
        grouped.getOrPut(key) { mutableListOf() }.add(charm)
    }
    return grouped.values.map { group ->
        GroupedEncounterCharmDisplay(
            charm = group.first(),
            quantity = group.size,
            members = group
        )
    }
}

private fun isAccumulatingCharm(charm: Encanto): Boolean =
    EncantosSolaresCatalog.sameName(charm.nome, NOME_CORPO_DE_TOURO)

internal fun groupAccumulatedCharms(items: List<Encanto>): List<GroupedCharmDisplay> {
    val grouped = linkedMapOf<String, MutableList<Encanto>>()

    items.forEachIndexed { index, charm ->
        val key = if (isAccumulatingCharm(charm)) {
            "acc:${charm.categoria}|${charm.circulo}|${charm.habilidadeVinculada}|${EncantosSolaresCatalog.normalize(charm.nome)}"
        } else {
            // Duplicatas acidentais de um Encanto não acumulável não podem ser
            // escondidas pela apresentação. Cada aquisição não acumulável
            // permanece em sua própria linha.
            "single:$index:${charm.id}"
        }
        grouped.getOrPut(key) { mutableListOf() }.add(charm)
    }

    return grouped.values.map { group ->
        GroupedCharmDisplay(
            charm = group.first(),
            quantity = group.size,
            members = group
        )
    }
}
