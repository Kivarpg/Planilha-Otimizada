package com.example.iniciativas

import org.junit.Assert.assertEquals
import org.junit.Test

class IniciativasTurnResolverTest {
    @Test
    fun vencedorRecebeUmPontoGratuitoAoContinuarAposTransferencia() {
        val vencedor = ParticipanteIniciativa(
            id = "v", nome = "Vencedor", iniciativa = 6, ordemInsercao = 1
        )
        val perdedor = ParticipanteIniciativa(
            id = "p", nome = "Perdedor", iniciativa = 4, ordemInsercao = 2
        )
        val state = IniciativasState(
            participantes = listOf(vencedor, perdedor),
            declaracoes = mapOf("v" to "p", "p" to "v"),
            ataqueTravado = true,
            vencedorClashId = "v",
            contadorTransferencia = 2,
            iniciativasAntesTransferencia = mapOf("v" to 8, "p" to 2)
        )

        val result = IniciativasTurnResolver.resolver(state).state
        val finalVencedor = result.participantes.first { it.id == "v" }.iniciativa
        val finalPerdedor = result.participantes.first { it.id == "p" }.iniciativa

        assertEquals(10, finalVencedor)
        assertEquals(4, finalPerdedor)
    }

    @Test
    fun crashEmOutroParClashNaoUsaSempreOPrimeiroPar() {
        val a = ParticipanteIniciativa(id = "a", nome = "A", iniciativa = 5, ordemInsercao = 1)
        val b = ParticipanteIniciativa(id = "b", nome = "B", iniciativa = -1, ordemInsercao = 2)
        val c = ParticipanteIniciativa(id = "c", nome = "C", iniciativa = 5, ordemInsercao = 3)
        val d = ParticipanteIniciativa(id = "d", nome = "D", iniciativa = -1, ordemInsercao = 4)
        val state = IniciativasState(
            participantes = listOf(a, b, c, d),
            declaracoes = mapOf("a" to "b", "b" to "a", "c" to "d", "d" to "c"),
            ataqueTravado = true,
            vencedorClashId = "c",
            iniciativasAntesTransferencia = mapOf("a" to 5, "b" to 1, "c" to 5, "d" to 1)
        )

        val result = IniciativasTurnResolver.resolver(state).state
        val cFinal = result.participantes.first { it.id == "c" }.iniciativa
        assertEquals(13, cFinal)
    }

    @Test
    fun declaracaoNaoSobrescreveReduzidoPorDeCombatenteQueNaoEntrouEmCrash() {
        val a = ParticipanteIniciativa(id = "a", nome = "A", iniciativa = 5, ordemInsercao = 1)
        val b = ParticipanteIniciativa(id = "b", nome = "B", iniciativa = 0, ordemInsercao = 2)
        val c = ParticipanteIniciativa(
            id = "c",
            nome = "C",
            iniciativa = 3,
            ordemInsercao = 3,
            reduzidoPorId = "historico"
        )
        val state = IniciativasState(
            participantes = listOf(a, b, c),
            declaracoes = mapOf("a" to "b"),
            ataqueTravado = true,
            iniciativasAntesTransferencia = mapOf("a" to 5, "b" to 1, "c" to 3)
        )

        val result = IniciativasTurnResolver.resolver(state).state
        assertEquals("historico", result.participantes.first { it.id == "c" }.reduzidoPorId)
    }

    @Test
    fun viradaDeRodadaResetaAcaoSemPassagemIntermediariaEExpiraPenalidadeNoMaiorSlot() {
        val primeiro = ParticipanteIniciativa(
            id = "a", nome = "A", iniciativa = 7, ordemInsercao = 1,
            origemNpcId = "npc-a",
            jaAgiramNesteTurno = true, penalidadeClashDefesa = 1
        )
        val segundo = ParticipanteIniciativa(
            id = "b", nome = "B", iniciativa = 4, ordemInsercao = 2,
            jaAgiramNesteTurno = true
        )
        val state = IniciativasState(
            participantes = listOf(primeiro, segundo),
            declaracoes = mapOf("a" to "b"),
            iniciativasAntesTransferencia = mapOf("a" to 7, "b" to 4)
        )

        val result = IniciativasTurnResolver.resolver(state)
        val a = result.state.participantes.first { it.id == "a" }
        val b = result.state.participantes.first { it.id == "b" }

        assertEquals(2, result.state.rodada)
        assertEquals(false, a.jaAgiramNesteTurno)
        assertEquals(false, b.jaAgiramNesteTurno)
        assertEquals(0, a.penalidadeClashDefesa)
        assertEquals(0, b.penalidadeClashDefesa)
        assertEquals(setOf("npc-a"), result.penalidadesExpiradasNpcIds)
    }

    @Test
    fun empateNoProximoSlotExpiraPenalidadeDeTodosOsNpcs() {
        val a = ParticipanteIniciativa(
            id = "a", nome = "A", iniciativa = 7, ordemInsercao = 1,
            origemNpcId = "npc-a", jaAgiramNesteTurno = true, penalidadeClashDefesa = 2
        )
        val b = ParticipanteIniciativa(
            id = "b", nome = "B", iniciativa = 7, ordemInsercao = 2,
            origemNpcId = "npc-b", jaAgiramNesteTurno = true, penalidadeClashDefesa = 2
        )
        val state = IniciativasState(
            participantes = listOf(a, b),
            declaracoes = mapOf("a" to "b"),
            ataqueTravado = true
        )

        val result = IniciativasTurnResolver.resolver(state)

        assertEquals(setOf("npc-a", "npc-b"), result.penalidadesExpiradasNpcIds)
        assertEquals(listOf(0, 0), result.state.participantes.map { it.penalidadeClashDefesa })
    }


    @Test
    fun decisivoTambemExpiraPenalidadeDeTodosNoProximoSlot() {
        val a = ParticipanteIniciativa(
            id = "a", nome = "A", iniciativa = 7, ordemInsercao = 1,
            origemNpcId = "npc-a", jaAgiramNesteTurno = true, penalidadeClashDefesa = 2
        )
        val b = ParticipanteIniciativa(
            id = "b", nome = "B", iniciativa = 7, ordemInsercao = 2,
            origemNpcId = "npc-b", jaAgiramNesteTurno = true, penalidadeClashDefesa = 2
        )
        val state = IniciativasState(
            participantes = listOf(a, b),
            declaracoes = mapOf("a" to "b"),
            ataqueTravado = true,
            tipoAtaquePendente = TipoAtaque.DECISIVO,
            ataqueBemSucedido = false
        )

        val result = IniciativasTurnResolver.resolver(state)

        assertEquals(setOf("npc-a", "npc-b"), result.penalidadesExpiradasNpcIds)
        assertEquals(listOf(0, 0), result.state.participantes.map { it.penalidadeClashDefesa })
    }


}
