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
internal fun SummaryCombatSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
// Combat Readonly (sem Vitalidade, que passou para o item 5)
GildedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        AppText("4. Combate", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = ExaltedAmber, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        AppText("Essência: ${sheet.essencia}")
        AppText("Motes Pessoais: ${sheet.motesPessoaisDisponiveis()}/${sheet.motesPessoaisMax()}")
        AppText("Motes Periféricos: ${sheet.motesPerifericosDisponiveis()}/${sheet.motesPerifericosMax()}")
        Spacer(modifier = Modifier.height(10.dp))
        com.example.ui.components.WillpowerTrack(
            valor = sheet.forcaVontadeBase,
            usados = sheet.forcaVontadeUsados,
            planilhaConcluida = sheet.planilhaConcluida,
            onValorChange = { viewModel.updateForcaVontadeBase(it) },
            onToggleUsado = { viewModel.toggleForcaVontadeUsado(it) }
        )
        Spacer(modifier = Modifier.height(7.dp))

        val armaduraEquipada = sheet.armaduraEquipada()
        val armasEquipadas = sheet.armasEquipadas()

        if (armasEquipadas.isEmpty() && armaduraEquipada == null) {
            Spacer(modifier = Modifier.height(6.dp))
            AppText("Nenhuma arma ou armadura equipada.", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted)
        } else {
            if (armasEquipadas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                AppText("Arma(s) Equipada(s):", fontWeight = FontWeight.Bold, color = ExaltedGold)
                armasEquipadas.forEach { w ->
                    val nomeExibido = if (w.ataqueDesarmado) "${w.nome} [Ataque Desarmado]" else w.nome
                    val comitamentoReal = w.motesPessoaisComitados + w.motesPerifericosComitados
                    AppText(
                        "• $nomeExibido — Ini: ${w.iniciativa} | Dec: ${w.decisivo} | Defesa: ${w.defesa} | Dano: ${w.dano} | Dano Mín: ${w.danoMinimo.ifBlank { "-" }} | Comitamento: $comitamentoReal",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExaltedOnSurface
                    )
                }
            }
            if (armaduraEquipada != null) {
                Spacer(modifier = Modifier.height(6.dp))
                AppText("Armadura Equipada:", fontWeight = FontWeight.Bold, color = ExaltedGold)
                AppText(
                    "• ${armaduraEquipada.nome} — Absorção: ${armaduraEquipada.absorcao} | Dureza: ${armaduraEquipada.dureza} | Penalidade: ${armaduraEquipada.penalidadeMobilidade} | Comitamento: ${com.example.model.ArmorStatsTable.stats(armaduraEquipada.tipoArmadura, armaduraEquipada.categoriaPeso).comitamento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedOnSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValorResumoCard(label = "Juntar-se à Batalha", valor = sheet.juntarBatalhaCalculado(), modifier = Modifier.weight(1f))
            ValorResumoCard(label = "Absorção Total", valor = sheet.absorcaoTotalCalculada(), modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValorResumoCard(label = "Evasão", valor = sheet.evasaoCalculada(), modifier = Modifier.weight(1f))
            ValorResumoCard(label = "Aparar", valor = sheet.apararCalculado(), modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValorResumoCard(label = "Astúcia", valor = sheet.astuciaCalculada(), modifier = Modifier.weight(1f))
            ValorResumoCard(label = "Perseverança", valor = sheet.perseverancaCalculada(), modifier = Modifier.weight(1f))
        }
    }
}
}
