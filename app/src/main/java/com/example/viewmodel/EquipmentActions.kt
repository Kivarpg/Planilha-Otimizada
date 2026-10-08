package com.example.viewmodel

import com.example.model.CharacterSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Pertences gerais (Aba 7) — texto livre, sem regra de custo.
//
// NOTA: armas, armaduras e a lógica de comitamento de motes (o resto da
// Aba 7) ficam no SheetViewModel por enquanto — é um domínio bem maior
// e mais interdependente (motes tocam tanto armas quanto armaduras, e a
// validação de peso/comitamento cruza os dois), que precisa de uma
// rodada própria mais cuidadosa, não uma extração rápida junto desta.
class EquipmentActions(private val sheetState: MutableStateFlow<CharacterSheet>) {

    fun updatePertences(valStr: String) = sheetState.update { it.copy(pertences = valStr) }
}
