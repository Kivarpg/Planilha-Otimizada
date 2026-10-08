package com.example.data

/**
 * Catálogo de armaduras Mundanas — pedido explícito do usuário, mesma
 * estrutura do catálogo de armaduras Artefato (ArmorCatalog.kt).
 * Diferença chave: [custoRecursos] (nível de Recursos exigido), não
 * custo de Pontos de Mérito.
 */
object ArmorCatalogMundano {

    data class CatalogoArmaduraMundana(
        val nome: String,
        val peso: String, // Leve | Média | Pesada
        val custoRecursos: Int,
        val etiquetas: List<String>
    )

    private val TODAS = listOf(
        CatalogoArmaduraMundana("Peitoral", "Leve", 2, emptyList()),
        CatalogoArmaduraMundana("Gibão de Couro Reforçado", "Leve", 1, emptyList()),
        CatalogoArmaduraMundana("Camisa de Malha", "Leve", 2, listOf("Ocultável")),
        CatalogoArmaduraMundana("Cota de Malha Longa (Hauberk)", "Média", 3, emptyList()),
        CatalogoArmaduraMundana("Armadura Lamelar", "Média", 3, emptyList()),
        CatalogoArmaduraMundana("Peitoral Reforçado", "Média", 3, emptyList()),
        CatalogoArmaduraMundana("Gibão de Couro Reforçado e Acolchoado", "Média", 2, listOf("Silencioso")),
        CatalogoArmaduraMundana("Armadura de Placas Articuladas", "Pesada", 5, emptyList()),
        CatalogoArmaduraMundana("Armadura Mista (Placas e Malha)", "Pesada", 4, emptyList())
    )

    /** Todas as armaduras mundanas de um peso — mesmo formato de retorno de ArmorCatalog.candidatas(). */
    fun candidatas(peso: String): List<CatalogoArmaduraMundana> = TODAS.filter { it.peso == peso }
}
