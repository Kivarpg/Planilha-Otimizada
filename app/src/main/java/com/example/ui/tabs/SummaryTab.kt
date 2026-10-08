package com.example.ui.tabs
import com.example.ui.components.exaltedContentStage

import com.example.ui.components.exaltedTabIdentity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.CharacterSheet
import com.example.ui.components.SectionHeader
import com.example.viewmodel.SheetViewModel

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
fun SummaryTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    val bpInfo = remember(sheet) { viewModel.calculateBpBreakdown(sheet) }
    Column(
        modifier = modifier.fillMaxSize().exaltedTabIdentity(9).exaltedContentStage(9).padding(horizontal = 14.dp, vertical = 12.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExperienceCounterSection(sheet, viewModel)
        Spacer(modifier = Modifier.height(5.dp))
        SummaryPersonalDataSection(sheet)
        Spacer(modifier = Modifier.height(5.dp))
        SummaryAttributesSection(sheet)
        Spacer(modifier = Modifier.height(5.dp))
        SummaryAbilitiesSection(sheet)
        Spacer(modifier = Modifier.height(5.dp))
        SummaryCombatSection(sheet, viewModel)
        Spacer(modifier = Modifier.height(5.dp))
        SummaryVitalitySection(sheet, viewModel)
        Spacer(modifier = Modifier.height(5.dp))
        SummaryCharmsSection(sheet, viewModel)
        Spacer(modifier = Modifier.height(5.dp))
        SummaryExperienceSection(sheet, bpInfo)
    }
}
