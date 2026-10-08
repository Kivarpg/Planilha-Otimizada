package com.example.ui.tabs

import com.example.model.ArmaEncontro
import com.example.model.ArmaduraEncontro
import com.example.model.NpcEncontro
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterEquipmentDetailsTest {
    @Test
    fun usesNpcEquipmentAndAba7WeaponStatsInsteadOfCharacterSheet() {
        val npc = NpcEncontro(
            habilidadePrincipal = "Armas Brancas",
            arma = ArmaEncontro(
                nome = "Daiklave",
                peso = "Média",
                tipo = "Artefato",
                precisao = 999,
                dano = 999,
                defesa = 999,
                motesComitados = 5,
                etiquetas = listOf("Letal")
            )
        )

        val details = buildEquipamentoDetalhesAba7(npc)

        assertTrue(details.contains("Nome: Daiklave"))
        assertTrue(details.contains("Precisão: 3"))
        assertTrue(details.contains("Dano: 12"))
        assertTrue(details.contains("Dano Mínimo: 4"))
        assertTrue(details.contains("Defesa: 1"))
        assertTrue(details.contains("Cometimento: 5 motes"))
        assertTrue(!details.contains("999"))
    }

    @Test
    fun usesNpcEquipmentAndAba7ArmorStats() {
        val npc = NpcEncontro(
            armadura = ArmaduraEncontro(
                nome = "Armadura de Seda",
                peso = "Leve",
                tipo = "Artefato",
                absorcao = 999,
                dureza = 999,
                penalidadeMobilidade = 999,
                motesComitados = 4,
                marcadores = listOf("Silenciosa"),
                custoMeritoArtefato = 4
            )
        )

        val details = buildEquipamentoDetalhesAba7(npc)

        assertTrue(details.contains("Nome: Armadura de Seda"))
        assertTrue(details.contains("Absorção: 5"))
        assertTrue(details.contains("Dureza: 4"))
        assertTrue(details.contains("Penalidade de Mobilidade: 0"))
        assertTrue(details.contains("Cometimento: 4 motes"))
        assertTrue(details.contains("Custo de Mérito: 4"))
        assertTrue(!details.contains("999"))
    }
    @Test
    fun resolvesTheRolledArtifactFromAba7CatalogByNpcEquipment() {
        val npc = NpcEncontro(
            habilidadePrincipal = "Armas Brancas",
            arma = ArmaEncontro(
                nome = "Daiklave (Média)",
                peso = "Média",
                tipo = "Artefato",
                precisao = 999,
                dano = 999,
                defesa = 999
            )
        )

        val details = buildEquipamentoDetalhesAba7(npc)

        assertTrue(details.contains("Fonte Aba 7: catálogo de armas Artefato — Daiklave"))
        assertTrue(details.contains("Características: Letal, Armas brancas, Equilibrada"))
        assertTrue(!details.contains("999"))
    }

}
