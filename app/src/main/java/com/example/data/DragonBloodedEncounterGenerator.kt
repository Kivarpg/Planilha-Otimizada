package com.example.data

import com.example.model.*
import kotlin.random.Random

internal object DragonBloodedEncounterGenerator {
    // O catálogo de Sangue de Dragão é imutável durante a vida do ViewModel.
    // A conversão para o formato Solar compartilhado é puramente estrutural e
    // antes recriava centenas de definições a cada NPC e a cada expansão de XP.
    // WeakHashMap mantém o cache preso somente enquanto o catálogo de origem
    // existir, evitando retenção permanente entre recargas.
    private val convertedCatalogCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<
                List<com.example.data.EncantoSangueDeDragaoDefinition>,
                List<EncantoSolarDefinition>
            >()
        )

    private val habilidadesPorAspecto: Map<Aspecto, List<String>> =
        Aspecto.entries.associateWith { it.allowedAbilities() }
    private val habilidadesSetPorAspecto: Map<Aspecto, Set<String>> =
        habilidadesPorAspecto.mapValues { (_, habilidades) -> habilidades.toSet() }
    private val habilidadesForaDoAspecto: Map<Aspecto, List<String>> =
        habilidadesSetPorAspecto.mapValues { (_, habilidades) ->
            ExaltedConstants.ALL_25_ABILITIES.filter { it !in habilidades }
        }

    internal fun catalogoConvertidoPreparado(
        catalogo: List<com.example.data.EncantoSangueDeDragaoDefinition>
    ): List<EncantoSolarDefinition> = catalogoConvertido(catalogo)

    private fun catalogoConvertido(
        catalogo: List<com.example.data.EncantoSangueDeDragaoDefinition>
    ): List<EncantoSolarDefinition> = synchronized(convertedCatalogCache) {
        convertedCatalogCache[catalogo]
            ?: catalogo.map { it.paraFormatoSolar() }
                .also { convertedCatalogCache[catalogo] = it }
    }

    /**
     * Seleciona as 5 Habilidades Favorecidas reais do Sangue de Dragão.
     *
     * As 5 Habilidades do Aspecto são canônicas e nunca são alteradas pelo
     * Arquétipo. O Arquétipo só influencia esta seleção, sempre no universo
     * das 20 Habilidades que estão fora do Aspecto.
     */
    private fun selecionarHabilidadesFavorecidas(
        arquetipo: ArquetipoEncontro,
        aspecto: Aspecto,
        habilidadesDoAspecto: List<String>,
        random: Random,
        focoPersonalizado: String? = null
    ): List<String> {
        val foraDoAspecto = habilidadesForaDoAspecto.getValue(aspecto)
        check(foraDoAspecto.size == 20) { "Sangue de Dragão deve ter 20 Habilidades fora do Aspecto" }
        val prioridade = when (arquetipo) {
            ArquetipoEncontro.FISICO -> PhysicalArchetypePolicy.priorityAbilities
            ArquetipoEncontro.SOCIAL -> SocialArchetypePolicy.priorityAbilities
            ArquetipoEncontro.MENTAL -> MentalArchetypePolicy.priorityAbilities
        }
        val candidatasLegais = foraDoAspecto
        val prioritarias = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOfNotNull(focoPersonalizado?.takeIf { it in candidatasLegais }),
            archetype = prioridade.filter { it in candidatasLegais }
        )
        val prioritariasSet = prioritarias.toSet()
        val restantes = candidatasLegais.filter { it !in prioritariasSet }.shuffled(random)
        return (prioritarias + restantes).take(5).also {
            check(it.size == 5 && it.distinct().size == 5) { "Sangue de Dragão deve possuir exatamente 5 Favorecidas" }
            check(it.intersect(habilidadesDoAspecto.toSet()).isEmpty()) { "Favorecida não pode pertencer ao Aspecto" }
        }
    }

    fun gerarSangueDeDragao(
        nomeManual: String,
        arquetipo: ArquetipoEncontro,
        encantosSangueDeDragao: List<com.example.data.EncantoSangueDeDragaoDefinition>,
        feiticos: List<com.example.data.FeiticoDefinition> = emptyList(),
        origemNome: OrigemNomeSangueDeDragao = OrigemNomeSangueDeDragao.SEM_CASTA,
        generoNome: GeneroNome? = null,
        random: Random = Random.Default,
        meritosCatalogo: List<MeritoDefinition> = emptyList(),
        focoPersonalizado: String? = null,
        customizacao: EncounterCustomization? = null,
        estilosArtesMarciais: List<EstiloArteMarcialDefinition> = emptyList()
    ): NpcEncontro {
        val encantosConvertidos = catalogoConvertido(encantosSangueDeDragao)

        val focoEfetivoBruto = customizacao?.focoEfetivo(focoPersonalizado) ?: focoPersonalizado.takeIf { customizacao == null }
        val focoFeiticariaExplicito = customizacao?.rotaFeiticaria == true || focoEfetivoBruto == ENCOUNTER_FOCUS_SORCERY
        val focoEfetivo = customizacao?.focoMecanico(focoPersonalizado, "Ocultismo")
            ?: if (focoFeiticariaExplicito) "Ocultismo" else focoEfetivoBruto
        val arquetipoEfetivo = arquetipo
        val generoEscolhido = generoNome ?: GeneroNome.entries.random(random)
        val nome = nomeManual.trim().ifBlank { NomesSangueDeDragao.gerar(origemNome, generoEscolhido, random) }

        // --- Aspecto: 5 habilidades fixas, sem Supernal ---
        val aspecto = Aspecto.entries.random(random)

        // As 5 Habilidades do Aspecto são canônicas e imutáveis.
        val habilidadesDoAspecto = habilidadesPorAspecto.getValue(aspecto)
        check(habilidadesDoAspecto.size == 5 && habilidadesDoAspecto.distinct().size == 5)
        val habilidadesFavorecidas = selecionarHabilidadesFavorecidas(arquetipoEfetivo, aspecto, habilidadesDoAspecto, random, focoEfetivo)

        val (primarios, secundarios, terciarios) = EncounterGenerationRules.gruposDeAtributoPara(arquetipoEfetivo, random)
        var attributes = EncounterDistributionService.distribuirAtributos(primarios, secundarios, terciarios, random, arquetipo)

        val habilidadeCombate = EncounterGenerationRules.resolverHabilidadeCombate(customizacao?.ataqueExplicito, customizacao?.focoParaAtaque(focoEfetivo) ?: focoEfetivo.takeIf { customizacao == null }, null, random)
        val habilidadeDefensivaObrigatoria = customizacao?.defesaExplicita ?: EncounterGenerationRules.habilidadeDefensivaPara(habilidadeCombate)
        // Restrições de Força/Destreza conforme a Habilidade de combate (Aba 11).
        attributes = EncounterDistributionService.ajustarAtributosPorHabilidadeCombate(
            attributes, habilidadeCombate, random
        )
        EncounterValidationService.validarDistribuicaoBaseDeAtributos(attributes, arquetipoEfetivo)
        // Sem Supernal — a habilidade de combate ainda sobe via Pontos de
        // Bônus, mas a ausência é representada por null para não misturar
        // identidade com foco de combate.
        // Índice construído uma vez: evita uma varredura completa do catálogo
        // para cada uma das cinco Habilidades de Aspecto.
        val metadataEncantos = EncounterCharmSelectionService.catalogMetadata(encantosConvertidos)
        val habilidadesComEncantos = metadataEncantos.habilidades
        val definicaoCorpoDeTouro = metadataEncantos.corpoDeTouro
        val habilidadesAspectoRelevantes = habilidadesDoAspecto.filter { it in habilidadesComEncantos }
        val resultado = EncounterDistributionService.distribuirHabilidades(
            arquetipoEfetivo,
            habilidadeCombate,
            habilidadeDefensivaObrigatoria,
            null,
            habilidadesFavorecidas,
            random,
            habilidadesEstruturaisRelevantes = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOfNotNull(focoEfetivo, customizacao?.secundariaExplicita),
                exaltStructure = habilidadesAspectoRelevantes
            ),
            habilidadesMinimoUm = if (arquetipoEfetivo == ArquetipoEncontro.MENTAL || origemNome in setOf(OrigemNomeSangueDeDragao.IMPERIO, OrigemNomeSangueDeDragao.LOOKSHY)) setOf("Linguística") else emptySet()
        )
        val (abilities, forcaDeVontadeComBonus, precisaEspecialidadeAdicional) = EncounterDistributionService.distribuirPontosDeBonus(
            arquetipo = arquetipoEfetivo,
            abilitiesBase = resultado.abilities,
            habilidadesFavorecidasOuCasta = EncounterRulePolicy.resolvePriority(
                exaltStructure = habilidadesDoAspecto + habilidadesFavorecidas
            ),
            forcaDeVontadeBase = 5,
            ordemPrioridade = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOfNotNull(focoEfetivo, customizacao?.secundariaExplicita),
                archetype = EncounterArchetypePolicy.abilityPriority(
                    archetype = arquetipoEfetivo,
                    combat = habilidadeCombate,
                    support = resultado.habilidadeSocialOuMental,
                    favored = EncounterRulePolicy.resolvePriority(
                        exaltStructure = habilidadesDoAspecto + habilidadesFavorecidas
                    )
                )
            ),
            habilidadeCombate = habilidadeCombate,
            habilidadesEstruturaisRelevantes = habilidadesAspectoRelevantes
        )
        var especialidades = EncounterDistributionService.distribuirEspecialidades(
            arquetipoEfetivo, habilidadeCombate, habilidadeDefensivaObrigatoria, resultado.habilidadeSocialOuMental, abilities, random,
            habilidadesEstruturaisRelevantes = habilidadesAspectoRelevantes
        )
        especialidades = EncounterDistributionService.adicionarEspecialidadeAdicional(
            especialidades,
            abilities,
            EncounterRulePolicy.resolvePriority(
                exaltStructure = habilidadesDoAspecto,
                efficiency = listOf(habilidadeCombate, habilidadeDefensivaObrigatoria, resultado.habilidadeSocialOuMental)
            ),
            precisaEspecialidadeAdicional
        )

        val essencia = EncounterGenerationRules.ESSENCIA_SANGUE_DE_DRAGAO
        val (arma, armadura) = EncounterEquipmentService.selecionarEquipamentoParaEncontro(
            habilidadeCombate = habilidadeCombate, random = random, permitirDoisArtefatos = true
        )

        // --- Encantos: 15 iniciais priorizando Aspecto + Favorecidas e
        // 5 gratuitos adicionais com palavra-chave "Excelência"
        // — pedido explícito do usuário, distinto da regra Solar. ---
        // Arquétipo Mental: "Ocultismo" precisa vir logo no início da
        // prioridade porque o Círculo de Magia exige 4 Encantos de Ocultismo.
        val explorarFeiticaria = EncounterSorceryRoutePolicy.decide(
            archetype = arquetipoEfetivo,
            explicitSorceryFocus = focoFeiticariaExplicito,
            random = random
        ).explore
        val priorizarOcultismo = EncounterSorceryRoutePolicy.shouldPrioritizeOccultism(
            archetype = arquetipoEfetivo,
            exploreSorcery = explorarFeiticaria,
            abilities = abilities
        )
        // O Foco escolhido pelo usuário na Aba 11 comanda a rota de Encantos.
        // Depois dele, priorizamos as Habilidades que efetivamente ficaram mais
        // altas na ficha e só então aplicamos a prioridade normal do arquétipo.
        val habilidadesPorEficiencia = abilities.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        val ordemHabilidadesIniciais = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOfNotNull(focoEfetivo),
            efficiency = habilidadesPorEficiencia,
            archetype = EncounterArchetypePolicy.charmPriority(
                    arquetipoEfetivo, habilidadeCombate, resultado.habilidadeSocialOuMental,
                    EncounterRulePolicy.resolvePriority(
                        exaltStructure = habilidadesDoAspecto + habilidadesFavorecidas
                    ), "",
                    priorizarOcultismo = priorizarOcultismo
                )
        )
        val circuloTerrestreDisponivel = metadataEncantos.feiticariaTerrestre != null
        val feiticariaNecessaria = EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject(
            archetype = arquetipoEfetivo,
            exploreSorcery = explorarFeiticaria,
            abilities = abilities,
            terrestrialCircleAvailable = circuloTerrestreDisponivel
        )
        val catalogoParaSelecao = if (feiticariaNecessaria) {
            metadataEncantos.semFeiticariaTerrestre
        } else encantosConvertidos
        val charmsIniciais = EncounterCharmSelectionService.selecionarIniciais(
            catalogo = catalogoParaSelecao,
            abilities = abilities,
            essencia = essencia,
            ordemHabilidades = ordemHabilidadesIniciais,
            quantidade = if (feiticariaNecessaria) {
                (EncounterGenerationRules.ENCANTOS_INICIAIS - EncounterGenerationRules.QUANTIDADE_OCULTISMO_FEITICARIA - 1).coerceAtLeast(0)
            } else EncounterGenerationRules.ENCANTOS_INICIAIS,
            habilidadeCombatePrincipal = habilidadeCombate,
            habilidadePrincipalArquetipo = focoEfetivo ?: when (arquetipoEfetivo) {
                ArquetipoEncontro.FISICO -> habilidadeCombate
                ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL -> resultado.habilidadeSocialOuMental
            },
            focoEscolhidoPeloUsuario = focoEfetivo != null,
            minimosHabilidadesAdicionais = if (feiticariaNecessaria) {
                mapOf(EncounterGenerationRules.HABILIDADE_OCULTISMO to 4)
            } else emptyMap(),
            restricaoCandidato = { def, nomes ->
                DragonBloodedSignaturePolicy.canSelect(
                    def, nomes, catalogoParaSelecao, essencia, aspecto.displayName, habilidadesFavorecidas
                )
            }
        )
        // O Corpo de Touro faz parte dos 15 slots normais. Quando o Círculo
        // de Magia é obrigatório/sorteado, o projeto reserva 1 vaga para ele
        // e 4 vagas preparatórias de Ocultismo; a seleção comum usa as 10
        // vagas restantes antes da inclusão do projeto.
        val quantidadeBase = if (feiticariaNecessaria) {
            (EncounterGenerationRules.ENCANTOS_INICIAIS - EncounterGenerationRules.QUANTIDADE_OCULTISMO_FEITICARIA - 1).coerceAtLeast(0)
        } else EncounterGenerationRules.ENCANTOS_INICIAIS
        val charmsIniciaisComCorpo = if (charmsIniciais.any { it.nome == NOME_CORPO_DE_TOURO }) {
            charmsIniciais
        } else {
            val definicaoCorpo = definicaoCorpoDeTouro
            if (definicaoCorpo != null && charmsIniciais.size < quantidadeBase) charmsIniciais + definicaoCorpo
            else if (definicaoCorpo != null && charmsIniciais.isNotEmpty()) charmsIniciais.dropLast(1) + definicaoCorpo
            else charmsIniciais
        }

        val candidatoFeiticaria = EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre(
            catalogo = encantosConvertidos,
            selecionados = charmsIniciaisComCorpo,
            abilities = abilities,
            essencia = essencia,
            quantidadeTotal = EncounterGenerationRules.ENCANTOS_INICIAIS,
            exigirProjeto = feiticariaNecessaria
        )
        val compararCandidatosFeiticaria =
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                archetype = arquetipoEfetivo,
                exploreSorcery = explorarFeiticaria,
                explicitSorceryFocus = focoFeiticariaExplicito,
                sorceryConstructible = feiticariaNecessaria
            )
        val candidatoPuro = if (compararCandidatosFeiticaria) {
            val ordemPura = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOfNotNull(focoEfetivo),
                efficiency = habilidadesPorEficiencia,
                archetype = EncounterArchetypePolicy.charmPriority(
                    arquetipoEfetivo, habilidadeCombate, resultado.habilidadeSocialOuMental,
                    EncounterRulePolicy.resolvePriority(
                        exaltStructure = habilidadesDoAspecto + habilidadesFavorecidas
                    ), "", priorizarOcultismo = false
                )
            )
            EncounterCharmSelectionService.selecionarIniciais(
                catalogo = encantosConvertidos,
                abilities = abilities,
                essencia = essencia,
                ordemHabilidades = ordemPura,
                quantidade = EncounterGenerationRules.ENCANTOS_INICIAIS,
                habilidadeCombatePrincipal = habilidadeCombate,
                habilidadePrincipalArquetipo = focoEfetivo ?: resultado.habilidadeSocialOuMental,
                focoEscolhidoPeloUsuario = focoEfetivo != null,
                restricaoCandidato = { def, nomes ->
                    DragonBloodedSignaturePolicy.canSelect(
                        def, nomes, encantosConvertidos, essencia, aspecto.displayName, habilidadesFavorecidas
                    )
                }
            ).let { lista ->
                if (lista.any { it.nome == NOME_CORPO_DE_TOURO }) lista else {
                    val corpo = definicaoCorpoDeTouro
                    if (corpo != null && lista.size < EncounterGenerationRules.ENCANTOS_INICIAIS) lista + corpo
                    else if (corpo != null && lista.isNotEmpty()) lista.dropLast(1) + corpo
                    else lista
                }
            }
        } else candidatoFeiticaria
        val selecionarFeiticaria = if (compararCandidatosFeiticaria) {
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = false,
                exploreSorcery = explorarFeiticaria,
                sorceryConstructible = candidatoFeiticaria.any {
                    it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE
                },
                pureQuality = EncounterSorceryRoutePolicy.qualityOfTerrestrialCharmCandidate(candidatoPuro, abilities),
                sorceryQuality = EncounterSorceryRoutePolicy.qualityOfTerrestrialCharmCandidate(candidatoFeiticaria, abilities)
            )
        } else feiticariaNecessaria
        val todosOsCharmsIniciais = if (selecionarFeiticaria) candidatoFeiticaria else candidatoPuro

        val nomesCharms = todosOsCharmsIniciais.map { it.nome }.toMutableSet()
        val charmsExcelencia = EncounterCharmSelectionService.selecionarExcelencias(
            catalogo = encantosConvertidos,
            abilities = abilities,
            essencia = essencia,
            nomesJaSelecionados = nomesCharms,
            quantidade = 5,
            habilidadeCombatePrincipal = focoEfetivo ?: habilidadeCombate
        )
        val todosOsCharms = todosOsCharmsIniciais + charmsExcelencia
        val charmsComCorpoDeTouro = todosOsCharms
        val corpoDeTouroInicial = charmsComCorpoDeTouro.count { it.nome == NOME_CORPO_DE_TOURO }

        // --- Méritos: imediatamente após Habilidades e antes de Ações. ---
        val preferirArtesMarciais = EncounterArchetypePolicy.devePreferirArtesMarciais(
            arquetipoEfetivo, abilities["Briga"] ?: 0, random
        )
        val merits = EncounterMeritDistributionService.distribuir(
            NpcEncontro(tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO, arquetipo = arquetipoEfetivo, arma = arma, armadura = armadura),
            meritosCatalogo, random, origemNome, preferirArtistaMarcial = preferirArtesMarciais || customizacao?.rotaArtesMarciais == true,
            exigirArtistaMarcial = customizacao?.rotaArtesMarciais == true
        )
        val abilitiesAjustadas = if (merits.any { it.nome.equals("Artista Marcial", ignoreCase = true) }) {
            abilities.toMutableMap().apply { this["Briga"] = 1 }.toMap()
        } else abilities

        val vigor = attributes["Vigor"] ?: 1
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(merits)

        val healthBoxes = EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, trilhaVitalidadeSangueDeDragaoPorVigor(vigor, corpoDeTouroInicial))

        val motes = EncounterMoteService.calcular(
            tipo = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            essencia = essencia,
            arma = arma,
            armadura = armadura
        )
        val motesPessoais = motes.pessoais
        val motesPerifericos = motes.perifericos
        val forcaDeVontade = forcaDeVontadeComBonus

        val combate = EncounterCombatCalculationService.calcular(
            arquetipo = arquetipoEfetivo,
            habilidadeCombate = habilidadeCombate,
            attributes = attributes,
            abilities = abilitiesAjustadas,
            arma = arma,
            armadura = armadura
        )
        val acaoPrincipal = combate.acaoPrincipal
        val acaoDecisiva = combate.acaoDecisiva
        val defesaPrimaria = combate.defesaPrimaria
        val esquivaFinal = combate.esquiva
        val dano = combate.dano


        val habilidadesComEspecialidade = especialidades.asSequence().map { it.habilidade }.toHashSet()
        val acessoFeiticaria = EncounterSorceryAccess.fromNames(charmsIniciais.asSequence().map { it.nome })
        val feiticosSelecionados = EncounterSpellSelectionService.selecionarParaArquetipo(
            arquetipo = arquetipoEfetivo,
            catalogoTerrestre = feiticos,
            catalogoCelestial = feiticos,
            catalogoSolar = feiticos,
            possuiFeiticariaTerrestre = acessoFeiticaria.terrestre,
            random = random,
            possuiFeiticariaCelestial = false,
            possuiFeiticariaSolar = false
        )
        val feiticosComGratuito = EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
            possuiFeiticariaTerrestre = acessoFeiticaria.terrestre,
            selecionados = feiticosSelecionados,
            catalogo = feiticos,
            random = random
        )
        val feiticoInicialNome = if (acessoFeiticaria.terrestre) {
            // A vaga gratuita precisa nascer identificada mesmo quando o
            // Feitiço escolhido já fazia parte da seleção normal.
            feiticosComGratuito.lastOrNull { it.circulo == "Terrestre" }?.nome
        } else null

        val idiomaInicial = EncounterLanguageService.selecionar(
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            origemNomeSangueDeDragao = origemNome,
            random = random
        )

        val rotaMarcialAtiva = customizacao?.rotaArtesMarciais == true ||
            merits.any { it.nome.equals("Artista Marcial", ignoreCase = true) && it.valor >= 4 }
        val selecaoMarcial = if (rotaMarcialAtiva) {
            EncounterMartialArtsSelectionService.selecionar(
                estilos = estilosArtesMarciais,
                tipo = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
                briga = abilitiesAjustadas["Briga"] ?: 0,
                essencia = essencia,
                random = random
            )
        } else null
        val (armaFinal, armaduraFinal) = selecaoMarcial?.let {
            EncounterEquipmentService.ajustarParaEstiloMarcial(it.estilo, arma, armadura, random)
        } ?: (arma to armadura)
        val encantosNpc = EncounterMartialArtsSelectionService.integrarMantendoQuantidade(
            charmsComCorpoDeTouro.map { EncantoEncontro(it.nome, it.habilidade, it.custo) },
            selecaoMarcial
        )
        val derivadosFinais = EncounterDerivedStatsService.calcularDerivadosComuns(
            attributes, abilitiesAjustadas, armaduraFinal,
            bonusPerseveranca = if ("Integridade" in habilidadesComEspecialidade) 1 else 0,
            bonusAstucia = if ("Socialização" in habilidadesComEspecialidade) 1 else 0,
            bonusJuntarBatalha = (if ("Prontidão" in habilidadesComEspecialidade) 1 else 0) + efeitosMeritos.bonusJuntarABatalha,
            bonusInvestida = (if ("Atletismo" in habilidadesComEspecialidade) 1 else 0) + efeitosMeritos.bonusInvestida,
            bonusDesengajamento = (if ("Esquiva" in habilidadesComEspecialidade) 1 else 0) + efeitosMeritos.bonusDesengajamento
        )


        val npcBase = NpcEncontro(
            nome = nome,
            genero = generoEscolhido.name,
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = arquetipoEfetivo,
            casta = aspecto.displayName, // reaproveita o campo "casta" pra guardar o Aspecto
            habilidadesFavorecidas = habilidadesFavorecidas,
            habilidadeSupernal = "",
            attributes = attributes,
            abilities = abilitiesAjustadas,
            merits = merits,
            especialidades = especialidades,
            habilidadePrincipal = habilidadeCombate,
            habilidadeDefensiva = habilidadeDefensivaObrigatoria,
            charms = encantosNpc,
            estiloArtesMarciais = selecaoMarcial?.estilo?.nomePt.orEmpty(),
            feiticos = feiticosComGratuito.map { com.example.model.FeiticoEncontro(nome = it.nome, circulo = it.circulo, custo = it.custo) },
            feiticoInicialNome = feiticoInicialNome,
            corpoDeTouroCount = corpoDeTouroInicial,
            essencia = essencia,
            idioma = idiomaInicial,
            motesPersonais = motesPessoais,
            motesPerifericos = motesPerifericos,
            forcaDeVontade = forcaDeVontade,
            xpAtual = 0,
            xpGastoTotal = 0,
            arma = armaFinal,
            armadura = armaduraFinal,
            acaoPrincipal = acaoPrincipal,
            acaoDecisiva = acaoDecisiva,
            defesaPrimaria = defesaPrimaria,
            esquiva = esquivaFinal,
            absorcaoNatural = derivadosFinais.absorcaoNatural,
            absorcaoArmadura = armaduraFinal.absorcao,
            absorcao = derivadosFinais.absorcaoTotal,
            dureza = armaduraFinal.dureza,
            perseveranca = derivadosFinais.perseveranca,
            astucia = derivadosFinais.astucia,
            juntarABatalha = derivadosFinais.juntarABatalha,
            investida = derivadosFinais.investida,
            desengajamento = derivadosFinais.desengajamento,
            healthBoxes = healthBoxes,
            iniciativaAtual = 0, // pedido explícito do usuário: contador começa zerado (substitui a instrução anterior de começar em 3)
            dano = dano,
            focoProgressaoExplicito = customizacao?.focoExplicito,
            alertasValidacao = EncounterValidationService.validar(
                arquetipo = arquetipoEfetivo,
                attributes = attributes,
                charms = encantosNpc.map { it.nome },
                feiticos = feiticosComGratuito
            )
        )
        val audit = EncounterNpcAuditor.audit(
            npcBase, sangueDeDragao = encantosSangueDeDragao, feiticos = feiticos, estilosMarciais = estilosArtesMarciais
        )
        val npcFinal = if (audit.issues.isEmpty()) npcBase else npcBase.copy(
            alertasValidacao = (npcBase.alertasValidacao + audit.issues.asLegacyMessages()).distinct()
        )
        val npcComFeiticoInicial = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            npc = npcFinal,
            catalogo = feiticos,
            random = random
        )
        EncounterValidationService.validarIdentidadeEstrutural(npcComFeiticoInicial)
        return npcComFeiticoInicial
    }

    // --- Corpo de Touro (Sangue de Dragão) — regra própria, diferente da
    // Solar: Vigor 1-2: dois níveis -2. Vigor 3-4: um -1 e um -2. Vigor 5:
    // um -1 e dois -2. Repetível (Resistência) vezes — pedido explícito
    // do usuário, com a mecânica descrita por ele mesmo.
    fun trilhaVitalidadeSangueDeDragaoPorVigor(vigor: Int, corpoDeTouroCount: Int): List<CaixaVitalidade> =
        EncounterVitalityService.trilhaVitalidadeSangueDeDragao(vigor, corpoDeTouroCount)

    // Custo de Encanto pra Sangue de Dragão: 8 XP para as Habilidades do
    // Aspecto ou Favorecidas reais; 10 XP para as demais. As duas categorias
    // continuam armazenadas separadamente no NPC.
    private fun custoXpEncantoSangueDeDragao(
        habilidade: String,
        habilidadesFavorecidas: List<String>,
        aspecto: String
    ): Int {
        val habilidadesAspecto = Aspecto.entries
            .firstOrNull { it.displayName == aspecto }
            ?.allowedAbilities()
            .orEmpty()
        return if (habilidade in habilidadesFavorecidas || habilidade in habilidadesAspecto) 8 else 10
    }

    // Progressão por XP de Sangue de Dragão — mesmo esqueleto de
    // expandirEncantosPorExperiencia (Solar), mas com catálogo, custo e
    // prioridade próprios. Prioridade (pedido explícito do usuário): se o
    // perfil for físico ("guerreiro"), usa primeiro a habilidade de
    // combate escolhida, depois as habilidades do Aspecto; nos outros
    // perfis, começa direto pelas habilidades do Aspecto (não há
    // Supernal aqui pra vir antes de tudo, como no Solar).
    internal fun criarExpansorXpPreparado(
        encantosSangueDeDragao: List<com.example.data.EncantoSangueDeDragaoDefinition>,
        npcBase: NpcEncontro
    ): EncounterXpExpander = criarExpansorXpComCatalogoConvertido(
        catalogoConvertidoCompleto = catalogoConvertido(encantosSangueDeDragao),
        npcBase = npcBase
    )

    internal fun criarExpansorXpComCatalogoConvertido(
        catalogoConvertidoCompleto: List<EncantoSolarDefinition>,
        npcBase: NpcEncontro
    ): EncounterXpExpander {
        val habilidadesAspecto = Aspecto.entries.firstOrNull { it.displayName == npcBase.casta }
            ?.let { habilidadesPorAspecto[it] }
            .orEmpty()
        val habilidadesCustoFavorecido = buildSet {
            addAll(npcBase.habilidadesFavorecidas)
            addAll(habilidadesAspecto)
        }
        // Casta/Aspecto e Favorecidas fazem parte do perfil imutável usado para
        // cachear este expansor. Normalizar esses conjuntos aqui evita recriá-los
        // em cada clique de +XP; selectedNames e Essência continuam dinâmicos.
        val favorecidasNormalizadas = npcBase.habilidadesFavorecidas.asSequence()
            .map { it.lowercase() }
            .toHashSet()
        val habilidadesAspectoNormalizadas = habilidadesAspecto.asSequence()
            .map { it.lowercase() }
            .toHashSet()
        // Assim como no caminho Lunar, estruturas invariantes do catálogo
        // pertencem ao expansor preparado, não a cada passo do roadmap.
        val catalogoPorHabilidadeCompleto = catalogoConvertidoCompleto.groupBy { it.habilidade }
        val catalogoRotasCompleto = EncounterCharmRouteOptimizer.prepareCatalog(
            catalogoCompleto = catalogoConvertidoCompleto,
            nome = { it.nome },
            categoria = { it.habilidade }
        )
        data class SignatureCatalogKey(
            val selectedSignatureNames: Set<String>,
            val essence: Int,
            val aspecto: String,
            val favorecidas: List<String>
        )
        data class PreparedSignatureCatalog(
            val catalogo: List<EncantoSolarDefinition>,
            val porHabilidade: Map<String, List<EncantoSolarDefinition>>,
            val rotas: EncounterCharmRouteOptimizer.PreparedCatalog<EncantoSolarDefinition>
        )
        // Janela curta do roadmap (3 passos): o mesmo subconjunto pode reaparecer
        // enquanto o saldo muda sem alterar Essência/seleção. Cache local evita
        // preparar novamente esse catálogo filtrado e morre junto com o expansor.
        val catalogosFiltradosPreparados = HashMap<SignatureCatalogKey, PreparedSignatureCatalog>()
        return EncounterXpExpander { npc ->
            val ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(npc)
            val nomesSelecionados = HashSet<String>(npc.charms.size * 2).apply {
                npc.charms.forEach { add(it.nome) }
            }
            // Somente Assinaturas já possuídas podem alterar este filtro.
            // Compras de Encantos comuns deixam de invalidar/reconstruir groupBy
            // e PreparedCatalog sem mudar a política mecânica.
            val assinaturasSelecionadas = DragonBloodedSignaturePolicy.selectedSignatureNames(
                catalogoConvertidoCompleto,
                nomesSelecionados
            )
            val chaveFiltro = SignatureCatalogKey(
                selectedSignatureNames = assinaturasSelecionadas,
                essence = npc.essencia,
                aspecto = npc.casta,
                favorecidas = npc.habilidadesFavorecidas
            )
            val preparado = catalogosFiltradosPreparados.getOrPut(chaveFiltro) {
                val filtrado = DragonBloodedSignaturePolicy.filterForProgression(
                    catalog = catalogoConvertidoCompleto,
                    selectedNames = nomesSelecionados,
                    essence = npc.essencia,
                    aspecto = npc.casta,
                    favorecidas = npc.habilidadesFavorecidas,
                    favoredNormalizedPrepared = favorecidasNormalizadas,
                    aspectAbilitiesNormalizedPrepared = habilidadesAspectoNormalizadas
                )
                val inalterado = filtrado.size == catalogoConvertidoCompleto.size &&
                    filtrado.indices.all { filtrado[it] === catalogoConvertidoCompleto[it] }
                if (inalterado) {
                    PreparedSignatureCatalog(filtrado, catalogoPorHabilidadeCompleto, catalogoRotasCompleto)
                } else {
                    PreparedSignatureCatalog(
                        catalogo = filtrado,
                        porHabilidade = filtrado.groupBy { it.habilidade },
                        rotas = EncounterCharmRouteOptimizer.prepareCatalog(
                            catalogoCompleto = filtrado,
                            nome = { it.nome },
                            categoria = { it.habilidade }
                        )
                    )
                }
            }
            EncounterExperienceService.expandWithBatch(
                npc = npc,
                catalogo = preparado.catalogo,
                ordemHabilidades = ordemHabilidades,
                custoEncanto = { habilidade ->
                    if (habilidade in habilidadesCustoFavorecido) 8 else 10
                },
                trilhaVitalidade = ::trilhaVitalidadeSangueDeDragaoPorVigor,
                habilidadeCombate = npc.habilidadePrincipal,
                catalogoPorHabilidadePreparado = preparado.porHabilidade,
                catalogoRotasPreparado = preparado.rotas
            )
        }
    }

    fun expandirEncantosPorExperienciaSangueDeDragaoComBatch(
        npc: NpcEncontro,
        encantosSangueDeDragao: List<com.example.data.EncantoSangueDeDragaoDefinition>
    ): ExpansionResult {
        val ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(npc)
        val catalogoConvertidoCompleto = catalogoConvertido(encantosSangueDeDragao)
        val catalogoConvertido = DragonBloodedSignaturePolicy.filterForProgression(
            catalog = catalogoConvertidoCompleto,
            selectedNames = npc.charms.asSequence().map { it.nome }.toSet(),
            essence = npc.essencia,
            aspecto = npc.casta,
            favorecidas = npc.habilidadesFavorecidas
        )
        val habilidadesCustoFavorecido = buildSet {
            addAll(npc.habilidadesFavorecidas)
            Aspecto.entries.firstOrNull { it.displayName == npc.casta }
                ?.let { habilidadesPorAspecto[it] }
                ?.let(::addAll)
        }
        return EncounterExperienceService.expandWithBatch(
            npc = npc,
            catalogo = catalogoConvertido,
            ordemHabilidades = ordemHabilidades,
            custoEncanto = { habilidade ->
                if (habilidade in habilidadesCustoFavorecido) 8 else 10
            },
            trilhaVitalidade = ::trilhaVitalidadeSangueDeDragaoPorVigor,
            habilidadeCombate = npc.habilidadePrincipal
        )
    }

    fun expandirEncantosPorExperienciaSangueDeDragao(
        npc: NpcEncontro,
        encantosSangueDeDragao: List<com.example.data.EncantoSangueDeDragaoDefinition>
    ): NpcEncontro {
        val ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(npc)

        // Conversão para o formato compartilhado ocorre uma única vez por lote.
        val catalogoConvertidoCompleto = catalogoConvertido(encantosSangueDeDragao)
        val catalogoConvertido = DragonBloodedSignaturePolicy.filterForProgression(
            catalog = catalogoConvertidoCompleto,
            selectedNames = npc.charms.asSequence().map { it.nome }.toSet(),
            essence = npc.essencia,
            aspecto = npc.casta,
            favorecidas = npc.habilidadesFavorecidas
        )
        return EncounterExperienceService.expand(
            npc = npc,
            catalogo = catalogoConvertido,
            ordemHabilidades = ordemHabilidades,
            custoEncanto = { habilidade ->
                custoXpEncantoSangueDeDragao(habilidade, npc.habilidadesFavorecidas, npc.casta)
            },
            trilhaVitalidade = ::trilhaVitalidadeSangueDeDragaoPorVigor,
            habilidadeCombate = npc.habilidadePrincipal
        )
    }

    // --- Reduz Experiência: a implementação vive no serviço compartilhado.
    // A fachada mantém a API da classe e deixa a diferença Solar/Sangue de Dragão
    // confinada às políticas de progressão.
}
