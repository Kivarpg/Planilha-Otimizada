package com.example.viewmodel

import com.example.model.CharacterSheet
import com.example.model.Especializacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Ações da Aba 4 — Habilidades.
//
// Recebe charmsActions além de experienceActions: setAbilityRating
// precisa de sanitizeKnownCharms() sempre que um nível de habilidade cai
// (pode invalidar pré-requisitos de Encanto já adquirido).
class AbilitiesActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val experienceActions: ExperienceActions,
    private val charmsActions: CharmsActions,
    private val meritosCatalog: com.example.data.MeritosCatalog,
    private val pendingMeritBreak: MutableStateFlow<PendingMeritBreak?>
) {

    fun removeSpecialization(id: String) {
        sheetState.update { current ->
            val index = current.specializations.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            val specializations = current.specializations.toMutableList()
            specializations.removeAt(index)
            current.copy(specializations = specializations)
        }
    }

    fun addSpecialization(nome: String, habilidade: String) {
        if (habilidade.isBlank()) return
        sheetState.update { current ->
            val item = Especializacao(nome = nome.trim(), habilidade = habilidade)
            if (current.planilhaConcluida) {
                val debitado = experienceActions.debitarExperiencia(current, 3, "Especialização: ${item.nome} ($habilidade)") ?: return@update current
                return@update debitado.copy(specializations = debitado.specializations + item)
            }
            experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(specializations = current.specializations + item))
        }
    }

    fun setAbilityRating(abilityName: String, rating: Int) {
        val current = sheetState.value
        val ehFavorecida = current.favoredAbilities.contains(abilityName)
        val minimo = if (ehFavorecida) 1 else 0
        val novo = rating.coerceIn(minimo, 5)
        val atual = current.abilities[abilityName] ?: 0
        if (novo < atual) {
            val novasHabilidades = current.abilities.toMutableMap().apply { put(abilityName, novo) }
            val quebrados = com.example.data.meritosQuebradosPor(
                current.merits, meritosCatalog, current.attributes, current.abilities, current.attributes, novasHabilidades
            )
            if (quebrados.isNotEmpty()) {
                pendingMeritBreak.value = PendingMeritBreak(
                    descricaoMudanca = "$abilityName $atual → $novo",
                    meritosAfetados = quebrados,
                    aplicar = { aplicarReducaoHabilidade(abilityName, novo, quebrados) }
                )
                return
            }
        }
        aplicarAbilityRating(abilityName, novo)
    }

    private fun aplicarReducaoHabilidade(abilityName: String, novo: Int, meritosARemover: List<String>) {
        sheetState.update { it.copy(merits = it.merits.filter { m -> m.nome !in meritosARemover }) }
        aplicarAbilityRating(abilityName, novo)
    }

    private fun aplicarAbilityRating(abilityName: String, novoBruto: Int) {
        sheetState.update { current ->
            var novo = novoBruto
            val atual = current.abilities[abilityName] ?: 0
            if (current.planilhaConcluida && novo < atual) {
                val baseline = SheetCalculations.planilhaNaConclusao(current)?.abilities?.get(abilityName) ?: atual
                novo = novo.coerceAtLeast(baseline)
                if (novo == atual) return@update current
            }
            if (current.planilhaConcluida && novo > atual) {
                val favorecida = current.casteAbilities.contains(abilityName) || current.favoredAbilities.contains(abilityName)
                val custo = (atual until novo).sumOf { nivel -> SheetCalculations.custoAumentoHabilidade(nivel, favorecida) }
                val debitado = experienceActions.debitarExperiencia(current, custo, "Habilidade: $abilityName ($atual → $novo)") ?: return@update current
                val map = debitado.abilities.toMutableMap()
                map[abilityName] = novo
                // Aumentar uma Habilidade só pode satisfazer novos requisitos;
                // nunca invalida Encantos já adquiridos. Evita a sanitização
                // completa do catálogo no caminho quente de avanço por XP.
                return@update debitado.copy(abilities = map)
            }
            val map = current.abilities.toMutableMap()
            map[abilityName] = novo
            charmsActions.sanitizeKnownCharms(experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(abilities = map)))
        }
    }
}
