package com.example.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.example.ui.components.SaveValidationModal
import com.example.viewmodel.SheetViewModel
import kotlinx.coroutines.launch


@Composable
fun SaveValidationDialog(
    viewModel: SheetViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    onSaveSuccessful: () -> Unit,
    modifier: Modifier = Modifier
) {
    val saveValidationEvent by viewModel.saveValidationEvent.collectAsState()
    val latestOnSaveSuccessful by rememberUpdatedState(onSaveSuccessful)
    val latestSnackbarHostState by rememberUpdatedState(snackbarHostState)

    if (saveValidationEvent?.isValid == true) {
        LaunchedEffect(viewModel, saveValidationEvent) {
            viewModel.dismissValidationDialog()
            latestOnSaveSuccessful()
            latestSnackbarHostState.showSnackbar("Planilha salva com sucesso!")
        }
    } else {
        saveValidationEvent?.let { valResult ->
            SaveValidationModal(
                errors = valResult.errors,
                onDismiss = { viewModel.dismissValidationDialog() },
                onConfirmIncompleteSave = { viewModel.confirmSaveIncompleteSheet() },
                modifier = modifier
            )
        }
    }
}

// SKIN: gera o QR Code do código de compartilhamento como Bitmap. Nível de
// correção de erro H (o mais alto — tolera ~30% de dano/obstrução nos
// módulos), dando margem extra de leitura em telas pequenas ou fotos
// tiradas em ângulo. Sem logo sobreposta (versão anterior tinha uma; foi
// removida numa limpeza anterior e não recriada agora, por simplicidade —
// prioridade é confiabilidade de leitura, não estética).
