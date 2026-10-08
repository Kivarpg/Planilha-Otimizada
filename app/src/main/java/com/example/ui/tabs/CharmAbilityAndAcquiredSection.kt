package com.example.ui.tabs
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
internal fun CharmAbilityAndAcquiredSection(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    selectedAbility: String?,
    feiticariaHabilitada: Boolean,
    onSelectAbility: (String?) -> Unit,
    onShowAbilityTree: (String) -> Unit,
    gavetasExpandidas: Set<String>,
    onToggleGaveta: (String) -> Unit,
    onShowDetail: (Encanto) -> Unit,
    onLongPress: (Encanto) -> Unit
) {
GildedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val botoesPrincipais = if (sheet.tipoPersonagem.isLunar()) {
                com.example.data.LunarCharmHierarchy.ORDEM_ATRIBUTOS
            } else {
                ExaltedConstants.ALL_25_ABILITIES
            }
            botoesPrincipais.forEach { ability ->
                CharmAbilityButton(
                    text = ability,
                    selected = selectedAbility == ability,
                    enabled = true,
                    onClick = { onSelectAbility(if (selectedAbility == ability) null else ability) },
                    onLongPress = { onShowAbilityTree(ability) },
                    modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CharmAbilityButton(
                text = "Feitiços",
                selected = selectedAbility == "Feitiços",
                // A consulta dos feitiços é livre; a compra continua bloqueada
                // pelos círculos disponíveis em addFeiticoFromDefinition.
                enabled = true,
                onClick = { onSelectAbility(if (selectedAbility == "Feitiços") null else "Feitiços") },
                modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight)
            )
            CharmAbilityButton(
                text = "Artes Marciais",
                selected = selectedAbility == "Artes Marciais",
                // A consulta dos estilos/encantos é livre; a aquisição continua
                // sujeita ao mérito Artista Marcial.
                enabled = true,
                onClick = { onSelectAbility(if (selectedAbility == "Artes Marciais") null else "Artes Marciais") },
                // Artes Marciais não possuem uma árvore global: cada estilo é
                // uma árvore própria. O long press abre o mesmo seletor de
                // estilos do toque normal; dali o usuário abre a árvore do
                // estilo desejado sem misturar seus Encantos com Briga ou com
                // outros estilos.
                onLongPress = { onSelectAbility("Artes Marciais") },
                modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight)
            )
            CharmAbilityButton(text = "Necromancia", selected = false, enabled = false, onClick = {}, modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight))
        }
    }
}

Spacer(Modifier.height(16.dp))

SectionHeader(title = "Encantos adquiridos (${sheet.charms.size})")
if (sheet.charms.isEmpty()) {
    AppText(
        "Nenhum Encanto adquirido.",
        style = MaterialTheme.typography.bodySmall,
        color = ExaltedMuted,
        modifier = Modifier.padding(14.dp)
    )
} else {
    // Gavetas por Habilidade (Encantos comuns) ou por categoria
    // (Feitiçaria/Necromancia sempre em gaveta própria — ver
    // Encanto.gavetaChave()). Dentro de cada gaveta: pinados
    // primeiro (ordem de pino 1-5), depois o restante em ordem
    // alfabética.
    val gavetas = remember(sheet.charms) {
        sheet.charms
            .groupBy { it.gavetaChave() }
            .toSortedMap()
            .mapValues { (_, itens) ->
                itens.sortedWith(
                    compareBy<Encanto> { it.pinOrder ?: Int.MAX_VALUE }
                        .thenBy { it.nome }
                )
            }
    }
    gavetas.forEach { (nomeGaveta, itensDaGaveta) ->
        val expandida = nomeGaveta in gavetasExpandidas
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .feedbackClickable {
                    onToggleGaveta(nomeGaveta)
                }
        ) {
            AppText(if (expandida) "▾" else "▸", color = ExaltedGold, modifier = Modifier.padding(end = 6.dp))
            AppText(
                "$nomeGaveta (${itensDaGaveta.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ExaltedAccentBright
            )
        }
        if (expandida) {
            val encantosAgrupados = remember(itensDaGaveta) {
                groupAccumulatedCharms(itensDaGaveta)
            }
            encantosAgrupados.forEach { grupo ->
                AcquiredCharmCard(
                    charm = grupo.charm,
                    quantity = grupo.quantity,
                    onClick = { onShowDetail(grupo.charm) },
                    onLongPress = { onLongPress(grupo.charm) },
                    isPinned = grupo.members.any { it.pinOrder != null },
                    onTogglePin = {
                        val alvoPin = grupo.members.firstOrNull { it.pinOrder != null } ?: grupo.charm
                        viewModel.toggleCharmPin(alvoPin.id)
                    }
                )
            }
        }
    }
}
}
