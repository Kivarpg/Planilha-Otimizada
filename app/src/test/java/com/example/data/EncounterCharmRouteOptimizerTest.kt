package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCharmRouteOptimizerTest {
    private fun encanto(
        nome: String,
        custo: Int,
        preRequisitos: String = "",
        habilidade: String = "Armas Brancas"
    ) = EncantoSolarDefinition(
        id = nome,
        habilidade = habilidade,
        nome = nome,
        nomeIngles = nome,
        custo = "1",
        minsTexto = "Armas Brancas 1, Essência 1",
        minHabilidade = 1,
        minEssencia = 1,
        tipo = "Encanto",
        palavrasChave = "",
        duracao = "Permanente",
        preRequisitos = preRequisitos,
        descricao = ""
    ) to custo

    @Test
    fun `rota escolhe pre requisito que abre cadeia futura`() {
        val (a, custoA) = encanto("A", 10)
        val (b, _) = encanto("B", 10, "A")
        val (c, _) = encanto("C", 10, "B")
        val (x, _) = encanto("X", 10)
        val catalogo = listOf(a, b, c, x)

        val escolhido = EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(x, a),
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { def, nomes, _ ->
                def.preRequisitos.isBlank() || def.preRequisitos in nomes
            },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { if (it == a) custoA else 10 }
        )

        assertEquals("A", escolhido?.nome)
    }

    @Test
    fun `rota prefere caminho de 8 XP quando o desbloqueio e equivalente`() {
        val (barato, _) = encanto("Barato", 8)
        val (caro, _) = encanto("Caro", 10)
        val catalogo = listOf(barato, caro)

        val escolhido = EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(caro, barato),
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { if (it.nome == "Barato") 8 else 10 }
        )

        assertEquals("Barato", escolhido?.nome)
        assertNotEquals("Caro", escolhido?.nome)
    }
    @Test
    fun `rota permite aquisicao repetida de encanto explicitamente repetivel`() {
        val repetivel = EncantoSolarDefinition(
            id = "repetivel", habilidade = "Resistência", nome = "Corpo Repetível",
            nomeIngles = "Repeatable", custo = "1", minsTexto = "Resistência •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "", descricao = ""
        )
        val dependente = EncantoSolarDefinition(
            id = "dependente", habilidade = "Resistência", nome = "Dependente",
            nomeIngles = "Dependent", custo = "1", minsTexto = "Resistência •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "Quaisquer dois Encantos de Resistência", descricao = ""
        )

        val escolhido = EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(dependente, repetivel),
            catalogoCompleto = listOf(dependente, repetivel),
            nomesSelecionados = setOf("Corpo Repetível"),
            contagensCategorias = mapOf("resistência" to 1),
            elegivel = { def, _, contagens ->
                if (def.nome == "Corpo Repetível") true
                else (contagens["resistência"] ?: 0) >= 2
            },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            permiteAquisicaoRepetida = { it.nome == "Corpo Repetível" }
        )

        assertEquals("Corpo Repetível", escolhido?.nome)
    }

    @Test
    fun `cache de elegibilidade evita recalcular o mesmo estado`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8)
        val (c, _) = encanto("C", 8)
        val catalogo = listOf(a, b, c)
        var chamadasElegibilidade = 0
        val metrics = EncounterCharmRouteMetrics()

        EncounterCharmRouteOptimizer.escolher(
            candidatos = catalogo,
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> chamadasElegibilidade++; true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            beamWidth = 6,
            profundidade = 3,
            metrics = metrics
        )

        val snapshot = metrics.snapshot()
        assert(snapshot.eligibilityCacheHits > 0)
        assert(snapshot.eligibilityCacheMisses > 0)
        // Cada miss avalia apenas aquisições ainda possíveis; depois que um Encanto
        // não repetível é adquirido, ele deixa de integrar o pool daquele estado.
        // Portanto misses * tamanhoDoCatalogo é um limite superior, não uma igualdade.
        assert(chamadasElegibilidade > 0)
        assert(chamadasElegibilidade.toLong() <= snapshot.eligibilityCacheMisses * catalogo.size.toLong())
    }

    @Test
    fun `caminhos que convergem ao mesmo estado sao deduplicados antes do proximo nivel`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8)
        val catalogo = listOf(a, b)
        val metrics = EncounterCharmRouteMetrics()

        EncounterCharmRouteOptimizer.escolher(
            candidatos = catalogo,
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            beamWidth = 6,
            profundidade = 3,
            metrics = metrics
        )

        assert(metrics.snapshot().statesDeduplicated > 0)
    }


    @Test
    fun `memo compartilhado evita reavaliar estados entre escolhas consecutivas`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8)
        val (c, _) = encanto("C", 8)
        val catalogo = listOf(a, b, c)
        val memo = HashMap<EncounterCharmRouteOptimizer.EligibilityKey, Set<String>>()
        var chamadas = 0

        fun executar() = EncounterCharmRouteOptimizer.escolher(
            candidatos = catalogo,
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> chamadas++; true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            eligibilityMemo = memo
        )

        val primeiro = executar()
        val chamadasPrimeira = chamadas
        val segundo = executar()

        assertEquals(primeiro?.nome, segundo?.nome)
        assertEquals("segunda busca equivalente deve reutilizar toda a legalidade", chamadasPrimeira, chamadas)
        assert(memo.isNotEmpty())
    }

    @Test
    fun `caminho monotônico preserva escolha e reduz checagens de elegibilidade`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8, "A")
        val (c, _) = encanto("C", 8, "B")
        val (x, _) = encanto("X", 8)
        val catalogo = listOf(a, b, c, x)

        fun executar(monotonic: Boolean): Pair<String?, EncounterCharmRouteMetrics.Snapshot> {
            val metrics = EncounterCharmRouteMetrics()
            val escolhido = EncounterCharmRouteOptimizer.escolher(
                candidatos = listOf(x, a),
                catalogoCompleto = catalogo,
                nomesSelecionados = emptySet(),
                contagensCategorias = emptyMap(),
                elegivel = { def, nomes, _ ->
                    def.preRequisitos.isBlank() || def.preRequisitos in nomes
                },
                nome = { it.nome },
                categoria = { it.habilidade },
                custoXp = { 8 },
                metrics = metrics,
                monotonicEligibility = monotonic
            )
            return escolhido?.nome to metrics.snapshot()
        }

        val legacy = executar(false)
        val incremental = executar(true)
        assertEquals(legacy.first, incremental.first)
        assert(incremental.second.eligibilityChecks <= legacy.second.eligibilityChecks)
        assert(incremental.second.marginalUnlockCalls > 0)
    }

    @Test
    fun `delta direcionado preserva escolha com menos checagens que monotonia ampla`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8, "A")
        val independentes = (1..20).map { encanto("X$it", 8).first }
        val catalogo = listOf(a, b) + independentes

        fun executar(direcionado: Boolean): Pair<String?, Long> {
            val metrics = EncounterCharmRouteMetrics()
            val escolhido = EncounterCharmRouteOptimizer.escolher(
                candidatos = listOf(a) + independentes.take(2),
                catalogoCompleto = catalogo,
                nomesSelecionados = emptySet(),
                contagensCategorias = emptyMap(),
                elegivel = { def, nomes, _ -> def.preRequisitos.isBlank() || def.preRequisitos in nomes },
                nome = { it.nome },
                categoria = { it.habilidade },
                custoXp = { 8 },
                metrics = metrics,
                monotonicEligibility = true,
                monotonicAffectedNames = if (direcionado) {
                    { candidate, _, _, _ -> if (candidate.nome == "A") setOf("B") else emptySet() }
                } else null
            )
            return escolhido?.nome to metrics.snapshot().eligibilityChecks
        }

        val amplo = executar(false)
        val direcionado = executar(true)
        assertEquals(amplo.first, direcionado.first)
        assertTrue("delta direcionado deve reduzir checagens", direcionado.second < amplo.second)
    }

    @Test
    fun `memo compacto preserva escolha e reutiliza legalidade entre chamadas`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8, "A")
        val (x, _) = encanto("X", 8)
        val catalogo = listOf(a, b, x)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(catalogo, { it.nome }, { it.habilidade })
        val memo = HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()
        var chamadas = 0

        fun executar() = EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(x, a),
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { def, nomes, _ -> chamadas++; def.preRequisitos.isBlank() || def.preRequisitos in nomes },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            preparedCatalog = prepared,
            compactEligibilityMemo = memo,
            monotonicEligibility = true
        )

        val primeiro = executar()
        val chamadasPrimeira = chamadas
        val segundo = executar()
        assertEquals(primeiro?.nome, segundo?.nome)
        assertEquals(chamadasPrimeira, chamadas)
        assertTrue(memo.isNotEmpty())
    }

    @Test
    fun `chave compacta e canonica para ordem diferente do mesmo estado`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(listOf(a, b), { it.nome }, { it.habilidade })
        val first = prepared.compactKey(linkedSetOf("A", "B"), linkedMapOf("armas brancas" to 2))
        val second = prepared.compactKey(linkedSetOf("B", "A"), linkedMapOf("armas brancas" to 2))
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `rota valoriza crescimento futuro coerente na mesma arvore`() {
        val (rotaProfunda, _) = encanto("Z Rota Profunda", 10, habilidade = "Armas Brancas")
        val (descendenteProfundo, _) = encanto("Descendente Profundo", 10, "Z Rota Profunda", "Armas Brancas")
        val (rotaDispersa, _) = encanto("A Rota Dispersa", 10, habilidade = "Atletismo")
        val (descendenteDisperso, _) = encanto("Descendente Disperso", 10, "A Rota Dispersa", "Ocultismo")
        val catalogo = listOf(rotaProfunda, descendenteProfundo, rotaDispersa, descendenteDisperso)

        val escolhido = EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(rotaDispersa, rotaProfunda),
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { def, nomes, _ -> def.preRequisitos.isBlank() || def.preRequisitos in nomes },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 10 },
            profundidade = 1
        )

        assertEquals("Z Rota Profunda", escolhido?.nome)
    }

    @Test
    fun `chave incremental equivale a reconstrucao integral`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(listOf(a, b), { it.nome }, { it.habilidade })
        val parent = prepared.compactKey(setOf("A"), mapOf("armas brancas" to 1))
        val child = prepared.compactKeyAfter(parent, "B", "armas brancas")
        val rebuilt = prepared.compactKey(setOf("A", "B"), mapOf("armas brancas" to 2))
        assertEquals(rebuilt, child)
        assertEquals(prepared.compactKey(setOf("A"), mapOf("armas brancas" to 1)), parent)
    }

    @Test
    fun `chave incremental preserva overflow canonico e nome desconhecido`() {
        val (a, _) = encanto("A", 8)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(listOf(a), { it.nome }, { it.habilidade })
        val parent = prepared.compactKey(
            setOf("A"),
            linkedMapOf("zeta dinâmica" to 1, "armas brancas" to 1)
        )
        val child = prepared.compactKeyAfter(parent, "fora-do-catalogo", "alfa dinâmica")
        val rebuilt = prepared.compactKey(
            setOf("A", "fora-do-catalogo"),
            linkedMapOf("armas brancas" to 1, "zeta dinâmica" to 1, "alfa dinâmica" to 1)
        )
        assertEquals(rebuilt, child)
        assertEquals(listOf("alfa dinâmica", "zeta dinâmica"), child.overflowCounts.map { it.first })

        val repeated = prepared.compactKeyAfter(child, "fora-do-catalogo", "alfa dinâmica")
        val rebuiltRepeated = prepared.compactKey(
            setOf("A", "fora-do-catalogo"),
            linkedMapOf("armas brancas" to 1, "zeta dinâmica" to 1, "alfa dinâmica" to 2)
        )
        assertEquals(rebuiltRepeated, repeated)
    }

    @Test
    fun `custo XP permanece local a cada chamada mesmo com catalogo preparado`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8)
        val catalogo = listOf(a, b)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(catalogo, { it.nome }, { it.habilidade })

        fun escolher(barato: String) = EncounterCharmRouteOptimizer.escolher(
            candidatos = catalogo,
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { if (it.nome == barato) 8 else 10 },
            preparedCatalog = prepared,
            profundidade = 1
        )

        assertEquals("A", escolher("A")?.nome)
        assertEquals("B", escolher("B")?.nome)
    }

    @Test
    fun `categoria dinamica e calculada uma vez por aquisicao avaliada`() {
        val (a, _) = encanto("A", 8)
        var chamadasCategoriaDinamica = 0

        val escolhido = EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(a),
            catalogoCompleto = listOf(a),
            nomesSelecionados = emptySet(),
            contagensCategorias = mapOf("foco" to 1),
            elegivel = { _, _, _ -> true },
            nome = { it.nome },
            categoria = { it.habilidade },
            categoriaNoEstado = { _, _, _ ->
                chamadasCategoriaDinamica++
                "foco"
            },
            custoXp = { 8 },
            profundidade = 1
        )

        assertEquals("A", escolhido?.nome)
        assertEquals(1, chamadasCategoriaDinamica)
    }

    @Test
    fun `orçamento esgotado não publica elegibilidade parcial no memo`() {
        val (a, _) = encanto("A", 8)
        val (b, _) = encanto("B", 8)
        val (c, _) = encanto("C", 8)
        val catalogo = listOf(a, b, c)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(catalogo, { it.nome }, { it.habilidade })
        val memo = HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()
        val estadoBase = prepared.compactKey(emptySet(), emptyMap())

        val metrics = EncounterCharmRouteMetrics()
        EncounterCharmRouteOptimizer.escolher(
            candidatos = catalogo,
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            preparedCatalog = prepared,
            compactEligibilityMemo = memo,
            metrics = metrics,
            maxWorkUnits = 1
        )

        assertTrue("estado incompleto não pode contaminar chamadas futuras", estadoBase !in memo)
        assertEquals(1L, metrics.snapshot().budgetExhaustions)
    }


    @Test
    fun `memo incremental nao conserva encanto nao repetivel ja adquirido`() {
        val (base, _) = encanto("Base", 8)
        val (outro, _) = encanto("Outro", 8)
        val catalogo = listOf(base, outro)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(
            catalogo, { it.nome }, { it.habilidade }
        )
        val memo = HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()
        EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(base),
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            preparedCatalog = prepared,
            compactEligibilityMemo = memo,
            monotonicEligibility = true,
            profundidade = 1
        )
        val estadoAposCompra = prepared.compactKey(setOf("Base"), mapOf("armas brancas" to 1))
        val elegiveis = memo[estadoAposCompra]
        assertTrue("estado pos-compra deve ter sido avaliado", elegiveis != null)
        assertFalse("encanto nao repetivel nao pode permanecer elegivel", "Base" in elegiveis.orEmpty())
    }

    @Test
    fun `memo incremental preserva encanto repetivel depois da compra`() {
        val (repetivel, _) = encanto("Repetivel", 8)
        val (outro, _) = encanto("Outro", 8)
        val catalogo = listOf(repetivel, outro)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(
            catalogo, { it.nome }, { it.habilidade }
        )
        val memo = HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()
        EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(repetivel),
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { _, _, _ -> true },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            permiteAquisicaoRepetida = { it.nome == "Repetivel" },
            preparedCatalog = prepared,
            compactEligibilityMemo = memo,
            monotonicEligibility = true,
            profundidade = 1
        )
        val estadoAposCompra = prepared.compactKey(
            setOf("Repetivel"), mapOf("armas brancas" to 1)
        )
        val elegiveis = memo[estadoAposCompra]
        assertTrue("estado pos-compra deve ter sido avaliado", elegiveis != null)
        assertTrue("encanto repetivel deve permanecer elegivel", "Repetivel" in elegiveis.orEmpty())
    }

    @Test
    fun `orcamento esgotado no delta nao publica estado pos compra no memo`() {
        val (base, _) = encanto("Base", 8)
        val (dependente, _) = encanto("Dependente", 8, "Base")
        val catalogo = listOf(base, dependente)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(
            catalogo, { it.nome }, { it.habilidade }
        )
        val memo = HashMap<EncounterCharmRouteOptimizer.CompactEligibilityKey, Set<String>>()
        val estadoAposCompra = prepared.compactKey(
            setOf("Base"), mapOf("armas brancas" to 1)
        )
        val metrics = EncounterCharmRouteMetrics()

        EncounterCharmRouteOptimizer.escolher(
            candidatos = listOf(base),
            catalogoCompleto = catalogo,
            nomesSelecionados = emptySet(),
            contagensCategorias = emptyMap(),
            elegivel = { def, nomes, _ ->
                def.preRequisitos.isBlank() || def.preRequisitos in nomes
            },
            nome = { it.nome },
            categoria = { it.habilidade },
            custoXp = { 8 },
            preparedCatalog = prepared,
            compactEligibilityMemo = memo,
            monotonicEligibility = true,
            maxWorkUnits = 3,
            metrics = metrics,
            profundidade = 1
        )

        assertTrue("orçamento deve se esgotar durante o delta", metrics.snapshot().budgetExhaustions > 0)
        assertTrue("estado pós-compra parcial não pode entrar no memo", estadoAposCompra !in memo)
    }

    @Test
    fun `chave incremental equivale a reconstrucao para categoria conhecida e extra`() {
        val (base, _) = encanto("Base", 8)
        val (outro, _) = encanto("Outro", 8, habilidade = "Resistência")
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(
            listOf(base, outro), { it.nome }, { it.habilidade }
        )
        val parent = prepared.compactKey(
            setOf("Outro"), mapOf("resistência" to 1, "categoria extra" to 2)
        )
        val expectedKnown = prepared.compactKey(
            setOf("Outro", "Base"),
            mapOf("resistência" to 1, "categoria extra" to 2, "armas brancas" to 1)
        )
        assertEquals(expectedKnown, prepared.compactKeyAfter(parent, "Base", "Armas Brancas"))

        val expectedExtra = prepared.compactKey(
            setOf("Outro", "Base"),
            mapOf("resistência" to 1, "categoria extra" to 3)
        )
        assertEquals(expectedExtra, prepared.compactKeyAfter(parent, "Base", "Categoria Extra"))
    }

    @Test
    fun `chave compacta distingue encantos externos ao catalogo preparado`() {
        val (base, _) = encanto("Base", 8)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(
            listOf(base), { it.nome }, { it.habilidade }
        )
        val semExterno = prepared.compactKey(emptySet(), emptyMap())
        val comExterno = prepared.compactKey(setOf("Encanto Externo"), emptyMap())
        assertNotEquals(semExterno, comExterno)
        assertEquals(
            prepared.compactKey(setOf("Encanto Externo"), mapOf("armas brancas" to 1)),
            prepared.compactKeyAfter(semExterno, "Encanto Externo", "Armas Brancas")
        )
    }

    @Test
    fun `chave externa e canonica e nao colide entre selecoes diferentes`() {
        val (base, _) = encanto("Base", 8)
        val prepared = EncounterCharmRouteOptimizer.prepareCatalog(
            listOf(base), { it.nome }, { it.habilidade }
        )
        val first = prepared.compactKey(
            linkedSetOf("Externo A", "Base", "Externo B"),
            mapOf("armas brancas" to 1)
        )
        val reordered = prepared.compactKey(
            linkedSetOf("Externo B", "Externo A", "Base"),
            mapOf("armas brancas" to 1)
        )
        val different = prepared.compactKey(
            linkedSetOf("Externo A", "Base", "Externo C"),
            mapOf("armas brancas" to 1)
        )
        assertEquals(first, reordered)
        assertNotEquals(first, different)
        val memo = hashMapOf(first to setOf("Base"))
        assertEquals(setOf("Base"), memo[reordered])
        assertFalse("estado com outro Encanto externo nao deve herdar memo", different in memo)
    }

}
