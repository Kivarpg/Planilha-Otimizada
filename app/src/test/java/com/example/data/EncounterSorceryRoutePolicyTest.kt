package com.example.data

import com.example.model.ArquetipoEncontro
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterSorceryRoutePolicyTest {
    @Test
    fun `focus feiticaria exige exploracao em qualquer arquetipo`() {
        ArquetipoEncontro.values().forEach { archetype ->
            val decision = EncounterSorceryRoutePolicy.decide(archetype, true, Random(1))
            assertTrue(decision.explore)
            assertEquals(EncounterSorceryRoutePolicy.Reason.EXPLICIT_FOCUS, decision.reason)
        }
    }

    @Test
    fun `sem focus arquetipo nao mental nao explora rota automaticamente`() {
        listOf(ArquetipoEncontro.FISICO, ArquetipoEncontro.SOCIAL).forEach { archetype ->
            val decision = EncounterSorceryRoutePolicy.decide(archetype, false, Random(2))
            assertFalse(decision.explore)
            assertEquals(EncounterSorceryRoutePolicy.Reason.NOT_EXPLORED, decision.reason)
        }
    }

    @Test
    fun `mental automatico preserva regra nove em dez`() {
        val actual = (0 until 200).map { seed ->
            EncounterSorceryRoutePolicy.decide(
                ArquetipoEncontro.MENTAL, false, Random(seed)
            ).explore
        }
        val expected = (0 until 200).map { seed ->
            EncounterGenerationRules.sortearFeiticariaMental(Random(seed))
        }
        assertEquals(expected, actual)
    }
    @Test
    fun `uma decisao de exploracao pode ser reutilizada entre passagens sem novo sorteio`() {
        val decision = EncounterSorceryRoutePolicy.decide(
            ArquetipoEncontro.MENTAL, false, Random(37)
        )
        val primeiraPassagem = decision.explore
        val segundaPassagem = decision.explore
        assertEquals(primeiraPassagem, segundaPassagem)
    }


    @Test
    fun `rota terrestre solar ou draconica exige Ocultismo minimo antes de reservar vagas`() {
        assertFalse(EncounterSorceryRoutePolicy.canConstructTerrestrialWithOccultism(mapOf("Ocultismo" to 2)))
        assertTrue(EncounterSorceryRoutePolicy.canConstructTerrestrialWithOccultism(mapOf("Ocultismo" to 3)))
        assertTrue(EncounterSorceryRoutePolicy.canConstructTerrestrialWithOccultism(mapOf("Ocultismo" to 5)))
    }


    @Test
    fun `prioridade de ocultismo exige rota materializavel`() {
        val insuficiente = mapOf("Ocultismo" to 2)
        val suficiente = mapOf("Ocultismo" to 3)

        assertFalse(EncounterSorceryRoutePolicy.shouldPrioritizeOccultism(ArquetipoEncontro.MENTAL, true, insuficiente))
        assertFalse(EncounterSorceryRoutePolicy.shouldPrioritizeOccultism(ArquetipoEncontro.MENTAL, false, suficiente))
        assertTrue(EncounterSorceryRoutePolicy.shouldPrioritizeOccultism(ArquetipoEncontro.MENTAL, true, suficiente))
        assertTrue(EncounterSorceryRoutePolicy.shouldPrioritizeOccultism(ArquetipoEncontro.SOCIAL, true, suficiente))
    }

    @Test
    fun `fisico preserva prioridade historica quando ocultismo permite a rota`() {
        assertTrue(
            EncounterSorceryRoutePolicy.shouldPrioritizeOccultism(
                ArquetipoEncontro.FISICO, false, mapOf("Ocultismo" to 3)
            )
        )
    }

    @Test
    fun `projeto terrestre automatico exige exploracao catalogo e ocultismo minimo`() {
        val ocultismo3 = mapOf(EncounterGenerationRules.HABILIDADE_OCULTISMO to 3)
        assertFalse(EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject(
            ArquetipoEncontro.MENTAL, false, ocultismo3, true
        ))
        assertFalse(EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject(
            ArquetipoEncontro.MENTAL, true, ocultismo3, false
        ))
        assertTrue(EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject(
            ArquetipoEncontro.MENTAL, true, ocultismo3, true
        ))
    }

    @Test
    fun `fisico preserva projeto estrutural com ocultismo minimo`() {
        val ocultismo3 = mapOf(EncounterGenerationRules.HABILIDADE_OCULTISMO to 3)
        assertTrue(EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject(
            ArquetipoEncontro.FISICO, false, ocultismo3, false
        ))
        assertFalse(EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject(
            ArquetipoEncontro.FISICO, false,
            mapOf(EncounterGenerationRules.HABILIDADE_OCULTISMO to 2), true
        ))
    }



    @Test
    fun `exploracao automatica nao obriga candidato de feiticaria`() {
        val pura = EncounterBuildQuality.combine(current = 8, future = 2)
        val feiticaria = EncounterBuildQuality.combine(current = 7, future = 2)
        assertFalse(
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = false,
                exploreSorcery = true,
                sorceryConstructible = true,
                pureQuality = pura,
                sorceryQuality = feiticaria
            )
        )
    }

    @Test
    fun `exploracao automatica escolhe feiticaria somente quando qualidade e maior`() {
        val pura = EncounterBuildQuality.combine(current = 7, future = 1)
        val feiticaria = EncounterBuildQuality.combine(current = 8, future = 2)
        assertTrue(
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = false,
                exploreSorcery = true,
                sorceryConstructible = true,
                pureQuality = pura,
                sorceryQuality = feiticaria
            )
        )
    }

    @Test
    fun `empate automatico preserva candidato puro`() {
        val pura = EncounterBuildQuality.combine(current = 8, future = 2)
        val feiticaria = EncounterBuildQuality.combine(current = 8, future = 2)
        assertFalse(
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = false,
                exploreSorcery = true,
                sorceryConstructible = true,
                pureQuality = pura,
                sorceryQuality = feiticaria
            )
        )
    }

    @Test
    fun `focus explicito seleciona feiticaria quando rota e legal`() {
        assertTrue(
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = true,
                exploreSorcery = true,
                sorceryConstructible = true,
                pureQuality = EncounterBuildQuality.combine(20, 3),
                sorceryQuality = EncounterBuildQuality.combine(1, 0)
            )
        )
    }

    @Test
    fun `rota ilegal nunca e selecionada nem com focus explicito`() {
        assertFalse(
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = true,
                exploreSorcery = true,
                sorceryConstructible = false,
                pureQuality = EncounterBuildQuality.combine(1, 0),
                sorceryQuality = EncounterBuildQuality.combine(20, 3)
            )
        )
    }


    @Test
    fun `qualidade concreta favorece conjunto alinhado a habilidades mais altas`() {
        fun charm(nome: String, habilidade: String) = EncantoSolarDefinition(
            id = nome,
            habilidade = habilidade,
            nome = nome,
            nomeIngles = nome,
            custo = "-",
            minsTexto = "$habilidade 1, Essência 1",
            minHabilidade = 1,
            minEssencia = 1,
            tipo = "Permanente",
            palavrasChave = "",
            duracao = "Permanente",
            preRequisitos = "",
            descricao = ""
        )
        val abilities = mapOf("Ocultismo" to 5, "Investigação" to 3)
        val ocultismo = EncounterSorceryRoutePolicy.qualityOfTerrestrialCharmCandidate(
            listOf(charm("O1", "Ocultismo"), charm("O2", "Ocultismo")), abilities
        )
        val investigacao = EncounterSorceryRoutePolicy.qualityOfTerrestrialCharmCandidate(
            listOf(charm("I1", "Investigação"), charm("I2", "Investigação")), abilities
        )
        assertTrue(ocultismo.total > investigacao.total)
    }


    @Test
    fun `seed de comparacao lunar e deterministico e independente da ordem do mapa`() {
        val a = EncounterSorceryRoutePolicy.lunarComparisonSeed(
            attributes = linkedMapOf("Inteligência" to 5, "Percepção" to 3),
            focus = "Inteligência",
            spiritTraits = emptySet()
        )
        val b = EncounterSorceryRoutePolicy.lunarComparisonSeed(
            attributes = linkedMapOf("Percepção" to 3, "Inteligência" to 5),
            focus = "Inteligência",
            spiritTraits = emptySet()
        )
        assertEquals(a, b)
    }

    @Test
    fun `qualidade lunar usa atributos reais da ficha`() {
        fun charm(nome: String, atributo: String) = EncantoLunarDefinition(
            id = nome,
            atributo = atributo,
            subdivisao = null,
            nome = nome,
            nomeIngles = nome,
            custo = "-",
            minsTexto = "$atributo 1, Essência 1",
            minAtributo = 1,
            minEssencia = 1,
            tipo = "Permanente",
            palavrasChave = "",
            duracao = "Permanente",
            preRequisitos = "",
            descricao = ""
        )
        val attributes = mapOf("Inteligência" to 5, "Percepção" to 3)
        val inteligencia = EncounterSorceryRoutePolicy.qualityOfLunarCharmCandidate(
            listOf(charm("I1", "Inteligência"), charm("I2", "Inteligência")), attributes
        )
        val percepcao = EncounterSorceryRoutePolicy.qualityOfLunarCharmCandidate(
            listOf(charm("P1", "Percepção"), charm("P2", "Percepção")), attributes
        )
        assertTrue(inteligencia.total > percepcao.total)
    }


    @Test
    fun `comparacao automatica existe somente no mental explorado construivel e sem focus explicito`() {
        assertTrue(
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                ArquetipoEncontro.MENTAL, true, false, true
            )
        )
        assertFalse(
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                ArquetipoEncontro.MENTAL, false, false, true
            )
        )
        assertFalse(
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                ArquetipoEncontro.MENTAL, true, true, true
            )
        )
        assertFalse(
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                ArquetipoEncontro.MENTAL, true, false, false
            )
        )
        assertFalse(
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                ArquetipoEncontro.FISICO, true, false, true
            )
        )
        assertFalse(
            EncounterSorceryRoutePolicy.shouldCompareAutomaticCandidates(
                ArquetipoEncontro.SOCIAL, true, false, true
            )
        )
    }

    @Test
    fun `selecao automatica nunca escolhe rota nao construivel`() {
        assertFalse(
            EncounterSorceryRoutePolicy.shouldSelectSorceryCandidate(
                explicitSorceryFocus = false,
                exploreSorcery = true,
                sorceryConstructible = false,
                pureQuality = EncounterBuildQuality.combine(0, 0),
                sorceryQuality = EncounterBuildQuality.combine(100, 100)
            )
        )
    }

}
