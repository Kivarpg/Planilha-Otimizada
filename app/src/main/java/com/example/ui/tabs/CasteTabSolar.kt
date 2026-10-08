package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.InkButton
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.example.ui.theme.ExaltedLimiteClaro
import com.example.ui.theme.ExaltedLimiteEscuro
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedOutline
import com.example.viewmodel.SheetViewModel

// Extraído de CasteTab.kt (refatoração de organização — pedido explícito
// do usuário, sem mudança de comportamento). Conteúdo específico de
// Solar — confirmado que Sangue de Dragão usa AspectoTab.kt inteiramente
// à parte (roteamento em SheetTabs.kt), então este bloco nunca foi
// realmente compartilhado entre os dois, ao contrário do que um
// comentário antigo sugeria.
@OptIn(ExperimentalLayoutApi::class)
// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
internal fun SolarCasteTabContent(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    // Assim como o Aspecto na planilha de Sangue de Dragão, uma planilha Solar
    // nova não possui Casta selecionada. O valor Dawn no modelo é apenas um
    // valor técnico de fallback; a escolha real é indicada por castaEscolhida.
    if (!sheet.castaEscolhida) {
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

    val allowedCasteAbilities = sheet.casta.allowedAbilities()
    // Habilidades disponíveis para Favorecidas: todas as habilidades do sistema
    // que ainda não foram marcadas como Habilidade de Casta. Isso inclui as
    // habilidades permitidas para a Casta que não foram selecionadas entre as 5.
    // A seleção da tela é recalculada durante recomposições; materializar o Set
    // evita uma busca linear para cada uma das 25 habilidades do sistema.
    val remainingSystemAbilities = androidx.compose.runtime.remember(sheet.casteAbilities) {
        val casteAbilitiesSet = sheet.casteAbilities.toHashSet()
        ExaltedConstants.ALL_25_ABILITIES.filter { it !in casteAbilitiesSet }
    }

    // Estado do bloco de Limite (movido da Aba 1 para cá — ver seção final).
    var expandedVirtude by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val falhasDeVirtude = com.example.data.EncounterLimitCatalog.SOLAR


    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(2).exaltedContentStage(2)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Casta selecionada")

        // A troca de Casta agora ocorre somente na Aba 1 (Dados Pessoais).
        // Aqui exibimos apenas a Casta já escolhida, sem permitir seleção.
        com.example.ui.components.CasteEmblemButton(
            caste = sheet.casta,
            isSelected = true,
            onClick = {},
            enabled = false,
            iconSize = com.example.ui.theme.Dimens.CasteAspectIconSize * 2
        )
        AppText(
            text = sheet.casta.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = com.example.ui.theme.ExaltedAccentBright,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        com.example.ui.components.ExpandableTextCard(
            titulo = "Poder da Anima",
            texto = com.example.data.AnimaDescriptions.paraCasta(sheet.casta),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // --- Caste Abilities Selection (5 out of 8) ---
        SectionHeader(title = "HABILIDADES DE CASTA (${sheet.casteAbilities.size}/5)")

        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2),
            colors = CardDefaults.cardColors(containerColor = ExaltedBlack)
        ) {
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allowedCasteAbilities.forEach { ab ->
                    val isSelected = sheet.casteAbilities.contains(ab)
                    ErgonomicMultiWordButton(
                        text = ab,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleCasteAbility(ab) },
                        modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight),
                        matchCharmPaletteWhenUnselected = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Supernal Ability Selection (1 of the 5 chosen caste abilities) ---
        SectionHeader(title = "HABILIDADE SUPERNAL (1/1)")

        if (sheet.casteAbilities.isNotEmpty()) {
            GildedCard(
                modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2),
                colors = CardDefaults.cardColors(containerColor = ExaltedBlack)
            ) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sheet.casteAbilities.forEach { ab ->
                        val isSupernal = sheet.supernalAbility == ab
                        ErgonomicMultiWordButton(
                            text = if (isSupernal) "$ab\n[SUPERNAL]" else ab,
                            isSelected = isSupernal,
                            onClick = {
                                if (isSupernal) viewModel.setSupernalAbility(null)
                                else viewModel.setSupernalAbility(ab)
                            },
                            modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight),
                        matchCharmPaletteWhenUnselected = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Favored Abilities Selection (5 of 20 remaining) ---
        SectionHeader(title = "HABILIDADES FAVORECIDAS (${sheet.favoredAbilities.size}/5)")

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
                remainingSystemAbilities.forEach { ab ->
                    val isSelected = sheet.favoredAbilities.contains(ab)
                    ErgonomicMultiWordButton(
                        text = ab,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleFavoredAbility(ab) },
                        modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight),
                        matchCharmPaletteWhenUnselected = true
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))

        // --- Limite (Limit Break) — movido integralmente da Aba 1 (Dados
        // Pessoais) para cá, preservando todos os campos, regras e dados já
        // salvos. Só a posição na tela mudou. ---
        SectionHeader(title = "Limite (quebra de limite)")
        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Falha de Virtude (Dropdown Selector)
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.OutlinedTextField(
                        value = sheet.falhaVirtude,
                        onValueChange = {},
                        readOnly = true,
                        label = { AppText("Falha de Virtude") },
                        placeholder = { AppText("Selecione uma Falha de Virtude") },
                        trailingIcon = {
                            InkButton(onClick = { expandedVirtude = !expandedVirtude }) {
                                androidx.compose.material3.Icon(
                                    imageVector = if (expandedVirtude) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar Falha de Virtude",
                                    tint = com.example.ui.theme.ExaltedAmber
                                )
                            }
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.ui.theme.ExaltedAccentBright,
                            unfocusedBorderColor = com.example.ui.theme.ExaltedOutline.copy(alpha = 0.55f),
                            focusedLabelColor = com.example.ui.theme.ExaltedAccentBright,
                            unfocusedLabelColor = ExaltedMuted,
                            cursorColor = com.example.ui.theme.ExaltedGold,
                            focusedTextColor = com.example.ui.theme.ExaltedOnSurface,
                            unfocusedTextColor = com.example.ui.theme.ExaltedOnSurface,
                            focusedContainerColor = com.example.ui.theme.ExaltedDarkSurface,
                            unfocusedContainerColor = com.example.ui.theme.ExaltedDarkSurface,
                            focusedPlaceholderColor = ExaltedMuted,
                            unfocusedPlaceholderColor = ExaltedMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .feedbackClickable { expandedVirtude = true }
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = expandedVirtude,
                        onDismissRequest = { expandedVirtude = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(ExaltedDarkSurfaceVariant)
                    ) {
                        falhasDeVirtude.forEach { falha ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    AppText(
                                        text = falha,
                                        fontWeight = if (sheet.falhaVirtude == falha) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                        color = if (sheet.falhaVirtude == falha) com.example.ui.theme.ExaltedAmber else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.updateFalhaVirtude(falha)
                                    expandedVirtude = false
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
                        focusedBorderColor = com.example.ui.theme.ExaltedAccentBright,
                        unfocusedBorderColor = com.example.ui.theme.ExaltedOutline.copy(alpha = 0.55f),
                        focusedLabelColor = com.example.ui.theme.ExaltedAccentBright,
                        unfocusedLabelColor = ExaltedMuted,
                        cursorColor = com.example.ui.theme.ExaltedGold,
                        focusedTextColor = com.example.ui.theme.ExaltedOnSurface,
                        unfocusedTextColor = com.example.ui.theme.ExaltedOnSurface,
                        focusedContainerColor = com.example.ui.theme.ExaltedDarkSurface,
                        unfocusedContainerColor = com.example.ui.theme.ExaltedDarkSurface,
                        focusedPlaceholderColor = ExaltedMuted,
                        unfocusedPlaceholderColor = ExaltedMuted
                    )
                )
                Spacer(modifier = Modifier.height(5.dp))

                // Limite: trilha visual de 10 níveis.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppText(
                        text = "Limite",
                        color = com.example.ui.theme.ExaltedGold,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(28.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                color = if (level <= sheet.limiteContador) {
                                    limiteGradientColor(level)
                                } else {
                                    com.example.ui.theme.ExaltedDarkBackground.copy(alpha = 0.85f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (level <= sheet.limiteContador) com.example.ui.theme.ExaltedGold else com.example.ui.theme.ExaltedAmber.copy(alpha = 0.45f)
                                ),
                                onClick = { viewModel.updateLimiteContador(level) }
                            ) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    AppText(
                                        text = level.toString(),
                                        fontFamily = com.example.ui.theme.ExaltedSymbolFont,
                                        fontSize = 9.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = if (level <= sheet.limiteContador) limiteTextColor(level) else com.example.ui.theme.ExaltedAccentBright,
                                        maxLines = 1,
                                        softWrap = false
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

private fun limiteGradientColor(level: Int): androidx.compose.ui.graphics.Color {
    val claro = ExaltedLimiteClaro
    val escuro = ExaltedLimiteEscuro
    val linear = ((level - 1) / 9f).coerceIn(0f, 1f)
    // Curva de contraste: mantém o início luminoso, mas acelera o
    // escurecimento na metade final para o nível 10 ficar inequivocamente grave.
    val t = linear * linear * (3f - 2f * linear)
    return androidx.compose.ui.graphics.lerp(claro, escuro, t)
}

// Cor do número sobre a caixa preenchida: como o degradê vai de claro a
// escuro, o texto precisa trocar de preto (metade clara) para um tom claro
// (metade escura) para se manter legível em qualquer nível.
private fun limiteTextColor(level: Int): androidx.compose.ui.graphics.Color {
    val t = (level - 1) / 9f
    return if (t <= 0.5f) com.example.ui.theme.ExaltedMetalGoldDeep else com.example.ui.theme.ExaltedMetalGoldFlash
}
