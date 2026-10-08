package com.example.data

/**
 * Catálogo de nomes de armaduras Artefato reais, organizado por peso —
 * pedido explícito do usuário, mesmo padrão do WeaponCatalog. Cada item
 * carrega seu próprio custo em pontos de Mérito: a maioria custa o
 * padrão de 3 (Artefato •••), mas a Armadura de Seda é Artefato ••••
 * (4 pontos) — pedido explícito do usuário.
 */
object ArmorCatalog {

    data class CatalogoArmadura(
        val nome: String,
        val peso: String, // "Leve" | "Média" | "Pesada"
        val marcadores: List<String>,
        val custoMerito: Int = 3
    )

    const val NOME_ARMADURA_DE_SEDA = "Armadura de Seda"

    private val ARMADURAS_LEVES = listOf(
        CatalogoArmadura("Peitoral", "Leve", emptyList()),
        CatalogoArmadura("Camisa de Malha", "Leve", listOf("Ocultável")),
        CatalogoArmadura(NOME_ARMADURA_DE_SEDA, "Leve", listOf("Silenciosa", "Especial"), custoMerito = 4)
    )

    private val ARMADURAS_MEDIAS = listOf(
        CatalogoArmadura("Armadura Lamelar", "Média", emptyList()),
        CatalogoArmadura("Jaqueta Acolchoada Reforçada", "Média", listOf("Silenciosa"))
    )

    private val ARMADURAS_PESADAS = listOf(
        CatalogoArmadura("Peitoral Reforçado", "Pesada", emptyList()),
        CatalogoArmadura("Armadura de Placas Articuladas", "Pesada", emptyList())
    )

    fun candidatas(peso: String): List<CatalogoArmadura> = when (peso) {
        "Leve" -> ARMADURAS_LEVES
        "Média" -> ARMADURAS_MEDIAS
        "Pesada" -> ARMADURAS_PESADAS
        else -> emptyList()
    }

    /**
     * Sorteia uma armadura pro peso e Habilidade de combate informados.
     * Armadura de Seda (Leve) é favorecida pra NPCs de Briga na maioria
     * das vezes, mas não exclusivamente — pedido explícito do usuário:
     * 70% de chance de sair Seda quando Briga e peso Leve; caso
     * contrário (ou fora desse caso), sorteio uniforme entre todas as
     * candidatas do peso, Seda incluída.
     */
    fun sortear(peso: String, habilidadeCombate: String?, random: kotlin.random.Random): CatalogoArmadura? {
        val candidatas = candidatas(peso)
        if (candidatas.isEmpty()) return null
        if (peso == "Leve" && habilidadeCombate == "Briga" && random.nextInt(0, 100) < 70) {
            return candidatas.first { it.nome == NOME_ARMADURA_DE_SEDA }
        }
        return candidatas.random(random)
    }
}
