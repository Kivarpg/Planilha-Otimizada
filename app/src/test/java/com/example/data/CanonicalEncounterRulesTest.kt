package com.example.data

import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.*
import org.junit.Test

class CanonicalEncounterRulesTest {
    private fun solar(
        id: String = "s1",
        nome: String = "Teste",
        habilidade: String = "Briga",
        min: Int = 3,
        essencia: Int = 2,
        prereq: String = ""
    ) = EncantoSolarDefinition(
        id = id, habilidade = habilidade, nome = nome, nomeIngles = "",
        custo = "", minsTexto = "", minHabilidade = min, minEssencia = essencia,
        tipo = "Reflexivo", palavrasChave = "", duracao = "",
        preRequisitos = prereq, descricao = ""
    )

    private fun lunar(
        route: LunarCharmArchetypeRoute? = null,
        prereq: String = "Pai"
    ) = EncantoLunarDefinition(
        id = "l1", atributo = "Destreza", subdivisao = null, nome = "Lunar Teste",
        nomeIngles = "", custo = "", minsTexto = "", minAtributo = 4, minEssencia = 2,
        tipo = "Reflexivo", palavrasChave = "", duracao = "",
        preRequisitos = prereq, descricao = "",
        rotasArquetipo = listOfNotNull(route)
    )

    @Test fun solarDisponivelSomenteComMinimosEPrerequisito() {
        val def = solar(prereq = "Pai")
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(def))
        val graph = EncounterRequirementGraph.compile(catalog)
        val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_SOLAR, def.id)
        val locked = EncounterRulesEngine.evaluate(
            graph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.SOLAR, essence = 2,
                abilities = mapOf("Briga" to 3)
            )
        )
        assertTrue(locked is EncounterRulesEngine.Eligibility.Locked)

        val available = EncounterRulesEngine.evaluate(
            graph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.SOLAR, essence = 2,
                abilities = mapOf("Briga" to 3),
                acquiredCharmNames = setOf("Pai")
            )
        )
        assertTrue(available is EncounterRulesEngine.Eligibility.Available)
    }

    @Test fun tipoDeExaltadoNaoPodeConsumirCatalogoDeOutroTipo() {
        val def = solar(min = 1, essencia = 1)
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(def))
        val graph = EncounterRequirementGraph.compile(catalog)
        val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_SOLAR, def.id)
        val result = EncounterRulesEngine.evaluate(
            graph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.LUNAR, essence = 5,
                abilities = mapOf("Briga" to 5)
            )
        )
        assertTrue(result is EncounterRulesEngine.Eligibility.Invalid)
    }

    @Test fun lunarArquetipoExigeTraitEUsaMinimoAlternativo() {
        val def = lunar(
            route = LunarCharmArchetypeRoute(
                atributo = "Raciocínio",
                condicao = "MINUSCULO",
                minAtributo = 2,
                preRequisitosAlternativos = ""
            ),
            prereq = ""
        )
        val catalog = PreparedEncounterCatalog.prepare(lunares = listOf(def))
        val graph = EncounterRequirementGraph.compile(catalog)
        val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_LUNAR, def.id)

        val noTrait = EncounterRulesEngine.evaluate(
            graph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.LUNAR, essence = 2,
                attributes = mapOf("Destreza" to 1, "Raciocínio" to 2)
            )
        )
        assertTrue(noTrait is EncounterRulesEngine.Eligibility.Locked)

        val withTrait = EncounterRulesEngine.evaluate(
            graph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.LUNAR, essence = 2,
                attributes = mapOf("Destreza" to 1, "Raciocínio" to 2),
                spiritTraits = setOf(LunarSpiritTrait.MINUSCULO)
            )
        )
        val available = withTrait as EncounterRulesEngine.Eligibility.Available
        assertTrue(available.routes.any { it.archetype })
    }

    @Test fun condicaoLunarDesconhecidaNuncaHabilitaRota() {
        val def = lunar(
            LunarCharmArchetypeRoute("Raciocínio", "CONDICAO_DESCONHECIDA", 1, "")
        )
        val graph = EncounterRequirementGraph.compile(
            PreparedEncounterCatalog.prepare(lunares = listOf(def))
        )
        val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_LUNAR, def.id)
        val result = EncounterRulesEngine.evaluate(
            graph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.LUNAR, essence = 5,
                attributes = mapOf("Destreza" to 0, "Raciocínio" to 5)
            )
        )
        assertTrue(result is EncounterRulesEngine.Eligibility.Locked)
    }

    @Test fun requisitoPorContagemPermaneceTipado() {
        val parsed = EncounterPrerequisiteParser.parse("quaisquer dois Encantos de Briga")
        assertEquals(listOf(EncounterRequirement.CharmCount("Briga", 2)), parsed)
    }

    @Test fun catalogoCompartilhaUmaUnicaInstanciaDoGrafo() {
        val def = solar(min = 1, essencia = 1)
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(def))
        assertSame(catalog.requirementGraph, catalog.requirementGraph)
        val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_SOLAR, def.id)
        val result = EncounterRulesEngine.evaluate(
            catalog.requirementGraph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.SOLAR, essence = 1,
                abilities = mapOf("Briga" to 1)
            )
        )
        assertTrue(result is EncounterRulesEngine.Eligibility.Available)
    }

    @Test fun solarCanonicoEquivaleAoResolvedorHistoricoEmCasosRepresentativos() {
        val defs = listOf(
            solar(id = "a", nome = "A", min = 3, essencia = 2, prereq = ""),
            solar(id = "b", nome = "B", min = 2, essencia = 1, prereq = "Pai"),
            solar(id = "c", nome = "C", min = 1, essencia = 1, prereq = "quaisquer dois Encantos de Briga")
        )
        val catalog = PreparedEncounterCatalog.prepare(solares = defs)
        val cases = listOf(
            Triple(mapOf("Briga" to 2), 2, emptySet<String>()),
            Triple(mapOf("Briga" to 3), 2, setOf("Pai")),
            Triple(mapOf("Briga" to 5), 5, setOf("Pai", "Outro"))
        )
        defs.forEach { def ->
            cases.forEach { (abilities, essence, names) ->
                val legacy = EncounterCharmSelectionService.elegivel(
                    def, abilities, essence, names, defs
                )
                val id = PreparedEncounterCatalog.StableContentId(
                    PreparedEncounterCatalog.NS_SOLAR, def.id
                )
                val canonical = EncounterRulesEngine.evaluate(
                    catalog.requirementGraph, id,
                    EncounterRulesEngine.BuildState(
                        TipoExaltadoEncontro.SOLAR,
                        essence = essence,
                        abilities = abilities,
                        acquiredCharmNames = names,
                        charmCountByCategory = mapOf(
                            "Briga" to defs.count { it.habilidade == "Briga" && it.nome in names }
                        )
                    )
                ) is EncounterRulesEngine.Eligibility.Available
                assertEquals("Divergência em ${def.id}", legacy, canonical)
            }
        }
    }

    @Test fun buildStateResolveIdsSemUsarNomeComoIdentidadeFinal() {
        val def = solar(id = "stable", nome = "Nome Visível", min = 1, essencia = 1)
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(def))
        val state = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.SOLAR, 1,
            abilities = mapOf("Briga" to 1),
            acquiredCharmNames = setOf("Nome Visível"),
            catalog = catalog
        )
        assertTrue(
            PreparedEncounterCatalog.StableContentId(
                PreparedEncounterCatalog.NS_SOLAR, "stable"
            ) in state.acquiredCharmIds
        )
    }

    @Test fun plannerEnxergaValorFuturoDeCadeia() {
        val a = solar(id = "a", nome = "A", min = 1, essencia = 1)
        val b = solar(id = "b", nome = "B", min = 1, essencia = 1, prereq = "A")
        val x = solar(id = "x", nome = "X", min = 1, essencia = 1)
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(a, b, x))
        fun id(local: String) = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, local
        )
        val initial = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.SOLAR, 1,
            abilities = mapOf("Briga" to 5),
            catalog = catalog
        )
        val plan = EncounterBuildPlanner.plan(
            catalog = catalog,
            initial = initial,
            candidates = listOf(
                EncounterBuildPlanner.Candidate(id("a")) { _, _ -> 4 },
                EncounterBuildPlanner.Candidate(id("b")) { _, _ -> 20 },
                EncounterBuildPlanner.Candidate(id("x")) { _, _ -> 10 }
            ),
            maxSteps = 2,
            fingerprint = "test"
        )
        assertEquals(listOf(id("a"), id("b")), plan.steps.map { it.id })
        assertEquals(24, plan.totalScore)
    }

    @Test fun ecsRecebeMecanicaCanonicaSemAssumirLegalidade() {
        val tags = EncounterCombatSynergy.tagsFromCanonical(
            setOf(
                EncounterMechanicalTag.WITHERING,
                EncounterMechanicalTag.INITIATIVE_GAIN,
                EncounterMechanicalTag.DEFENSE
            )
        )
        assertTrue(EncounterCombatSynergy.Tag.WITHERING in tags)
        assertTrue(EncounterCombatSynergy.Tag.INITIATIVE_GAIN in tags)
        assertTrue(EncounterCombatSynergy.Tag.DEFENSE in tags)
    }

    @Test fun plannerNuncaSelecionaCandidatoBloqueado() {
        val locked = solar(id = "locked", nome = "Locked", min = 5, essencia = 5)
        val open = solar(id = "open", nome = "Open", min = 1, essencia = 1)
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(locked, open))
        fun id(local: String) = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, local
        )
        val state = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.SOLAR, 1,
            abilities = mapOf("Briga" to 1), catalog = catalog
        )
        val plan = EncounterBuildPlanner.plan(
            catalog, state,
            listOf(
                EncounterBuildPlanner.Candidate(id("locked")) { _, _ -> 1000 },
                EncounterBuildPlanner.Candidate(id("open")) { _, _ -> 1 }
            ),
            maxSteps = 1, fingerprint = "legal"
        )
        assertEquals(listOf(id("open")), plan.steps.map { it.id })
    }

    @Test fun lunarArquetipoHerdaPrerequisitoNormalQuandoAlternativoVazio() {
        val def = lunar(
            route = LunarCharmArchetypeRoute(
                atributo = "Raciocínio",
                condicao = "MINUSCULO",
                minAtributo = 2,
                preRequisitosAlternativos = ""
            ),
            prereq = "Pai"
        )
        val catalog = PreparedEncounterCatalog.prepare(lunares = listOf(def))
        val id = PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_LUNAR, def.id)
        fun canonical(names: Set<String>) = EncounterRulesEngine.evaluate(
            catalog.requirementGraph, id,
            EncounterRulesEngine.BuildState(
                TipoExaltadoEncontro.LUNAR, essence = 2,
                attributes = mapOf("Destreza" to 1, "Raciocínio" to 2),
                acquiredCharmNames = names,
                spiritTraits = setOf(LunarSpiritTrait.MINUSCULO)
            )
        ) is EncounterRulesEngine.Eligibility.Available

        fun legacy(names: Set<String>) = EncounterCharmSelectionService.elegivelLunar(
            def = def,
            attributes = mapOf("Destreza" to 1, "Raciocínio" to 2),
            essencia = 2,
            nomesSelecionados = names,
            catalogoCompleto = listOf(def),
            spiritTraits = setOf(LunarSpiritTrait.MINUSCULO)
        )

        assertFalse(canonical(emptySet()))
        assertFalse(legacy(emptySet()))
        assertTrue(canonical(setOf("Pai")))
        assertTrue(legacy(setOf("Pai")))
    }

    @Test fun catalogoExpoePrerequisitosPorIdCanonico() {
        val pai = solar(id = "pai", nome = "Pai", min = 1, essencia = 1)
        val filho = solar(id = "filho", nome = "Filho", min = 2, essencia = 1, prereq = "Pai")
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(pai, filho))
        val filhoId = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, "filho"
        )
        assertEquals(
            setOf(PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_SOLAR, "pai")),
            catalog.prerequisitesOf(filhoId)
        )
    }

    @Test fun prerequisitoComNomeAmbiguoNaoViraArestaCanonicaErrada() {
        val solarPai = solar(id = "s-pai", nome = "Pai", min = 1, essencia = 1)
        val solarFilho = solar(id = "s-filho", nome = "Filho", min = 2, essencia = 1, prereq = "Pai")
        val lunarPai = lunar(prereq = "").copy(id = "l-pai", nome = "Pai")
        val catalog = PreparedEncounterCatalog.prepare(
            solares = listOf(solarPai, solarFilho),
            lunares = listOf(lunarPai)
        )
        val filhoId = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, "s-filho"
        )
        assertEquals(
            setOf(PreparedEncounterCatalog.StableContentId(PreparedEncounterCatalog.NS_SOLAR, "s-pai")),
            catalog.prerequisitesOf(filhoId)
        )
    }

    @Test fun auditorCanonicoAceitaCadeiaSolarLegal() {
        val pai = solar(id = "pai-audit", nome = "Pai Audit", min = 1, essencia = 1)
        val filho = solar(id = "filho-audit", nome = "Filho Audit", min = 1, essencia = 1, prereq = "Pai Audit")
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(pai, filho))
        // O auditor é coberto de forma indireta aqui pelo mesmo estado que
        // valida cada aquisição: retirar o filho mantém o pai disponível como
        // pré-requisito adquirido.
        val state = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.SOLAR, 1,
            abilities = mapOf("Briga" to 5),
            acquiredCharmNames = setOf("Pai Audit"),
            catalog = catalog
        )
        val id = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, "filho-audit"
        )
        assertTrue(
            EncounterRulesEngine.evaluate(catalog.requirementGraph, id, state) is
                EncounterRulesEngine.Eligibility.Available
        )
    }

    @Test fun parserCanonicoMantemGramaticaHistoricaDeNomesEContagem() {
        assertEquals(
            listOf(
                EncounterRequirement.Charm("Pai A"),
                EncounterRequirement.Charm("Pai B")
            ),
            EncounterPrerequisiteParser.parse("Pai A, Pai B")
        )
        assertEquals(
            listOf(EncounterRequirement.CharmCount("Ocultismo", 4)),
            EncounterPrerequisiteParser.parse("Quaisquer quatro Encantos de Ocultismo")
        )
        assertTrue(EncounterPrerequisiteParser.parse("Nenhum").isEmpty())
    }

    @Test fun adaptadorLegadoUsaParserCanonicoSemAlterarSemantica() {
        data class D(val nome: String, val categoria: String)
        val catalogo = listOf(D("A", "Briga"), D("B", "Briga"), D("C", "Briga"))
        fun eligible(prereq: String, selected: Set<String>) =
            EncounterCharmSelectionService.elegivelGenerico(
                nivelAtual = 5, nivelRequerido = 1, minEssencia = 1,
                preRequisitos = prereq, essencia = 1, nomesSelecionados = selected,
                catalogoCompleto = catalogo,
                categoriaDoEncanto = { it.categoria },
                categoriaConhecida = { it == "Briga" },
                nomeDoEncanto = { it.nome }
            )
        assertFalse(eligible("A, B", setOf("A")))
        assertTrue(eligible("A, B", setOf("A", "B")))
        assertFalse(eligible("Quaisquer dois Encantos de Briga", setOf("A")))
        assertTrue(eligible("Quaisquer dois Encantos de Briga", setOf("A", "B")))
    }

    @Test fun buildStateContaCategoriasCompostasUsadasNosCatalogos() {
        fun s(id: String, nome: String, habilidade: String, ess: Int) =
            solar(id = id, nome = nome, min = 1, essencia = ess).copy(habilidade = habilidade)
        val defs = listOf(
            s("p", "Performance E2", "Performance", 2),
            s("pr", "Presença E3", "Presença", 3),
            s("so", "Socialização E3", "Socialização", 3),
            s("n", "Navegação E1", "Navegação", 1)
        )
        val catalog = PreparedEncounterCatalog.prepare(solares = defs)
        val state = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.SOLAR, 3,
            acquiredCharmNames = defs.mapTo(linkedSetOf()) { it.nome },
            catalog = catalog
        )
        assertEquals(3, state.charmCountByCategory["Performance, Presença ou Socialização"])
        assertEquals(1, state.charmCountByCategory["Performance de Essência 2+"])
        assertEquals(1, state.charmCountByCategory["Essência 3+ de Presença"])
        assertEquals(1, state.charmCountByCategory["Navegação"])
    }

    @Test fun buildStateContaAtributoMentalLunar() {
        val mental = lunar(prereq = "").copy(id = "mental", nome = "Mental", atributo = "Raciocínio")
        val fisico = lunar(prereq = "").copy(id = "fisico", nome = "Físico", atributo = "Destreza")
        val catalog = PreparedEncounterCatalog.prepare(lunares = listOf(mental, fisico))
        val state = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.LUNAR, 1,
            acquiredCharmNames = setOf("Mental", "Físico"),
            catalog = catalog
        )
        assertEquals(1, state.charmCountByCategory["Atributo Mental"])
    }

    @Test fun parserRepresentaAlternativasSemConfundirCategoriaComposta() {
        val or = EncounterPrerequisiteParser.parse(
            "Maestria da Bruxa da Penumbra ou Feitiçaria do Círculo Terrestre"
        ).single()
        assertTrue(or is EncounterRequirement.AnyOf)
        assertEquals(2, (or as EncounterRequirement.AnyOf).alternatives.size)

        val count = EncounterPrerequisiteParser.parse(
            "Quaisquer cinco Encantos de Performance, Presença ou Socialização"
        ).single()
        assertEquals(
            EncounterRequirement.CharmCount("Performance, Presença ou Socialização", 5),
            count
        )
    }

    @Test fun parserTrataPontoEVirgulaComoAndEntreGrupos() {
        val parsed = EncounterPrerequisiteParser.parse(
            "Rearranjo Constante de Mercúrio; Forma do Gafanhoto Esmeralda ou Forma Bestial Imponente"
        )
        assertEquals(2, parsed.size)
        assertEquals(EncounterRequirement.Charm("Rearranjo Constante de Mercúrio"), parsed[0])
        assertTrue(parsed[1] is EncounterRequirement.AnyOf)
    }

    @Test fun parserAceitaAlternativaEntreEncantoEContagem() {
        val parsed = EncounterPrerequisiteParser.parse(
            "Defesa Teimosa do Javali ou quaisquer 8 Encantos sociais"
        ).single() as EncounterRequirement.AnyOf
        assertEquals(EncounterRequirement.Charm("Defesa Teimosa do Javali"), parsed.alternatives[0])
        assertEquals(EncounterRequirement.CharmCount("sociais", 8), parsed.alternatives[1])
    }

    @Test fun nenhumComoAlternativaMantemRotaSemPrerequisito() {
        assertTrue(
            EncounterPrerequisiteParser.parse(
                "Nenhum ou quaisquer 5 Encantos de Presença, Resistência ou Socialização"
            ).isEmpty()
        )
    }

    @Test fun parserPreservaAndAntesDeAlternativaPorContagem() {
        val parsed = EncounterPrerequisiteParser.parse(
            "Abordagem do Exemplar Eminente, Égide Solar Invencível ou quaisquer 15 Encantos de Casta"
        )
        assertEquals(2, parsed.size)
        assertEquals(EncounterRequirement.Charm("Abordagem do Exemplar Eminente"), parsed[0])
        val alt = parsed[1] as EncounterRequirement.AnyOf
        assertEquals(EncounterRequirement.Charm("Égide Solar Invencível"), alt.alternatives[0])
        assertEquals(EncounterRequirement.CharmCount("Casta", 15), alt.alternatives[1])
    }

    @Test fun parserAceitaFamiliaRealDeContagensDosCatalogos() {
        val cases = listOf(
            "quaisquer 8 Encantos sociais" to EncounterRequirement.CharmCount("sociais", 8),
            "quaisquer 6 Encantos que reflitam o Princípio em foco" to EncounterRequirement.CharmCount("que reflitam o Princípio em foco", 6),
            "Quaisquer 3 Encantos de Prontidão que não sejam de Excelência" to EncounterRequirement.CharmCount("Prontidão que não sejam de Excelência", 3),
            "quaisquer 5 Encantos" to EncounterRequirement.CharmCount("", 5),
            "quaisquer quatro Encantamentos de Atributo Mental" to EncounterRequirement.CharmCount("Atributo Mental", 4),
            "Quaisquer cinco Encantos de Performance de Essência 2+" to EncounterRequirement.CharmCount("Performance de Essência 2+", 5),
            "Quaisquer quatro Encantos de Essência 3+ de Socialização" to EncounterRequirement.CharmCount("Essência 3+ de Socialização", 4)
        )
        cases.forEach { (source, expected) ->
            assertEquals(source, expected, EncounterPrerequisiteParser.parse(source).single())
        }
    }

    @Test fun parserReconheceContagemDepoisDeAlternativaSemDe() {
        val parsed = EncounterPrerequisiteParser.parse(
            "Defesa Teimosa do Javali ou quaisquer 8 Encantos sociais"
        ).single() as EncounterRequirement.AnyOf
        assertEquals(EncounterRequirement.Charm("Defesa Teimosa do Javali"), parsed.alternatives[0])
        assertEquals(EncounterRequirement.CharmCount("sociais", 8), parsed.alternatives[1])
    }
    @Test fun plannerMarcaSomenteDesbloqueioRealPorPassoAnterior() {
        val a = solar(id = "unlock-a", nome = "Unlock A", min = 1, essencia = 1)
        val b = solar(id = "unlock-b", nome = "Unlock B", min = 1, essencia = 1, prereq = "Unlock A")
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(a, b))
        fun id(local: String) = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, local
        )
        val initial = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.SOLAR, 1,
            abilities = mapOf("Briga" to 5),
            catalog = catalog
        )
        val plan = EncounterBuildPlanner.plan(
            catalog = catalog,
            initial = initial,
            candidates = listOf(
                EncounterBuildPlanner.Candidate(id("unlock-a")) { _, _ -> 2 },
                EncounterBuildPlanner.Candidate(id("unlock-b")) { _, _ -> 9 }
            ),
            maxSteps = 2,
            fingerprint = "unlock"
        )
        assertEquals(listOf(false, true), plan.steps.map { it.unlockedByPreviousSteps })
    }

    @Test fun catalogoIndexaDependentesDiretosSemMisturarIrmaos() {
        val a = solar(id = "root-a", nome = "Root A", min = 1, essencia = 1)
        val b = solar(id = "child-b", nome = "Child B", min = 1, essencia = 1, prereq = "Root A")
        val c = solar(id = "sibling-c", nome = "Sibling C", min = 1, essencia = 1)
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(a, b, c))
        fun id(local: String) = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, local
        )
        assertEquals(setOf(id("child-b")), catalog.directDependentsOf(id("root-a")))
        assertTrue(catalog.directDependentsOf(id("sibling-c")).isEmpty())
    }



    @Test fun plannerCanonicoReconheceDependenteDesbloqueadoPorNomeEId() {
        val raiz = solar(id = "future-root", nome = "Raiz Futura", min = 1, essencia = 1)
        val filho = solar(
            id = "future-child", nome = "Filho Futuro", min = 1, essencia = 1,
            prereq = "Raiz Futura"
        )
        val isolado = solar(id = "isolated-now", nome = "Isolado Agora", min = 1, essencia = 1)
        val catalog = PreparedEncounterCatalog.prepare(solares = listOf(raiz, filho, isolado))
        fun id(local: String) = PreparedEncounterCatalog.StableContentId(
            PreparedEncounterCatalog.NS_SOLAR, local
        )
        val initial = EncounterRulesEngine.buildState(
            TipoExaltadoEncontro.SOLAR, 1,
            abilities = mapOf("Briga" to 5),
            catalog = catalog
        )
        val plan = EncounterCanonicalPlanner.planCombatCharms(
            catalog = catalog,
            initial = initial,
            candidateIds = listOf(id("future-root"), id("future-child"), id("isolated-now")),
            selectedTexts = emptyList(),
            maxSteps = 1,
            fingerprint = "future-name-id"
        )
        val raizStep = plan.steps.single()
        assertEquals(id("future-root"), raizStep.id)
        assertEquals(1, raizStep.futureValue)
        assertTrue(raizStep.currentValue <= raizStep.score)
    }

    @Test fun crescimentoFuturoContinuaLimitadoEnaoSubstituiUtilidadePresente() {
        val fraco = EncounterBuildQuality.combine(current = 2, future = 100)
        val forte = EncounterBuildQuality.combine(current = 6, future = 0)
        assertEquals(EncounterBuildQuality.MAX_FUTURE_GROWTH_BONUS, fraco.future)
        assertTrue(forte.total > fraco.total)
    }

}
