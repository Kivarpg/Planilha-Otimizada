package com.example.viewmodel

import com.example.data.EncantosSolaresCatalog
import com.example.data.EncantoSolarDefinition
import com.example.model.CharacterSheet
import com.example.model.NOME_CORPO_DE_TOURO

/** Regras de limite de recompra / máximo de aquisições de Encantos. */
internal object CharmsRepurchaseRules {
    private val REPURCHASE_ESSENCE_VALUE = Regex("(?i)ess[eê]ncia\\s+(\\d+)\\+")

    private fun thresholdsDeEssencia(def: EncantoSolarDefinition): List<Int> {
        // A exigência pode aparecer antes ou depois de "recompra" na mesma
        // frase, por exemplo: "Com Essência 3+, este Encanto pode ser
        // recomprado" ou "Uma recompra com Essência 5+".
        return def.descricao
            .replace('\n', ' ')
            .split(Regex("[.!?]"))
            .asSequence()
            .filter { it.contains("recompr", ignoreCase = true) }
            .mapNotNull { REPURCHASE_ESSENCE_VALUE.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            .toList()
    }

    /**
     * Essência mínima da próxima recompra, quando o texto do Encanto
     * explicita essa exigência. quantidadeAtual=1 significa a segunda
     * aquisição; quantidadeAtual=2 significa a terceira, e assim por diante.
     */
    fun essenciaMinimaParaRecompra(def: EncantoSolarDefinition, quantidadeAtual: Int): Int? {
        if (quantidadeAtual <= 0) return null
        return thresholdsDeEssencia(def).getOrNull(quantidadeAtual - 1)
    }

    fun maximoDeAquisicoes(def: EncantoSolarDefinition, sheet: CharacterSheet): Int {
        if (EncantosSolaresCatalog.sameName(def.nome, NOME_CORPO_DE_TOURO)) return sheet.limiteCorpoDeTouro()
        if (EncantosSolaresCatalog.sameName(def.nome, "Método de Manifestação do Destino")) {
            return 1 + (sheet.charms.size / 10)
        }
        val texto = def.descricao
        val recomprasComEssencia = thresholdsDeEssencia(def).size
        if (recomprasComEssencia > 0) return 1 + recomprasComEssencia
        if (EncantosSolaresCatalog.sameName(def.nome, "Técnica dos Traços Bestiais")) return 4
        if (REGEX_RECOMPRA_UNICA_M.containsMatchIn(texto) ||
            REGEX_RECOMPRA_UNICA_F.containsMatchIn(texto)) return 2
        if (REGEX_SEGUNDA_RECOMPRA.containsMatchIn(texto)) return 3
        if (REGEX_TERCEIRA_RECOMPRA.containsMatchIn(texto)) return 3
        val mencoesDeRecompra = REGEX_MENCAO_RECOMPRA.findAll(texto).count()
        if (mencoesDeRecompra > 0) return 1 + mencoesDeRecompra
        return 1
    }
}
