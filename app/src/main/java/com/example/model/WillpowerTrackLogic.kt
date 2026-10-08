package com.example.model

/**
 * Mantém a trilha de Força de Vontade sequencial.
 *
 * Um toque em uma caixa vazia consome o primeiro ponto disponível da esquerda
 * para a direita. Um toque em qualquer caixa já marcada devolve o último ponto
 * marcado da sequência, independentemente da caixa tocada.
 */
object WillpowerTrackLogic {
    fun toggle(usados: Set<Int>, valor: Int, indice: Int): Set<Int> {
        val limite = valor.coerceAtLeast(0)
        if (limite == 0 || indice !in 1..limite) return usados

        return if (indice in usados) {
            val ultimoMarcado = usados.maxOrNull() ?: return usados
            usados - ultimoMarcado
        } else {
            val primeiroDisponivel = (1..limite).firstOrNull { it !in usados } ?: return usados
            usados + primeiroDisponivel
        }
    }
}
