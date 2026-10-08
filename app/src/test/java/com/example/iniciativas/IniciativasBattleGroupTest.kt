package com.example.iniciativas

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IniciativasBattleGroupTest {
    @Test
    fun `battle group participant is updated by its own origin id`() {
        val controller = IniciativasController()
        assertTrue(controller.adicionarOuAtualizarBattleGroup("Grupo", 8, "bg-1"))
        assertTrue(controller.adicionarOuAtualizarBattleGroup("Grupo Atualizado", 11, "bg-1"))
        assertEquals(1, controller.state.value.participantes.size)
        val participant = controller.state.value.participantes.single()
        assertEquals("bg-1", participant.origemBattleGroupId)
        assertEquals("Grupo Atualizado", participant.nome)
        assertEquals(11, participant.iniciativa)
        assertTrue(participant.origemNpcId == null)
    }

    @Test
    fun `removing battle group origin removes only its participant`() {
        val controller = IniciativasController()
        controller.adicionarOuAtualizarNpc("NPC", 10, "npc-1")
        controller.adicionarOuAtualizarBattleGroup("Grupo", 8, "bg-1")
        controller.removerPorOrigemBattleGroupId("bg-1")
        assertEquals(1, controller.state.value.participantes.size)
        assertEquals("npc-1", controller.state.value.participantes.single().origemNpcId)
        assertFalse(controller.state.value.participantes.any { it.origemBattleGroupId == "bg-1" })
    }

    @Test
    fun `old initiative json without battle group origin remains compatible`() {
        val json = """{"participantes":[{"id":"p1","nome":"NPC","iniciativa":7,"ordemInsercao":1}],"declaracoes":{},"ataqueTravado":false,"vencedorClashId":null,"contadorTransferencia":0}"""
        val state = IniciativasJsonCodec.decode(json)
        assertEquals(1, state.participantes.size)
        assertEquals("NPC", state.participantes.single().nome)
        assertEquals(null, state.participantes.single().origemBattleGroupId)
    }
}
