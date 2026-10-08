package com.example.data

/**
 * Hierarquia fixa de navegação da Aba 8 pra Lunar — pedido explícito do
 * usuário, validado junto com a reextração de encantos_lunares.json:
 * 10 caixas (Universal + 9 Atributos), cada uma com suas subdivisões
 * temáticas. "Sangue do Coração" se repete em cada Atributo (exceto
 * Universal, que não tem subdivisão nenhuma) — são subdivisões DISTINTAS
 * por Atributo, não uma categoria compartilhada.
 */
object LunarCharmHierarchy {
    // Ordem de exibição das 10 caixas.
    val ORDEM_ATRIBUTOS = listOf(
        "Universal", "Força", "Destreza", "Vigor", "Carisma",
        "Manipulação", "Aparência", "Percepção", "Inteligência", "Raciocínio"
    )

    // Atributo -> subdivisões, na ordem em que aparecem no documento oficial.
    val SUBDIVISOES: Map<String, List<String>> = mapOf(
        "Universal" to emptyList(),
        "Força" to listOf("Sangue do Coração", "Ofensivo", "Mobilidade", "Feitos de Força"),
        "Destreza" to listOf("Sangue do Coração", "Ofensivo", "Defensivo", "Mobilidade", "Subterfúgio", "Enxame"),
        "Vigor" to listOf("Sangue do Coração", "Defensivo", "Resistência", "Fúria"),
        "Carisma" to listOf("Sangue do Coração", "Influência", "Guerra", "Território"),
        "Manipulação" to listOf("Sangue do Coração", "Influência", "Astúcia", "Subterfúgio"),
        "Aparência" to listOf("Sangue do Coração", "Influência", "Subterfúgio", "Guerra"),
        "Percepção" to listOf("Sangue do Coração", "Sentidos", "Escrutínio", "Misticismo"),
        "Inteligência" to listOf("Sangue do Coração", "Conhecimento", "Misticismo", "Ofícios", "Guerra", "Feitiçaria"),
        "Raciocínio" to listOf("Sangue do Coração", "Perseverança", "Afinidade com Animais", "Navegação", "Território")
    )

    fun subdivisoesDe(atributo: String): List<String> = SUBDIVISOES[atributo] ?: emptyList()
}
