package com.example.model

import java.util.UUID

data class EncantoQuadro(
    val titulo: String = "",
    val texto: String = ""
)

data class Encanto(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val nomeIngles: String = "",
    val habilidadeVinculada: String,
    val custo: String = "",
    val mins: String = "",
    val minHabilidade: Int = 0,
    val minEssencia: Int = 0,
    val tipo: String = "",
    val palavrasChave: String = "",
    val duracao: String = "",
    val preRequisitos: String = "",
    val descricao: String = "",
    val quadros: List<EncantoQuadro> = emptyList(),
    // Categoria do poder: "Encanto", "Feitiçaria" ou "Necromancia".
    val categoria: String = "Encanto",
    // Círculo do Feitiço ("Terrestre"/"Celestial"/"Solar"); vazio para Encantos comuns.
    val circulo: String = "",
    // Posição de fixação (1 a 5) dentro da gaveta da Aba 8 — null significa
    // não fixado (ordenação alfabética normal). Numeração é POR GAVETA, não
    // global: cada gaveta tem seus próprios pinos 1-5 independentes.
    val pinOrder: Int? = null
) {
}

// Chave de agrupamento em "gavetas" (Aba 8) — poderes de categoria diferente
// de "Encanto" (Feitiçaria, Necromancia, futuras categorias) sempre ganham
// gaveta própria, mesmo compartilhando a mesma Habilidade vinculada (ex.:
// Feitiços são todos "Ocultismo", mas não devem se misturar com Encantos de
// Ocultismo comuns). Só Encantos comuns agrupam pela Habilidade de verdade.
fun Encanto.gavetaChave(): String = if (categoria == "Encanto") habilidadeVinculada else categoria
