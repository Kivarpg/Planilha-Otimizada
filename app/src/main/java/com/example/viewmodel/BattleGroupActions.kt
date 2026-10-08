package com.example.viewmodel

import com.example.model.BattleGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BattleGroupActions(
    private val save: (List<BattleGroup>) -> Unit,
    initial: List<BattleGroup>
) {
    private val _groups = MutableStateFlow(initial)
    val groups: StateFlow<List<BattleGroup>> = _groups.asStateFlow()

    fun load(groups: List<BattleGroup>) {
        _groups.value = groups
    }

    fun upsert(group: BattleGroup) {
        val updated = _groups.value.toMutableList()
        val index = updated.indexOfFirst { it.id == group.id }
        if (index >= 0) updated[index] = group else updated += group
        _groups.value = updated
        save(updated)
    }

    fun remove(id: String) {
        val updated = _groups.value.filterNot { it.id == id }
        if (updated.size == _groups.value.size) return
        _groups.value = updated
        save(updated)
    }

    /**
     * Ajusta a Magnitude atual do grupo em combate — pedido explícito do
     * usuário (Parte C, ajuste manual pela Aba 13). [magnitudeBaseDerivada]
     * só é usado na primeira redução (quando currentMagnitude ainda é
     * null); depois disso, currentMagnitude já existe e vira a base —
     * o parâmetro pode ficar 0 sem efeito nenhuma vez que o grupo já
     * tiver sofrido alguma alteração de Magnitude.
     */
    fun ajustarMagnitude(id: String, delta: Int, magnitudeBaseDerivada: Int) {
        val grupo = _groups.value.firstOrNull { it.id == id } ?: return
        val atual = grupo.currentMagnitude ?: magnitudeBaseDerivada
        val novo = (atual + delta).coerceAtLeast(0)
        upsert(grupo.copy(currentMagnitude = novo))
    }

    /** Chamado ao reingressar em um novo combate — spec não exige reset
     * automático, então fica explícito (botão) em vez de implícito. */
    fun resetarMagnitude(id: String) {
        val grupo = _groups.value.firstOrNull { it.id == id } ?: return
        upsert(grupo.copy(currentMagnitude = null))
    }
}
