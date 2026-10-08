package com.example.data

import com.example.model.ArmaEncontro
import com.example.model.ArmaduraEncontro
import com.example.model.ArquetipoEncontro
import kotlin.math.ceil

/**
 * Cálculos compartilhados de combate para os geradores de NPC.
 *
 * // APPROVED REFACTORING
 * // DO NOT DUPLICATE COMBAT FORMULAS IN ARCHETYPE GENERATORS.
 * A única diferença entre Solar, Sangue de Dragão e Lunar que interessa aqui
 * é a parametrização recebida. As fórmulas permanecem centralizadas para que
 * uma correção de combate seja aplicada aos três arquétipos simultaneamente.
 */
object EncounterCombatCalculationService {
    data class Resultado(
        val acaoPrincipal: Int,
        val acaoDecisiva: Int,
        val defesaPrimaria: Int,
        val esquiva: Int,
        val dano: String
    )

    private const val BONUS_ESPECIALIZACAO_ATAQUE = 1
    private const val BONUS_ESPECIALIZACAO_DEFESA = 1

    fun calcular(
        arquetipo: ArquetipoEncontro,
        habilidadeCombate: String,
        attributes: Map<String, Int>,
        abilities: Map<String, Int>,
        arma: ArmaEncontro?,
        armadura: ArmaduraEncontro
    ): Resultado {
        val destreza = attributes["Destreza"] ?: 1
        val habAtaque = abilities[habilidadeCombate] ?: 0

        return when (arquetipo) {
            ArquetipoEncontro.FISICO -> calcularFisico(
                habilidadeCombate = habilidadeCombate,
                destreza = destreza,
                habilidadeAtaque = habAtaque,
                forca = attributes["Força"] ?: 1,
                arma = arma,
                armadura = armadura,
                esquiva = abilities["Esquiva"] ?: 0,
                briga = abilities["Briga"] ?: 0
            )
            ArquetipoEncontro.SOCIAL,
            ArquetipoEncontro.MENTAL -> {
                val acao = destreza + habAtaque + BONUS_ESPECIALIZACAO_ATAQUE
                Resultado(
                    acaoPrincipal = acao,
                    acaoDecisiva = 0,
                    defesaPrimaria = calcularApararComBriga(destreza, abilities["Briga"] ?: 0, arma),
                    esquiva = calcularEsquiva(destreza, abilities["Esquiva"] ?: 0, armadura),
                    dano = ""
                )
            }
        }
    }

    private fun calcularFisico(
        habilidadeCombate: String,
        destreza: Int,
        habilidadeAtaque: Int,
        forca: Int,
        arma: ArmaEncontro?,
        armadura: ArmaduraEncontro,
        esquiva: Int,
        briga: Int
    ): Resultado {
        return when (habilidadeCombate) {
            "Armas Brancas", "Briga" -> {
                val fulminante = destreza + habilidadeAtaque + BONUS_ESPECIALIZACAO_ATAQUE + (arma?.precisao ?: 0)
                val decisivo = destreza + habilidadeAtaque + BONUS_ESPECIALIZACAO_ATAQUE
                val aparar = calcularApararComBriga(destreza, habilidadeAtaque, arma)
                val tipoDano = if (arma == null) "C" else "L"
                val danoTotal = forca + (arma?.dano ?: 0)
                Resultado(
                    acaoPrincipal = fulminante,
                    acaoDecisiva = decisivo,
                    defesaPrimaria = aparar,
                    esquiva = calcularEsquiva(destreza, esquiva, armadura),
                    dano = "${if (arma == null) "+" else ""}$danoTotal$tipoDano"
                )
            }
            else -> {
                // Arremesso/Arqueirismo não usam Precisão fixa da arma.
                val fulminante = destreza + habilidadeAtaque + BONUS_ESPECIALIZACAO_ATAQUE
                val decisivo = destreza + habilidadeAtaque + BONUS_ESPECIALIZACAO_ATAQUE
                Resultado(
                    acaoPrincipal = fulminante,
                    acaoDecisiva = decisivo,
                    defesaPrimaria = calcularApararComBriga(destreza, briga, arma),
                    esquiva = calcularEsquiva(destreza, esquiva, armadura),
                    dano = "${arma?.dano ?: 0}L"
                )
            }
        }
    }

    /**
     * Aparar usa sempre Briga quando a habilidade ofensiva não é Armas Brancas/Briga.
     * O valor de Briga pode ser zero; a fórmula continua sendo aplicada.
     */
    private fun calcularApararComBriga(destreza: Int, briga: Int, arma: ArmaEncontro?): Int =
        ceil((destreza + briga + BONUS_ESPECIALIZACAO_DEFESA) / 2.0).toInt() + (arma?.defesa ?: 0)

    private fun calcularEsquiva(destreza: Int, esquiva: Int, armadura: ArmaduraEncontro): Int =
        (ceil((destreza + esquiva + BONUS_ESPECIALIZACAO_DEFESA) / 2.0).toInt() - armadura.penalidadeMobilidade).coerceAtLeast(0)
}
