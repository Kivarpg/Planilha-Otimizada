package com.example.ui.tabs


import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButton

import com.example.model.isDragonBlooded
import com.example.model.isLunar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EncantoSolarDefinition
import com.example.data.EncantosSolaresCatalog
import com.example.data.FeiticoDefinition
import com.example.model.CharacterSheet
import com.example.model.Encanto
import com.example.model.ExaltedConstants
import com.example.ui.components.SectionHeader
import com.example.ui.components.ActiveFilterChips
import com.example.ui.components.SearchButton
import com.example.ui.components.SkillSearchConfig
import com.example.ui.components.SkillSearchModal
import com.example.search.SkillFilter
import com.example.search.SkillSearchEngine
import com.example.ui.theme.ExaltedDangerCore
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.viewmodel.SheetViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
fun CharmsTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    var selectedAbility by remember { mutableStateOf<String?>(null) }
    var catalogDetail by remember { mutableStateOf<EncantoSolarDefinition?>(null) }
    var feiticoDetail by remember { mutableStateOf<FeiticoDefinition?>(null) }
    var detailCharm by remember { mutableStateOf<Encanto?>(null) }
    var charmToDelete by remember { mutableStateOf<Encanto?>(null) }
    var gavetasExpandidas by remember { mutableStateOf(setOf<String>()) }
    var encantosFilter by remember { mutableStateOf(SkillFilter()) }
    var showEncantosSearch by remember { mutableStateOf(false) }
    var abilityTree by remember { mutableStateOf<String?>(null) }

    val feiticariaHabilitada = viewModel.feiticariaHabilitada(sheet)

    // Catálogo e metadados da árvore são estáveis enquanto o Tipo de Exaltado
    // não muda. Mantê-los fora do bloco condicional evita reconstruir o
    // catálogo e criar uma nova função de pré-requisitos a cada recomposição
    // enquanto o diálogo está aberto.
    val charmsParaArvore = remember(viewModel, sheet.tipoPersonagem) {
        viewModel.todosOsEncantosParaArvore(sheet.tipoPersonagem)
    }
    val canonicalPrerequisites = remember(viewModel, sheet.tipoPersonagem) {
        { localId: String ->
            viewModel.prerequisiteIdsCanonicosParaEncanto(localId, sheet.tipoPersonagem)
        }
    }
    val charmTreeCache = remember(viewModel, sheet.tipoPersonagem, charmsParaArvore) { CharmTreeOpenCache(maxEntries = 8) }
    // O long press não deve começar filtrando o catálogo inteiro na mesma
    // recomposição que abre o diálogo. As gavetas são indexadas uma vez por
    // Tipo de Exaltado e o diálogo recebe somente a lista relevante.
    val charmsParaArvorePorHabilidade = remember(sheet.tipoPersonagem, charmsParaArvore) {
        charmsParaArvore.groupBy { it.habilidadeVinculada.trim().lowercase() }
    }
    val acquiredCharmNames = remember(sheet.charms) {
        sheet.charms.mapTo(linkedSetOf()) { it.nome }
    }

    val adquiridoPorNome = remember(sheet.charms) {
        buildMap<String, Encanto> {
            sheet.charms.forEach { charm ->
                putIfAbsent(EncantosSolaresCatalog.normalize(charm.nome), charm)
            }
        }
    }

    // Config de busca para Encantos: opções derivadas dinamicamente dos
    // dados carregados (tipo, palavras-chave, duração, pré-requisitos) — uma
    // nova categoria de Encanto ou palavra-chave não exige mudança de código.
    val encantosSearchable = remember(viewModel, sheet.tipoPersonagem) { viewModel.encantosSearchableParaTipo(sheet.tipoPersonagem) }
    val ehLunar = sheet.tipoPersonagem.isLunar()
    val encantosSearchConfig = remember(encantosSearchable, ehLunar) {
        // Uma única passagem pelo catálogo para montar as três listas de filtros.
        // Evita flatMap/map + distinct intermediários durante recomposições.
        val tipos = HashSet<String>()
        val palavrasChave = HashSet<String>()
        val duracoes = HashSet<String>()
        encantosSearchable.forEach { encanto ->
            encanto.tipo?.let(tipos::add)
            palavrasChave.addAll(encanto.keywords)
            if (encanto.duracao.isNotBlank()) duracoes.add(encanto.duracao)
        }
        SkillSearchConfig(
            skillGroupLabel = if (ehLunar) "Atributo" else "Habilidade",
            availableSkillIds = if (ehLunar) com.example.data.LunarCharmHierarchy.ORDEM_ATRIBUTOS else ExaltedConstants.ALL_25_ABILITIES,
            showMinsEssencia = true,
            availableTypes = tipos.toList().sorted(),
            availableKeywords = palavrasChave.toList().sorted(),
            availableDurations = duracoes.toList().sorted()
        )
    }
    val encantosResultados = remember(encantosFilter, encantosSearchable) {
        if (encantosFilter.isEmpty) emptyList() else SkillSearchEngine.filterSkills(encantosSearchable, encantosFilter)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(8).exaltedContentStage(8)
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SearchButton(activeCount = encantosFilter.activeCount, onClick = { showEncantosSearch = true })
        }
        Spacer(Modifier.height(6.dp))

        if (!encantosFilter.isEmpty) {
            ActiveFilterChips(
                filter = encantosFilter,
                onRemove = { encantosFilter = it },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SectionHeader(title = "RESULTADOS DA BUSCA (${encantosResultados.size})")
            if (encantosResultados.isEmpty()) {
                AppText(
                    "Nenhum Encanto encontrado.\nTente remover um filtro ou alterar os termos da busca.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExaltedMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
                )
                InkButton(label = "Limpar filtros", onClick = { encantosFilter = SkillFilter() }, modifier = Modifier.fillMaxWidth(), fillMaxWidth = true, size = InkButtonSize.Small)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    encantosResultados.forEach { skill ->
                        val def = viewModel.encantoDefinitionPorIdParaTipo(skill.id, sheet.tipoPersonagem)
                        if (def != null) {
                            val item = viewModel.elegibilidadeEncanto(def, sheet)
                            CharmEligibilityRow(
                                item = item,
                                mostrarHabilidade = true,
                                onToggle = { marcado ->
                                    if (marcado) viewModel.addCharmFromDefinition(def)
                                    else {
                                        val adquirido = adquiridoPorNome[EncantosSolaresCatalog.normalize(def.nome)]
                                        adquirido?.let { viewModel.removeCharm(it.id) }
                                    }
                                },
                                onShowDetail = { catalogDetail = def }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        CharmAbilityAndAcquiredSection(
            sheet = sheet,
            viewModel = viewModel,
            selectedAbility = selectedAbility,
            feiticariaHabilitada = feiticariaHabilitada,
            onSelectAbility = { selectedAbility = it },
            onShowAbilityTree = { abilityTree = it },
            gavetasExpandidas = gavetasExpandidas,
            onToggleGaveta = { nome -> gavetasExpandidas = if (nome in gavetasExpandidas) gavetasExpandidas - nome else gavetasExpandidas + nome },
            onShowDetail = { detailCharm = it },
            onLongPress = { charmToDelete = it }
        )

    }

    if (showEncantosSearch) {
        SkillSearchModal(
            config = encantosSearchConfig,
            currentFilter = encantosFilter,
            onApply = { encantosFilter = it; showEncantosSearch = false },
            onDismiss = { showEncantosSearch = false }
        )
    }

    selectedAbility?.let { ability ->
        if (ability == "Artes Marciais") {
            MartialArtsCharmsPopup(
                sheet = sheet,
                viewModel = viewModel,
                onDismiss = { selectedAbility = null },
                onShowDetail = { catalogDetail = it }
            )
        } else if (ability == "Feitiços") {
            FeiticosPopup(
                sheet = sheet,
                viewModel = viewModel,
                onDismiss = { selectedAbility = null },
                onShowDetail = { feiticoDetail = it }
            )
        } else if (sheet.tipoPersonagem.isLunar()) {
            AtributoLunarCharmsPopup(
                atributo = ability,
                sheet = sheet,
                viewModel = viewModel,
                onDismiss = { selectedAbility = null },
                onShowDetail = { catalogDetail = it }
            )
        } else {
            HabilidadeCharmsPopup(
                habilidade = ability,
                sheet = sheet,
                viewModel = viewModel,
                onDismiss = { selectedAbility = null },
                onShowDetail = { catalogDetail = it }
            )
        }
    }

    abilityTree?.let { ability ->
        AbilityCharmTreeDialog(
            ability = ability,
            charms = charmsParaArvorePorHabilidade[ability.trim().lowercase()].orEmpty(),
            onDismiss = { abilityTree = null },
            canonicalPrerequisites = canonicalPrerequisites,
            acquiredCharmNames = acquiredCharmNames,
            preparedTree = charmTreeCache[ability],
            onTreePrepared = { prepared -> charmTreeCache.put(ability, prepared) }
        )
    }

    catalogDetail?.let { def ->
        CatalogDetailsDialog(def = def, onDismiss = { catalogDetail = null }, viewModel = viewModel, charmsParaArvore = charmsParaArvore)
    }

    feiticoDetail?.let { def ->
        FeiticoDetailsDialog(def = def, onDismiss = { feiticoDetail = null })
    }

    detailCharm?.let { charm ->
        CharmDetailsDialog(
            charm = charm,
            onDismiss = { detailCharm = null },
            viewModel = viewModel,
            dragonBlooded = sheet.tipoPersonagem.isDragonBlooded(),
            tipoPersonagem = sheet.tipoPersonagem
        )
    }

    charmToDelete?.let { charm ->
        AlertDialog(
            onDismissRequest = { charmToDelete = null },
            modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
            shape = com.example.ui.components.dialogShape,
            icon = {
                Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = ExaltedDangerCore)
            },
            title = { AppText("Remover Encanto", color = ExaltedAccentBright, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
            text = { AppText("Deseja remover \"${charm.nome}\"?") },
            dismissButton = {
                com.example.ui.components.GildedDialogButton(
                    text = "Remover",
                    onClick = {
                        viewModel.removeCharm(charm.id)
                        charmToDelete = null
                    },
                    isDanger = true
                )
            },
            confirmButton = {
                com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = { charmToDelete = null })
            },
            containerColor = ExaltedDarkSurfaceVariant
        )
    }
}
