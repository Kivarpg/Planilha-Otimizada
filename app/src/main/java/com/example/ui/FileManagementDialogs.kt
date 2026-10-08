package com.example.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.ui.components.SaveValidationModal
import com.example.viewmodel.SheetViewModel
import kotlinx.coroutines.launch


@Composable
fun SaveValidationDialog(
    viewModel: SheetViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    onSaveSuccessful: () -> Unit
) {
    val saveValidationEvent by viewModel.saveValidationEvent.collectAsState()
    saveValidationEvent?.let { valResult ->
        if (!valResult.isValid) {
            SaveValidationModal(
                errors = valResult.errors,
                onDismiss = { viewModel.dismissValidationDialog() },
                onConfirmIncompleteSave = { viewModel.confirmSaveIncompleteSheet() }
            )
        } else {
            viewModel.dismissValidationDialog()
            onSaveSuccessful()
            scope.launch {
                snackbarHostState.showSnackbar("Planilha salva com sucesso!")
            }
        }
    }
}

// SKIN: gera o QR Code do código de compartilhamento como Bitmap. Nível de
// correção de erro H (o mais alto — tolera ~30% de dano/obstrução nos
// módulos), dando margem extra de leitura em telas pequenas ou fotos
// tiradas em ângulo. Sem logo sobreposta (versão anterior tinha uma; foi
// removida numa limpeza anterior e não recriada agora, por simplicidade —
// prioridade é confiabilidade de leitura, não estética).
