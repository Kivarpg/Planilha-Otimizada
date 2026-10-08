package com.example.data

/**
 * Catálogo de armas Mundanas — pedido explícito do usuário, mesma
 * estrutura e padrão visual do catálogo de armas Artefato
 * (WeaponCatalog.kt). Diferença chave: cada arma tem um [custoRecursos]
 * (nível de Recursos exigido pra possuir o equipamento, 0-5), em vez de
 * um custo de Pontos de Mérito — Artefatos usam PM, Mundanas usam
 * Recursos, são sistemas de custo completamente diferentes.
 */
object WeaponCatalogMundano {

    data class CatalogoArmaMundana(
        val nome: String,
        val peso: String, // Leve | Média | Pesada
        val custoRecursos: Int, // 0-5 — "—" no material de origem = 0
        val etiquetas: List<String>
    )

    private val ARMAS_BRANCAS_LEVES = listOf(
        CatalogoArmaMundana("Cestus", "Leve", 1, listOf("Contusão", "Briga", "Esmagamento", "Vestível")),
        CatalogoArmaMundana("Clava/Cassetete/Bastão", "Leve", 0, listOf("Contusão", "Armas brancas", "Esmagamento", "às vezes Arremesso (Curto)", "Improvisada")),
        CatalogoArmaMundana("Khatar", "Leve", 2, listOf("Letal", "Briga", "Perfuração")),
        CatalogoArmaMundana("Faca", "Leve", 0, listOf("Letal", "Armas brancas", "Arremesso (Curto)")),
        CatalogoArmaMundana("Sai", "Leve", 2, listOf("Letal", "Armas brancas", "Desarmamento")),
        CatalogoArmaMundana("Espada Curta", "Leve", 1, listOf("Letal", "Armas brancas", "Equilibrada")),
        CatalogoArmaMundana("Garras de Tigre", "Leve", 1, listOf("Letal", "Briga", "Vestível")),
        CatalogoArmaMundana("Desarmado", "Leve", 0, listOf("Contusão", "Briga", "Agarrar", "Natural")),
        CatalogoArmaMundana("Chicote", "Leve", 1, listOf("Contusão", "Armas brancas", "Desarmamento", "Flexível", "Agarrar", "Alcance")),
        CatalogoArmaMundana("Roda de Vento e Fogo/Leque de Guerra", "Leve", 2, listOf("Letal", "Artes Marciais", "Desarmamento"))
    )

    private val ARMAS_BRANCAS_MEDIAS = listOf(
        CatalogoArmaMundana("Machado/Machadinha", "Média", 1, listOf("Letal", "Armas brancas", "Arremesso (Curto)", "Corte")),
        CatalogoArmaMundana("Espada de Corte", "Média", 2, listOf("Letal", "Armas brancas", "Corte")),
        CatalogoArmaMundana("Corrente de Combate", "Média", 1, listOf("Contusão", "Artes Marciais", "Desarmamento", "Flexível", "Agarrar", "Alcance")),
        CatalogoArmaMundana("Espada de Gancho", "Média", 3, listOf("Letal", "Artes Marciais", "Desarmamento", "pelo par")),
        CatalogoArmaMundana("Javelim", "Média", 1, listOf("Letal", "Armas brancas", "Arremesso (Médio)")),
        CatalogoArmaMundana("Maça/Martelo", "Média", 1, listOf("Contusão", "Armas brancas", "Esmagamento", "às vezes Improvisada")),
        CatalogoArmaMundana("Bastão de Sete Seções", "Média", 2, listOf("Contusão", "Artes Marciais", "Desarmar", "Flexível")),
        CatalogoArmaMundana("Escudo", "Média", 1, listOf("Contusão", "Armas brancas", "Escudo")),
        CatalogoArmaMundana("Lança Curta", "Média", 1, listOf("Letal", "Armas brancas", "Arremesso (Curto)", "Perfuração")),
        CatalogoArmaMundana("Lança", "Média", 1, listOf("Letal", "Armas brancas", "Perfuração", "Alcance")),
        CatalogoArmaMundana("Espada Reta", "Média", 2, listOf("Letal", "Armas brancas", "Equilibrada")),
        CatalogoArmaMundana("Bastão", "Média", 0, listOf("Contusão", "Armas brancas", "Alcance"))
    )

    // Força ••• exigida pra usar sem penalidade — anotado na etiqueta,
    // já que o modelo de dados atual não tem um campo de pré-requisito
    // de Atributo pra armas (diferente do peso, que é rastreado).
    private val ARMAS_BRANCAS_PESADAS = listOf(
        CatalogoArmaMundana("Machado Grande/Foice", "Pesada", 1, listOf("Letal", "Armas brancas", "Corte", "Alcance", "Duas Mãos", "Força •••")),
        CatalogoArmaMundana("Espada Grande", "Pesada", 2, listOf("Letal", "Armas brancas", "Equilibrada", "Alcance", "Duas Mãos", "Força •••")),
        CatalogoArmaMundana("Lança de Cavalaria", "Pesada", 1, listOf("Letal", "Armas brancas", "Perfuração", "Alcance", "Duas mãos (quando a pé)", "Força •••")),
        CatalogoArmaMundana("Machado de Haste/Alabarda", "Pesada", 2, listOf("Letal", "Armas brancas", "Corte", "Alcance", "Duas mãos", "Força •••")),
        CatalogoArmaMundana("Marreta/Tetsubo", "Pesada", 2, listOf("Impacto", "Armas brancas", "Alcance", "Esmagamento", "Duas mãos", "Força •••"))
    )

    private val ARREMESSO_LEVES = listOf(
        CatalogoArmaMundana("Chakram", "Leve", 1, listOf("Letal", "Arremesso (Médio)", "Corte", "Montado")),
        CatalogoArmaMundana("Dardo", "Leve", 1, listOf("Letal", "Arremesso (Médio)", "Ocultável", "Montado", "Envenenável")),
        CatalogoArmaMundana("Agulha", "Leve", 1, listOf("Sutil", "Arremesso (Curto)", "Ocultável", "Envenenável")),
        CatalogoArmaMundana("Shuriken", "Leve", 1, listOf("Letal", "Arremesso (Curto)", "Ocultável", "Montado", "Envenenável")),
        CatalogoArmaMundana("Funda", "Leve", 1, listOf("Impacto", "Arremesso (Médio)", "Ocultável", "Especial"))
    )

    private val ARREMESSO_MEDIAS = listOf(
        CatalogoArmaMundana("Funda de Haste", "Média", 1, listOf("Impacto", "Arremesso (Médio)", "Especial")),
        CatalogoArmaMundana("Bumerangue de Guerra", "Média", 1, listOf("Letal", "Arremesso (Médio)", "Corte", "Montado", "Especial"))
    )

    private val ARQUEIRISMO_LEVES = listOf(
        CatalogoArmaMundana("Besta de Mão", "Leve", 2, listOf("Letal", "Arqueirismo (Médio)", "Besta", "Montado", "Uma mão", "Perfuração", "Lento", "às vezes Ocultável", "Ocultável custa •••")),
        CatalogoArmaMundana("Arco Simples", "Leve", 1, listOf("Letal", "Arqueirismo (Longo)", "Montado"))
    )

    private val ARQUEIRISMO_MEDIAS = listOf(
        CatalogoArmaMundana("Arco Composto", "Média", 3, listOf("Letal", "Arqueirismo (Longo)", "Montado")),
        CatalogoArmaMundana("Besta", "Média", 3, listOf("Letal", "Arqueirismo (Longo)", "Besta", "Perfurante", "Poderoso", "Lento")),
        CatalogoArmaMundana("Arma de Fogo Portátil", "Média", 2, listOf("Letal", "Arqueirismo (Curto)", "Fogo", "Montado", "Uma Mão", "Lento")),
        CatalogoArmaMundana("Arco Longo", "Média", 2, listOf("Letal", "Arqueirismo (Longo)"))
    )

    private val ARQUEIRISMO_PESADAS = listOf(
        CatalogoArmaMundana("Lança-Fogo", "Pesada", 3, listOf("Letal", "Arqueirismo (Curto)", "Fogo", "Lento"))
    )

    /**
     * Todas as armas mundanas de uma Habilidade, agrupadas por peso —
     * mesmo formato de retorno de WeaponCatalog.candidatasPorHabilidade().
     */
    fun candidatasPorHabilidade(habilidadeCombate: String): List<CatalogoArmaMundana> = when (habilidadeCombate) {
        "Armas Brancas" -> (ARMAS_BRANCAS_LEVES + ARMAS_BRANCAS_MEDIAS + ARMAS_BRANCAS_PESADAS)
            .filter { it.etiquetas.any { etiqueta -> etiqueta.equals("Armas brancas", ignoreCase = true) } }
        "Briga" -> listOf(
            ARMAS_BRANCAS_LEVES.first { it.nome == "Cestus" },
            ARMAS_BRANCAS_LEVES.first { it.nome == "Khatar" },
            ARMAS_BRANCAS_LEVES.first { it.nome == "Garras de Tigre" },
            ARMAS_BRANCAS_LEVES.first { it.nome == "Desarmado" }
        )
        "Arremesso" -> ARREMESSO_LEVES + ARREMESSO_MEDIAS
        "Arqueirismo" -> ARQUEIRISMO_LEVES + ARQUEIRISMO_MEDIAS + ARQUEIRISMO_PESADAS
        else -> emptyList()
    }
}
