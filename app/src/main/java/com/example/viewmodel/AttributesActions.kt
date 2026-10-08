package com.example.viewmodel

import com.example.model.CharacterSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Ações da Aba 3 — Atributos.
//
// Recebe experienceActions (o núcleo de XP/Pontos de Bônus compartilhado)
// como dependência, já que setAttributeRating precisa debitar XP no Modo
// Experiência e checar saldo de BP no Modo Criação — mesma dependência
// que existia quando a função ainda vivia dentro de SheetViewModel.
class AttributesActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val experienceActions: ExperienceActions,
    private val meritosCatalog: com.example.data.MeritosCatalog,
    private val pendingMeritBreak: MutableStateFlow<PendingMeritBreak?>
) {

    fun setGroupPriority(group: String, priority: String) {
        sheetState.update { current ->
            val map = current.attributePriorities.toMutableMap()
            val oldOwner = map.entries.firstOrNull { it.value == priority }?.key
            val currentGroupPriority = map[group] ?: "1º"
            if (oldOwner != null && oldOwner != group) {
                map[oldOwner] = currentGroupPriority
            }
            map[group] = priority
            current.copy(attributePriorities = map)
        }
    }

    fun setAttributeRating(attrName: String, rating: Int) {
        val current = sheetState.value
        val novo = rating.coerceIn(1, 5)
        val atual = current.attributes[attrName] ?: 1
        if (novo < atual) {
            val novosAtributos = current.attributes.toMutableMap().apply { put(attrName, novo) }
            val quebrados = com.example.data.meritosQuebradosPor(
                current.merits, meritosCatalog, current.attributes, current.abilities, novosAtributos, current.abilities
            )
            if (quebrados.isNotEmpty()) {
                pendingMeritBreak.value = PendingMeritBreak(
                    descricaoMudanca = "$attrName $atual → $novo",
                    meritosAfetados = quebrados,
                    aplicar = { aplicarReducaoAtributo(attrName, novo, quebrados) }
                )
                return
            }
        }
        aplicarAttributeRating(attrName, novo)
    }

    private fun aplicarReducaoAtributo(attrName: String, novo: Int, meritosARemover: List<String>) {
        sheetState.update { it.copy(merits = it.merits.filter { m -> m.nome !in meritosARemover }) }
        aplicarAttributeRating(attrName, novo)
    }

    private fun aplicarAttributeRating(attrName: String, novoBruto: Int) {
        sheetState.update { current ->
            var novo = novoBruto
            val atual = current.attributes[attrName] ?: 1
            if (current.planilhaConcluida && novo < atual) {
                val baseline = SheetCalculations.planilhaNaConclusao(current)?.attributes?.get(attrName) ?: atual
                novo = novo.coerceAtLeast(baseline)
                if (novo == atual) return@update current
            }
            if (current.planilhaConcluida && novo > atual) {
                val custo = (atual until novo).sumOf { nivel -> nivel * 4 }
                val debitado = experienceActions.debitarExperiencia(current, custo, "Atributo: $attrName ($atual → $novo)") ?: return@update current
                val map = debitado.attributes.toMutableMap()
                map[attrName] = novo
                val atualizado = debitado.copy(attributes = map)
                return@update if (attrName == "Vigor") SheetCalculations.recalcularCaixasCorpoDeTouro(atualizado) else atualizado
            }
            val map = current.attributes.toMutableMap()
            map[attrName] = novo
            val mutado = experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(attributes = map))
            if (attrName == "Vigor") SheetCalculations.recalcularCaixasCorpoDeTouro(mutado) else mutado
        }
    }
}
