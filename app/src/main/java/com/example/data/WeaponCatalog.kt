package com.example.data

/**
 * Catálogo de nomes de armas Artefato reais, organizados por Habilidade
 * ofensiva e categoria de peso — pedido explícito do usuário. Usado tanto
 * na geração de NPCs (Aba 11) quanto como sugestão de nomes na planilha do
 * jogador (Aba 5), substituindo o nome genérico "Arma de X (peso)" pelo
 * nome de uma arma específica do catálogo.
 *
 * Armas Brancas e Briga compartilham a mesma lista física de armas
 * (Armas Brancas Leves/Médias/Pesadas), mas cada Habilidade consulta uma
 * categoria de etiqueta diferente dentro dela: Armas Brancas filtra pela
 * etiqueta "Armas brancas" (todas as armas de combate corpo a corpo,
 * incluindo as Pesadas); Briga filtra especificamente pela etiqueta
 * "Briga" — e só entre as armas Leves ("Briga — Leve").
 *
 * Armas marcadas apenas "Artes Marciais" (Corrente Mortal, Cajado Ferrão
 * de Serpente) não entram em nenhuma das duas listas — pertencem a uma
 * Habilidade separada, fora do escopo desta geração automática.
 */
object WeaponCatalog {

    data class CatalogoArma(
        val nome: String,
        val peso: String, // "Leve" | "Média" | "Pesada"
        val etiquetas: List<String>
    )

    private val ARMAS_BRANCAS_LEVES = listOf(
        CatalogoArma("Direlash", "Leve", listOf("Letal", "Armas brancas", "Desarmar", "Agarrar", "Flexível", "Alcance")),
        CatalogoArma("Garras de Lâmina", "Leve", listOf("Letal", "Briga", "Ocultável", "Vestível")),
        CatalogoArma("Daiklave Curta", "Leve", listOf("Letal", "Armas brancas", "Equilibrada")),
        CatalogoArma("Khatar Assassino", "Leve", listOf("Letal", "Briga", "Perfurante", "às vezes Vestível")),
        CatalogoArma("Punho Esmagador", "Leve", listOf("Contusão", "Briga", "Agarrar", "Esmagamento", "Vestível"))
    )

    private val ARMAS_BRANCAS_MEDIAS = listOf(
        CatalogoArma("Daiklave", "Média", listOf("Letal", "Armas brancas", "Equilibrada")),
        CatalogoArma("Corrente Mortal", "Média", listOf("Contusão", "Artes Marciais", "Desarmar", "Flexível", "Agarrar", "Alcance")),
        CatalogoArma("Goremaul", "Média", listOf("Contusão", "Armas brancas", "Esmagamento")),
        CatalogoArma("Grimcleaver", "Média", listOf("Letal", "Armas brancas", "Corte")),
        CatalogoArma("Presa Longa", "Média", listOf("Letal", "Armas brancas", "Arremesso (Curto)", "Perfurante")),
        CatalogoArma("Daiklave Ceifadora", "Média", listOf("Letal", "Armas brancas", "Equilibrada")),
        CatalogoArma("Daiklave Devastadora", "Média", listOf("Letal", "Armas brancas", "Corte")),
        CatalogoArma("Cajado Ferrão de Serpente", "Média", listOf("Contusão", "Artes Marciais", "Desarmar", "Flexível", "Alcance")),
        CatalogoArma("Cajado de Tortura", "Média", listOf("Contusão", "Armas brancas", "Alcance"))
    )

    private val ARMAS_BRANCAS_PESADAS = listOf(
        CatalogoArma("Lança Mortal", "Pesada", listOf("Letal", "Armas brancas", "Perfurante", "Alcance", "Duas mãos (quando a pé)")),
        CatalogoArma("Grande Daiklave", "Pesada", listOf("Letal", "Armas brancas", "Equilibrada", "Alcance", "Duas mãos")),
        CatalogoArma("Grande Goremaul", "Pesada", listOf("Contusão", "Armas brancas", "Alcance", "Esmagadora", "Duas mãos")),
        CatalogoArma("Grande Grimcleaver", "Pesada", listOf("Letal", "Armas brancas", "Corte", "Alcance", "Duas mãos")),
        CatalogoArma("Grande Foice", "Pesada", listOf("Letal", "Armas brancas", "Perfuração", "Alcance", "Duas mãos"))
    )

    private val ARMAS_DE_ARREMESSO_LEVES = listOf(
        CatalogoArma("Chakram Infinito", "Leve", listOf("Letal", "Arremesso (Médio)", "Corte", "Especial")),
        CatalogoArma("Funda da Surpresa Distante", "Leve", listOf("Letal", "Arremesso (Longo)", "Ocultável", "Especial"))
    )

    private val ARMAS_DE_ARREMESSO_MEDIAS = listOf(
        CatalogoArma("Funda de Cajado Gloriosa", "Média", listOf("Letal", "Arremesso (Longo)")),
        CatalogoArma("Skycutter", "Média", listOf("Letal", "Arremesso (Longo)", "Corte", "Especial"))
    )

    private val ARMAS_DE_ARQUEIRISMO_LEVES = listOf(
        CatalogoArma("Arco Curto do Poder", "Leve", listOf("Letal", "Arqueirismo (Longo)", "Montado"))
    )

    private val ARMAS_DE_ARQUEIRISMO_MEDIAS = listOf(
        CatalogoArma("Arco do Poder", "Média", listOf("Letal", "Arqueirismo (Longo)")),
        CatalogoArma("Besta de Cerco", "Média", listOf("Letal", "Arqueirismo (Longo)", "Besta", "Perfuração", "Poderosa", "Lenta"))
    )

    private val ARMAS_DE_ARQUEIRISMO_PESADAS = listOf(
        CatalogoArma("Varinha do Suspiro do Dragão", "Pesada", listOf("Letal", "Arqueirismo (Curto)", "Chama", "Lenta"))
    )

    private val ARMAS_ARTES_MARCIAIS = listOf(
        // Armas do Estilo do Diabo Justo. Permanecem no domínio Artes Marciais:
        // a natureza física da arma não transforma o ataque em Arqueirismo.
        CatalogoArma("Varinha de Fogo", "Média", listOf("Letal", "Artes Marciais", "Pistola de Fogo", "Chama")),
        CatalogoArma("Pistola de Fogo", "Média", listOf("Letal", "Artes Marciais", "Pistola de Fogo", "Chama")),
        CatalogoArma("Revólver de Fogo", "Média", listOf("Letal", "Artes Marciais", "Pistola de Fogo", "Chama"))
    )

    private val TODAS_ARMAS_BRANCAS = ARMAS_BRANCAS_LEVES + ARMAS_BRANCAS_MEDIAS + ARMAS_BRANCAS_PESADAS

    /**
     * Lista de candidatas pra uma Habilidade + peso específicos, seguindo
     * as regras de filtragem por etiqueta descritas pelo usuário. Retorna
     * lista vazia se a combinação Habilidade/peso não existir no catálogo
     * (ex.: Arremesso não tem categoria Pesada).
     */
    fun candidatas(habilidadeCombate: String, peso: String): List<CatalogoArma> = when (habilidadeCombate) {
        "Armas Brancas" -> TODAS_ARMAS_BRANCAS.filter {
            it.peso == peso && it.etiquetas.contains("Armas brancas")
        }
        "Briga" -> if (peso == "Leve") ARMAS_BRANCAS_LEVES.filter { it.etiquetas.contains("Briga") } else emptyList()
        "Arremesso" -> when (peso) {
            "Leve" -> ARMAS_DE_ARREMESSO_LEVES
            "Média" -> ARMAS_DE_ARREMESSO_MEDIAS
            else -> emptyList()
        }
        "Arqueirismo" -> when (peso) {
            "Leve" -> ARMAS_DE_ARQUEIRISMO_LEVES
            "Média" -> ARMAS_DE_ARQUEIRISMO_MEDIAS
            "Pesada" -> ARMAS_DE_ARQUEIRISMO_PESADAS
            else -> emptyList()
        }
        else -> emptyList()
    }

    /** Todas as candidatas de uma Habilidade, em qualquer peso — usado pra sugestão na Aba 5. */
    fun candidatasPorHabilidade(habilidadeCombate: String): List<CatalogoArma> = when (habilidadeCombate) {
        "Armas Brancas" -> TODAS_ARMAS_BRANCAS.filter { it.etiquetas.contains("Armas brancas") }
        "Briga" -> ARMAS_BRANCAS_LEVES.filter { it.etiquetas.contains("Briga") }
        "Arremesso" -> ARMAS_DE_ARREMESSO_LEVES + ARMAS_DE_ARREMESSO_MEDIAS
        "Arqueirismo" -> ARMAS_DE_ARQUEIRISMO_LEVES + ARMAS_DE_ARQUEIRISMO_MEDIAS + ARMAS_DE_ARQUEIRISMO_PESADAS
        "Artes Marciais" -> ARMAS_ARTES_MARCIAIS + TODAS_ARMAS_BRANCAS.filter { it.etiquetas.contains("Artes Marciais") }
        else -> emptyList()
    }
}
