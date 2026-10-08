package com.example.data

import com.example.model.CaixaVitalidade
import java.util.UUID

/**
 * Centraliza a construção das trilhas de vitalidade de NPCs.
 *
 * O cálculo é puro: não depende de UI, ViewModel ou estado global.
 * Isso elimina duplicação entre os geradores e deixa as diferenças de
 * Corpo de Touro explícitas em uma única tabela de regras.
 */
object EncounterVitalityService {
    enum class Tipo { SOLAR, SANGUE_DE_DRAGAO }

    private fun base(): MutableList<CaixaVitalidade> = mutableListOf<CaixaVitalidade>().apply {
        add(caixa("-0", permanente = true))
        add(caixa("-1", permanente = true)); add(caixa("-1", permanente = true))
        add(caixa("-2", permanente = true)); add(caixa("-2", permanente = true))
        add(caixa("-4", permanente = true))
        add(caixa("Inc", permanente = true))
    }

    fun trilhaVitalidade(
        vigor: Int,
        corpoDeTouroCount: Int,
        tipo: Tipo = Tipo.SOLAR
    ): List<CaixaVitalidade> {
        val caixas = base()
        val vigorNormalizado = vigor.coerceAtLeast(0)
        repeat(corpoDeTouroCount.coerceAtLeast(0)) {
            when (tipo) {
                Tipo.SOLAR -> when {
                    vigorNormalizado <= 2 -> {
                        caixas += caixa("-1", permanente = false)
                        caixas += caixa("-2", permanente = false)
                    }
                    vigorNormalizado <= 4 -> {
                        caixas += caixa("-1", permanente = false)
                        caixas += caixa("-2", permanente = false)
                        caixas += caixa("-2", permanente = false)
                    }
                    else -> {
                        caixas += caixa("-0", permanente = false)
                        caixas += caixa("-1", permanente = false)
                        caixas += caixa("-2", permanente = false)
                    }
                }
                Tipo.SANGUE_DE_DRAGAO -> when {
                    vigorNormalizado <= 2 -> {
                        caixas += caixa("-2", permanente = false)
                        caixas += caixa("-2", permanente = false)
                    }
                    vigorNormalizado <= 4 -> {
                        caixas += caixa("-1", permanente = false)
                        caixas += caixa("-2", permanente = false)
                    }
                    else -> {
                        caixas += caixa("-1", permanente = false)
                        caixas += caixa("-2", permanente = false)
                        caixas += caixa("-2", permanente = false)
                    }
                }
            }
        }
        return caixas
    }

    fun trilhaVitalidadeSolar(vigor: Int, corpoDeTouroCount: Int): List<CaixaVitalidade> =
        trilhaVitalidade(vigor, corpoDeTouroCount, Tipo.SOLAR)

    fun trilhaVitalidadeSangueDeDragao(vigor: Int, corpoDeTouroCount: Int): List<CaixaVitalidade> =
        trilhaVitalidade(vigor, corpoDeTouroCount, Tipo.SANGUE_DE_DRAGAO)

    /**
     * Trilha de vitalidade do Lunar com Técnica do Corpo de Touro.
     *
     * A regra Lunar precisa ser separada da tabela Solar: com Vigor 5 ou
     * mais, cada aquisição de Corpo de Touro concede 2 caixas -2 e 2 caixas
     * -4. Isso evita que a regra genérica do Solar transforme as aquisições
     * em caixas -0/-1/-2.
     */
    fun trilhaVitalidadeLunar(vigor: Int, corpoDeTouroCount: Int): List<CaixaVitalidade> {
        val caixas = base()
        val vigorNormalizado = vigor.coerceAtLeast(0)
        repeat(corpoDeTouroCount.coerceAtLeast(0)) {
            when {
                vigorNormalizado <= 2 -> {
                    caixas += caixa("-1", permanente = false)
                    caixas += caixa("-2", permanente = false)
                }
                vigorNormalizado <= 4 -> {
                    caixas += caixa("-1", permanente = false)
                    caixas += caixa("-2", permanente = false)
                    caixas += caixa("-2", permanente = false)
                }
                else -> {
                    caixas += caixa("-2", permanente = false)
                    caixas += caixa("-2", permanente = false)
                    caixas += caixa("-4", permanente = false)
                    caixas += caixa("-4", permanente = false)
                }
            }
        }
        return caixas
    }

    private fun caixa(penalidade: String, permanente: Boolean): CaixaVitalidade =
        CaixaVitalidade(UUID.randomUUID().toString(), penalidade, 0, isPermanente = permanente)
}
