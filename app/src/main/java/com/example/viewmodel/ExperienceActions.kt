package com.example.viewmodel

import com.example.model.CharacterSheet
import com.example.model.GastoExperiencia
import com.example.model.isDragonBlooded
import kotlinx.coroutines.flow.MutableStateFlow

// Núcleo de XP/Pontos de Bônus compartilhado — usado por praticamente
// todo domínio que "aumenta um valor" (Atributos, Habilidades,
// Especializações, Força de Vontade, Artes Marciais, Méritos, Encantos,
// Feitiços): 16 pontos de chamada ao todo espalhados pelo
// SheetViewModel original. É por isso que essas 2 funções ficaram pra
// ser extraídas por último — vários outros domínios (setAttributeRating,
// setAbilityRating, addSpecialization, updateMartialArtValue, addMerit)
// esperavam por elas antes de poderem sair do SheetViewModel também.
//
// Recebe _commitmentError por construtor (a MESMA instância do
// ViewModel principal) — as funções abaixo mostram erro de validação
// ao usuário como efeito colateral quando o custo não cabe no saldo
// disponível. Não guarda _sheetState: cada função recebe a planilha
// diretamente por parâmetro, então esse valor nunca era usado aqui.
class ExperienceActions(
    private val commitmentError: MutableStateFlow<String?>
) {

    // Debita um custo em Experiência (Modo Experiência, depois de "Planilha
    // Concluída") — usado por toda compra feita nesse modo. Retorna null
    // (sem aplicar nada) se o saldo de XP disponível não cobrir o custo,
    // mostrando o erro em commitmentError. Em Modo Livre, todo custo é
    // tratado como zero (a planilha simplesmente não é debitada).
    fun debitarExperiencia(sheet: CharacterSheet, custo: Int, descricao: String): CharacterSheet? {
        if (sheet.modoLivre) return sheet
        if (custo <= 0) return sheet
        val disponivel = sheet.experienciaDisponivel()
        if (custo > disponivel) {
            commitmentError.value = "Experiência insuficiente para esta compra (custo: $custo, disponível: $disponivel)."
            return null
        }
        val novoTotal = sheet.experienciaGastaTotal + custo
        val novaEssencia = if (sheet.tipoPersonagem.isDragonBlooded()) {
            SheetCalculations.essenceFromXpSangueDeDragao(novoTotal)
        } else {
            SheetCalculations.essenceFromXp(novoTotal)
        }
        return sheet.copy(
            experienciaGastaTotal = novoTotal,
            historicoExperiencia = sheet.historicoExperiencia + GastoExperiencia(descricao, custo),
            // Evita uma segunda cópia completa da CharacterSheet no caminho
            // quente de cada compra por XP. A regra/tabela de Essência é a
            // mesma usada por normalizeEssence().
            essencia = novaEssencia
        )
    }

    // Modo de criação (Pontos de Bônus): qualquer alteração que dependa de
    // gastar Pontos de Bônus é bloqueada automaticamente se o saldo
    // resultante ficasse negativo — o saldo nunca assume valor negativo.
    // No Modo Experiência (planilhaConcluida) ou Modo Livre, o gasto já é
    // controlado à parte (por debitarExperiencia() ou não é controlado de
    // todo), então esta função apenas deixa passar sem checagem.
    fun aplicarSeSaldoBpPermitir(current: CharacterSheet, mutado: CharacterSheet): CharacterSheet {
        if (current.planilhaConcluida || current.modoLivre) return mutado
        val saldoResultante = SheetCalculations.calculateBpBreakdown(mutado).remainingBalance
        if (saldoResultante < 0) {
            commitmentError.value = "Pontos de Bônus insuficientes para esta alteração."
            return current
        }
        return mutado
    }
}
