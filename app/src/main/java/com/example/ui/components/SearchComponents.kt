package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButtonVariant
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.search.KeywordMode
import com.example.search.NumericMatchMode
import com.example.search.SkillFilter
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import kotlinx.coroutines.delay

// Configuração de quais grupos de filtro exibir — cada categoria (Encantos,
// Feitiços, Necromancia, Artes Marciais) informa apenas os campos que
// realmente possui, sem exigir mudanças no componente para novas categorias.
data class SkillSearchConfig(
    val skillGroupLabel: String,          // ex.: "Habilidade" ou "Círculo"
    val availableSkillIds: List<String>,  // 25 habilidades, ou os Círculos disponíveis
    val showMinsEssencia: Boolean,
    val availableTypes: List<String>,
    val availableKeywords: List<String>,
    val availableDurations: List<String>
)

// Botão "Busca" com indicador da quantidade de filtros ativos.
@Composable
fun SearchButton(activeCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    InkButton(
        label = if (activeCount > 0) "Busca ($activeCount)" else "Busca",
        onClick = onClick,
        modifier = modifier,
        size = InkButtonSize.Small,
        variant = if (activeCount > 0) InkButtonVariant.Primary else InkButtonVariant.Secondary,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ActiveFilterChips(
    filter: SkillFilter,
    onRemove: (SkillFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filter.isEmpty) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (filter.query.isNotBlank()) {
            FilterChip("Nome: \"${filter.query}\"") { onRemove(filter.copy(query = "")) }
        }
        filter.minHabilidade?.let { v ->
            val prefixo = if (filter.minHabilidadeMode == NumericMatchMode.EXACT) "Mins = " else "Mins ≥ "
            FilterChip("$prefixo$v") { onRemove(filter.copy(minHabilidade = null)) }
        }
        filter.essencia?.let { v ->
            val prefixo = if (filter.essenciaMode == NumericMatchMode.EXACT) "Essência = " else "Essência ≥ "
            FilterChip("$prefixo$v") { onRemove(filter.copy(essencia = null)) }
        }
        filter.types.forEach { t ->
            FilterChip(t) { onRemove(filter.copy(types = filter.types - t)) }
        }
        filter.keywords.forEach { k ->
            FilterChip(k) { onRemove(filter.copy(keywords = filter.keywords - k)) }
        }
        filter.durations.forEach { d ->
            FilterChip(d) { onRemove(filter.copy(durations = filter.durations - d)) }
        }
    }
}

@Composable
private fun FilterChip(text: String, onRemove: () -> Unit) {
    Surface(
        color = ExaltedDarkSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, ExaltedAmber.copy(alpha = 0.6f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
            AppText(text, color = ExaltedGold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            InkButton(onClick = onRemove, modifier = Modifier.width(24.dp).height(24.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Remover filtro", tint = ExaltedMuted, modifier = Modifier.width(14.dp))
            }
        }
    }
}

// Modal de busca/filtragem — genérico para qualquer categoria, controlado
// por SkillSearchConfig. Mantém um rascunho local até "Aplicar" ser tocado,
// preservando o filtro já aplicado se o usuário cancelar.
@Composable
fun SkillSearchModal(
    config: SkillSearchConfig,
    currentFilter: SkillFilter,
    onApply: (SkillFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(currentFilter) }
    var skillQuery by remember { mutableStateOf("") }
    // Cada seção começa recolhida — só mostra as opções depois que o
    // usuário toca no respectivo cabeçalho.
    var skillsExpanded by remember { mutableStateOf(false) }
    var typesExpanded by remember { mutableStateOf(false) }
    var keywordsExpanded by remember { mutableStateOf(false) }
    var durationsExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText("Busca e Filtros", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis, forceStroke = true) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nome (com debounce próprio já embutido pelo campo de texto)
                OutlinedTextField(
                    value = draft.query,
                    onValueChange = com.example.ui.components.rememberTypingFeedback { draft = draft.copy(query = it) },
                    label = { AppText("Nome (PT ou EN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ExaltedAmber, focusedLabelColor = ExaltedAmber)
                )

                // Seleção por habilidade/círculo — recolhida por padrão
                if (config.availableSkillIds.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().feedbackClickable { skillsExpanded = !skillsExpanded }
                    ) {
                        AppText(if (skillsExpanded) "▾" else "▸", color = ExaltedGold, modifier = Modifier.padding(end = 6.dp))
                        AppText(
                            config.skillGroupLabel + if (draft.selectedSkillIds.isNotEmpty()) " (${draft.selectedSkillIds.size})" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = ExaltedGold
                        )
                    }
                    if (skillsExpanded) {
                        OutlinedTextField(
                            value = skillQuery,
                            onValueChange = com.example.ui.components.rememberTypingFeedback { skillQuery = it },
                            label = { AppText("Buscar em ${config.skillGroupLabel.lowercase()}s") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ExaltedAmber, focusedLabelColor = ExaltedAmber)
                        )
                        val skillsFiltrados = remember(skillQuery, config.availableSkillIds) {
                            if (skillQuery.isBlank()) config.availableSkillIds
                            else config.availableSkillIds.filter { it.contains(skillQuery, ignoreCase = true) }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).verticalScroll(rememberScrollState())
                        ) {
                            skillsFiltrados.forEach { skillId ->
                                val checked = skillId in draft.selectedSkillIds
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().feedbackClickable {
                                        draft = draft.copy(selectedSkillIds = if (checked) draft.selectedSkillIds - skillId else draft.selectedSkillIds + skillId)
                                    }
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = ExaltedAmber, uncheckedColor = ExaltedGold))
                                    AppText(skillId, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Mins e Essência
                if (config.showMinsEssencia) {
                    NumericFilterRow(
                        label = "Mins",
                        value = draft.minHabilidade,
                        mode = draft.minHabilidadeMode,
                        onValueChange = { draft = draft.copy(minHabilidade = it) },
                        onModeChange = { draft = draft.copy(minHabilidadeMode = it) }
                    )
                    NumericFilterRow(
                        label = "Essência",
                        value = draft.essencia,
                        mode = draft.essenciaMode,
                        onValueChange = { draft = draft.copy(essencia = it) },
                        onModeChange = { draft = draft.copy(essenciaMode = it) }
                    )
                }

                // Tipo — recolhido por padrão
                if (config.availableTypes.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().feedbackClickable { typesExpanded = !typesExpanded }
                    ) {
                        AppText(if (typesExpanded) "▾" else "▸", color = ExaltedGold, modifier = Modifier.padding(end = 6.dp))
                        AppText(
                            "Tipo" + if (draft.types.isNotEmpty()) " (${draft.types.size})" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = ExaltedGold
                        )
                    }
                    if (typesExpanded) {
                        MultiSelectChips(
                            options = config.availableTypes,
                            selected = draft.types,
                            onToggle = { opt -> draft = draft.copy(types = if (opt in draft.types) draft.types - opt else draft.types + opt) }
                        )
                    }
                }

                // Palavras-chave — recolhida por padrão
                if (config.availableKeywords.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).feedbackClickable { keywordsExpanded = !keywordsExpanded }
                        ) {
                            AppText(if (keywordsExpanded) "▾" else "▸", color = ExaltedGold, modifier = Modifier.padding(end = 6.dp))
                            AppText(
                                "Palavras-chave" + if (draft.keywords.isNotEmpty()) " (${draft.keywords.size})" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = ExaltedGold
                            )
                        }
                        if (keywordsExpanded) {
                            AppText(
                                if (draft.keywordMode == KeywordMode.ANY) "Qualquer uma" else "Todas",
                                style = MaterialTheme.typography.labelSmall,
                                color = ExaltedMuted,
                                modifier = Modifier.feedbackClickable {
                                    draft = draft.copy(keywordMode = if (draft.keywordMode == KeywordMode.ANY) KeywordMode.ALL else KeywordMode.ANY)
                                }.padding(4.dp)
                            )
                        }
                    }
                    if (keywordsExpanded) {
                        MultiSelectChips(
                            options = config.availableKeywords,
                            selected = draft.keywords,
                            onToggle = { opt -> draft = draft.copy(keywords = if (opt in draft.keywords) draft.keywords - opt else draft.keywords + opt) }
                        )
                    }
                }

                // Duração — recolhida por padrão
                if (config.availableDurations.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().feedbackClickable { durationsExpanded = !durationsExpanded }
                    ) {
                        AppText(if (durationsExpanded) "▾" else "▸", color = ExaltedGold, modifier = Modifier.padding(end = 6.dp))
                        AppText(
                            "Duração" + if (draft.durations.isNotEmpty()) " (${draft.durations.size})" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = ExaltedGold
                        )
                    }
                    if (durationsExpanded) {
                        MultiSelectChips(
                            options = config.availableDurations,
                            selected = draft.durations,
                            onToggle = { opt -> draft = draft.copy(durations = if (opt in draft.durations) draft.durations - opt else draft.durations + opt) }
                        )
                    }
                }
            }
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(text = "Aplicar", onClick = { onApply(draft) })
        },
        confirmButton = {
            Row {
                InkButton(label = "Limpar filtros", onClick = { draft = SkillFilter() }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                Spacer(modifier = Modifier.width(4.dp))
                com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = onDismiss)
            }
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

@Composable
private fun NumericFilterRow(
    label: String,
    value: Int?,
    mode: NumericMatchMode,
    onValueChange: (Int?) -> Unit,
    onModeChange: (NumericMatchMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AppText(label, style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { n ->
                val selected = value == n
                Surface(
                    modifier = Modifier.feedbackClickable { onValueChange(if (selected) null else n) },
                    color = if (selected) ExaltedAmber.copy(alpha = 0.3f) else ExaltedDarkSurface,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, if (selected) ExaltedAccentBright else ExaltedGold.copy(alpha = 0.4f))
                ) {
                    AppText("$n", color = if (selected) ExaltedAccentBright else ExaltedMuted, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            if (value != null) {
                Spacer(modifier = Modifier.width(4.dp))
                AppText(
                    if (mode == NumericMatchMode.EXACT) "Exato" else "A partir de",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExaltedMuted,
                    modifier = Modifier.feedbackClickable {
                        onModeChange(if (mode == NumericMatchMode.EXACT) NumericMatchMode.AT_LEAST else NumericMatchMode.EXACT)
                    }.padding(4.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MultiSelectChips(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { opt ->
            val isSelected = opt in selected
            Surface(
                modifier = Modifier.feedbackClickable { onToggle(opt) },
                color = if (isSelected) ExaltedAmber.copy(alpha = 0.28f) else ExaltedDarkSurface,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, if (isSelected) ExaltedAccentBright else ExaltedGold.copy(alpha = 0.4f))
            ) {
                AppText(
                    opt,
                    color = if (isSelected) ExaltedAccentBright else ExaltedMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// Campo de nome com debounce próprio (250–300ms), para uso fora do modal —
// ex.: uma busca rápida embutida diretamente na lista de resultados.
@Composable
fun DebouncedSearchField(
    value: String,
    onDebouncedChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Buscar por nome"
) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(text) {
        delay(280)
        if (text != value) onDebouncedChange(text)
    }
    OutlinedTextField(
        value = text,
        onValueChange = com.example.ui.components.rememberTypingFeedback { text = it },
        label = { AppText(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ExaltedAmber, focusedLabelColor = ExaltedAmber)
    )
}
