package com.example.data

import com.example.model.TipoExaltadoEncontro
import kotlin.random.Random

/**
 * Fonte canônica das Falhas de Virtude/Limites exibidas na seção Casta
 * (Aba 2) e usadas na caracterização dos NPCs da Aba 11.
 *
 * Solares e Lunares possuem tabelas próprias. Sangue de Dragão não usa
 * esta mecânica e, portanto, não recebe opções nem sorteio de Limite.
 */
object EncounterLimitCatalog {
    val SOLAR: List<String> = listOf(
        "Avareza Desenfreada",
        "Coração de Pedra",
        "Correntes da Honra",
        "Crueldade Deliberada",
        "Desprezo dos Virtuosos",
        "Dúvida Esmagadora",
        "Fúria Frenética",
        "Luto Avassalador",
        "Martírio Compassivo"
    )

    val LUNAR: List<String> = listOf(
        "Anseio da Besta-Tesoureira Avariciosa",
        "Crueldade que Espalha o Terror",
        "Desfile da Sereia Vaidosa",
        "Espírito do Demônio Risonho",
        "Fome Primordial Indomada",
        "Fúria do Beemote Enfurecido",
        "Julgamento Devorador de Pecadores",
        "Presságio do Barghest à Espreita",
        "Proclamação do Tirano Trovejante"
    )

    fun para(tipo: TipoExaltadoEncontro): List<String> = when (tipo) {
        TipoExaltadoEncontro.SOLAR -> SOLAR
        TipoExaltadoEncontro.LUNAR -> LUNAR
        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> emptyList()
    }

    fun sortear(tipo: TipoExaltadoEncontro, casta: String, random: Random): String {
        if (casta.isBlank()) return ""
        return para(tipo).let { tabela -> if (tabela.isEmpty()) "" else tabela.random(random) }
    }
}
