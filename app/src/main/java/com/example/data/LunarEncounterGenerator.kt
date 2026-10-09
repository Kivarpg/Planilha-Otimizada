package com.example.data

import com.example.model.*
import kotlin.random.Random

// Gerador de NPCs Lunares pra Aba 11 (Encontros) — estrutura inicial.
// Reaproveita a mesma infraestrutura compartilhada dos geradores Solar e
// Sangue de Dragão (distribuição de atributos/habilidades, equipamento,
// estatísticas derivadas, trilha de vitalidade), mas com as duas diferenças
// fundamentais do Lunar:
//   1. Atributos Favorecidos (não Habilidades Favorecidas) — cada Casta
//      nomeada fornece opções, das quais a criação sorteia 2 como
//      Atributos de Casta; depois sorteia 2 Atributos Favorecidos adicionais,
//      distintos dos 2 de Casta. O conjunto especial final tem exatamente
//      4 Atributos, preservando separadamente Casta e Favorecidos.
//   2. Encantos usam Atributo como requisito mínimo, não Habilidade — por
//      isso a seleção de Encantos aqui NÃO reaproveita
//      EncounterCharmSelectionService (que é tipado pra EncantoSolarDefinition
//      e filtra por Habilidade); em vez disso, filtra diretamente por
//      EncantoLunarDefinition.atributo.
//
// Simplificações desta rodada (documentadas, não esquecidas):
//   - Sem conceito de Habilidade Supernal (Lunares não têm; o campo
//     habilidadeSupernal do NpcEncontro fica vazio) nem de Excelência restrita
//     por Atributo com nível mínimo — todo Atributo Favorecido/Casta é
//     tratado igual pra fins de custo de XP futuro.
//   - A seleção inicial usa a mesma validação central de pré-requisitos do
//     Solar, incluindo requisitos por contagem (por exemplo, Atributo Mental).
internal object LunarEncounterGenerator {
    // Extraído do corpo de gerar() (refatoração de organização — pedido
    // explícito do usuário, feito com cautela extra). Já vinha isolado
    // num bloco run{} próprio no código original — só precisou ganhar
    // nome e assinatura. Correção de lacuna real: a Casta é sorteada de
    // forma totalmente independente do arquétipo, então um NPC "Físico"
    // podia sair com Casta e Favorecidos 100% Sociais/Mentais, contra o
    // próprio propósito do arquétipo. Garante que pelo menos 2 dos 4
    // Atributos Favorecidos pertençam à categoria do arquétipo, trocando
    // os que não pertencem (fora da Casta primeiro, pra não desfazer a
    // escolha de Casta já sorteada) por Atributos da categoria certa.
    private fun ajustarAtributosFavorecidosParaArquetipo(
        atributosFavorecidos: List<String>,
        atributosCastaSet: Set<String>,
        categoriaDoArquetipo: List<String>,
        random: Random
    ): List<String> {
        val categoriaSet = categoriaDoArquetipo.toSet()
        val resultado = atributosFavorecidos.distinct().toMutableList()

        // Os dois Atributos de Casta são imutáveis nesta etapa. A meta é que
        // pelo menos 2 dos 4 Atributos especiais (Casta + Favorecidos)
        // pertençam à categoria do arquétipo; por isso, se a Casta já
        // fornece 1 ou 2 deles, apenas o número restante de Favorecidos é
        // ajustado.
        val quantidadeDeCastaNaCategoria = atributosCastaSet.count { it in categoriaSet }
        val quantidadeDeFavorecidosNaCategoria = resultado.count { it in categoriaSet }
        val faltam = (2 - quantidadeDeCastaNaCategoria - quantidadeDeFavorecidosNaCategoria)
            .coerceAtLeast(0)
        if (faltam == 0) return resultado

        val candidatosDaCategoria = categoriaDoArquetipo
            .filter { it !in resultado }
            .shuffled(random)
            .toMutableList()

        val indicesTrocaveis = resultado.indices
            .filter { resultado[it] !in categoriaSet && resultado[it] !in atributosCastaSet }
            .toMutableList()

        // Em caso de categoria totalmente coincidente com a Casta, não há
        // motivo para alterar os Atributos de Casta; basta manter os quatro
        // elementos já distintos. Nos demais casos, existem até dois espaços
        // adicionais que podem ser substituídos.
        var trocas = 0
        for (indice in indicesTrocaveis) {
            if (trocas >= faltam || candidatosDaCategoria.isEmpty()) break
            resultado[indice] = candidatosDaCategoria.removeAt(0)
            trocas++
        }

        return resultado.distinct()
    }

    fun gerarNome(cultura: CulturaNome? = null, genero: GeneroNome? = null, random: Random = Random.Default): String =
        NameGenerator.gerarSolarOuLunar(cultura, genero, random)

    fun gerar(
        nomeManual: String,
        arquetipo: ArquetipoEncontro,
        encantosLunares: List<EncantoLunarDefinition>,
        feiticos: List<com.example.data.FeiticoDefinition> = emptyList(),
        culturaNome: CulturaNome? = null,
        generoNome: GeneroNome? = null,
        random: Random = Random.Default,
        meritosCatalogo: List<MeritoDefinition> = emptyList(),
        focoPersonalizado: String? = null,
        customizacao: EncounterCustomization? = null,
        estilosArtesMarciais: List<EstiloArteMarcialDefinition> = emptyList()
    ): NpcEncontro {
        val arquetipoEfetivo = arquetipo
        val focoEfetivoBruto = customizacao?.focoEfetivo(focoPersonalizado)
            ?: focoPersonalizado.takeIf { customizacao == null }
        val focoFeiticariaExplicito =
            customizacao?.rotaFeiticaria == true || focoEfetivoBruto == ENCOUNTER_FOCUS_SORCERY
        val explorarFeiticaria = EncounterSorceryRoutePolicy.decide(
            archetype = arquetipoEfetivo,
            explicitSorceryFocus = focoFeiticariaExplicito,
            random = random
        ).explore
        val focoEfetivo = customizacao?.focoMecanico(focoPersonalizado, "Inteligência")
            ?: if (focoFeiticariaExplicito) "Inteligência" else focoEfetivoBruto
        val generoEscolhido = generoNome ?: GeneroNome.entries.random(random)
        val nome = nomeManual.trim().ifBlank { gerarNome(culturaNome, generoEscolhido, random) }

        // --- Categorias de Atributo por arquétipo — movido pra cá (antes
        // vinha depois) porque agora também é usado pra alinhar os
        // Atributos Favorecidos ao arquétipo, não só a distribuição de
        // pontos. ---
        val categoriasAtributo = EncounterGenerationRules.ATTRIBUTE_GROUPS
        val categoriaDoArquetipo = categoriasAtributo.getValue(arquetipoEfetivo)

        // --- Casta Lunar (sorteada) — do pool de 3 (ou 9, Sem Casta),
        // sorteia exatamente 2 como Atributo de Casta de verdade (pedido
        // explícito do usuário: não são os 3 automaticamente, só 2). ---
        val casta = LunarCasta.entries.random(random)
        val poolCasta = casta.poolAtributosCasta()
        val atributosCasta = poolCasta.shuffled(random).take(2)

        // --- Atributos Favorecidos: exatamente 2 adicionais. ---
        // Os 2 Atributos de Casta e os 2 Favorecidos são categorias distintas
        // na planilha. Os Favorecidos são escolhidos além dos de Casta, sem
        // duplicar nenhum dos 2 Atributos de Casta.
        val todosAtributos = EncounterGenerationRules.ALL_ATTRIBUTES
        val atributosCastaSet = atributosCasta.toSet()
        var atributosFavorecidos = todosAtributos
            .filter { it !in atributosCastaSet }
            .shuffled(random)
            .take(2)

        // O ajuste de arquétipo atua somente nos 2 Favorecidos adicionais.
        // Os 2 Atributos de Casta já escolhidos são imutáveis.
        atributosFavorecidos = ajustarAtributosFavorecidosParaArquetipo(
            atributosFavorecidos,
            atributosCastaSet,
            categoriaDoArquetipo,
            random
        ).filter { it !in atributosCastaSet }.distinct().take(2)
        if (focoEfetivo != null && focoEfetivo !in atributosCastaSet && focoEfetivo !in atributosFavorecidos) {
            atributosFavorecidos = EncounterRulePolicy.resolvePriority(
                explicitIntent = listOf(focoEfetivo),
                exaltStructure = atributosFavorecidos
            ).take(2)
        }

        // Garantia final: exatamente 2 Atributos de Casta + 2 Favorecidos
        // adicionais, todos distintos entre si.
        if (atributosFavorecidos.size < 2) {
            todosAtributos
                .filter { it !in atributosCastaSet && it !in atributosFavorecidos }
                .shuffled(random)
                .take(2 - atributosFavorecidos.size)
                .forEach { atributosFavorecidos += it }
        }
        check(atributosCasta.size == 2) {
            "Lunar deve possuir exatamente 2 Atributos de Casta"
        }
        check(atributosFavorecidos.size == 2) {
            "Lunar deve possuir exatamente 2 Atributos Favorecidos adicionais"
        }
        check(atributosCasta.toSet().intersect(atributosFavorecidos.toSet()).isEmpty()) {
            "Atributos Favorecidos Lunares não podem duplicar Atributos de Casta"
        }
        val atributosCastaOuFavorecidos = EncounterRulePolicy.resolvePriority(
            exaltStructure = atributosCasta + atributosFavorecidos
        )
        check(atributosCastaOuFavorecidos.size == 4) {
            "Lunar deve possuir 4 Atributos especiais distintos: 2 de Casta + 2 Favorecidos"
        }

        val (primarios, secundarios, terciarios) = EncounterGenerationRules.gruposDeAtributoPara(arquetipoEfetivo, random)
        var attributes = EncounterDistributionService.distribuirAtributos(primarios, secundarios, terciarios, random, arquetipo)
        if (focoEfetivo != null) {
            attributes = EncounterGenerationRules.priorizarAtributoSelecionado(attributes, focoEfetivo)
        }

        // --- Habilidade de combate + defensiva obrigatória (igual ao Solar) ---
        val habilidadeCombate = EncounterGenerationRules.resolverHabilidadeCombate(customizacao?.ataqueExplicito, customizacao?.focoParaAtaque(focoEfetivo) ?: focoEfetivo.takeIf { customizacao == null }, null, random)
        val habilidadeDefensivaObrigatoria = customizacao?.defesaExplicita ?: EncounterGenerationRules.habilidadeDefensivaPara(habilidadeCombate)

        // --- Distribuição de habilidades: reaproveita a mesma função do
        // Solar. O parâmetro de Supernal permanece null porque Lunar não tem
        // Supernal; combate é tratado separadamente como foco de construção.
        val resultado = EncounterDistributionService.distribuirHabilidades(
            arquetipoEfetivo, habilidadeCombate, habilidadeDefensivaObrigatoria, null, emptyList(), random,
            habilidadesEstruturaisRelevantes = listOfNotNull(customizacao?.secundariaExplicita),
            habilidadesMinimoUm = if (arquetipoEfetivo == ArquetipoEncontro.MENTAL) setOf("Linguística") else emptySet()
        )
        // Restrições de Força/Destreza fazem parte da montagem do conjunto-base
        // de Atributos e precisam ocorrer ANTES dos Pontos de Bônus.
        // Se fossem aplicadas depois, a redistribuição poderia retirar um ponto
        // que acabou de ser comprado em Atributo de Casta/Favorecido, fazendo
        // parecer que o PB Lunar "sumiu" na planilha final.
        attributes = EncounterDistributionService.ajustarAtributosPorHabilidadeCombate(
            attributes, habilidadeCombate, random
        )
        EncounterValidationService.validarDistribuicaoBaseDeAtributos(attributes, arquetipoEfetivo)

        // O planejador estratégico Lunar permanece coberto por testes próprios,
        // mas não participa da geração enquanto sua prioridade não é autoritativa.
        // Executá-lo em shadow mode aqui reconstruía comparações de catálogo sem
        // alterar nenhum campo do NPC final.
        val prioridadePbAtiva: List<String> = emptyList()

        val distribuicaoPbLunar = EncounterDistributionService.distribuirPontosDeBonusLunar(
            arquetipo = arquetipoEfetivo,
            abilitiesBase = resultado.abilities,
            attributesBase = attributes,
            atributosCastaOuFavorecidos = atributosCastaOuFavorecidos,
            habilidadeCombate = habilidadeCombate,
            habilidadeDefensiva = habilidadeDefensivaObrigatoria,
            habilidadeSuporte = resultado.habilidadeSocialOuMental,
            random = random,
            prioridadeAtributos = prioridadePbAtiva
        )
        val abilities = distribuicaoPbLunar.abilities
        val forcaDeVontadeComBonus = distribuicaoPbLunar.forcaDeVontade
        attributes = distribuicaoPbLunar.attributes
        if (focoEfetivo != null) {
            attributes = EncounterGenerationRules.priorizarAtributoSelecionado(attributes, focoEfetivo)
        }

        var especialidades = EncounterDistributionService.distribuirEspecialidades(
            arquetipoEfetivo, habilidadeCombate, habilidadeDefensivaObrigatoria, resultado.habilidadeSocialOuMental, abilities, random,
            habilidadesEstruturaisRelevantes = EncounterGenerationRules.LUNAR_ABILITY_PROFILES.getValue(arquetipoEfetivo)
        )
        especialidades = EncounterDistributionService.adicionarEspecialidadeAdicional(
            especialidades,
            abilities,
            (listOf(resultado.habilidadeSocialOuMental, habilidadeDefensivaObrigatoria, habilidadeCombate) +
                EncounterGenerationRules.LUNAR_ABILITY_PROFILES.getValue(arquetipoEfetivo)).distinct(),
            distribuicaoPbLunar.precisaEspecialidadeAdicional
        )

        val essencia = EncounterGenerationRules.ESSENCIA_LUNAR
        val (arma, armadura) = EncounterEquipmentService.selecionarEquipamentoParaEncontro(
            habilidadeCombate = habilidadeCombate, random = random, permitirDoisArtefatos = true
        )

        // --- Encantos: seleção por Atributo (não Habilidade) — diferença
        // central do Lunar. Prioriza os Atributos Favorecidos (Casta +
        // adicionais), do mais barato (menor Essência mínima) pro mais caro,
        // respeitando pré-requisito por nome exato. Meta de 15, igual ao Solar.
        // O pré-requisito do Círculo de Magia ("Quaisquer 4 Encantamentos
        // de Atributo Mental") já é checado corretamente em elegivelLunar,
        // contando qualquer um dos 3 Atributos Mentais — sem precisar
        // forçar prioridade nenhuma aqui. ---
        // A Forma Espiritual antecede a aquisição de Encantos. Ela pode ser
        // Minúscula/Lendária mesmo antes do Encanto necessário para assumir a forma.
        val ordemEncantosLunares = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOfNotNull(focoEfetivo),
            archetype = LunarArchetypePolicy.attributePriority(arquetipoEfetivo, atributosCastaOuFavorecidos)
        )
        val formaEspiritual = LunarSpiritFormStrategy.escolherPrincipal(
            catalogo = encantosLunares,
            attributes = attributes,
            essencia = essencia,
            foco = focoEfetivo,
            ordemAtributos = ordemEncantosLunares,
            random = random
        )
        val traitsFormaPrincipal = LunarSpiritShapeArchetypeTraits.forAnimal(formaEspiritual)
        val resultadoPrimeiraPassagem = selecionarEncantosIniciaisComRotas(
            catalogo = encantosLunares,
            attributes = attributes,
            essencia = essencia,
            ordemAtributos = ordemEncantosLunares,
            quantidade = 15,
            random = random,
            arquetipo = arquetipoEfetivo,
            atributoFocoUsuario = focoEfetivo,
            spiritTraits = traitsFormaPrincipal,
            exigirFeiticaria = explorarFeiticaria
        )
        val charmsPrimeiraPassagem = resultadoPrimeiraPassagem.charms
        // Expressão da Alma da Quimera faz os DOIS animais contarem como forma
        // espiritual para Encantos de Arquétipo. Portanto, quando ela entra na
        // criação inicial, a segunda forma precisa existir antes da seleção
        // definitiva; caso contrário seus traços só seriam úteis depois da
        // criação do NPC. A segunda passagem é a seleção autoritativa.
        val possuiQuimeraNaPrimeiraPassagem = charmsPrimeiraPassagem.any {
            it.nome.equals("Expressão da Alma da Quimera", ignoreCase = true)
        }
        val formaEspiritualSecundariaInicial = if (possuiQuimeraNaPrimeiraPassagem) {
            LunarSpiritFormStrategy.escolherSecundaria(
                principal = formaEspiritual,
                catalogo = encantosLunares,
                attributes = attributes,
                essencia = essencia,
                foco = focoEfetivo,
                ordemAtributos = ordemEncantosLunares,
                random = random
            )
        } else null
        val traitsFormasNaCriacao = (
            traitsFormaPrincipal +
                formaEspiritualSecundariaInicial?.let { LunarSpiritShapeArchetypeTraits.forAnimal(it) }.orEmpty()
            ).toSet()
        val resultadoSegundaPassagem = if (formaEspiritualSecundariaInicial != null) {
            selecionarEncantosIniciaisComRotas(
                catalogo = encantosLunares,
                attributes = attributes,
                essencia = essencia,
                ordemAtributos = ordemEncantosLunares,
                quantidade = 15,
                random = random,
                arquetipo = arquetipoEfetivo,
                atributoFocoUsuario = focoEfetivo,
                spiritTraits = traitsFormasNaCriacao,
                exigirFeiticaria = explorarFeiticaria
            )
        } else resultadoPrimeiraPassagem
        val charmsSegundaPassagem = resultadoSegundaPassagem.charms
        // Se a segunda passagem deixar de conter a própria Quimera, seus traços
        // não podem permanecer habilitados. Nesse caso preservamos a passagem
        // legal anterior e descartamos a segunda forma.
        val quimeraPermanece = charmsSegundaPassagem.any {
            it.nome.equals("Expressão da Alma da Quimera", ignoreCase = true)
        }
        val resultadoFeiticaria = if (formaEspiritualSecundariaInicial == null || quimeraPermanece) {
            resultadoSegundaPassagem
        } else resultadoPrimeiraPassagem

        // No Mental automático, os 90% apenas autorizam explorar a rota mágica.
        // Construímos também a alternativa pura com o MESMO estado do Random
        // capturado antes da seleção mágica. Assim os dois candidatos recebem
        // decisões aleatórias equivalentes (empates de Atributo/beam), sem
        // consumir RNG adicional no fluxo autoritativo.
        val compararCandidatosFeiticaria =
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                archetype = arquetipoEfetivo,
                exploreSorcery = explorarFeiticaria,
                explicitSorceryFocus = focoFeiticariaExplicito,
                sorceryConstructible = (attributes["Inteligência"] ?: 0) >= 3 &&
                    encantosLunares.any { it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE }
            )
        val resultadoPuro = if (compararCandidatosFeiticaria) {
            selecionarEncantosIniciaisComRotas(
                catalogo = encantosLunares,
                attributes = attributes,
                essencia = essencia,
                ordemAtributos = ordemEncantosLunares,
                quantidade = 15,
                random = Random(EncounterSorceryRoutePolicy.lunarComparisonSeed(
                    attributes = attributes,
                    focus = focoEfetivo,
                    spiritTraits = traitsFormaPrincipal
                )),
                arquetipo = arquetipoEfetivo,
                atributoFocoUsuario = focoEfetivo,
                spiritTraits = traitsFormaPrincipal,
                exigirFeiticaria = false
            )
        } else resultadoFeiticaria
        val selecionarFeiticaria = if (compararCandidatosFeiticaria) {
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = false,
                exploreSorcery = true,
                sorceryConstructible = resultadoFeiticaria.charms.any {
                    it.nome == com.example.model.NOME_FEITICARIA_TERRESTRE
                },
                pureQuality = EncounterSorceryRoutePolicy.qualityOfLunarCharmCandidate(
                    resultadoPuro.charms, attributes
                ),
                sorceryQuality = EncounterSorceryRoutePolicy.qualityOfLunarCharmCandidate(
                    resultadoFeiticaria.charms, attributes
                )
            )
        } else explorarFeiticaria
        val resultadoSelecaoInicial = if (selecionarFeiticaria) resultadoFeiticaria else resultadoPuro
        resultadoSelecaoInicial.routeMetrics?.let(EncounterBuildObservability::publishRouteMetrics)
        val charmsIniciais = resultadoSelecaoInicial.charms
        val formaEspiritualSecundariaCriacao = formaEspiritualSecundariaInicial.takeIf { quimeraPermanece }
        val traitsFormasCriacaoEfetivos = if (formaEspiritualSecundariaCriacao != null) {
            traitsFormasNaCriacao
        } else traitsFormaPrincipal

        // --- Méritos: imediatamente após Habilidades e antes de Ações. ---
        // O catálogo de Méritos pode estar vazio em testes/ambientes em que
        // os dados ainda não foram carregados. Nesse caso, não há Méritos a
        // distribuir; não devemos transformar catálogo vazio em erro de
        // geração do NPC. Quando o catálogo existe, a distribuição central
        // continua aplicando orçamento e pré-requisitos normalmente.
        val merits = if (meritosCatalogo.isEmpty()) {
            emptyList()
        } else {
            run {
                val preferirArtesMarciais = EncounterArchetypePolicy.devePreferirArtesMarciais(
                    arquetipoEfetivo, abilities["Briga"] ?: 0, random
                )
                EncounterMeritDistributionService.distribuir(
                    NpcEncontro(tipoExaltado = TipoExaltadoEncontro.LUNAR, arquetipo = arquetipoEfetivo, arma = arma, armadura = armadura),
                    meritosCatalogo, random,
                    preferirArtistaMarcial = preferirArtesMarciais || customizacao?.rotaArtesMarciais == true,
                    exigirArtistaMarcial = customizacao?.rotaArtesMarciais == true
                )
            }
        }
        val abilitiesAjustadas = if (merits.any { it.nome.equals("Artista Marcial", ignoreCase = true) }) {
            abilities.toMutableMap().apply { this["Briga"] = 1 }.toMap()
        } else abilities

        val vigor = attributes["Vigor"] ?: 1
        // Corpo de Touro Lunar é limitado por Vigor (Atributo), não por
        // Resistência (Habilidade) como no Solar — confirmado no texto do
        // próprio Encanto ("Este Encanto pode ser comprado (Vigor) vezes").
        val corpoDeTouroInicial = charmsIniciais.count { it.nome == NOME_CORPO_DE_TOURO }
        val efeitosMeritos = EncounterMeritEffectsService.efeitos(merits)

        val healthBoxes = EncounterMeritEffectsService.adicionarVitalidade(efeitosMeritos, trilhaVitalidadePorVigor(vigor, corpoDeTouroCount = corpoDeTouroInicial))
        val motes = EncounterMoteService.calcular(
            tipo = TipoExaltadoEncontro.LUNAR,
            essencia = essencia,
            arma = arma,
            armadura = armadura
        )
        val motesPersonais = motes.pessoais
        val motesPerifericos = motes.perifericos
        var forcaDeVontade = forcaDeVontadeComBonus + (essencia - 3).coerceAtLeast(0)

        // Os 15 Pontos de Bônus já foram integralmente distribuídos pela
        // rotina específica de criação Lunar acima. Não existe uma segunda
        // etapa de gasto aqui: isso evita duplicar orçamento e garante que
        // Habilidades, Atributos e Força de Vontade compartilhem os mesmos
        // 15 PB.

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

        val habilidadesComEspecialidade = especialidades.asSequence().map { it.habilidade }.toHashSet()
        val temEspecialidadeIntegridade = "Integridade" in habilidadesComEspecialidade
        val temEspecialidadeSocializacao = "Socialização" in habilidadesComEspecialidade
        val temEspecialidadeAtletismo = "Atletismo" in habilidadesComEspecialidade
        val temEspecialidadeEsquiva = "Esquiva" in habilidadesComEspecialidade
        val rotaMarcialAtiva = customizacao?.rotaArtesMarciais == true ||
            merits.any { it.nome.equals("Artista Marcial", ignoreCase = true) && it.valor >= 4 }
        val selecaoMarcial = if (rotaMarcialAtiva) {
            EncounterMartialArtsSelectionService.selecionar(
                estilos = estilosArtesMarciais,
                tipo = TipoExaltadoEncontro.LUNAR,
                briga = abilitiesAjustadas["Briga"] ?: 0,
                essencia = essencia,
                random = random
            )
        } else null
        val (armaFinal, armaduraFinal) = selecaoMarcial?.let {
            EncounterEquipmentService.ajustarParaEstiloMarcial(it.estilo, arma, armadura, random)
        } ?: (arma to armadura)
        val nomesEncantosIniciais = charmsIniciais.map { it.nome }.toSet()
        val encantosNpc = EncounterMartialArtsSelectionService.integrarMantendoQuantidade(
            charmsIniciais.map { def ->
                val atributoAquisicao = requireNotNull(
                    resultadoSelecaoInicial.acquisitionAttributes[def.nome]
                ) {
                    "Seleção Lunar perdeu a rota de aquisição de ${def.nome}"
                }
                EncantoEncontro(def.nome, atributoAquisicao, def.custo)
            },
            selecaoMarcial,
            nomesProtegidos = if (formaEspiritualSecundariaCriacao != null) {
                setOf("Expressão da Alma da Quimera")
            } else emptySet()
        )
        val derivadosFinais = EncounterDerivedStatsService.calcularDerivadosComuns(
            attributes, abilitiesAjustadas, armaduraFinal,
            bonusPerseveranca = if (temEspecialidadeIntegridade) 1 else 0,
            bonusAstucia = if (temEspecialidadeSocializacao) 1 else 0,
            bonusJuntarBatalha = 1 + efeitosMeritos.bonusJuntarABatalha,
            bonusInvestida = (if (temEspecialidadeAtletismo) 1 else 0) + efeitosMeritos.bonusInvestida,
            bonusDesengajamento = (if (temEspecialidadeEsquiva) 1 else 0) + efeitosMeritos.bonusDesengajamento
        )


        val limiteInicial = EncounterLimitCatalog.sortear(TipoExaltadoEncontro.LUNAR, casta.displayName, random)
        val idiomaInicial = EncounterLanguageService.selecionar(TipoExaltadoEncontro.LUNAR, random = random)
        // Expressão da Alma da Quimera faz ambos os animais contarem como forma
        // espiritual para Arquétipo. A segunda forma é persistida para a progressão.
        val formaEspiritualSecundaria = formaEspiritualSecundariaCriacao.takeIf {
            encantosNpc.any { encanto ->
                encanto.nome.equals("Expressão da Alma da Quimera", ignoreCase = true)
            }
        }
        val traitsFormasArquetipo = if (formaEspiritualSecundaria != null) {
            traitsFormasCriacaoEfetivos.map { it.name }.sorted()
        } else {
            traitsFormaPrincipal.map { it.name }.sorted()
        }
        // Sinal Lunar: todo NPC Lunar gerado recebe imediatamente um sinal
        // sorteado no idioma atual do sistema. O valor é persistido no
        // NpcEncontro para aparecer também na ficha/PDF da Aba 11.
        val sinal = LunarMarksService.sortearExibicaoCoerente(formaEspiritual, random = random)

        val npcBase = NpcEncontro(
            nome = nome,
            genero = generoEscolhido.name,
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            arquetipo = arquetipoEfetivo,
            casta = casta.displayName,
            // Campo reaproveitado (mesmo do Solar) para guardar os 2
            // Atributos Favorecidos adicionais do Lunar. Os 2 de Casta ficam
            // separados em lunarAtributosCasta.
            habilidadesFavorecidas = atributosFavorecidos,
            lunarAtributosCasta = atributosCasta,
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
            limite = limiteInicial,
            idioma = idiomaInicial,
            formaEspiritual = SpiritualFormService.exibir(formaEspiritual),
            formaEspiritualSecundaria = formaEspiritualSecundaria?.let { SpiritualFormService.exibir(it) }.orEmpty(),
            lunarArchetypeTraits = traitsFormasArquetipo,
            lunarPrimaryArchetypeTraits = traitsFormaPrincipal.map { it.name }.sorted(),
            sinal = sinal,
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
            iniciativaAtual = 0,
            dano = dano,
            focoProgressaoExplicito = customizacao?.focoExplicito,
            lunarAtaqueEscolhido = resultadoSelecaoInicial.ataqueEscolhido,
            alertasValidacao = EncounterValidationService.validar(
                arquetipo = arquetipoEfetivo,
                attributes = attributes,
                charms = encantosNpc.map { it.nome },
                feiticos = feiticosComGratuito
            )
        )
        val audit = EncounterNpcAuditor.audit(
            npcBase, lunares = encantosLunares, feiticos = feiticos, estilosMarciais = estilosArtesMarciais
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
        EncounterVitalityService.trilhaVitalidadeLunar(vigor, corpoDeTouroCount)

    private fun selecionarEncantosIniciaisComRotas(
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
    ): LunarEncounterCharmSelection.SelectionResult =
        LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo, attributes, essencia, ordemAtributos, quantidade, random,
            arquetipo, atributoFocoUsuario, spiritTraits, exigirFeiticaria
        )


}
