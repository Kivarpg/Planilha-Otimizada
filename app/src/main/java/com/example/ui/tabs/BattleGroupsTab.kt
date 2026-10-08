package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.feedbackOnPress

import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButtonVariant
import com.example.iniciativas.adicionarOuAtualizarBattleGroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.BattleGroupRules
import com.example.data.battleGroupTroopTypes
import com.example.model.BattleGroup
import com.example.model.BattleGroupCustomAttack
import com.example.model.BattleGroupCustomStats
import com.example.model.BattleGroupDrill
import com.example.ui.components.GildedCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedOutline
import com.example.viewmodel.SheetViewModel

/** Battle Group management UI. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BattleGroupsTab(viewModel: SheetViewModel) {
    val groups by viewModel.battleGroups.collectAsState()
    var editingId by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(battleGroupTroopTypes.firstOrNull()?.name) }
    var size by remember { mutableStateOf(1) }
    var drill by remember { mutableStateOf(BattleGroupDrill.POOR) }
    var might by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var customJoin by remember { mutableStateOf("0") }
    var customAttacks by remember { mutableStateOf(listOf(BattleGroupCustomAttack("Ataque", 0, 0, null))) }
    var customDefense by remember { mutableStateOf("0") }
    var customMagnitude by remember { mutableStateOf("0") }
    var customSoak by remember { mutableStateOf("0") }
    var customSenses by remember { mutableStateOf("0") }
    var customResolve by remember { mutableStateOf("0") }
    var customResist by remember { mutableStateOf("") }
    var customRout by remember { mutableStateOf("") }
    var customPerfectMorale by remember { mutableStateOf(false) }

    fun clearEditor() {
        editingId = null
        selectedType = battleGroupTroopTypes.firstOrNull()?.name
        size = 1
        drill = BattleGroupDrill.POOR
        might = 0
        name = ""
        customJoin = "0"
        customAttacks = listOf(BattleGroupCustomAttack("Ataque", 0, 0, null))
        customDefense = "0"
        customMagnitude = "0"
        customSoak = "0"
        customSenses = "0"
        customResolve = "0"
        customResist = ""
        customRout = ""
        customPerfectMorale = false
    }

    fun edit(group: BattleGroup) {
        editingId = group.id
        selectedType = group.troopTypeName
        size = group.size.coerceIn(1, 5)
        drill = group.drill
        might = group.might.coerceIn(0, 3)
        name = group.name
        group.customStats?.let { custom ->
            customJoin = custom.joinBattle.toString()
            customAttacks = custom.attacks.ifEmpty { listOf(BattleGroupCustomAttack("Ataque", 0, 0, null)) }
            customDefense = custom.defenseBase.toString()
            customMagnitude = custom.magnitudeBase.toString()
            customSoak = custom.soakBase.toString()
            customSenses = custom.senses.toString()
            customResolve = custom.resolve.toString()
            customResist = custom.resist
            customRout = custom.routDifficulty?.toString() ?: ""
            customPerfectMorale = custom.perfectMorale
        }
    }

    fun save() {
        val custom = if (selectedType == null) BattleGroupCustomStats(
            joinBattle = customJoin.toIntOrNull() ?: 0,
            attacks = customAttacks,
            defenseBase = customDefense.toIntOrNull() ?: 0,
            magnitudeBase = customMagnitude.toIntOrNull() ?: 0,
            soakBase = customSoak.toIntOrNull() ?: 0,
            senses = customSenses.toIntOrNull() ?: 0,
            resolve = customResolve.toIntOrNull() ?: 0,
            resist = customResist,
            routDifficulty = customRout.toIntOrNull(),
            perfectMorale = customPerfectMorale
        ) else null
        viewModel.salvarBattleGroup(
            BattleGroup(
                id = editingId ?: java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                troopTypeName = selectedType,
                size = size,
                drill = drill,
                might = might,
                customStats = custom
            )
        )
        clearEditor()
    }

    val coresCampoTexto = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ExaltedAccentBright,
        unfocusedBorderColor = ExaltedOutline.copy(alpha = 0.55f),
        focusedLabelColor = ExaltedAccentBright,
        unfocusedLabelColor = ExaltedMuted,
        cursorColor = ExaltedGold,
        focusedTextColor = ExaltedOnSurface,
        unfocusedTextColor = ExaltedOnSurface,
        focusedContainerColor = ExaltedDarkSurface,
        unfocusedContainerColor = ExaltedDarkSurface
    )

    Column(Modifier.fillMaxSize().exaltedTabIdentity(13).exaltedContentStage(13).verticalScroll(rememberScrollState()).padding(12.dp)) {
        GildedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { AppText("Nome") }, modifier = Modifier.fillMaxWidth(), colors = coresCampoTexto)
                Spacer(Modifier.height(8.dp))
                AppText("Tipo de tropa", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                InkButton(label = selectedType ?: "Custom Army", onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth(), size = InkButtonSize.Small, fillMaxWidth = true)
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    battleGroupTroopTypes.forEach { type ->
                        DropdownMenuItem({ AppText(type.name, color = ExaltedOnSurface) }, { selectedType = type.name; menuOpen = false })
                    }
                    DropdownMenuItem({ AppText("Custom Army", color = ExaltedOnSurface) }, { selectedType = null; menuOpen = false })
                }
                Spacer(Modifier.height(6.dp))
                SelectorRow("Tamanho", size, 1..5) { size = it }
                SelectorRow("Mobilização", drill.ordinal, BattleGroupDrill.entries.indices, displayValue = drillLabel(drill)) { drill = BattleGroupDrill.entries[it] }
                SelectorRow("Potência", might, 0..3) { might = it }
                if (selectedType == null) {
                    Spacer(Modifier.height(8.dp))
                    AppText("Custom Army — entrada manual", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                    IntField("Join Battle", customJoin, coresCampoTexto) { customJoin = it }
                    val attack = customAttacks.firstOrNull() ?: BattleGroupCustomAttack("Ataque", 0, 0, null)
                    Spacer(Modifier.height(6.dp))
                    AppText("Ataque", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                    OutlinedTextField(
                        attack.name,
                        { value -> customAttacks = listOf(attack.copy(name = value)) },
                        label = { AppText("Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = coresCampoTexto
                    )
                    IntField("Attack", attack.attackBase.toString(), coresCampoTexto) { value -> customAttacks = listOf(attack.copy(attackBase = value.toIntOrNull() ?: 0)) }
                    IntField("Damage", attack.damageBase.toString(), coresCampoTexto) { value -> customAttacks = listOf(attack.copy(damageBase = value.toIntOrNull() ?: 0)) }
                    IntField("Minimum Dice", attack.minimumDice?.toString() ?: "", coresCampoTexto) { value -> customAttacks = listOf(attack.copy(minimumDice = value.toIntOrNull())) }
                    IntField("Defense", customDefense, coresCampoTexto) { customDefense = it }
                    IntField("Magnitude", customMagnitude, coresCampoTexto) { customMagnitude = it }
                    IntField("Soak", customSoak, coresCampoTexto) { customSoak = it }
                    IntField("Senses", customSenses, coresCampoTexto) { customSenses = it }
                    IntField("Resolve", customResolve, coresCampoTexto) { customResolve = it }
                    OutlinedTextField(customResist, { customResist = it }, label = { AppText("Resist") }, modifier = Modifier.fillMaxWidth(), colors = coresCampoTexto)
                    IntField("Rout Difficulty", customRout, coresCampoTexto) { customRout = it }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(customPerfectMorale, { customPerfectMorale = it }, modifier = Modifier.feedbackOnPress()); AppText("Perfect Morale", color = ExaltedOnSurface) }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InkButton(label = if (editingId == null) "Salvar" else "Atualizar", enabled = name.isNotBlank(), onClick = ::save, size = InkButtonSize.Small)
                    if (editingId != null) InkButton(label = "Cancelar", onClick = ::clearEditor, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        groups.forEach { group ->
            val troop = group.troopTypeName?.let { n -> battleGroupTroopTypes.firstOrNull { it.name == n } }
            val derived = BattleGroupRules.derive(group, troop)
            GildedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    AppText(group.name, style = MaterialTheme.typography.titleMedium, color = ExaltedAccentBright)
                    AppText(group.troopTypeName ?: "Custom Army", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted)
                    derived?.let { stats ->
                        Spacer(Modifier.height(4.dp))
                        AppText(
                            "Join Battle: ${stats.joinBattle}   Defense: ${stats.defense}   Soak: ${stats.soak}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExaltedOnSurface
                        )
                        val magnitudeAtual = group.currentMagnitude ?: stats.magnitude
                        Column {
                            AppText("Magnitude: $magnitudeAtual", color = ExaltedOnSurface)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InkButton(label = "−", onClick = { viewModel.ajustarMagnitudeBattleGroup(group.id, -1, stats.magnitude) }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                            Spacer(Modifier.width(6.dp))
                            InkButton(label = "+", onClick = { viewModel.ajustarMagnitudeBattleGroup(group.id, +1, stats.magnitude) }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                            if (group.currentMagnitude != null) {
                                Spacer(Modifier.width(6.dp))
                                InkButton(label = "Resetar", onClick = { viewModel.resetarMagnitudeBattleGroup(group.id) }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                            }
                            }
                        }
                        stats.attacks.forEach { attack ->
                            AppText(
                                "${attack.name}: Attack ${attack.attack} / Raw Damage ${attack.rawDamage}" + (attack.minimumDice?.let { " / Minimum Dice $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ExaltedOnSurface
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InkButton(label = "Editar", onClick = { edit(group) }, size = InkButtonSize.Small)
                            InkButton(label = "Juntar-se à Batalha", onClick = { viewModel.iniciativasController.adicionarOuAtualizarBattleGroup(group.name, stats.joinBattle, group.id) }, size = InkButtonSize.Small)
                            InkButton(label = "Excluir", onClick = {
                                if (editingId == group.id) clearEditor()
                                viewModel.removerBattleGroup(group.id)
                            }, size = InkButtonSize.Small, variant = InkButtonVariant.Danger)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun IntField(label: String, value: String, colors: androidx.compose.material3.TextFieldColors, onChange: (String) -> Unit) {
    OutlinedTextField(value, { input -> onChange(input.filter { it.isDigit() || it == '-' }) }, label = { AppText(label) }, modifier = Modifier.fillMaxWidth(), colors = colors)
}


private fun drillLabel(drill: BattleGroupDrill): String = when (drill) {
    BattleGroupDrill.POOR -> "Pobre"
    BattleGroupDrill.AVERAGE -> "Mediano"
    BattleGroupDrill.ELITE -> "Elite"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorRow(label: String, value: Int, range: IntRange, displayValue: String = value.toString(), onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        AppText("$label: $displayValue", style = MaterialTheme.typography.bodyMedium, color = ExaltedOnSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InkButton(label = "−", enabled = value > range.first, onClick = { onChange(value - 1) }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
            InkButton(label = "+", enabled = value < range.last, onClick = { onChange(value + 1) }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
        }
    }
}
