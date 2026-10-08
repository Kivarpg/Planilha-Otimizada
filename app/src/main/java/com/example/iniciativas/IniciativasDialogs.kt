package com.example.iniciativas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.InkButtonVariant
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DialogHistoricoCombates(historico: List<HistoricoCombateEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Histórico de combates", color = ExaltedAccentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            if (historico.isEmpty()) {
                AppText("Nenhum combate encerrado ainda.", color = ExaltedMuted)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    historico.forEachIndexed { indice, registro ->
                        val dataFormatada = remember(registro.dataHora) {
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.Builder().setLanguage("pt").setRegion("BR").build())
                            sdf.format(java.util.Date(registro.dataHora))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExaltedDarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            AppText(
                                "Combate ${indice + 1} — $dataFormatada",
                                style = MaterialTheme.typography.labelMedium,
                                color = ExaltedAmber
                            )
                            AppText(
                                "${registro.rodadasTotais} rodada(s) · ${registro.turnStatus.name} / ${registro.combatStatus.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExaltedMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            registro.participantes.forEach { p ->
                                AppText(
                                    "${p.nome} — Iniciativa final: ${p.iniciativaFinal}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ExaltedOnSurface
                                )
                            }
                            // CORREÇÃO — mostrava só o último turno
                            // (registro.logTurno, um item só). Agora lista
                            // TODOS os turnos resolvidos no combate
                            // (registro.eventosLog), em ordem, deixando
                            // explícito quem atacou, quem foi atacado e
                            // quantos pontos de Iniciativa foram perdidos
                            // (transferidos) em cada um.
                            val turnosComConteudo = registro.eventosLog.filter {
                                it.combatenteAtivoNome != null || it.alvoNome != null
                            }
                            if (turnosComConteudo.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                AppText(
                                    "Turnos:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ExaltedGold
                                )
                                turnosComConteudo.forEach { log ->
                                    val cabecalho = buildString {
                                        append("Rodada ${log.rodada}: ")
                                        append(log.combatenteAtivoNome ?: "Atacante não identificado")
                                        log.alvoNome?.let { append(" → ").append(it) }
                                        if (log.tipoAtaque == TipoAtaque.DECISIVO) {
                                            append(" · ATAQUE DECISIVO")
                                        } else if (log.tipoAtaque == TipoAtaque.FULMINANTE) {
                                            append(" · Ataque Fulminante")
                                        }
                                    }
                                    AppText(
                                        cabecalho,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (log.tipoAtaque == TipoAtaque.DECISIVO) ExaltedAmber else ExaltedOnSurface
                                    )
                                    log.iniciativaPerdidaPorId.forEach { (id, pontos) ->
                                        val nome = registro.participantes
                                            .firstOrNull { participante -> participante.id == id }
                                            ?.nome
                                            ?: if (id == log.alvoId) log.alvoNome else null
                                            ?: if (id == log.combatenteAtivoId) log.combatenteAtivoNome else null
                                            ?: id
                                        AppText(
                                            "  $nome perdeu $pontos ponto(s) de Iniciativa",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ExaltedMuted
                                        )
                                    }
                                    val detalhes = buildList {
                                        log.resultadoSucesso?.let { add(if (it) "Sucesso" else "Falha") }
                                        log.motivoEncerramento?.let { add("Motivo: $it") }
                                    }
                                    if (detalhes.isNotEmpty()) {
                                        AppText(
                                            "  ${detalhes.joinToString(" · ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ExaltedMuted
                                        )
                                    }
                                }
                            }
                            if (registro.eventosCrash.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                AppText(
                                    "Atordoamento de Iniciativa: ${registro.eventosCrash.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExaltedMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            InkButton(label = "Fechar", onClick = onDismiss, size = InkButtonSize.Small)
        }
    )
}

@Composable
internal fun DialogAdicionar(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var iniciativa by remember { mutableIntStateOf(3) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Adicionar combatente", color = ExaltedAccentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(40) },
                    label = { AppText("Nome") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExaltedAccentBright,
                        focusedTextColor = ExaltedOnSurface,
                        unfocusedTextColor = ExaltedOnSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                AppText("Iniciativa", style = MaterialTheme.typography.labelMedium, color = ExaltedAmber)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InkButton(onClick = { iniciativa = (iniciativa - 1).coerceAtLeast(-20) }
) {
                        Icon(Icons.Default.Remove, null, tint = ExaltedAmber)
                    }
                    AppText(
                        iniciativa.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ExaltedAccentBright,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center
                    )
                    InkButton(onClick = { iniciativa = (iniciativa + 1).coerceAtMost(99) }
) {
                        Icon(Icons.Default.Add, null, tint = ExaltedAmber)
                    }
                }
            }
        },
        dismissButton = {
            InkButton(label = "Adicionar", onClick = { if (nome.isNotBlank()) onConfirm(nome.trim(), iniciativa) }, enabled = nome.isNotBlank(), size = InkButtonSize.Small)
        },
        confirmButton = {
            InkButton(label = "Cancelar", onClick = onDismiss, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
        }
    )
}


/**
 * Confirmação de Término (PDF §10).
 * Se houver resolução pendente, o log registrará turnStatus = INCOMPLETO.
 */
@Composable
internal fun DialogConfirmarTermino(
    resolucaoPendente: Boolean,
    onCancelar: () -> Unit,
    onConfirmar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            AppText(
                "Deseja realmente terminar o combate?",
                color = ExaltedAccentBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                AppText(
                    "O estado atual será salvo no log.",
                    color = ExaltedOnSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (resolucaoPendente) {
                    Spacer(Modifier.height(8.dp))
                    AppText(
                        "Há resolução pendente: o turno será registrado como INCOMPLETO.",
                        color = ExaltedAmber,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        dismissButton = {
            InkButton(label = "Término", onClick = onConfirmar, size = InkButtonSize.Small)
        },
        confirmButton = {
            InkButton(label = "Cancelar", onClick = onCancelar, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
        }
    )
}
