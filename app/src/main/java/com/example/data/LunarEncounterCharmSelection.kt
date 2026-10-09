package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import com.example.model.NOME_CORPO_DE_TOURO
import kotlin.random.Random

/**
 * Seleção inicial de Encantos Lunares na geração de NPCs (Aba 11).
 *
 * Extraído de LunarEncounterGenerator (refatoração de organização —
 * roteiro de refatoração agressiva, sem mudança de comportamento).
 */
internal object LunarEncounterCharmSelection {
    private val atributosMentaisNormalizados: Set<String> =
        ExaltedConstants.MENTAL_ATTRIBUTES.mapTo(hashSetOf()) { it.lowercase() }

    private data class LunarCatalogMetadata(
        val corpoDeTouro: EncantoLunarDefinition?,
        val feiticariaTerrestre: EncantoLunarDefinition?,
        val porNome: Map<String, EncantoLunarDefinition>
    )

    private val catalogMetadataCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<List<EncantoLunarDefinition>, LunarCatalogMetadata>()
        )

    private fun catalogMetadata(catalogo: List<EncantoLunarDefinition>): LunarCatalogMetadata =
        synchronized(catalogMetadataCache) {
            catalogMetadataCache[catalogo] ?: LunarCatalogMetadata(
                corpoDeTouro = catalogo.firstOrNull { it.nome == NOME_CORPO_DE_TOURO },
                feiticariaTerrestre = catalogo.firstOrNull {
                    it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE
                },
                porNome = catalogo.associateBy { it.nome }
            ).also { catalogMetadataCache[catalogo] = it }
        }

    private val orderedCatalogCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<List<EncantoLunarDefinition>, MutableMap<ArquetipoEncontro, Map<String, List<EncantoLunarDefinition>>>>()
        )

    private fun orderedByAttribute(
        catalogo: List<EncantoLunarDefinition>,
        catalogoPorAtributo: Map<String, List<EncantoLunarDefinition>>,
        arquetipo: ArquetipoEncontro
    ): Map<String, List<EncantoLunarDefinition>> = synchronized(orderedCatalogCache) {
        val byArchetype = orderedCatalogCache.getOrPut(catalogo) { mutableMapOf() }
        byArchetype[arquetipo] ?: catalogoPorAtributo.mapValues { (_, lista) ->
            lista.sortedWith(
                compareBy<EncantoLunarDefinition> { it.minEssencia }
                    .thenBy { LunarArchetypePolicy.subdivisionRank(arquetipo, it.subdivisao) }
                    .thenBy { it.nome }
            )
        }.also { byArchetype[arquetipo] = it }
    }

    private val preparedRouteCatalogCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<
                List<EncantoLunarDefinition>,
                EncounterCharmRouteOptimizer.PreparedCatalog<EncantoLunarDefinition>
            >()
        )

    private fun preparedRouteCatalog(
        catalogo: List<EncantoLunarDefinition>
    ): EncounterCharmRouteOptimizer.PreparedCatalog<EncantoLunarDefinition> =
        synchronized(preparedRouteCatalogCache) {
            preparedRouteCatalogCache[catalogo]
                ?: EncounterCharmRouteOptimizer.prepareCatalog(
                    catalogoCompleto = catalogo,
                    nome = { it.nome },
                    categoria = { it.atributo }
                ).also { preparedRouteCatalogCache[catalogo] = it }
        }

    data class SelectionResult(
        val charms: List<EncantoLunarDefinition>,
        val acquisitionAttributes: Map<String, String>,
        /** Métricas do hot path; diagnósticas, não alteram seleção nem persistência. */
        val routeMetrics: EncounterCharmRouteMetrics.Snapshot? = null,
        val ataqueEscolhido: String? = null
    )

    fun selecionarEncantosIniciais(
        catalogo: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>,
        essencia: Int,
        ordemAtributos: List<String>,
        quantidade: Int,
        random: Random,
        arquetipo: ArquetipoEncontro,
        atributoFocoUsuario: String? = null,
        spiritTraits: Set<LunarSpiritTrait> = emptySet(),
        exigirFeiticaria: Boolean = false
    ): List<EncantoLunarDefinition> = selecionarEncantosIniciaisComRotas(
        catalogo, attributes, essencia, ordemAtributos, quantidade, random,
        arquetipo, atributoFocoUsuario, spiritTraits, exigirFeiticaria
    ).charms

    fun selecionarEncantosIniciaisComRotas(

        catalogo: List<EncantoLunarDefinition>,
        attributes: Map<String, Int>,
        essencia: Int,
        ordemAtributos: List<String>,
        quantidade: Int,
        random: Random,
        arquetipo: ArquetipoEncontro,
        atributoFocoUsuario: String? = null,
        spiritTraits: Set<LunarSpiritTrait> = emptySet(),
        exigirFeiticaria: Boolean = false
    ): SelectionResult {
        // Um Encanto de Arquétipo entra também na gaveta do Atributo alternativo
        // quando a forma espiritual satisfaz sua condição.
        // As rotas habilitadas dependem apenas do catálogo + traços da(s) Forma(s)
        // durante esta seleção. Construímos as gavetas em uma única passagem em vez
        // de varrer todo o catálogo novamente para cada Atributo.
        val routeContext = LunarCharmArchetypePolicy.prepare(catalogo, spiritTraits)
        val catalogoPorAtributo: Map<String, List<EncantoLunarDefinition>> = routeContext.charmsByAttribute
        val selecionados = mutableListOf<EncantoLunarDefinition>()
        val nomesSelecionados = mutableSetOf<String>()
        val categoriasSelecionadas = mutableMapOf<String, Int>()
        val atributosAquisicaoSelecionados = mutableMapOf<String, String>()
        var contagemAtributoMentalSelecionada = 0
        var contagemCorpoDeTouroSelecionada = 0
        var contagemUniversalSelecionada = 0
        // Escolher somente uma arvore ofensiva. Projetar a cadeia de
        // prerequisitos de cada candidata usando as mesmas rotas da compra,
        // inclusive as alternativas habilitadas pela Forma Espiritual.
        // A projecao considera Essencia futura (ate 5), mas nunca inventa
        // aumentos de Atributo ou ignora prerequisitos.
        val ataqueEscolhido = if (arquetipo == ArquetipoEncontro.FISICO) {
            val foco = atributoFocoUsuario?.takeIf { it == "Força" || it == "Destreza" }
            foco ?: run {
                val ofensivos = listOf("Força", "Destreza")
                val pontuacoes = ofensivos.associateWith { atributo ->
                    val candidatos = catalogoPorAtributo[atributo].orEmpty()
                        .sortedWith(compareBy<EncantoLunarDefinition> { it.minEssencia }.thenBy { it.nome })
                    val adquiridos = mutableSetOf<String>()
                    val categoriaNomes = routeContext.charmNamesByCategory
                    var maiorEssencia = 0
                    // Cada passagem adquire ao menos um Encanto ou encerra.
                    // Limitar pelo tamanho do catalogo impede ciclos.
                    while (adquiridos.size < candidatos.size) {
                        val proximo = candidatos.firstOrNull { def ->
                            def.nome !in adquiridos &&
                                LunarCharmArchetypePolicy.eligibleRoutes(
                                    def, routeContext, attributes, 5, adquiridos, catalogo,
                                    contagemCategoriaSelecionada = { categoria ->
                                        if (categoria.isBlank()) adquiridos.size
                                        else categoriaNomes[categoria.trim().lowercase()]
                                            ?.count { it in adquiridos } ?: 0
                                    }
                                ).any { it.atributo == atributo }
                        }
                        if (proximo == null) break
                        adquiridos += proximo.nome
                        maiorEssencia = maxOf(maiorEssencia, proximo.minEssencia)
                    }
                    maiorEssencia to adquiridos.size
                }
                ofensivos.maxWithOrNull(
                    compareBy<String> { pontuacoes[it]?.first ?: 0 }
                        .thenBy { pontuacoes[it]?.second ?: 0 }
                        .thenBy { attributes[it] ?: 0 }
                )
            }
        } else null
        val ataqueDescartado = if (ataqueEscolhido == "Força") "Destreza" else "Força"
        fun vigorPermitido(contagens: Map<String, Int>): Boolean =
            arquetipo != ArquetipoEncontro.FISICO ||
                (contagens["vigor"] ?: 0) < (contagens[ataqueEscolhido?.lowercase()] ?: 0) + 3


        fun rotaElegivel(
            def: EncantoLunarDefinition,
            atributoPreferido: String? = null,
            nomes: Set<String> = nomesSelecionados,
            contagens: Map<String, Int> = categoriasSelecionadas
        ): LunarCharmArchetypePolicy.AcquisitionRoute? =
            LunarCharmArchetypePolicy.acquisitionRoute(
                def, atributoPreferido, routeContext, attributes, essencia, nomes, catalogo,
                contagemCategoriaSelecionada = { categoria ->
                    if (categoria.equals("Atributo Mental", ignoreCase = true)) {
                        atributosMentaisNormalizados.sumOf { contagens[it] ?: 0 }
                    } else {
                        contagens[categoria.lowercase()] ?: 0
                    }
                }
            )?.takeIf { rota ->
                (arquetipo != ArquetipoEncontro.FISICO || !rota.atributo.equals(ataqueDescartado, ignoreCase = true)) &&
                    (!rota.atributo.equals("Vigor", ignoreCase = true) || vigorPermitido(contagens))
            }

        fun registrar(
            def: EncantoLunarDefinition,
            atributoPreferido: String? = null,
            rotaDecidida: LunarCharmArchetypePolicy.AcquisitionRoute? = null
        ) {
            val rota = rotaDecidida ?: requireNotNull(rotaElegivel(def, atributoPreferido)) {
                "Encanto Lunar registrado sem rota elegível: ${def.nome}"
            }
            val atributoAquisicao = rota.atributo
            require(
                arquetipo != ArquetipoEncontro.FISICO ||
                    (!atributoAquisicao.equals(ataqueDescartado, ignoreCase = true) &&
                        (!atributoAquisicao.equals("Vigor", ignoreCase = true) ||
                            vigorPermitido(categoriasSelecionadas)))
            ) { "Aquisição Lunar física viola árvore ofensiva única ou limite de Vigor: ${def.nome}" }
            nomesSelecionados += def.nome
            atributosAquisicaoSelecionados.putIfAbsent(def.nome, atributoAquisicao)
            val categoria = atributoAquisicao.lowercase()
            categoriasSelecionadas[categoria] = (categoriasSelecionadas[categoria] ?: 0) + 1
            if (atributoAquisicao in ExaltedConstants.MENTAL_ATTRIBUTES) {
                contagemAtributoMentalSelecionada++
            }
            if (def.nome == NOME_CORPO_DE_TOURO) contagemCorpoDeTouroSelecionada++
            if (def.atributo.equals("Universal", ignoreCase = true)) contagemUniversalSelecionada++
        }

        fun elegivelSelecionado(def: EncantoLunarDefinition): Boolean =
            rotaElegivel(def) != null && LunarCharmArchetypePolicy.hasEligibleRoute(
                def, routeContext, attributes, essencia, nomesSelecionados, catalogo,
                contagemCategoriaSelecionada = { categoria ->
                    if (categoria.equals("Atributo Mental", ignoreCase = true)) {
                        contagemAtributoMentalSelecionada
                    } else {
                        categoriasSelecionadas[categoria.lowercase()] ?: 0
                    }
                }
            )

        // Lunar com projeto solicitado/explorado de Feitiçaria — ou Físico com Inteligência 3+ — reserva 5 slots do
        // orçamento inicial: 4 Encantos de Atributo Mental + Feitiçaria do
        // Círculo Terrestre. Sem essa reserva, Corpo de Touro, o Atributo
        // principal ou o Encanto Universal podiam consumir as 15 vagas antes
        // da etapa obrigatória de Feitiçaria, fazendo a validação estrutural
        // rejeitar uma geração que deveria ser válida.
        val reservaFeiticaria = if (
            (exigirFeiticaria || arquetipo == ArquetipoEncontro.FISICO) &&
                (attributes["Inteligência"] ?: 0) >= 3
        ) 5 else 0
        val limiteDeVagasAntesDaFeiticaria = (quantidade - reservaFeiticaria).coerceAtLeast(0)
        val limiteCorpoDeTouro = (attributes["Vigor"] ?: 0).coerceAtLeast(0)
        val candidatoCorpoDeTouro = catalogMetadata(catalogo).corpoDeTouro
        val elegivelCorpoDeTouro = candidatoCorpoDeTouro != null && elegivelSelecionado(candidatoCorpoDeTouro)

        // Cada Corpo de Touro ocupa um slot normal. Para Lunares Fisicos,
        // a geracao inicial compra no maximo um, preservando vagas para
        // aprofundar a unica arvore ofensiva; outros arquetipos mantem
        // o limite anterior determinado por Vigor.
        if (elegivelCorpoDeTouro && reservaFeiticaria == 0) {
            val corpoDeTouro = candidatoCorpoDeTouro ?: error("Corpo de Touro elegível sem definição")
            val limiteInicial = if (arquetipo == ArquetipoEncontro.FISICO) 1 else limiteCorpoDeTouro
            repeat(limiteInicial.coerceAtMost(limiteDeVagasAntesDaFeiticaria)) {
                if (rotaElegivel(corpoDeTouro) == null) return@repeat
                selecionados += corpoDeTouro
                registrar(corpoDeTouro)
            }
        }

        // PRINCÍPIO LUNAR DO ARQUÉTIPO:
        // o atributo principal é o atributo com maior valor dentro da
        // categoria do arquétipo. Em caso de empate, a escolha é aleatória.
        // A partir da primeira compra dessa gaveta, tentamos completar pelo
        // menos 3 Encantos do mesmo Atributo antes de abrir a seleção geral.
        val categoriaArquetipo = EncounterGenerationRules.ATTRIBUTE_GROUPS.getValue(arquetipo)
        val maiorValor = categoriaArquetipo.maxOfOrNull { attributes[it] ?: 0 } ?: 0
        val atributosPrincipaisEmpatados = categoriaArquetipo.filter { (attributes[it] ?: 0) == maiorValor }
        val atributoPrincipalAutomatico = (if (arquetipo == ArquetipoEncontro.FISICO) listOfNotNull(ataqueEscolhido) else atributosPrincipaisEmpatados.ifEmpty { categoriaArquetipo }.shuffled(random)).firstOrNull()
        val atributoPrincipal = atributoFocoUsuario
            ?.takeIf { it in EncounterGenerationRules.ALL_ATTRIBUTES && (arquetipo != ArquetipoEncontro.FISICO || it != ataqueDescartado) }
            ?: atributoPrincipalAutomatico
        val alvoEncantosAtributoPrincipal = if (atributoFocoUsuario != null) 6 else 3

        // A ordenação é estática; só a elegibilidade muda a cada compra.
        // Pré-ordenar uma vez evita reordenar a mesma gaveta em cada tentativa.
        val catalogoOrdenadoPorAtributo = orderedByAttribute(
            catalogo = catalogo,
            catalogoPorAtributo = catalogoPorAtributo,
            arquetipo = arquetipo
        )

        // Catálogo/tags/categorias são invariantes durante esta seleção. Preparar
        // uma vez evita reconstruí-los a cada uma das muitas chamadas do beam.
        val preparedRouteCatalog = preparedRouteCatalog(catalogo)
        val routeMetrics = EncounterCharmRouteMetrics()
        // Uma geração chama o beam search repetidamente à medida que compra Encantos.
        // Os lookaheads de uma chamada reaparecem na seguinte; preservar o memo evita
        // reavaliar todo o catálogo para estados canonicamente idênticos.
        val compactEligibilityMemo = HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()

        fun melhorCandidatoEntre(
            candidatos: List<EncantoLunarDefinition>,
            atributoPreferido: String? = null
        ): EncantoLunarDefinition? =
            EncounterCharmRouteOptimizer.escolher(
                candidatos = candidatos,
                catalogoCompleto = catalogo,
                nomesSelecionados = nomesSelecionados,
                contagensCategorias = categoriasSelecionadas,
                elegivel = { def, nomes, contagens ->
                    rotaElegivel(def, atributoPreferido, nomes, contagens) != null &&
                    LunarCharmArchetypePolicy.hasEligibleRoute(
                        def,
                        routeContext,
                        attributes,
                        essencia,
                        nomes,
                        catalogo,
                        contagemCategoriaSelecionada = { categoria ->
                            if (categoria.equals("Atributo Mental", ignoreCase = true)) {
                                atributosMentaisNormalizados.sumOf { contagens[it] ?: 0 }
                            } else {
                                contagens[categoria.lowercase()] ?: 0
                            }
                        }
                    )
                },
                nome = { it.nome },
                categoria = { def ->
                    val routes = routeContext.routesFor(def)
                    routes.firstOrNull {
                        atributoPreferido != null && it.atributo.equals(atributoPreferido, ignoreCase = true)
                    }?.atributo ?: routes.firstOrNull()?.atributo ?: def.atributo
                },
                categoriaNoEstado = { def, nomes, contagens ->
                    requireNotNull(
                        rotaElegivel(def, atributoPreferido, nomes, contagens)
                    ) { "Otimizador Lunar perdeu a rota elegível de ${def.nome} no estado avaliado" }.atributo
                },
                custoXp = { 10 },
                compactEligibilityMemo = compactEligibilityMemo,
                preparedCatalog = preparedRouteCatalog,
                metrics = routeMetrics,
                monotonicEligibility = true,
                monotonicAffectedNames = { candidate, selected, counts, afterCounts ->
                    val acquiredCategory = requireNotNull(
                        rotaElegivel(candidate, atributoPreferido, selected, counts)
                    ) { "Otimizador Lunar perdeu a rota elegível de ${candidate.nome} ao calcular delta" }.atributo
                    routeContext.affectedAfterAcquisition(candidate.nome, acquiredCategory)
                }
            )

        fun melhorCandidato(
            atributo: String,
            filtroExtra: ((EncantoLunarDefinition) -> Boolean)? = null
        ): EncantoLunarDefinition? {
            val bucket = catalogoOrdenadoPorAtributo[atributo].orEmpty()
            return melhorCandidatoEntre(
                if (filtroExtra == null) bucket else bucket.filter(filtroExtra),
                atributoPreferido = atributo
            )
        }

        // Invariantes estruturais do gerador não podem depender da heurística
        // da busca em feixe. Para esses pontos obrigatórios, selecionamos o
        // primeiro Encanto legal em ordem determinística. A otimização continua
        // sendo usada para as decisões de preenchimento posteriores.
        fun melhorCandidatoLegalDireto(
            candidatos: List<EncantoLunarDefinition>
        ): EncantoLunarDefinition? {
            // Os buckets recebidos daqui vêm de orderedByAttribute(), portanto
            // já estão na ordem determinística minEssencia/subdivisão/nome.
            // Reordená-los a cada compra repetia O(n log n) no hot path.
            for (def in candidatos) {
                if (def.nome !in nomesSelecionados && elegivelSelecionado(def)) return def
            }
            return null
        }

        // Formas escolhidas estrategicamente por MINUSCULO/TAMANHO_LENDARIO
        // precisam materializar a ponte que justificou a escolha quando a rota
        // de Arquétipo correspondente pertence ao Foco. A Forma habilita a rota;
        // somente a aquisição do Encanto concede seus efeitos.
        LunarSpiritFormStrategy.encantosEstruturais(
            traits = spiritTraits,
            catalogo = catalogo,
            attributes = attributes,
            essencia = essencia,
            foco = atributoFocoUsuario
        ).forEach { estrutural ->
            if (estrutural.nome !in nomesSelecionados &&
                selecionados.size < limiteDeVagasAntesDaFeiticaria
            ) {
                val rota = rotaElegivel(estrutural, atributoFocoUsuario)
                if (rota != null) {
                    selecionados += estrutural
                    registrar(estrutural, atributoFocoUsuario, rota)
                }
            }
        }

        // 1) Corpo de Touro é priorizado normalmente.
        //    Exceção: Físico com Inteligência 3+ possui um projeto estrutural
        //    obrigatório de 4 Encantos Mentais + Feitiçaria. Nesse caso,
        //    aquisições repetíveis de Corpo de Touro não podem consumir as
        //    vagas reservadas antes da conclusão do projeto; elas voltarão a
        //    disputar as vagas restantes no preenchimento final.
        // 2) Pelo menos 3 Encantos do Atributo principal, se o catálogo e os
        //    pré-requisitos permitirem. Se houver menos de 3 elegíveis, compra
        //    todos os disponíveis e segue normalmente — nunca cria aquisição
        //    inválida apenas para cumprir a prioridade do arquétipo.
        if (atributoPrincipal != null) {
            var tentativasPrincipal = 0
            while (
                ((categoriasSelecionadas[atributoPrincipal.lowercase()] ?: 0) -
                    if (atributoPrincipal.equals("Vigor", ignoreCase = true)) contagemCorpoDeTouroSelecionada else 0) <
                    alvoEncantosAtributoPrincipal &&
                selecionados.size < limiteDeVagasAntesDaFeiticaria &&
                tentativasPrincipal < 100
            ) {
                tentativasPrincipal++
                val candidato = melhorCandidatoLegalDireto(
                    catalogoOrdenadoPorAtributo[atributoPrincipal].orEmpty()
                ) ?: break
                selecionados += candidato
                registrar(candidato, atributoPrincipal)
            }
        }

        // 3) Pelo menos 1 Encanto Universal, quando houver Encantos
        //    Universais elegíveis. A seleção reavalia pré-requisitos a cada
        //    compra para permitir cadeias como Transformação Corporal Híbrida
        //    -> Empoderamento da Forma Bestial.
        var tentativasUniversal = 0
        while (
            contagemUniversalSelecionada < 1 &&
            selecionados.size < limiteDeVagasAntesDaFeiticaria &&
            tentativasUniversal < 100
        ) {
            tentativasUniversal++
            val candidato = melhorCandidatoLegalDireto(
                catalogoOrdenadoPorAtributo["Universal"].orEmpty()
            ) ?: break
            selecionados += candidato
            registrar(candidato)
        }

        // 4) Projeto de Feitiçaria solicitado/explorado — ou Lunar Físico com Inteligência 3+ — recebe
        // Feitiçaria do Círculo Terrestre. O JSON define o pré-requisito como
        // quaisquer 4 Encantamentos de Atributo Mental; portanto, cinco slots
        // são tratados como prioridade estrutural: 4 Encantos Mentais elegíveis
        // + o Círculo de Magia. O Círculo não é adicionado fora do orçamento.
        if ((exigirFeiticaria || arquetipo == ArquetipoEncontro.FISICO) && (attributes["Inteligência"] ?: 0) >= 3) {
            // O conjunto de candidatos mentais não muda durante esta etapa;
            // apenas a elegibilidade muda após cada aquisição.
            val candidatosMentais = ExaltedConstants.MENTAL_ATTRIBUTES
                .flatMap { catalogoOrdenadoPorAtributo[it].orEmpty() }
            var tentativasMental = 0
            while (contagemAtributoMentalSelecionada < 4 && selecionados.size < quantidade && tentativasMental < 200) {
                tentativasMental++
                val candidato = melhorCandidatoLegalDireto(candidatosMentais) ?: break
                selecionados += candidato
                registrar(candidato)
            }

            val defFeiticaria = catalogMetadata(catalogo).feiticariaTerrestre
            if (defFeiticaria != null &&
                defFeiticaria.nome !in nomesSelecionados &&
                contagemAtributoMentalSelecionada >= 4 &&
                selecionados.size < quantidade &&
                elegivelSelecionado(defFeiticaria)
            ) {
                selecionados += defFeiticaria
                registrar(defFeiticaria)
            }
        }

        // Garantia estrutural do Lunar Físico com Inteligência 3+: depois que
        // o projeto de Feitiçaria foi concluído, Corpo de Touro volta a ser uma
        // prioridade obrigatória. A reserva de 5 vagas impede a compra antes
        // dos 4 Encantos Mentais + Feitiçaria, mas não pode permitir que o
        // preenchimento heurístico consuma as 15 vagas sem nenhuma aquisição
        // de Corpo de Touro. Uma única aquisição é garantida aqui; compras
        // adicionais continuam sujeitas ao Vigor e à otimização normal.
        if (
            arquetipo == ArquetipoEncontro.FISICO &&
            (attributes["Inteligência"] ?: 0) >= 3 &&
            contagemCorpoDeTouroSelecionada == 0 &&
            selecionados.size < quantidade &&
            elegivelCorpoDeTouro
        ) {
            val corpoDeTouro = candidatoCorpoDeTouro
                ?: error("Corpo de Touro elegível sem definição")
            if (rotaElegivel(corpoDeTouro) != null) {
                selecionados += corpoDeTouro
                registrar(corpoDeTouro)
            }
        }

        // 5) As vagas restantes seguem a diretriz geral da Aba 11:
        // profundidade antes de largura. O Atributo principal permanece ativo
        // enquanto possuir progressão legal; somente depois abrimos outro
        // Atributo. Blocos de 3 são unidade de processamento, não autorização
        // para espalhar a ficha por três árvores rasas.
        // REGRA DE COMPLETUDE POR ARQUÉTIPO:
        // antes de recorrer a Atributos de outro arquétipo, esgotamos todas
        // as opções dos três Atributos pertencentes ao arquétipo atual.
        // Ex.: Físico = Força/Destreza/Vigor; Social = Carisma/Manipulação/
        // Aparência; Mental = Percepção/Inteligência/Raciocínio.
        // Isso evita declarar prematuramente que faltam Encantos para chegar
        // aos 15 quando ainda existem opções válidas no mesmo arquétipo.
        val grupoAtributosArquetipo = if (arquetipo == ArquetipoEncontro.FISICO) listOfNotNull(ataqueEscolhido, "Vigor") else EncounterGenerationRules.ATTRIBUTE_GROUPS.getValue(arquetipo)
        val atributosDoArquetipo = grupoAtributosArquetipo
            .filter { !it.equals(atributoPrincipal, ignoreCase = true) }

        val atributosForaDoArquetipo = EncounterGenerationRules.ALL_ATTRIBUTES
            .filter { atributo ->
                atributo !in grupoAtributosArquetipo &&
                    (arquetipo != ArquetipoEncontro.FISICO || atributo != ataqueDescartado) &&
                    !atributo.equals("Universal", ignoreCase = true)
            }

        val ordemMesmoArquetipo = EncounterRulePolicy.resolvePriority(
            explicitIntent = ordemAtributos.filter {
                it in grupoAtributosArquetipo &&
                    !it.equals(atributoPrincipal, ignoreCase = true)
            },
            fallback = atributosDoArquetipo
        )

        val ordemForaDoArquetipo = EncounterRulePolicy.resolvePriority(
            explicitIntent = ordemAtributos.filter { it in atributosForaDoArquetipo },
            fallback = atributosForaDoArquetipo
        )

        val arvoresAtivas = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOfNotNull(atributoPrincipal),
            fallback = ordemMesmoArquetipo
        ).take(1).toMutableList()

        fun completarBloco(atributo: String, bloco: Int = 3): Boolean {
            var progresso = false
            val categoriaAtributo = atributo.lowercase()
            val alvo = (categoriasSelecionadas[categoriaAtributo] ?: 0) + bloco
            var tentativasBloco = 0
            while (
                selecionados.size < quantidade &&
                selecionados.size < limiteDeVagasAntesDaFeiticaria &&
                (categoriasSelecionadas[categoriaAtributo] ?: 0) < alvo &&
                tentativasBloco++ < 100
            ) {
                val candidato = melhorCandidato(atributo) ?: break
                selecionados += candidato
                registrar(candidato, atributo)
                progresso = true
            }
            return progresso
        }

        // Aprofunda primeiro a Árvore principal. Só abre uma nova quando as
        // já ativas não conseguem mais fornecer Encantos elegíveis.
        var tentativasDistribuicao = 0
        while (selecionados.size < limiteDeVagasAntesDaFeiticaria && tentativasDistribuicao++ < 500) {
            var progressoRodada = false
            for (atributo in arvoresAtivas) {
                if (selecionados.size >= limiteDeVagasAntesDaFeiticaria) break
                if (completarBloco(atributo)) progressoRodada = true
            }
            if (progressoRodada) continue

            // Primeiro tenta abrir qualquer outro Atributo do mesmo
            // arquétipo que ainda não tenha sido ativado. Só depois de
            // esgotar os três Atributos do arquétipo atual é permitido
            // recorrer a Atributos de outro arquétipo.
            val proximaArvoreMesmoArquetipo = ordemMesmoArquetipo
                .firstOrNull { it !in arvoresAtivas }
            val proximaArvore = proximaArvoreMesmoArquetipo
                ?: ordemForaDoArquetipo.firstOrNull { it !in arvoresAtivas }
                ?: break
            arvoresAtivas += proximaArvore
            completarBloco(proximaArvore)
        }

        // Último preenchimento: se ainda houver vagas, esgota explicitamente
        // todos os candidatos elegíveis do arquétipo antes de aceitar qualquer
        // Encanto de fora dele. Isso corrige o caso em que uma árvore não
        // consegue formar um bloco de 3, mas ainda possui 1 ou 2 Encantos
        // válidos que podem completar as 15 vagas.
        // A reserva deixa de existir depois que o projeto estrutural foi
        // concluído. Antes da Feitiçaria ela protege cinco vagas; depois que
        // os quatro Encantos Mentais e a própria Feitiçaria já foram
        // adquiridos, as vagas restantes voltam a pertencer ao orçamento
        // normal de 15 Encantos.
        if (selecionados.size < quantidade) {
            val mesmoArquetipoCompleto = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOfNotNull(atributoPrincipal),
                fallback = ordemMesmoArquetipo
            )
            for (atributo in mesmoArquetipoCompleto) {
                while (selecionados.size < quantidade) {
                    val candidato = melhorCandidato(atributo) ?: break
                    selecionados += candidato
                    registrar(candidato, atributo)
                }
            }
        }

        // Somente se os três Atributos do arquétipo realmente não tiverem
        // Encantos suficientes é que o gerador pode recorrer a outro
        // arquétipo para completar o orçamento.
        if (selecionados.size < quantidade) {
            for (atributo in ordemForaDoArquetipo) {
                if (selecionados.size >= quantidade) break
                while (selecionados.size < quantidade) {
                    val candidato = melhorCandidatoLegalDireto(
                        catalogoOrdenadoPorAtributo[atributo].orEmpty()
                    ) ?: break
                    selecionados += candidato
                    registrar(candidato, atributo)
                }
            }
        }

        // Último guardião de completude: depois de esgotar as Árvores por
        // prioridade, percorremos o catálogo inteiro para preencher qualquer
        // vaga restante. Isso só atua quando as heurísticas não encontraram
        // uma composição completa; não muda a distribuição normal por blocos.
        // Corpo de Touro permanece a única aquisição repetível.
        if (selecionados.size < quantidade) {
            val candidatosFinais = catalogo.asSequence()
                .filter { it.nome !in nomesSelecionados || it.nome == NOME_CORPO_DE_TOURO }
                .sortedWith(
                    compareBy<EncantoLunarDefinition> { it.minEssencia }
                        .thenBy { LunarArchetypePolicy.subdivisionRank(arquetipo, it.subdivisao) }
                        .thenBy { it.atributo }
                        .thenBy { it.nome }
                )
                .toList()

            var tentativasFinais = 0
            var indiceBuscaFinal = 0
            while (selecionados.size < quantidade && tentativasFinais < candidatosFinais.size * 2 + 1) {
                tentativasFinais++
                var candidato: EncantoLunarDefinition? = null
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

        return SelectionResult(
            charms = selecionados,
            acquisitionAttributes = atributosAquisicaoSelecionados.toMap(),
            routeMetrics = routeMetrics.snapshot(),
            ataqueEscolhido = ataqueEscolhido
        )
}
}
