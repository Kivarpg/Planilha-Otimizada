package com.example.model

// Aspecto de Sangue de Dragão — paralelo a Casta (Solar), mas com 5
// habilidades por Aspecto (não 8) e sem Habilidade Supernal nem Quebra
// de Limite (ver especificação "Mudanças – Template Sangue de Dragão").
enum class Aspecto(val displayName: String) {
    Ar(BoxNames.DragonBloodedAspect.AIR),
    Terra(BoxNames.DragonBloodedAspect.EARTH),
    Fogo(BoxNames.DragonBloodedAspect.FIRE),
    Agua(BoxNames.DragonBloodedAspect.WATER),
    Madeira(BoxNames.DragonBloodedAspect.WOOD);

    fun allowedAbilities(): List<String> = when (this) {
        Ar -> listOf("Linguística", "Conhecimento", "Ocultismo", "Furtividade", "Arremesso")
        Terra -> listOf("Prontidão", "Ofícios", "Integridade", "Resistência", "Guerra")
        Fogo -> listOf("Atletismo", "Esquiva", "Armas Brancas", "Presença", "Socialização")
        Agua -> listOf("Briga", "Burocracia", "Investigação", "Crime", "Navegação")
        Madeira -> listOf("Arqueirismo", "Medicina", "Performance", "Cavalgar", "Sobrevivência")
    }

    // Nome do recurso em drawable-nodpi (ver res/drawable-nodpi/aspecto_*.webp).
    fun nomeRecursoImagem(): String = when (this) {
        Ar -> "aspecto_ar"
        Terra -> "aspecto_terra"
        Fogo -> "aspecto_fogo"
        Agua -> "aspecto_agua"
        Madeira -> "aspecto_madeira"
    }
}
