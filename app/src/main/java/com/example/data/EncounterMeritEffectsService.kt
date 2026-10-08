package com.example.data

import com.example.model.CaixaVitalidade
import com.example.model.Merito
import java.util.UUID

/**
 * Efeitos mecânicos de Méritos que possuem correspondência direta no modelo
 * simplificado da Aba 11.
 *
 * Méritos puramente situacionais/narrativos continuam sendo carregados na
 * lista de Méritos, mas não recebem um efeito numérico inventado quando o
 * modelo do NPC não possui o recurso correspondente.
 */
object EncounterMeritEffectsService {
    private const val REFLEXOS_RAPIDOS = "Reflexos Rápidos"
    private const val PE_VELOZ = "Pé Veloz"
    private const val GIGANTE = "Gigante"
    private const val TOLERANCIA_A_DOR = "Tolerância à dor"

    // APPROVED PERFORMANCE REFACTOR
    // Um lote de recalculo costuma consultar vários efeitos do mesmo conjunto
    // de Méritos. Normalizamos os nomes uma única vez e reutilizamos o snapshot,
    // evitando quatro varreduras independentes da mesma lista.
    data class Efeitos(
        val bonusJuntarABatalha: Int,
        val bonusInvestida: Int,
        val bonusDesengajamento: Int,
        val temGigante: Boolean,
        val temToleranciaADor: Boolean
    )

    fun efeitos(meritos: List<Merito>): Efeitos {
        var reflexosRapidos = false
        var peVeloz = false
        var gigante = false
        var toleranciaADor = false
        for (merito in meritos) {
            when (merito.nome.trim().lowercase()) {
                REFLEXOS_RAPIDOS.lowercase() -> reflexosRapidos = true
                PE_VELOZ.lowercase() -> peVeloz = true
                GIGANTE.lowercase() -> gigante = true
                TOLERANCIA_A_DOR.lowercase() -> toleranciaADor = true
            }
        }
        return Efeitos(
            bonusJuntarABatalha = if (reflexosRapidos) 1 else 0,
            bonusInvestida = if (peVeloz) 1 else 0,
            bonusDesengajamento = if (peVeloz) 1 else 0,
            temGigante = gigante,
            temToleranciaADor = toleranciaADor
        )
    }

    fun bonusJuntarABatalha(meritos: List<Merito>): Int = efeitos(meritos).bonusJuntarABatalha

    fun bonusInvestida(meritos: List<Merito>): Int = efeitos(meritos).bonusInvestida

    fun bonusDesengajamento(meritos: List<Merito>): Int = efeitos(meritos).bonusDesengajamento

    /** Gigante concede um nível -0 adicional de vitalidade. */
    fun adicionarVitalidade(meritos: List<Merito>, base: List<CaixaVitalidade>): List<CaixaVitalidade> =
        adicionarVitalidade(efeitos(meritos), base)

    fun adicionarVitalidade(efeitos: Efeitos, base: List<CaixaVitalidade>): List<CaixaVitalidade> {
        if (!efeitos.temGigante) return base
        return base + CaixaVitalidade(
            id = UUID.randomUUID().toString(),
            penalidade = "-0",
            tipoDano = 0,
            isPermanente = true
        )
    }

    /**
     * Converte a penalidade efetiva de ferimento conforme Tolerância à dor:
     * -2 -> -1 e -4 -> -3. Incapacitado permanece -4.
     */
    fun penalidadeFerimentoEfetiva(meritos: List<Merito>, penalidadeTexto: String?): Int {
        val penalidade = when (penalidadeTexto) {
            null, "-0" -> 0
            "-1" -> 1
            "-2" -> 2
            "-4", "Inc" -> 4
            else -> 0
        }
        if (!efeitos(meritos).temToleranciaADor) return penalidade
        return when (penalidadeTexto) {
            "-2" -> 1
            "-4" -> 3
            else -> penalidade
        }
    }
}
