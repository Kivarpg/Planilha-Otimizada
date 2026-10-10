package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.InteractionFeedback
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDangerCore
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.viewmodel.SheetViewModel

// Diálogos extraídos de SheetScreen.kt (arquivo ficou grande demais —
// mesma reorganização já feita com CommonComponents.kt). Cada diálogo
// recebe seu próprio estado de visibilidade por parâmetro (hasteado no
// chamador, já que o GATILHO de abrir cada um vive em outro lugar da
// tela); qualquer estado transitório interno ao diálogo (texto digitado,
// erro, carregamento) fica como remember{} dentro do próprio composable,
// não precisa ser hasteado.
//
// Reorganizado de novo (2026-09, refatoração estrutural): os diálogos de
// arquivo/persistência (salvar, carregar, importar, exportar, backup,
// nova planilha) viraram FileManagementDialogs.kt — concern genuinamente
// separado dos 4 que ficaram aqui, que são sobre validar/editar o
// ESTADO da planilha aberta (Configurações, reversão de conclusão, erro de
// comitamento, quebra de Mérito pendente), não sobre arquivos.

@Composable
fun SettingsDialog(show: Boolean, onDismiss: () -> Unit, viewModel: SheetViewModel, onAbrirLogErros: () -> Unit, modifier: Modifier = Modifier) {
    if (!show) return
    val context = LocalContext.current
    var hapticEnabled by remember { mutableStateOf(InteractionFeedback.isHapticEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(InteractionFeedback.isSoundEnabled(context)) }
    val currentStyle by viewModel.ratingStyle.collectAsState()
    val sheetAtual by viewModel.sheetState.collectAsState()
    // Limite legado: validar em modo de tela dividida e com IME antes de alterar.
    val settingsContentHeight = (LocalConfiguration.current.screenHeightDp - 240).coerceIn(160, 500).dp
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        icon = {
            Icon(imageVector = Icons.Outlined.Settings, contentDescription = null, tint = ExaltedAccentBright)
        },
        title = {
            com.example.ui.components.AutoSizeText(
                text = "Configurações",
                color = ExaltedAccentBright,
                fontWeight = FontWeight.Bold,
                maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = settingsContentHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppText(
                    "Estilo de avaliação (Atributos, Habilidades e Méritos)",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedGold
                )
                listOf(
                    com.example.model.RatingStyle.DIAMOND to "Modo Clássico (trilha de círculos ●●○○○)",
                    com.example.model.RatingStyle.STEPPER to "Modo Compacto (botões −/número/+)"
                ).forEach { (style, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .feedbackClickable { viewModel.setRatingStyle(style) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = currentStyle == style,
                            onClick = null,
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = ExaltedAccentBright)
                        
)
                        AppText(label, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Idioma do app — abaixo do modo compacto (pedido explícito).
                // English permanece visível porém desabilitado até a localização existir.
                AppText(
                    "Idioma",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedGold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = true,
                        onClick = { },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = ExaltedAccentBright)
                    
)
                    AppText("Português", color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = false,
                        onClick = { },
                        enabled = false,
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = ExaltedAccentBright)
                    ,
    modifier = Modifier.feedbackOnPress(enabled = false)
)
                    AppText("English", color = ExaltedMuted)
                }

                androidx.compose.material3.HorizontalDivider(color = ExaltedMuted.copy(alpha = 0.3f))

                AppText(
                    "Feedback de interação",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExaltedGold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { enabled ->
                            hapticEnabled = enabled
                            InteractionFeedback.setHapticEnabled(context, enabled)
                            // Ao ativar, confirma a preferência com um único pulso.
                            // Ao desativar, não vibra depois que a opção foi desligada.
                            if (enabled) InteractionFeedback.perform(context)
                        }
                    
)
                    AppText("Vibração ao tocar", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Switch(
                        checked = soundEnabled,
                        onCheckedChange = { enabled ->
                            soundEnabled = enabled
                            InteractionFeedback.setSoundEnabled(context, enabled)
                            if (enabled) InteractionFeedback.perform(context)
                        }
                    
)
                    AppText("Som ao tocar", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                }

                androidx.compose.material3.HorizontalDivider(color = ExaltedMuted.copy(alpha = 0.3f))

                // Modo Livre — movido do menu de 3 pontos pra cá, pedido
                // explícito do usuário. Não afeta a Aba 11 (Encontros),
                // que já tem suas próprias regras de geração, independentes
                // de planilhaConcluida/validações da planilha principal.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .feedbackClickable { viewModel.toggleModoLivre() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = sheetAtual.modoLivre,
                        onCheckedChange = null,
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = ExaltedAccentBright)
                    
)
                    AppText("Modo Livre", color = MaterialTheme.colorScheme.onSurface)
                }

                androidx.compose.material3.HorizontalDivider(color = ExaltedMuted.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .feedbackClickable { onDismiss(); onAbrirLogErros() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = ExaltedMuted, modifier = Modifier.padding(horizontal = 12.dp))
                    AppText("Ver Log de Erros", color = MaterialTheme.colorScheme.onSurface)
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
fun ReversionConfirmDialog(viewModel: SheetViewModel, modifier: Modifier = Modifier) {
    val show by viewModel.showReversionConfirm.collectAsState()
    if (!show) return
    AlertDialog(
        onDismissRequest = { viewModel.cancelarReversaoPlanilhaConcluida() },
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        icon = {
            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = ExaltedDangerCore)
        },
        title = {
            AppText(
                forceStroke = true,
                text = "Desmarcar \"Planilha concluída\"?",
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            AppText(
                text = "Ao desmarcar \"Planilha concluída\", todos os avanços comprados com pontos de Experiência serão perdidos. Deseja continuar?",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(
                text = "Desmarcar e perder avanços",
                onClick = { viewModel.confirmarReversaoPlanilhaConcluida() },
                isDanger = true
            )
        },
        confirmButton = {
            com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = { viewModel.cancelarReversaoPlanilhaConcluida() })
        },
        containerColor = ExaltedDarkSurface
    )
}

@Composable
fun CommitmentErrorDialog(viewModel: SheetViewModel, modifier: Modifier = Modifier) {
    val message by viewModel.commitmentError.collectAsState()
    if (message == null) return
    // Mantem a area de aviso rolavel em telas compactas sem modificar a confirmacao.
    val messageHeightLimit = (LocalConfiguration.current.screenHeightDp - 260).coerceIn(120, 420).dp
    AlertDialog(
        onDismissRequest = { viewModel.dismissCommitmentError() },
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        icon = {
            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = ExaltedAmber)
        },
        title = {
            AppText(
                forceStroke = true,
                text = "Aviso",
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = messageHeightLimit).verticalScroll(rememberScrollState())) {
                AppText(text = message ?: "", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(text = "OK", onClick = { viewModel.dismissCommitmentError() })
        },
        containerColor = ExaltedDarkSurface
    )
}

// Mostrada quando reduzir um atributo/habilidade quebraria o
// pré-requisito de um Mérito já adquirido — o usuário vê exatamente o
// que está tirando e o que vai perder, e escolhe confirmar ou cancelar.
// Nada é alterado até essa escolha (ver AttributesActions/
// AbilitiesActions.setAttributeRating/setAbilityRating).
@Composable
fun PendingMeritBreakDialog(viewModel: SheetViewModel, modifier: Modifier = Modifier) {
    val pendente by viewModel.pendingMeritBreak.collectAsState()
    val info = pendente ?: return
    // Preserva a lista rolavel de meritos afetados ate a validacao em dispositivo.
    val affectedMeritsHeightLimit = (LocalConfiguration.current.screenHeightDp - 260).coerceIn(120, 420).dp
    AlertDialog(
        onDismissRequest = { viewModel.cancelarPendingMeritBreak() },
        modifier = modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        icon = {
            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = ExaltedAmber)
        },
        title = {
            AppText(
                forceStroke = true,
                text = "Isso vai remover um Mérito",
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = affectedMeritsHeightLimit).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppText(
                    text = "Você está reduzindo ${info.descricaoMudanca}.",
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppText(
                    text = "Isso deixa de atender o pré-requisito de:",
                    color = MaterialTheme.colorScheme.onSurface
                )
                info.meritosAfetados.forEach { nome ->
                    AppText(text = "• $nome", color = ExaltedDangerCore, fontWeight = FontWeight.Bold)
                }
                AppText(
                    text = "Confirmando, a redução é aplicada e esse(s) Mérito(s) são removidos da planilha.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedMuted
                )
            }
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(text = "OK", onClick = { viewModel.confirmarPendingMeritBreak() })
        },
        confirmButton = {
            com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = { viewModel.cancelarPendingMeritBreak() })
        },
        containerColor = ExaltedDarkSurface
    )
}
