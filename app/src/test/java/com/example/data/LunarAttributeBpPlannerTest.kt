package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarAttributeBpPlannerTest {
    private val especiais = listOf("Força", "Destreza", "Carisma", "Inteligência")
    private val atributos = mapOf(
        "Força" to 3, "Destreza" to 3, "Vigor" to 2,
        "Carisma" to 3, "Manipulação" to 2, "Aparência" to 2,
        "Percepção" to 2, "Inteligência" to 3, "Raciocínio" to 2
    )

    private fun charm(nome: String, atributo: String, minimo: Int) = EncantoLunarDefinition(
        id = nome.lowercase().replace(" ", "_"),
        atributo = atributo,
        nome = nome,
        nomeIngles = nome,
        custo = "4m",
        minsTexto = "$atributo $minimo, Essência 1",
        minAtributo = minimo,
        minEssencia = 1,
        tipo = "Reflexivo",
        palavrasChave = "",
        duracao = "Instantâneo",
        preRequisitos = "Nenhum",
        descricao = "",
        subdivisao = null
    )

    @Test
    fun `projecao e deterministica e preserva exatamente candidatos legais`() {
        val catalogo = listOf(
            charm("Força A", "Força", 4),
            charm("Destreza A", "Destreza", 4),
            charm("Carisma A", "Carisma", 3),
            charm("Inteligência A", "Inteligência", 5)
        )
        val a = LunarAttributeBpPlanner.project(
            ArquetipoEncontro.FISICO, atributos, especiais, catalogo, null
        )
        val b = LunarAttributeBpPlanner.project(
            ArquetipoEncontro.FISICO, atributos, especiais, catalogo, null
        )

        assertEquals(a, b)
        assertEquals(especiais.toSet(), a.priority.toSet())
        assertEquals(especiais.size, a.priority.size)
    }

    @Test
    fun `foco explicito domina entre atributos legalmente elegiveis`() {
        val catalogo = (1..8).map { charm("Força $it", "Força", 4) } +
            charm("Inteligência foco", "Inteligência", 4)

        val plano = LunarAttributeBpPlanner.project(
            ArquetipoEncontro.FISICO, atributos, especiais, catalogo, "Inteligência"
        )

        assertEquals("Inteligência", plano.priority.first())
    }

    @Test
    fun `foco fora do conjunto legal nao entra na prioridade`() {
        val plano = LunarAttributeBpPlanner.project(
            ArquetipoEncontro.MENTAL, atributos, especiais, emptyList(), "Raciocínio"
        )

        assertTrue("Raciocínio" !in plano.priority)
        assertEquals(especiais.toSet(), plano.priority.toSet())
    }

    @Test
    fun `gancho vazio preserva distribuicao lunar anterior para mesma semente`() {
        val abilities = ExaltedConstants.ALL_25_ABILITIES.associateWith { 0 }.toMutableMap().apply {
            put("Briga", 3)
            put("Esquiva", 3)
            put("Prontidão", 3)
            put("Atletismo", 3)
            put("Resistência", 3)
            put("Sobrevivência", 3)
        }
        val seed = 917
        val anterior = EncounterBonusPointDistribution.distribuirPontosDeBonusLunar(
            ArquetipoEncontro.FISICO, abilities, atributos, especiais,
            "Briga", "Esquiva", "Prontidão", Random(seed)
        )
        val comGanchoVazio = EncounterBonusPointDistribution.distribuirPontosDeBonusLunar(
            ArquetipoEncontro.FISICO, abilities, atributos, especiais,
            "Briga", "Esquiva", "Prontidão", Random(seed), prioridadeAtributos = emptyList()
        )

        assertEquals(anterior, comGanchoVazio)
    }
    @Test
    fun `shadow identifica melhoria sem alterar conjunto legal`() {
        val base = atributos.toMutableMap().apply {
            put("Força", 4)
            put("Inteligência", 3)
        }
        val catalogo = listOf(
            charm("Força já acessível", "Força", 3),
            charm("Inteligência abre A", "Inteligência", 4),
            charm("Inteligência abre B", "Inteligência", 4)
        )

        val comparacao = LunarAttributeBpPlanner.compareShadow(
            ArquetipoEncontro.MENTAL, base, especiais, catalogo, "Inteligência"
        )

        assertEquals("Força", comparacao.currentPriority.first())
        assertEquals("Inteligência", comparacao.strategicPriority.first())
        assertTrue(comparacao.strategicFirstScore > comparacao.currentFirstScore)
        assertTrue(comparacao.nonRegressive)
    }

    @Test
    fun `shadow considera empate nao regressivo`() {
        val comparacao = LunarAttributeBpPlanner.compareShadow(
            ArquetipoEncontro.FISICO, atributos, especiais, emptyList(), null
        )

        assertTrue(comparacao.nonRegressive)
        assertEquals(especiais.toSet(), comparacao.strategicPriority.toSet())
    }

    @Test
    fun `horizonte usa marcos lunares E1 a E5 e aprova melhoria focada`() {
        val catalogo = listOf(
            charm("Força E1", "Força", 4),
            charm("Inteligência E1", "Inteligência", 4),
            charm("Inteligência E2", "Inteligência", 4).copy(minEssencia = 2),
            charm("Inteligência E3", "Inteligência", 4).copy(minEssencia = 3),
            charm("Inteligência E4", "Inteligência", 4).copy(minEssencia = 4),
            charm("Inteligência E5", "Inteligência", 4).copy(minEssencia = 5)
        )
        val horizonte = LunarAttributeBpPlanner.compareEssenceHorizon(
            attributes = atributos,
            catalog = catalogo,
            currentFirst = "Força",
            strategicFirst = "Inteligência",
            explicitFocus = "Inteligência"
        )

        assertEquals(listOf(0, 50, 125, 200, 300), horizonte.strategic.map { it.xpThreshold })
        assertTrue(horizonte.strategicNonWorseAtEveryMilestone)
        assertTrue(horizonte.strategicStrictlyBetterSomewhere)
    }

    @Test
    fun `horizonte reprova proposta que perde continuidade em algum marco`() {
        val catalogo = listOf(
            charm("Força E1 A", "Força", 4),
            charm("Força E1 B", "Força", 4),
            charm("Força E3", "Força", 4).copy(minEssencia = 3),
            charm("Inteligência E1", "Inteligência", 4)
        )
        val horizonte = LunarAttributeBpPlanner.compareEssenceHorizon(
            attributes = atributos,
            catalog = catalogo,
            currentFirst = "Força",
            strategicFirst = "Inteligência",
            explicitFocus = null
        )

        assertTrue(!horizonte.strategicNonWorseAtEveryMilestone)
    }

    @Test
    fun `prioridade estrategica altera apenas atributo legal na distribuicao real`() {
        val abilities = ExaltedConstants.ALL_25_ABILITIES.associateWith { 0 }.toMutableMap().apply {
            put("Briga", 3)
            put("Esquiva", 3)
            put("Prontidão", 3)
            put("Atletismo", 3)
            put("Resistência", 3)
            put("Sobrevivência", 3)
        }
        val seed = 1441
        val historico = EncounterBonusPointDistribution.distribuirPontosDeBonusLunar(
            ArquetipoEncontro.FISICO, abilities, atributos, especiais,
            "Briga", "Esquiva", "Prontidão", Random(seed)
        )
        val estrategico = EncounterBonusPointDistribution.distribuirPontosDeBonusLunar(
            ArquetipoEncontro.FISICO, abilities, atributos, especiais,
            "Briga", "Esquiva", "Prontidão", Random(seed),
            prioridadeAtributos = listOf("Inteligência", "Carisma", "Destreza", "Força")
        )

        assertEquals(historico.abilities, estrategico.abilities)
        assertEquals(historico.forcaDeVontade, estrategico.forcaDeVontade)
        assertEquals(historico.precisaEspecialidadeAdicional, estrategico.precisaEspecialidadeAdicional)

        val alterados = atributos.keys.filter { historico.attributes[it] != estrategico.attributes[it] }
        assertTrue(alterados.all { it in especiais })
        assertEquals(
            historico.attributes.values.sum(),
            estrategico.attributes.values.sum()
        )
    }

    @Test
    fun `gate de qualidade E1 nao permite crescimento futuro compensar regressao presente`() {
        val atual = EncounterBuildQuality.combine(current = 10, future = 0)
        val regressivoComFuturo = EncounterBuildQuality.combine(current = 9, future = 99)

        assertEquals(10, atual.total)
        assertEquals(12, regressivoComFuturo.total)
        // O total agregado sozinho não é gate suficiente: qualidade presente
        // é requisito independente antes de considerar crescimento.
        assertTrue(regressivoComFuturo.current < atual.current)
        assertEquals(EncounterBuildQuality.MAX_FUTURE_GROWTH_BONUS, regressivoComFuturo.future)
    }

    @Test
    fun `prioridade estrategica so pode ser candidata se gate local e longitudinal passarem`() {
        val catalogo = listOf(
            charm("Força A", "Força", 4),
            charm("Inteligência A", "Inteligência", 4),
            charm("Inteligência E2", "Inteligência", 4).copy(minEssencia = 2)
        )
        val local = LunarAttributeBpPlanner.compareShadow(
            ArquetipoEncontro.MENTAL, atributos, especiais, catalogo, "Inteligência"
        )
        val longitudinal = LunarAttributeBpPlanner.compareEssenceHorizon(
            atributos, catalogo,
            local.currentPriority.firstOrNull(),
            local.strategicPriority.firstOrNull(),
            "Inteligência"
        )

        assertTrue(local.nonRegressive)
        assertTrue(longitudinal.strategicNonWorseAtEveryMilestone)
    }

    @Test
    fun `preview A B de atributos e deterministico e nao muta entrada`() {
        val entrada = atributos.toMap()
        val historico = EncounterBonusPointDistribution.previewComprasAtributosLunares(
            attributesBase = entrada,
            atributosCastaOuFavorecidos = especiais,
            saldoInicial = 5
        )
        val estrategico = EncounterBonusPointDistribution.previewComprasAtributosLunares(
            attributesBase = entrada,
            atributosCastaOuFavorecidos = especiais,
            saldoInicial = 5,
            prioridadeAtributos = listOf("Inteligência", "Carisma", "Destreza", "Força")
        )

        assertEquals(atributos, entrada)
        assertEquals(historico.saldoRestante, estrategico.saldoRestante)
        assertEquals(historico.attributes.values.sum(), estrategico.attributes.values.sum())
        assertTrue(
            atributos.keys.filter { historico.attributes[it] != estrategico.attributes[it] }
                .all { it in especiais }
        )
    }

    @Test
    fun `preview respeita custo de tres PB e teto cinco`() {
        val base = atributos.toMutableMap().apply { put("Inteligência", 5) }
        val preview = EncounterBonusPointDistribution.previewComprasAtributosLunares(
            attributesBase = base,
            atributosCastaOuFavorecidos = especiais,
            saldoInicial = 5,
            prioridadeAtributos = listOf("Inteligência", "Carisma", "Destreza", "Força")
        )

        assertEquals(2, preview.saldoRestante)
        assertEquals(5, preview.attributes.getValue("Inteligência"))
        assertEquals(4, preview.attributes.getValue("Carisma"))
    }

    @Test
    fun `foco ofensivo lunar escolhe somente um atributo prioritario`() {
        val catalogo = (1..5).map { charm("Força ofensiva $it", "Força", 4) } +
            (1..5).map { charm("Destreza ofensiva $it", "Destreza", 4) } +
            (1..5).map { charm("Vigor defensivo $it", "Vigor", 3) }
        val elegiveis = listOf("Força", "Destreza", "Vigor", "Carisma")
        for ((foco, outro) in listOf("Força" to "Destreza", "Destreza" to "Força")) {
            val projecao = LunarAttributeBpPlanner.project(
                ArquetipoEncontro.FISICO, atributos, elegiveis, catalogo, foco
            )
            assertEquals(foco, projecao.priority.first())
            assertEquals(1, projecao.priority.count { it == foco })
            assertTrue(outro in projecao.priority)
            assertEquals(elegiveis.toSet(), projecao.priority.toSet())
        }
    }

    @Test
    fun `activation priority falha fechada quando horizonte reprova`() {
        val local = LunarAttributeBpPlanner.Comparison(
            currentPriority = especiais,
            strategicPriority = especiais.reversed(),
            sameFirstChoice = false,
            currentFirstScore = 10,
            strategicFirstScore = 11,
            nonRegressive = true
        )
        val horizon = LunarAttributeBpPlanner.HorizonComparison(
            current = emptyList(),
            strategic = emptyList(),
            strategicNonWorseAtEveryMilestone = false,
            strategicStrictlyBetterSomewhere = false
        )

        assertTrue(LunarAttributeBpPlanner.activationPriority(local, horizon).isEmpty())
    }

}
