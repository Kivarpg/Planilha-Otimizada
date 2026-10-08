package com.example.ui.tabs

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.CharacterSheet
import com.example.model.isLunar
import com.example.viewmodel.SheetViewModel

// Despachante fino — pedido explícito do usuário (refatoração de
// organização, sem mudança de comportamento). O conteúdo real foi
// dividido em CasteTabLunar.kt e CasteTabSolar.kt, já que os dois
// blocos nunca se misturavam de verdade (um único "if isLunar" no
// topo decidia tudo). Sangue de Dragão nem passa por aqui — usa
// AspectoTab.kt inteiramente à parte, roteado em SheetTabs.kt.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CasteTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    if (sheet.tipoPersonagem.isLunar()) {
        LunarCasteTabContent(sheet, viewModel, modifier)
    } else {
        SolarCasteTabContent(sheet, viewModel, modifier)
    }
}
