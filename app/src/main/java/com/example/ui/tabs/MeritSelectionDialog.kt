package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.InkButtonSize
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CharacterSheet
import com.example.ui.components.InkButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ExaltedDangerCore
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedSuccess
import com.example.viewmodel.SheetViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun MeritSelectionDialog(
    viewModel: SheetViewModel,
    sheet: CharacterSheet,
    ratingStyle: com.example.model.RatingStyle,
    onDismiss: () -> Unit
) {
    var meritoSelecionado by remember { mutableStateOf<com.example.data.MeritoDefinition?>(null) }
    var custoSelecionado by remember { mutableStateOf<Int?>(null) }
    var erroMensagem by remember { mutableStateOf<String?>(null) }
    var mostrarPersonalizado by remember { mutableStateOf(false) }
    var nomePersonalizado by remember { mutableStateOf("") }
    var nivelPersonalizado by remember { mutableStateOf(1) }
    var temPreRequisitoPersonalizado by remember { mutableStateOf(false) }
    var textoPreRequisitoPersonalizado by remember { mutableStateOf("") }
    var categoriaPersonalizada by remember { mutableStateOf("normal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            val meritoAtual = meritoSelecionado
            AppText(
                when {
                    mostrarPersonalizado -> "Mérito Personalizado"
                    meritoAtual != null -> meritoAtual.nome
                    else -> "Cadastrar Mérito"
                },
                color = ExaltedAccentBright,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            val def = meritoSelecionado
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (mostrarPersonalizado) {
                    androidx.compose.material3.OutlinedTextField(
                        value = nomePersonalizado,
                        onValueChange = com.example.ui.components.rememberTypingFeedback { nomePersonalizado = it.take(60); erroMensagem = null },
                        label = { AppText("Nome do Mérito") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AppText("Graduação:", fontWeight = FontWeight.Bold, color = ExaltedGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { n ->
                            val selecionado = n == nivelPersonalizado
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (selecionado) ExaltedGold else Color.Transparent)
                                    .border(1.5.dp, ExaltedGold, androidx.compose.foundation.shape.CircleShape)
                                    .feedbackClickable { nivelPersonalizado = n },
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(
                                    n.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (selecionado) ExaltedOnSurface else ExaltedGold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AppText("Categoria:", fontWeight = FontWeight.Bold, color = ExaltedGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("normal" to "Normal", "sobrenatural" to "Sobrenatural").forEach { (valor, rotulo) ->
                            val selecionado = categoriaPersonalizada == valor
                            InkButton(label = rotulo, onClick = { categoriaPersonalizada = valor }, selected = selecionado, size = InkButtonSize.Small)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .feedbackClickable { temPreRequisitoPersonalizado = !temPreRequisitoPersonalizado }
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = temPreRequisitoPersonalizado,
                            onCheckedChange = { temPreRequisitoPersonalizado = it }
                        )
                        AppText("Possui pré-requisito?", color = ExaltedOnSurface)
                    }
                    if (temPreRequisitoPersonalizado) {
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = textoPreRequisitoPersonalizado,
                            onValueChange = com.example.ui.components.rememberTypingFeedback { textoPreRequisitoPersonalizado = it.take(200) },
                            label = { AppText("Descreva o pré-requisito") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    erroMensagem?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppText(it, color = ExaltedDangerCore, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (def == null) {
                    SectionHeader(title = "Méritos normais")
                    viewModel.meritosNormais.forEach { m ->
                        AppText(
                            text = m.nome,
                            color = ExaltedOnSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .feedbackClickable {
                                    meritoSelecionado = m
                                    custoSelecionado = null
                                    erroMensagem = null
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionHeader(title = "Méritos sobrenaturais")
                    viewModel.meritosSobrenaturais.forEach { m ->
                        AppText(
                            text = m.nome,
                            color = ExaltedOnSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .feedbackClickable {
                                    meritoSelecionado = m
                                    custoSelecionado = null
                                    erroMensagem = null
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    InkButton(
                        label = "Personalizado",
                        onClick = { mostrarPersonalizado = true },
                        modifier = Modifier.fillMaxWidth(),
                        fillMaxWidth = true
                    )
                } else {
                    com.example.ui.components.JustifiedBodyAppText(
                        text = def.descricao,
                        style = MaterialTheme.typography.bodySmall,
                        color = ExaltedOnSurface
                    )
                    if (def.preRequisitos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AppText("Pré-requisitos:", fontWeight = FontWeight.Bold, color = ExaltedGold)
                        def.preRequisitos.forEach { textoPreReq ->
                            val checagem = com.example.data.checarPreRequisito(textoPreReq, sheet.attributes, sheet.abilities)
                            val cor = when (checagem.atendido) {
                                true -> ExaltedSuccess
                                false -> ExaltedDangerCore
                                null -> ExaltedMuted
                            }
                            val prefixo = when (checagem.atendido) {
                                true -> "✓ "
                                false -> "✗ "
                                null -> "• "
                            }
                            AppText(
                                text = "$prefixo${checagem.textoOriginal}",
                                color = cor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AppText("Custo:", fontWeight = FontWeight.Bold, color = ExaltedGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (def.custosPermitidos.size == 1) {
                        // Quando o Mérito possui apenas um custo possível, não há decisão a ser tomada.
                        // Exibe o custo informativo e deixa apenas a confirmação no rodapé.
                        val custoUnico = def.custosPermitidos.first()
                        AppText(
                            text = if (custoUnico == 0) "Grátis" else "$custoUnico ${if (custoUnico == 1) "ponto" else "pontos"}",
                            color = ExaltedAccentBright,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            def.custosPermitidos.forEach { custo ->
                                val selecionado = custoSelecionado == custo
                                InkButton(label = if (custo == 0) "Grátis" else "$custo ${if (custo == 1) "ponto" else "pontos"}", onClick = { custoSelecionado = custo; erroMensagem = null }, selected = selecionado, size = InkButtonSize.Small)
                            }
                        }
                    }
                    erroMensagem?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppText(it, color = ExaltedDangerCore, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        dismissButton = {
            val def = meritoSelecionado
            if (mostrarPersonalizado) {
                com.example.ui.components.GildedDialogButton(
                    text = "Adicionar",
                    onClick = {
                        if (nomePersonalizado.isBlank()) {
                            erroMensagem = "Digite o nome do Mérito."
                            return@GildedDialogButton
                        }
                        val textoPreReq = if (temPreRequisitoPersonalizado) textoPreRequisitoPersonalizado.trim() else ""
                        if (temPreRequisitoPersonalizado && textoPreReq.isBlank()) {
                            erroMensagem = "Descreva o pré-requisito, ou desmarque a opção."
                            return@GildedDialogButton
                        }
                        when (val resultado = viewModel.addMeritoPersonalizado(nomePersonalizado, nivelPersonalizado, categoriaPersonalizada, textoPreReq)) {
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.Sucesso -> onDismiss()
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.SaldoInsuficiente ->
                                erroMensagem = "Pontos de Bônus insuficientes para esta compra."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.PontosBaseNaoConcluidos ->
                                erroMensagem = "Distribua primeiro os 13 pontos base de Méritos antes de usar os 5 pontos extras."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.CategoriaNaoElegivelParaPontosAdicionais ->
                                erroMensagem = "Os 13 pontos livres de Mérito já foram usados. Os 5 pontos adicionais só valem para: ${com.example.viewmodel.MeritsActions.CATEGORIAS_PONTOS_ADICIONAIS_SANGUE_DRAGAO.joinToString(", ")}."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.LimiteDePontosAdicionaisAtingido ->
                                erroMensagem = "Limite dos 5 pontos adicionais de Mérito atingido."
                            else -> Unit
                        }
                    }
                )
            } else if (def != null) {
                com.example.ui.components.GildedDialogButton(
                    text = if (def.custosPermitidos.size == 1) "Confirmar" else "Adicionar",
                    onClick = {
                        // Para Méritos com custo único, o valor é determinado pelo catálogo.
                        // Para Méritos com múltiplos custos, o usuário precisa escolher.
                        val custo = if (def.custosPermitidos.size == 1) {
                            def.custosPermitidos.first()
                        } else {
                            custoSelecionado
                        }
                        if (custo == null) {
                            erroMensagem = "Escolha o custo antes de adicionar."
                            return@GildedDialogButton
                        }
                        val naoAtendidos = def.preRequisitos.filter {
                            com.example.data.checarPreRequisito(it, sheet.attributes, sheet.abilities).atendido == false
                        }
                        if (naoAtendidos.isNotEmpty()) {
                            erroMensagem = "Pré-requisito não atendido: ${naoAtendidos.joinToString(", ")}"
                            return@GildedDialogButton
                        }
                        when (val resultado = viewModel.addMerit(def.nome, custo)) {
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.Sucesso -> onDismiss()
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.JaAdquirido ->
                                erroMensagem = "Você já possui este Mérito."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.CustoInvalido ->
                                erroMensagem = "Custo inválido para este Mérito."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.SaldoInsuficiente ->
                                erroMensagem = "Pontos de Bônus insuficientes para esta compra."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.PontosBaseNaoConcluidos ->
                                erroMensagem = "Distribua primeiro os 13 pontos base de Méritos antes de usar os 5 pontos extras."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.CategoriaNaoElegivelParaPontosAdicionais ->
                                erroMensagem = "Os 13 pontos livres de Mérito já foram usados. Os 5 pontos adicionais só valem para: ${com.example.viewmodel.MeritsActions.CATEGORIAS_PONTOS_ADICIONAIS_SANGUE_DRAGAO.joinToString(", ")}."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.LimiteDePontosAdicionaisAtingido ->
                                erroMensagem = "Limite dos 5 pontos adicionais de Mérito atingido."
                            is com.example.viewmodel.MeritsActions.ResultadoAdicaoMerito.RestritoAOutroTemplate ->
                                erroMensagem = "Este Mérito não está disponível para este tipo de personagem."
                        }
                    }
                )
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogTextButton(
                text = if (meritoSelecionado != null || mostrarPersonalizado) "Voltar" else "Cancelar",
                onClick = {
                    if (mostrarPersonalizado) {
                        mostrarPersonalizado = false
                        erroMensagem = null
                    } else if (meritoSelecionado != null) {
                        meritoSelecionado = null
                        custoSelecionado = null
                        erroMensagem = null
                    } else {
                        onDismiss()
                    }
                }
            )
        },
        containerColor = ExaltedDarkSurface
    )
}
