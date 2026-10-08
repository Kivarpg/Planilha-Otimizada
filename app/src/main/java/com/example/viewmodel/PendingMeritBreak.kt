package com.example.viewmodel

// Representa uma redução de atributo/habilidade que quebraria o
// pré-requisito de um ou mais Méritos já adquiridos. Não é aplicada
// direto — fica pendente até o usuário confirmar ou cancelar na tela de
// aviso (ver SheetViewModel.confirmarPendingMeritBreak/
// cancelarPendingMeritBreak).
data class PendingMeritBreak(
    // Ex: "Briga 1 → 0" -- o que o usuário está tentando tirar.
    val descricaoMudanca: String,
    // Nomes dos Méritos que seriam removidos se a mudança for confirmada.
    val meritosAfetados: List<String>,
    // Aplica a mudança de atributo/habilidade E remove os Méritos
    // afetados, de uma vez -- só invocada em confirmarPendingMeritBreak.
    val aplicar: () -> Unit
)
