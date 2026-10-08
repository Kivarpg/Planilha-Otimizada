package com.example.model

import java.util.UUID

data class Intimidade(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val tipo: String, // "Laço" or "Princípio"
    val intensidade: String // "Menor", "Maior", or "Definidora"
) {
}

data class Especializacao(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val habilidade: String,
    val valor: Int = 1
) {
}

// Artes Marciais: lista dinâmica de habilidades com nome editável (estilos)
data class HabilidadeCustomizada(
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "Nova Arte Marcial",
    val valor: Int = 0
) {
}

data class GastoExperiencia(
    val descricao: String,
    val custo: Int
) {
}
