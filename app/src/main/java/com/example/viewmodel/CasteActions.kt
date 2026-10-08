package com.example.viewmodel

import com.example.model.Casta
import com.example.model.CharacterSheet
import com.example.model.ExaltedConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Ações da Aba 2 — Casta: troca de Casta (com reset das seleções
// dependentes), Habilidades de Casta, Favorecidas e Supernal. Também
// independente dos outros domínios — só manipula listas/campos da
// própria planilha, sem chamar nada de Atributos/Combate/Encantos.
class CasteActions(private val sheetState: MutableStateFlow<CharacterSheet>) {

    // --- Caste Updates & Reset Rule ---
    fun updateCasta(newCasta: Casta) {
        sheetState.update { current ->
            current.copy(
                casta = newCasta,
                castaEscolhida = true,
                casteAbilities = emptyList(),
                favoredAbilities = emptyList(),
                supernalAbility = null
            )
        }
    }

    // Lunares usam Atributos de Casta (fixos por LunarCasta) em vez de
    // Habilidades de Casta escolhidas manualmente — não há seleção livre
    // de 5 aqui, o próprio enum já define os 3 (ou 0, pra Sem Casta).
    fun updateLunarCasta(newCasta: com.example.model.LunarCasta) {
        sheetState.update { current ->
            current.copy(
                lunarCasta = newCasta,
                lunarCastaEscolhida = true,
                lunarCasteAttributesEscolhidos = emptyList(),
                favoredAttributes = emptyList()
            )
        }
    }

    fun toggleCasteAbility(abilityName: String) {
        sheetState.update { current ->
            val list = current.casteAbilities.toMutableList()
            var supernal = current.supernalAbility
            if (list.contains(abilityName)) {
                list.remove(abilityName)
                if (supernal == abilityName) supernal = null
            } else {
                if (list.size < 5) {
                    list.add(abilityName)
                    // If ability was in favored, remove from favored
                    val favList = current.favoredAbilities.toMutableList()
                    favList.remove(abilityName)
                    return@update current.copy(casteAbilities = list, favoredAbilities = favList, supernalAbility = supernal)
                }
            }
            current.copy(casteAbilities = list, supernalAbility = supernal)
        }
    }

    fun toggleFavoredAbility(abilityName: String) {
        sheetState.update { current ->
            val list = current.favoredAbilities.toMutableList()
            if (list.contains(abilityName)) {
                list.remove(abilityName)
                current.copy(favoredAbilities = list)
            } else {
                if (list.size < 5 && !current.casteAbilities.contains(abilityName)) {
                    list.add(abilityName)
                    // Habilidade recém-favorecida sempre nasce com pelo menos
                    // 1 ponto — pedido explícito do usuário.
                    val abilities = if ((current.abilities[abilityName] ?: 0) < 1) {
                        current.abilities.toMutableMap().apply { put(abilityName, 1) }
                    } else current.abilities
                    current.copy(favoredAbilities = list, abilities = abilities)
                } else current
            }
        }
    }

    fun setSupernalAbility(abilityName: String?) {
        sheetState.update { current ->
            if (abilityName == null || current.casteAbilities.contains(abilityName)) {
                current.copy(supernalAbility = abilityName)
            } else {
                current
            }
        }
    }

    // Campos de texto livre exclusivos de Lunar — pedido explícito do
    // usuário: "Forma Espiritual" e "Sinal", lado a lado abaixo da
    // descrição da Anima.
    fun updateLunarFormaEspiritual(valor: String) {
        sheetState.update { it.copy(lunarFormaEspiritual = valor) }
    }

    fun updateLunarSinal(valor: String) {
        sheetState.update { it.copy(lunarSinal = valor) }
    }

    // Lunar: a criação tem duas seleções independentes. Cada Casta nomeada
    // fornece 3 opções e o jogador escolhe exatamente 2 como Atributos de
    // Casta. Depois escolhe exatamente 2 Atributos Favorecidos adicionais.
    // Um Atributo de Casta não pode ser Favorecido novamente. O estado é
    // mantido separado para que a regra de 2 + 2 permaneça explícita.
    fun toggleLunarCasteAttribute(attributeName: String) {
        sheetState.update { current ->
            if (attributeName !in current.lunarCasta.poolAtributosCasta()) return@update current

            val list = current.lunarCasteAttributesEscolhidos.toMutableList()
            // A remoção deve continuar possível mesmo se uma planilha antiga ou
            // uma entrada externa contiver um estado inválido com duplicação.
            if (attributeName in list) {
                list.remove(attributeName)
                return@update current.copy(lunarCasteAttributesEscolhidos = list)
            }

            // Regra de domínio: Casta e Favorecido são conjuntos disjuntos.
            if (attributeName in current.favoredAttributes || list.size >= 2) return@update current
            list.add(attributeName)
            current.copy(lunarCasteAttributesEscolhidos = list)
        }
    }

    fun toggleFavoredAttribute(attributeName: String) {
        sheetState.update { current ->
            if (attributeName !in ExaltedConstants.ALL_ATTRIBUTES) return@update current

            val list = current.favoredAttributes.toMutableList()
            // Primeiro permita remover: isso também recupera planilhas antigas
            // que eventualmente tenham uma duplicação Casta/Favorecido.
            if (attributeName in list) {
                list.remove(attributeName)
                return@update current.copy(favoredAttributes = list)
            }

            // Só a inclusão é bloqueada para Atributos já escolhidos como Casta.
            if (attributeName in current.lunarCasteAttributesEscolhidos || list.size >= 2) return@update current
            list.add(attributeName)
            current.copy(favoredAttributes = list)
        }
    }
}
