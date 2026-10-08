package com.example.ui.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Arma
import com.example.model.CharacterSheet
import com.example.model.WeaponStatsTable
import com.example.ui.components.AutoSizeText
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
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

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Opções estáticas da seção de armas: evitam recriação de listas durante recomposição.
private val WEAPON_HABILIDADES = listOf("Armas Brancas", "Arqueirismo", "Arremesso", "Briga")
private val WEAPON_ATRIBUTOS_BRIGA = listOf("Força", "Destreza")
private val WEAPON_TIPOS = listOf("Artefato", "Mundana")
private val WEAPON_CATEGORIAS_LEVE_MEDIA_PESADA = listOf("Leve", "Média", "Pesada")
private val WEAPON_CATEGORIAS_LEVE_MEDIA = listOf("Leve", "Média")
private val WEAPON_CATEGORIA_LEVE = listOf("Leve")

// Extraído de EquipmentTab.kt (2026-09, refatoração estrutural). Concern
// genuinamente separado de Armadura — nenhuma variável de estado local
// daqui é lida por ArmorSection ou pelo restante de EquipmentTab.kt (só
// MoteSourceDialog e CategoryChipGroup são de fato compartilhadas, e
// continuam em EquipmentTab.kt como internal). Comportamento idêntico ao
// de antes — só o local do código mudou.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun WeaponSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    var showWeaponConfigDialog by remember { mutableStateOf(false) }
    var mostrarCatalogoArma by remember { mutableStateOf(false) }
    var mostrarCatalogoArmaMundana by remember { mutableStateOf(false) }
    var armaCatalogoSelecionada by remember { mutableStateOf<Pair<com.example.data.WeaponCatalog.CatalogoArma, String>?>(null) }
    var armaMundanaCatalogoSelecionada by remember { mutableStateOf<Triple<com.example.data.WeaponCatalogMundano.CatalogoArmaMundana, String, Boolean>?>(null) }
    var weaponName by remember { mutableStateOf("") }
    var weaponHabilidade by remember { mutableStateOf("Armas Brancas") }
    var weaponAtributoBriga by remember { mutableStateOf("Força") }
    var weaponTipo by remember { mutableStateOf("Artefato") }
    var weaponCategoria by remember { mutableStateOf("Leve") }
    var weaponDesarmado by remember { mutableStateOf(false) }
    var weaponEtiquetas by remember { mutableStateOf<List<String>>(emptyList()) }
    // Quando uma arma é escolhida do catálogo, restringe a Habilidade às
    // formas de ataque realmente compatíveis com as etiquetas da arma.
    // Armas cadastradas manualmente continuam livres para configuração manual.
    var weaponHabilidadesPermitidas by remember { mutableStateOf<List<String>?>(null) }
    var weaponToDelete by remember { mutableStateOf<Arma?>(null) }
    var weaponToCommitWithMotes by remember { mutableStateOf<Arma?>(null) }
    var weaponToEditManually by remember { mutableStateOf<Arma?>(null) }

    AppText("ARMA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ExaltedAmber, modifier = Modifier.padding(vertical = 4.dp))

    InkButton(
        label = "Cadastrar Arma",
        selected = true,
        onClick = { showWeaponConfigDialog = true }
    )

    Spacer(modifier = Modifier.height(5.dp))

    if (sheet.weapons.isEmpty()) {
        AppText(
            text = "Nenhuma arma registrada.",
            style = MaterialTheme.typography.bodySmall,
            color = ExaltedMuted,
            textAlign = TextAlign.Center
        )
    } else {
        AppText(
            text = "Toque no cartão para comitar. Pressione longo no cartão para remover; pressione longo na habilidade para editar manualmente.",
            style = MaterialTheme.typography.bodySmall,
            color = ExaltedMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        sheet.weapons.forEach { w ->
            val estaComitada = w.motesPessoaisComitados > 0 || w.motesPerifericosComitados > 0
            LongPressCard(
                onLongClick = { weaponToDelete = w },
                onClick = { viewModel.toggleWeaponEquipped(w.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (w.equipada) 0.6f else 1f }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
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
                                checked = w.equipada,
                                onCheckedChange = { viewModel.toggleWeaponEquipped(w.id) },
                                colors = CheckboxDefaults.colors(checkedColor = ExaltedGold, uncheckedColor = ExaltedMuted),
                                modifier = Modifier.feedbackOnPress(enabled = true)
                            )
                            AutoSizeText(
                                text = w.nome,
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
                            if (w.modificadaManualmente) {
                                Surface(
                                    color = ExaltedMuted.copy(alpha = 0.25f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    AppText(
                                        text = "Modificada",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ExaltedMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (estaComitada) {
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
                            if (w.tipoArma == "Artefato" && !w.ataqueDesarmado) {
                                InkButton(
                                    label = if (estaComitada) "Descomitar" else "Comitar",
                                    onClick = {
                                        if (estaComitada) viewModel.descomitarArma(w.id)
                                        else weaponToCommitWithMotes = w
                                    },
                                    size = InkButtonSize.Small
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (w.ataqueDesarmado) {
                        AppText(
                            text = "Ataque Desarmado",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ExaltedAmber
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    AppText(
                        text = "${w.habilidadeVinculada}${if (w.atributoBriga != null) " (${w.atributoBriga})" else ""} — ${w.tipoArma}, ${w.categoriaPeso}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ExaltedGold,
                        modifier = Modifier.pointerInput(w.id) {
                            detectTapGestures(onLongPress = { weaponToEditManually = w })
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AppText(
                        text = "Fulminante: ${w.iniciativa}   Decisivo: ${w.decisivo}   Defesa: ${w.defesa}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AppText(
                        text = "Dano: ${w.dano}   Dano Mínimo: ${w.danoMinimo.ifBlank { "-" }}   Comitamento: ${if (w.ataqueDesarmado) 0 else WeaponStatsTable.comitamento(w.tipoArma)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (w.habilidadeVinculada == "Arremesso" || w.habilidadeVinculada == "Arqueirismo") {
                        val distancias = remember(w.id, w.habilidadeVinculada, w.tipoArma) {
                            if (w.habilidadeVinculada == "Arremesso") {
                                WeaponStatsTable.distanciasArremesso(w.tipoArma)
                            } else {
                                WeaponStatsTable.distanciasArqueirismo(w.tipoArma)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        AppText(
                            text = "Distância: " + distancias.joinToString(" | ") { (nome, valor) ->
                                "$nome ${if (valor >= 0) "+" else ""}$valor"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ExaltedMuted
                        )
                    }
                    if (w.etiquetas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        AppText(
                            text = "Etiquetas: ${w.etiquetas.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExaltedMuted
                        )
                    }
                }
            }
        }
    }

    if (showWeaponConfigDialog) {
        AlertDialog(
            onDismissRequest = { showWeaponConfigDialog = false },
            modifier = Modifier.then(gildedDialogBorder()),
            shape = dialogShape,
            title = { AppText("Configuração da Arma", color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = weaponName,
                        onValueChange = com.example.ui.components.rememberTypingFeedback {
                            weaponName = it.take(60)
                            weaponEtiquetas = emptyList()
                            weaponHabilidadesPermitidas = null
                        },
                        label = { AppText("Nome da Arma") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.components.exaltedTextFieldColors()
                    )

                    val habilidadesDaArma = weaponHabilidadesPermitidas ?: WEAPON_HABILIDADES
                    CategoryChipGroup(
                        options = habilidadesDaArma,
                        selected = weaponHabilidade,
                        onSelect = { if (it in habilidadesDaArma) weaponHabilidade = it }
                    )

                    if (weaponHabilidade == "Briga") {
                        AppText("Ataque usa", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                        CategoryChipGroup(
                            options = WEAPON_ATRIBUTOS_BRIGA,
                            selected = weaponAtributoBriga,
                            onSelect = { weaponAtributoBriga = it }
                        )
                    }

                    CategoryChipGroup(
                        options = WEAPON_TIPOS,
                        selected = weaponTipo,
                        onSelect = { novoTipo ->
                            if (novoTipo != weaponTipo) {
                                // Trocar de Artefato para Mundana inicia uma
                                // configuração limpa: nenhuma seleção, etiqueta,
                                // nome ou restrição específica do catálogo de
                                // Artefato pode permanecer no formulário.
                                weaponTipo = novoTipo
                                weaponName = ""
                                weaponEtiquetas = emptyList()
                                weaponHabilidadesPermitidas = null
                                armaCatalogoSelecionada = null
                                armaMundanaCatalogoSelecionada = null
                                weaponHabilidade = "Armas Brancas"
                                weaponAtributoBriga = "Força"
                                weaponCategoria = "Leve"
                                weaponDesarmado = false
                            }
                        }
                    )

                    val categoriasDisponiveis = when (weaponHabilidade) {
                        // Briga é exclusivamente Leve.
                        "Briga" -> WEAPON_CATEGORIA_LEVE
                        // Não existe arma "Pesada" de Arremesso.
                        "Arremesso" -> WEAPON_CATEGORIAS_LEVE_MEDIA
                        else -> WEAPON_CATEGORIAS_LEVE_MEDIA_PESADA
                    }
                    androidx.compose.runtime.LaunchedEffect(weaponHabilidade) {
                        if (weaponCategoria !in categoriasDisponiveis) {
                            weaponCategoria = categoriasDisponiveis.first()
                        }
                        if (weaponHabilidade != "Briga") {
                            weaponDesarmado = false
                        }
                        if (weaponHabilidadesPermitidas == null) {
                            weaponEtiquetas = emptyList()
                        }
                    }
                    // Ataque Desarmado usa os valores de uma arma leve
                    // mundana — pedido explícito do usuário: ao marcar,
                    // preenche Tipo=Mundana e Categoria=Leve
                    // automaticamente (Categoria já é sempre Leve pra
                    // Briga, então só o Tipo precisa mudar aqui).
                    androidx.compose.runtime.LaunchedEffect(weaponDesarmado) {
                        if (weaponDesarmado) {
                            weaponTipo = "Mundana"
                            weaponCategoria = "Leve"
                        }
                    }
                    CategoryChipGroup(
                        options = categoriasDisponiveis,
                        selected = weaponCategoria,
                        onSelect = { weaponCategoria = it }
                    )

                    // Campo "Catálogo" — pedido explícito do usuário.
                    // Cobre tanto Artefato (catálogo original, custo em PM)
                    // quanto Mundana (catálogo novo, custo em Recursos —
                    // valida contra sheet.nivelRecursos(), com opção
                    // "Presente" pra adicionar mesmo sem o Recursos
                    // necessário).
                    if (weaponTipo == "Artefato") {
                        val temCandidatas = com.example.data.WeaponCatalog.candidatasPorHabilidade(weaponHabilidade).isNotEmpty()
                        if (temCandidatas) {
                            InkButton(
                                label = "Catálogo de Armas",
                                onClick = { mostrarCatalogoArma = true },
                                modifier = Modifier.fillMaxWidth(),
                                size = InkButtonSize.Small,
                                fillMaxWidth = true
                            )
                        }
                    } else if (weaponTipo == "Mundana") {
                        val temCandidatasMundanas = com.example.data.WeaponCatalogMundano.candidatasPorHabilidade(weaponHabilidade).isNotEmpty()
                        if (temCandidatasMundanas) {
                            InkButton(
                                label = "Catálogo de Armas",
                                onClick = { mostrarCatalogoArmaMundana = true },
                                modifier = Modifier.fillMaxWidth(),
                                size = InkButtonSize.Small,
                                fillMaxWidth = true
                            )
                        }
                    }

                    if (weaponHabilidade == "Briga") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = weaponDesarmado,
                                onCheckedChange = { weaponDesarmado = it },
                                colors = CheckboxDefaults.colors(checkedColor = ExaltedAmber, uncheckedColor = ExaltedMuted)
                            ,
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                            AppText("Ataque Desarmado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            dismissButton = {
                GildedDialogButton(
                    text = "Cadastrar",
                    onClick = {
                        if (weaponName.isNotBlank()) {
                            viewModel.addWeapon(
                                weaponName, weaponHabilidade,
                                if (weaponHabilidade == "Briga") weaponAtributoBriga else null,
                                weaponTipo, weaponCategoria, weaponDesarmado, weaponEtiquetas
                            )
                            weaponName = ""
                            weaponHabilidade = "Armas Brancas"
                            weaponAtributoBriga = "Força"
                            weaponTipo = "Artefato"
                            weaponCategoria = "Leve"
                            weaponDesarmado = false
                            weaponEtiquetas = emptyList()
                            weaponHabilidadesPermitidas = null
                            showWeaponConfigDialog = false
                        }
                    }
                )
            },
            confirmButton = {
                GildedDialogTextButton(text = "Cancelar", onClick = { showWeaponConfigDialog = false })
            },
            containerColor = ExaltedDarkSurfaceVariant
        )
    }

    weaponToEditManually?.let { w ->
        WeaponEditManualDialog(
            arma = w,
            viewModel = viewModel,
            onDismiss = { weaponToEditManually = null }
        )
    }

    weaponToCommitWithMotes?.let { w ->
        val custo = WeaponStatsTable.comitamento(w.tipoArma)
        MoteSourceDialog(
            itemName = w.nome,
            custoTotal = custo,
            motesPessoaisDisponiveis = sheet.motesPessoaisDisponiveis(),
            motesPerifericosDisponiveis = sheet.motesPerifericosDisponiveis(),
            onConfirm = { pessoais, perifericos ->
                if (viewModel.equipWeaponWithMoteSource(w.id, pessoais, perifericos)) {
                    weaponToCommitWithMotes = null
                }
            },
            onDismiss = { weaponToCommitWithMotes = null }
        )
    }

    weaponToDelete?.let { w ->
        ConfirmDeleteDialog(
            itemTitle = w.nome,
            onConfirm = {
                viewModel.removeWeapon(w.id)
                weaponToDelete = null
            },
            onDismiss = { weaponToDelete = null }
        )
    }
    if (mostrarCatalogoArma) {
        CatalogoArmaDialog(
            habilidade = weaponHabilidade,
            onEscolher = { candidata, peso ->
                // Um toque apenas abre a ficha do item; a seleção para cadastro
                // ocorre explicitamente no diálogo de estatísticas.
                armaCatalogoSelecionada = candidata to peso
            },
            onDismiss = { mostrarCatalogoArma = false }
        )
    }
    if (mostrarCatalogoArmaMundana) {
        CatalogoArmaMundanaDialog(
            habilidade = weaponHabilidade,
            nivelRecursos = sheet.nivelRecursos(),
            onEscolher = { candidata, peso, presente ->
                // Um toque apenas abre a ficha do item; a seleção para cadastro
                // ocorre explicitamente no diálogo de estatísticas.
                armaMundanaCatalogoSelecionada = Triple(candidata, peso, presente)
            },
            onDismiss = { mostrarCatalogoArmaMundana = false }
        )
    }

    armaCatalogoSelecionada?.let { (candidata, peso) ->
        CatalogoArmaDetalhesDialog(
            nome = candidata.nome,
            peso = peso,
            etiquetas = candidata.etiquetas,
            tipoArma = "Artefato",
            habilidade = weaponHabilidade,
            onSelecionar = {
                weaponName = "${candidata.nome} ($peso)"
                weaponCategoria = peso
                weaponEtiquetas = candidata.etiquetas
                weaponHabilidadesPermitidas = habilidadesCompativeis(candidata.etiquetas)
                if (weaponHabilidade !in (weaponHabilidadesPermitidas ?: WEAPON_HABILIDADES)) {
                    weaponHabilidade = weaponHabilidadesPermitidas?.firstOrNull() ?: weaponHabilidade
                }
                armaCatalogoSelecionada = null
                mostrarCatalogoArma = false
            },
            onDismiss = { armaCatalogoSelecionada = null }
        )
    }

    armaMundanaCatalogoSelecionada?.let { (candidata, peso, presente) ->
        CatalogoArmaDetalhesDialog(
            nome = candidata.nome,
            peso = peso,
            etiquetas = candidata.etiquetas,
            tipoArma = "Mundana",
            habilidade = weaponHabilidade,
            custoRecursos = candidata.custoRecursos,
            presente = presente,
            onSelecionar = {
                weaponName = "${candidata.nome} ($peso)" + if (presente) " [Presente]" else ""
                weaponCategoria = peso
                weaponEtiquetas = candidata.etiquetas
                weaponHabilidadesPermitidas = habilidadesCompativeis(candidata.etiquetas)
                if (weaponHabilidade !in (weaponHabilidadesPermitidas ?: WEAPON_HABILIDADES)) {
                    weaponHabilidade = weaponHabilidadesPermitidas?.firstOrNull() ?: weaponHabilidade
                }
                armaMundanaCatalogoSelecionada = null
                mostrarCatalogoArmaMundana = false
            },
            onDismiss = { armaMundanaCatalogoSelecionada = null }
        )
    }
}

// Extraído do corpo de WeaponSection (refatoração de organização —
// pedido explícito do usuário, sem mudança de comportamento). Já era
// autocontido no original: o estado local (remember) fica escoparado
// à própria arma sendo editada, sem depender de mais nada do restante
// da tela além de `arma` e `viewModel`.
@Composable
private fun WeaponEditManualDialog(
    arma: Arma,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit
) {
    var ini by remember(arma.id) { mutableStateOf(arma.iniciativa) }
    var dec by remember(arma.id) { mutableStateOf(arma.decisivo) }
    var def by remember(arma.id) { mutableStateOf(arma.defesa.toString()) }
    var dmg by remember(arma.id) { mutableStateOf(arma.dano) }
    var dmgMin by remember(arma.id) { mutableStateOf(arma.danoMinimo) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = { AppText("Editar manualmente — ${arma.nome}", color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 3, overflow = TextOverflow.Ellipsis, forceStroke = true) },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 380.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = ini, onValueChange = com.example.ui.components.rememberTypingFeedback { ini = it.take(6) }, label = { AppText("Ini.") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dec, onValueChange = com.example.ui.components.rememberTypingFeedback { dec = it.take(6) }, label = { AppText("Dec.") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = def, onValueChange = com.example.ui.components.rememberTypingFeedback { def = it.take(6) }, label = { AppText("Def.") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dmg, onValueChange = com.example.ui.components.rememberTypingFeedback { dmg = it.take(6) }, label = { AppText("Dano") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dmgMin, onValueChange = com.example.ui.components.rememberTypingFeedback { dmgMin = it.take(6) }, label = { AppText("Dano Mín.") }, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = ini, onValueChange = com.example.ui.components.rememberTypingFeedback { ini = it.take(6) }, label = { AppText("Ini.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = dec, onValueChange = com.example.ui.components.rememberTypingFeedback { dec = it.take(6) }, label = { AppText("Dec.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = def, onValueChange = com.example.ui.components.rememberTypingFeedback { def = it.take(6) }, label = { AppText("Def.") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = dmg, onValueChange = com.example.ui.components.rememberTypingFeedback { dmg = it.take(6) }, label = { AppText("Dano") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = dmgMin, onValueChange = com.example.ui.components.rememberTypingFeedback { dmgMin = it.take(6) }, label = { AppText("Dano Mín.") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        dismissButton = {
            GildedDialogButton(
                text = "Salvar",
                onClick = {
                    viewModel.updateWeaponManual(arma.id, ini, dec, def.toIntOrNull() ?: 0, dmg, dmgMin)
                    onDismiss()
                }
            )
        },
        confirmButton = {
            GildedDialogTextButton(text = "Cancelar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

// Diálogo de seleção do catálogo de armas — pedido explícito do usuário.
// Substituiu os botões inline por uma lista tocável, cada item com sua
// própria área de toque isolada (Row + clickable), evitando o bug
// reportado onde tocar na área de sugestões preenchia o nome com uma
// arma diferente da realmente tocada.
//
// CORREÇÃO: este diálogo mostrava só Peso/Recursos/Características —
// as estatísticas de combate (Precisão, Dano, Dano Mínimo, Defesa,
// Comitamento) já existiam prontas em WeaponStatsTable (mesma tabela
// usada ao efetivamente cadastrar a arma em CombatActions.kt), só não
// eram lidas aqui. Agora calculamos com base em tipoArma + habilidade +
// peso (os três já determinam a linha certa da tabela) e exibimos,
// igual pra Artefato e pra Mundana.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogoArmaDetalhesDialog(
    nome: String,
    peso: String,
    etiquetas: List<String>,
    tipoArma: String,
    habilidade: String,
    custoRecursos: Int? = null,
    presente: Boolean = false,
    onSelecionar: () -> Unit,
    onDismiss: () -> Unit
) {
    val ehDistancia = habilidade == "Arremesso" || habilidade == "Arqueirismo"
    val comitamento = com.example.model.WeaponStatsTable.comitamento(tipoArma)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = {
            AppText(
                nome,
                color = ExaltedAmber,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                forceStroke = true
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppText("Categoria: $peso", color = ExaltedOnSurface)
                custoRecursos?.let {
                    AppText(
                        "Recursos: ${custoRecursosTexto(it)} ($it)" + if (presente) " — Presente" else "",
                        color = ExaltedGold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                AppText(
                    "Características: ${if (etiquetas.isEmpty()) "Nenhuma" else etiquetas.joinToString(", ")}",
                    color = ExaltedOnSurface,
                    modifier = Modifier.padding(top = 6.dp)
                )
                AppText(
                    "Estatísticas de combate",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExaltedAmber,
                    modifier = Modifier.padding(top = 10.dp)
                )
                if (ehDistancia) {
                    val (dano, danoMinimo) = com.example.model.WeaponStatsTable.distancia(tipoArma, peso)
                    AppText("Precisão: —", color = ExaltedOnSurface)
                    AppText("Dano: $dano", color = ExaltedOnSurface)
                    AppText("Defesa: —", color = ExaltedOnSurface)
                    AppText("Dano Mínimo: $danoMinimo", color = ExaltedOnSurface)
                } else {
                    val stats = com.example.model.WeaponStatsTable.corpoACorpo(tipoArma, peso)
                    AppText("Precisão: ${stats.precisao}", color = ExaltedOnSurface)
                    AppText("Dano: ${stats.dano}", color = ExaltedOnSurface)
                    AppText("Defesa: ${stats.defesa}", color = ExaltedOnSurface)
                    AppText("Dano Mínimo: ${stats.danoMinimo}", color = ExaltedOnSurface)
                }
                AppText("Comitamento: $comitamento motes", color = ExaltedOnSurface)
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GildedDialogButton(text = "Selecionar", onClick = onSelecionar)
                GildedDialogTextButton(text = "Voltar", onClick = onDismiss)
            }
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

@Composable
private fun CatalogoArmaDialog(
    habilidade: String,
    onEscolher: (com.example.data.WeaponCatalog.CatalogoArma, String) -> Unit,
    onDismiss: () -> Unit
) {
    val candidatasPorPeso = com.example.data.WeaponCatalog
        .candidatasPorHabilidade(habilidade)
        .groupBy { it.peso }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = { AppText("Catálogo — $habilidade", color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                listOf("Leve", "Média", "Pesada").forEach { peso ->
                    val candidatas = candidatasPorPeso[peso] ?: return@forEach
                    AppText(peso, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ExaltedAccentBright, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    candidatas.forEach { candidata ->
                        AppText(
                            candidata.nome,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExaltedOnSurface,
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

// Retorna somente as Habilidades compatíveis com uma arma do catálogo.
// A compatibilidade é derivada das etiquetas de combate do próprio item;
// portanto uma arma de Armas Brancas não pode ser cadastrada como Arqueirismo,
// e uma arma de Arqueirismo não pode ser cadastrada como Armas Brancas.
private fun habilidadesCompativeis(etiquetas: List<String>): List<String> = buildList {
    if (etiquetas.any { it.equals("Armas brancas", ignoreCase = true) }) add("Armas Brancas")
    if (etiquetas.any { it.equals("Briga", ignoreCase = true) }) add("Briga")
    if (etiquetas.any { it.startsWith("Arremesso", ignoreCase = true) }) add("Arremesso")
    if (etiquetas.any { it.startsWith("Arqueirismo", ignoreCase = true) }) add("Arqueirismo")
}.ifEmpty { WEAPON_HABILIDADES }

// Representa o custo em Recursos como pontos ("•••") — "0" vira "—",
// mesma convenção visual do material de referência do catálogo mundano.
private fun custoRecursosTexto(custo: Int): String = if (custo <= 0) "—" else "•".repeat(custo)

// Diálogo de seleção do catálogo de armas MUNDANAS — pedido explícito do
// usuário. Mesmo padrão visual do catálogo de Artefato, com uma diferença
// central: valida o nível de Recursos do personagem contra o custo do
// item antes de permitir a escolha. A opção "Presente" (também pedida
// explicitamente) ignora essa validação — pra itens dados de presente,
// roubados, herdados etc., que a planilha não deveria bloquear.
@Composable
private fun CatalogoArmaMundanaDialog(
    habilidade: String,
    nivelRecursos: Int,
    onEscolher: (com.example.data.WeaponCatalogMundano.CatalogoArmaMundana, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var presente by remember { mutableStateOf(false) }
    var erroRecursos by remember { mutableStateOf<String?>(null) }
    val candidatasPorPeso = com.example.data.WeaponCatalogMundano
        .candidatasPorHabilidade(habilidade)
        .groupBy { it.peso }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        title = { AppText("Catálogo — $habilidade", color = ExaltedAmber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
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
                    AppText("Seu nível de Recursos: ${custoRecursosTexto(nivelRecursos)} ($nivelRecursos)", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted, modifier = Modifier.padding(bottom = 6.dp))
                }
                erroRecursos?.let {
                    AppText(it, style = MaterialTheme.typography.bodySmall, color = ExaltedDangerCore, modifier = Modifier.padding(bottom = 6.dp))
                }
                if (candidatasPorPeso.isEmpty()) {
                    AppText("Nenhuma arma mundana cadastrada pra $habilidade.", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted)
                } else {
                    listOf("Leve", "Média", "Pesada").forEach { peso ->
                        val candidatas = candidatasPorPeso[peso] ?: return@forEach
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
                                AppText(candidata.nome, style = MaterialTheme.typography.bodyMedium, color = ExaltedOnSurface, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                AppText(custoRecursosTexto(candidata.custoRecursos), style = MaterialTheme.typography.bodyMedium, color = ExaltedGold)
                            }
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
