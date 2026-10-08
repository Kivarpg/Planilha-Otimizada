package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterSheet
import com.example.ui.components.GildedCard
import com.example.ui.theme.*
import com.example.viewmodel.BpBreakdown

@Composable
internal fun SummaryExperienceSection(sheet: CharacterSheet, bpInfo: BpBreakdown) {
    // "Resumo de pontos de bônus" removido a pedido — antes aparecia aqui
    // enquanto sheet.planilhaConcluida == false. Agora essa fase não mostra
    // nada nesta seção; o card de "Gasto dos pontos de experiência" (depois
    // da planilha concluída) continua igual.
    if (sheet.planilhaConcluida) {
        GildedCard(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppText("Gasto dos pontos de experiência", style=MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), fontWeight=FontWeight.Bold, color=ExaltedAmber, textAlign=TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                if(sheet.historicoExperiencia.isEmpty()) AppText("Nenhum ponto de Experiência gasto ainda.", style=MaterialTheme.typography.bodySmall, color=ExaltedMuted)
                else sheet.historicoExperiencia.forEach { gasto ->
                    Row(Modifier.fillMaxWidth().padding(vertical=2.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                        AppText("• ${gasto.descricao}", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurface, modifier=Modifier.weight(1f))
                        AppText("${gasto.custo} XP", style=MaterialTheme.typography.bodySmall, fontWeight=FontWeight.SemiBold, color=ExaltedGold)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical=5.dp), color=ExaltedAmber)
                AppText("Total Gasto: ${sheet.experienciaGastaTotal} / ${sheet.experienciaCalculada()} Experiência", fontWeight=FontWeight.Bold, color=ExaltedAccentBright)
                Spacer(Modifier.height(4.dp))
                AppText("Saldo Restante: ${sheet.experienciaDisponivel()} Experiência", fontWeight=FontWeight.Bold, color=if(sheet.experienciaDisponivel()>=0) ExaltedAmber else MaterialTheme.colorScheme.error)
            }
        }
    }
}
