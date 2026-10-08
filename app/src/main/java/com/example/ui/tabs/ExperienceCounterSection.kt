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
import com.example.viewmodel.SheetViewModel

@Composable
internal fun ExperienceCounterSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
// Contador Dinâmico de Experiência: funciona como a carteira de pontos
// do personagem — gerencia sessões jogadas e o saldo de Experiência.
GildedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        AppText("Contador dinâmico de experiência", style = MaterialTheme.typography.titleMedium, color = ExaltedAmber, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        SyncedCounterColumn(
            label = "Sessões Jogadas",
            value = sheet.sessoes,
            step = 1,
            minVal = 0,
            maxVal = 60,
            onValueChange = { viewModel.updateSessoes(it) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExperienceStat(
                label = "XP Total",
                valor = sheet.experienciaCalculada(),
                cor = ExaltedGold,
                modifier = Modifier.weight(1f)
            )
            ExperienceStat(
                label = "XP Gasto",
                valor = sheet.experienciaGastaTotal,
                cor = ExaltedMuted,
                modifier = Modifier.weight(1f)
            )
            ExperienceStat(
                label = "XP Disponível",
                valor = sheet.experienciaDisponivel(),
                cor = ExaltedAmber,
                destaque = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
}
