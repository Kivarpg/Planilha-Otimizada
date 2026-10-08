package com.example.data

/**
 * Separa utilidade presente de crescimento futuro.
 *
 * O crescimento é deliberadamente limitado: ele pode desempatar ou justificar
 * um pré-requisito coerente, mas não transformar uma opção fraca hoje na melhor
 * escolha apenas por abrir muitas possibilidades futuras.
 */
internal object EncounterBuildQuality {
    const val MAX_FUTURE_GROWTH_BONUS = 3

    data class Score(
        val current: Int,
        val future: Int
    ) {
        val total: Int get() = current + future.coerceIn(0, MAX_FUTURE_GROWTH_BONUS)
    }

    fun combine(current: Int, future: Int): Score =
        Score(current = current, future = future.coerceIn(0, MAX_FUTURE_GROWTH_BONUS))
}
