package com.example.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Parser e cache de pré-requisitos de Méritos (Aba 11).
 *
 * Extraído de EncounterMeritDistributionService (refatoração de organização —
 * roteiro de refatoração agressiva, sem mudança de comportamento).
 * Isola Regex/cache/avaliação para reduzir o tamanho do serviço de distribuição.
 */
internal object EncounterMeritPrerequisiteParser {
    private val MERITO_PRE_REQUISITO_DOTS = Regex("^(.+?)\\s*(•+)$")
    private val MERITO_PRE_REQUISITO_MAIS = Regex("^(.+?)\\s+(\\d+)\\+$")

    private val CACHE = ConcurrentHashMap<String, PreRequisitoParseado>()

    internal sealed interface PreRequisitoParseado {
        data object Vazio : PreRequisitoParseado
        data class AtributoOuHabilidade(val nomes: List<String>, val nivel: Int) : PreRequisitoParseado
        data class MeritoNivel(val nomeNormalizado: String, val nivel: Int) : PreRequisitoParseado
        data object OutroSobrenatural : PreRequisitoParseado
        data object TextoLivre : PreRequisitoParseado
    }

    fun parsear(requisito: String): PreRequisitoParseado =
        CACHE.computeIfAbsent(requisito) { textoOriginal ->
            val texto = textoOriginal.trim()
            if (texto.isBlank()) return@computeIfAbsent PreRequisitoParseado.Vazio

            val dots = MERITO_PRE_REQUISITO_DOTS.matchEntire(texto)
            if (dots != null) {
                val nivel = dots.groupValues[2].length
                val nomes = dots.groupValues[1]
                    .split(",", " ou ")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                return@computeIfAbsent PreRequisitoParseado.AtributoOuHabilidade(nomes, nivel)
            }

            val mais = MERITO_PRE_REQUISITO_MAIS.matchEntire(texto)
            if (mais != null) {
                return@computeIfAbsent PreRequisitoParseado.MeritoNivel(
                    nomeNormalizado = mais.groupValues[1].trim().lowercase(),
                    nivel = mais.groupValues[2].toInt()
                )
            }

            if (texto.startsWith("Outro Mérito Sobrenatural", ignoreCase = true)) {
                return@computeIfAbsent PreRequisitoParseado.OutroSobrenatural
            }

            // Texto livre não possui representação mecânica suficiente no modelo.
            PreRequisitoParseado.TextoLivre
        }

    fun baseAtendidos(
        prerequisitos: List<PreRequisitoParseado>,
        niveisEfetivos: Map<String, Int>
    ): Boolean = prerequisitos.all { requisito ->
        when (requisito) {
            PreRequisitoParseado.Vazio,
            PreRequisitoParseado.OutroSobrenatural,
            PreRequisitoParseado.TextoLivre -> true
            is PreRequisitoParseado.MeritoNivel -> true
            is PreRequisitoParseado.AtributoOuHabilidade -> requisito.nomes.any { nome ->
                (niveisEfetivos[nome] ?: 0) >= requisito.nivel
            }
        }
    }
}
