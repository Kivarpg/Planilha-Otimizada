package com.example.iniciativas

import org.junit.Assert.assertEquals
import org.junit.Test

class IniciativasRulesTest {
    @Test
    fun paresClashSaoEncontradosSemDuplicatas() {
        val declaracoes = linkedMapOf("a" to "b", "b" to "a", "c" to "d", "d" to "c")
        assertEquals(listOf("a" to "b", "c" to "d"), IniciativasRules.paresClash(declaracoes))
    }

    @Test
    fun elegiveisConsideramApenasQuemAindaNaoAgiram() {
        val participantes = listOf(
            ParticipanteIniciativa(id = "a", nome = "A", iniciativa = 10, ordemInsercao = 1),
            ParticipanteIniciativa(id = "b", nome = "B", iniciativa = 12, ordemInsercao = 2, jaAgiramNesteTurno = true),
            ParticipanteIniciativa(id = "c", nome = "C", iniciativa = 10, ordemInsercao = 3)
        )
        assertEquals(listOf("a", "c"), IniciativasRules.elegiveis(participantes).map { it.id })
    }

    @Test
    fun ordenarUsaSomenteIniciativaMaiorNoTopo() {
        val participantes = listOf(
            ParticipanteIniciativa(id = "a", nome = "A", iniciativa = 5, ordemInsercao = 1),
            ParticipanteIniciativa(id = "b", nome = "B", iniciativa = 12, ordemInsercao = 2, jaAgiramNesteTurno = true),
            ParticipanteIniciativa(id = "c", nome = "C", iniciativa = 8, ordemInsercao = 3),
            ParticipanteIniciativa(id = "d", nome = "D", iniciativa = 12, ordemInsercao = 4)
        )
        // Exclusivamente por iniciativa (maior no topo). Quem já agiu não
        // é empurrado para o fim — só o número importa. Empate: ordemInsercao.
        assertEquals(listOf("b", "d", "c", "a"), IniciativasRules.ordenar(participantes).map { it.id })
    }
}
