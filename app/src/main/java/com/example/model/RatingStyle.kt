package com.example.model

// Preferência global de exibição para trilhas de avaliação (Atributos,
// Habilidades, Méritos) — configurável pelo usuário no menu de
// Configurações (engrenagem no topo do app), independente da planilha ativa.
enum class RatingStyle {
    DIAMOND,  // trilha de círculos (●●○○○)
    STEPPER   // botões −/número/+
}
