package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ============================================================================
// ATENÇÃO: arquivo MISTO — componentes puramente visuais (GildedCard,
// SectionHeader, botões estilizados) convivem aqui com componentes que têm
// comportamento de verdade (RatingControl, CompactAutoSizeField, campos de
// texto com validação). Cada composable abaixo marcado com "// SKIN:" é
// seguro editar/reestilizar sem risco; qualquer coisa SEM essa marcação
// pode ter lógica própria — confira o corpo da função antes de mexer.
// ============================================================================

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltVisualTemplate
import com.example.ui.theme.ExaltedDangerCore
import com.example.ui.theme.ExaltedMetalGoldDeep
import com.example.ui.theme.ExaltedMetalGoldFlash
import com.example.ui.theme.ExaltedMetalGoldShine

@Composable
fun ConfirmDeleteDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = ExaltedDangerCore
            )
        },
        title = {
            AppText(
                forceStroke = true,
                text = "Confirmar exclusão",
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            AppText(
                text = "Deseja realmente remover \"$itemTitle\"?",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        dismissButton = {
            GildedDialogButton(text = "Remover", onClick = onConfirm, isDanger = true)
        },
        confirmButton = {
            GildedDialogTextButton(text = "Cancelar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurface
    )
}

// SKIN: botão de ação primária (confirmar/prosseguir) em pop-ups —
// substitui o Button genérico e chapado usado antes em todo diálogo do
// app. Mesmo padrão visual de ErgonomicMultiWordButton (gradiente
// metálico + borda gradiente + fio de brilho no topo), adaptado pro
// contexto de botão de diálogo (largura flexível, não fixa). isDanger
// troca a família de cor de dourado pra vermelho (família
// ExaltedDanger*), pra ações destrutivas (excluir/remover) sem perder o
// mesmo acabamento — só a paleta muda.
@Composable
fun GildedDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false,
    visualTemplate: ExaltVisualTemplate? = null
) {
    InkButton(
        visualTemplate = visualTemplate,
        label = text,
        onClick = onClick,
        modifier = modifier,
        variant = if (isDanger) InkButtonVariant.Danger else InkButtonVariant.Primary,
        size = InkButtonSize.Small,
        enabled = enabled,
        fillMaxWidth = fillMaxWidth,
    )
}

@Composable
fun GildedDialogTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    fillMaxWidth: Boolean = false
) {
    InkButton(
        label = text,
        onClick = onClick,
        modifier = modifier,
        variant = if (isDanger) InkButtonVariant.Danger else InkButtonVariant.Secondary,
        size = InkButtonSize.Small,
        fillMaxWidth = fillMaxWidth,
    )
}

// SKIN: forma e borda padrão do container de todo AlertDialog do app —
// AlertDialog aceita `modifier`, então basta encadear
// `.then(gildedDialogBorder())` no modifier de cada diálogo pra ganhar a
// borda dourada gradiente, sem precisar reconstruir o componente do zero.
// dialogShape junto do parâmetro `shape =` do AlertDialog (cantos mais
// arredondados que o padrão do Material3, mais alinhado à linguagem
// visual "gema lapidada" do resto do app).
val dialogShape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)

fun gildedDialogBorder(): Modifier = Modifier.border(
    width = 1.5.dp,
    brush = Brush.linearGradient(
        listOf(ExaltedMetalGoldDeep, ExaltedMetalGoldShine, ExaltedMetalGoldFlash, ExaltedMetalGoldShine, ExaltedMetalGoldDeep)
    ),
    shape = dialogShape
)

@Composable
fun SaveValidationModal(
    errors: List<String>,
    onDismiss: () -> Unit,
    onConfirmIncompleteSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(gildedDialogBorder()),
        shape = dialogShape,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = ExaltedAmber
            )
        },
        title = {
            AppText(
                forceStroke = true,
                text = "Planilha incompleta",
                color = ExaltedAmber,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppText(
                    text = "O personagem que você está tentando criar possui informações pendentes. Deseja salvá-lo mesmo assim?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                errors.forEach { err ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        AppText(text = "• ", color = ExaltedAmber, fontWeight = FontWeight.Bold)
                        AppText(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        dismissButton = {
            GildedDialogButton(text = "Salvar mesmo assim", onClick = onConfirmIncompleteSave)
        },
        confirmButton = {
            GildedDialogTextButton(text = "Cancelar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurface
    )
}

