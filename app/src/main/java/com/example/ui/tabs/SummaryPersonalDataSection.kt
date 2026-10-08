package com.example.ui.tabs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
internal fun SummaryPersonalDataSection(sheet: CharacterSheet) {
// Personal Data & Caste
GildedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        val ehSangueDeDragao = sheet.tipoPersonagem.isDragonBlooded()
        val ehLunar = sheet.tipoPersonagem.isLunar()
        AppText(
            when {
                ehSangueDeDragao -> "1. Dados pessoais e aspecto"
                ehLunar -> "1. Dados pessoais e casta"
                else -> "1. Dados pessoais e casta"
            },
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = ExaltedAmber, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        AppText("Nome: ${sheet.nome.ifBlank { "-" }}")
        AppText("Jogador: ${sheet.jogador.ifBlank { "-" }}")
        AppText("Conceito: ${sheet.conceito.ifBlank { "-" }}")
        if (ehSangueDeDragao) {
            AppText("Aspecto: ${sheet.aspecto.ifBlank { "-" }}")
        } else if (ehLunar) {
            AppText("Casta: ${sheet.lunarCasta.displayName}")
            AppText("Atributos de Casta: ${sheet.lunarCasteAttributesEscolhidos.joinToString(", ").ifBlank { "Nenhum" }}")
            AppText("Atributos Favorecidos: ${sheet.favoredAttributes.joinToString(", ").ifBlank { "Nenhum" }}")
        } else {
            AppText("Casta: ${sheet.casta.displayName}")
            AppText("Supernal: ${sheet.supernalAbility ?: "Nenhuma"}")
            AppText("Quebra de Limite: ${sheet.limiteContador}/10")
            AppText("Falha de Virtude: ${sheet.falhaVirtude.ifBlank { "-" }}")
            AppText("Gatilho: ${sheet.limiteGatilho.ifBlank { "-" }}")
        }
    }
}
}
