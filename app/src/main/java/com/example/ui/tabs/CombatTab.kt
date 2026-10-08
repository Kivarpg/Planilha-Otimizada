package com.example.ui.tabs

import com.example.ui.theme.ExaltedOnSurface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.InkButtonVariant
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButton
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Arma
import com.example.model.CaixaVitalidade
import com.example.model.CharacterSheet
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ErgonomicNumericSelector
import com.example.ui.components.IniciativaAjusteButton
import com.example.ui.components.corIniciativaPorValor
import com.example.ui.components.SectionHeader
import com.example.ui.components.GildedCard
import com.example.ui.components.WillpowerTrack
import com.example.ui.theme.ExaltedAlertaFundo
import com.example.ui.theme.ExaltedAlertaTexto
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDanoAgravado
import com.example.ui.theme.ExaltedDanoContundente
import com.example.ui.theme.ExaltedDanoLetal
import com.example.ui.theme.ExaltedDarkBackground
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOutline
import com.example.viewmodel.SheetViewModel

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
private val HEALTH_PENALTY_ORDER = listOf("-0", "-1", "-2", "-4", "Inc")
private val HEALTH_EXTRA_LEVELS = listOf("-0", "-1", "-2", "-4")

// Validação do campo de Iniciativa manual: sinal opcional + até 3 dígitos.
// Compilado uma única vez — o campo roda esse regex a cada tecla digitada.
private val INICIATIVA_REGEX = Regex("^-?\\d{0,3}$")

// Card simples de valor calculado (somente leitura). Aceita um modifier para
// permitir uso lado a lado (peso) em blocos de 2 colunas, evitando ocupar uma
// linha inteira para um único número.
// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
private fun ValorCalculadoCard(label: String, valor: Int, modifier: Modifier = Modifier) {
    GildedCard(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedOutline),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = ExaltedAmber.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small,
                border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedAmber.copy(alpha = 0.5f))
            ) {
                AppText(
                    text = "$valor",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExaltedAmber,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CombatTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    var showVitalitySettingsDialog by remember { mutableStateOf(false) }
    var boxToDelete by remember { mutableStateOf<CaixaVitalidade?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(5).exaltedContentStage(5)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Essência")

        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(5),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = "Essência Permanente",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExaltedAccentBright
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (nivel in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (nivel <= sheet.essencia) essenciaGradientColor(nivel) else ExaltedDarkBackground.copy(alpha = 0.85f))
                                .border(1.dp, if (nivel <= sheet.essencia) ExaltedGold else ExaltedAmber.copy(alpha = 0.45f), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))

                val motesPessoaisMax = sheet.motesPessoaisMax()
                val motesPerifericosMax = sheet.motesPerifericosMax()

                AppText(
                    text = "Motes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ExaltedAmber,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compactMotes = maxWidth < 430.dp
                    val arrangement = Arrangement.spacedBy(8.dp)
                    if (compactMotes) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = arrangement) {
                            ErgonomicNumericSelector(
                                label = "Motes Pessoais",
                                value = sheet.motesPessoaisDisponiveis(),
                                minVal = 0,
                                maxVal = (motesPessoaisMax - sheet.motesPessoaisComitados).coerceAtLeast(0),
                                displayMax = motesPessoaisMax,
                                onValueChange = { novoDisponivel -> viewModel.updateMotesPessoaisGastos(motesPessoaisMax - novoDisponivel) },
                                modifier = Modifier.fillMaxWidth(),
                                centerLabel = true, buttonsOnSameSide = true, showSpentBelow = true,
                                markerFontSize = 28.sp, sameSideButtonSizeOverride = 34.dp
                            )
                            ErgonomicNumericSelector(
                                label = "Motes Periféricos",
                                value = sheet.motesPerifericosDisponiveis(),
                                minVal = 0,
                                maxVal = (motesPerifericosMax - sheet.motesPerifericosComitados).coerceAtLeast(0),
                                displayMax = motesPerifericosMax,
                                onValueChange = { novoDisponivel -> viewModel.updateMotesPerifericosGastos(motesPerifericosMax - novoDisponivel) },
                                modifier = Modifier.fillMaxWidth(),
                                centerLabel = true, buttonsOnSameSide = true, showSpentBelow = true,
                                markerFontSize = 28.sp, sameSideButtonSizeOverride = 34.dp
                            )
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                            ErgonomicNumericSelector(
                                label = "Motes Pessoais", value = sheet.motesPessoaisDisponiveis(), minVal = 0,
                                maxVal = (motesPessoaisMax - sheet.motesPessoaisComitados).coerceAtLeast(0), displayMax = motesPessoaisMax,
                                onValueChange = { novoDisponivel -> viewModel.updateMotesPessoaisGastos(motesPessoaisMax - novoDisponivel) },
                                modifier = Modifier.weight(1f), centerLabel = true, buttonsOnSameSide = true, showSpentBelow = true,
                                markerFontSize = 28.sp, sameSideButtonSizeOverride = 34.dp
                            )
                            ErgonomicNumericSelector(
                                label = "Motes Periféricos", value = sheet.motesPerifericosDisponiveis(), minVal = 0,
                                maxVal = (motesPerifericosMax - sheet.motesPerifericosComitados).coerceAtLeast(0), displayMax = motesPerifericosMax,
                                onValueChange = { novoDisponivel -> viewModel.updateMotesPerifericosGastos(motesPerifericosMax - novoDisponivel) },
                                modifier = Modifier.weight(1f), centerLabel = true, buttonsOnSameSide = true, showSpentBelow = true,
                                markerFontSize = 28.sp, sameSideButtonSizeOverride = 34.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))
                ValorCalculadoCard(label = "Motes Comitados", valor = sheet.comitamentoTotalCalculado())
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        SectionHeader(title = "Força de vontade")
        WillpowerTrack(
            valor = sheet.forcaVontadeBase,
            usados = sheet.forcaVontadeUsados,
            planilhaConcluida = sheet.planilhaConcluida,
            onValorChange = { viewModel.updateForcaVontadeBase(it) },
            onToggleUsado = { viewModel.toggleForcaVontadeUsado(it) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        AppText(
            text = "Contador de iniciativa",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(5),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val iniciativaAtual = sheet.iniciativaValor ?: 0
                var iniciativaTexto by remember(sheet.iniciativaValor) {
                    mutableStateOf(if (sheet.iniciativaValor != null) iniciativaAtual.toString() else "0")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(-1, -5, -10).forEach { delta ->
                            IniciativaAjusteButton(delta = delta, onClick = { viewModel.ajustarIniciativa(delta) }, width = 88.dp)
                        }
                    }
                    OutlinedTextField(
                            value = iniciativaTexto,
                            onValueChange = { novoTexto ->
                                if (novoTexto.matches(INICIATIVA_REGEX)) {
                                    iniciativaTexto = novoTexto
                                    novoTexto.toIntOrNull()?.let(viewModel::updateIniciativa)
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(84.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ExaltedAccentBright,
                                unfocusedBorderColor = ExaltedOutline.copy(alpha = 0.55f),
                                focusedLabelColor = ExaltedAccentBright,
                                unfocusedLabelColor = ExaltedMuted,
                                cursorColor = ExaltedGold,
                                focusedTextColor = corIniciativaPorValor(iniciativaAtual),
                                unfocusedTextColor = corIniciativaPorValor(iniciativaAtual),
                                focusedContainerColor = ExaltedDarkSurface,
                                unfocusedContainerColor = ExaltedDarkSurface,
                                focusedPlaceholderColor = ExaltedMuted,
                                unfocusedPlaceholderColor = ExaltedMuted
                            )
                        )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(1, 5, 10).forEach { delta ->
                            IniciativaAjusteButton(delta = delta, onClick = { viewModel.ajustarIniciativa(delta) }, width = 88.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))

                if (sheet.temTagAtordoado()) {
                    Surface(
                        color = ExaltedAlertaFundo,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedAlertaTexto)
                    ) {
                        AppText(
                            text = "Atordoado",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ExaltedAlertaTexto,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                }

                InkButton(label = "Limpar combate", onClick = { viewModel.limparCombate() }, customWidth = 160.dp, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Ataques: espelha automaticamente as armas cadastradas na aba
        // Equipamentos, mas só as que estão de fato prontas para uso em
        // combate — equipadas, e no caso de Artefatos, também comitadas
        // (sem comitamento, o Artefato não tem seus poderes ativos).
        // Ordem cronológica de cadastro, com ataques desarmados sempre no topo.
        SectionHeader(title = "Ataques")
        val ataques = remember(sheet.weapons) {
            sheet.weapons
                .filter { w -> w.equipada && (w.tipoArma != "Artefato" || w.motesPessoaisComitados > 0 || w.motesPerifericosComitados > 0) }
                .sortedByDescending { it.ataqueDesarmado }
        }
        if (ataques.isEmpty()) {
            AppText(
                text = "Nenhuma arma registrada",
                style = MaterialTheme.typography.bodySmall,
                color = ExaltedMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ataques.forEach { arma -> AtaqueCard(arma = arma) }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bloco principal de combate: agrupa tematicamente todos os valores
        // calculados relacionados a defesa e ações de combate/sociais em pares,
        // evitando que cada valor ocupe uma linha inteira sozinho. Arma e
        // Armadura são cadastradas na aba "Equipamentos"; os valores abaixo já
        // refletem automaticamente o equipamento comitado.
        SectionHeader(title = "Defesas")
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(5),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValorCalculadoCard(label = "Juntar-se à Batalha", valor = sheet.juntarBatalhaCalculado(), modifier = Modifier.weight(1f))
                    ValorCalculadoCard(label = "Aparar", valor = sheet.apararCalculado(), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValorCalculadoCard(label = "Evasão", valor = sheet.evasaoCalculada(), modifier = Modifier.weight(1f))
                    ValorCalculadoCard(label = "Absorção Total", valor = sheet.absorcaoTotalCalculada(), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValorCalculadoCard(label = "Astúcia", valor = sheet.astuciaCalculada(), modifier = Modifier.weight(1f))
                    ValorCalculadoCard(label = "Perseverança", valor = sheet.perseverancaCalculada(), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValorCalculadoCard(label = "Investida", valor = sheet.investidaCalculada(), modifier = Modifier.weight(1f))
                    ValorCalculadoCard(label = "Desengajamento", valor = sheet.desengajamentoCalculado(), modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        SectionHeader(title = "Trilha de vitalidade")
        Spacer(modifier = Modifier.height(18.dp))

        val gruposPorPenalidade = remember(sheet.healthBoxes) {
            // Uma única passagem e cinco grupos fixos. O índice da penalidade
            // é pequeno e fechado; não há necessidade de criar Map/Map.Entry
            // temporários para cada recomposição.
            val grupos = Array(HEALTH_PENALTY_ORDER.size) { ArrayList<CaixaVitalidade>() }
            sheet.healthBoxes.forEach { caixa ->
                val indice = when (caixa.penalidade) {
                    "-0" -> 0
                    "-1" -> 1
                    "-2" -> 2
                    "-4" -> 3
                    "Inc" -> 4
                    else -> -1
                }
                if (indice >= 0) grupos[indice].add(caixa)
            }
            HEALTH_PENALTY_ORDER.zip(grupos.map { it.toList() }).toMap()
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(0.94f)) {
            val labelWidth = 40.dp
            val boxSize = 38.dp
            val gap = 6.dp
            val availableForBoxes = (maxWidth - labelWidth).value.coerceAtLeast(boxSize.value)
            val maxPerRow = ((availableForBoxes + gap.value) / (boxSize.value + gap.value)).toInt().coerceAtLeast(1)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HEALTH_PENALTY_ORDER.forEach { penalidade ->
                    val caixas = gruposPorPenalidade[penalidade] ?: emptyList()
                    if (caixas.isNotEmpty()) {
                        caixas.chunked(maxPerRow).forEachIndexed { rowIndex, rowBoxes ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (rowIndex == 0) {
                                    com.example.ui.components.AutoSizeText(
                                        text = if (penalidade == "Inc") "Inc." else penalidade,
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                        maxFontSize = MaterialTheme.typography.titleMedium.fontSize, minFontSize = 10.sp,
                                        color = ExaltedGold, modifier = Modifier.width(labelWidth)
                                    )
                                } else Spacer(modifier = Modifier.width(labelWidth))
                                Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                                    rowBoxes.forEach { box ->
                                        val bloqueadaParaRemocao = box.isPermanente || box.origemAutomatica != null
                                        val (damageText, damageColor) = when (box.tipoDano) {
                                            1 -> "/" to ExaltedDanoContundente
                                            2 -> "X" to ExaltedDanoLetal
                                            3 -> "*" to ExaltedDanoAgravado
                                            else -> "" to ExaltedMuted
                                        }
                                        val ehExtra = !box.isPermanente
                                        Box(
                                            modifier = Modifier.size(boxSize).clip(RoundedCornerShape(6.dp)).background(ExaltedDarkSurface)
                                                .border(if (ehExtra) 2.dp else 1.dp, if (ehExtra) ExaltedAmber else damageColor.takeIf { box.tipoDano != 0 } ?: ExaltedGold.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                .pointerInput(box.id) { detectTapGestures(onTap = { viewModel.cycleHealthDamage(box.id) }, onLongPress = { if (!bloqueadaParaRemocao) boxToDelete = box }) }
                                                .feedbackOnPress(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppText(damageText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = damageColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Ações da Vitalidade: Adicionar à esquerda e Limpar à direita,
        // mantendo a trilha livre de botões e seguindo a leitura visual do card.
        Row(
            modifier = Modifier.fillMaxWidth(0.94f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InkButton(
                label = "Adicionar",
                onClick = { showVitalitySettingsDialog = true },
                customWidth = 150.dp,
                size = InkButtonSize.Small
            )
            InkButton(
                label = "Limpar",
                onClick = { viewModel.clearHealthDamage() },
                customWidth = 150.dp,
                size = InkButtonSize.Small,
                variant = InkButtonVariant.Secondary
            )
        }
    }

    if (showVitalitySettingsDialog) {
        ConfiguracoesVitalidadeDialog(
            sheet = sheet,
            viewModel = viewModel,
            onDismiss = { showVitalitySettingsDialog = false }
        )
    }

    boxToDelete?.let { box ->
        ConfirmDeleteDialog(
            itemTitle = "Caixa Extra (${box.penalidade})",
            onConfirm = {
                viewModel.removeExtraHealthBox(box.id)
                boxToDelete = null
            },
            onDismiss = { boxToDelete = null }
        )
    }
}

// Extraído do corpo de CombatTab (refatoração de organização — pedido
// explícito do usuário, sem mudança de comportamento). Diálogo
// autocontido, recebe sheet/viewModel/onDismiss como parâmetros.
@Composable
private fun ConfiguracoesVitalidadeDialog(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText("Configurações da Vitalidade", color = ExaltedAccentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val extras = remember(sheet.healthBoxes) {
                    sheet.healthBoxes.filter { !it.isPermanente && it.origemAutomatica == null }
                }
                if (extras.isNotEmpty()) {
                    AppText("Caixas extras atuais", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                    extras.forEach { box ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppText("Penalidade ${box.penalidade}", color = MaterialTheme.colorScheme.onSurface)
                            InkButton(label = "Remover", onClick = { viewModel.removeExtraHealthBox(box.id) }, size = InkButtonSize.Small, variant = InkButtonVariant.Danger)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                AppText("Adicionar novo nível", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                HEALTH_EXTRA_LEVELS.forEach { nivel ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .feedbackClickable { viewModel.addExtraHealthBox(nivel) },
                        color = ExaltedDarkSurface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        AppText(
                            nivel,
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            InkButton(label = "Fechar", onClick = onDismiss, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

// Card compacto de um Ataque, espelhando os dados da arma cadastrada na
// aba Equipamentos (somente leitura — a edição continua em Equipamentos).
