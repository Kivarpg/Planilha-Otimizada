package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackOnPress

import com.example.model.isDragonBlooded
import com.example.model.isLunar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EncantosSolaresCatalog
import com.example.data.FeiticoDefinition
import com.example.model.CharacterSheet
import com.example.search.SkillSearchEngine
import com.example.search.toSearchableSkill
import com.example.ui.components.EncantoQuadroBox
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.viewmodel.SheetViewModel

// Extraído de CharmsTab.kt (2026-09, refatoração estrutural) — o código
// exclusivo de Feitiços (não compartilha nenhuma função com o código de
// Encantos: não usa CharmEligibilityRow, CharmAbilityButton nem os
// outros helpers que ficaram em CharmsTab.kt), então vive melhor no seu
// próprio arquivo. Comportamento idêntico ao de antes — só o local do
// código mudou, nenhuma lógica foi alterada.

@Composable
internal fun FeiticosPopup(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit,
    onShowDetail: (FeiticoDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val circulosDesbloqueados = remember(sheet) { viewModel.circulosDesbloqueados(sheet) }
    val circulos = when {
        sheet.tipoPersonagem.isDragonBlooded() -> listOf("Terrestre")
        sheet.tipoPersonagem.isLunar() -> listOf("Terrestre", "Celestial")
        else -> listOf("Terrestre", "Celestial", "Solar")
    }
    var buscaFeiticos by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText("Feitiços", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                com.example.ui.components.DebouncedSearchField(
                    value = buscaFeiticos,
                    onDebouncedChange = { buscaFeiticos = it },
                    label = "Buscar Feitiço (PT ou EN)"
                )
                val buscaNormalizada = remember(buscaFeiticos) { SkillSearchEngine.normalize(buscaFeiticos) }
                // Índice textual preparado uma única vez enquanto o popup vive. Os nomes PT/EN
                // dos feitiços são imutáveis; normalizá-los novamente a cada tecla digitada
                // desperdiçava NFD/Regex em todo o catálogo durante recomposições da busca.
                val feiticosIndexadosPorCirculo = remember(circulos, viewModel) {
                    buildMap(circulos.size) {
                        circulos.forEach { circulo ->
                            put(circulo, viewModel.feiticosDoCirculo(circulo).map { def -> def to def.toSearchableSkill() })
                        }
                    }
                }
                val feiticosPorCirculo = remember(feiticosIndexadosPorCirculo, buscaNormalizada) {
                    buildMap(circulos.size) {
                        circulos.forEach { circulo ->
                            val indexados = feiticosIndexadosPorCirculo[circulo].orEmpty()
                            put(
                                circulo,
                                if (buscaNormalizada.isBlank()) indexados.map { it.first } else indexados.asSequence()
                                    .filter { (_, searchable) ->
                                        searchable.namePtNormalized.contains(buscaNormalizada) ||
                                            searchable.nameEnNormalized.contains(buscaNormalizada)
                                    }
                                    .map { it.first }
                                    .toList()
                            )
                        }
                    }
                }
                val totalEncontrados = feiticosPorCirculo.values.sumOf { it.size }
                val encantosAdquiridos = remember(sheet.charms) {
                    val nomes = HashSet<String>(sheet.charms.size)
                    val primeiroPorNome = HashMap<String, com.example.model.Encanto>(sheet.charms.size)
                    sheet.charms.forEach { encanto ->
                        val nomeNormalizado = EncantosSolaresCatalog.normalize(encanto.nome)
                        nomes += nomeNormalizado
                        primeiroPorNome.putIfAbsent(nomeNormalizado, encanto)
                    }
                    nomes to primeiroPorNome
                }
                val nomesEncantosAdquiridos = encantosAdquiridos.first
                val primeiroEncantoPorNome = encantosAdquiridos.second
                if (totalEncontrados == 0) {
                    AppText(
                        "Nenhum Feitiço encontrado.\nTente remover o filtro ou alterar os termos da busca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExaltedMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
                circulos.forEach { circulo ->
                    val desbloqueado = sheet.modoLivre || circulo in circulosDesbloqueados
                    val feiticos = feiticosPorCirculo[circulo].orEmpty()
                    if (feiticos.isEmpty()) return@forEach
                    AppText(
                        "Círculo $circulo",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (desbloqueado) ExaltedGold else ExaltedMuted
                    )
                    feiticos.forEach { def ->
                        val nomeDefinicaoNormalizado = EncantosSolaresCatalog.normalize(def.nome)
                        val jaAdquirido = nomeDefinicaoNormalizado in nomesEncantosAdquiridos
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(def.nome) {
                                    detectTapGestures(onDoubleTap = { onShowDetail(def) })
                                }
                                .feedbackOnPress(),
                            color = if (desbloqueado) ExaltedDarkSurface else ExaltedDarkSurface.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, if (desbloqueado) ExaltedGold.copy(alpha = 0.5f) else ExaltedMuted.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (desbloqueado) {
                                    Checkbox(
                                        modifier = Modifier.feedbackOnPress(),
                                        checked = jaAdquirido,
                                        onCheckedChange = { marcado ->
                                            if (marcado) viewModel.addFeiticoFromDefinition(def)
                                            else {
                                                primeiroEncantoPorNome[nomeDefinicaoNormalizado]?.let { viewModel.removeCharm(it.id) }
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = ExaltedAmber, uncheckedColor = ExaltedGold)
                                    )
                                } else {
                                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Lock, contentDescription = "Círculo bloqueado", tint = ExaltedMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                                AppText(
                                    text = def.nome,
                                    color = if (desbloqueado) MaterialTheme.colorScheme.onSurface else ExaltedMuted,
                                    fontWeight = if (jaAdquirido) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                InkButton(onClick = { onShowDetail(def) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Outlined.Info, contentDescription = "Ver detalhes", tint = ExaltedGold, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(text = "Fechar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

@Composable
internal fun FeiticoDetailsDialog(
    def: FeiticoDefinition,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { com.example.ui.components.ChamTitleTwoLines(nomePt = def.nome, nomeEn = def.nomeIngles) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CampoNegrito("Círculo", def.circulo)
                CampoNegrito("Custo", def.custo)
                CampoNegrito("Palavras-chave", def.palavrasChave)
                CampoNegrito("Duração", def.duracao)
                CampoNegrito("Livro", def.livro)
                Spacer(Modifier.height(6.dp))
                com.example.ui.components.JustifiedBodyAppText(def.descricao)
                def.quadros.forEach { quadro ->
                    Spacer(Modifier.height(8.dp))
                    EncantoQuadroBox(quadro)
                }
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(text = "Fechar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}
