package com.example.model

import java.util.UUID

data class Merito(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val valor: Int = 1,
    // "normal" | "sobrenatural" — vazio quando o mérito foi cadastrado
    // livremente (sem bater com o catálogo), preservando compatibilidade
    // com planilhas salvas antes desta mudança.
    val categoria: String = "",
    // Texto livre descrevendo o pré-requisito, só preenchido em méritos
    // cadastrados manualmente pelo usuário (méritos do catálogo têm seus
    // próprios pré-requisitos vindos da definição, não daqui).
    val preRequisitoTexto: String = "",
    val detalhe: String = "",
    val origemAutomatica: String = ""
) {
    /**
     * [detalhe] pronto para exibição. Alguns detalhes (ex.: nome de Artefato
     * de arma/armadura, montado como "Nome (Peso)" em EncounterEquipmentService)
     * já trazem um parêntese próprio. Como a linha do Mérito volta a envolver
     * o detalhe inteiro em outro parêntese, o resultado ficava com parênteses
     * aninhados: "Artefato (Chakram Infinito (Leve)) 3". Aqui o parêntese
     * interno é convertido para um travessão antes de ser reenvolvido, então
     * o resultado final é "Artefato 3 (Chakram Infinito – Leve)".
     */
    fun detalheParaExibicao(): String {
        val texto = detalhe.trim()
        if (texto.isEmpty()) return texto
        val match = REGEX_PARENTESE_FINAL.find(texto) ?: return texto
        val (base, especificacao) = match.destructured
        return "$base – $especificacao"
    }

    /** "Nome graduação (especificação)" — formato padronizado de exibição. */
    fun linhaExibicao(): String {
        val det = detalheParaExibicao()
        return if (det.isNotBlank()) "$nome $valor ($det)" else "$nome $valor"
    }

    /** "Nome (especificação)", sem a graduação — usado em títulos/cabeçalhos. */
    fun tituloExibicao(): String {
        val det = detalheParaExibicao()
        return if (det.isNotBlank()) "$nome ($det)" else nome
    }

    private companion object {
        // Casa só um único parêntese ao final do texto, sem parênteses
        // aninhados dentro dele (ex.: "Chakram Infinito (Leve)"), para não
        // mexer em detalhes que legitimamente usam parênteses no meio do
        // texto de outras formas.
        val REGEX_PARENTESE_FINAL = Regex("^(.*) \\(([^()]+)\\)$")
    }
}
