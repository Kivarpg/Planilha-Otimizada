package com.example.iniciativas

import kotlin.random.Random

/** Regras do teste secreto de Juntar-se à Batalha. */
object IniciativaRollService {
    /**
     * Rola a quantidade de dados indicada pelo valor de Juntar-se à Batalha.
     * 7, 8 e 9 valem 1 sucesso; 0 (10 no d10) vale 2 sucessos.
     * O resultado final recebe +3.
     */
    fun rolar(juntarSeABatalha: Int, random: Random = Random.Default): Int {
        val quantidadeDados = juntarSeABatalha.coerceAtLeast(0)
        var total = 0
        repeat(quantidadeDados) {
            total += when (random.nextInt(0, 10)) {
                0 -> 2 // 0 no d10
                7, 8, 9 -> 1
                else -> 0
            }
        }
        return total + 3
    }
}
