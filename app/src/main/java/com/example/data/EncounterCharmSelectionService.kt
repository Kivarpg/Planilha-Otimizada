package com.example.data

import com.example.model.ExaltedConstants
import com.example.model.NOME_CORPO_DE_TOURO
import java.util.concurrent.ConcurrentHashMap

/** Catalog indexing, prerequisite checks and deterministic initial Charm selection. */
object EncounterCharmSelectionService {

    // Pré-requisitos são texto imutável do catálogo. Durante uma única geração,
    // a busca de rotas consulta a mesma definição centenas/milhares de vezes.
    // Parsear uma vez por texto elimina Regex/splits repetidos sem alterar a
    // avaliação dependente do estado atual da ficha.
    private val requisitosParseadosCache = ConcurrentHashMap<String, List<EncounterRequirement>>()

    private fun requisitosParseados(texto: String): List<EncounterRequirement> =
        requisitosParseadosCache.computeIfAbsent(texto) { EncounterPrerequisiteParser.parse(it) }

    // --- Pré-agrupa e pré-ordena por Essência mínima. O catálogo é imutável
    // durante a vida do ViewModel, então múltiplos NPCs podem compartilhar o
    // mesmo índice em vez de reconstruí-lo a cada seleção. ---
    internal data class CatalogMetadata(
        val porHabilidade: Map<String, List<EncantoSolarDefinition>>,
        val porHabilidadeParaMinimoPrincipal: Map<String, List<EncantoSolarDefinition>>,
        val habilidades: Set<String>,
        val corpoDeTouro: EncantoSolarDefinition?,
        val excelencias: List<EncantoSolarDefinition>,
        val excelenciasPorHabilidade: Map<String, List<EncantoSolarDefinition>>,
        val excelenciasPreparadas: EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition>,
        val porNome: Map<String, EncantoSolarDefinition>,
        val porHabilidadeNormalizada: Map<String, List<EncantoSolarDefinition>>,
        val feiticariaTerrestre: EncantoSolarDefinition?,
        val semFeiticariaTerrestre: List<EncantoSolarDefinition>
    )

    private val catalogMetadataCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<List<EncantoSolarDefinition>, CatalogMetadata>()
        )

    internal fun catalogMetadata(catalogo: List<EncantoSolarDefinition>): CatalogMetadata =
        synchronized(catalogMetadataCache) {
            catalogMetadataCache[catalogo] ?: run {
                val porHabilidade = catalogo.groupBy { it.habilidade }
                    .mapValues { (_, lista) -> lista.sortedBy { it.minEssencia } }
                val porHabilidadeParaMinimoPrincipal = porHabilidade.mapValues { (_, lista) ->
                    lista.sortedWith(
                        compareBy<EncantoSolarDefinition> { it.minEssencia }
                            .thenBy { it.minHabilidade }
                            .thenBy { it.nome }
                    )
                }
                val excelencias = catalogo.filter {
                    it.palavrasChave.contains("Excelência", ignoreCase = true)
                }
                CatalogMetadata(
                    porHabilidade = porHabilidade,
                    porHabilidadeParaMinimoPrincipal = porHabilidadeParaMinimoPrincipal,
                    habilidades = porHabilidade.keys,
                    corpoDeTouro = catalogo.firstOrNull { it.nome == NOME_CORPO_DE_TOURO },
                    excelencias = excelencias,
                    excelenciasPorHabilidade = excelencias.groupBy { it.habilidade },
                    excelenciasPreparadas = EncounterCharmRouteOptimizer.prepareCatalog(
                        catalogoCompleto = excelencias,
                        nome = { it.nome },
                        categoria = { it.habilidade }
                    ),
                    porNome = catalogo.associateBy { it.nome },
                    porHabilidadeNormalizada = porHabilidade.entries.associate { (habilidade, defs) ->
                        habilidade.lowercase() to defs
                    },
                    feiticariaTerrestre = catalogo.firstOrNull {
                        it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE
                    },
                    semFeiticariaTerrestre = catalogo.filter {
                        it.nome != com.example.model.NOME_FEITICARIA_TERRESTRE
                    }
                ).also { catalogMetadataCache[catalogo] = it }
            }
        }

    fun agruparPorHabilidade(catalogo: List<EncantoSolarDefinition>): Map<String, List<EncantoSolarDefinition>> =
        catalogMetadata(catalogo).porHabilidade

    private val preparedRouteCatalogCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<
                List<EncantoSolarDefinition>,
                EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition>
            >()
        )

    private fun preparedRouteCatalog(
        catalogo: List<EncantoSolarDefinition>
    ): EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition> =
        synchronized(preparedRouteCatalogCache) {
            preparedRouteCatalogCache[catalogo]
                ?: EncounterCharmRouteOptimizer.prepareCatalog(
                    catalogoCompleto = catalogo,
                    nome = { it.nome },
                    categoria = { it.habilidade }
                ).also { preparedRouteCatalogCache[catalogo] = it }
        }

    /** Índice Lunar equivalente, preservando a ordem original do catálogo. */
    fun agruparPorAtributoLunar(catalogo: List<EncantoLunarDefinition>): Map<String, List<EncantoLunarDefinition>> =
        catalogo.groupBy { it.atributo }

    private data class CompletionCatalogIndex(
        val ordinalPorNome: Map<String, Int>,
        val desbloqueiosPorNome: Map<String, Int>
    )

    private val completionCatalogIndexCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<List<EncantoSolarDefinition>, CompletionCatalogIndex>()
        )

    private fun indiceCompletude(
        catalogo: List<EncantoSolarDefinition>
    ): CompletionCatalogIndex = synchronized(completionCatalogIndexCache) {
        completionCatalogIndexCache[catalogo] ?: CompletionCatalogIndex(
            ordinalPorNome = catalogo.mapIndexed { index, def -> def.nome to index }.toMap(),
            desbloqueiosPorNome = buildMap {
                catalogo.forEach { outro ->
                    outro.preRequisitos.split(",")
                        .asSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { requisito ->
                            val chave = requisito.lowercase()
                            put(chave, (get(chave) ?: 0) + 1)
                        }
                }
            }
        ).also { completionCatalogIndexCache[catalogo] = it }
    }

    private val HABILIDADES_CONHECIDAS = ExaltedConstants.ALL_25_ABILITIES.toSet()
    private val ATRIBUTOS_CONHECIDOS = (ExaltedConstants.PHYSICAL_ATTRIBUTES + ExaltedConstants.SOCIAL_ATTRIBUTES + ExaltedConstants.MENTAL_ATTRIBUTES).toSet()

    /**
     * Avalia requisitos compartilhados de Encantos.
     *
     * A regra de nível mínimo varia apenas na origem do nível (Habilidade para
     * Solar/Sangue de Dragão; Atributo para Lunar). O parsing e a contagem de
     * pré-requisitos, porém, são uma única implementação.
     */
    internal fun <D> elegivelGenerico(
        nivelAtual: Int,
        nivelRequerido: Int,
        minEssencia: Int,
        preRequisitos: String,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogoCompleto: List<D>,
        categoriaDoEncanto: (D) -> String,
        categoriaConhecida: (String) -> Boolean,
        nomeDoEncanto: (D) -> String,
        categoriaCorresponde: (D, String) -> Boolean = { def, categoria ->
            categoriaDoEncanto(def).equals(categoria, ignoreCase = true)
        },
        contagemCategoriaSelecionada: ((String) -> Int)? = null
    ): Boolean {
        if (nivelAtual < nivelRequerido) return false
        if (minEssencia > essencia) return false

        // Uma única gramática de pré-requisitos: o caminho legado mantém sua
        // API/callbacks, mas não possui mais parser próprio.
        fun satisfaz(requisito: EncounterRequirement): Boolean = when (requisito) {
            is EncounterRequirement.Charm -> requisito.name in nomesSelecionados
            is EncounterRequirement.CharmCount -> {
                val contagem = if (requisito.category.isBlank()) {
                    nomesSelecionados.size
                } else {
                    if (!categoriaConhecida(requisito.category)) return false
                    contagemCategoriaSelecionada?.invoke(requisito.category)
                        ?: catalogoCompleto.count {
                            categoriaCorresponde(it, requisito.category) &&
                                nomeDoEncanto(it) in nomesSelecionados
                        }
                }
                contagem >= requisito.minimum
            }
            is EncounterRequirement.AnyOf -> requisito.alternatives.any(::satisfaz)
            // Este adaptador recebe somente texto de pré-requisito; mínimos
            // de nível/Essência já foram avaliados acima.
            else -> true
        }
        return requisitosParseados(preRequisitos).all(::satisfaz)
    }


    fun elegivel(
        def: EncantoSolarDefinition,
        abilities: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogoCompleto: List<EncantoSolarDefinition> = emptyList(),
        contagemCategoriaSelecionada: ((String) -> Int)? = null
    ): Boolean = elegivelGenerico(
        nivelAtual = abilities[def.habilidade] ?: 0,
        nivelRequerido = def.minHabilidade,
        minEssencia = def.minEssencia,
        preRequisitos = def.preRequisitos,
        essencia = essencia,
        nomesSelecionados = nomesSelecionados,
        catalogoCompleto = catalogoCompleto,
        categoriaDoEncanto = { it.habilidade },
        categoriaConhecida = { it in HABILIDADES_CONHECIDAS },
        nomeDoEncanto = { it.nome },
        contagemCategoriaSelecionada = contagemCategoriaSelecionada
    )

    internal fun elegivelLunar(
        def: EncantoLunarDefinition,
        attributes: Map<String, Int>,
        essencia: Int,
        nomesSelecionados: Set<String>,
        catalogoCompleto: List<EncantoLunarDefinition> = emptyList(),
        contagemCategoriaSelecionada: ((String) -> Int)? = null,
        spiritTraits: Set<LunarSpiritTrait> = emptySet()
    ): Boolean = LunarCharmArchetypePolicy.eligibleRoutes(
        def = def,
        attributes = attributes,
        essencia = essencia,
        nomesSelecionados = nomesSelecionados,
        catalogo = catalogoCompleto,
        spiritTraits = spiritTraits,
        contagemCategoriaSelecionada = contagemCategoriaSelecionada
    ).isNotEmpty()


    /**
     * Aplica a cadeia de Feitiçaria do Círculo Terrestre como uma única operação.
     *
     * O projeto só é aplicado quando existe espaço para os quatro Encantos de
     * Ocultismo exigidos e para a própria Feitiçaria. Se a cadeia não puder ser
     * completada legalmente, a lista original é devolvida sem aquisições parciais.
     * Isso evita reservar/adicionar a Feitiçaria antes de seus pré-requisitos.
     */
    fun aplicarProjetoFeiticariaTerrestre(
        catalogo: List<EncantoSolarDefinition>,
        selecionados: List<EncantoSolarDefinition>,
        abilities: Map<String, Int>,
        essencia: Int,
        quantidadeTotal: Int,
        exigirProjeto: Boolean,
        minimoOcultismo: Int = EncounterGenerationRules.QUANTIDADE_OCULTISMO_FEITICARIA
    ): List<EncantoSolarDefinition> {
        if (!exigirProjeto) return selecionados
        if (selecionados.size >= quantidadeTotal &&
            selecionados.none { it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE }
        ) return selecionados

        val nomesOriginais = selecionados.map { it.nome }.toSet()
        // A busca recursiva abaixo reutiliza os mesmos índices em todos os níveis.
        // Capturar uma vez evita sincronização/WeakHashMap lookup por candidato.
        val metadata = catalogMetadata(catalogo)
        val feiticaria = metadata.feiticariaTerrestre ?: return selecionados

        val candidatosOcultismo = catalogo
            .asSequence()
            .filter { it.habilidade.equals(EncounterGenerationRules.HABILIDADE_OCULTISMO, ignoreCase = true) }
            // A própria Feitiçaria é o produto final do projeto. Ela não pode
            // ocupar uma das quatro vagas de preparação de Ocultismo, senão
            // a operação pode consumir a Feitiçaria durante a preparação e
            // terminar sem os quatro pré-requisitos distintos exigidos.
            .filter { it.nome != feiticaria.nome }
            .filter { it.nome !in nomesOriginais }
            .sortedWith(compareBy<EncantoSolarDefinition> { it.minEssencia }.thenBy { it.nome })
            .toList()

        val ocultismoExistente = selecionados.count {
            it.habilidade.equals(EncounterGenerationRules.HABILIDADE_OCULTISMO, ignoreCase = true)
        }
        val faltamOcultismo = (minimoOcultismo - ocultismoExistente).coerceAtLeast(0)
        if (selecionados.size + faltamOcultismo + 1 > quantidadeTotal) return selecionados

        // Os quatro Encantos de Ocultismo formam uma pequena cadeia de pré-
        // requisitos. Uma escolha gulosa pelo primeiro candidato de menor
        // Essência pode entrar em um beco sem saída mesmo quando outra
        // sequência legal existe no catálogo. Como o projeto tem no máximo
        // quatro aquisições preparatórias, resolvemos esta parte com busca
        // exata e limitada, em vez de depender da heurística da beam search.
        fun encontrarCadeiaOcultismo(
            projetoAtual: List<EncantoSolarDefinition>,
            nomesAtual: Set<String>,
            contagemAtual: Int,
            restantes: Int
        ): List<EncantoSolarDefinition>? {
            if (restantes <= 0) {
                val podeComprarFeiticaria = elegivel(
                    feiticaria,
                    abilities,
                    essencia,
                    nomesAtual,
                    catalogo,
                    contagemCategoriaSelecionada = { categoria ->
                        if (categoria.equals(EncounterGenerationRules.HABILIDADE_OCULTISMO, ignoreCase = true)) contagemAtual else 0
                    }
                )
                return if (podeComprarFeiticaria && feiticaria.nome !in nomesAtual) projetoAtual else null
            }

            // candidatosOcultismo já foi ordenado por Essência/nome antes
            // da DFS; preserve essa ordem e evite ordenar novamente em cada nó.
            for (candidato in candidatosOcultismo) {
                if (candidato.nome in nomesAtual) continue
                if (!elegivel(
                        candidato,
                        abilities,
                        essencia,
                        nomesAtual,
                        catalogo,
                        contagemCategoriaSelecionada = { categoria ->
                            if (categoria.equals(EncounterGenerationRules.HABILIDADE_OCULTISMO, ignoreCase = true)) {
                                contagemAtual
                            } else {
                                metadata.porHabilidadeNormalizada[categoria.lowercase()]
                                    ?.count { it.nome in nomesAtual }
                                    ?: 0
                            }
                        }
                    )
                ) continue
                val novoProjeto = projetoAtual + candidato
                val novosNomes = nomesAtual + candidato.nome
                val resultado = encontrarCadeiaOcultismo(
                    projetoAtual = novoProjeto,
                    nomesAtual = novosNomes,
                    contagemAtual = contagemAtual + 1,
                    restantes = restantes - 1
                )
                if (resultado != null) return resultado
            }
            return null
        }

        var projetoComOcultismo = encontrarCadeiaOcultismo(
            projetoAtual = selecionados,
            nomesAtual = nomesOriginais,
            contagemAtual = ocultismoExistente,
            restantes = faltamOcultismo
        )

        // Se o prefixo escolhido pela seleção comum bloqueou a cadeia de
        // Feitiçaria, não devolvemos silenciosamente as vagas reservadas sem
        // o projeto. Recomeçamos apenas a construção do projeto a partir de
        // um estado mínimo: preservamos as aquisições repetíveis de Corpo de
        // Touro, mas descartamos os demais Encantos que podem ter fechado a
        // rota. Assim a recuperação continua legal e não transforma uma
        // escolha gulosa anterior em uma ficha com menos de 15 Encantos.
        if (projetoComOcultismo == null) {
            val corposPreservados = selecionados
                .filter { it.nome == NOME_CORPO_DE_TOURO }
                .take((quantidadeTotal - minimoOcultismo - 1).coerceAtLeast(0))
            val nomesBaseProjeto = corposPreservados.map { it.nome }.toSet()
            projetoComOcultismo = encontrarCadeiaOcultismo(
                projetoAtual = corposPreservados,
                nomesAtual = nomesBaseProjeto,
                contagemAtual = 0,
                restantes = minimoOcultismo
            )
        }

        projetoComOcultismo ?: return selecionados

        val nomesProjeto = projetoComOcultismo.map { it.nome }.toMutableSet()
        val contagemOcultismo = ocultismoExistente + faltamOcultismo
        if (feiticaria.nome in nomesProjeto) return selecionados

        val elegivelFeiticaria = elegivel(
            feiticaria,
            abilities,
            essencia,
            nomesProjeto,
            catalogo,
            contagemCategoriaSelecionada = { categoria ->
                if (categoria.equals(EncounterGenerationRules.HABILIDADE_OCULTISMO, ignoreCase = true)) contagemOcultismo else 0
            }
        )
        if (!elegivelFeiticaria) return selecionados

        // O projeto reserva cinco aquisições, mas a seleção comum pode ficar
        // abaixo das dez vagas reservadas quando uma rota de pré-requisitos
        // esgota as opções de uma Árvore. Nesse caso, não basta acrescentar
        // Ocultismo + Feitiçaria: precisamos completar o orçamento normal de
        // 15 Encantos para não produzir fichas estruturalmente incompletas.
        val projetoCompleto = projetoComOcultismo + feiticaria
        if (projetoCompleto.size >= quantidadeTotal) return projetoCompleto

        val estadoComProjeto = preencherAteQuantidadeComBusca(
            catalogo = catalogo,
            estadoInicial = projetoCompleto,
            abilities = abilities,
            essencia = essencia,
            quantidade = quantidadeTotal,
            prioridade = abilities.keys.toList(),
            filtro = { it.nome != feiticaria.nome }
        )
        val complemento = estadoComProjeto.drop(projetoCompleto.size)

        return projetoCompleto + complemento
    }

    private fun preencherAteQuantidadeComBusca(
        catalogo: List<EncantoSolarDefinition>,
        estadoInicial: List<EncantoSolarDefinition>,
        abilities: Map<String, Int>,
        essencia: Int,
        quantidade: Int,
        prioridade: List<String> = emptyList(),
        filtro: (EncantoSolarDefinition) -> Boolean = { true },
        restricaoCandidato: (EncantoSolarDefinition, Set<String>) -> Boolean = { _, _ -> true }
    ): List<EncantoSolarDefinition> {
        if (estadoInicial.size >= quantidade) return estadoInicial.take(quantidade)

        data class Estado(
            val lista: List<EncantoSolarDefinition>,
            val nomes: Set<String>,
            val contagens: Map<String, Int>,
            val score: Int,
            val chave: java.util.BitSet
        )

        val categoriaConhecida: (String) -> Boolean = { it in HABILIDADES_CONHECIDAS }
        val categoriaDoEncanto: (EncantoSolarDefinition) -> String = { it.habilidade }
        val nomeDoEncanto: (EncantoSolarDefinition) -> String = { it.nome }

        fun elegivel(def: EncantoSolarDefinition, estado: Estado): Boolean {
            if (def.nome in estado.nomes || !filtro(def) || !restricaoCandidato(def, estado.nomes)) return false
            val contagemCategoriaSelecionada: (String) -> Int = { categoria ->
                estado.contagens[categoria.lowercase()] ?: 0
            }
            return elegivelGenerico(
                nivelAtual = abilities[def.habilidade] ?: 0,
                nivelRequerido = def.minHabilidade,
                minEssencia = def.minEssencia,
                preRequisitos = def.preRequisitos,
                essencia = essencia,
                nomesSelecionados = estado.nomes,
                catalogoCompleto = catalogo,
                categoriaDoEncanto = categoriaDoEncanto,
                categoriaConhecida = categoriaConhecida,
                nomeDoEncanto = nomeDoEncanto,
                contagemCategoriaSelecionada = contagemCategoriaSelecionada
            )
        }

        // O mesmo catálogo entra repetidamente nos fallbacks (estado atual,
        // retrocesso e projeto de Feitiçaria). A contagem abaixo é puramente
        // derivada do texto imutável do catálogo; cacheá-la evita reconstruir
        // todo o mapa de desbloqueios a cada tentativa de completude.
        val indiceCompletude = indiceCompletude(catalogo)
        val desbloqueiosPorNome = indiceCompletude.desbloqueiosPorNome

        fun desbloqueios(def: EncantoSolarDefinition): Int =
            desbloqueiosPorNome[def.nome.lowercase()] ?: 0

        // Foco, desbloqueios e baixo requisito de Essência são invariáveis
        // durante esta busca. A DFS/beam pode avaliar o mesmo candidato em
        // milhares de estados; pré-calcular essa parcela preserva exatamente
        // o score e deixa por estado somente a profundidade da Árvore.
        val prioridadeSet = prioridade.toHashSet()
        val scoreBasePorNome = HashMap<String, Int>(catalogo.size)
        val categoriaNormalizadaPorNome = HashMap<String, String>(catalogo.size)
        catalogo.forEach { def ->
            val foco = if (def.habilidade in prioridadeSet) 30 else 0
            val baixoCusto = (essencia - def.minEssencia).coerceAtLeast(0) * 2
            scoreBasePorNome[def.nome] = foco + desbloqueios(def) * 8 + baixoCusto
            categoriaNormalizadaPorNome[def.nome] = def.habilidade.lowercase()
        }

        fun score(def: EncantoSolarDefinition, estado: Estado): Int {
            val categoria = categoriaNormalizadaPorNome[def.nome] ?: def.habilidade.lowercase()
            val profundidade = (estado.contagens[categoria] ?: 0).coerceAtMost(3) * 6
            return (scoreBasePorNome[def.nome] ?: 0) + profundidade
        }

        val nomesInicial = estadoInicial.asSequence().map { it.nome }.toHashSet()
        val contagensInicial = HashMap<String, Int>()
        estadoInicial.forEach { def ->
            val categoria = categoriaNormalizadaPorNome[def.nome] ?: def.habilidade.lowercase()
            contagensInicial[categoria] = (contagensInicial[categoria] ?: 0) + 1
        }

        // Antes da heurística beam, tenta uma busca exata e limitada para
        // garantir o preenchimento quando existe uma cadeia legal curta.
        // A versão anterior podia podar justamente a única rota que fechava
        // um pré-requisito. O orçamento de nós evita custo descontrolado e,
        // se esgotado, a beam search abaixo continua sendo o fallback.
        val limiteNosBuscaExata = 100_000
        var nosVisitados = 0
        // A DFS visita até 100 mil estados. A chave antiga ordenava todos
        // os nomes e criava uma String em cada nó. O catálogo é imutável durante
        // a busca, então um BitSet ordinal representa exatamente o mesmo conjunto
        // sem sort/join e com equals/hashCode por conteúdo.
        val ordinalPorNome = indiceCompletude.ordinalPorNome
        // "Visitado" e "sem saída comprovada" são conceitos distintos.
        // Um estado visto antes de o orçamento acabar não pode ser tratado como
        // impossível pela beam search: a DFS pode simplesmente ter sido cortada.
        val estadosVisitados = HashSet<java.util.BitSet>()
        val estadosSemSaida = HashSet<java.util.BitSet>()
        var orcamentoEsgotado = false
        // A ordem da DFS não depende do estado: Essência, foco, desbloqueios,
        // Habilidade e nome são invariantes nesta chamada. Ordenamos o catálogo
        // uma vez e, em cada nó, apenas filtramos os candidatos legais.
        val ordemBuscaExata = catalogo.sortedWith(
            compareBy<EncantoSolarDefinition> { it.minEssencia }
                .thenBy { if (it.habilidade in prioridadeSet) 0 else 1 }
                .thenByDescending { desbloqueios(it) }
                .thenBy { it.habilidade }
                .thenBy { it.nome }
        )

        val ordinalPorDefinicao = HashMap<EncantoSolarDefinition, Int>(catalogo.size).apply {
            catalogo.forEach { def -> ordinalPorNome[def.nome]?.let { put(def, it) } }
        }

        fun chaveInicial(nomes: Set<String>): java.util.BitSet =
            java.util.BitSet(ordinalPorNome.size).apply {
                nomes.forEach { nome -> ordinalPorNome[nome]?.let(::set) }
            }

        fun chaveComCandidato(estado: Estado, candidato: EncantoSolarDefinition): java.util.BitSet =
            (estado.chave.clone() as java.util.BitSet).apply {
                ordinalPorDefinicao[candidato]?.let(::set)
            }

        fun estadoComCandidato(
            estado: Estado,
            candidato: EncantoSolarDefinition,
            scoreCandidato: Int
        ): Estado {
            val categoria =
                categoriaNormalizadaPorNome[candidato.nome] ?: candidato.habilidade.lowercase()
            val novosNomes = HashSet<String>((estado.nomes.size + 1) * 4 / 3 + 1).apply {
                addAll(estado.nomes)
                add(candidato.nome)
            }
            val novasContagens = HashMap<String, Int>((estado.contagens.size + 1) * 4 / 3 + 1).apply {
                putAll(estado.contagens)
                this[categoria] = (this[categoria] ?: 0) + 1
            }
            return Estado(
                lista = estado.lista + candidato,
                nomes = novosNomes,
                contagens = novasContagens,
                score = estado.score + scoreCandidato,
                chave = chaveComCandidato(estado, candidato)
            )
        }

        fun buscarCompleto(estado: Estado): List<EncantoSolarDefinition>? {
            if (estado.lista.size >= quantidade) return estado.lista.take(quantidade)
            if (nosVisitados++ >= limiteNosBuscaExata) {
                orcamentoEsgotado = true
                return null
            }

            val chave = estado.chave
            if (!estadosVisitados.add(chave)) return null

            val candidatos = ArrayList<Pair<EncantoSolarDefinition, Int>>()
            for (def in ordemBuscaExata) {
                if (elegivel(def, estado)) candidatos += def to score(def, estado)
            }
            // A DFS continua sendo uma prova limitada de completude, mas
            // quando há várias extensões legais ela visita primeiro a que
            // o próprio modelo estratégico considera mais coerente.
            candidatos.sortWith(
                compareByDescending<Pair<EncantoSolarDefinition, Int>> { it.second }
                    .thenBy { it.first.minEssencia }
                    .thenBy { it.first.habilidade }
                    .thenBy { it.first.nome }
            )

            // Não podar pela quantidade de candidatos atualmente legais:
            // um candidato pode desbloquear descendentes e aumentar esse conjunto.
            // A prova de impossibilidade só é válida depois de esgotar o espaço
            // alcançável (ou quando não existe qualquer próximo candidato).
            if (candidatos.isEmpty()) {
                estadosSemSaida.add(chave)
                return null
            }

            for ((candidato, scoreCandidato) in candidatos) {
                val novoEstado = estadoComCandidato(estado, candidato, scoreCandidato)
                val resultado = buscarCompleto(novoEstado)
                if (resultado != null) return resultado
                if (nosVisitados >= limiteNosBuscaExata) break
            }
            // Só é seguro podar este conjunto no fallback quando todos os seus
            // descendentes foram examinados sem que o limite global interrompesse
            // a prova. Estados apenas visitados continuam disponíveis à beam.
            if (!orcamentoEsgotado) estadosSemSaida.add(chave)
            return null
        }

        val estadoInicialCompleto = Estado(
            estadoInicial, nomesInicial, contagensInicial, 0, chaveInicial(nomesInicial)
        )
        buscarCompleto(estadoInicialCompleto)?.let {
            EncounterSelectionTelemetry.recordExactSearchFound()
            return it
        }
        if (nosVisitados >= limiteNosBuscaExata) {
            EncounterSelectionTelemetry.recordExactSearchBudgetExhausted()
        } else {
            // A DFS esgotou todo o espaço alcançável dentro do catálogo sem
            // atingir a quantidade solicitada: ausência de solução provada
            // para este estado inicial, distinta de orçamento esgotado.
            EncounterSelectionTelemetry.recordExactSearchProvenImpossible()
        }

        var fronteira = listOf(estadoInicialCompleto)
        val largura = 48
        val maxExpansoesPorEstado = 96
        val faltam = quantidade - estadoInicial.size
        // A chave compacta também deduplica convergências da beam: ordens de
        // aquisição diferentes que chegam ao mesmo conjunto de Encantos possuem
        // exatamente a mesma legalidade futura. Mantemos apenas o estado de
        // maior score antes de aplicar a largura do feixe.
        val chaveInicial = estadoInicialCompleto.chave

        repeat(faltam) {
            val expansoes = mutableListOf<Estado>()
            for (estado in fronteira) {
                // Se a busca exata terminou este estado e o marcou como sem
                // saída, a beam search não precisa explorar exatamente o mesmo
                // conjunto novamente. O estado inicial é preservado quando a
                // DFS foi interrompida pelo orçamento, para que a beam continue
                // funcionando como fallback real em vez de ser anulada.
                val chaveAtual = estado.chave
                if (chaveAtual != chaveInicial && chaveAtual in estadosSemSaida) continue
                val comparator =
                    compareByDescending<Pair<EncantoSolarDefinition, Int>> { it.second }
                        .thenBy { it.first.minEssencia }
                        .thenBy { it.first.habilidade }
                        .thenBy { it.first.nome }
                val worstFirst = java.util.PriorityQueue<Pair<EncantoSolarDefinition, Int>>(
                    maxExpansoesPorEstado,
                    comparator.reversed()
                )
                for (def in catalogo) {
                    if (!elegivel(def, estado)) continue
                    val candidate = def to score(def, estado)
                    if (worstFirst.size < maxExpansoesPorEstado) {
                        worstFirst += candidate
                    } else if (comparator.compare(candidate, worstFirst.peek()) < 0) {
                        worstFirst.poll()
                        worstFirst += candidate
                    }
                }
                val candidatos = worstFirst.toMutableList().apply { sortWith(comparator) }

                for ((candidato, scoreCandidato) in candidatos) {
                    expansoes += estadoComCandidato(estado, candidato, scoreCandidato)
                }
            }
            if (expansoes.isEmpty()) {
                return fronteira
                    .maxWithOrNull(compareBy<Estado> { it.lista.size }.thenBy { it.score })
                    ?.lista
                    ?: estadoInicial
            }
            val melhoresPorEstado = LinkedHashMap<java.util.BitSet, Estado>()
            for (candidato in expansoes) {
                val chave = candidato.chave
                val anterior = melhoresPorEstado[chave]
                if (anterior == null ||
                    candidato.score > anterior.score ||
                    (candidato.score == anterior.score &&
                        (candidato.lista.lastOrNull()?.nome ?: "") <
                        (anterior.lista.lastOrNull()?.nome ?: ""))
                ) {
                    melhoresPorEstado[chave] = candidato
                }
            }
            fronteira = melhoresPorEstado.values
                .sortedWith(
                    compareByDescending<Estado> { it.score }
                        .thenBy { it.lista.lastOrNull()?.minEssencia ?: Int.MAX_VALUE }
                        .thenBy { it.lista.lastOrNull()?.nome ?: "" }
                )
                .take(largura)
        }

        return fronteira
            .maxWithOrNull(compareBy<Estado> { it.lista.size }.thenBy { it.score })
            ?.lista
            ?: estadoInicial
    }

    fun selecionarIniciais(
        catalogo: List<EncantoSolarDefinition>,
        abilities: Map<String, Int>,
        essencia: Int,
        ordemHabilidades: List<String>,
        quantidade: Int,
        habilidadeCombatePrincipal: String? = null,
        habilidadePrincipalArquetipo: String? = null,
        minimoEncantosHabilidadePrincipal: Int = 3,
        focoEscolhidoPeloUsuario: Boolean = false,
        minimosHabilidadesAdicionais: Map<String, Int> = emptyMap(),
        restricaoCandidato: (EncantoSolarDefinition, Set<String>) -> Boolean = { _, _ -> true }
    ): List<EncantoSolarDefinition> {
        val metadata = catalogMetadata(catalogo)
        val catalogoPorHabilidade = metadata.porHabilidade
        // A seleção chama o otimizador repetidamente enquanto preenche a ficha.
        // Preparar o catálogo uma única vez evita reconstruir índices, ordinais e
        // tags de sinergia a cada escolha de candidato.
        val catalogoPreparado = preparedRouteCatalog(catalogo)
        // A mesma seleção invoca o beam várias vezes conforme novos Encantos
        // são comprados. Estados de lookahead de uma chamada reaparecem na
        // seguinte; preserve o memo compacto durante toda a seleção, como já
        // ocorre no fluxo Lunar, sem compartilhar estados entre NPCs.
        val compactEligibilityMemo =
            HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()
        val routeMetrics = EncounterCharmRouteMetrics()
        val selecionados = mutableListOf<EncantoSolarDefinition>()
        val nomesSelecionados = mutableSetOf<String>()
        val categoriasSelecionadas = mutableMapOf<String, Int>()
        fun registrar(def: EncantoSolarDefinition) {
            nomesSelecionados += def.nome
            val categoria = def.habilidade.lowercase()
            categoriasSelecionadas[categoria] = (categoriasSelecionadas[categoria] ?: 0) + 1
        }
        fun elegivelSelecionado(def: EncantoSolarDefinition): Boolean =
            restricaoCandidato(def, nomesSelecionados) && elegivelGenerico(
                nivelAtual = abilities[def.habilidade] ?: 0,
                nivelRequerido = def.minHabilidade,
                minEssencia = def.minEssencia,
                preRequisitos = def.preRequisitos,
                essencia = essencia,
                nomesSelecionados = nomesSelecionados,
                catalogoCompleto = catalogo,
                categoriaDoEncanto = { it.habilidade },
                categoriaConhecida = { it in HABILIDADES_CONHECIDAS },
                nomeDoEncanto = { it.nome },
                contagemCategoriaSelecionada = { categoria -> categoriasSelecionadas[categoria.lowercase()] ?: 0 }
            )

        fun melhorCandidatoPorRota(candidatos: List<EncantoSolarDefinition>): EncantoSolarDefinition? =
            EncounterCharmRouteOptimizer.escolher(
                candidatos = candidatos,
                catalogoCompleto = catalogo,
                nomesSelecionados = nomesSelecionados,
                contagensCategorias = categoriasSelecionadas,
                elegivel = { def, nomes, contagens ->
                    restricaoCandidato(def, nomes) && elegivelGenerico(
                        nivelAtual = abilities[def.habilidade] ?: 0,
                        nivelRequerido = def.minHabilidade,
                        minEssencia = def.minEssencia,
                        preRequisitos = def.preRequisitos,
                        essencia = essencia,
                        nomesSelecionados = nomes,
                        catalogoCompleto = catalogo,
                        categoriaDoEncanto = { it.habilidade },
                        categoriaConhecida = { it in HABILIDADES_CONHECIDAS },
                        nomeDoEncanto = { it.nome },
                        contagemCategoriaSelecionada = { categoria -> contagens[categoria.lowercase()] ?: 0 }
                    )
                },
                nome = { it.nome },
                categoria = { it.habilidade },
                custoXp = { 10 },
                compactEligibilityMemo = compactEligibilityMemo,
                preparedCatalog = catalogoPreparado,
                metrics = routeMetrics
            )

        val habilidadePrincipal = habilidadePrincipalArquetipo?.takeIf {
            it.isNotBlank() && it in ExaltedConstants.ALL_25_ABILITIES
        }
        // Em geração personalizada, o foco escolhido pelo usuário não é apenas
        // uma sugestão de desempate: ele deve dominar a seleção inicial.
        // Reservamos até 6 aquisições para a árvore escolhida (quando existem
        // Encantos legais), preservando os mínimos estruturais obrigatórios.
        val minimoPrincipalSolicitado = if (focoEscolhidoPeloUsuario) {
            maxOf(minimoEncantosHabilidadePrincipal, 6)
        } else minimoEncantosHabilidadePrincipal
        val minimoPrincipal = minimoPrincipalSolicitado.coerceAtLeast(0).coerceAtMost(quantidade)

        val limiteCorpoDeTouro = (abilities["Resistência"] ?: 0).coerceAtLeast(0)
        val candidatoCorpoDeTouro = metadata.corpoDeTouro
        val elegivelCorpoDeTouro = candidatoCorpoDeTouro != null && elegivelSelecionado(candidatoCorpoDeTouro)
        if (elegivelCorpoDeTouro) {
            val corpoDeTouro = candidatoCorpoDeTouro ?: error("Corpo de Touro elegível sem definição")

            // Quando há mínimos estruturais adicionais (por exemplo, os quatro
            // Encantos de Ocultismo necessários antes da Feitiçaria), Corpo de
            // Touro não pode consumir todo o espaço reservado para a seleção
            // comum. A versão anterior repetia Corpo de Touro até Resistência
            // mesmo nesses casos; com Resistência 4+, isso podia ocupar 4 das
            // 10 vagas e tornar matematicamente impossível acomodar, ao mesmo
            // tempo, 4 Ocultismo + os 3 Encantos mínimos da Habilidade principal.
            // Reservamos a maior exigência por Habilidade, evitando somar duas
            // vezes quando a Habilidade principal e a Habilidade adicional são
            // a mesma (caso comum em Arquétipo Mental).
            val minimoPorHabilidadeParaReserva = minimosHabilidadesAdicionais
                .toMutableMap()
                .apply {
                    if (habilidadePrincipal != null) {
                        this[habilidadePrincipal] = maxOf(
                            this[habilidadePrincipal] ?: 0,
                            minimoPrincipal
                        )
                    }
                }
            val vagasNecessariasParaMinimos = minimoPorHabilidadeParaReserva.values.sum().coerceAtMost(quantidade)
            val vagasDisponiveisParaCorpo = (quantidade - vagasNecessariasParaMinimos).coerceAtLeast(0)

            repeat(limiteCorpoDeTouro.coerceAtMost(vagasDisponiveisParaCorpo)) {
                selecionados += corpoDeTouro
                registrar(corpoDeTouro)
            }
        }

        // DIRETRIZ DE DISTRIBUIÇÃO DE ENCANTOS — Aba 11 / Encontros.
        // Antes de abrir uma nova Árvore, o gerador tenta completar um bloco
        // de 3 Encantos na Árvore atual. Depois que as primeiras Árvores
        // prioritárias atingem 3, os blocos seguintes aprofundam as mesmas
        // Árvores antes de espalhar Encantos por muitas linhas diferentes.
        // Isso vale para Solar, Sangue de Dragão e qualquer fluxo que use este
        // seletor. Pré-requisitos continuam sendo reavaliados a cada compra.
        val habilidadesOrdenadas = EncounterRulePolicy.resolvePriority(
            explicitIntent = ordemHabilidades,
            fallback = ExaltedConstants.ALL_25_ABILITIES
        )
        val candidatosPorHabilidadeFiltrado = habilidadesOrdenadas.associateWith { hab ->
            // O bucket já contém somente Encantos de "hab". A restrição de
            // combate é, portanto, uma decisão por Habilidade e não exige
            // filtrar/alocar uma nova lista para cada bucket.
            val ramoCombateIncompativel =
                habilidadeCombatePrincipal != null &&
                    hab in EncounterGenerationRules.COMBAT_ABILITIES &&
                    hab != habilidadeCombatePrincipal
            if (ramoCombateIncompativel) emptyList() else catalogoPorHabilidade[hab].orEmpty()
        }

        // Regra de construção do arquétipo: a habilidade principal recebe
        // pelo menos 3 Encantos antes que a seleção se espalhe. O foco pode
        // receber blocos adicionais depois, conforme a regra geral de
        // aprofundamento por Árvores.
        if (habilidadePrincipal != null && minimoPrincipal > 0) {
            var tentativasPrincipal = 0
            val categoriaPrincipal = habilidadePrincipal.lowercase()
            val candidatosPrincipalOrdenados =
                if (habilidadePrincipal in candidatosPorHabilidadeFiltrado) {
                    metadata.porHabilidadeParaMinimoPrincipal[habilidadePrincipal].orEmpty()
                } else {
                    emptyList()
                }
            while ((categoriasSelecionadas[categoriaPrincipal] ?: 0) < minimoPrincipal &&
                selecionados.size < quantidade && tentativasPrincipal < 200) {
                tentativasPrincipal++
                val candidato = candidatosPrincipalOrdenados
                    .firstOrNull { it.nome !in nomesSelecionados && elegivelSelecionado(it) }
                    ?: break
                selecionados += candidato
                registrar(candidato)
            }
        }

        // Mínimos estruturais adicionais continuam sendo respeitados quando
        // uma regra específica exige uma quantidade maior que 3, como os
        // 4 Encantos de Ocultismo necessários para determinadas aquisições
        // de Feitiçaria.
        minimosHabilidadesAdicionais
            .filter { (habilidade, minimo) -> habilidade in ExaltedConstants.ALL_25_ABILITIES && minimo > 0 }
            .forEach { (habilidade, minimo) ->
                var tentativasAdicional = 0
                val categoriaAdicional = habilidade.lowercase()
                // A ordem é invariável durante este mínimo estrutural; apenas a
                // elegibilidade muda conforme os pré-requisitos são adquiridos.
                val candidatosAdicionaisOrdenados =
                    if (habilidade in candidatosPorHabilidadeFiltrado) {
                        // O metadata já mantém esta ordem canônica; não precisamos
                        // ordenar novamente o mesmo bucket para cada mínimo estrutural.
                        metadata.porHabilidadeParaMinimoPrincipal[habilidade].orEmpty()
                    } else {
                        emptyList()
                    }
                while ((categoriasSelecionadas[categoriaAdicional] ?: 0) < minimo &&
                    selecionados.size < quantidade && tentativasAdicional < 200) {
                    tentativasAdicional++
                    // Mínimos estruturais (como os 4 Encantos de Ocultismo
                    // exigidos antes de Feitiçaria) não podem depender da
                    // beam search. A busca pode descartar justamente o único
                    // candidato que fecha a cadeia local e, nesse caso,
                    // produziria um falso déficit estrutural. Aqui consultamos
                    // diretamente todo o conjunto elegível dessa Habilidade.
                    val candidato = candidatosAdicionaisOrdenados
                        .firstOrNull { it.nome !in nomesSelecionados && elegivelSelecionado(it) }
                        ?: break
                    selecionados += candidato
                    registrar(candidato)
                }
            }

        var tentativas = 0
        // Profundidade antes de largura: apenas a Árvore de maior prioridade
        // começa ativa. Enquanto ela possuir Encantos legais, continuamos
        // aprofundando-a em blocos. Uma nova Árvore só é aberta quando as
        // Árvores já ativas não conseguem mais avançar. Mínimos estruturais
        // (Feitiçaria, Foco explícito etc.) já foram resolvidos acima.
        val arvoresAtivas = habilidadesOrdenadas.take(1).toMutableList()

        fun completarBloco(habilidade: String, bloco: Int = 3): Boolean {
            var progresso = false
            var tentativasBloco = 0
            val categoriaBloco = habilidade.lowercase()
            val alvo = (categoriasSelecionadas[categoriaBloco] ?: 0) + bloco
            while (
                selecionados.size < quantidade &&
                (categoriasSelecionadas[categoriaBloco] ?: 0) < alvo &&
                tentativasBloco++ < 100
            ) {
                val candidato = candidatosPorHabilidadeFiltrado[habilidade].orEmpty().firstOrNull {
                    it.nome !in nomesSelecionados && elegivelSelecionado(it)
                } ?: break
                selecionados += candidato
                registrar(candidato)
                progresso = true
            }
            return progresso
        }

        // Aprofunda a Árvore atual em blocos sucessivos. Só abre uma nova
        // Árvore quando nenhuma das já abertas consegue fornecer outro Encanto
        // legal. Isso evita NPCs formados por vários primeiros Encantos de
        // árvores diferentes quando ainda existe progressão na árvore principal.
        while (selecionados.size < quantidade && tentativas++ < 500) {
            var progressoRodada = false
            for (habilidade in arvoresAtivas) {
                if (selecionados.size >= quantidade) break
                if (completarBloco(habilidade, 3)) progressoRodada = true
            }
            if (selecionados.size >= quantidade) break
            if (progressoRodada) continue

            val proximaArvore = habilidadesOrdenadas.firstOrNull { it !in arvoresAtivas }
            if (proximaArvore == null) break
            arvoresAtivas += proximaArvore
            completarBloco(proximaArvore, 3)
        }

        // Fallback apenas para catálogos muito restritos: se as Árvores
        // prioritárias não conseguem mais fornecer Encantos, preenche as vagas
        // restantes com qualquer Árvore elegível. A regra de blocos continua
        // sendo a estratégia normal; o fallback evita NPCs com menos Encantos
        // apenas por falta de opções em suas Árvores prioritárias.
        var fallbackAtivadoNestaSelecao = false
        if (selecionados.size < quantidade) {
            fallbackAtivadoNestaSelecao = true
            EncounterSelectionTelemetry.recordCompletionFallbackActivated()
            // A recuperação de orçamento não restringe a Habilidade de combate.
            // A estratégia normal já prioriza a Habilidade escolhida; este caminho
            // só é usado quando a heurística de concentração deixou vagas. Nesse
            // caso, qualquer Encanto legal do catálogo pode ocupar a vaga restante.
            // Manter o filtro de combate aqui podia esgotar artificialmente o
            // conjunto elegível e produzir fichas com menos de 15 Encantos, apesar
            // de existirem Encantos legais em outras Árvores.
            val filtroRecuperacao: (EncantoSolarDefinition) -> Boolean = { true }

            val recuperacaoDoEstadoAtual = preencherAteQuantidadeComBusca(
                catalogo = catalogo,
                estadoInicial = selecionados.toList(),
                abilities = abilities,
                essencia = essencia,
                quantidade = quantidade,
                prioridade = ordemHabilidades,
                filtro = filtroRecuperacao,
                restricaoCandidato = restricaoCandidato
            )

            // Se a escolha gulosa das Árvores fechou em um beco sem saída,
            // não insistimos no mesmo prefixo. Recomeçamos somente a parte
            // não obrigatória, preservando Corpo de Touro e os mínimos
            // explícitos do arquétipo. Isso permite trocar uma escolha antiga
            // que bloqueou a cadeia de pré-requisitos sem relaxar as regras
            // estruturais da ficha.
            val minimoPorHabilidade = minimosHabilidadesAdicionais
                .toMutableMap()
                .apply {
                    if (habilidadePrincipal != null) {
                        this[habilidadePrincipal] = maxOf(
                            this[habilidadePrincipal] ?: 0,
                            minimoPrincipal
                        )
                    }
                }

            val nomesObrigatorios = mutableSetOf<String>()
            val estadoObrigatorio = mutableListOf<EncantoSolarDefinition>()
            val preservadosPorHabilidade = mutableMapOf<String, Int>()
            selecionados.forEach { def ->
                val devePreservarCorpo = def.nome == NOME_CORPO_DE_TOURO
                val minimo = minimoPorHabilidade[def.habilidade] ?: 0
                val quantidadeJaPreservada = preservadosPorHabilidade[def.habilidade] ?: 0
                if (devePreservarCorpo || quantidadeJaPreservada < minimo) {
                    if (nomesObrigatorios.add(def.nome) || devePreservarCorpo) {
                        estadoObrigatorio += def
                        preservadosPorHabilidade[def.habilidade] = quantidadeJaPreservada + 1
                    }
                }
            }

            val recuperacaoComRetrocesso = preencherAteQuantidadeComBusca(
                catalogo = catalogo,
                estadoInicial = estadoObrigatorio,
                abilities = abilities,
                essencia = essencia,
                quantidade = quantidade,
                prioridade = ordemHabilidades,
                filtro = filtroRecuperacao,
                restricaoCandidato = restricaoCandidato
            )

            val candidatosRecuperacao = arrayOf(recuperacaoDoEstadoAtual, recuperacaoComRetrocesso)
            var completados: List<EncantoSolarDefinition>? = null
            var melhorSomaEssencia = Int.MIN_VALUE
            for (candidato in candidatosRecuperacao) {
                if (candidato.size > quantidade) continue
                val somaEssencia = candidato.sumOf { it.minEssencia }
                val atual = completados
                if (atual == null ||
                    candidato.size > atual.size ||
                    (candidato.size == atual.size && somaEssencia > melhorSomaEssencia)
                ) {
                    completados = candidato
                    melhorSomaEssencia = somaEssencia
                }
            }
            val recuperacaoEscolhida = completados ?: selecionados.toList()

            if (recuperacaoEscolhida.size > selecionados.size) {
                selecionados.clear()
                nomesSelecionados.clear()
                categoriasSelecionadas.clear()
                recuperacaoEscolhida.forEach {
                    selecionados += it
                    registrar(it)
                }
            }
        }

        // Último guardião de completude: se as heurísticas e as duas buscas
        // anteriores ainda deixarem vagas, percorremos o catálogo inteiro de
        // forma determinística. Este caminho não altera a estratégia normal;
        // ele somente impede que uma poda heurística produza uma ficha com
        // menos Encantos do que o orçamento solicitado. Corpo de Touro é a
        // única aquisição que pode ser repetida pelo modelo de construção.
        if (selecionados.size < quantidade) {
            val candidatosFinais = catalogo.asSequence()
                .filter { def ->
                    def.nome !in nomesSelecionados || def.nome == NOME_CORPO_DE_TOURO
                }
                .sortedWith(
                    compareBy<EncantoSolarDefinition> { it.minEssencia }
                        .thenBy { if (it.habilidade in ordemHabilidades) 0 else 1 }
                        .thenBy { it.habilidade }
                        .thenBy { it.nome }
                )
                .toList()

            var tentativasFinais = 0
            var indiceBuscaFinal = 0
            while (selecionados.size < quantidade && tentativasFinais < candidatosFinais.size * 2 + 1) {
                tentativasFinais++
                var candidato: EncantoSolarDefinition? = null
                while (indiceBuscaFinal < candidatosFinais.size) {
                    val def = candidatosFinais[indiceBuscaFinal++]
                    if ((def.nome !in nomesSelecionados || def.nome == NOME_CORPO_DE_TOURO) &&
                        elegivelSelecionado(def)
                    ) {
                        candidato = def
                        break
                    }
                }
                candidato ?: break
                selecionados += candidato
                registrar(candidato)
            }
        }

        // Shadow mode: explica e mede a build somente depois de concluída.
        // Nenhum dado abaixo retorna ao score/eligibilidade/seleção.
        val trace = buildEncounterSelectionTrace(
            selected = selecionados.map { it.nome to it.habilidade },
            mainCategory = habilidadePrincipal,
            combatCategory = habilidadeCombatePrincipal,
            fallbackActivated = fallbackAtivadoNestaSelecao
        )
        val report = EncounterBuildQualityAnalyzer.analyze(
            categories = selecionados.map { it.habilidade },
            mainCategory = habilidadePrincipal,
            trace = trace
        )
        EncounterBuildObservability.publish(trace, report)
        EncounterBuildObservability.publishRouteMetrics(routeMetrics.snapshot())

        return selecionados
    }

    fun selecionarExcelencias(
        catalogo: List<EncantoSolarDefinition>,
        abilities: Map<String, Int>,
        essencia: Int,
        nomesJaSelecionados: Set<String>,
        quantidade: Int,
        habilidadeCombatePrincipal: String? = null
    ): List<EncantoSolarDefinition> {
        val nomesSelecionados = nomesJaSelecionados.toMutableSet()
        val selecionados = mutableListOf<EncantoSolarDefinition>()
        val categoriasSelecionadas = mutableMapOf<String, Int>()

        // APPROVED PERFORMANCE REFACTOR
        // Excelências são uma classe estática do catálogo. Filtrar essa classe
        // uma única vez evita testar palavras-chave em cada iteração da seleção.
        // Também mantemos a contagem das categorias já selecionadas para que
        // pré-requisitos do tipo "Quaisquer N Encantos de X" não precisem
        // revarrer o catálogo inteiro a cada candidato.
        val metadata = catalogMetadata(catalogo)
        val catalogoExcelenciasCompleto = metadata.excelencias
        val catalogoExcelencias = if (habilidadeCombatePrincipal == null) {
            catalogoExcelenciasCompleto
        } else {
            catalogoExcelenciasCompleto.filter {
                it.habilidade !in EncounterGenerationRules.COMBAT_ABILITIES ||
                    it.habilidade == habilidadeCombatePrincipal
            }
        }
        for (nome in nomesSelecionados) {
            val def = metadata.porNome[nome] ?: continue
            val categoria = def.habilidade.lowercase()
            categoriasSelecionadas[categoria] = (categoriasSelecionadas[categoria] ?: 0) + 1
        }

        // As Excelências gratuitas também obedecem à diretriz de foco:
        // aprofundam primeiro uma única Árvore já usada e só abrem outra
        // quando a atual não consegue mais avançar.
        val catalogoExcelenciasPorHabilidade = if (habilidadeCombatePrincipal == null) {
            metadata.excelenciasPorHabilidade
        } else {
            catalogoExcelencias.groupBy { it.habilidade }
        }
        val catalogoExcelenciasPreparado = if (habilidadeCombatePrincipal == null) {
            metadata.excelenciasPreparadas
        } else {
            EncounterCharmRouteOptimizer.prepareCatalog(
                catalogoCompleto = catalogoExcelencias,
                nome = { it.nome },
                categoria = { it.habilidade }
            )
        }
        val habilidadesJaUsadas = ArrayList<String>()
        val habilidadesJaUsadasSet = HashSet<String>()
        for (nome in nomesSelecionados) {
            val habilidade = metadata.porNome[nome]?.habilidade ?: continue
            if (habilidadesJaUsadasSet.add(habilidade)) habilidadesJaUsadas += habilidade
        }
        val habilidadesComExcelencia = EncounterRulePolicy.resolvePriority(
            synergy = habilidadesJaUsadas,
            fallback = catalogoExcelenciasPorHabilidade.keys.toList()
        )

        var tentativas = 0
        val arvoresAtivas = habilidadesComExcelencia.take(1).toMutableList()

        fun elegivelExcelencia(def: EncantoSolarDefinition): Boolean =
            def.nome !in nomesSelecionados &&
                elegivelGenerico(
                    nivelAtual = abilities[def.habilidade] ?: 0,
                    nivelRequerido = def.minHabilidade,
                    minEssencia = def.minEssencia,
                    preRequisitos = def.preRequisitos,
                    essencia = essencia,
                    nomesSelecionados = nomesSelecionados,
                    catalogoCompleto = catalogo,
                    categoriaDoEncanto = { it.habilidade },
                    categoriaConhecida = { it in HABILIDADES_CONHECIDAS },
                    nomeDoEncanto = { it.nome },
                    contagemCategoriaSelecionada = { categoria ->
                        categoriasSelecionadas[categoria.lowercase()] ?: 0
                    }
                )

        fun melhorExcelenciaPorRota(candidatos: List<EncantoSolarDefinition>): EncantoSolarDefinition? =
            EncounterCharmRouteOptimizer.escolher(
                candidatos = candidatos,
                catalogoCompleto = catalogoExcelencias,
                nomesSelecionados = nomesSelecionados,
                contagensCategorias = categoriasSelecionadas,
                elegivel = { def, nomes, contagens ->
                    elegivelGenerico(
                        nivelAtual = abilities[def.habilidade] ?: 0,
                        nivelRequerido = def.minHabilidade,
                        minEssencia = def.minEssencia,
                        preRequisitos = def.preRequisitos,
                        essencia = essencia,
                        nomesSelecionados = nomes,
                        catalogoCompleto = catalogo,
                        categoriaDoEncanto = { it.habilidade },
                        categoriaConhecida = { it in HABILIDADES_CONHECIDAS },
                        nomeDoEncanto = { it.nome },
                        contagemCategoriaSelecionada = { categoria -> contagens[categoria.lowercase()] ?: 0 }
                    )
                },
                nome = { it.nome },
                categoria = { it.habilidade },
                custoXp = { 0 },
                preparedCatalog = catalogoExcelenciasPreparado
            )

        fun completarBloco(habilidade: String): Boolean {
            var progresso = false
            val categoriaHabilidade = habilidade.lowercase()
            val alvo = (categoriasSelecionadas[categoriaHabilidade] ?: 0) + 3
            var tentativasBloco = 0
            while (
                selecionados.size < quantidade &&
                (categoriasSelecionadas[categoriaHabilidade] ?: 0) < alvo &&
                tentativasBloco++ < 100
            ) {
                val candidato = melhorExcelenciaPorRota(catalogoExcelenciasPorHabilidade[habilidade].orEmpty()) ?: break
                selecionados += candidato
                nomesSelecionados += candidato.nome
                val categoria = candidato.habilidade.lowercase()
                categoriasSelecionadas[categoria] = (categoriasSelecionadas[categoria] ?: 0) + 1
                progresso = true
            }
            return progresso
        }

        arvoresAtivas.forEach { if (selecionados.size < quantidade) completarBloco(it) }
        while (selecionados.size < quantidade && tentativas++ < 200) {
            var progresso = false
            for (habilidade in arvoresAtivas) {
                if (selecionados.size >= quantidade) break
                if (completarBloco(habilidade)) progresso = true
            }
            if (progresso) continue
            val proxima = habilidadesComExcelencia.firstOrNull { it !in arvoresAtivas } ?: break
            arvoresAtivas += proxima
            completarBloco(proxima)
        }

        if (selecionados.size < quantidade) {
            // A preferência pela Habilidade de combate principal não pode reduzir
            // a quantidade estrutural de Excelências gratuitas. Primeiro mantemos
            // a preferência; se ela esgotar candidatos legais, ampliamos somente
            // o preenchimento final para todas as Excelências elegíveis.
            val comparadorExcelenciaFinal =
                compareBy<EncantoSolarDefinition> {
                    if (habilidadeCombatePrincipal != null && it.habilidade == habilidadeCombatePrincipal) 0 else 1
                }.thenBy { it.minEssencia }
                    .thenBy { it.habilidade }
                    .thenBy { it.nome }
            while (selecionados.size < quantidade) {
                // Só precisamos do melhor candidato. minWithOrNull preserva
                // exatamente a mesma ordem de desempate sem ordenar toda a
                // coleção elegível em cada compra.
                val candidato = catalogoExcelenciasCompleto
                    .asSequence()
                    .filter { it.nome !in nomesSelecionados && elegivelExcelencia(it) }
                    .minWithOrNull(comparadorExcelenciaFinal)
                    ?: break
                selecionados += candidato
                nomesSelecionados += candidato.nome
                val categoria = candidato.habilidade.lowercase()
                categoriasSelecionadas[categoria] = (categoriasSelecionadas[categoria] ?: 0) + 1
            }
        }
        return selecionados
    }

}
