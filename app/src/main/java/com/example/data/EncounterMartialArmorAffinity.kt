package com.example.data

/**
 * Afinidade de armadura entre estilos. "Sem armadura" é sempre uma configuração
 * candidata comum: incompatibilidade de classes de armadura não torna estilos
 * mutuamente ilegais.
 */
internal object EncounterMartialArmorAffinity {
    private val CLASSES = listOf<String?>(null, "Leve", "Média", "Pesada")

    fun supports(style: EstiloArteMarcialDefinition, armorClass: String?): Boolean {
        if (armorClass == null) return true
        return EncounterMartialArtsSelectionService.armaduraCompativel(style, armorClass)
    }

    fun commonConfigurations(styles: List<EstiloArteMarcialDefinition>): List<String?> =
        CLASSES.filter { armor -> styles.all { supports(it, armor) } }

    fun bestCommonArmor(styles: List<EstiloArteMarcialDefinition>): String? {
        val common = commonConfigurations(styles)
        return when {
            "Pesada" in common -> "Pesada"
            "Média" in common -> "Média"
            "Leve" in common -> "Leve"
            else -> null
        }
    }

    fun affinity(first: EstiloArteMarcialDefinition, second: EstiloArteMarcialDefinition): Int =
        when (bestCommonArmor(listOf(first, second))) {
            "Pesada" -> 5
            "Média" -> 4
            "Leve" -> 3
            else -> 1 // ainda conciliáveis removendo a armadura
        }
}
