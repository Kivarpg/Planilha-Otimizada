package com.example.iniciativas

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IniciativasJsonCodecSecurityTest {
    @Test fun limitaQuantidadeDeParticipantesImportados() {
        val participantes = (0 until 300).joinToString(",") { i ->
            "{\"id\":\"id-$i\",\"nome\":\"NPC $i\",\"iniciativa\":$i}"
        }
        val json = "{\"participantes\":[$participantes]}"
        val state = IniciativasJsonCodec.decode(json)
        assertEquals(256, state.participantes.size)
    }

    @Test fun removeDeclaracoesQueApontamParaParticipantesInexistentes() {
        val json = "{\"participantes\":[{\"id\":\"a\",\"nome\":\"A\"}],\"declaracoes\":{\"a\":\"inexistente\"}}"
        val state = IniciativasJsonCodec.decode(json)
        assertTrue(state.declaracoes.isEmpty())
    }

    @Test fun normalizaEstadoIniciativaCorrompido() {
        val json = "{\"participantes\":[{\"id\":\"a\",\"nome\":\"A\",\"penalidadeClashDefesa\":999}]}"
        val state = IniciativasJsonCodec.decode(json)
        assertEquals(2, state.participantes.single().penalidadeClashDefesa)
    }
}
