package com.example.viewmodel

import com.example.model.CaixaVitalidade
import com.example.model.CharacterSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Ações da Trilha de Vitalidade (Aba Combate).
//
// Recebe um CoroutineScope (a mesma viewModelScope do ViewModel
// principal) por construtor — a reorganização com debounce só existe
// fora do SheetViewModel porque essa referência é passada explicitamente;
// viewModelScope em si só está disponível dentro de uma classe ViewModel.
class HealthActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val scope: CoroutineScope
) {

    // Rastreia a reorganização pendente (debounce de 2s) — cancelada e
    // reagendada a cada nova marcação/desmarcação de dano, pra só rodar
    // uma vez depois que o usuário parar de interagir.
    private var vitalidadeReorgJob: Job? = null

    fun addExtraHealthBox(penalidade: String) {
        val box = CaixaVitalidade(penalidade = penalidade, tipoDano = 0, isPermanente = false)
        sheetState.update { it.copy(healthBoxes = it.healthBoxes + box) }
    }

    fun removeExtraHealthBox(id: String) {
        sheetState.update { current ->
            val index = current.healthBoxes.indexOfFirst { box -> box.id == id && !box.isPermanente }
            if (index < 0) return@update current
            val boxes = current.healthBoxes.toMutableList()
            boxes.removeAt(index)
            current.copy(healthBoxes = boxes)
        }
    }

    fun clearHealthDamage() {
        vitalidadeReorgJob?.cancel()
        vitalidadeReorgJob = null
        sheetState.update { current ->
            if (current.healthBoxes.all { it.tipoDano == 0 }) current
            else current.copy(healthBoxes = current.healthBoxes.map { it.copy(tipoDano = 0) })
        }
    }

    fun cycleHealthDamage(id: String) {
        var alterou = false
        sheetState.update { current ->
            val alvo = current.healthBoxes.firstOrNull { it.id == id }
                ?: return@update current

            val list = current.healthBoxes.toMutableList()
            if (alvo.tipoDano == 0) {
                // O clique inicial marca EXATAMENTE a caixa escolhida, mesmo
                // que ela esteja fora da ordem canônica. A reorganização
                // posterior, após 2s sem novas alterações, reposicionará todos
                // os ferimentos pela prioridade * > X > /.
                val indice = list.indexOfFirst { it.id == id }
                if (indice < 0) return@update current
                list[indice] = alvo.copy(tipoDano = 1)
            } else {
                // Caixa já marcada: mantém o ciclo de tipos de dano. A
                // reorganização posterior recompõe toda a trilha pela ordem
                // * > X > / e da esquerda para a direita.
                val indice = list.indexOfFirst { it.id == id }
                if (indice < 0) return@update current
                list[indice] = alvo.copy(tipoDano = (alvo.tipoDano + 1) % 4)
            }
            alterou = true
            current.copy(healthBoxes = list)
        }
        if (alterou) agendarReorganizacaoVitalidade()
    }

    // Ordem lógica obrigatória da trilha: por penalidade crescente (-0, -1,
    // -2, -4, Inc.) e, dentro da mesma penalidade, pela ordem em que as
    // caixas existem na lista (caixas extras entram à direita das originais
    // pois são sempre adicionadas ao final da lista).
    private fun sequenciaLogicaVitalidade(sheet: CharacterSheet) =
        sheet.healthBoxes.withIndex().sortedBy { it.value.penaltyRank() }

    private fun agendarReorganizacaoVitalidade() {
        vitalidadeReorgJob?.cancel()
        vitalidadeReorgJob = scope.launch {
            delay(2000)
            sheetState.update { reorganizarTrilhaDeVitalidade(it) }
        }
    }

    // Reorganização central da Trilha de Vitalidade (executada 2s após a
    // última alteração): recalculada do zero a partir do estado atual —
    // todos os símbolos marcados (em qualquer posição) são compactados para
    // o início da sequência lógica, ordenados por prioridade (* > X > /).
    // Isso garante que apagar uma caixa no MEIO da trilha desloque as
    // caixas marcadas à frente dela para preencher o espaço, sem deixar
    // buracos — e que marcar uma caixa distante ocupe a posição de menor
    // penalidade disponível, como a sequência obrigatória exige.
    private fun reorganizarTrilhaDeVitalidade(sheet: CharacterSheet): CharacterSheet {
        val ordenada = sequenciaLogicaVitalidade(sheet)
        if (ordenada.isEmpty()) return sheet

        // A ordenação anterior criava uma lista de símbolos + um mapa por ID.
        // Como existem apenas quatro estados de dano, contar os três tipos
        // e gravá-los diretamente nos índices originais elimina coleções
        // intermediárias e a segunda busca por ID.
        var agravados = 0
        var letais = 0
        var contundentes = 0
        for (item in ordenada) {
            when (item.value.tipoDano) {
                3 -> agravados++
                2 -> letais++
                1 -> contundentes++
            }
        }
        if (agravados == 0 && letais == 0 && contundentes == 0) {
            if (sheet.healthBoxes.all { it.tipoDano == 0 }) return sheet
            return sheet.copy(healthBoxes = sheet.healthBoxes.map { it.copy(tipoDano = 0) })
        }

        val novaLista = sheet.healthBoxes.toMutableList()
        var restantesAgravados = agravados
        var restantesLetais = letais
        var restantesContundentes = contundentes
        for (item in ordenada) {
            val novoTipo = when {
                restantesAgravados > 0 -> { restantesAgravados--; 3 }
                restantesLetais > 0 -> { restantesLetais--; 2 }
                restantesContundentes > 0 -> { restantesContundentes--; 1 }
                else -> 0
            }
            if (item.value.tipoDano != novoTipo) {
                novaLista[item.index] = item.value.copy(tipoDano = novoTipo)
            }
        }
        return if (novaLista == sheet.healthBoxes) sheet else sheet.copy(healthBoxes = novaLista)
    }
}
