package com.example.viewmodel

import com.example.iniciativas.adicionarOuAtualizarNpc
import com.example.iniciativas.atualizarPorOrigemNpcId
import com.example.iniciativas.removerPorOrigemBattleGroupId
import com.example.iniciativas.removerPorOrigemNpcId

import com.example.model.isDragonBlooded
import com.example.model.isLunar
import com.example.model.CharacterType
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SheetRepository
import com.example.data.EncantosSolaresCatalog
import com.example.data.EncantoSolarDefinition
import com.example.data.paraFormatoSolar
import com.example.model.TipoExaltadoEncontro
import com.example.data.FeiticariaCatalog
import com.example.data.FeiticoDefinition
import com.example.data.PreparedEncounterCatalog
import com.example.search.SearchableSkill
import com.example.search.toSearchableSkill
import com.example.model.Casta
import com.example.model.CharacterSheet
import com.example.model.Encanto
import com.example.model.RatingStyle
import com.example.model.Npc
import kotlinx.coroutines.delay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val requiresConfirmation: Boolean = false
)

data class BpBreakdown(
    val attributeBp: Int,
    val abilityBp: Int,
    val specBp: Int,
    val willpowerBp: Int,
    val meritBp: Int,
    val charmBp: Int,
    val totalSpent: Int,
    val remainingBalance: Int
)

@OptIn(FlowPreview::class)
class SheetViewModel(application: Application) : AndroidViewModel(application) {

    // Ver comentário no backup periódico (mais abaixo) pra explicação —
    // achado numa revisão de impacto em bateria: sem isso, o backup a
    // cada 5 min continuaria disparando mesmo com o app em segundo
    // plano, enquanto o processo permanecesse vivo.
    @Volatile
    private var appEmPrimeiroPlano = true

    fun onAppForegrounded() { appEmPrimeiroPlano = true }
    fun onAppBackgrounded() { appEmPrimeiroPlano = false }

    private val repository = SheetRepository(application)
    private val _sheetState = MutableStateFlow(CharacterSheet())
    // Aba 12 (Conflito) — controller do colega, integrado aqui pra
    // sobreviver à troca de aba e à rotação de tela, mesmo padrão do
    // resto do estado deste ViewModel. Carrega o que foi salvo
    // anteriormente (ou um estado vazio, na primeira vez).
    val iniciativasController = com.example.iniciativas.IniciativasController(repository.carregarIniciativas())
    private val battleGroupActions by lazy {
        BattleGroupActions(
            save = { groups -> _sheetState.update { it.copy(battleGroups = groups) } },
            initial = _sheetState.value.battleGroups
        )
    }
    val battleGroups: StateFlow<List<com.example.model.BattleGroup>> = battleGroupActions.groups

    fun salvarBattleGroup(group: com.example.model.BattleGroup) = battleGroupActions.upsert(group)
    fun removerBattleGroup(id: String) {
        iniciativasController.removerPorOrigemBattleGroupId(id)
        battleGroupActions.remove(id)
    }
    fun ajustarMagnitudeBattleGroup(id: String, delta: Int, magnitudeBaseDerivada: Int) =
        battleGroupActions.ajustarMagnitude(id, delta, magnitudeBaseDerivada)
    fun resetarMagnitudeBattleGroup(id: String) = battleGroupActions.resetarMagnitude(id)

    private fun carregarBattleGroupsDaPlanilha(sheet: CharacterSheet) {
        battleGroupActions.load(sheet.battleGroups)
    }
    private val _historicoCombates = MutableStateFlow(repository.carregarHistoricoCombates())
    val historicoCombates: StateFlow<List<com.example.iniciativas.HistoricoCombateEntry>> = _historicoCombates.asStateFlow()

    /** Encerra o combate da Aba 12 e guarda o registro no log (até 3 — pedido do usuário). */
    fun encerrarCombateEGuardarHistorico() {
        // Penalidades de defesa da Aba 11 pertencem somente ao combate atual.
        // Capture os vínculos antes de encerrar, pois encerrarCombate() recria
        // IniciativasState() sem participantes.
        val npcIdsDoCombate = iniciativasController.state.value.participantes
            .mapNotNull { it.origemNpcId }
            .toSet()
        val registro = iniciativasController.encerrarCombate()
        if (registro != null) {
            if (npcIdsDoCombate.isNotEmpty()) {
                encounterNpcActions.limparPenalidadesDefesa(npcIdsDoCombate)
            }
            _historicoCombates.value = repository.adicionarHistoricoCombate(registro)
        }
    }
    private val encantoCatalog = EncantosSolaresCatalog(application)
    private val encantosSangueDragoesCatalog = com.example.data.EncantosSangueDosDragoesCatalog(application)
    private val artesMarciaisCatalog = com.example.data.ArtesMarciaisCatalog(application)

    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // Catálogos são imutáveis durante a vida do ViewModel. Conversões usadas
    // pela busca/árvore não precisam ser reconstruídas a cada recomposição.
    private val solarSearchableSkills: List<SearchableSkill> by lazy {
        encantoCatalog.definitions.map { it.toSearchableSkill() }
    }
    private val dragonBloodedSearchableSkills: List<SearchableSkill> by lazy {
        encantosSangueDragoesCatalog.definitions.map { it.paraFormatoSolar().toSearchableSkill() }
    }
    private val lunarSearchableSkills: List<SearchableSkill> by lazy {
        encantosLunaresCatalog.definitions.map { it.paraFormatoSolar().toSearchableSkill() }
    }
    private val solarCharmTree: List<Encanto> by lazy {
        encantoCatalog.definitions.map { it.toEncanto() }
    }
    private val dragonBloodedCharmTree: List<Encanto> by lazy {
        encantosSangueDragoesCatalog.definitions.map { it.paraFormatoSolar().toEncanto() }
    }
    private val martialArtsCharmTree: List<Encanto> by lazy {
        artesMarciaisCatalog.definitions.flatMap { it.encantos }.map { it.toEncanto() }
    }
    private val lunarCharmTree: List<Encanto> by lazy {
        encantosLunaresCatalog.definitions.map { it.toEncanto() }
    }
    // APPROVED PERFORMANCE REFACTOR
    // A conversão do catálogo Lunar é imutável durante a vida do ViewModel.
    // Reutiliza a mesma materialização já usada pela árvore de Encantos em vez
    // de reconstruir ~N objetos a cada chamada/recomposição.
    private val lunarCharmsCached: List<com.example.model.Encanto> by lazy {
        lunarCharmTree
    }
    private val dragonBloodedCharmByName: Map<String, EncantoSolarDefinition> by lazy {
        buildMap(encantosSangueDragoesCatalog.definitions.size) {
            for (definition in encantosSangueDragoesCatalog.definitions) {
                put(definition.nome, definition.paraFormatoSolar())
            }
        }
    }
    // Estrutura da rodada 1: catálogo carregado e disponível, ainda não
    // conectado a CharmsActions/busca/ShareCode — esses pontos usam
    // Encanto.habilidadeVinculada extensivamente (~19 arquivos) e a
    // integração completa fica pra uma próxima rodada.
    private val encantosLunaresCatalog = com.example.data.EncantosLunaresCatalog(application)
    fun todosOsEncantosLunares(): List<com.example.model.Encanto> = lunarCharmsCached
    fun encantoLunarDefinitionPorId(id: String): com.example.data.EncantoLunarDefinition? = encantosLunaresCatalog.porId(id)
    private val feiticariaCatalog = FeiticariaCatalog(application)
    private val meritosCatalog = com.example.data.MeritosCatalog(application)

    /**
     * Catálogo canônico compartilhado também pela Aba 8. A árvore visual pode
     * consultar dependências pela mesma identidade usada no RulesEngine,
     * eliminando reconstruções por nome conforme os consumidores migram.
     */
    private val preparedEncounterCatalog by lazy(LazyThreadSafetyMode.NONE) {
        PreparedEncounterCatalog.prepare(
            solares = encantoCatalog.definitions,
            sangueDeDragao = encantosSangueDragoesCatalog.definitions,
            lunares = encantosLunaresCatalog.definitions,
            feiticos = feiticariaCatalog.definitions,
            estilosMarciais = artesMarciaisCatalog.definitions
        )
    }

    internal fun prerequisiteIdsCanonicosParaEncanto(
        localId: String,
        tipoPersonagem: String
    ): Set<String> {
        val namespace = when {
            tipoPersonagem.isDragonBlooded() -> PreparedEncounterCatalog.NS_DRAGON_BLOODED
            tipoPersonagem.isLunar() -> PreparedEncounterCatalog.NS_LUNAR
            else -> PreparedEncounterCatalog.NS_SOLAR
        }
        val id = PreparedEncounterCatalog.StableContentId(namespace, localId)
        return preparedEncounterCatalog.prerequisitesOf(id).mapTo(linkedSetOf()) { it.localId }
    }

    private val encounterNpcActionsLazy = lazy {
        EncounterNpcActions(
            state = _npcsEncontro,
            loading = _gerandoNpcEncontro,
            error = _erroNpcEncontro,
            repository = repository,
            solarCatalog = encantoCatalog,
            dragonBloodedCatalog = encantosSangueDragoesCatalog,
            lunarCatalog = encantosLunaresCatalog,
            feiticariaCatalog = feiticariaCatalog,
            meritosCatalog = meritosCatalog,
            martialArtsCatalog = artesMarciaisCatalog,
            preparedEncounterCatalog = preparedEncounterCatalog,
            scope = viewModelScope,
            removerDaIniciativa = { iniciativasController.removerPorOrigemNpcId(it) },
            atualizarNaIniciativa = { npc ->
                iniciativasController.atualizarPorOrigemNpcId(npc.id, npc.nome, npc.iniciativaAtual)
            }
        )
    }
    private val encounterNpcActions: EncounterNpcActions
        get() = encounterNpcActionsLazy.value

    private val npcActions by lazy { NpcActions(_npcs, repository) }

    // Preferência global de estilo de avaliação (Atributos/Habilidades/
    // Méritos), configurável em Configurações — não faz parte da planilha.
    private val _ratingStyle = MutableStateFlow(repository.getRatingStyle())
    val ratingStyle: StateFlow<RatingStyle> = _ratingStyle.asStateFlow()

    fun setRatingStyle(style: RatingStyle) {
        repository.setRatingStyle(style)
        _ratingStyle.value = style
    }

    // --- Busca e filtragem (Aba 8) ---
    // Corrigido: antes sempre pesquisava só o catálogo Solar
    // (encantoCatalog), independente do template ativo — Lunar e Sangue
    // de Dragão nunca apareciam na busca. Agora é uma função que recebe o
    // tipo de personagem e devolve o catálogo certo, convertido pro
    // formato comum via paraFormatoSolar() (mesmo padrão já usado por
    // catalogos indexados por habilidade em CharmsActions.kt).
    fun encantosSearchableParaTipo(tipoPersonagem: String): List<SearchableSkill> = when {
        tipoPersonagem.isDragonBlooded() -> dragonBloodedSearchableSkills
        tipoPersonagem.isLunar() -> lunarSearchableSkills
        else -> solarSearchableSkills
    }

    val feiticosSearchable: List<SearchableSkill> by lazy {
        feiticariaCatalog.definitions.map { it.toSearchableSkill() }
    }

    fun encantoDefinitionPorId(id: String): EncantoSolarDefinition? = encantoCatalog.porId(id)

    // Resolve o ID de busca no catálogo certo pro template — antes
    // encantoDefinitionPorId só olhava o Solar, então um resultado de
    // busca de Encanto Lunar ou de Sangue de Dragão tinha um ID válido,
    // mas essa função devolvia null pra ele, descartando a linha da
    // busca silenciosamente (o código de exibição só mostra a linha
    // quando a definição não é null).
    fun encantoDefinitionPorIdParaTipo(id: String, tipoPersonagem: String): EncantoSolarDefinition? = when {
        tipoPersonagem.isDragonBlooded() -> encantosSangueDragoesCatalog.porId(id)?.paraFormatoSolar()
        tipoPersonagem.isLunar() -> encantosLunaresCatalog.porId(id)?.paraFormatoSolar()
        else -> encantoCatalog.porId(id)
    }

    /** Catálogo completo de Encantos Solares, convertido pro modelo Encanto —
     * usado pela Árvore de Pré-requisitos (Aba 8), que precisa do catálogo
     * inteiro (não só os já adquiridos) pra montar a árvore completa. */
    fun todosOsEncantosSolares(): List<com.example.model.Encanto> = solarCharmTree

    fun todosOsEncantosParaArvore(dragonBlooded: Boolean = false): List<com.example.model.Encanto> =
        if (dragonBlooded) dragonBloodedCharmTree else solarCharmTree

    // Sobrecarga de 3 vias (Solar/Sangue de Dragão/Lunar) — a original
    // acima (booleana) fica pros chamadores que só distinguem Solar/DB.
    fun todosOsEncantosParaArvore(tipoPersonagem: String): List<com.example.model.Encanto> = when {
        tipoPersonagem.isDragonBlooded() -> dragonBloodedCharmTree + martialArtsCharmTree
        tipoPersonagem.isLunar() -> lunarCharmTree + martialArtsCharmTree
        else -> solarCharmTree + martialArtsCharmTree
    }

    val sheetState: StateFlow<CharacterSheet> = _sheetState.asStateFlow()

    // Aba 14 (Mapa) — a rota pertence à sessão da planilha, não à composição
    // da aba. Mantê-la no ViewModel impede que trocar de aba apague os pontos;
    // eles só são removidos quando o usuário aciona Limpar.
    val mapRoutePoints = androidx.compose.runtime.mutableStateListOf<androidx.compose.ui.geometry.Offset>()

    // Chamada uma única vez, ao entrar em MainSheetScreen vindo da tela de
    // seleção de template — troca a planilha padrão (Solar) por uma planilha
    // nova já marcada com o template escolhido. Só faz sentido antes de
    // qualquer edição real acontecer (por isso é chamada de um
    // LaunchedEffect(Unit), não repetidamente).
    fun iniciarNovaPlanilha(tipoPersonagem: String) {
        if (_sheetState.value.tipoPersonagem != tipoPersonagem) {
            val essenciaInicial = if (tipoPersonagem.isDragonBlooded()) 2 else 1
            val novaPlanilha = CharacterSheet(tipoPersonagem = tipoPersonagem, essencia = essenciaInicial)
            _sheetState.value = novaPlanilha
            carregarBattleGroupsDaPlanilha(novaPlanilha)
        }
    }

    // Classes de domínio extraídas do ViewModel — cada uma recebe a MESMA
    // instância de _sheetState (não uma cópia), então o estado continua
    // unificado numa única fonte de verdade mesmo com a lógica dividida
    // em vários arquivos.
    private val casteActions = CasteActions(_sheetState)
    private val equipmentActions = EquipmentActions(_sheetState)

    private val _savedSheets = MutableStateFlow<List<CharacterSheet>>(emptyList())
    val savedSheets: StateFlow<List<CharacterSheet>> = _savedSheets.asStateFlow()

    // NPCs — lista própria do aparelho, independente da CharacterSheet
    // ativa (ver comentário na classe Npc, no modelo). Carregada uma vez
    // na criação do ViewModel; toda alteração persiste imediatamente via
    // repository.salvarNpcs(), sem debounce — a lista tende a ser bem
    // menor que uma planilha, escrita com bem menos frequência.
    private val _npcs = MutableStateFlow(repository.carregarNpcs())
    val npcs: StateFlow<List<Npc>> = _npcs.asStateFlow()

    private val _npcsEncontro = MutableStateFlow(repository.carregarNpcsEncontro())
    val npcsEncontro: StateFlow<List<com.example.model.NpcEncontro>> = _npcsEncontro.asStateFlow()

    // Indicador de carregamento (a geração passa pelo catálogo inteiro de
    // Encantos algumas vezes; sem isso, a tela fica parada sem nenhuma
    // pista de que algo está acontecendo) e mensagem de erro, caso a
    // geração falhe por algum motivo — evita que uma exceção não tratada
    // derrube o app inteiro ("Gerador Harmônico parou").
    private val _gerandoNpcEncontro = MutableStateFlow(false)
    val gerandoNpcEncontro: StateFlow<Boolean> = _gerandoNpcEncontro.asStateFlow()
    private val _erroNpcEncontro = MutableStateFlow<String?>(null)
    val erroNpcEncontro: StateFlow<String?> = _erroNpcEncontro.asStateFlow()
    fun limparErroNpcEncontro() { _erroNpcEncontro.value = null }

    private val _saveValidationEvent = MutableStateFlow<ValidationResult?>(null)
    val saveValidationEvent: StateFlow<ValidationResult?> = _saveValidationEvent.asStateFlow()

    private val _commitmentError = MutableStateFlow<String?>(null)
    val commitmentError: StateFlow<String?> = _commitmentError.asStateFlow()

    // Confirmação pendente: populada quando uma redução de atributo ou
    // habilidade quebraria o pré-requisito de um Mérito já adquirido.
    // AttributesActions/AbilitiesActions NÃO aplicam a mudança direto
    // nesse caso — só preenchem este estado com o que seria perdido e uma
    // função que aplica a mudança de verdade (chamada só se o usuário
    // confirmar na tela de aviso). Compartilhado entre abas porque tanto
    // Atributos quanto Habilidades podem disparar isso.
    private val _pendingMeritBreak = MutableStateFlow<PendingMeritBreak?>(null)
    val pendingMeritBreak: StateFlow<PendingMeritBreak?> = _pendingMeritBreak.asStateFlow()
    fun confirmarPendingMeritBreak() {
        _pendingMeritBreak.value?.aplicar?.invoke()
        _pendingMeritBreak.value = null
    }
    fun cancelarPendingMeritBreak() {
        _pendingMeritBreak.value = null
    }

    private val experienceActions = ExperienceActions(_commitmentError)
    private val personalDataActions = PersonalDataActions(_sheetState, experienceActions, _commitmentError)
    private val attributesActions = AttributesActions(_sheetState, experienceActions, meritosCatalog, _pendingMeritBreak)
    private val meritsActions = MeritsActions(_sheetState, experienceActions, meritosCatalog)
    private val charmsActions = CharmsActions(_sheetState, _commitmentError, encantoCatalog, feiticariaCatalog, experienceActions, encantosSangueDragoesCatalog, encantosLunaresCatalog, artesMarciaisCatalog)
    private val abilitiesActions = AbilitiesActions(_sheetState, experienceActions, charmsActions, meritosCatalog, _pendingMeritBreak)
    private val martialArtsActions = MartialArtsActions(_sheetState, experienceActions, charmsActions)
    private val healthActions = HealthActions(_sheetState, viewModelScope)
    private val combatActions = CombatActions(_sheetState, _commitmentError, experienceActions)

    private val _showReversionConfirm = MutableStateFlow(false)
    val showReversionConfirm: StateFlow<Boolean> = _showReversionConfirm.asStateFlow()

    private val fileManagementActions = FileManagementActions(
        _sheetState, _savedSheets, _saveValidationEvent, _showReversionConfirm, repository, charmsActions,
        encantoCatalog.definitions, feiticariaCatalog.definitions,
        onSheetChanged = ::carregarBattleGroupsDaPlanilha
    )

    fun dismissCommitmentError() {
        _commitmentError.value = null
    }

    init {
        val loaded = repository.loadActiveSheet()
        _sheetState.value = sanitizeKnownCharms(normalizeEssence(loaded))
        carregarBattleGroupsDaPlanilha(_sheetState.value)
        // A leitura do índice pode decodificar várias planilhas e executar
        // migrações. Ela não precisa bloquear a Main thread durante a criação
        // da tela inicial. O resultado continua sendo publicado no mesmo
        // StateFlow, preservando o comportamento da UI após o carregamento.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val salvas = repository.getAllSheets()
            _savedSheets.value = salvas
        }
        // LOGICA: auto-save contínuo — toda alteração na planilha (qualquer
        // campo, em qualquer aba) é persistida automaticamente em disco,
        // não só quando o usuário aperta "Salvar". Sem isso, o Android pode
        // matar o processo em segundo plano (comum sob pressão de memória)
        // e qualquer edição desde o último salvamento manual se perderia.
        // drop(1): ignora a primeira emissão (o próprio valor recém-
        // carregado acima), evitando reescrever o disco com o que já está
        // lá sem necessidade. debounce(400): interações rápidas (segurar
        // um botão de +/-, digitar) disparam muitas mudanças de estado por
        // segundo — sem isso, cada uma delas gravaria no disco
        // individualmente. 400ms é curto o bastante pra manter a garantia
        // de "sempre salvo" na prática (a maioria dos casos reais de morte
        // de processo pelo Android acontece bem depois do app ir pra
        // segundo plano, não numa janela de meio segundo durante uso ativo).
        viewModelScope.launch {
            _sheetState.drop(1).debounce(400).collect { sheet ->
                repository.saveActiveSheet(sheet)
            }
        }

        // Mesmo auto-save debounced, agora pro estado da Aba 12 —
        // controller próprio, StateFlow própria, mas o raciocínio de
        // "não gravar a cada tecla" é idêntico.
        viewModelScope.launch {
            iniciativasController.state.drop(1).debounce(400).collect { estado ->
                repository.salvarIniciativas(estado)
            }
        }

        // Conclusão automática — assim que Atributos, Habilidades, Méritos,
        // Encantos e Pontos de Bônus estiverem todos totalmente distribuídos
        // (ver SheetCalculations.computeCompletionIssues, a mesma checagem
        // por trás da luz verde), a planilha passa a "Concluída" sozinha, sem
        // precisar de um botão manual. marcarPlanilhaConcluida() já é segura
        // pra chamar repetidamente: ela mesma verifica se já está concluída
        // e se as pendências realmente acabaram antes de fazer qualquer
        // coisa, então observar TODA mudança de estado aqui não arrisca
        // reprocessar ou entrar em loop. Planilhas em Modo Livre nunca
        // concluem sozinhas — esse modo não segue as mesmas regras de
        // criação por Pontos de Bônus.
        viewModelScope.launch {
            _sheetState.collect { sheet ->
                if (!sheet.planilhaConcluida && !sheet.modoLivre) {
                    fileManagementActions.marcarPlanilhaConcluida()
                }
            }
        }

        // Backup periódico — a cada 5 minutos, guarda um snapshot rotativo
        // (últimos 5) separado do auto-save contínuo acima. O auto-save
        // protege contra o app fechar; isso aqui protege contra uma
        // edição indesejada que o próprio auto-save já gravou por cima,
        // dando uma forma de "voltar no tempo" pra um estado de minutos
        // atrás sem precisar de exportação manual.
        // appEmPrimeiroPlano (ver onAppForegrounded/onAppBackgrounded,
        // chamadas por SheetScreen.kt via ciclo de vida do processo):
        // enquanto o app estiver em segundo plano, pula o backup — o
        // processo pode continuar vivo por um tempo depois que o usuário
        // troca de app, e sem essa checagem o backup continuaria
        // disparando a cada 5 min à toa, gastando bateria sem necessidade
        // real (o auto-save contínuo já cobre a proteção principal).
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                if (appEmPrimeiroPlano) {
                    repository.salvarBackupPeriodico(_sheetState.value)
                }
            }
        }
    }

    // --- Gestão de arquivo/sessão e Planilha Concluída — ver FileManagementActions.kt ---
    fun reloadSavedSheets() = fileManagementActions.reloadSavedSheets()
    fun loadSheet(sheet: CharacterSheet) = fileManagementActions.loadSheet(sheet)
    // "Novo" reseta TUDO — incluindo NPCs, que vivem fora de CharacterSheet
    // (guardados à parte por serem originalmente pensados como "da
    // campanha", não por planilha individual). Planilha, Combate, Habilidades
    // etc. já resetam sozinhos porque fileManagementActions.createNewSheet()
    // substitui o CharacterSheet inteiro por um novo — NPCs é o único
    // estado que precisa ser limpo explicitamente aqui.
    fun createNewSheet(tipoPersonagem: String = CharacterType.SOLAR) {
        fileManagementActions.createNewSheet(tipoPersonagem)
        com.example.ui.theme.aplicarPaletaPorTemplate(tipoPersonagem)
        _npcs.value = emptyList()
        repository.salvarNpcs(emptyList())
        encounterNpcActions.limpar()
    }
    fun exportarComoCodigo(): Result<String> = fileManagementActions.exportarComoCodigo()

    fun importarPlanilhaPorCodigo(codigo: String): com.example.data.ShareCodeCodec.ResultadoImportacao =
        com.example.data.ShareCodeCodec.importar(codigo, encantoCatalog.definitions, feiticariaCatalog.definitions)
    fun aplicarPlanilhaImportada(sheet: CharacterSheet) = fileManagementActions.aplicarPlanilhaImportada(sheet)
    fun listarBackupsPeriodicos(): List<com.example.data.SheetRepository.BackupSnapshot> = fileManagementActions.listarBackupsPeriodicos()
    fun restaurarBackupPeriodico(snapshot: com.example.data.SheetRepository.BackupSnapshot): Boolean = fileManagementActions.restaurarBackupPeriodico(snapshot)
    fun deleteSheet(sheetId: String) = fileManagementActions.deleteSheet(sheetId)
    fun dismissValidationDialog() = fileManagementActions.dismissValidationDialog()
    fun confirmSaveIncompleteSheet() = fileManagementActions.confirmSaveIncompleteSheet()


    // --- Personal Data Updates ---
    fun updateNome(valStr: String) = personalDataActions.updateNome(valStr)
    fun updateJogador(valStr: String) = personalDataActions.updateJogador(valStr)
    fun updateConceito(valStr: String) = personalDataActions.updateConceito(valStr)
    fun updateDescricaoAnima(valStr: String) = personalDataActions.updateDescricaoAnima(valStr)
    fun updateFalhaVirtude(valStr: String) = personalDataActions.updateFalhaVirtude(valStr)
    fun updateLimiteGatilho(valStr: String) = personalDataActions.updateLimiteGatilho(valStr)
    fun updateLimiteContador(valInt: Int) = personalDataActions.updateLimiteContador(valInt)

    fun addIntimidade(nome: String, tipo: String, intensidade: String) = personalDataActions.addIntimidade(nome, tipo, intensidade)
    fun removeIntimidade(id: String) = personalDataActions.removeIntimidade(id)
    fun updateLinguaNativa(lingua: String?) = personalDataActions.updateLinguaNativa(lingua)
    fun addLinguaAdicional(lingua: String) = personalDataActions.addLinguaAdicional(lingua)
    fun removeLinguaAdicional(lingua: String) = personalDataActions.removeLinguaAdicional(lingua)

    fun updateCasta(newCasta: Casta) = casteActions.updateCasta(newCasta)
    fun updateLunarCasta(newCasta: com.example.model.LunarCasta) = casteActions.updateLunarCasta(newCasta)
    // Aspecto de Sangue de Dragão — não usa CasteActions porque não tem a
    // mesma lógica de "5 de 8 habilidades escolhidas" (Aspecto já vem com
    // suas 5 habilidades fixas, sem escolha); só troca o campo e limpa as
    // avaliações das habilidades do Aspecto anterior, se houver.
    fun updateAspecto(novoAspecto: com.example.model.Aspecto) {
        _sheetState.update { atual ->
            atual.copy(aspecto = novoAspecto.displayName, casteAbilities = novoAspecto.allowedAbilities())
        }
    }
    fun toggleCasteAbility(abilityName: String) = casteActions.toggleCasteAbility(abilityName)
    fun toggleFavoredAbility(abilityName: String) = casteActions.toggleFavoredAbility(abilityName)
    fun toggleFavoredAttribute(attributeName: String) = casteActions.toggleFavoredAttribute(attributeName)
    fun toggleLunarCasteAttribute(attributeName: String) = casteActions.toggleLunarCasteAttribute(attributeName)
    fun updateLunarFormaEspiritual(valor: String) = casteActions.updateLunarFormaEspiritual(valor)
    fun updateLunarSinal(valor: String) = casteActions.updateLunarSinal(valor)
    fun setSupernalAbility(abilityName: String?) = casteActions.setSupernalAbility(abilityName)

    fun setGroupPriority(group: String, priority: String) = attributesActions.setGroupPriority(group, priority)


    // Retorna a planilha exatamente como estava no momento em que foi marcada
    // como "Planilha Concluída" (snapshot salvo por marcarPlanilhaConcluida()).
    // Usado para impedir que os botões de redução apaguem valores definidos
    // durante a criação do personagem (Atributos e Habilidades).

    fun setAttributeRating(attrName: String, rating: Int) = attributesActions.setAttributeRating(attrName, rating)

    // --- Ability Rating ---
    fun setAbilityRating(abilityName: String, rating: Int) = abilitiesActions.setAbilityRating(abilityName, rating)

    // --- Specializations CRUD ---
    fun addSpecialization(nome: String, habilidade: String) = abilitiesActions.addSpecialization(nome, habilidade)
    fun removeSpecialization(id: String) = abilitiesActions.removeSpecialization(id)

    // --- Combat & Motes ---
    // Essência é derivada exclusivamente do XP Total; não existe edição manual.
    // O piso do gasto (limite inferior) é o quanto já está travado por
    // comitamento de arma/armadura Artefato — o usuário não pode "subir" os
    // motes disponíveis além desse piso, mesmo com os botões manuais.
    // --- Domínio de Combate — ver CombatActions.kt ---
    fun updateMotesPessoaisGastos(valInt: Int) = combatActions.updateMotesPessoaisGastos(valInt)
    fun updateMotesPerifericosGastos(valInt: Int) = combatActions.updateMotesPerifericosGastos(valInt)
    fun updateForcaVontadeBase(valInt: Int) = combatActions.updateForcaVontadeBase(valInt)
    fun toggleForcaVontadeUsado(indice: Int) = combatActions.toggleForcaVontadeUsado(indice)

    fun addWeapon(nome: String, habilidadeVinculada: String, atributoBriga: String?, tipoArma: String, categoriaPeso: String, ataqueDesarmado: Boolean = false, etiquetas: List<String> = emptyList()) =
        combatActions.addWeapon(nome, habilidadeVinculada, atributoBriga, tipoArma, categoriaPeso, ataqueDesarmado, etiquetas)
    fun updateWeaponManual(id: String, iniciativa: String, decisivo: String, defesa: Int, dano: String, danoMinimo: String) =
        combatActions.updateWeaponManual(id, iniciativa, decisivo, defesa, dano, danoMinimo)
    fun removeWeapon(id: String) = combatActions.removeWeapon(id)
    fun comitarArma(id: String, motesPessoais: Int, motesPerifericos: Int): Boolean = combatActions.comitarArma(id, motesPessoais, motesPerifericos)
    fun equipWeaponWithMoteSource(id: String, motesPessoais: Int, motesPerifericos: Int): Boolean = combatActions.equipWeaponWithMoteSource(id, motesPessoais, motesPerifericos)
    fun descomitarArma(id: String) = combatActions.descomitarArma(id)
    fun toggleWeaponEquipped(id: String) = combatActions.toggleWeaponEquipped(id)

    fun addArmor(nome: String, tipoArmadura: String, categoriaPeso: String, marcadores: List<String> = emptyList()) = combatActions.addArmor(nome, tipoArmadura, categoriaPeso, marcadores)
    fun removeArmor(id: String) = combatActions.removeArmor(id)
    fun equipArmorWithMoteSource(id: String, motesPessoais: Int, motesPerifericos: Int): Boolean = combatActions.equipArmorWithMoteSource(id, motesPessoais, motesPerifericos)
    fun toggleArmorEquipped(id: String) = combatActions.toggleArmorEquipped(id)


    // --- Equipamentos: pertences e sessões/experiência ---
    fun updatePertences(valStr: String) = equipmentActions.updatePertences(valStr)

    fun updateSessoes(valInt: Int) = _sheetState.update {
        val sessoes = valInt.coerceIn(0, 60)
        sanitizeKnownCharms(normalizeEssence(it.copy(sessoes = sessoes)))
    }

    private fun essenceFromXp(xpGasto: Int): Int = SheetCalculations.essenceFromXp(xpGasto)

    private fun normalizeEssence(sheet: CharacterSheet): CharacterSheet = SheetCalculations.normalizeEssence(sheet)

    // --- Artes Marciais (habilidades customizadas) ---
    // Nova Arte Marcial sempre começa com o contador zerado — mesmo tratamento
    // das demais habilidades, cujo primeiro ponto é comprado depois via +/-.
    fun addMartialArt(nome: String) = martialArtsActions.addMartialArt(nome)
    fun possuiMeritoArtistaMarcial(sheet: CharacterSheet): Boolean = martialArtsActions.possuiMeritoArtistaMarcial(sheet)
    fun possuiBrigaMinima(sheet: CharacterSheet): Boolean = martialArtsActions.possuiBrigaMinima(sheet)
    fun removeMartialArt(id: String) = martialArtsActions.removeMartialArt(id)
    fun updateMartialArtValue(id: String, valInt: Int) = martialArtsActions.updateMartialArtValue(id, valInt)

    /**
     * Nomes de todos os estilos de Arte Marcial cadastrados no catálogo da Aba 8
     * (Encantos), filtrados pelo tipo de Exaltado quando aplicável (ex.: estilos
     * exclusivos de Sangue de Dragão). Usado pelo diálogo "Adicionar Arte Marcial"
     * na Aba 4 para o jogador selecionar um estilo em vez de digitar o nome livremente.
     */
    fun nomesEstilosArtesMarciaisParaCadastro(sheet: CharacterSheet = _sheetState.value): List<String> {
        val tipo = when {
            sheet.tipoPersonagem.isDragonBlooded() -> com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO
            sheet.tipoPersonagem.isLunar() -> com.example.model.TipoExaltadoEncontro.LUNAR
            else -> com.example.model.TipoExaltadoEncontro.SOLAR
        }
        return artesMarciaisCatalog.definitions
            .filter { com.example.data.ArtesMarciaisAvailability.disponivel(it.nomePt, tipo) }
            .map { it.nomePt }
            .sorted()
    }


    // --- Health Track CRUD & Damage Cycle ---
    // --- Domínio de Vitalidade — ver HealthActions.kt ---
    fun addExtraHealthBox(penalidade: String) = healthActions.addExtraHealthBox(penalidade)
    fun removeExtraHealthBox(id: String) = healthActions.removeExtraHealthBox(id)
    fun cycleHealthDamage(id: String) = healthActions.cycleHealthDamage(id)
    fun clearHealthDamage() = healthActions.clearHealthDamage()

    // --- Merits CRUD ---
    fun addMerit(nome: String, valor: Int) = meritsActions.addMerit(nome, valor)
    fun addMeritoPersonalizado(nome: String, valor: Int, categoria: String, preRequisitoTexto: String) =
        meritsActions.addMeritoPersonalizado(nome, valor, categoria, preRequisitoTexto)
    fun removeMerit(id: String) = meritsActions.removeMerit(id)
    fun updateMeritDetalhe(id: String, detalhe: String) = meritsActions.updateMeritDetalhe(id, detalhe)
    // APPROVED PERFORMANCE REFACTOR
    // Os catálogos de Méritos são imutáveis durante a vida do ViewModel e a
    // restrição depende somente do tipo de personagem. A UI pode consultar
    // estas propriedades várias vezes durante recomposição; pré-calcular as
    // três variantes evita filter + alocação de List em cada leitura.
    private val meritosNormaisPorTipo: Map<String, List<com.example.data.MeritoDefinition>> by lazy {
        listOf(CharacterType.SOLAR, CharacterType.DRAGON_BLOODED, CharacterType.LUNAR).associateWith { tipo ->
            meritosCatalog.normais.filter { it.restritoAoTemplate == null || it.restritoAoTemplate == tipo }
        }
    }
    private val meritosSobrenaturaisPorTipo: Map<String, List<com.example.data.MeritoDefinition>> by lazy {
        listOf(CharacterType.SOLAR, CharacterType.DRAGON_BLOODED, CharacterType.LUNAR).associateWith { tipo ->
            meritosCatalog.sobrenaturais.filter { it.restritoAoTemplate == null || it.restritoAoTemplate == tipo }
        }
    }
    val meritosNormais get() = meritosNormaisPorTipo[sheetState.value.tipoPersonagem] ?: meritosCatalog.normais
    val meritosSobrenaturais get() = meritosSobrenaturaisPorTipo[sheetState.value.tipoPersonagem] ?: meritosCatalog.sobrenaturais
    fun meritoDefinitionPorNome(nome: String) = meritosCatalog.porNome(nome)

    // Busca a definição completa de um Encanto pelo nome, em qualquer um
    // dos dois catálogos — usado pra mostrar o popup de texto completo
    // no card do NPC (Aba 11), já que NpcEncontro só guarda uma versão
    // simplificada (nome/habilidade/custo), sem a descrição.
    private val solarCharmByName: Map<String, EncantoSolarDefinition> by lazy {
        buildMap(encantoCatalog.definitions.size) {
            for (definition in encantoCatalog.definitions) {
                put(definition.nome, definition)
            }
        }
    }

    fun encantoDefinitionPorNome(nome: String): EncantoSolarDefinition? =
        solarCharmByName[nome] ?: dragonBloodedCharmByName[nome]

    // APPROVED FUNCTIONAL FIX — Aba 11 / Encantos do NPC
    // NpcEncontro armazena apenas o nome simplificado do Encanto. A busca
    // anterior consultava somente os catálogos Solar e Sangue de Dragão e
    // ainda exigia igualdade textual exata nas duas tabelas em memória.
    // Isso fazia os Encantos Lunares (e nomes com diferença de acentuação)
    // retornarem null; o long press então abria o estado de detalhe, mas o
    // bloco descartava silenciosamente a visualização porque `definicao`
    // era null. Resolver pelo tipo real do NPC mantém a fonte de dados correta.
    fun encantoDefinitionPorNomeParaTipo(
        nome: String,
        tipo: TipoExaltadoEncontro
    ): EncantoSolarDefinition? = when (tipo) {
        TipoExaltadoEncontro.SOLAR -> encantoCatalog.porNome(nome)
        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> encantosSangueDragoesCatalog.definitions.firstOrNull { EncantosSolaresCatalog.sameName(it.nome, nome) }?.paraFormatoSolar()
        TipoExaltadoEncontro.LUNAR -> encantosLunaresCatalog.porNome(nome)?.paraFormatoSolar()
    }

    // --- NPCs (independentes da planilha ativa) ---
    fun addNpc(nome: String, lealdade: String, tipo: String, descricao: String) =
        npcActions.add(nome, lealdade, tipo, descricao)

    fun removeNpc(id: String) = npcActions.remove(id)

    fun exportarNpcsComoCodigo(): Result<String> = npcActions.exportarComoCodigo()

    // --- Gerador de Encontros (Aba 11, independente da planilha ativa) ---
    fun gerarNpcEncontro(
        nomeManual: String,
        arquetipo: com.example.model.ArquetipoEncontro,
        culturaNome: com.example.data.CulturaNome? = null,
        generoNome: com.example.data.GeneroNome? = null,
        onResultado: (com.example.model.NpcEncontro?) -> Unit,
        focoPersonalizado: String? = null,
        customizacao: com.example.data.EncounterCustomization? = null
    ) = encounterNpcActions.gerarSolar(nomeManual, arquetipo, culturaNome, generoNome, onResultado, focoPersonalizado, customizacao)

    fun gerarNpcSangueDeDragao(
        nomeManual: String,
        arquetipo: com.example.model.ArquetipoEncontro,
        origemNome: com.example.data.OrigemNomeSangueDeDragao = com.example.data.OrigemNomeSangueDeDragao.SEM_CASTA,
        generoNome: com.example.data.GeneroNome? = null,
        onResultado: (com.example.model.NpcEncontro?) -> Unit,
        focoPersonalizado: String? = null,
        customizacao: com.example.data.EncounterCustomization? = null
    ) = encounterNpcActions.gerarSangueDeDragao(nomeManual, arquetipo, origemNome, generoNome, onResultado, focoPersonalizado, customizacao)

    fun gerarNpcLunar(
        nomeManual: String,
        arquetipo: com.example.model.ArquetipoEncontro,
        generoNome: com.example.data.GeneroNome? = null,
        onResultado: (com.example.model.NpcEncontro?) -> Unit,
        focoPersonalizado: String? = null,
        customizacao: com.example.data.EncounterCustomization? = null
    ) = encounterNpcActions.gerarLunar(nomeManual, arquetipo, generoNome, onResultado, focoPersonalizado, customizacao)

    fun removeNpcEncontro(id: String) = encounterNpcActions.remove(id)

    fun cycleHealthDamageNpcEncontro(npcId: String, boxId: String) =
        encounterNpcActions.cycleHealthDamage(npcId, boxId)

    fun clearHealthDamageNpcEncontro(npcId: String) =
        encounterNpcActions.clearHealthDamage(npcId)

    fun removeExtraHealthBoxNpcEncontro(npcId: String, boxId: String) =
        encounterNpcActions.removeExtraHealthBox(npcId, boxId)

    fun feiticosDisponiveisNpcEncontro(npcId: String): List<com.example.data.FeiticoDefinition> =
        encounterNpcActions.feiticosDisponiveis(npcId)

    /** Catálogo completo para detalhes de Feitiços já adquiridos.
     * Não concede nem reabre a seleção gratuita após o primeiro XP.
     */
    fun feiticoDefinitionPorNome(nome: String): com.example.data.FeiticoDefinition? =
        feiticariaCatalog.definitions.firstOrNull {
            com.example.data.EncantosSolaresCatalog.sameName(it.nome, nome)
        }

    fun atualizarFeiticoNpcEncontro(npcId: String, def: com.example.data.FeiticoDefinition, adicionar: Boolean) =
        encounterNpcActions.atualizarFeitico(npcId, def, adicionar)

    fun reduzirDefesasPassivasNpcEncontro(npcIds: Collection<String>) =
        encounterNpcActions.reduzirDefesasPorAtaques(npcIds)

    fun ajustarIniciativaNpcEncontro(npcId: String, delta: Int) =
        encounterNpcActions.adjustInitiative(npcId, delta)

    fun atualizarArmaNpcEncontro(npcId: String, novaArma: com.example.model.ArmaEncontro) =
        encounterNpcActions.atualizarArmaNpc(npcId, novaArma)

    fun atualizarArmaduraNpcEncontro(npcId: String, novaArmadura: com.example.model.ArmaduraEncontro) =
        encounterNpcActions.atualizarArmaduraNpc(npcId, novaArmadura)

    fun carregarNpcDeArquivo(npcId: String, carregado: com.example.model.NpcEncontro) =
        encounterNpcActions.carregarDeArquivo(npcId, carregado)

    fun aplicarPenalidadeClashNpc(npcId: String) = encounterNpcActions.aplicarPenalidadeClash(npcId)

    fun limparPenalidadesDefesaNpcEncontro(npcIds: Collection<String>) =
        encounterNpcActions.limparPenalidadesDefesa(npcIds)

    /** Rola secretamente a iniciativa do NPC e o insere/atualiza na Aba 12. */
    fun juntarNpcEncontroABatalha(npcId: String): Boolean {
        val npc = _npcsEncontro.value.firstOrNull { it.id == npcId } ?: return false
        val iniciativa = com.example.iniciativas.IniciativaRollService.rolar(npc.juntarABatalha)
        return iniciativasController.adicionarOuAtualizarNpc(
            nome = npc.nome,
            iniciativa = iniciativa,
            origemNpcId = npc.id
        )
    }

    fun expandirEncantosNpcEncontro(npcId: String) = encounterNpcActions.expandirEncantos(npcId)

    fun reduzirExperienciaNpcEncontro(npcId: String) = encounterNpcActions.reduzirExperiencia(npcId)

    fun toggleForcaDeVontadeNpcEncontro(npcId: String, indice: Int) =
        encounterNpcActions.toggleForcaDeVontade(npcId, indice)

    fun updateForcaDeVontadeBaseNpcEncontro(npcId: String, novoValor: Int) =
        encounterNpcActions.updateForcaDeVontadeBase(npcId, novoValor)

    fun aplicarNpcsImportados(npcsRecebidos: List<Npc>) = npcActions.aplicarImportados(npcsRecebidos)

    // --- Encantos vindos exclusivamente de encantos_solares.json ---
    // Regra da Habilidade Superna: o personagem é considerado como possuindo
    // Essência 5 nessa habilidade especificamente, permitindo comprar os
    // Encantos de Essência 5 associados a ela mesmo com Essência real menor.
    // --- Domínio de Encantos/Feitiçaria — ver CharmsActions.kt ---
    fun encantosDaHabilidadeComElegibilidade(habilidade: String): List<EncantoComElegibilidade> =
        charmsActions.encantosDaHabilidadeComElegibilidade(habilidade)

    fun artesMarciaisHabilitadas(sheet: CharacterSheet = _sheetState.value): Boolean =
        charmsActions.artesMarciaisHabilitadas(sheet)

    fun estilosArtesMarciaisDisponiveis(sheet: CharacterSheet = _sheetState.value) =
        charmsActions.estilosArtesMarciaisDisponiveis(sheet)

    fun encantosDaArteMarcialComElegibilidade(estiloId: String, sheet: CharacterSheet = _sheetState.value): List<EncantoComElegibilidade> =
        charmsActions.encantosDaArteMarcialComElegibilidade(estiloId, sheet)

    /** Retorna somente os Encantos do estilo marcial indicado para a árvore de pré-requisitos.
     * Isso evita montar o grafo contra todos os catálogos quando o usuário
     * está visualizando um Encanto específico de Artes Marciais.
     */
    fun encantosDaArteMarcialParaArvore(estiloId: String): List<com.example.model.Encanto> =
        artesMarciaisCatalog.encantosDoEstilo(estiloId).map { it.toEncanto() }

    fun encantosLunaresPorAtributoESubdivisaoComElegibilidade(atributo: String, subdivisao: String?): List<EncantoComElegibilidade> =
        charmsActions.encantosLunaresPorAtributoESubdivisaoComElegibilidade(atributo, subdivisao)

    fun elegibilidadeEncanto(def: EncantoSolarDefinition, sheet: CharacterSheet = _sheetState.value): EncantoComElegibilidade =
        charmsActions.elegibilidadeEncanto(def, sheet)

    fun addCharmFromDefinition(def: EncantoSolarDefinition): Boolean = charmsActions.addCharmFromDefinition(def)

    fun removeCharm(id: String) = charmsActions.removeCharm(id)

    fun toggleCharmPin(id: String) = charmsActions.toggleCharmPin(id)

    fun feiticariaHabilitada(sheet: CharacterSheet = _sheetState.value): Boolean = charmsActions.feiticariaHabilitada(sheet)

    fun circulosDesbloqueados(sheet: CharacterSheet = _sheetState.value): Set<String> = charmsActions.circulosDesbloqueados(sheet)

    fun feiticosDoCirculo(circulo: String): List<FeiticoDefinition> = charmsActions.feiticosDoCirculo(circulo)

    fun addFeiticoFromDefinition(def: FeiticoDefinition): Boolean = charmsActions.addFeiticoFromDefinition(def)

    // Ainda usada por setAbilityRating/updateMartialArtValue, que continuam
    // no SheetViewModel esperando este domínio (agora resolvido) — mantida
    // privada aqui, delegando pra versão pública em CharmsActions.
    private fun sanitizeKnownCharms(sheet: CharacterSheet): CharacterSheet = charmsActions.sanitizeKnownCharms(sheet)


    // --- Contador de Iniciativa (Aba Combate) ---
    // O valor começa nulo (nenhum valor definido ainda); a primeira interação
    // do usuário (botão ou edição direta) ativa o monitoramento da tag
    // "Atordoado", que passa a valer sempre que o valor for <= 0.
    fun updateIniciativa(valInt: Int) = combatActions.updateIniciativa(valInt)
    fun ajustarIniciativa(delta: Int) = combatActions.ajustarIniciativa(delta)
    fun limparCombate() = combatActions.limparCombate()

    // Quantidade de pontos de habilidade (pool de 28 dots gratuitos, dots 1-3
    // de cada habilidade) que ainda não foram gastos. Dots além do pool viram
    // custo de Pontos de Bônus (ver calculateBpBreakdown) e não voltam a contar aqui.
    fun calculateAbilityPointsRemaining(sheet: CharacterSheet = _sheetState.value): Int =
        SheetCalculations.calculateAbilityPointsRemaining(sheet)

    // --- Tabela 1.6: Custos em Experiência ---
    // Custo do nível "nivel" -> "nivel + 1" de uma Habilidade ou Arte Marcial.
    // O primeiro ponto (nivel 0 -> 1) custa sempre 3, independente de Casta/Favorecida.
    // custoAumentoHabilidade agora vive em SheetCalculations (função pura,
    // sem acesso a estado do ViewModel) — chamado como
    // SheetCalculations.custoAumentoHabilidade(...) nos pontos de uso.

    // Custo de um Encanto/Feitiço/Necromancia recém-adquirido.
    // custoExperienciaEncanto agora vive em SheetCalculations (função
    // pura) — chamado como SheetCalculations.custoExperienciaEncanto(...).

    // --- Indicador de Status & "Planilha Concluída" ---
    // Lista de pendências que impedem o indicador de ficar verde e o botão
    // "Planilha concluída" de ser habilitado. Mais rigorosa que a validação de
    // salvamento (validateAndSaveSheet), pois exige o saldo de Pontos de Bônus
    // exatamente zerado e os campos obrigatórios preenchidos.
    // LOGICA: lista de pendências para permitir marcar "Planilha Concluída".
    // Cada item corresponde a um campo obrigatório específico, exibido por
    // nome na mensagem de aviso — não simplificar/reverter sem confirmar antes.
    // Retorna os requisitos pendentes agrupados por aba (chave = rótulo da
    // aba, ex.: "Aba 2 — Casta"; valor = lista de requisitos daquela aba,
    // sem repetir o rótulo em cada item). LinkedHashMap preserva a ordem de
    // inserção (mesma ordem das abas no app). "Pontos de Bônus" não é uma
    // aba específica — fica com sua própria chave, sem prefixo "Aba N".
    fun computeCompletionIssues(sheet: CharacterSheet = _sheetState.value): LinkedHashMap<String, MutableList<String>> =
        SheetCalculations.computeCompletionIssues(sheet)

    fun isSheetComplete(sheet: CharacterSheet = _sheetState.value): Boolean =
        SheetCalculations.isSheetComplete(sheet)

    fun calculateBpBreakdown(sheet: CharacterSheet = _sheetState.value): BpBreakdown =
        SheetCalculations.calculateBpBreakdown(sheet)

    // --- Planilha Concluída/reversão/validação — ver FileManagementActions.kt ---
    fun toggleModoLivre() = fileManagementActions.toggleModoLivre()
    fun solicitarDesmarcarPlanilhaConcluida() = fileManagementActions.solicitarDesmarcarPlanilhaConcluida()
    fun cancelarReversaoPlanilhaConcluida() = fileManagementActions.cancelarReversaoPlanilhaConcluida()
    fun confirmarReversaoPlanilhaConcluida() = fileManagementActions.confirmarReversaoPlanilhaConcluida()
    fun validateAndSaveSheet() = fileManagementActions.validateAndSaveSheet()

    override fun onCleared() {
        // O roadmap é somente cache derivado. Antes de liberar o ViewModel,
        // cancela prefetches/gerações pendentes e descarta esse cache sem
        // alterar os NPCs persistidos. O lazy não é inicializado apenas
        // para executar a limpeza.
        if (encounterNpcActionsLazy.isInitialized()) {
            encounterNpcActionsLazy.value.dispose()
        }
        repository.close()
        super.onCleared()
    }
}
