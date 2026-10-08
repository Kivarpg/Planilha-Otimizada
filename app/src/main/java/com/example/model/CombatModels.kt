package com.example.model

import java.util.UUID

data class CaixaVitalidade(
    val id: String = UUID.randomUUID().toString(),
    val penalidade: String, // "-0", "-1", "-2", "-4", "Inc"
    val tipoDano: Int = 0, // 0: Livre, 1: Contusão (/), 2: Letal (X), 3: Agravado (*)
    val isPermanente: Boolean = false,
    // Nome do Encanto que gerou esta caixa automaticamente (ex.: "Técnica do
    // Corpo de Touro"). null = caixa base ou adicionada manualmente pelo
    // usuário. Caixas com origem automática não podem ser removidas
    // manualmente — somente removendo o Encanto ou recalculando (ex.: ao
    // mudar Vigor) elas são atualizadas/removidas.
    val origemAutomatica: String? = null
) {
    fun penaltyRank(): Int = when (penalidade) {
        "-0" -> 0
        "-1" -> 1
        "-2" -> 2
        "-4" -> 3
        "Inc" -> 4
        else -> 5
    }

}

// Extraído de CharacterSheet.penalidadeFerimentoAtual() e
// NpcEncontro.penalidadeFerimentoAtual() (refatoração de organização —
// pedido explícito do usuário). Mesma lógica de "achar a pior caixa
// danificada" duplicada nos dois — só o pós-processamento do resultado
// difere entre eles (CharacterSheet mapeia direto pra Int; NpcEncontro
// repassa pro serviço de efeitos de Mérito).
fun List<CaixaVitalidade>.piorPenalidadeDeFerimento(): String? {
    var piorCaixa: CaixaVitalidade? = null
    var piorRank = Int.MIN_VALUE
    for (box in this) {
        if (box.tipoDano == 0) continue
        val rank = box.penaltyRank()
        if (rank > piorRank) {
            piorRank = rank
            piorCaixa = box
        }
    }
    return piorCaixa?.penalidade
}
