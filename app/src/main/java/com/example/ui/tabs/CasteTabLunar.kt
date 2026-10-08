package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.InkButton
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterSheet
import com.example.model.ExaltedConstants
import com.example.ui.components.ErgonomicMultiWordButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.GildedCard
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkBackground
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedBlack
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedOutline
import com.example.viewmodel.SheetViewModel

// Extraído de CasteTab.kt (refatoração de organização — pedido explícito
// do usuário, sem mudança de comportamento). Conteúdo específico da Casta
// Lunar, antes misturado com Solar/Sangue de Dragão no mesmo arquivo.
@OptIn(ExperimentalLayoutApi::class)
// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
internal fun LunarCasteTabContent(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    if (!sheet.lunarCastaEscolhida) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            SectionHeader(title = "Casta selecionada")
            AppText(
                text = "Casta não selecionada",
                style = MaterialTheme.typography.bodyMedium,
                color = ExaltedMuted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var expandedVirtudeLunar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val falhasDeVirtudeLunar = com.example.data.EncounterLimitCatalog.LUNAR


    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(2).exaltedContentStage(2)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Casta selecionada")

        // Imagem padronizada — pedido explícito do usuário: mesmo
        // padrão do Solar/Sangue de Dragão (emblema fixo, não
        // clicável, com o nome logo abaixo).
        com.example.ui.components.LunarCasteEmblemButton(
            caste = sheet.lunarCasta,
            isSelected = true,
            onClick = {},
            enabled = false,
            iconSize = com.example.ui.theme.Dimens.CasteAspectIconSize * 2
        )
        AppText(
            text = sheet.lunarCasta.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ExaltedAccentBright,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // --- Poder da Anima ---
        // Mesmo comportamento visual de Solar e Sangue de Dragão:
        // o conteúdo começa recolhido e é aberto por toque.
        Spacer(modifier = Modifier.height(10.dp))
        com.example.ui.components.ExpandableTextCard(
            titulo = "Poder da Anima",
            texto = sheet.lunarCasta.poderesDeCasta().joinToString("\n\n"),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Forma Espiritual e Sinal — pedido explícito do usuário: dois
        // campos de texto livre, lado a lado, exclusivos de Lunar.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = sheet.lunarFormaEspiritual,
                onValueChange = { viewModel.updateLunarFormaEspiritual(it) },
                label = { AppText("Forma Espiritual") },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
            )
            androidx.compose.material3.OutlinedTextField(
                value = sheet.lunarSinal,
                onValueChange = { viewModel.updateLunarSinal(it) },
                label = { AppText("Sinal") },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Atributos de Casta: escolha exatamente 2 das 3 opções
        // fornecidas pela Casta. Apenas os selecionados entram no conjunto
        // Casta/Favorecido da planilha.
        val pool = androidx.compose.runtime.remember(sheet.lunarCasta) { sheet.lunarCasta.poolAtributosCasta() }
        val lunarCasteSelected = androidx.compose.runtime.remember(sheet.lunarCasteAttributesEscolhidos) { sheet.lunarCasteAttributesEscolhidos.toSet() }
        SectionHeader(title = "ATRIBUTOS DE CASTA — ESCOLHA 2 (${sheet.lunarCasteAttributesEscolhidos.size}/2)")
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2),
            colors = CardDefaults.cardColors(containerColor = ExaltedBlack)
        ) {
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pool.forEach { atributo ->
                    val isSelected = lunarCasteSelected.contains(atributo)
                    ErgonomicMultiWordButton(
                        text = atributo,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleLunarCasteAttribute(atributo) },
                        modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight),
                        matchCharmPaletteWhenUnselected = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Atributos Favorecidos: escolha exatamente 2 adicionais.
        // Atributos já selecionados como Casta ficam fora desta lista.
        val atributosDisponiveisParaFavorecido = androidx.compose.runtime.remember(sheet.lunarCasteAttributesEscolhidos) {
            ExaltedConstants.ALL_ATTRIBUTES.filter { it !in lunarCasteSelected }
        }
        val favoredSelected = androidx.compose.runtime.remember(sheet.favoredAttributes) { sheet.favoredAttributes.toSet() }
        SectionHeader(title = "ATRIBUTOS FAVORECIDOS (${sheet.favoredAttributes.size}/2)")
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2),
            colors = CardDefaults.cardColors(containerColor = ExaltedBlack)
        ) {
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                atributosDisponiveisParaFavorecido.forEach { atributo ->
                    val isSelected = favoredSelected.contains(atributo)
                    ErgonomicMultiWordButton(
                        text = atributo,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleFavoredAttribute(atributo) },
                        modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight),
                        matchCharmPaletteWhenUnselected = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Limite (Limit Break) — idêntico ao do Solar, pedido
        // explícito do usuário, com o contador prateado em vez de
        // dourado. ---
        SectionHeader(title = "Limite (quebra de limite)")
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.OutlinedTextField(
                        value = sheet.falhaVirtude,
                        onValueChange = {},
                        readOnly = true,
                        label = { AppText("Falha de Virtude") },
                        placeholder = { AppText("Selecione uma Falha de Virtude") },
                        trailingIcon = {
                            InkButton(onClick = { expandedVirtudeLunar = !expandedVirtudeLunar }) {
                                androidx.compose.material3.Icon(
                                    imageVector = if (expandedVirtudeLunar) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar Falha de Virtude",
                                    tint = ExaltedAmber
                                )
                            }
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .feedbackClickable { expandedVirtudeLunar = true }
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = expandedVirtudeLunar,
                        onDismissRequest = { expandedVirtudeLunar = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(ExaltedDarkSurfaceVariant)
                    ) {
                        falhasDeVirtudeLunar.forEach { falha ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    AppText(
                                        text = falha,
                                        fontWeight = if (sheet.falhaVirtude == falha) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sheet.falhaVirtude == falha) ExaltedAmber else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.updateFalhaVirtude(falha)
                                    expandedVirtudeLunar = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = sheet.limiteGatilho,
                    onValueChange = { viewModel.updateLimiteGatilho(it) },
                    label = { AppText("Gatilho do Limite") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                Spacer(modifier = Modifier.height(5.dp))

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppText(
                        text = "Limite",
                        color = ExaltedGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..10).forEach { level ->
                            androidx.compose.material3.Surface(
                                modifier = Modifier.width(28.dp).height(28.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                // Prateado em vez de dourado — pedido
                                // explícito do usuário.
                                color = if (level <= sheet.limiteContador) limiteGradientColorLunar(level) else ExaltedDarkBackground.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (level <= sheet.limiteContador) ExaltedAccentBright else ExaltedMuted.copy(alpha = 0.45f)
                                ),
                                onClick = { viewModel.updateLimiteContador(level) }
                            ) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    AppText(
                                        text = level.toString(),
                                        fontFamily = com.example.ui.theme.ExaltedSymbolFont,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (level <= sheet.limiteContador) limiteTextColorLunar(level) else ExaltedAccentBright,
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun limiteGradientColorLunar(level: Int): androidx.compose.ui.graphics.Color {
    val claro = com.example.ui.theme.ExaltedStructuralMetalShine
    val escuro = com.example.ui.theme.ExaltedStructuralMetalDeep
    val linear = ((level - 1) / 9f).coerceIn(0f, 1f)
    // Mesma curva de contraste do Solar: início luminoso e queda visual
    // mais marcada nos níveis altos, preservando a paleta prateada Lunar.
    val t = linear * linear * (3f - 2f * linear)
    return androidx.compose.ui.graphics.lerp(claro, escuro, t)
}

private fun limiteTextColorLunar(level: Int): androidx.compose.ui.graphics.Color {
    val t = (level - 1) / 9f
    return if (t <= 0.5f) com.example.ui.theme.ExaltedStructuralMetalDeep else com.example.ui.theme.ExaltedStructuralMetalShine
}
