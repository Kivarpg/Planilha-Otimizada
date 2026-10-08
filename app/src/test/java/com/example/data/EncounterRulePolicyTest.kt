package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.model.EncantoEncontro
import com.example.model.NpcEncontro

class EncounterRulePolicyTest {
    @Test
    fun `registro distingue legalidade estrutura e qualidade`() {
        val naturezas = EncounterRulePolicy.registry.map { it.nature }.toSet()
        assertTrue(EncounterRulePolicy.Nature.LEGALITY in naturezas)
        assertTrue(EncounterRulePolicy.Nature.STRUCTURAL in naturezas)
        assertTrue(EncounterRulePolicy.Nature.QUALITY in naturezas)
        assertEquals(EncounterRulePolicy.registry.size, EncounterRulePolicy.registry.map { it.id }.distinct().size)
    }

    @Test
    fun `intencao explicita lidera ranking sem duplicar candidatos`() {
        val ordem = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("Ocultismo"),
            exaltStructure = listOf("Presença", "Ocultismo"),
            archetype = listOf("Investigação", "Presença"),
            efficiency = listOf("Conhecimento")
        )
        assertEquals(listOf("Ocultismo", "Investigação", "Presença", "Conhecimento"), ordem)
    }

    @Test
    fun `legalidade permanece acima de intencao explicita`() {
        assertTrue(
            EncounterRulePolicy.Precedence.LEGALITY.rank <
                EncounterRulePolicy.Precedence.EXPLICIT_USER_INTENT.rank
        )
        assertTrue(
            EncounterRulePolicy.Precedence.EXALT_TYPE_STRUCTURE.rank <
                EncounterRulePolicy.Precedence.EXPLICIT_USER_INTENT.rank
        )
    }    @Test
    fun `foco explicito continua primeiro durante progressao por xp`() {
        val npc = NpcEncontro(
            focoProgressaoExplicito = "Ocultismo",
            habilidadePrincipal = "Briga",
            habilidadeDefensiva = "Esquiva",
            habilidadeSuporte = "Atletismo",
            abilities = mapOf("Briga" to 5, "Ocultismo" to 3, "Esquiva" to 4),
            charms = listOf(
                EncantoEncontro("A", "Briga", ""),
                EncantoEncontro("B", "Briga", ""),
                EncantoEncontro("C", "Briga", "")
            )
        )

        val ordem = EncounterRulePolicy.abilityPriorityFor(npc)

        assertEquals("Ocultismo", ordem.first())
        assertTrue(ordem.indexOf("Briga") > 0)
    }

    @Test
    fun `progressao preserva arvore ja desenvolvida como sinal de sinergia`() {
        val npc = NpcEncontro(
            habilidadePrincipal = "Briga",
            habilidadeSuporte = "Atletismo",
            abilities = mapOf("Briga" to 4, "Atletismo" to 3, "Presença" to 5),
            charms = listOf(
                EncantoEncontro("A", "Briga", ""),
                EncantoEncontro("B", "Briga", "")
            )
        )

        val ordem = EncounterRulePolicy.abilityPriorityFor(npc)

        assertTrue(ordem.contains("Briga"))
        assertTrue(ordem.contains("Presença"))
        assertTrue(ordem.indexOf("Briga") < ordem.indexOf("Presença"))
    }


    @Test
    fun `restricoes obrigatorias filtram antes do ranking de preferencias`() {
        val permitidos = EncounterRulePolicy.filterRequired(listOf("ilegal", "foco", "fallback")) {
            it != "ilegal"
        }
        val prioridade = EncounterRulePolicy.resolvePriority(
            explicitIntent = listOf("foco"),
            exaltStructure = listOf("estrutura"),
            fallback = permitidos
        )
        assertTrue("ilegal" !in prioridade)
        assertEquals("foco", prioridade.first())
    }

    @Test
    fun `foco explicito lidera atributos lunares`() {
        val npc = NpcEncontro(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            arquetipo = com.example.model.ArquetipoEncontro.MENTAL,
            focoProgressaoExplicito = "Inteligência",
            lunarAtributosCasta = listOf("Força", "Vigor"),
            habilidadesFavorecidas = listOf("Destreza", "Aparência")
        )
        assertEquals("Inteligência", EncounterRulePolicy.lunarAttributePriorityFor(npc).first())
    }

}
