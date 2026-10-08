package com.example.data

import com.example.model.ArquetipoEncontro
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncounterArchetypePolicyTest {
    @Test
    fun `proporcoes de artes marciais por arquetipo sao 70 50 30`() {
        assertEquals(70, EncounterArchetypePolicy.chanceArtesMarciaisPercent(ArquetipoEncontro.FISICO))
        assertEquals(50, EncounterArchetypePolicy.chanceArtesMarciaisPercent(ArquetipoEncontro.SOCIAL))
        assertEquals(30, EncounterArchetypePolicy.chanceArtesMarciaisPercent(ArquetipoEncontro.MENTAL))
    }

    @Test
    fun `briga zero nunca permite rota de artes marciais`() {
        repeat(EncounterTestSamples.count(100)) { seed ->
            assertFalse(EncounterArchetypePolicy.devePreferirArtesMarciais(ArquetipoEncontro.FISICO, 0, Random(seed)))
        }
    }

    @Test
    fun `com briga um a decisao permanece probabilistica`() {
        val fisico = (0 until 1000).count {
            EncounterArchetypePolicy.devePreferirArtesMarciais(ArquetipoEncontro.FISICO, 1, Random(it))
        }
        assertTrue(fisico in 620..780, "Amostra inesperada: $fisico/1000")
    }

    @Test
    fun `defesa obrigatoria e centralizada pela habilidade de combate`() {
        assertEquals("Armas Brancas", EncounterGenerationRules.habilidadeDefensivaPara("Armas Brancas"))
        assertEquals("Briga", EncounterGenerationRules.habilidadeDefensivaPara("Briga"))
        assertEquals("Esquiva", EncounterGenerationRules.habilidadeDefensivaPara("Arqueirismo"))
        assertEquals("Esquiva", EncounterGenerationRules.habilidadeDefensivaPara("Arremesso"))
    }

    @Test
    fun `prioridade de encantos usa a defesa nativa do combate`() {
        val armasBrancas = EncounterArchetypePolicy.abilityPriority(
            archetype = ArquetipoEncontro.FISICO,
            combat = "Armas Brancas",
            support = "Atletismo",
            favored = emptyList()
        )
        assertEquals("Armas Brancas", armasBrancas.first())
        assertTrue(armasBrancas.indexOf("Armas Brancas") < (armasBrancas.indexOf("Esquiva").takeIf { it >= 0 } ?: Int.MAX_VALUE))

        val arqueirismo = EncounterArchetypePolicy.abilityPriority(
            archetype = ArquetipoEncontro.FISICO,
            combat = "Arqueirismo",
            support = "Atletismo",
            favored = emptyList()
        )
        assertEquals("Arqueirismo", arqueirismo.first())
        assertEquals("Esquiva", arqueirismo.getOrNull(1))
    }

    @Test
    fun `prioridade de encantos fisico nao promove esquiva como prioridade generica para armas brancas`() {
        val prioridade = EncounterArchetypePolicy.charmPriority(
            archetype = ArquetipoEncontro.FISICO,
            combat = "Armas Brancas",
            support = "Atletismo",
            favored = emptyList()
        )
        assertEquals("Armas Brancas", prioridade.first())
        val indiceEsquiva = prioridade.indexOf("Esquiva")
        assertTrue(indiceEsquiva == -1 || indiceEsquiva > 0)
    }

    @Test
    fun `ocultismo pode ser elevado explicitamente ao topo da prioridade de encantos`() {
        val prioridade = EncounterArchetypePolicy.charmPriority(
            archetype = ArquetipoEncontro.MENTAL,
            combat = "Briga",
            support = "Conhecimento",
            favored = listOf("Medicina"),
            supernal = "Medicina",
            priorizarOcultismo = true
        )
        assertEquals("Ocultismo", prioridade.first())
        assertEquals(1, prioridade.count { it == "Ocultismo" })
    }
    @Test
    fun `supernal nao ultrapassa direcao central do arquetipo na prioridade de habilidade`() {
        val prioridade = EncounterArchetypePolicy.abilityPriority(
            archetype = com.example.model.ArquetipoEncontro.MENTAL,
            combat = "Briga",
            support = "Conhecimento",
            favored = listOf("Medicina"),
            supernal = "Medicina"
        )
        assertEquals("Conhecimento", prioridade.first())
        assertTrue(prioridade.indexOf("Medicina") > prioridade.indexOf("Conhecimento"))
    }

}
