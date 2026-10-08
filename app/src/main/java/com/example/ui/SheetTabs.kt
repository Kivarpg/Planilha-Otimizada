package com.example.ui

import com.example.model.isDragonBlooded
import com.example.model.isLunar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.model.ArquetipoEncontro
import com.example.model.CharacterSheet
import com.example.oldrealm.TranslatorTab
import com.example.ui.tabs.AbilitiesTab
import com.example.ui.tabs.BattleGroupsTab
import com.example.ui.tabs.AttributesTab
import com.example.ui.tabs.CasteTab
import com.example.ui.tabs.CharmsTab
import com.example.ui.tabs.CombatTab
import com.example.ui.tabs.EncounterGeneratorTab
import com.example.ui.tabs.EquipmentTab
import com.example.ui.tabs.MapTab
import com.example.ui.tabs.MeritsTab
import com.example.ui.tabs.NPCsTab
import com.example.ui.tabs.PersonalDataTab
import com.example.ui.tabs.SummaryTab
import com.example.viewmodel.SheetViewModel

/**
 * Única fonte declarativa das abas da planilha.
 *
 * A lista é recriada quando o estado muda, mas a relação título/ícone/conteúdo
 * permanece inseparável. Nenhum código consumidor deve manter listas paralelas
 * indexadas pelo número da aba.
 */
@Composable
fun createSheetTabs(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    abilitiesSubTabIndex: Int,
    onAbilitiesSubTabChange: (Int) -> Unit,
    encontroNomeManual: String,
    onEncontroNomeManualChange: (String) -> Unit,
    encontroArquetipo: ArquetipoEncontro,
    onEncontroArquetipoChange: (ArquetipoEncontro) -> Unit,
    encontroGenero: com.example.data.GeneroNome?,
    onEncontroGeneroChange: (com.example.data.GeneroNome?) -> Unit,
    encontroAbaSelecionadaId: String?,
    onEncontroAbaSelecionadaChange: (String?) -> Unit,
    encontroMensagemLimite: String?,
    onEncontroMensagemLimiteChange: (String?) -> Unit
): List<SheetTab> {
    return listOf(
    SheetTab("1. Dados Pessoais", com.example.R.drawable.tab_icon_dados_pessoais) { PersonalDataTab(sheet = sheet, viewModel = viewModel) },
    SheetTab(
        if (sheet.tipoPersonagem.isDragonBlooded()) "2. Aspecto" else "2. Casta",
        when {
            // Ícones especiais da Casta/Aspecto: Dragão para Sangue de Dragão,
            // Lua para Lunar e Sol para Solar.
            sheet.tipoPersonagem.isDragonBlooded() -> com.example.R.drawable.tab_icon_dragao
            sheet.tipoPersonagem.isLunar() -> com.example.R.drawable.tab_icon_lua
            else -> com.example.R.drawable.tab_icon_sol
        }
    ) {
        if (sheet.tipoPersonagem.isDragonBlooded()) {
            com.example.ui.tabs.AspectoTab(sheet = sheet, viewModel = viewModel)
        } else {
            CasteTab(sheet = sheet, viewModel = viewModel)
        }
    },
    SheetTab("3. Atributos", com.example.R.drawable.tab_icon_atributos) { AttributesTab(sheet = sheet, viewModel = viewModel) },
    SheetTab("4. Habilidades", com.example.R.drawable.tab_icon_habilidades) {
        AbilitiesTab(sheet = sheet, viewModel = viewModel, subTabIndex = abilitiesSubTabIndex, onSubTabChange = onAbilitiesSubTabChange)
    },
    SheetTab("5. Combate", com.example.R.drawable.tab_icon_combate) { CombatTab(sheet = sheet, viewModel = viewModel) },
    SheetTab("6. Méritos", com.example.R.drawable.tab_icon_meritos) { MeritsTab(sheet = sheet, viewModel = viewModel) },
    SheetTab("7. Equipamentos", com.example.R.drawable.tab_icon_equipamentos) { EquipmentTab(sheet = sheet, viewModel = viewModel) },
    SheetTab("8. Encantos", com.example.R.drawable.tab_icon_encantos) { CharmsTab(sheet = sheet, viewModel = viewModel) },
    SheetTab("9. Planilha", com.example.R.drawable.tab_icon_planilha) { SummaryTab(sheet = sheet, viewModel = viewModel) },
    SheetTab("10. Vínculos", com.example.R.drawable.tab_icon_npcs) { NPCsTab(viewModel = viewModel) },
    SheetTab("11. Encontros", com.example.R.drawable.tab_icon_encontros) {
        EncounterGeneratorTab(
            sheetAtual = sheet,
            viewModel = viewModel,
            nomeManual = encontroNomeManual,
            onNomeManualChange = onEncontroNomeManualChange,
            arquetipoSelecionado = encontroArquetipo,
            onArquetipoChange = onEncontroArquetipoChange,
            culturaSelecionada = null,
            generoSelecionado = encontroGenero,
            onGeneroChange = onEncontroGeneroChange,
            abaSelecionadaId = encontroAbaSelecionadaId,
            onAbaSelecionadaChange = onEncontroAbaSelecionadaChange,
            mensagemLimite = encontroMensagemLimite,
            onMensagemLimiteChange = onEncontroMensagemLimiteChange
        )
    },
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    SheetTab("12. Conflito", com.example.R.drawable.tab_icon_conflito) {
        val historicoCombates by viewModel.historicoCombates.collectAsState()
        com.example.iniciativas.IniciativasTab(
            controller = viewModel.iniciativasController,
            onClashResolved = { perdedorNpcId, proximosNpcIds ->
                perdedorNpcId?.let(viewModel::aplicarPenalidadeClashNpc)
                if (proximosNpcIds.isNotEmpty()) viewModel.limparPenalidadesDefesaNpcEncontro(proximosNpcIds)
            },
            onAtaquesTravados = viewModel::reduzirDefesasPassivasNpcEncontro,
            onEncerrarCombate = viewModel::encerrarCombateEGuardarHistorico,
            historico = historicoCombates
        )
    },
    SheetTab("13. Grupos de Batalha", com.example.R.drawable.tab_icon_battle_groups) {
        BattleGroupsTab(viewModel = viewModel)
    },
    SheetTab("14. Mapa", com.example.R.drawable.tab_icon_mapa) {
        androidx.compose.runtime.key(sheet.id) {
            MapTab(
                modifier = Modifier.fillMaxSize(),
                routePoints = viewModel.mapRoutePoints
            )
        }
    },
    SheetTab("15. Tradutor", com.example.R.drawable.tab_icon_tradutor) {
        androidx.compose.runtime.key(sheet.id) { TranslatorTab(modifier = Modifier.fillMaxSize()) }
    }

)
}
