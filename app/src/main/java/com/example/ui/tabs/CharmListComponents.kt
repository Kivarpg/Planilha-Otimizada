package com.example.ui.tabs
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress
import com.example.ui.components.feedbackCombinedClickable

import androidx.compose.foundation.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.EncantoComElegibilidade
import com.example.viewmodel.SheetViewModel

@Composable
internal fun AtributoLunarCharmsPopup(
    atributo: String,
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit,
    onShowDetail: (EncantoSolarDefinition) -> Unit
) {
    val subdivisoes = remember(atributo) { com.example.data.LunarCharmHierarchy.subdivisoesDe(atributo) }
    val adquiridoPorNome = remember(sheet.charms) {
        buildMap<String, Encanto> {
            sheet.charms.forEach { charm ->
                putIfAbsent(EncantosSolaresCatalog.normalize(charm.nome), charm)
            }
        }
    }
    // "Universal" não tem subdivisão — mostra os Encantos direto, sem
    // nível de agrupamento extra.
    var subdivisaoExpandida by remember(atributo) { mutableStateOf<String?>(if (subdivisoes.isEmpty()) "" else null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText(atributo, color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (subdivisoes.isEmpty()) {
                    // Universal: lista direta, sem gaveta de subdivisão.
                    val itens = remember(sheet, atributo) {
                        viewModel.encantosLunaresPorAtributoESubdivisaoComElegibilidade(atributo, null)
                    }
                    if (itens.isEmpty()) {
                        AppText(
                            "Nenhum Encanto cadastrado para este Atributo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExaltedMuted,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        itens.forEach { item ->
                            CharmEligibilityRow(
                                item = item,
                                onToggle = { marcado ->
                                    if (marcado) viewModel.addCharmFromDefinition(item.definicao)
                                    else {
                                        val adquirido = adquiridoPorNome[EncantosSolaresCatalog.normalize(item.definicao.nome)]
                                        adquirido?.let { viewModel.removeCharm(it.id) }
                                    }
                                },
                                onShowDetail = { onShowDetail(item.definicao) }
                            )
                        }
                    }
                } else {
                    subdivisoes.forEach { subdivisao ->
                        val expandida = subdivisaoExpandida == subdivisao
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .feedbackClickable { subdivisaoExpandida = if (expandida) null else subdivisao }
                        ) {
                            AppText(if (expandida) "▾" else "▸", color = ExaltedStructuralMetalShine, modifier = Modifier.padding(end = 6.dp))
                            AppText(
                                subdivisao,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ExaltedAccentBright
                            )
                        }
                        if (expandida) {
                            val itens = remember(sheet, atributo, subdivisao) {
                                viewModel.encantosLunaresPorAtributoESubdivisaoComElegibilidade(atributo, subdivisao)
                            }
                            if (itens.isEmpty()) {
                                AppText(
                                    "Nenhum Encanto cadastrado nesta subdivisão.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExaltedMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itens.forEach { item ->
                                        CharmEligibilityRow(
                                            item = item,
                                            onToggle = { marcado ->
                                                if (marcado) viewModel.addCharmFromDefinition(item.definicao)
                                                else {
                                                    val adquirido = adquiridoPorNome[EncantosSolaresCatalog.normalize(item.definicao.nome)]
                                                    adquirido?.let { viewModel.removeCharm(it.id) }
                                                }
                                            },
                                            onShowDetail = { onShowDetail(item.definicao) }
                                        )
                                    }
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
internal fun HabilidadeCharmsPopup(
    habilidade: String,
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit,
    onShowDetail: (EncantoSolarDefinition) -> Unit
) {
    val itens = remember(sheet, habilidade) { viewModel.encantosDaHabilidadeComElegibilidade(habilidade) }
    val adquiridoPorNome = remember(sheet.charms) {
        buildMap<String, Encanto> {
            sheet.charms.forEach { charm ->
                putIfAbsent(EncantosSolaresCatalog.normalize(charm.nome), charm)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText(habilidade, color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true)
        },
        text = {
            if (itens.isEmpty()) {
                AppText(
                    "Nenhum Encanto cadastrado para esta habilidade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itens.forEach { item ->
                        CharmEligibilityRow(
                            item = item,
                            onToggle = { marcado ->
                                if (marcado) viewModel.addCharmFromDefinition(item.definicao)
                                else {
                                    val adquirido = adquiridoPorNome[EncantosSolaresCatalog.normalize(item.definicao.nome)]
                                    adquirido?.let { viewModel.removeCharm(it.id) }
                                }
                            },
                            onShowDetail = { onShowDetail(item.definicao) }
                        )
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
internal fun CharmEligibilityRow(
    item: EncantoComElegibilidade,
    onToggle: (Boolean) -> Unit,
    onShowDetail: () -> Unit,
    mostrarHabilidade: Boolean = false,
    onIncrement: () -> Unit = { onToggle(true) },
    onDecrement: () -> Unit = { onToggle(false) }
) {
    val def = item.definicao
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (item.elegivel) ExaltedDarkSurface else ExaltedDarkSurface.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (item.elegivel) ExaltedStructuralMetal.copy(alpha = 0.52f) else ExaltedMuted.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.maximo > 1 && (item.elegivel || item.quantidade > 0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val podeDiminuir = item.quantidade > 0
                    val podeAumentar = item.quantidade < item.maximo && item.elegivel
                    InkButton(
                        onClick = onDecrement,
                        enabled = podeDiminuir,
                        modifier = Modifier.size(40.dp)) {
                        AppText("−", color = if (podeDiminuir) ExaltedStructuralMetalShine else ExaltedMuted, fontWeight = FontWeight.Bold)
                    }
                    AppText(
                        text = "${item.quantidade}/${item.maximo}",
                        color = if (item.quantidade > 0) ExaltedAccentBright else ExaltedMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    InkButton(
                        onClick = onIncrement,
                        enabled = podeAumentar,
                        modifier = Modifier.size(40.dp)) {
                        AppText("+", color = if (podeAumentar) ExaltedStructuralMetalShine else ExaltedMuted, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (item.elegivel) {
                Checkbox(
                    checked = item.jaAdquirido,
                    onCheckedChange = onToggle,
                    colors = CheckboxDefaults.colors(checkedColor = ExaltedAccentBright, uncheckedColor = ExaltedStructuralMetal)
                ,
    modifier = Modifier.feedbackOnPress(enabled = true)
)
            } else {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Requisitos não atendidos", tint = ExaltedMuted, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = def.nome,
                    color = if (item.elegivel) MaterialTheme.colorScheme.onSurface else ExaltedMuted,
                    fontWeight = if (item.jaAdquirido) FontWeight.Bold else FontWeight.Normal
                )
                if (mostrarHabilidade) {
                    AppText(
                        text = def.habilidade,
                        style = MaterialTheme.typography.labelSmall,
                        color = ExaltedMuted
                    )
                }
            }
            InkButton(onClick = onShowDetail, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = "Ver detalhes", tint = ExaltedStructuralMetalShine, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun CharmAbilityButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.feedbackCombinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongPress
        ),
        // Mantém cada caixa visualmente separada do fundo da tela.
        // O estado selecionado é indicado principalmente pelo contorno/acento,
        // sem clarear o painel a ponto de se confundir com o backdrop.
        color = when {
            !enabled -> ExaltedBlack.copy(alpha = 0.72f)
            selected -> ExaltedDarkSurface
            else -> ExaltedBlack
        },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) ExaltedAccentBright else ExaltedStructuralMetal.copy(alpha = 0.45f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // AutoSizeText desenha duas camadas do mesmo glifo:
            // Stroke(ExaltedTextStroke) + preenchimento. É contorno real,
            // não shadow, e mantém autoajuste para nomes longos.
            AutoSizeText(
                text = text,
                color = if (!enabled) ExaltedMuted else if (selected) ExaltedAccentBright else ExaltedStructuralMetalShine,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.titleSmall,
                maxFontSize = 13.sp,
                minFontSize = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun AcquiredCharmCard(
    charm: Encanto,
    quantity: Int = 1,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .pointerInput(charm.id) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
            .feedbackOnPress(),
        color = ExaltedDarkSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (isPinned) ExaltedAccentBright else ExaltedStructuralMetal.copy(alpha = 0.55f))
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AppText(
                    "${charm.nome}${if (quantity > 1) " (x$quantity)" else ""} (${charm.nomeIngles.ifBlank { "—" }})",
                    fontWeight = FontWeight.Bold,
                    color = ExaltedAccentBright,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.58f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
                AppText(
                    if (charm.circulo.isNotBlank()) "Feitiço — Círculo ${charm.circulo}"
                    else "Mins: " + charm.mins.ifBlank { "${charm.habilidadeVinculada} ${charm.minHabilidade}, Essência ${charm.minEssencia}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedMuted
                )
            }
            // Coração: fixa o Encanto no início da gaveta (até 5 por gaveta).
            // Desenhado via Canvas (curvas Bézier) em vez de ícone de
            // biblioteca — mesmo padrão de CustomAbilityIcons.kt.
            InkButton(
                onClick = onTogglePin,
                modifier = (Modifier.size(40.dp).semantics {
                    contentDescription = if (isPinned) "Desfixar Encanto" else "Fixar Encanto"
                    role = Role.Checkbox
                    stateDescription = if (isPinned) "Fixado no topo da gaveta" else "Não fixado"
                })) {
                val corCoracao = if (isPinned) ExaltedDangerCore else ExaltedMuted
                androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                    val w = size.width
                    val h = size.height
                    val caminho = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.5f, h * 0.85f)
                        cubicTo(w * 0.5f, h * 0.85f, w * 0.15f, h * 0.55f, w * 0.15f, h * 0.35f)
                        cubicTo(w * 0.15f, h * 0.15f, w * 0.35f, h * 0.05f, w * 0.5f, h * 0.25f)
                        cubicTo(w * 0.65f, h * 0.05f, w * 0.85f, h * 0.15f, w * 0.85f, h * 0.35f)
                        cubicTo(w * 0.85f, h * 0.55f, w * 0.5f, h * 0.85f, w * 0.5f, h * 0.85f)
                        close()
                    }
                    if (isPinned) {
                        drawPath(caminho, color = corCoracao)
                    } else {
                        drawPath(caminho, color = corCoracao, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f))
                    }
                }
            }
        }
    }
}
