package com.example.viewmodel

import com.example.data.SheetRepository
import com.example.model.CaixaVitalidade
import com.example.model.NpcEncontro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Mutações da trilha de vitalidade dos NPCs de encontro (Aba 11).
 *
 * Extraído de EncounterNpcActions (refatoração de organização —
 * roteiro de refatoração agressiva, sem mudança de comportamento).
 */
internal class EncounterNpcVitalityActions(
    private val state: MutableStateFlow<List<NpcEncontro>>,
    private val repository: SheetRepository,
    private val removerDaIniciativa: (String) -> Unit,
    private val scope: CoroutineScope,
) {
    // A reorganização da trilha da Aba 11 é deliberadamente adiada: cada
    // nova interação reinicia a janela de 2 segundos. Jobs são separados por
    // NPC para que interagir com uma trilha não cancele a reorganização de
    // outra trilha aberta no mesmo encontro.
    private val reorganizationJobs = ConcurrentHashMap<String, Job>()

    /**
     * Ordena os ferimentos por gravidade (* > X > /) e preenche os níveis
     * disponíveis na sequência visual: esquerda para direita, de cima para
     * baixo. As caixas continuam com os mesmos IDs; somente o símbolo de
     * dano é redistribuído entre elas.
     */
    private fun reorganizarDanoPorGravidade(boxes: List<CaixaVitalidade>): List<CaixaVitalidade> {
        if (boxes.isEmpty()) return boxes

        val sequenciaVisual = boxes.withIndex().sortedBy { it.value.penaltyRank() }
        val danos = sequenciaVisual
            .map { it.value.tipoDano }
            .filter { it != 0 }
            .sortedDescending()

        // Como danos contém todos os tipoDano != 0, lista vazia implica
        // necessariamente que a trilha já está limpa.
        if (danos.isEmpty()) return boxes

        val novaLista = boxes.toMutableList()
        sequenciaVisual.forEachIndexed { posicao, item ->
            val novoTipo = danos.getOrNull(posicao) ?: 0
            if (item.value.tipoDano != novoTipo) {
                novaLista[item.index] = item.value.copy(tipoDano = novoTipo)
            }
        }
        return novaLista
    }

    private fun agendarReorganizacao(npcId: String) {
        reorganizationJobs[npcId]?.cancel()
        lateinit var job: Job
        job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                delay(2_000L)
                var estadoFinal: List<NpcEncontro>? = null
                state.update { lista ->
                    val indiceNpc = lista.indexOfFirst { it.id == npcId }
                    if (indiceNpc < 0) return@update lista
                    val npc = lista[indiceNpc]
                    val reorganizadas = reorganizarDanoPorGravidade(npc.healthBoxes)
                    if (reorganizadas == npc.healthBoxes) return@update lista

                    val novaLista = ArrayList(lista)
                    novaLista[indiceNpc] = npc.copy(healthBoxes = reorganizadas)
                    estadoFinal = novaLista
                    novaLista
                }
                if (estadoFinal != null) repository.salvarNpcsEncontro(state.value)
            } finally {
                // Um job antigo pode terminar depois que uma nova interação já
                // registrou seu substituto. Remova somente a própria instância
                // para não perder a referência do debounce mais recente.
                reorganizationJobs.remove(npcId, job)
            }
        }
        reorganizationJobs[npcId] = job
        job.start()
    }

    fun dispose() {
        reorganizationJobs.forEach { (_, job) -> job.cancel() }
        reorganizationJobs.clear()
    }

    fun remove(npcId: String) {
        reorganizationJobs.remove(npcId)?.cancel()
    }

    /** Remove uma caixa extra (não-permanente, ex.: bônus de Corpo de
     * Touro) da trilha de vitalidade do NPC — mesmo padrão da Aba 5
     * (pedido explícito do usuário). Caixas permanentes nunca são
     * removidas por essa via, mesmo se o id bater. */
    fun removeExtraHealthBox(npcId: String, boxId: String) {
        var novoEstado: List<NpcEncontro>? = null
        state.update { lista ->
            val indiceNpc = lista.indexOfFirst { it.id == npcId }
            if (indiceNpc < 0) return@update lista
            val npc = lista[indiceNpc]
            val indiceBox = npc.healthBoxes.indexOfFirst { it.id == boxId && !it.isPermanente }
            if (indiceBox < 0) return@update lista

            val boxes = npc.healthBoxes.toMutableList()
            boxes.removeAt(indiceBox)
            val reorganizadas = reorganizarDanoPorGravidade(boxes)
            val novaLista = ArrayList(lista)
            novaLista[indiceNpc] = npc.copy(healthBoxes = reorganizadas)
            novoEstado = novaLista
            novaLista
        }
        // A remoção já reorganiza a trilha imediatamente. Se havia um
        // debounce aguardando uma interação anterior, ele se tornou redundante.
        if (novoEstado != null) {
            reorganizationJobs.remove(npcId)?.cancel()
            repository.salvarNpcsEncontro(state.value)
        }
    }

    fun clearHealthDamage(npcId: String) {
        reorganizationJobs.remove(npcId)?.cancel()
        var novoEstado: List<NpcEncontro>? = null
        state.update { lista ->
            val indiceNpc = lista.indexOfFirst { it.id == npcId }
            if (indiceNpc < 0) return@update lista
            val npc = lista[indiceNpc]
            if (npc.healthBoxes.all { it.tipoDano == 0 }) return@update lista
            val novaLista = ArrayList(lista)
            novaLista[indiceNpc] = npc.copy(
                healthBoxes = npc.healthBoxes.map { it.copy(tipoDano = 0) }
            )
            novoEstado = novaLista
            novaLista
        }
        if (novoEstado != null) repository.salvarNpcsEncontro(state.value)
    }

    fun cycleHealthDamage(npcId: String, boxId: String) {
        var morreu = false
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            val indiceNpc = list.indexOfFirst { it.id == npcId }
            if (indiceNpc < 0) return@update list
            val npc = list[indiceNpc]
            val indiceBox = npc.healthBoxes.indexOfFirst { it.id == boxId }
            if (indiceBox < 0) return@update list

            val boxes = npc.healthBoxes.toMutableList()
            val alvo = boxes[indiceBox]
            // Na Aba 11 qualquer quadrado pode ser escolhido. A posição
            // tocada recebe o próximo símbolo imediatamente; a reorganização
            // definitiva acontece somente após 2 segundos sem interação.
            boxes[indiceBox] = alvo.copy(tipoDano = (alvo.tipoDano + 1) % 4)

            // Mantém a regra existente de remover o NPC quando todos os
            // quadrados estão preenchidos. Caso contrário, a trilha permanece
            // livre para receber novas interações durante a janela de 2s.
            morreu = boxes.isNotEmpty() && boxes.all { it.tipoDano != 0 }
            val novaLista = ArrayList(list)
            if (morreu) {
                novaLista.removeAt(indiceNpc)
            } else {
                novaLista[indiceNpc] = npc.copy(healthBoxes = boxes)
            }
            novoEstado = novaLista
            novaLista
        }
        if (novoEstado == null) return
        if (morreu) {
            reorganizationJobs.remove(npcId)?.cancel()
            removerDaIniciativa(npcId)
        } else {
            agendarReorganizacao(npcId)
        }
        repository.salvarNpcsEncontro(state.value)
    }

}
