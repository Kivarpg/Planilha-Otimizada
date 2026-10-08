package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Armadura
import com.example.model.ArmorStatsTable
import com.example.model.CharacterSheet
import com.example.ui.components.AutoSizeText
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.GildedDialogButton
import com.example.ui.components.GildedDialogTextButton
import com.example.ui.components.LongPressCard
import com.example.ui.components.dialogShape
import com.example.ui.components.gildedDialogBorder
import com.example.ui.theme.ExaltedDangerCore
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.viewmodel.SheetViewModel

// Extraído de EquipmentTab.kt (2026-09, refatoração estrutural). Concern
// genuinamente separado de Arma — nenhuma variável de estado local daqui
// é lida por WeaponSection ou pelo restante de EquipmentTab.kt.
// Comportamento idêntico ao de antes — só o local do código mudou.
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
internal fun ArmorSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    var showArmorConfigDialog by remember { mutableStateOf(false) }
    var mostrarCatalogoArmadura by remember { mutableStateOf(false) }
    var mostrarCatalogoArmaduraMundana by remember { mutableStateOf(false) }
    var armaduraCatalogoSelecionada by remember { mutableStateOf<Pair<com.example.data.ArmorCatalog.CatalogoArmadura, String>?>(null) }
    var armaduraMundanaCatalogoSelecionada by remember { mutableStateOf<Triple<com.example.data.ArmorCatalogMundano.CatalogoArmaduraMundana, String, Boolean>?>(null) }
    var armorName by remember { mutableStateOf("") }
    var armorTipo by remember { mutableStateOf("Artefato") }
    var armorCategoria by remember { mutableStateOf("Leve") }
    var armorToDelete by remember { mutableStateOf<Armadura?>(null) }
    var armorToCommitWithMotes by remember { mutableStateOf<Armadura?>(null) }
    var armorMarcadores by remember { mutableStateOf<List<String>>(emptyList()) }

    AppText("Armadura", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ExaltedAmber, modifier = Modifier.padding(vertical = 4.dp))

    InkButton(
        label = "Cadastrar Armadura",
        selected = true,
        onClick = { showArmorConfigDialog = true },
        brushIndex = 1
    )

    Spacer(modifier = Modifier.height(5.dp))

    if (sheet.armaduras.isEmpty()) {
        AppText(
            text = "Nenhuma armadura registrada.",
            style = MaterialTheme.typography.bodySmall,
            color = ExaltedMuted,
            textAlign = TextAlign.Center
        )
    } else {
        AppText(
            text = "Toque no cartão para comitar (apenas uma por vez). Pressione longo para remover.",
            style = MaterialTheme.typography.bodySmall,
            color = ExaltedMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        sheet.armaduras.forEach { armor ->
            LongPressCard(
                onLongClick = { armorToDelete = armor },
                onClick = {
                    val custo = ArmorStatsTable.stats(armor.tipoArmadura, armor.categoriaPeso).comitamento
                    if (!armor.equipada && custo > 0) {
                        armorToCommitWithMotes = armor
                    } else {
                        viewModel.toggleArmorEquipped(armor.id)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = armor.equipada,
                                onCheckedChange = {
                                    val custo = ArmorStatsTable.stats(armor.tipoArmadura, armor.categoriaPeso).comitamento
                                    if (!armor.equipada && custo > 0) {
                                        armorToCommitWithMotes = armor
                                    } else {
                                        viewModel.toggleArmorEquipped(armor.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = ExaltedGold, uncheckedColor = ExaltedMuted)
                            ,
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                            AutoSizeText(
                                text = armor.nome,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                                minFontSize = 12.sp,
                                color = ExaltedAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (armor.equipada) {
                            Surface(
                                color = ExaltedGold.copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, ExaltedGold)
                            ) {
                                AppText(
                                    text = "Comitado",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ExaltedGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AppText(
                        text = "${armor.tipoArmadura}, ${armor.categoriaPeso} — Absorção: ${armor.absorcao} | Dureza: ${armor.dureza} | Penalidade: ${armor.penalidadeMobilidade} | Comitamento: ${ArmorStatsTable.stats(armor.tipoArmadura, armor.categoriaPeso).comitamento}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (armor.marcadores.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        AppText(
                            text = "Marcadores: ${armor.marcadores.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExaltedMuted
                        )
                    }
                }
            }
        }
    }

    if (showArmorConfigDialog) {
        AlertDialog(
            onDismissRequest = { showArmorConfigDialog = false },
            modifier = Modifier.then(gildedDialogBorder()),
            shape = dialogShape,
            title = { AppText("Configuração da Armadura", color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = armorName,
                        onValueChange = com.example.ui.components.rememberTypingFeedback { armorName = it.take(60); armorMarcadores = emptyList() },
                        label = { AppText("Nome da Armadura") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.components.exaltedTextFieldColors()
                    )
                    CategoryChipGroup(
                        options = listOf("Artefato", "Mundana"),
                        selected = armorTipo,
                        onSelect = { armorTipo = it }
                    )
                    CategoryChipGroup(
                        options = listOf("Leve", "Média", "Pesada"),
                        selected = armorCategoria,
                        onSelect = { armorCategoria = it; armorMarcadores = emptyList() }
                    )

                    // Campo "Catálogo" de armaduras Artefato — mesmo padrão
                    // já aplicado às armas: substituiu os botões inline por
                    // um campo que abre um diálogo de seleção, evitando o
                    // mesmo tipo de confusão de toque já corrigido lá.
                    if (armorTipo == "Artefato") {
                        val temCandidatas = (listOf("Leve", "Média", "Pesada").any { com.example.data.ArmorCatalog.candidatas(it).isNotEmpty() })
                        if (temCandidatas) {
                            InkButton(
                                label = "Catálogo de Armaduras",
                                onClick = { mostrarCatalogoArmadura = true },
                                modifier = Modifier.fillMaxWidth(),
                                size = InkButtonSize.Small,
                                fillMaxWidth = true
                            )
                        }
                    } else if (armorTipo == "Mundana") {
                        val temCandidatasMundanas = (listOf("Leve", "Média", "Pesada").any { com.example.data.ArmorCatalogMundano.candidatas(it).isNotEmpty() })
                        if (temCandidatasMundanas) {
                            InkButton(
                                label = "Catálogo de Armaduras",
                                onClick = { mostrarCatalogoArmaduraMundana = true },
                                modifier = Modifier.fillMaxWidth(),
                                size = InkButtonSize.Small,
                                fillMaxWidth = true
                            )
                        }
                    }
                }
            },
            dismissButton = {
                GildedDialogButton(
                    text = "Cadastrar",
                    onClick = {
                        if (armorName.isNotBlank()) {
                            viewModel.addArmor(armorName, armorTipo, armorCategoria, armorMarcadores)
                            armorName = ""
                            armorTipo = "Artefato"
                            armorCategoria = "Leve"
                            armorMarcadores = emptyList()
                            showArmorConfigDialog = false
                        }
                    }
                )
            },
            confirmButton = {
                GildedDialogTextButton(text = "Cancelar", onClick = { showArmorConfigDialog = false })
            },
            containerColor = ExaltedDarkSurfaceVariant
        )
    }

    armorToCommitWithMotes?.let { armor ->
        val custo = ArmorStatsTable.stats(armor.tipoArmadura, armor.categoriaPeso).comitamento
        MoteSourceDialog(
            itemName = armor.nome,
            custoTotal = custo,
            motesPessoaisDisponiveis = sheet.motesPessoaisDisponiveis(),
            motesPerifericosDisponiveis = sheet.motesPerifericosDisponiveis(),
            onConfirm = { pessoais, perifericos ->
                if (viewModel.equipArmorWithMoteSource(armor.id, pessoais, perifericos)) {
                    armorToCommitWithMotes = null
                }
            },
            onDismiss = { armorToCommitWithMotes = null }
        )
    }

    armorToDelete?.let { armor ->
        ConfirmDeleteDialog(
            itemTitle = armor.nome,
            onConfirm = {
                viewModel.removeArmor(armor.id)
                armorToDelete = null
            },
            onDismiss = { armorToDelete = null }
        )
    }

    if (mostrarCatalogoArmadura) {
        CatalogoArmaduraDialog(
            onEscolher = { candidata, peso ->
                // Um toque apenas abre a ficha do item; o cadastro ocorre
                // somente após confirmação explícita no diálogo de estatísticas.
                armaduraCatalogoSelecionada = candidata to peso
            },
            onDismiss = { mostrarCatalogoArmadura = false }
        )
    }
    if (mostrarCatalogoArmaduraMundana) {
        CatalogoArmaduraMundanaDialog(
            nivelRecursos = sheet.nivelRecursos(),
            onEscolher = { candidata, peso, presente ->
                // Um toque apenas abre a ficha do item; o cadastro ocorre
                // somente após confirmação explícita no diálogo de estatísticas.
                armaduraMundanaCatalogoSelecionada = Triple(candidata, peso, presente)
            },
            onDismiss = { mostrarCatalogoArmaduraMundana = false }
        )
    }

    armaduraCatalogoSelecionada?.let { (candidata, peso) ->
        CatalogoArmaduraDetalhesDialog(
            nome = candidata.nome,
            tipo = "Artefato",
            peso = peso,
            marcadores = candidata.marcadores,
            custoMerito = candidata.custoMerito,
            onSelecionar = {
                armorName = "${candidata.nome} ($peso)"
                armorCategoria = peso
                armorTipo = "Artefato"
                armorMarcadores = candidata.marcadores
                armaduraCatalogoSelecionada = null
                mostrarCatalogoArmadura = false
            },
            onDismiss = { armaduraCatalogoSelecionada = null }
        )
    }

    armaduraMundanaCatalogoSelecionada?.let { (candidata, peso, presente) ->
        CatalogoArmaduraDetalhesDialog(
            nome = candidata.nome,
            tipo = "Mundana",
            peso = peso,
            marcadores = candidata.etiquetas,
            custoRecursos = candidata.custoRecursos,
            presente = presente,
            onSelecionar = {
                armorName = "${candidata.nome} ($peso)" + if (presente) " [Presente]" else ""
                armorCategoria = peso
                armorTipo = "Mundana"
                armorMarcadores = candidata.etiquetas
                armaduraMundanaCatalogoSelecionada = null
                mostrarCatalogoArmaduraMundana = false
            },
            onDismiss = { armaduraMundanaCatalogoSelecionada = null }
        )
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun CatalogoArmaduraDetalhesDialog(
    nome: String,
    tipo: String,
    peso: String,
    marcadores: List<String>,
    custoMerito: Int? = null,
    custoRecursos: Int? = null,
    presente: Boolean = false,
    onSelecionar: () -> Unit,
    onDismiss: () -> Unit
) {
    val stats = ArmorStatsTable.stats(tipo, peso)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        title = {
            AppText(nome, color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppText("Tipo: $tipo", color = ExaltedOnSurface)
                AppText("Peso: $peso", color = ExaltedOnSurface)
                AppText("Absorção: ${stats.absorcao}", color = ExaltedOnSurface, modifier = Modifier.padding(top = 6.dp))
                AppText("Dureza: ${stats.dureza}", color = ExaltedOnSurface)
                AppText("Penalidade: ${stats.penalidadeMobilidade}", color = ExaltedOnSurface)
                AppText("Comitamento: ${stats.comitamento}", color = ExaltedGold)
                custoMerito?.let {
                    AppText("Mérito: ${"•".repeat(it)} ($it)", color = ExaltedGold, modifier = Modifier.padding(top = 6.dp))
                }
                custoRecursos?.let {
                    AppText("Recursos: ${"•".repeat(it)} ($it)" + if (presente) " — Presente" else "", color = ExaltedGold, modifier = Modifier.padding(top = 6.dp))
                }
                AppText(
                    "Características: ${if (marcadores.isEmpty()) "Nenhuma" else marcadores.joinToString(", ")}",
                    color = ExaltedOnSurface,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GildedDialogButton(text = "Selecionar", onClick = onSelecionar)
                GildedDialogTextButton(text = "Voltar", onClick = onDismiss)
            }
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

// Diálogo de seleção do catálogo de armaduras — mesmo padrão já aplicado
// ao catálogo de armas: lista tocável, cada item com sua própria área de
// toque isolada, em vez de botões numa grade.
@Composable
private fun CatalogoArmaduraDialog(
    onEscolher: (com.example.data.ArmorCatalog.CatalogoArmadura, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = { AppText("Catálogo de Armaduras", color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                listOf("Leve", "Média", "Pesada").forEach { peso ->
                    val candidatas = com.example.data.ArmorCatalog.candidatas(peso)
                    if (candidatas.isEmpty()) return@forEach
                    AppText(peso, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ExaltedAccentBright, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    candidatas.forEach { candidata ->
                        AppText(
                            candidata.nome,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .feedbackClickable { onEscolher(candidata, peso) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            GildedDialogTextButton(text = "Cancelar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

// Diálogo de seleção do catálogo de armaduras MUNDANAS — pedido explícito
// do usuário, mesmo padrão do catálogo de armas mundanas: valida
// Recursos, opção "Presente" ignora a validação.
@Composable
private fun CatalogoArmaduraMundanaDialog(
    nivelRecursos: Int,
    onEscolher: (com.example.data.ArmorCatalogMundano.CatalogoArmaduraMundana, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var presente by remember { mutableStateOf(false) }
    var erroRecursos by remember { mutableStateOf<String?>(null) }
    fun custoTexto(c: Int) = if (c <= 0) "—" else "•".repeat(c)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = { AppText("Catálogo de Armaduras", color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Checkbox(checked = presente, onCheckedChange = { presente = it; erroRecursos = null },
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                    AppText("Presente (ignora o requisito de Recursos)", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted)
                }
                if (!presente) {
                    AppText("Seu nível de Recursos: ${custoTexto(nivelRecursos)} ($nivelRecursos)", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted, modifier = Modifier.padding(bottom = 6.dp))
                }
                erroRecursos?.let {
                    AppText(it, style = MaterialTheme.typography.bodySmall, color = ExaltedDangerCore, modifier = Modifier.padding(bottom = 6.dp))
                }
                listOf("Leve", "Média", "Pesada").forEach { peso ->
                    val candidatas = com.example.data.ArmorCatalogMundano.candidatas(peso)
                    if (candidatas.isEmpty()) return@forEach
                    AppText(peso, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ExaltedAccentBright, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    candidatas.forEach { candidata ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .feedbackClickable {
                                    if (presente || nivelRecursos >= candidata.custoRecursos) {
                                        erroRecursos = null
                                        onEscolher(candidata, peso, presente)
                                    } else {
                                        erroRecursos = "Este equipamento exige Recursos ${candidata.custoRecursos}. O personagem possui Recursos $nivelRecursos e não pode adicioná-lo à planilha."
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AppText(candidata.nome, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            AppText(custoTexto(candidata.custoRecursos), style = MaterialTheme.typography.bodyMedium, color = ExaltedGold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            GildedDialogTextButton(text = "Cancelar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}
