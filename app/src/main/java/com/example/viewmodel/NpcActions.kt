package com.example.viewmodel

import com.example.data.NpcShareCodec
import com.example.data.SheetRepository
import com.example.model.Npc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Actions for the campaign-wide manual NPC list. */
class NpcActions(
    private val state: MutableStateFlow<List<Npc>>,
    private val repository: SheetRepository
) {
    fun add(nome: String, lealdade: String, tipo: String, descricao: String) {
        val nomeNormalizado = nome.trim()
        if (nomeNormalizado.isBlank()) return
        val novoNpc = Npc(
            nome = nomeNormalizado,
            lealdade = lealdade,
            tipo = tipo.trim(),
            descricao = descricao.trim()
        )
        var novoEstado: List<Npc>? = null
        state.update { atual ->
            val atualizado = atual + novoNpc
            novoEstado = atualizado
            atualizado
        }
        novoEstado?.let(repository::salvarNpcs)
    }

    fun remove(id: String) {
        var novoEstado: List<Npc>? = null
        state.update { atual ->
            val indice = atual.indexOfFirst { it.id == id }
            if (indice < 0) return@update atual
            val atualizado = atual.toMutableList().apply { removeAt(indice) }
            novoEstado = atualizado
            atualizado
        }
        // No-op real não deve provocar serialização nem escrita em disco.
        // Persiste exatamente a lista produzida pela mutação, evitando uma
        // segunda leitura do StateFlow após update().
        novoEstado?.let(repository::salvarNpcs)
    }

    fun exportarComoCodigo(): Result<String> = NpcShareCodec.exportar(state.value)

    fun aplicarImportados(npcs: List<Npc>) {
        if (npcs.isEmpty()) return
        var novoEstado: List<Npc>? = null
        state.update { atual ->
            val atualizado = atual + npcs
            novoEstado = atualizado
            atualizado
        }
        novoEstado?.let(repository::salvarNpcs)
    }
}
