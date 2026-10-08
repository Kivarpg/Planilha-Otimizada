package com.example.ui.tabs

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
internal fun SummaryCharmsSection(sheet: CharacterSheet, viewModel: com.example.viewmodel.SheetViewModel) {
var encantoEmDetalhes by remember { mutableStateOf<Encanto?>(null) }
// Charms & Spells Readonly
GildedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        AppText("6. Encantos (${sheet.charms.size})", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = ExaltedAmber, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        if (sheet.charms.isEmpty()) {
            AppText("Nenhum encanto, feitiço ou necromancia cadastrado.", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted)
        } else {
            // Agrupado por Habilidade (Encantos comuns) ou por
            // categoria (Feitiçaria/Necromancia sempre em seção
            // própria — ver Encanto.gavetaChave()). groupBy preserva
            // a ordem de primeira aparição das chaves e a ordem de
            // inserção dentro de cada lista — ou seja, ordem de
            // compra, sem qualquer influência da organização por
            // pino/alfabética da Aba 8.
            var numeroEncanto = 1
            // APPROVED PERFORMANCE REFACTOR
            // O agrupamento depende somente da lista de Encantos. Memorizar
            // por identidade da lista evita reconstruir o mapa a cada
            // recomposição causada por estado de outras seções da planilha.
            val encantosPorGaveta = remember(sheet.charms) {
                sheet.charms.groupBy { it.gavetaChave() }
            }
            encantosPorGaveta.forEach { (nomeGaveta, itensDaGaveta) ->
                // Título da Habilidade com destaque: cor própria
                // (diferente da cor dos itens abaixo) + sublinhado,
                // pra não se confundir visualmente com os Encantos.
                AppText(
                    nomeGaveta,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.Bold,
                    color = ExaltedAccentBright,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .drawBehind {
                            val y = size.height - 1.dp.toPx()
                            drawLine(
                                color = ExaltedAccentBright,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                )
                val encantosAgrupados = groupAccumulatedCharms(itensDaGaveta)
                encantosAgrupados.forEach { grupo ->
                    val c = grupo.charm
                    val nomeExibicao = if (grupo.quantity > 1) "${c.nome} (x${grupo.quantity})" else c.nome
                    AppText(
                        "${numeroEncanto++}. $nomeExibicao",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = ExaltedOnSurface,
                        modifier = Modifier
                            .pointerInput(c.id, c.nome) {
                                detectTapGestures(onLongPress = { encantoEmDetalhes = c })
                            }
                            .feedbackOnPress()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    encantoEmDetalhes?.let { encanto ->
        CharmDetailsDialog(
            charm = encanto,
            onDismiss = { encantoEmDetalhes = null },
            viewModel = viewModel,
            tipoPersonagem = sheet.tipoPersonagem
        )
    }
}
}
