package com.example.data

import com.example.model.ExaltedConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterRulePolicyContractTest {
    @Test
    fun `intencao explicita precede arquetipo sem duplicar candidato`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("Ocultismo"),
            archetype = listOf("Conhecimento", "Ocultismo"),
            exaltStructure = listOf("Medicina")
        )
        assertEquals("Ocultismo", prioridade.first())
        assertEquals(1, prioridade.count { it == "Ocultismo" })
    }

    @Test
    fun `arquetipo precede preferencias de tipo quando nao ha foco explicito`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            archetype = listOf("Conhecimento"),
            exaltStructure = listOf("Medicina")
        )
        assertEquals(listOf("Conhecimento", "Medicina"), prioridade)
    }

    @Test
    fun `restricao obrigatoria filtra antes do ranking`() {
        val legais = EncounterRulePolicy.filterRequired(
            candidates = listOf("legal", "ilegal")
        ) { it != "ilegal" }
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("ilegal", "legal")
        ).filter { it in legais }

        assertFalse("ilegal" in prioridade)
        assertTrue("legal" in prioridade)
        assertEquals("legal", prioridade.first())
    }

    @Test
    fun `ranking elimina vazios e duplicatas preservando primeira precedencia`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("", "Ocultismo"),
            archetype = listOf("Ocultismo", "Conhecimento"),
            fallback = listOf("Conhecimento", "Investigação")
        )
        assertEquals(listOf("Ocultismo", "Conhecimento", "Investigação"), prioridade)
    }
    @Test
    fun `foco explicito lidera prioridade antes de aspecto ou favorecida`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("Ocultismo"),
            archetype = EncounterArchetypePolicy.abilityPriority(
                archetype = com.example.model.ArquetipoEncontro.MENTAL,
                combat = "Briga",
                support = "Conhecimento",
                favored = listOf("Medicina", "Burocracia")
            )
        )
        assertEquals("Ocultismo", prioridade.first())
        assertTrue(prioridade.indexOf("Conhecimento") > 0)
        assertTrue(prioridade.indexOf("Medicina") > 0)
    }

    @Test
    fun `foco lunar lidera prioridade de atributos sem duplicar preferencia de casta`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("Inteligência"),
            archetype = LunarArchetypePolicy.attributePriority(
                com.example.model.ArquetipoEncontro.MENTAL,
                listOf("Percepção", "Inteligência", "Raciocínio")
            )
        )
        assertEquals("Inteligência", prioridade.first())
        assertEquals(1, prioridade.count { it == "Inteligência" })
        assertTrue(prioridade.contains("Percepção"))
        assertTrue(prioridade.contains("Raciocínio"))
    }

    @Test
    fun `habilidade de aspecto e filtrada antes do ranking de favorecidas`() {
        val foraDoAspecto = listOf("Briga", "Ocultismo", "Medicina", "Esquiva")
        val legais = EncounterRulePolicy.filterRequired(foraDoAspecto) { it != "Briga" }
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("Briga", "Ocultismo").filter { it in legais },
            archetype = listOf("Briga", "Medicina", "Esquiva").filter { it in legais }
        )
        assertTrue("Briga" !in prioridade)
        assertEquals("Ocultismo", prioridade.first())
    }

    @Test
    fun `ordem lunar preserva foco e usa fallback sem duplicar arvores`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("Destreza"),
            fallback = listOf("Destreza", "Força", "Vigor")
        )
        assertEquals(listOf("Destreza", "Força", "Vigor"), prioridade)
    }


    @Test
    fun `estrutura do exaltado e fallback preservam primeira ocorrencia`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            exaltStructure = listOf("Briga", "Esquiva"),
            fallback = listOf("Esquiva", "Atletismo", "Briga")
        )
        assertEquals(listOf("Briga", "Esquiva", "Atletismo"), prioridade)
    }

    @Test
    fun `sinergia precede fallback sem criar duplicatas`() {
        val prioridade = EncounterRulePolicy.resolvePriority(
            synergy = listOf("Presença", "Performance"),
            fallback = listOf("Performance", "Socialização")
        )
        assertEquals(listOf("Presença", "Performance", "Socialização"), prioridade)
    }


    @Test
    fun `prioridade de progressao preserva arquetipo antes de fallback generico`() {
        val arquetipo = EncounterArchetypePolicy.abilityPriority(
            archetype = com.example.model.ArquetipoEncontro.SOCIAL,
            combat = "Briga",
            support = "Presença",
            favored = listOf("Socialização")
        )
        val prioridade = EncounterRulePolicy.resolvePriority(
            archetype = arquetipo,
            fallback = ExaltedConstants.ALL_25_ABILITIES
        )
        assertEquals(arquetipo.first(), prioridade.first())
        assertEquals(prioridade.size, prioridade.distinct().size)
    }

    @Test
    fun `atributos lunares especiais sao normalizados antes do ranking`() {
        val especiais = EncounterRulePolicy.resolvePriority(
            exaltStructure = listOf("Inteligência", "Percepção", "Inteligência", "Raciocínio")
        )
        assertEquals(listOf("Inteligência", "Percepção", "Raciocínio"), especiais)
    }


}
