package com.example.viewmodel

import com.example.data.EncantosSangueDosDragoesCatalog
import com.example.data.EncantosSolaresCatalog
import com.example.data.EncounterGenerator
import com.example.data.EncounterProgressionRoadmapService
import com.example.data.EncounterExperienceService
import com.example.data.EncounterMutationPipeline
import com.example.data.PreparedEncounterCatalog
import com.example.data.CulturaNome
import com.example.data.GeneroNome
import com.example.data.OrigemNomeSangueDeDragao
import com.example.data.SheetRepository
import com.example.model.ArquetipoEncontro
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns encounter-NPC mutations and generation. The ViewModel remains the
 * state owner, while this class owns the orchestration and persistence rules.
 */
internal class EncounterNpcActions(
    private val state: MutableStateFlow<List<NpcEncontro>>,
    private val loading: MutableStateFlow<Boolean>,
    private val error: MutableStateFlow<String?>,
    private val repository: SheetRepository,
    private val solarCatalog: EncantosSolaresCatalog,
    private val dragonBloodedCatalog: EncantosSangueDosDragoesCatalog,
    private val lunarCatalog: com.example.data.EncantosLunaresCatalog,
    private val feiticariaCatalog: com.example.data.FeiticariaCatalog,
    private val meritosCatalog: com.example.data.MeritosCatalog,
    private val martialArtsCatalog: com.example.data.ArtesMarciaisCatalog? = null,
    // PERFORMANCE CONTRACT: o catálogo canônico já é preparado pelo SheetViewModel.
    // Torná-lo obrigatório impede uma segunda compilação acidental de todos os
    // índices/grafos ao construir EncounterNpcActions.
    private val preparedEncounterCatalog: PreparedEncounterCatalog,
    private val scope: CoroutineScope,
    private val removerDaIniciativa: (String) -> Unit,
    private val atualizarNaIniciativa: (NpcEncontro) -> Unit
) {
    // O catálogo canônico é preparado pelo SheetViewModel e compartilhado
    // entre Aba 8 e Aba 11; não existe mais uma segunda compilação do grafo.

    // Calculado uma única vez a partir do conteúdo canônico carregado.
    private val roadmapCatalogFingerprint = EncounterProgressionRoadmapService.catalogFingerprint(
        preparedEncounterCatalog.solares,
        preparedEncounterCatalog.sangueDeDragao,
        preparedEncounterCatalog.lunares,
        preparedEncounterCatalog.estilosMarciais
    )

    // O primeiro +XP Solar pode cair no fallback antes de existir roadmap.
    // Preparar o expansor uma vez evita reconstruir os índices imutáveis do
    // catálogo justamente nesse clique mais sensível à latência.
    private val solarXpExpander = com.example.data.SolarEncounterGenerator
        .criarExpansorXpPreparado(preparedEncounterCatalog.solares)

    private val dragonBloodedXpCatalog = com.example.data.DragonBloodedEncounterGenerator
        .catalogoConvertidoPreparado(preparedEncounterCatalog.sangueDeDragao)

    // O custo favorecido de Sangue de Dragão depende apenas de Aspecto +
    // Habilidades Favorecidas, que são imutáveis durante a progressão por XP.
    // Reutilize o expansor por essa assinatura em vez de reconstruir o mesmo
    // conjunto e closure a cada clique no fallback.
    private data class DragonBloodedXpProfile(
        val aspecto: String,
        val favorecidas: List<String>
    )
    private val dragonBloodedXpExpanders = HashMap<DragonBloodedXpProfile, com.example.data.EncounterXpExpander>()

    private fun dragonBloodedXpExpander(npc: NpcEncontro): com.example.data.EncounterXpExpander {
        val profile = DragonBloodedXpProfile(npc.casta, npc.habilidadesFavorecidas)
        return dragonBloodedXpExpanders.getOrPut(profile) {
            com.example.data.DragonBloodedEncounterGenerator
                .criarExpansorXpComCatalogoConvertido(dragonBloodedXpCatalog, npc)
        }
    }

    // Trilha de vitalidade: EncounterNpcVitalityActions
    private val vitality = EncounterNpcVitalityActions(state, repository, removerDaIniciativa, scope)

    /**
     * Cancela gerações pendentes e limpa os NPCs de encontro. A operação é
     * usada pelo comando "Novo" para impedir que trabalho assíncrono iniciado
     * na campanha anterior ressuscite NPCs depois da limpeza.
     */
    fun limpar() {
        limparRecursosTransitórios()
        state.value = emptyList()
        repository.salvarNpcsEncontro(emptyList())
    }

    /**
     * Libera recursos transitórios quando o dono do ciclo de vida deixa de
     * existir. Não altera o estado persistido dos NPCs: apenas cancela
     * trabalhos assíncronos e descarta o cache derivado do roadmap.
     *
     * A separação em um método próprio permite que [SheetViewModel.onCleared]
     * execute a limpeza mesmo quando a UI nunca passou pelo comando "Novo".
     */
    fun dispose() {
        limparRecursosTransitórios()
    }

    private fun limparRecursosTransitórios() {
        // Invalida primeiro qualquer trabalho derivado que esteja entre
        // criação e registro no conjunto de Jobs.
        backgroundEpoch++
        // ConcurrentHashMap/KeySet permitem iteração weakly-consistent durante
        // cancelamento. Não precisamos materializar cópias apenas para cancelar
        // os Jobs; eliminar essas listas temporárias reduz alocação no comando
        // de limpeza, especialmente quando há muitas gerações/reorganizações.
        geracaoJobs.forEach { it.cancel() }
        geracaoJobs.clear()
        vitality.dispose()
        backgroundJobs.forEach { it.cancel() }
        backgroundJobs.clear()
        roadmapPrefetchAtivos.clear()
        roadmapPrefetchJobs.clear()
        roadmapCache.clear()
        // Os expansores DB carregam caches derivados do perfil e do catálogo.
        // Novo/dispose encerra a janela em que esses dados podem ser reutilizados;
        // mantê-los aqui só prolongaria a retenção de estados de campanhas antigas.
        dragonBloodedXpExpanders.clear()
    }

    fun gerarSolar(
        nomeManual: String,
        arquetipo: ArquetipoEncontro,
        culturaNome: CulturaNome?,
        generoNome: GeneroNome?,
        onResultado: (NpcEncontro?) -> Unit,
        focoPersonalizado: String? = null,
        customizacao: com.example.data.EncounterCustomization? = null
    ) = gerarComTratamento(onResultado) {
        EncounterGenerator.gerarSolar(
            nomeManual = nomeManual,
            arquetipo = arquetipo,
            encantosSolares = solarCatalog.definitions,
            feiticos = feiticariaCatalog.definitions,
            culturaNome = culturaNome,
            generoNome = generoNome,
            meritosCatalogo = meritosCatalog.definitions,
            focoPersonalizado = focoPersonalizado,
            customizacao = customizacao,
            estilosArtesMarciais = martialArtsCatalog?.definitions.orEmpty()
        )
    }

    fun gerarSangueDeDragao(
        nomeManual: String,
        arquetipo: ArquetipoEncontro,
        origemNome: OrigemNomeSangueDeDragao,
        generoNome: GeneroNome?,
        onResultado: (NpcEncontro?) -> Unit,
        focoPersonalizado: String? = null,
        customizacao: com.example.data.EncounterCustomization? = null
    ) = gerarComTratamento(onResultado) {
        EncounterGenerator.gerarSangueDeDragao(
            nomeManual,
            arquetipo,
            dragonBloodedCatalog.definitions,
            feiticariaCatalog.definitions,
            origemNome,
            generoNome,
            meritosCatalogo = meritosCatalog.definitions,
            focoPersonalizado = focoPersonalizado,
            customizacao = customizacao,
            estilosArtesMarciais = martialArtsCatalog?.definitions.orEmpty()
        )
    }

    fun gerarLunar(
        nomeManual: String,
        arquetipo: ArquetipoEncontro,
        generoNome: GeneroNome?,
        onResultado: (NpcEncontro?) -> Unit,
        focoPersonalizado: String? = null,
        customizacao: com.example.data.EncounterCustomization? = null
    ) = gerarComTratamento(onResultado) {
        EncounterGenerator.gerarLunar(
            nomeManual,
            arquetipo,
            lunarCatalog.definitions,
            feiticos = feiticariaCatalog.definitions,
            generoNome = generoNome,
            meritosCatalogo = meritosCatalog.definitions,
            focoPersonalizado = focoPersonalizado,
            customizacao = customizacao,
            estilosArtesMarciais = martialArtsCatalog?.definitions.orEmpty()
        )
    }

    fun remove(id: String) {
        // PERFORMANCE: a versão anterior fazia uma leitura completa do estado
        // para descobrir se o id existia e depois uma segunda passagem dentro
        // de update. Uma única mutação atômica resolve as duas operações e
        // evita o segundo percurso da lista.
        var novoEstado: List<NpcEncontro>? = null
        state.update { lista ->
            val indice = lista.indexOfFirst { it.id == id }
            if (indice < 0) return@update lista
            val atualizado = lista.toMutableList().also { it.removeAt(indice) }
            novoEstado = atualizado
            atualizado
        }
        // Um NPC removido da Aba 11 não pode permanecer como combatente órfão
        // na Aba 12. A morte já usa este mesmo sincronismo; a remoção manual
        // precisa obedecer à mesma invariável.
        val estadoAtualizado = novoEstado ?: return
        roadmapCache.remove(id)
        roadmapPrefetchJobs.remove(id)?.cancel()
        roadmapPrefetchAtivos.remove(id)
        vitality.remove(id)
        removerDaIniciativa(id)
        // Persiste exatamente a lista produzida pela mutação.
        repository.salvarNpcsEncontro(estadoAtualizado)
    }

    fun feiticosDisponiveis(npcId: String): List<com.example.data.FeiticoDefinition> =
        state.value.firstOrNull { it.id == npcId }
            ?.let { com.example.data.EncounterNpcSpellManagement.disponiveis(it, feiticariaCatalog) }
            .orEmpty()

    fun atualizarFeitico(npcId: String, def: com.example.data.FeiticoDefinition, adicionar: Boolean) {
        var novoEstado: List<NpcEncontro>? = null
        state.update { lista ->
            val indice = lista.indexOfFirst { it.id == npcId }
            if (indice < 0) return@update lista
            val atual = lista[indice]
            val novo = com.example.data.EncounterNpcSpellManagement.atualizarInicial(
                atual, def, adicionar
            )
            if (novo == atual) return@update lista
            val posProcessado = EncounterMutationPipeline.recalcular(novo)
            val atualizada = lista.toMutableList()
            atualizada[indice] = posProcessado
            novoEstado = atualizada
            atualizada
        }
        val publicado = novoEstado ?: return
        roadmapCache.remove(npcId)
        repository.salvarNpcsEncontro(publicado)
    }

    fun removeExtraHealthBox(npcId: String, boxId: String) =
        vitality.removeExtraHealthBox(npcId, boxId)

    // APPROVED CONCURRENCY REFACTOR
    // Aumentar/reduzir XP pode ser disparado por toques sucessivos. Cada
    // operação precisa consumir o estado produzido pela operação anterior;
    // sem serialização, duas expansões concorrentes podiam partir do mesmo
    // snapshot e uma delas sobrescrever a outra. A trava é exclusiva para
    // mutações de XP e não bloqueia geração, combate ou reorganização de
    // vitalidade.
    private val xpMutationMutex = Mutex()
    private val roadmapMetrics = com.example.data.EncounterProgressionMetrics()
    private val performanceMetrics = com.example.data.EncounterPerformanceMetrics()
    private val roadmapCache = com.example.data.EncounterProgressionCache(metrics = roadmapMetrics)
    private val roadmapPrefetchAtivos = ConcurrentHashMap.newKeySet<String>()
    private val roadmapPrefetchJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val backgroundJobs = ConcurrentHashMap.newKeySet<kotlinx.coroutines.Job>()
    @Volatile private var backgroundEpoch = 0L

    // APPROVED CONCURRENCY REFACTOR
    // Gerações e mutações de XP podem trabalhar em paralelo quando são
    // independentes, mas todas participam do mesmo contador de operações
    // assíncronas. Assim, uma operação que termina não pode desligar [loading]
    // enquanto outra ainda está ativa. A serialização do domínio de XP continua
    // sendo responsabilidade exclusiva de [xpMutationMutex].
    private var operacoesAtivas = 0

    // APPROVED CONCURRENCY REFACTOR
    // O contador é simples porque todas as alterações ocorrem sob o mesmo
    // lock. O par "contador + loading" precisa ser observado como uma única
    // transação. Sem este lock, uma operação A pode
    // decrementar para zero, uma operação B pode incrementar novamente e,
    // antes de B publicar seu estado, A ainda pode escrever loading=false.
    // Isso cria uma janela real em que a UI informa que não há operações
    // ativas enquanto uma geração ainda está executando.
    private val operacoesAtivasLock = Any()

    // APPROVED CONCURRENCY REFACTOR
    // Gerações são canceláveis em bloco. Isso impede que uma geração iniciada
    // antes de "Novo" termine depois e reinsira um NPC na campanha recém-limpa.
    private val geracaoJobs = ConcurrentHashMap.newKeySet<kotlinx.coroutines.Job>()

    private fun iniciarOperacaoAssincrona() {
        synchronized(operacoesAtivasLock) {
            operacoesAtivas++
            loading.value = true
        }
    }

    private fun finalizarOperacaoAssincrona() {
        synchronized(operacoesAtivasLock) {
            val restante = --operacoesAtivas
            loading.value = restante > 0
        }
    }

    fun cycleHealthDamage(npcId: String, boxId: String) =
        vitality.cycleHealthDamage(npcId, boxId)

    fun clearHealthDamage(npcId: String) =
        vitality.clearHealthDamage(npcId)

    fun reduzirDefesasPorAtaques(npcIds: Collection<String>) {
        if (npcIds.isEmpty()) return
        // Cada ligação conta individualmente: se o mesmo NPC for alvo de
        // duas ligações no mesmo [Ataque], a penalidade soma 2, não 1.
        // Bug real encontrado aqui: a versão anterior usava .toSet(),
        // colapsando qualquer duplicata em uma única redução.
        // Fast path: a chamada mais comum envolve um único alvo; nesse caso
        // não há motivo para materializar o mapa de contagem.
        if (npcIds.size == 1) {
            val npcId = npcIds.first()
            var novoEstado: List<NpcEncontro>? = null
            state.update { list ->
                val indice = list.indexOfFirst { it.id == npcId }
                if (indice < 0) return@update list
                val npc = list[indice]
                val novaLista = ArrayList(list)
                novaLista[indice] = npc.copy(
                    penalidadeAtaquesDefesa = npc.penalidadeAtaquesDefesa + 1
                )
                novoEstado = novaLista
                novaLista
            }
            novoEstado?.let(repository::salvarNpcsEncontro)
            return
        }
        val contagemPorNpc = npcIds.groupingBy { it }.eachCount()
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            // Copy-on-write: ataques contra NPCs inexistentes são um caminho
            // válido e frequente durante sincronizações, mas não devem criar
            // uma cópia inteira da lista apenas para descartá-la depois.
            var novaLista: ArrayList<NpcEncontro>? = null
            for (indice in list.indices) {
                val npc = list[indice]
                val vezes = contagemPorNpc[npc.id] ?: continue
                if (vezes <= 0) continue
                if (novaLista == null) {
                    novaLista = ArrayList(list)
                }
                novaLista[indice] = npc.copy(
                    penalidadeAtaquesDefesa = npc.penalidadeAtaquesDefesa + vezes
                )
            }
            if (novaLista == null) return@update list
            novoEstado = novaLista
            novaLista
        }
        // PERFORMANCE: nenhum NPC correspondente significa nenhuma escrita.
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    /** Substitui o NPC [npcId] pelos dados carregados de um arquivo salvo
     * (pedido explícito do usuário — botão [Carregar]). Mantém o mesmo id,
     * pra não perder a aba/seleção atual. */
    fun carregarDeArquivo(npcId: String, carregado: com.example.model.NpcEncontro) {
        // O carregamento só é válido para um NPC existente. Evita sincronizar
        // uma entidade inexistente na Aba 12 e evita uma persistência inútil.
        val atualizado = EncounterMutationPipeline.recalcular(
            carregado.copy(id = npcId)
        )
        // PERFORMANCE: uma única atualização atômica resolve existência,
        // comparação idempotente e substituição; não há segunda busca linear.
        val novoEstado = substituirNpcNoEstado(npcId, atualizado) ?: return
        // O arquivo carregado pode alterar qualquer entrada da progressão.
        // Roadmap é cache derivado e nunca pode sobreviver à substituição
        // integral da ficha, mesmo quando fingerprints futuros ganharem campos.
        roadmapCache.remove(npcId)
        // Se o NPC já participa da Aba 12, o carregamento precisa manter nome e
        // iniciativa sincronizados. Caso contrário, as duas abas passam a
        // representar versões diferentes do mesmo combatente.
        atualizarNaIniciativa(atualizado)
        // CORREÇÃO FUNCIONAL — o botão Carregar atualizava apenas o StateFlow.
        // Ao reiniciar a aplicação, o NPC voltava ao estado anterior porque a
        // substituição nunca era persistida. O carregamento precisa ter a mesma
        // garantia de persistência das demais mutações do domínio.
        repository.salvarNpcsEncontro(novoEstado)
    }

    // Editor de equipamento do NPC (Aba 11) — pedido explícito do usuário:
    // ao tocar na linha "Equipamento" do card, poder trocar a Arma/Armadura
    // já atribuídas por qualquer uma cadastrada no catálogo. Mesmo padrão
    // de adjustInitiative: localizar por id, copiar com o novo valor,
    // persistir e sincronizar com a Iniciativas (a Defesa/Absorção mudam
    // com o equipamento, então o card de Iniciativas precisa saber).
    fun atualizarArmaNpc(npcId: String, novaArma: com.example.model.ArmaEncontro) {
        var atualizado: NpcEncontro? = null
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            val indice = list.indexOfFirst { it.id == npcId }
            if (indice < 0) return@update list
            val atual = list[indice]
            val novo = EncounterMutationPipeline.recalcular(
                atual.copy(arma = novaArma)
            )
            val novaLista = ArrayList(list)
            novaLista[indice] = novo
            atualizado = novo
            novoEstado = novaLista
            novaLista
        }
        if (atualizado == null) return
        atualizado.let(atualizarNaIniciativa)
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    fun atualizarArmaduraNpc(npcId: String, novaArmadura: com.example.model.ArmaduraEncontro) {
        var atualizado: NpcEncontro? = null
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            val indice = list.indexOfFirst { it.id == npcId }
            if (indice < 0) return@update list
            val atual = list[indice]
            val novo = EncounterMutationPipeline.recalcular(
                atual.copy(armadura = novaArmadura)
            )
            val novaLista = ArrayList(list)
            novaLista[indice] = novo
            atualizado = novo
            novoEstado = novaLista
            novaLista
        }
        if (atualizado == null) return
        atualizado.let(atualizarNaIniciativa)
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    fun limparPenalidadesDefesa(npcIds: Collection<String>) {
        if (npcIds.isEmpty()) return
        val ids = if (npcIds is Set<String>) npcIds else npcIds.toHashSet()
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            var novaLista: ArrayList<NpcEncontro>? = null
            for (indice in list.indices) {
                val npc = list[indice]
                if (npc.id !in ids || (npc.penalidadeClashDefesa == 0 && npc.penalidadeAtaquesDefesa == 0)) continue
                if (novaLista == null) novaLista = ArrayList(list)
                novaLista[indice] = npc.copy(
                    penalidadeClashDefesa = 0,
                    penalidadeAtaquesDefesa = 0
                )
            }
            if (novaLista == null) return@update list
            novoEstado = novaLista
            novaLista
        }
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    fun adjustInitiative(npcId: String, delta: Int) {
        if (delta == 0) return
        var atualizado: NpcEncontro? = null
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            val indice = list.indexOfFirst { it.id == npcId }
            if (indice < 0) return@update list
            val atual = list[indice]
            val novo = atual.copy(iniciativaAtual = atual.iniciativaAtual + delta)
            val novaLista = ArrayList(list)
            novaLista[indice] = novo
            atualizado = novo
            novoEstado = novaLista
            novaLista
        }
        if (atualizado == null) return
        atualizado.let(atualizarNaIniciativa)
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    fun aplicarPenalidadeClash(npcId: String) {
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            val indice = list.indexOfFirst { it.id == npcId }
            if (indice < 0 || list[indice].penalidadeClashDefesa == 2) return@update list
            val novaLista = ArrayList(list)
            novaLista[indice] = list[indice].copy(penalidadeClashDefesa = 2)
            novoEstado = novaLista
            novaLista
        }
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    /**
     * Snapshot das métricas transitórias do roadmap para diagnóstico e medição.
     * Ler estas métricas nunca altera o fluxo de XP.
     */
    fun obterMetricasRoadmap(): com.example.data.EncounterProgressionMetrics.Snapshot =
        roadmapMetrics.snapshot()

    /** Baseline transitório para profiling; não altera geração nem progressão. */
    fun obterMetricasPerformance(): com.example.data.EncounterPerformanceMetrics.Snapshot =
        performanceMetrics.snapshot()

    private fun expandirMarcialComObservabilidade(current: NpcEncontro): NpcEncontro? {
        val resultado = martialArtsCatalog?.definitions?.let {
            com.example.data.EncounterMartialArtsProgression.expandirComResultado(current, it)
        } ?: return null
        val atualizado = resultado.npc
        val estilos = buildSet {
            if (atualizado.estiloArtesMarciais.isNotBlank()) add(atualizado.estiloArtesMarciais)
            addAll(atualizado.estilosArtesMarciaisAdicionais.filter(String::isNotBlank))
        }
        com.example.data.EncounterBuildObservability.publishFinalBuild(
            selected = atualizado.charms.map { it.nome to it.habilidadeVinculada },
            mainCategory = if (atualizado.tipoExaltado == TipoExaltadoEncontro.LUNAR)
                null
            else atualizado.habilidadePrincipal,
            combatCategory = atualizado.habilidadePrincipal,
            martialStyleCategories = estilos,
            newlyOpenedMartialStyle = resultado.estiloSelecionado.takeIf { resultado.abriuNovoEstilo },
            martialPrerequisiteCharms = if (resultado.compraDeRequisito)
                setOf(atualizado.charms.last().nome)
            else emptySet()
        )
        return atualizado
    }

    fun expandirEncantos(npcId: String) {
        iniciarOperacaoAssincrona()
        error.value = null
        scope.launch {
            try {
                val xpInicio = System.nanoTime()
                var xpExecutado = false
                xpMutationMutex.withLock {
                    while (true) {
                    val currentOriginal = state.value.firstOrNull { it.id == npcId } ?: return@withLock
                    // Se um prefetch especulativo do estado anterior ainda
                    // estiver calculando, este clique já o tornou obsoleto.
                    // Cancelá-lo evita disputar CPU com a expansão real.
                    roadmapPrefetchJobs.remove(npcId)?.cancel()
                    roadmapPrefetchAtivos.remove(npcId)
                    // O clique em +XP concede o primeiro lote de 5 XP. O bloqueio
                    // do Feitiço inicial é monotônico e antecede a expansão para
                    // não existir janela concorrente de edição gratuita.
                    val current = if (currentOriginal.primeiroXpRecebido) {
                        currentOriginal
                    } else {
                        currentOriginal.copy(primeiroXpRecebido = true)
                    }
                    xpExecutado = true
                    val posProcessado = withContext(Dispatchers.Default) {
                        // Estilo principal é uma rota explícita e tem precedência
                        // sobre o roadmap genérico enquanto possuir descendente legal.
                        val marcial = expandirMarcialComObservabilidade(current)
                        var precisaRecalcularPipeline = true
                        val expanded = if (marcial != null) {
                            roadmapCache.remove(npcId)
                            marcial
                        } else {
                            val roadmap = roadmapCache.get(npcId)
                            val passo = EncounterProgressionRoadmapService.proximo(
                                current, roadmap, roadmapCatalogFingerprint
                            )
                            if (passo != null && roadmap != null) {
                                EncounterExperienceService.aplicarPassoRoadmapJaValidado(
                                    current, passo, roadmapCatalogFingerprint
                                )?.let { planejado ->
                                    val avancado = EncounterProgressionRoadmapService.avancar(roadmap)
                                    roadmapCache.put(npcId, avancado)
                                    // O aplicador do roadmap já recalcula derivados e
                                    // alertas. Repetir o pipeline inteiro aqui fazia
                                    // o mesmo trabalho duas vezes em cada cache hit.
                                    precisaRecalcularPipeline = false
                                    planejado
                                } ?: expandirXpSemRoadmap(current, tentarMarcial = false)
                            } else {
                                expandirXpSemRoadmap(current, tentarMarcial = false)
                            }
                        }
                        if (precisaRecalcularPipeline) {
                            EncounterMutationPipeline.recalcular(expanded)
                        } else {
                            expanded
                        }
                    }
                    val estadoPublicado = substituirNpcSeSnapshotAtual(
                        npcId = npcId,
                        snapshotEsperado = currentOriginal,
                        novo = posProcessado
                    )
                    if (estadoPublicado == null) {
                        // Outra mutação venceu enquanto o cálculo de XP estava fora
                        // da main thread. Nunca publique resultado calculado sobre um
                        // snapshot antigo: invalide o plano derivado e refaça o mesmo
                        // clique a partir do estado mais novo.
                        roadmapCache.remove(npcId)
                        if (state.value.none { it.id == npcId }) return@withLock
                        continue
                    }
                    repository.salvarNpcsEncontro(estadoPublicado)
                    // O clique atual já foi resolvido e publicado. Se não há
                    // janela válida restante, prepare poucos passos para os
                    // próximos cliques fora do caminho crítico da UI.
                    val cachedRoadmap = roadmapCache.peek(npcId)
                    if (cachedRoadmap == null || cachedRoadmap.proximoPasso >= cachedRoadmap.passos.size) {
                            if (roadmapPrefetchAtivos.add(npcId)) {
                                roadmapMetrics.recordPrefetchScheduled()
                                val epoch = backgroundEpoch
                                lateinit var prefetchJob: kotlinx.coroutines.Job
                                prefetchJob = scope.launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
                                    val inicioPrefetch = System.nanoTime()
                                    try {
                                        if (epoch != backgroundEpoch) return@launch
                                        val snapshot = state.value.firstOrNull { it.id == npcId }
                                        if (snapshot != posProcessado) {
                                            roadmapMetrics.recordPrefetchInvalidated()
                                            return@launch
                                        }
                                        // O snapshot pode ter sido invalidado entre a leitura e
                                        // o início do cálculo; não preparar expansores obsoletos.
                                        prefetchJob.ensureActive()
                                        if (epoch != backgroundEpoch || roadmapPrefetchJobs[npcId] !== prefetchJob) {
                                            roadmapMetrics.recordPrefetchInvalidated()
                                            return@launch
                                        }
                                        val roadmap = EncounterProgressionRoadmapService.construir(
                                            npc = posProcessado,
                                            solarCatalogo = preparedEncounterCatalog.solares,
                                            dragonCatalogo = preparedEncounterCatalog.sangueDeDragao,
                                            lunarCatalogo = preparedEncounterCatalog.lunares,
                                            catalogFingerprintPrecalculado = roadmapCatalogFingerprint,
                                            verificarCancelamento = { prefetchJob.ensureActive() }
                                        )
                                        prefetchJob.ensureActive()
                                        if (epoch != backgroundEpoch || roadmapPrefetchJobs[npcId] !== prefetchJob) {
                                            roadmapMetrics.recordPrefetchInvalidated()
                                        } else if (state.value.firstOrNull { it.id == npcId } == posProcessado) {
                                            roadmapCache.put(npcId, roadmap)
                                            roadmapMetrics.recordPrefetchCompleted()
                                        } else {
                                            roadmapMetrics.recordPrefetchDiscarded()
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        roadmapMetrics.recordPrefetchFailed()
                                    } finally {
                                        roadmapMetrics.recordPrefetchBuildNanos(System.nanoTime() - inicioPrefetch)
                                        if (roadmapPrefetchJobs.remove(npcId, prefetchJob)) {
                                            roadmapPrefetchAtivos.remove(npcId)
                                        }
                                        backgroundJobs.remove(prefetchJob)
                                    }
                                }
                                roadmapPrefetchJobs[npcId] = prefetchJob
                                backgroundJobs.add(prefetchJob)
                                prefetchJob.start()
                            } else {
                                roadmapMetrics.recordPrefetchSkippedActive()
                            }
                        }
                        // A auditoria canônica permanece disponível para testes e
                        // diagnósticos explícitos, mas não roda automaticamente
                        // após cada +XP. Ela não participa da decisão de compra e
                        // competia por CPU com o prefetch e com cliques seguintes.
                    break
                    }
                }
                if (xpExecutado) performanceMetrics.recordXp(System.nanoTime() - xpInicio)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = "Não foi possível aumentar a experiência: ${e.message ?: "erro desconhecido"}."
            } finally {
                finalizarOperacaoAssincrona()
            }
        }
    }


    private fun expandirXpSemRoadmap(current: NpcEncontro, tentarMarcial: Boolean = true): NpcEncontro {
        // A rota marcial principal tem continuidade própria antes de abrir
        // novas árvores comuns. Briga continua coexistindo, mas não participa
        // do score da árvore marcial.
        val marcial = if (tentarMarcial) expandirMarcialComObservabilidade(current) else null
        if (marcial != null) {
            roadmapMetrics.recordRoadmapFallback()
            return marcial
        }
        val expandido = when (current.tipoExaltado) {
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO ->
                dragonBloodedXpExpander(current).expand(current).npcResultante
            TipoExaltadoEncontro.LUNAR ->
                EncounterGenerator.expandirEncantosPorExperienciaLunar(current, lunarCatalog.definitions)
            TipoExaltadoEncontro.SOLAR ->
                solarXpExpander.expand(current).npcResultante
        }
        // Sem roadmap válido, o +XP executa somente a expansão normal.
        // Não há planejamento especulativo em background: isso preserva bateria
        // e mantém o estado atual do NPC como única fonte de verdade.
        roadmapMetrics.recordRoadmapFallback()
        return expandido
    }

    fun reduzirExperiencia(npcId: String) {
        iniciarOperacaoAssincrona()
        error.value = null
        scope.launch {
            try {
                xpMutationMutex.withLock {
                    while (true) {
                        val current = state.value.firstOrNull { it.id == npcId } ?: return@withLock
                        roadmapPrefetchJobs.remove(npcId)?.cancel()
                        roadmapPrefetchAtivos.remove(npcId)
                        roadmapCache.remove(npcId)
                        val posProcessado = withContext(Dispatchers.Default) {
                            val reduzido = EncounterGenerator.reduzirExperiencia(current)
                            // A recalculação deriva defesas/absorção/recursos do NPC
                            // inteiro e deve acompanhar a redução de XP fora da UI.
                            EncounterMutationPipeline.recalcular(reduzido)
                        }
                        val estadoPublicado = substituirNpcSeSnapshotAtual(
                            npcId = npcId,
                            snapshotEsperado = current,
                            novo = posProcessado
                        )
                        if (estadoPublicado == null) {
                            // Mesma garantia do +XP: redução calculada sobre estado
                            // antigo nunca pode apagar uma mutação concorrente.
                            if (state.value.none { it.id == npcId }) return@withLock
                            continue
                        }
                        repository.salvarNpcsEncontro(estadoPublicado)
                        break
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = "Não foi possível reduzir a experiência: ${e.message ?: "erro desconhecido"}."
            } finally {
                finalizarOperacaoAssincrona()
            }
        }
    }

    fun toggleForcaDeVontade(npcId: String, indice: Int) {
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            val posicao = list.indexOfFirst { it.id == npcId }
            if (posicao < 0) return@update list
            val npc = list[posicao]
            val novosUsados = com.example.model.WillpowerTrackLogic.toggle(
                usados = npc.forcaDeVontadeUsados,
                valor = npc.forcaDeVontade,
                indice = indice
            )
            if (novosUsados == npc.forcaDeVontadeUsados) return@update list
            val novaLista = ArrayList(list)
            novaLista[posicao] = npc.copy(forcaDeVontadeUsados = novosUsados)
            novoEstado = novaLista
            novaLista
        }
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    // Espelha exatamente o comportamento de updateForcaVontadeBase() da Aba 5
    // (CombatActions.kt): valor sempre entre 5 e 10, e os pontos já gastos
    // acima do novo teto são descartados. Só é de fato acionado quando
    // planilhaConcluida for false na trilha (hoje sempre true pra NPCs, mas
    // mantido para paridade exata de comportamento com a Aba 5).
    fun updateForcaDeVontadeBase(npcId: String, novoValor: Int) {
        var novoEstado: List<NpcEncontro>? = null
        state.update { list ->
            val posicao = list.indexOfFirst { it.id == npcId }
            if (posicao < 0) return@update list
            val npc = list[posicao]
            val novaBase = novoValor.coerceIn(5, 10)
            if (novaBase == npc.forcaDeVontade) return@update list
            val novosUsados = if (novaBase >= npc.forcaDeVontade) {
                npc.forcaDeVontadeUsados
            } else {
                npc.forcaDeVontadeUsados.filterTo(linkedSetOf()) { it <= novaBase }
            }
            val novaLista = ArrayList(list)
            novaLista[posicao] = npc.copy(
                forcaDeVontade = novaBase,
                forcaDeVontadeUsados = novosUsados
            )
            novoEstado = novaLista
            novaLista
        }
        novoEstado?.let(repository::salvarNpcsEncontro)
    }

    /**
     * Substitui um único NPC preservando a semântica do StateFlow sem executar
     * uma transformação sobre todos os elementos da lista.
     *
     * A comparação de igualdade também torna a operação idempotente: se o
     * gerador/carregamento produzir exatamente o mesmo NPC, nenhuma lista nova
     * é materializada e nenhum write de persistência é disparado.
     */
    /**
     * Publicação otimista para mutações calculadas fora da thread de estado.
     *
     * Só substitui o NPC se ele ainda for exatamente o snapshot usado pelo
     * cálculo. Se vitalidade, equipamento, feitiço ou qualquer outra action
     * tiver vencido durante o cálculo, o chamador recebe null e deve recalcular
     * a partir do estado mais novo em vez de sobrescrever a mutação concorrente.
     */
    private fun substituirNpcSeSnapshotAtual(
        npcId: String,
        snapshotEsperado: NpcEncontro,
        novo: NpcEncontro
    ): List<NpcEncontro>? {
        var publicado: List<NpcEncontro>? = null
        state.update { lista ->
            val indice = lista.indexOfFirst { it.id == npcId }
            if (indice < 0 || lista[indice] != snapshotEsperado) return@update lista
            if (lista[indice] == novo) {
                publicado = lista
                return@update lista
            }
            val novaLista = ArrayList(lista)
            novaLista[indice] = novo
            publicado = novaLista
            novaLista
        }
        return publicado
    }

    private fun substituirNpcNoEstado(npcId: String, novo: NpcEncontro): List<NpcEncontro>? {
        var novoEstado: List<NpcEncontro>? = null
        state.update { lista ->
            val indice = lista.indexOfFirst { it.id == npcId }
            if (indice < 0 || lista[indice] == novo) return@update lista
            val novaLista = ArrayList(lista)
            novaLista[indice] = novo
            novoEstado = novaLista
            novaLista
        }
        return novoEstado
    }

    private fun gerarComTratamento(onResultado: (NpcEncontro?) -> Unit, block: suspend () -> NpcEncontro) {
        iniciarOperacaoAssincrona()
        error.value = null
        lateinit var job: kotlinx.coroutines.Job
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val generationInicio = System.nanoTime()
                var generationCoreNanos = 0L
                var generationQualityNanos = 0L
                val generated = withContext(Dispatchers.Default) {
                    // O roadmap é um cache derivado e não precisa ser calculado
                    // durante a criação do NPC. A geração deve entregar apenas
                    // o estado de jogo; o primeiro clique de +XP usa o resultado
                    // da expansão real e, em seguida, constrói o cache dos passos
                    // seguintes. Isso evita deslocar o custo do planejamento
                    // pesado para toda criação de NPC, especialmente em geração
                    // em massa.
                    val coreInicio = System.nanoTime()
                    val npc = com.example.data.EncounterNpcSpellManagement.garantirInicialNaCriacao(
                        block(), feiticariaCatalog.definitions, kotlin.random.Random.Default
                    )
                    generationCoreNanos = System.nanoTime() - coreInicio

                    // Portão único pós-construção: mede separadamente a auditoria
                    // final para que profiling não atribua seu custo ao gerador.
                    val qualityInicio = System.nanoTime()
                    val quality = com.example.data.EncounterFinalBuildQuality.evaluate(
                        npc = npc,
                        catalog = preparedEncounterCatalog
                    )
                    com.example.data.EncounterBuildObservability.publishFinalQuality(quality)
                    generationQualityNanos = System.nanoTime() - qualityInicio
                    npc
                }
                performanceMetrics.recordGeneration(
                    nanos = System.nanoTime() - generationInicio,
                    coreNanos = generationCoreNanos,
                    qualityNanos = generationQualityNanos
                )
                var novoEstado: List<NpcEncontro>? = null
                state.update {
                    val novaLista = it + generated
                    novoEstado = novaLista
                    novaLista
                }
                novoEstado?.let(repository::salvarNpcsEncontro)
                onResultado(generated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = "Não foi possível gerar o NPC: ${e.message ?: "erro desconhecido"}."
                onResultado(null)
            } finally {
                geracaoJobs.remove(job)
                finalizarOperacaoAssincrona()
            }
        }
        geracaoJobs.add(job)
        job.start()
    }
}
