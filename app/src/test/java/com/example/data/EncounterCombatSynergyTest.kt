package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCombatSynergyTest {
    @Test fun `retirar objeto nao vira dreno de iniciativa`() {
        val tags = EncounterCombatSynergy.tags("O Solar pode retirar os arreios e a armadura de seu cavalo.")
        assertFalse(EncounterCombatSynergy.Tag.INITIATIVE_DRAIN in tags)
    }

    @Test fun `perda explicita de iniciativa vira dreno`() {
        val tags = EncounterCombatSynergy.tags("O inimigo perde 3 pontos de Iniciativa.")
        assertTrue(EncounterCombatSynergy.Tag.INITIATIVE_DRAIN in tags)
    }

    @Test fun `explorar alvo atordoado e distinto de causar atordoamento`() {
        val tags = EncounterCombatSynergy.tags("O Solar realiza o ataque contra um alvo atordoado.")
        assertTrue(EncounterCombatSynergy.Tag.CRASH_EXPLOIT in tags)
        assertFalse(EncounterCombatSynergy.Tag.CRASH_CAUSE in tags)
    }

    @Test fun `produtor de crash combina fortemente com explorador`() {
        val producer = "Este ataque causa Atordoamento de Iniciativa no oponente."
        val consumer = "Contra um alvo atordoado, o ataque recebe dados adicionais."
        assertTrue(EncounterCombatSynergy.score(consumer, listOf(producer)) >= 24)
    }

    @Test fun `sinergia de par independe da ordem de aquisicao`() {
        val a = "Este ataque fulminante recebe dados de dano adicionais."
        val b = "O Solar pode fazer um ataque adicional."
        val ab = EncounterCombatSynergy.pairAffinity(a, b)
        val ba = EncounterCombatSynergy.pairAffinity(b, a)
        assertTrue(ab == ba)
    }

    @Test fun `custo de iniciativa sem motor recebe penalidade`() {
        val score = EncounterCombatSynergy.score("Custo: 3i. O personagem brilha intensamente.", emptyList())
        assertTrue(score < 0)
    }
    @Test fun `score pre classificado preserva exatamente o score textual`() {
        val candidate = "Custo: 1i. Ataque fulminante recebe dados de dano e pode causar Atordoamento."
        val selected = listOf("Contra um alvo atordoado, recebe dados adicionais.", "O Solar pode fazer um ataque adicional.")
        assertEquals(
            EncounterCombatSynergy.score(candidate, selected),
            EncounterCombatSynergy.scoreTags(EncounterCombatSynergy.tags(candidate), selected.map(EncounterCombatSynergy::tags))
        )
    }
    @Test fun `briga e artes marciais coexistem mas afinidade cruzada e zero`() {
        val a = "Ataque fulminante recebe dados de dano."
        val b = "Ataque fulminante recebe precisão e dano."
        assertEquals(
            0,
            EncounterCombatSynergy.pairAffinity(
                a, EncounterCombatSynergy.CombatCharmDomain.BRIGA,
                b, EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS
            )
        )
        assertTrue(
            EncounterCombatSynergy.pairAffinity(
                a, EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS,
                b, EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS
            ) >= 0
        )
    }

    @Test fun `habilidades fisicas de combate diferentes nao geram afinidade cruzada`() {
        val textoA = "Ataque fulminante recebe dados de dano adicionais."
        val textoB = "O ataque fulminante recebe precisão e dano."
        val domains = listOf(
            EncounterCombatSynergy.CombatCharmDomain.BRIGA,
            EncounterCombatSynergy.CombatCharmDomain.ARMAS_BRANCAS,
            EncounterCombatSynergy.CombatCharmDomain.ARQUEIRISMO,
            EncounterCombatSynergy.CombatCharmDomain.ARREMESSO
        )
        for (a in domains) for (b in domains) {
            if (a != b) assertEquals(0, EncounterCombatSynergy.pairAffinity(textoA, a, textoB, b))
        }
    }

    @Test fun `artes marciais nao combinam com nenhuma habilidade fisica comum`() {
        val texto = "Ataque fulminante recebe dados de dano e precisão."
        val comuns = listOf(
            EncounterCombatSynergy.CombatCharmDomain.BRIGA,
            EncounterCombatSynergy.CombatCharmDomain.ARMAS_BRANCAS,
            EncounterCombatSynergy.CombatCharmDomain.ARQUEIRISMO,
            EncounterCombatSynergy.CombatCharmDomain.ARREMESSO
        )
        for (dominio in comuns) {
            assertEquals(0, EncounterCombatSynergy.pairAffinity(
                texto, EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS,
                texto, dominio
            ))
        }
        assertTrue(EncounterCombatSynergy.domainsCanSynergize(
            EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS,
            EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS
        ))
    }

    @Test fun `other nao herda bloqueio de combate fisico para preservar social e mental`() {
        assertTrue(EncounterCombatSynergy.domainsCanSynergize(
            EncounterCombatSynergy.CombatCharmDomain.OTHER,
            EncounterCombatSynergy.CombatCharmDomain.OTHER
        ))
    }

    @Test fun `classificador reconhece habilidades fisicas e estilos marciais`() {
        assertEquals(EncounterCombatSynergy.CombatCharmDomain.ARMAS_BRANCAS,
            EncounterCombatSynergy.domainForCategory("Armas Brancas"))
        assertEquals(EncounterCombatSynergy.CombatCharmDomain.ARQUEIRISMO,
            EncounterCombatSynergy.domainForCategory("Arqueirismo"))
        assertEquals(EncounterCombatSynergy.CombatCharmDomain.ARREMESSO,
            EncounterCombatSynergy.domainForCategory("Arremesso"))
        assertEquals(EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS,
            EncounterCombatSynergy.domainForCategory("Estilo Tigre", setOf("Estilo Tigre")))
        assertEquals(EncounterCombatSynergy.CombatCharmDomain.MARTIAL_ARTS,
            EncounterCombatSynergy.domainForCategory("Estilo Tigre"))
    }


    @Test fun `fronteira canonica por categoria preserva social e isola combate fisico`() {
        assertFalse(EncounterCombatSynergy.categoriesCanSynergize("Armas Brancas", "Arqueirismo"))
        assertFalse(EncounterCombatSynergy.categoriesCanSynergize("Estilo Tigre", "Briga"))
        assertTrue(EncounterCombatSynergy.categoriesCanSynergize("Estilo Tigre", "Estilo Garça"))
        assertTrue(EncounterCombatSynergy.categoriesCanSynergize("Presença", "Socialização"))
        assertTrue(EncounterCombatSynergy.categoriesCanSynergize("Investigação", "Linguística"))
    }

}
