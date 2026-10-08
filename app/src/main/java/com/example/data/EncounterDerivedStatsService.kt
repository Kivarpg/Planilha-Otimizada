package com.example.data

import com.example.model.ArmaduraEncontro

/** Pure calculations shared by Solar and Dragon-Blooded encounter generation. */
object EncounterDerivedStatsService {

    // Características derivadas compartilhadas entre gerar() (Solar) e
    // gerarSangueDeDragao() — extraído aqui porque a fórmula em si era
    // idêntica nos dois (só o bônus de especialidade chegava calculado
    // de formas diferentes: Solar verifica condicionalmente se há
    // especialidade na habilidade, Sangue de Dragão usa um bônus fixo —
    // essa diferença real fica no chamador, passada como parâmetro; a
    // aritmética (seções 11.7-11.11) é a mesma pros dois).
    data class DerivadosComuns(
        val absorcaoNatural: Int, val absorcaoTotal: Int, val perseveranca: Int,
        val astucia: Int, val juntarABatalha: Int, val investida: Int, val desengajamento: Int
    )

    fun calcularDerivadosComuns(
        attributes: Map<String, Int>,
        abilities: Map<String, Int>,
        armadura: ArmaduraEncontro,
        bonusPerseveranca: Int,
        bonusAstucia: Int,
        bonusJuntarBatalha: Int,
        bonusInvestida: Int = 0,
        bonusDesengajamento: Int = 0
    ): DerivadosComuns {
        val destreza = attributes["Destreza"] ?: 1
        val absorcaoNatural = attributes["Vigor"] ?: 1
        val absorcaoTotal = absorcaoNatural + armadura.absorcao
        // 11.7/11.8: a especialidade some no cálculo como +1 fixo quando
        // aplicável (ver bonusPerseveranca/bonusAstucia de cada chamador).
        val perseveranca = kotlin.math.ceil(((attributes["Raciocínio"] ?: 1) + (abilities["Integridade"] ?: 0) + bonusPerseveranca) / 2.0).toInt()
        val astucia = kotlin.math.ceil(((attributes["Manipulação"] ?: 1) + (abilities["Socialização"] ?: 0) + bonusAstucia) / 2.0).toInt()
        val juntarABatalha = (attributes["Raciocínio"] ?: 1) + (abilities["Prontidão"] ?: 0) + bonusJuntarBatalha
        // Investida se beneficia da especialidade de Atletismo; Desengajamento,
        // da especialidade de Esquiva (pedido explícito do usuário).
        val investida = kotlin.math.ceil((destreza + (abilities["Atletismo"] ?: 0) + bonusInvestida) / 2.0).toInt()
        val desengajamento = kotlin.math.ceil((destreza + (abilities["Esquiva"] ?: 0) + bonusDesengajamento) / 2.0).toInt()
        return DerivadosComuns(absorcaoNatural, absorcaoTotal, perseveranca, astucia, juntarABatalha, investida, desengajamento)
    }

}
