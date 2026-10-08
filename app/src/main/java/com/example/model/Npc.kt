package com.example.model

import java.util.UUID

// NPCs (Aba "NPCs") — não fazem parte da CharacterSheet: são uma lista
// separada, própria do aparelho, não vinculada a um personagem
// específico (um NPC costuma valer pra toda a campanha, não só pra uma
// planilha). Persistidos à parte pelo SheetRepository e exportáveis por um
// código de compartilhamento independente do código de planilha.
data class Npc(
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    val lealdade: String = "Neutro", // "Aliado" | "Inimigo" | "Neutro"
    val tipo: String = "",
    val descricao: String = ""
) {
}
