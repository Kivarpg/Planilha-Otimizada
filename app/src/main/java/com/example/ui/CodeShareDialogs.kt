package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedDangerCore
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.viewmodel.SheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun gerarBitmapQrCode(texto: String, tamanhoPx: Int = 512): android.graphics.Bitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val hints = mapOf(
            com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
            com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8",
            com.google.zxing.EncodeHintType.MARGIN to 1
        )
        val matrix = writer.encode(texto, com.google.zxing.BarcodeFormat.QR_CODE, tamanhoPx, tamanhoPx, hints)
        val bitmap = android.graphics.Bitmap.createBitmap(tamanhoPx, tamanhoPx, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until tamanhoPx) {
            for (y in 0 until tamanhoPx) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

// Acima disso, a leitura por câmera fica pouco confiável (QR muito denso)
// — a UI oferece só o texto/copiar nesse caso, sem tentar gerar um QR que
// provavelmente não vai escanear direito. Com a compactação de encantos
// por referência de catálogo (ver PoderesReferenciaCodec), a grande
// maioria das planilhas fica bem abaixo desse teto agora.
private const val LIMITE_CARACTERES_PARA_QR = 800


@Composable
fun CodeExportResultDialog(
    codigo: String?,
    onDismiss: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
    if (codigo == null) return
    val qrBitmap = remember(codigo) {
        if (codigo.length <= LIMITE_CARACTERES_PARA_QR) gerarBitmapQrCode(codigo) else null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText(
                "Código de Compartilhamento",
                color = ExaltedAccentBright,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (qrBitmap != null) {
                    AppText(
                        text = "Escaneie com Carregar → Ler QR Code em outro dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code do código de compartilhamento",
                        modifier = Modifier.size(240.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState())
                        .background(ExaltedDarkSurfaceVariant)
                        .padding(10.dp)
                ) {
                    AppText(codigo, style = MaterialTheme.typography.bodySmall, color = ExaltedOnSurface)
                }
                AppText(
                    text = "${codigo.length} caracteres",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExaltedMuted
                )
            }
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(
                text = "Copiar",
                onClick = {
                    clipboardManager.setText(AnnotatedString(codigo))
                    scope.launch { snackbarHostState.showSnackbar("Código copiado.") }
                }
            )
        },
        confirmButton = {
            com.example.ui.components.GildedDialogTextButton(text = "Fechar", onClick = onDismiss)
        },
        containerColor = ExaltedDarkSurface
    )
}


@Composable
fun CodeImportDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: SheetViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
    if (!show) return
    var codeImportText by remember { mutableStateOf("") }
    var codeImportError by remember { mutableStateOf<String?>(null) }
    var codeImportLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!codeImportLoading) onDismiss() },
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText("Carregar por Código", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = codeImportText,
                    onValueChange = {
                        codeImportText = it
                        codeImportError = null
                    },
                    label = { AppText("Código") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    isError = codeImportError != null,
                    enabled = !codeImportLoading
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "${codeImportText.length} / ${com.example.data.ShareCodeCodec.TAMANHO_MAXIMO_CODIGO}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (codeImportText.length > com.example.data.ShareCodeCodec.TAMANHO_MAXIMO_CODIGO) ExaltedDangerCore else ExaltedMuted
                    )
                    com.example.ui.components.GildedDialogTextButton(
                        text = "Colar",
                        onClick = {
                            clipboardManager.getText()?.text?.let { texto ->
                                codeImportText = texto
                                codeImportError = null
                            }
                        }
                    )
                }
                codeImportError?.let { erro ->
                    AppText(erro, color = ExaltedDangerCore, style = MaterialTheme.typography.bodySmall)
                }
                if (codeImportLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ExaltedAccentBright, modifier = Modifier.size(28.dp))
                    }
                }
            }
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(
                text = "OK",
                enabled = !codeImportLoading,
                onClick = {
                    codeImportLoading = true
                    codeImportError = null
                    val textoParaImportar = codeImportText
                    scope.launch {
                        val resultado = withContext(Dispatchers.Default) {
                            viewModel.importarPlanilhaPorCodigo(textoParaImportar)
                        }
                        codeImportLoading = false
                        when (resultado) {
                            is com.example.data.ShareCodeCodec.ResultadoImportacao.Sucesso -> {
                                viewModel.aplicarPlanilhaImportada(resultado.sheet)
                                onDismiss()
                                snackbarHostState.showSnackbar("Planilha nova criada a partir do código (Atributos, Casta, Habilidades, Méritos e Encantos). Preencha nome/conceito e salve explicitamente para manter no dispositivo.")
                            }
                            is com.example.data.ShareCodeCodec.ResultadoImportacao.Erro -> {
                                codeImportError = resultado.mensagem
                            }
                        }
                    }
                }
            )
        },
        confirmButton = {
            com.example.ui.components.GildedDialogTextButton(
                text = "Cancelar",
                onClick = { if (!codeImportLoading) onDismiss() }
            )
        },
        containerColor = ExaltedDarkSurface
    )
}
