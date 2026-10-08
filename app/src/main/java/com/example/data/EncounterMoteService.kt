package com.example.data

import com.example.model.ArmaduraEncontro
import com.example.model.ArmaEncontro
import com.example.model.TipoExaltadoEncontro

/**
 * Fórmulas de Motes da criação de NPCs da Aba 11.
 *
 * Centraliza somente a matemática; o comprometimento de equipamento continua
 * sendo aplicado aqui para que Solar, Sangue de Dragão e Lunar usem o mesmo
 * ponto de finalização e não mantenham fórmulas duplicadas nos geradores.
 */
internal object EncounterMoteService {
    data class Resultado(
        val pessoais: Int,
        val perifericos: Int
    )

    data class Totais(
        val pessoaisMax: Int,
        val perifericosMax: Int,
        val comitados: Int,
        val perifericosDisponiveis: Int
    )

    fun totais(
        tipo: TipoExaltadoEncontro,
        essencia: Int,
        arma: ArmaEncontro?,
        armadura: ArmaduraEncontro?
    ): Totais {
        val (pessoaisBase, perifericosBase) = when (tipo) {
            TipoExaltadoEncontro.SOLAR ->
                (10 + essencia * 3) to (26 + essencia * 7)
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO ->
                (11 + essencia) to (23 + essencia * 4)
            TipoExaltadoEncontro.LUNAR ->
                (15 + essencia) to (34 + essencia * 4)
        }
        val comitados = (arma?.motesComitados ?: 0) + (armadura?.motesComitados ?: 0)
        return Totais(
            pessoaisMax = pessoaisBase,
            perifericosMax = perifericosBase,
            comitados = comitados,
            perifericosDisponiveis = (perifericosBase - comitados).coerceAtLeast(0)
        )
    }

    fun calcular(
        tipo: TipoExaltadoEncontro,
        essencia: Int,
        arma: ArmaEncontro?,
        armadura: ArmaduraEncontro
    ): Resultado {
        val totais = totais(tipo, essencia, arma, armadura)
        return Resultado(
            pessoais = totais.pessoaisMax,
            perifericos = totais.perifericosDisponiveis
        )
    }
}
