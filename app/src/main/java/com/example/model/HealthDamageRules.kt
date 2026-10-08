package com.example.model

/**
 * Regras de progressão da Trilha de Vitalidade.
 *
 * Um nível de penalidade posterior só pode receber dano enquanto todas as
 * caixas das penalidades anteriores já estiverem marcadas. A regra é usada
 * tanto pela Aba 5 (Combate) quanto pela Aba 11 (Encontros).
 */
object HealthDamageRules {
    // APPROVED VISUAL/BEHAVIOR CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    fun podeMarcar(caixa: CaixaVitalidade, trilha: List<CaixaVitalidade>): Boolean {
        // Já marcada: permite ciclar o tipo de dano, inclusive para
        // desmarcá-la, sem exigir que a trilha esteja completa abaixo dela.
        if (caixa.tipoDano != 0) return true

        val sequencia = trilha.withIndex()
            .sortedBy { it.value.penaltyRank() }
            .map { it.value }
        val indice = sequencia.indexOfFirst { it.id == caixa.id }
        if (indice < 0) return false

        // A progressão é estritamente sequencial pela ordem visual da trilha:
        // esquerda para a direita dentro da linha e, somente depois, para a
        // linha seguinte. Isso vale também para caixas extras de Corpo de Touro
        // e para caixas adicionais concedidas por Méritos.
        return sequencia.take(indice).all { it.tipoDano != 0 }
    }
}
