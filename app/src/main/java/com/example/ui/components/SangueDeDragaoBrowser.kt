package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable

import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.example.data.EncantoSangueDeDragaoDefinition
import com.example.model.ExaltedConstants
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedOnSurface

// Navegação somente-consulta dos Encantos de Sangue de Dragão — pensada
// pra buscar/ler encantos sem precisar ter uma planilha desse Tipo de
// Exaltado ativa no momento, diferente do fluxo de aquisição normal
// (como o dos Encantos Solares em CharmsTab.kt, que marca/desmarca
// Encantos na própria planilha). Acessível via Opções > Consultar Sangue
// de Dragão (SheetScreen.kt).
@Composable
fun SangueDeDragaoBrowserDialog(show: Boolean, onDismiss: () -> Unit, definitions: List<EncantoSangueDeDragaoDefinition>) {
    if (!show) return

    var habilidadeExpandida by remember { mutableStateOf<String?>(null) }
    var encantoSelecionado by remember { mutableStateOf<EncantoSangueDeDragaoDefinition?>(null) }

    // APPROVED PERFORMANCE REFACTOR
    // A composição do diálogo pode ocorrer muitas vezes enquanto apenas o
    // estado de expansão muda. Agrupar, ordenar por Habilidade e ordenar os
    // Encantos de cada grupo são operações exclusivamente derivadas de
    // `definitions`; fazê-las uma única vez evita trabalho no hot path da UI.
    val porHabilidade = remember(definitions) {
        val ordemHabilidades = ExaltedConstants.ALL_25_ABILITIES
            .withIndex()
            .associate { it.value to it.index }
        definitions
            .groupBy { it.habilidade }
            .mapValues { (_, encantos) ->
                encantos.sortedWith(compareBy<EncantoSangueDeDragaoDefinition> { it.minEssencia }.thenBy { it.nome })
            }
            .toSortedMap(compareBy { habilidade -> ordemHabilidades[habilidade] ?: Int.MAX_VALUE })
    }
    val gruposOrdenados = remember(porHabilidade) { porHabilidade.entries.toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = { AppText("Encantos — Sangue de Dragão", color = ExaltedAccentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                items(gruposOrdenados) { (habilidade, encantosDaHabilidade) ->
                    val expandida = habilidadeExpandida == habilidade
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .feedbackClickable { habilidadeExpandida = if (expandida) null else habilidade },
                    ) {
                        AppText(if (expandida) "▾" else "▸", color = ExaltedGold, modifier = Modifier.padding(end = 6.dp))
                        AppText(
                            "$habilidade (${encantosDaHabilidade.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExaltedAccentBright
                        )
                    }
                    if (expandida) {
                        encantosDaHabilidade.forEach { def ->
                            AppText(
                                "• ${def.nome} (Essência ${def.minEssencia})",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExaltedOnSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 18.dp, top = 2.dp, bottom = 2.dp)
                                    .feedbackClickable { encantoSelecionado = def }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            GildedDialogButton(text = "Fechar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurface
    )

    val def = encantoSelecionado
    if (def != null) {
        AlertDialog(
            onDismissRequest = { encantoSelecionado = null },
            modifier = Modifier.then(gildedDialogBorder()),
            shape = dialogShape,
            title = { ChamTitleTwoLines(nomePt = def.nome, nomeEn = def.nomeIngles) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CampoNegritoSDD("Custo", def.custo)
                    CampoNegritoSDD("Mins", def.minsTexto.ifBlank { "${def.habilidade} ${def.minHabilidade}, Essência ${def.minEssencia}" })
                    CampoNegritoSDD("Tipo", def.tipo)
                    CampoNegritoSDD("Palavras-chave", def.palavrasChave)
                    CampoNegritoSDD("Duração", def.duracao)
                    CampoNegritoSDD("Pré-requisitos", def.preRequisitos)
                    Spacer(Modifier.height(6.dp))
                    JustifiedBodyAppText(def.descricao, color = ExaltedOnSurface)
                    def.quadros.forEach { quadro ->
                        Spacer(Modifier.height(8.dp))
                        EncantoQuadroBox(quadro)
                    }
                }
            },
            confirmButton = {
                GildedDialogButton(text = "Fechar", onClick = { encantoSelecionado = null })
            },
            containerColor = ExaltedDarkSurfaceVariant
        )
    }
}

/** Campo "Rótulo: valor" com o rótulo em negrito — mesma correção aplicada
 * em CharmDetailsDialogs.kt/FeiticosTab.kt, confirmada contra os
 * documentos de referência do usuário. */
@Composable
private fun CampoNegritoSDD(rotulo: String, valor: String) {
    AppText(
        androidx.compose.ui.text.buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                append("$rotulo: ")
            }
            append(valor)
        },
        color = ExaltedOnSurface
    )
}
