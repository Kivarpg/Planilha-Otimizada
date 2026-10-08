package com.example.viewmodel

import com.example.model.CharacterSheet
import com.example.model.Intimidade
import com.example.model.Merito
import com.example.model.isDragonBlooded
import com.example.data.LanguageAcquisitionRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Ações da Aba 1 — Dados Pessoais.
class PersonalDataActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val experienceActions: ExperienceActions,
    private val commitmentError: MutableStateFlow<String?>
) {
    fun updateNome(valStr: String) = sheetState.update { it.copy(nome = valStr) }
    fun updateJogador(valStr: String) = sheetState.update { it.copy(jogador = valStr) }
    fun updateConceito(valStr: String) = sheetState.update { it.copy(conceito = valStr) }
    fun updateDescricaoAnima(valStr: String) = sheetState.update { it.copy(descricaoAnima = valStr) }
    fun updateFalhaVirtude(valStr: String) = sheetState.update { it.copy(falhaVirtude = valStr) }
    fun updateLimiteGatilho(valStr: String) = sheetState.update { it.copy(limiteGatilho = valStr) }
    fun updateLimiteContador(valInt: Int) = sheetState.update { it.copy(limiteContador = valInt.coerceIn(0, 10)) }

    fun addIntimidade(nome: String, tipo: String, intensidade: String) {
        if (nome.isBlank()) return
        val item = Intimidade(nome = nome.trim(), tipo = tipo, intensidade = intensidade)
        sheetState.update { it.copy(intimacies = it.intimacies + item) }
    }
    fun removeIntimidade(id: String) {
        sheetState.update { current ->
            val index = current.intimacies.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            current.copy(intimacies = current.intimacies.toMutableList().also { it.removeAt(index) })
        }
    }

    fun updateLinguaNativa(lingua: String?) {
        sheetState.update { current ->
            val adicionaisMantidos = if (lingua != null) current.linguasAdicionais.filterNot { it.equals(lingua, ignoreCase = true) } else current.linguasAdicionais
            val removidos = current.linguasAdicionais.toSet() - adicionaisMantidos.toSet()
            current.copy(
                linguaNativa = lingua,
                linguasAdicionais = adicionaisMantidos,
                merits = current.merits.filterNot { it.origemAutomatica == "Idioma" && it.detalhe in removidos }
            )
        }
    }

    fun addLinguaAdicional(lingua: String): Boolean {
        val idioma = lingua.trim()
        if (idioma.isBlank()) return false
        var adicionada = false
        sheetState.update { current ->
            if (idioma.equals(current.linguaNativa, ignoreCase = true) ||
                current.linguasAdicionais.any { it.equals(idioma, ignoreCase = true) }) return@update current

            if (!LanguageAcquisitionRules.podeAdquirir(idioma, current.abilities)) {
                commitmentError.value = "Antigo Reino exige Ocultismo 1 ou Conhecimento 1."
                return@update current
            }

            val pontosAtuais = current.merits.sumOf { it.valor }
            if (current.tipoPersonagem.isDragonBlooded()) {
                val limite = 18
                if (pontosAtuais + 1 > limite) {
                    commitmentError.value = "Não há pontos de Mérito disponíveis para adquirir outro Idioma."
                    return@update current
                }
            }

            val novoMerito = Merito(
                nome = "Idioma",
                valor = 1,
                categoria = "Idioma",
                detalhe = idioma,
                origemAutomatica = "Idioma"
            )
            val mutado = current.copy(
                linguasAdicionais = current.linguasAdicionais + idioma,
                merits = current.merits + novoMerito
            )

            if (current.planilhaConcluida) {
                val debitado = experienceActions.debitarExperiencia(
                    current,
                    3,
                    "Mérito: Idioma ($idioma) (nível 1)"
                ) ?: return@update current
                adicionada = true
                debitado.copy(
                    linguasAdicionais = current.linguasAdicionais + idioma,
                    merits = current.merits + novoMerito
                )
            } else {
                val aplicado = experienceActions.aplicarSeSaldoBpPermitir(current, mutado)
                if (aplicado === current) return@update current
                adicionada = true
                aplicado
            }
        }
        return adicionada
    }

    fun removeLinguaAdicional(lingua: String) {
        sheetState.update { current ->
            current.copy(
                linguasAdicionais = current.linguasAdicionais.filterNot { it.equals(lingua, ignoreCase = true) },
                merits = current.merits.filterNot {
                    it.origemAutomatica == "Idioma" && it.detalhe.equals(lingua, ignoreCase = true)
                }
            )
        }
    }
}
