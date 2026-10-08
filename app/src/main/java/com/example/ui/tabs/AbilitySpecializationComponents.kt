package com.example.ui.tabs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
internal fun AddMartialArtButton(onClick: () -> Unit) {
    com.example.ui.components.InkButton(
        label = "Adicionar Arte Marcial",
        selected = true,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        fillMaxWidth = true
    )
}

@Composable
internal fun AbilityCompactRowForName(
    abName: String,
    index: Int,
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    onSpecializationDelete: (Especializacao) -> Unit,
    ratingStyle: com.example.model.RatingStyle,
    martialArt: HabilidadeCustomizada? = null,
    onMartialArtDelete: ((HabilidadeCustomizada) -> Unit)? = null,
    specializationsByAbility: Map<String, List<Especializacao>>
) {
    val rating = martialArt?.valor ?: (sheet.abilities[abName] ?: 0)
    val isCaste = martialArt == null && sheet.casteAbilities.contains(abName)
    val isFavored = martialArt == null && sheet.favoredAbilities.contains(abName)
    val isSuperna = martialArt == null && sheet.supernalAbility == abName
    val badgeText = when {
        martialArt != null -> "Arte Marcial"
        isSuperna -> "Supernal"
        isCaste -> if (sheet.tipoPersonagem.isDragonBlooded()) "Aspecto" else "Casta"
        isFavored -> "Favorecida"
        else -> null
    }
    val badgeColor = when {
        martialArt != null -> ExaltedAccentBright
        isSuperna -> ExaltedAccentBright
        isCaste -> ExaltedAmber
        isFavored -> ExaltedFavorecidaLaranja
        else -> ExaltedMuted
    }
    val martialArtIcon = if (martialArt != null) com.example.ui.components.AbilityMartialArt else null
    val abilityIconRes = if (martialArt == null) abilityRealIconRes(index) else null
    val onRatingChange: (Int) -> Unit = if (martialArt != null) {
        { viewModel.updateMartialArtValue(martialArt.id, it) }
    } else {
        { viewModel.setAbilityRating(abName, it) }
    }

    // O chamador já fornece o índice memoizado por lista de especializações.
    // Evita qualquer fallback de varredura O(E) dentro de cada linha.
    val abilitySpecs = specializationsByAbility[abName].orEmpty()
    var showSpecDialog by remember { mutableStateOf(false) }
    // Rascunho usado tanto para adicionar uma nova especialidade (habilidades
    // padrão) quanto para editar a especialidade única da Arte Marcial.
    var specDraft by remember(showSpecDialog) { mutableStateOf(if (martialArt != null) abilitySpecs.firstOrNull()?.nome ?: "" else "") }

    val specializationSummary = if (abilitySpecs.isEmpty()) null else abilitySpecs.joinToString(", ") { it.nome }
    // Único caso com 3 "nomes" na mesma especialização (pedido do
    // usuário): "Arte Marcial" (rótulo fixo) + nome dado pela usuária à
    // arte + a especialização em si, com seu próprio rótulo. Só se aplica
    // quando é de fato uma Arte Marcial com pelo menos 1 especialização.
    val specializationSegments = if (martialArt != null && abilitySpecs.isNotEmpty()) {
        listOf("Arte Marcial", abName, "Especialização: $specializationSummary")
    } else null

    AbilityCompactRow(
        name = abName,
        rating = rating,
        badgeText = badgeText,
        badgeColor = badgeColor,
        martialArtIcon = martialArtIcon,
        abilityIconRes = abilityIconRes,
        onValueChange = onRatingChange,
        ratingStyle = ratingStyle,
        specializationText = specializationSummary,
        specializationSegments = specializationSegments,
        onLongPress = {
            specDraft = if (martialArt != null) abilitySpecs.firstOrNull()?.nome ?: "" else ""
            showSpecDialog = true
        }
    )

    if (showSpecDialog) {
        if (martialArt != null) {
            AlertDialog(
                onDismissRequest = { showSpecDialog = false },
                modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
                shape = com.example.ui.components.dialogShape,
                title = {
                    AppText(
                        "Arte Marcial — $abName",
                        color = ExaltedAccentBright,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (abilitySpecs.isNotEmpty()) {
                            abilitySpecs.forEach { spec ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AppText(
                                        text = spec.nome,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    com.example.ui.components.GildedDialogTextButton(text = "Remover", onClick = { onSpecializationDelete(spec) }, isDanger = true)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = specDraft,
                            onValueChange = com.example.ui.components.rememberTypingFeedback { specDraft = it },
                            label = { AppText("Nova especialidade") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ExaltedAccentBright,
                            unfocusedBorderColor = ExaltedOutline.copy(alpha = 0.55f),
                            focusedLabelColor = ExaltedAccentBright,
                            unfocusedLabelColor = ExaltedMuted,
                            cursorColor = ExaltedGold,
                            focusedTextColor = ExaltedOnSurface,
                            unfocusedTextColor = ExaltedOnSurface,
                            focusedContainerColor = ExaltedDarkSurface,
                            unfocusedContainerColor = ExaltedDarkSurface,
                            focusedPlaceholderColor = ExaltedMuted,
                            unfocusedPlaceholderColor = ExaltedMuted
                        )
                        )
                    }
                },
                dismissButton = {
                    com.example.ui.components.GildedDialogButton(
                        text = "Adicionar",
                        onClick = {
                            val texto = specDraft.trim()
                            if (texto.isNotEmpty()) {
                                viewModel.addSpecialization(texto, abName)
                                specDraft = ""
                            } else {
                                showSpecDialog = false
                            }
                        }
                    )
                },
                confirmButton = {
                    Row {
                        com.example.ui.components.GildedDialogTextButton(
                            text = "Remover Arte Marcial",
                            onClick = {
                                onMartialArtDelete?.invoke(martialArt)
                                showSpecDialog = false
                            },
                            isDanger = true
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = { showSpecDialog = false })
                    }
                },
                containerColor = ExaltedDarkSurface
            )
        } else {
            AlertDialog(
                onDismissRequest = { showSpecDialog = false },
                modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
                shape = com.example.ui.components.dialogShape,
                title = {
                    AppText(
                        "Especialidades — $abName",
                        color = ExaltedAccentBright,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (abilitySpecs.isNotEmpty()) {
                            abilitySpecs.forEach { spec ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AppText(
                                        text = spec.nome,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    com.example.ui.components.GildedDialogTextButton(text = "Remover", onClick = { onSpecializationDelete(spec) }, isDanger = true)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = specDraft,
                            onValueChange = com.example.ui.components.rememberTypingFeedback { specDraft = it },
                            label = { AppText("Nova especialidade") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ExaltedAccentBright,
                            unfocusedBorderColor = ExaltedOutline.copy(alpha = 0.55f),
                            focusedLabelColor = ExaltedAccentBright,
                            unfocusedLabelColor = ExaltedMuted,
                            cursorColor = ExaltedGold,
                            focusedTextColor = ExaltedOnSurface,
                            unfocusedTextColor = ExaltedOnSurface,
                            focusedContainerColor = ExaltedDarkSurface,
                            unfocusedContainerColor = ExaltedDarkSurface,
                            focusedPlaceholderColor = ExaltedMuted,
                            unfocusedPlaceholderColor = ExaltedMuted
                        )
                        )
                    }
                },
                dismissButton = {
                    com.example.ui.components.GildedDialogButton(
                        text = "Adicionar",
                        onClick = {
                            val texto = specDraft.trim()
                            if (texto.isNotEmpty()) {
                                viewModel.addSpecialization(texto, abName)
                                specDraft = ""
                            } else {
                                showSpecDialog = false
                            }
                        }
                    )
                },
                confirmButton = {
                    com.example.ui.components.GildedDialogTextButton(text = "Fechar", onClick = { showSpecDialog = false })
                },
                containerColor = ExaltedDarkSurface
            )
        }
    }
}
