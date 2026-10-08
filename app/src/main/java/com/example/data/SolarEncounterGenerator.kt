package com.example.data

import com.example.model.*
import kotlin.random.Random

internal object SolarEncounterGenerator {
    private val habilidadesPorCasta: Map<Casta, List<String>> =
        Casta.entries.associateWith { it.allowedAbilities() }
    private val habilidadesSetPorCasta: Map<Casta, Set<String>> =
        habilidadesPorCasta.mapValues { (_, habilidades) -> habilidades.toSet() }
    private val castasPorHabilidade: Map<String, List<Casta>> =
        ExaltedConstants.ALL_25_ABILITIES.associateWith { habilidade ->
            Casta.entries.filter { habilidade in habilidadesPorCasta.getValue(it) }
        }
    private val categoriasPorArquetipo: Map<ArquetipoEncontro, Set<String>> = mapOf(
        ArquetipoEncontro.FISICO to setOf("Armas Brancas", "Briga", "Esquiva", "Arremesso", "Arqueirismo"),
        ArquetipoEncontro.SOCIAL to setOf("Presença", "Performance", "Socialização"),
        ArquetipoEncontro.MENTAL to setOf("Ocultismo", "Conhecimento", "Medicina")
    )
    private val castasPorArquetipo: Map<ArquetipoEncontro, List<Casta>> =
        categoriasPorArquetipo.mapValues { (_, categoria) ->
            Casta.entries.filter { casta -> habilidadesPorCasta.getValue(casta).any { it in categoria } }
        }

    fun gerarNome(cultura: CulturaNome? = null, genero: GeneroNome? = null, random: Random = Random.Default): String =
        NameGenerator.gerarSolarOuLunar(cultura, genero, random)
    fun gerar(
        nomeManual: String,
        arquetipo: ArquetipoEncontro,
        encantosSolares: List<EncantoSolarDefinition>,
        feiticos: List<com.example.data.FeiticoDefinition> = emptyList(),
        culturaNome: CulturaNome? = null,
        generoNome: GeneroNome? = null,
        random: Random = Random.Default,
        meritosCatalogo: List<MeritoDefinition> = emptyList(),
        focoPersonalizado: String? = null,
        customizacao: EncounterCustomization? = null,
        estilosArtesMarciais: List<EstiloArteMarcialDefinition> = emptyList()
    ): NpcEncontro {
        val focoEfetivoBruto = customizacao?.focoEfetivo(focoPersonalizado) ?: focoPersonalizado.takeIf { customizacao == null }
        val focoFeiticariaExplicito = customizacao?.rotaFeiticaria == true || focoEfetivoBruto == ENCOUNTER_FOCUS_SORCERY
        val focoEfetivo = customizacao?.focoMecanico(focoPersonalizado, "Ocultismo")
            ?: if (focoFeiticariaExplicito) "Ocultismo" else focoEfetivoBruto
        val arquetipoEfetivo = arquetipo
        val generoEscolhido = generoNome ?: GeneroNome.entries.random(random)
        val nome = nomeManual.trim().ifBlank { gerarNome(culturaNome, generoEscolhido, random) }

        // --- 4. Casta e habilidades ---
        // Pedido explícito do usuário (mesma lógica já aplicada ao
        // Lunar): enviesar a Casta pra uma que tenha pelo menos uma
        // Habilidade da categoria do arquétipo, aumentando a chance da
        // Supernal se encaixar naturalmente (em vez de cair de volta pro
        // sorteio aleatório na escolha da Habilidade principal).
        val categoriaHabilidadeArquetipo = categoriasPorArquetipo.getValue(arquetipoEfetivo)
        val castasDoFoco = focoEfetivo?.let { castasPorHabilidade[it] }.orEmpty()
        val castasCompativeis = castasPorArquetipo.getValue(arquetipoEfetivo)
        val casta = (castasDoFoco.ifEmpty { castasCompativeis }.ifEmpty { Casta.entries.toList() }).random(random)
        val habilidadesCasta = habilidadesPorCasta.getValue(casta)
        val habilidadesCastaSet = habilidadesSetPorCasta.getValue(casta)
        val supernal = focoEfetivo?.takeIf { it in habilidadesCasta }
            ?: (habilidadesCasta.filter { it in categoriaHabilidadeArquetipo }.ifEmpty { habilidadesCasta }).random(random)
        // 5 adicionais entre TODAS as habilidades ainda não selecionadas,
        // incluindo as restantes da casta quando elegíveis — sorteia
        // primeiro entre as da casta que sobraram, depois completa fora
        // dela se precisar, dando prioridade natural à própria casta.
        val restantesCasta = habilidadesCasta.filter { it != supernal }.shuffled(random)
        val foraDaCasta = ExaltedConstants.ALL_25_ABILITIES.filter { it !in habilidadesCastaSet }.shuffled(random)
        val adicionais = (restantesCasta + foraDaCasta).take(4)
        val habilidadesFavorecidas = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOfNotNull(focoEfetivo, supernal),
            fallback = adicionais + foraDaCasta
        ).take(5)

        // --- 5. Atributos: primário +8, secundário +6, terciário +4 ---
        val (primarios, secundarios, terciarios) = EncounterGenerationRules.gruposDeAtributoPara(arquetipoEfetivo, random)
        var attributes = EncounterDistributionService.distribuirAtributos(primarios, secundarios, terciarios, random, arquetipo)

        // --- 6.2/7. Habilidade de combate + defensiva obrigatória ---
        // Pedido explícito do usuário: o arquétipo sempre segue a
        // Habilidade Supernal quando ela se encaixa — evita que o Solar
        // fique com dois focos concorrendo (Supernal de um lado, foco do
        // arquétipo de outro) diluindo a especialização.
        val habilidadeCombate = EncounterGenerationRules.resolverHabilidadeCombate(customizacao?.ataqueExplicito, customizacao?.focoParaAtaque(focoEfetivo) ?: focoEfetivo.takeIf { customizacao == null }, supernal, random)
        val habilidadeDefensivaObrigatoria = customizacao?.defesaExplicita ?: EncounterGenerationRules.habilidadeDefensivaPara(habilidadeCombate)
        // Restrições de Força/Destreza conforme a Habilidade de combate (Aba 11).
        attributes = EncounterDistributionService.ajustarAtributosPorHabilidadeCombate(
            attributes, habilidadeCombate, random
        )
        EncounterValidationService.validarDistribuicaoBaseDeAtributos(attributes, arquetipoEfetivo)

        // --- 6. Distribuição de habilidades (28 pontos normais + Pontos de Bônus) ---
        val metadataEncantos = EncounterCharmSelectionService.catalogMetadata(encantosSolares)
        val habilidadesComEncantos = metadataEncantos.habilidades
        val definicaoCorpoDeTouro = metadataEncantos.corpoDeTouro
        val habilidadesCastaRelevantes = habilidadesCasta.filter { it in habilidadesComEncantos }
        val resultado = EncounterDistributionService.distribuirHabilidades(
            arquetipoEfetivo,
            habilidadeCombate,
            habilidadeDefensivaObrigatoria,
            supernal,
            habilidadesFavorecidas,
            random,
            habilidadesEstruturaisRelevantes = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOfNotNull(focoEfetivo, customizacao?.secundariaExplicita),
                exaltStructure = habilidadesCastaRelevantes
            ),
            habilidadesMinimoUm = if (arquetipoEfetivo == ArquetipoEncontro.MENTAL) setOf("Linguística") else emptySet()
        )
        val (abilities, forcaDeVontadeComBonus, precisaEspecialidadeAdicional) = EncounterDistributionService.distribuirPontosDeBonus(
            arquetipo = arquetipoEfetivo,
            abilitiesBase = resultado.abilities,
            habilidadesFavorecidasOuCasta = habilidadesFavorecidas,
            forcaDeVontadeBase = 5,
            ordemPrioridade = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOfNotNull(focoEfetivo, customizacao?.secundariaExplicita),
                archetype = EncounterArchetypePolicy.abilityPriority(
                    archetype = arquetipoEfetivo,
                    combat = habilidadeCombate,
                    support = resultado.habilidadeSocialOuMental,
                    favored = habilidadesFavorecidas,
                    supernal = supernal
                )
            ),
            habilidadeCombate = habilidadeCombate,
            habilidadesEstruturaisRelevantes = habilidadesCastaRelevantes
        )

        // --- 8. Especialidades (4, formato "Habilidade (+1)") ---
        var especialidades = EncounterDistributionService.distribuirEspecialidades(
            arquetipoEfetivo, habilidadeCombate, habilidadeDefensivaObrigatoria, resultado.habilidadeSocialOuMental, abilities, random,
            habilidadesEstruturaisRelevantes = habilidadesCastaRelevantes
        )
        especialidades = EncounterDistributionService.adicionarEspecialidadeAdicional(
            especialidades,
            abilities,
            EncounterArchetypePolicy.abilityPriority(
                archetype = arquetipoEfetivo,
                combat = habilidadeCombate,
                support = resultado.habilidadeSocialOuMental,
                favored = habilidadesFavorecidas,
                supernal = supernal
            ),
            precisaEspecialidadeAdicional
        )

        val essencia = EncounterGenerationRules.ESSENCIA_SOLAR

        // --- 9. Equipamento ---
        val (arma, armadura) = EncounterEquipmentService.selecionarEquipamentoParaEncontro(
            habilidadeCombate = habilidadeCombate, random = random, permitirDoisArtefatos = true
        )

        // --- Encantos: NPC Solar nasce com 15 Encantos (correção explícita
        // do usuário — a versão anterior só dava Corpo de Touro quando
        // elegível, o que estava errado). Prioriza Supernal, depois a
        // habilidade de combate/defensiva/suporte, depois o resto das
        // favorecidas, sempre do mais barato (menor Essência mínima) pro
        // mais caro, respeitando pré-requisitos — igual à lógica de
        // elegibilidade usada na progressão por XP (função elegivel()).
        // Encantos seguem o foco do arquétipo. Físico concentra fortemente
        // no ataque escolhido; Social/Mental deixam essa habilidade em
        // prioridade baixa, evitando que a seleção consuma o catálogo de
        // Encantos de combate quando o conceito principal não é físico.
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
        // O foco escolhido na Aba 11 deve comandar a distribuição de Encantos.
        // Depois dele, priorizamos as Habilidades efetivamente mais altas da ficha.
        // Assim, mudar o foco recalcula imediatamente a rota de Encantos em vez de
        // reaproveitar uma prioridade genérica do arquétipo.
        val habilidadesPorEficiencia = abilities.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        val ordemHabilidadesIniciais = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOfNotNull(focoEfetivo),
            efficiency = habilidadesPorEficiencia,
            archetype = EncounterArchetypePolicy.charmPriority(
                    arquetipoEfetivo, habilidadeCombate, resultado.habilidadeSocialOuMental,
                    habilidadesFavorecidas, supernal, priorizarOcultismo = priorizarOcultismo
                )
        )
        // Arquétipo Mental: a rota de Feitiçaria só é sorteada quando a ficha
        // já possui Ocultismo 3+, que é o pré-requisito real da Feitiçaria
        // Terrestre. Isso evita reservar cinco vagas para uma rota impossível
        // e devolver silenciosamente apenas 10 Encantos. Quando a rota é
        // sorteada, os 4 Encantos de Ocultismo são adicionados pelo projeto
        // final e a seleção comum recebe as vagas restantes do orçamento.
        // Mantemos o sorteio no mesmo ponto do fluxo para não alterar a
        // sequência do Random; a decisão só é aplicada quando a Habilidade
        // possui o pré-requisito real da Feitiçaria.
        val circuloTerrestreDisponivel = metadataEncantos.feiticariaTerrestre != null
        val feiticariaParaReservar = EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject(
            archetype = arquetipoEfetivo,
            exploreSorcery = explorarFeiticaria,
            abilities = abilities,
            terrestrialCircleAvailable = circuloTerrestreDisponivel
        )
        val catalogoParaSelecao = if (feiticariaParaReservar) {
            metadataEncantos.semFeiticariaTerrestre
        } else encantosSolares
        val charmsSemGarantia = EncounterCharmSelectionService.selecionarIniciais(
            catalogo = catalogoParaSelecao,
            abilities = abilities,
            essencia = essencia,
            ordemHabilidades = ordemHabilidadesIniciais,
            quantidade = if (feiticariaParaReservar) {
                (EncounterGenerationRules.ENCANTOS_INICIAIS - EncounterGenerationRules.QUANTIDADE_OCULTISMO_FEITICARIA - 1).coerceAtLeast(0)
            } else EncounterGenerationRules.ENCANTOS_INICIAIS,
            habilidadeCombatePrincipal = habilidadeCombate,
            habilidadePrincipalArquetipo = focoEfetivo ?: when (arquetipoEfetivo) {
                ArquetipoEncontro.FISICO -> habilidadeCombate
                ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL -> resultado.habilidadeSocialOuMental
            },
            focoEscolhidoPeloUsuario = focoEfetivo != null,
            minimosHabilidadesAdicionais = if (feiticariaParaReservar) {
                mapOf(EncounterGenerationRules.HABILIDADE_OCULTISMO to 4)
            } else emptyMap()
        ).let { lista ->
            val vistos = mutableSetOf<String>()
            lista.filter { it.nome == NOME_CORPO_DE_TOURO || vistos.add(it.nome) }
        }
        // Garantir pelo menos 1 Corpo de Touro — pedido explícito dos
        // documentos de Arquétipo: os 3 arquétipos precisam de pelo menos
        // 1 aquisição, não só o Físico. Antes só Sangue de Dragão e Lunar
        // tinham essa garantia explícita.
        val quantidadeBase = if (feiticariaParaReservar) {
            (EncounterGenerationRules.ENCANTOS_INICIAIS - EncounterGenerationRules.QUANTIDADE_OCULTISMO_FEITICARIA - 1).coerceAtLeast(0)
        } else EncounterGenerationRules.ENCANTOS_INICIAIS
        val charmsIniciaisBase = if (charmsSemGarantia.any { it.nome == NOME_CORPO_DE_TOURO }) charmsSemGarantia else {
            val definicaoCorpo = definicaoCorpoDeTouro
            if (definicaoCorpo != null && charmsSemGarantia.size < quantidadeBase) charmsSemGarantia + definicaoCorpo
            else if (definicaoCorpo != null && charmsSemGarantia.isNotEmpty()) charmsSemGarantia.dropLast(1) + definicaoCorpo
            else charmsSemGarantia
        }

        // Físico com Ocultismo 3+ recebe obrigatoriamente Feitiçaria do
        // Círculo Terrestre. A seleção de Ocultismo foi elevada na prioridade
        // acima para formar os 4 Encantos necessários antes da garantia.
        val candidatoFeiticaria = EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre(
            catalogo = encantosSolares,
            selecionados = charmsIniciaisBase,
            abilities = abilities,
            essencia = essencia,
            quantidadeTotal = EncounterGenerationRules.ENCANTOS_INICIAIS,
            exigirProjeto = feiticariaParaReservar
        )
        val compararCandidatosFeiticaria =
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                archetype = arquetipoEfetivo,
                exploreSorcery = explorarFeiticaria,
                explicitSorceryFocus = focoFeiticariaExplicito,
                sorceryConstructible = feiticariaParaReservar
            )
        val candidatoPuro = if (compararCandidatosFeiticaria) {
            val ordemPura = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOfNotNull(focoEfetivo),
                efficiency = habilidadesPorEficiencia,
                archetype = EncounterArchetypePolicy.charmPriority(
                    arquetipoEfetivo, habilidadeCombate, resultado.habilidadeSocialOuMental,
                    habilidadesFavorecidas, supernal, priorizarOcultismo = false
                )
            )
            EncounterCharmSelectionService.selecionarIniciais(
                catalogo = encantosSolares,
                abilities = abilities,
                essencia = essencia,
                ordemHabilidades = ordemPura,
                quantidade = EncounterGenerationRules.ENCANTOS_INICIAIS,
                habilidadeCombatePrincipal = habilidadeCombate,
                habilidadePrincipalArquetipo = focoEfetivo ?: resultado.habilidadeSocialOuMental,
                focoEscolhidoPeloUsuario = focoEfetivo != null
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
        } else feiticariaParaReservar
        val charmsIniciais = if (selecionarFeiticaria) candidatoFeiticaria else candidatoPuro

        // --- Méritos: imediatamente após Habilidades e antes de Ações. ---
        val preferirArtesMarciais = EncounterArchetypePolicy.devePreferirArtesMarciais(
            arquetipoEfetivo, abilities["Briga"] ?: 0, random
        )
        val merits = EncounterMeritDistributionService.distribuir(
            NpcEncontro(tipoExaltado = TipoExaltadoEncontro.SOLAR, arquetipo = arquetipoEfetivo, arma = arma, armadura = armadura),
            meritosCatalogo, random, preferirArtistaMarcial = preferirArtesMarciais || customizacao?.rotaArtesMarciais == true,
            exigirArtistaMarcial = customizacao?.rotaArtesMarciais == true
        )
        val abilitiesAjustadas = if (merits.any { it.nome.equals("Artista Marcial", ignoreCase = true) }) {
            abilities.toMutableMap().apply { this["Briga"] = 1 }.toMap()
        } else abilities

        // --- 3/12/13. Motes, Força de Vontade, trilha de vitalidade ---
        val vigor = attributes["Vigor"] ?: 1
        val corpoDeTouroInicial = charmsIniciais.count { it.nome == NOME_CORPO_DE_TOURO }
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(merits)

        val healthBoxes = EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, trilhaVitalidadePorVigor(vigor, corpoDeTouroCount = corpoDeTouroInicial))
        val motes = EncounterMoteService.calcular(
            tipo = TipoExaltadoEncontro.SOLAR,
            essencia = essencia,
            arma = arma,
            armadura = armadura
        )
        val motesPersonais = motes.pessoais
        val motesPerifericos = motes.perifericos
        val forcaDeVontade = forcaDeVontadeComBonus + (essencia - 3).coerceAtLeast(0)

        // --- 10/11. Combate e derivados ---
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

        val acessoFeiticaria = EncounterSorceryAccess.fromNames(charmsIniciais.asSequence().map { it.nome })
        val feiticosSelecionados = EncounterSpellSelectionService.selecionarParaArquetipo(
            arquetipo = arquetipoEfetivo,
            catalogoTerrestre = feiticos,
            catalogoCelestial = feiticos,
            catalogoSolar = feiticos,
            possuiFeiticariaTerrestre = acessoFeiticaria.terrestre,
            random = random,
            possuiFeiticariaCelestial = acessoFeiticaria.celestial,
            possuiFeiticariaSolar = acessoFeiticaria.solar
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

        val habilidadesComEspecialidade = especialidades.asSequence().map { it.habilidade }.toHashSet()
        val temEspecialidadeIntegridade = "Integridade" in habilidadesComEspecialidade
        val temEspecialidadeSocializacao = "Socialização" in habilidadesComEspecialidade
        val temEspecialidadeAtletismo = "Atletismo" in habilidadesComEspecialidade
        val temEspecialidadeEsquiva = "Esquiva" in habilidadesComEspecialidade
        val limiteInicial = EncounterLimitCatalog.sortear(TipoExaltadoEncontro.SOLAR, casta.displayName, random)
        val idiomaInicial = EncounterLanguageService.selecionar(TipoExaltadoEncontro.SOLAR, random = random)

        val rotaMarcialAtiva = customizacao?.rotaArtesMarciais == true ||
            merits.any { it.nome.equals("Artista Marcial", ignoreCase = true) && it.valor >= 4 }
        val selecaoMarcial = if (rotaMarcialAtiva) {
            EncounterMartialArtsSelectionService.selecionar(
                estilos = estilosArtesMarciais,
                tipo = TipoExaltadoEncontro.SOLAR,
                briga = abilitiesAjustadas["Briga"] ?: 0,
                essencia = essencia,
                random = random
            )
        } else null
        val (armaFinal, armaduraFinal) = selecaoMarcial?.let {
            EncounterEquipmentService.ajustarParaEstiloMarcial(it.estilo, arma, armadura, random)
        } ?: (arma to armadura)
        val encantosNpc = EncounterMartialArtsSelectionService.integrarMantendoQuantidade(
            charmsIniciais.map { EncantoEncontro(it.nome, it.habilidade, it.custo) },
            selecaoMarcial
        )
        val derivadosFinais = EncounterDerivedStatsService.calcularDerivadosComuns(
            attributes, abilitiesAjustadas, armaduraFinal,
            bonusPerseveranca = if (temEspecialidadeIntegridade) 1 else 0,
            bonusAstucia = if (temEspecialidadeSocializacao) 1 else 0,
            bonusJuntarBatalha = 1 + efeitosMeritos.bonusJuntarABatalha,
            bonusInvestida = (if (temEspecialidadeAtletismo) 1 else 0) + efeitosMeritos.bonusInvestida,
            bonusDesengajamento = (if (temEspecialidadeEsquiva) 1 else 0) + efeitosMeritos.bonusDesengajamento
        )


        val npcBase = NpcEncontro(
            nome = nome,
            genero = generoEscolhido.name,
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = arquetipoEfetivo,
            casta = casta.displayName,
            habilidadesFavorecidas = habilidadesFavorecidas,
            habilidadeSupernal = supernal,
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
            limite = limiteInicial,
            idioma = idiomaInicial,
            motesPersonais = motesPersonais,
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
            npcBase, solares = encantosSolares, feiticos = feiticos, estilosMarciais = estilosArtesMarciais
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

    fun trilhaVitalidadePorVigor(vigor: Int, corpoDeTouroCount: Int): List<CaixaVitalidade> =
        EncounterVitalityService.trilhaVitalidadeSolar(vigor, corpoDeTouroCount)

    // --- 13. Trilha de vitalidade por Vigor, mais caixas extras por
    // Técnica do Corpo de Touro (repetível). ---
    fun gerarNome(random: Random = Random.Default): String = gerarNome(null, null, random)


    // Custo de Encanto em XP: Encantos Supernais/favorecidos (Casta) custam
    // 8 XP; Encantos não favorecidos custam 10 XP.
    private fun custoXpEncanto(
        habilidade: String,
        supernal: String,
        favorecidas: List<String>,
        casta: String
    ): Int {
        val habilidadesCasta = Casta.entries
            .firstOrNull { it.displayName == casta }
            ?.allowedAbilities()
            .orEmpty()
        return if (habilidade == supernal || habilidade in favorecidas || habilidade in habilidadesCasta) 8 else 10
    }

    internal fun criarExpansorXpPreparado(
        encantosSolares: List<EncantoSolarDefinition>
    ): EncounterXpExpander {
        val catalogoPorHabilidade = encantosSolares.groupBy { it.habilidade }
        val catalogoPreparado = EncounterCharmRouteOptimizer.prepareCatalog(
            catalogoCompleto = encantosSolares,
            nome = { it.nome },
            categoria = { it.habilidade }
        )
        // allowedAbilities() depende apenas da Casta. Cache local ao expansor:
        // evita remontar a mesma lista em cliques sucessivos sem tornar o
        // estado global nem assumir que a identidade do NPC nunca muda.
        val habilidadesPorCasta = HashMap<String, List<String>>()
        data class SolarXpCostProfile(
            val casta: String,
            val supernal: String,
            val favorecidas: List<String>
        )
        val habilidadesCustoPorPerfil = HashMap<SolarXpCostProfile, Set<String>>()
        return EncounterXpExpander { npc ->
            val ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(npc)
            val perfilCusto = SolarXpCostProfile(
                casta = npc.casta,
                supernal = npc.habilidadeSupernal,
                favorecidas = npc.habilidadesFavorecidas
            )
            val habilidadesCustoFavorecido = habilidadesCustoPorPerfil.getOrPut(perfilCusto) {
                val habilidadesCasta = habilidadesPorCasta.getOrPut(npc.casta) {
                    Casta.entries.firstOrNull { it.displayName == npc.casta }
                        ?.allowedAbilities()
                        .orEmpty()
                }
                buildSet {
                    add(npc.habilidadeSupernal)
                    addAll(npc.habilidadesFavorecidas)
                    addAll(habilidadesCasta)
                }
            }
            // A ordem de prioridade pode mudar com o estado do NPC, mas
            // para um mesmo perfil ela é estável entre os passos curtos do
            // roadmap. O serviço recebe os índices invariantes já preparados,
            // evitando reconstruir groupBy/rotas a cada passo.
            EncounterExperienceService.expandWithBatch(
                npc = npc,
                catalogo = encantosSolares,
                ordemHabilidades = ordemHabilidades,
                custoEncanto = { habilidade -> if (habilidade in habilidadesCustoFavorecido) 8 else 10 },
                trilhaVitalidade = ::trilhaVitalidadePorVigor,
                habilidadeCombate = npc.habilidadePrincipal,
                catalogoPorHabilidadePreparado = catalogoPorHabilidade,
                catalogoRotasPreparado = catalogoPreparado
            )
        }
    }

    fun expandirEncantosPorExperienciaComBatch(
        npc: NpcEncontro,
        encantosSolares: List<EncantoSolarDefinition>
    ): ExpansionResult {
        val ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(npc)
        return EncounterExperienceService.expandWithBatch(
            npc = npc,
            catalogo = encantosSolares,
            ordemHabilidades = ordemHabilidades,
            custoEncanto = { habilidade ->
                custoXpEncanto(habilidade, npc.habilidadeSupernal, npc.habilidadesFavorecidas, npc.casta)
            },
            trilhaVitalidade = ::trilhaVitalidadePorVigor,
            habilidadeCombate = npc.habilidadePrincipal
        )
    }

    fun expandirEncantosPorExperiencia(
        npc: NpcEncontro,
        encantosSolares: List<EncantoSolarDefinition>
    ): NpcEncontro {
        val ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(npc)

        return EncounterExperienceService.expand(
            npc = npc,
            catalogo = encantosSolares,
            ordemHabilidades = ordemHabilidades,
            custoEncanto = { habilidade ->
                custoXpEncanto(habilidade, npc.habilidadeSupernal, npc.habilidadesFavorecidas, npc.casta)
            },
            trilhaVitalidade = ::trilhaVitalidadePorVigor,
            habilidadeCombate = npc.habilidadePrincipal
        )
    }

    fun expandirEncantosPorExperienciaRepetidos(
        npc: NpcEncontro,
        encantosSolares: List<EncantoSolarDefinition>,
        quantidade: Int
    ): NpcEncontro {
        if (quantidade <= 0) return npc
        val ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(npc)

        return EncounterExperienceService.expandRepeated(
            npc = npc,
            catalogo = encantosSolares,
            ordemHabilidades = ordemHabilidades,
            custoEncanto = { habilidade ->
                custoXpEncanto(habilidade, npc.habilidadeSupernal, npc.habilidadesFavorecidas, npc.casta)
            },
            trilhaVitalidade = ::trilhaVitalidadePorVigor,
            quantidade = quantidade,
            habilidadeCombate = npc.habilidadePrincipal
        )
    }
}
