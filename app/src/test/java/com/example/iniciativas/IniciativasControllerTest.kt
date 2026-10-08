package com.example.iniciativas

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IniciativasControllerTest {
    @Test fun ordenaPorIniciativaDescendente() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 5)
        c.adicionarParticipante("B", 10)
        assertEquals(listOf("B", "A"), c.participantesOrdenados().map { it.nome })
    }

    @Test fun declaracaoETravamentoFuncionam() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 8)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        assertTrue(c.state.value.ataqueTravado)
    }

    @Test fun clashExigeSelecaoExplicitaDoVencedor() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        c.declararAtaque(b.id, a.id)
        assertTrue(c.travarAtaque())
        assertNull(c.state.value.vencedorClashId)
        c.transferirIniciativa(1)
        assertEquals(10, c.state.value.participantes.first { it.id == a.id }.iniciativa)
        assertEquals(10, c.state.value.participantes.first { it.id == b.id }.iniciativa)
        assertEquals(null, c.selecionarVencedorClash("inexistente"))
    }

    @Test fun colisaoTransformaAtaqueSimplesEmClashEMarcaOsDoisComoAgiram() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }

        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())

        assertTrue(c.iniciarColisao())
        assertTrue(c.state.value.emClash(a.id))
        assertTrue(c.state.value.emClash(b.id))
        assertTrue(c.state.value.participantes.first { it.id == a.id }.jaAgiramNesteTurno)
        assertTrue(c.state.value.participantes.first { it.id == b.id }.jaAgiramNesteTurno)
        assertNull(c.state.value.vencedorClashId)
    }


    @Test fun colisaoFulminanteSempreSucessoEAjustaIniciativaDoPerdedor() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        assertTrue(c.iniciarColisao())
        c.selecionarVencedorClash(a.id)
        c.selecionarTipoAtaque(TipoAtaque.FULMINANTE)

        assertTrue(c.state.value.colisaoAtiva)
        assertEquals(true, c.state.value.ataqueBemSucedido)
        // Não existe etapa de resultado; a transferência já pode ser feita.
        c.transferirIniciativa(2)
        assertEquals(12, c.state.value.iniciativasPendentes[a.id])
        assertEquals(8, c.state.value.iniciativasPendentes[b.id])
    }

    @Test fun colisaoDecisivaSempreSucessoEColocaVencedorEm3() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        assertTrue(c.iniciarColisao())
        c.selecionarVencedorClash(b.id)
        c.selecionarTipoAtaque(TipoAtaque.DECISIVO)

        assertEquals(true, c.state.value.ataqueBemSucedido)
        assertEquals(3, c.state.value.iniciativasPendentes[b.id])
        assertEquals(null, c.state.value.iniciativasPendentes[a.id])
        c.terminarAcao()
        assertEquals(3, c.state.value.participantes.first { it.id == b.id }.iniciativa)
        assertEquals(10, c.state.value.participantes.first { it.id == a.id }.iniciativa)
        assertFalse(c.state.value.colisaoAtiva)
    }

    @Test fun colisaoNaoOcorreQuandoHaApenasUmElegivel() {
        val c = IniciativasController(
            IniciativasState(
                participantes = listOf(
                    ParticipanteIniciativa(id = "a", nome = "A", iniciativa = 10, ordemInsercao = 1),
                    ParticipanteIniciativa(id = "b", nome = "B", iniciativa = 9, ordemInsercao = 2)
                ),
                declaracoes = mapOf("a" to "b"),
                ataqueTravado = true
            )
        )
        assertFalse(c.podeIniciarColisao())
        assertFalse(c.iniciarColisao())
    }

    @Test fun colisaoNaoOcorreComIniciativasDiferentes() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 9)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }

        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        assertFalse(c.iniciarColisao())
        assertFalse(c.state.value.emClash(a.id))
        assertFalse(c.state.value.participantes.first { it.id == a.id }.jaAgiramNesteTurno)
        assertFalse(c.state.value.participantes.first { it.id == b.id }.jaAgiramNesteTurno)
    }

    @Test fun vencedorRecebeTransferenciaEPerdedorRecebePenalidadeDefesa() {
        val c = IniciativasController()
        c.adicionarOuAtualizarNpc("A", 10, "npc-a")
        c.adicionarOuAtualizarNpc("B", 10, "npc-b")
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        c.declararAtaque(b.id, a.id)
        c.travarAtaque()
        assertEquals("npc-b", c.selecionarVencedorClash(a.id))
        c.selecionarTipoAtaque(TipoAtaque.FULMINANTE)
        c.confirmarSucessoAtaque(true)
        c.transferirIniciativa(2)
        // PDF §7: valores temporários até Próximo — lista ainda não muda.
        assertEquals(10, c.state.value.participantes.first { it.id == a.id }.iniciativa)
        assertEquals(10, c.state.value.participantes.first { it.id == b.id }.iniciativa)
        assertEquals(12, c.state.value.iniciativasPendentes[a.id])
        assertEquals(8, c.state.value.iniciativasPendentes[b.id])
        assertEquals(2, c.state.value.participantes.first { it.id == b.id }.penalidadeClashDefesa)
        assertEquals(0, c.state.value.participantes.first { it.id == a.id }.penalidadeClashDefesa)
        c.terminarAcao()
        // Base 10 + transferência 2 = 12 (pendente) + bônus Clash (3+1) +1 Fulminante = 17
        assertEquals(17, c.state.value.participantes.first { it.id == a.id }.iniciativa)
        assertEquals(8, c.state.value.participantes.first { it.id == b.id }.iniciativa)
    }

    @Test fun penalidadeExpiraNoInicioDoProximoTurnoDoPerdedor() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        c.declararAtaque(b.id, a.id)
        c.travarAtaque()
        c.selecionarVencedorClash(a.id)
        c.selecionarTipoAtaque(TipoAtaque.FULMINANTE)
        c.confirmarSucessoAtaque(true)
        c.transferirIniciativa(1)
        c.terminarAcao()
        assertEquals(2, c.state.value.participantes.first { it.id == b.id }.penalidadeClashDefesa)

        // A tem o próximo slot; depois de agir, B passa a ser o próximo slot.
        c.declararAtaque(a.id, b.id)
        c.travarAtaque()
        c.terminarAcao()
        assertEquals(0, c.state.value.participantes.first { it.id == b.id }.penalidadeClashDefesa)
    }

    @Test fun penalidadeNaoExpiraEnquantoOutroCombatenteTemOProximoSlot() {
        val aId = "a"
        val bId = "b"
        val cId = "c"
        val initial = IniciativasState(
            participantes = listOf(
                ParticipanteIniciativa(id = aId, nome = "A", iniciativa = 10, ordemInsercao = 1),
                ParticipanteIniciativa(id = bId, nome = "B", iniciativa = 10, ordemInsercao = 2),
                ParticipanteIniciativa(id = cId, nome = "C", iniciativa = 20, ordemInsercao = 3, jaAgiramNesteTurno = true)
            )
        )
        val controller = IniciativasController(initial)
        val a = controller.state.value.participantes.first { it.id == aId }
        val b = controller.state.value.participantes.first { it.id == bId }
        controller.declararAtaque(a.id, b.id)
        controller.declararAtaque(b.id, a.id)
        controller.travarAtaque()
        controller.selecionarVencedorClash(a.id)
        controller.terminarAcao()
        assertEquals(2, controller.state.value.participantes.first { it.id == b.id }.penalidadeClashDefesa)
    }

    @Test fun nomeVazioNaoEAdicionado() {
        val c = IniciativasController()
        assertFalse(c.adicionarParticipante("   ", 3))
        assertTrue(c.state.value.participantes.isEmpty())
    }

    @Test fun jsonPreservaPenalidadeClash() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        c.declararAtaque(b.id, a.id)
        c.travarAtaque()
        c.selecionarVencedorClash(a.id)
        val restaurado = IniciativasJsonCodec.decode(IniciativasJsonCodec.encode(c.state.value))
        assertEquals(2, restaurado.participantes.first { it.id == b.id }.penalidadeClashDefesa)
    }
    @Test fun jsonPreservaVinculoDoNpcSincronizadoAposRestauracao() {
        val c = IniciativasController()
        c.adicionarOuAtualizarNpc(
            nome = "Regressao Persistencia Abas",
            iniciativa = 13,
            origemNpcId = "npc-persistencia-abas"
        )

        val restaurado = IniciativasJsonCodec.decode(IniciativasJsonCodec.encode(c.state.value))
        val participante = restaurado.participantes.single()

        assertEquals("npc-persistencia-abas", participante.origemNpcId)
        assertEquals("Regressao Persistencia Abas", participante.nome)
        assertEquals(13, participante.iniciativa)
    }

    @Test fun sincronizacaoNpcSemMudancaNaoCriaNovaLista() {
        val c = IniciativasController()
        c.adicionarOuAtualizarNpc("A", 10, "npc-a")
        val listaAntes = c.state.value.participantes
        val ordenadosAntes = c.participantesOrdenados()

        assertTrue(c.adicionarOuAtualizarNpc("A", 10, "npc-a"))

        assertTrue(listaAntes === c.state.value.participantes)
        assertTrue(ordenadosAntes === c.participantesOrdenados())
    }

    @Test fun atualizarNpcSemMudancaNaoCriaNovaLista() {
        val c = IniciativasController()
        c.adicionarOuAtualizarNpc("A", 10, "npc-a")
        val listaAntes = c.state.value.participantes
        val ordenadosAntes = c.participantesOrdenados()

        assertTrue(c.atualizarPorOrigemNpcId("npc-a", "A", 10))

        assertTrue(listaAntes === c.state.value.participantes)
        assertTrue(ordenadosAntes === c.participantesOrdenados())
    }

    @Test fun consultaOrdenadaReutilizaResultadoParaMesmaLista() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 5)

        val ordenados = c.participantesOrdenados()

        assertTrue(ordenados === c.participantesOrdenados())
    }

    @Test fun consultasElegiveisReutilizamResultadosParaMesmaLista() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 5)

        val elegiveis = c.elegiveisAtuais()
        val ids = c.idsElegiveis()

        assertTrue(elegiveis === c.elegiveisAtuais())
        assertTrue(ids === c.idsElegiveis())
        assertEquals(setOf(elegiveis.single().id), ids)
    }

    @Test fun editarNomeSemMudancaNaoCriaNovaLista() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        val antes = c.state.value.participantes
        val id = antes.single().id

        c.editarNome(id, "A")

        assertTrue(antes === c.state.value.participantes)
    }

    @Test fun ajustarIniciativaCalculaDeltaSobreEstadoAtualEmUmaUnicaMutacao() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        val id = c.state.value.participantes.single().id
        c.declararAtaque(id, "alvo")

        c.ajustarIniciativa(id, 2)

        assertEquals(12, c.state.value.participantes.single().iniciativa)
        assertTrue(c.state.value.declaracoes.isEmpty())
        assertNull(c.state.value.vencedorClashId)
    }

    @Test fun definirIniciativaSemMudancaNaoCriaNovaLista() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        val antes = c.state.value.participantes
        val id = antes.single().id

        c.definirIniciativa(id, 10)

        assertTrue(antes === c.state.value.participantes)
    }

    @Test fun removerParticipanteInexistenteNaoCriaNovaLista() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        val antes = c.state.value.participantes

        c.removerParticipante("nao-existe")

        assertTrue(antes === c.state.value.participantes)
    }

    @Test
    fun removerParticipanteDestravaSomenteQuandoDeclaracaoFoiRemovida() {
        val aId = "a"
        val bId = "b"
        val initial = IniciativasState(
            participantes = listOf(
                ParticipanteIniciativa(id = aId, nome = "A", iniciativa = 10, ordemInsercao = 1),
                ParticipanteIniciativa(id = bId, nome = "B", iniciativa = 9, ordemInsercao = 2)
            ),
            declaracoes = mapOf(aId to bId),
            ataqueTravado = true
        )
        val c = IniciativasController(initial)

        c.removerParticipante("nao-existe")
        assertTrue(c.state.value.ataqueTravado)

        c.removerParticipante(aId)
        assertFalse(c.state.value.ataqueTravado)
        assertTrue(c.state.value.declaracoes.isEmpty())
    }

    @Test
    fun encerrarCombateLimpaEstadoDeDeclaracaoPendente() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 9)
        val a = c.state.value.participantes.first { it.nome == "A" }
        c.onToqueCombatente(a.id)

        assertNotNull(c.encerrarCombate())

        c.adicionarParticipante("Novo", 10)
        val novo = c.state.value.participantes.single()
        c.onToqueCombatente(novo.id)
        assertTrue(c.state.value.declaracoes.isEmpty())
    }

    @Test
    fun paresClashReutilizamResultadoParaMesmoMapaDeDeclaracoes() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 5)
        c.adicionarParticipante("B", 5)
        val participantes = c.state.value.participantes
        val a = participantes[0].id
        val b = participantes[1].id
        c.declararAtaque(a, b)
        c.declararAtaque(b, a)
        val primeiro = c.paresClashAtuais()
        val segundo = c.paresClashAtuais()
        assertTrue(primeiro === segundo)
    }

    @Test
    fun declararAtaqueRepetidoNaoCriaNovoEstado() {
        val controller = IniciativasController()
        controller.adicionarParticipante("A", 10)
        controller.adicionarParticipante("B", 9)
        val a = controller.state.value.participantes[0].id
        val b = controller.state.value.participantes[1].id
        controller.declararAtaque(a, b)
        val antes = controller.state.value

        controller.declararAtaque(a, b)

        assertSame(antes, controller.state.value)
    }

    @Test
    fun tocarAlvoAguardandoDeclaraAtaqueDoElegivelSemSelecionarAtacante() {
        val controller = IniciativasController()
        controller.adicionarParticipante("A", 10)
        controller.adicionarParticipante("B", 9)
        val a = controller.state.value.participantes.first { it.nome == "A" }.id
        val b = controller.state.value.participantes.first { it.nome == "B" }.id

        // Sem tocar no atacante: um toque no alvo basta.
        controller.onToqueCombatente(b)

        assertEquals(b, controller.state.value.declaracoes[a])
    }

    @Test
    fun tocarMesmoAlvoDeNovoRemoveDeclaracao() {
        val controller = IniciativasController()
        controller.adicionarParticipante("A", 10)
        controller.adicionarParticipante("B", 9)
        val a = controller.state.value.participantes.first { it.nome == "A" }.id
        val b = controller.state.value.participantes.first { it.nome == "B" }.id
        controller.onToqueCombatente(b)
        controller.onToqueCombatente(b)
        assertTrue(controller.state.value.declaracoes.isEmpty())
        assertNull(controller.state.value.declaracoes[a])
    }

    @Test fun consumirMensagemRemoveApenasAPrimeiraMensagem() {
        val estado = IniciativasState(mensagensPendentes = listOf("A", "B", "C"))
        val c = IniciativasController(estado)

        c.consumirMensagem()

        assertEquals(listOf("B", "C"), c.state.value.mensagensPendentes)
    }

    @Test fun consumirUltimaMensagemDeixaListaVazia() {
        val c = IniciativasController(IniciativasState(mensagensPendentes = listOf("A")))

        c.consumirMensagem()

        assertTrue(c.state.value.mensagensPendentes.isEmpty())
    }

    @Test
    fun empateIniciativaExigeEscolherAtacanteAntesDoAlvo() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        c.adicionarParticipante("C", 5)
        val a = c.state.value.participantes.first { it.nome == "A" }.id
        val b = c.state.value.participantes.first { it.nome == "B" }.id
        val alvo = c.state.value.participantes.first { it.nome == "C" }.id

        // Sem escolher atacante: toque no alvo não declara (empate).
        c.onToqueCombatente(alvo)
        assertTrue(c.state.value.declaracoes.isEmpty())

        // Qualquer um dos empatados pode ser o atacante — ordem da tabela irrelevante.
        c.onToqueCombatente(b)
        assertEquals(b, c.atacanteAtivoAtual())
        c.onToqueCombatente(alvo)
        assertEquals(alvo, c.state.value.declaracoes[b])
        assertNull(c.state.value.declaracoes[a])
    }

    @Test
    fun empateIniciativaPermiteAtacarOutroEmpatadoDireto() {
        // Aba 12 — "Iniciativa igual entre combatentes": dois empatados devem
        // poder se atacar diretamente, sem precisar declarar alvo pra outra
        // pessoa antes. Antes da correção, o segundo toque (em B, também
        // elegível) trocava o atacante ativo em vez de declarar o alvo.
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 10)
        val a = c.state.value.participantes.first { it.nome == "A" }.id
        val b = c.state.value.participantes.first { it.nome == "B" }.id

        c.onToqueCombatente(a)
        assertEquals(a, c.atacanteAtivoAtual())

        c.onToqueCombatente(b)

        assertEquals(b, c.state.value.declaracoes[a])
        assertEquals(a, c.atacanteAtivoAtual())
    }

    @Test fun pularPropagaTodosNpcsDoProximoSlotEmpatado() {
        val controller = IniciativasController(
            IniciativasState(
                participantes = listOf(
                    ParticipanteIniciativa(id = "a", nome = "A", iniciativa = 12, ordemInsercao = 1, origemNpcId = "npc-a"),
                    ParticipanteIniciativa(id = "b", nome = "B", iniciativa = 8, ordemInsercao = 2, origemNpcId = "npc-b"),
                    ParticipanteIniciativa(id = "c", nome = "C", iniciativa = 8, ordemInsercao = 3, origemNpcId = "npc-c"),
                )
            )
        )

        val expiradas = controller.pularTurnoAtualComPenalidadesExpiradas()

        assertEquals(setOf("npc-b", "npc-c"), expiradas)
        assertTrue(controller.state.value.participantes.first { it.id == "a" }.jaAgiramNesteTurno)
    }

    @Test fun pularBloqueadoAposTravar() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 8)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.podePularTurno())
        assertTrue(c.travarAtaque())
        assertFalse(c.podePularTurno())
        assertFalse(c.pularTurnoAtual())
    }

    @Test fun terminoComResolucaoPendenteRegistraIncompleto() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        c.adicionarParticipante("B", 8)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        c.selecionarTipoAtaque(TipoAtaque.FULMINANTE)
        c.confirmarSucessoAtaque(true)
        val reg = c.encerrarCombate()
        assertNotNull(reg)
        assertEquals(TurnStatus.INCOMPLETO, reg!!.turnStatus)
        assertEquals(CombatStatus.ENCERRADO, reg.combatStatus)
        assertEquals("encerrado_manualmente", reg.eventosLog.lastOrNull()?.motivoEncerramento)
        assertTrue(c.state.value.participantes.isEmpty())
    }

    @Test fun terminoSemPendenciaRegistraEncerrado() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 10)
        val reg = c.encerrarCombate()
        assertNotNull(reg)
        assertEquals(TurnStatus.ENCERRADO, reg!!.turnStatus)
        assertEquals(CombatStatus.ENCERRADO, reg.combatStatus)
    }

    @Test fun decisivoFalhaReduzConformeRegraPdf() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 12)
        c.adicionarParticipante("B", 5)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        c.selecionarTipoAtaque(TipoAtaque.DECISIVO)
        c.confirmarSucessoAtaque(false)
        // Pendente: 12 >= 11 → -3 = 9
        assertEquals(9, c.state.value.iniciativasPendentes[a.id])
        assertEquals(12, c.state.value.participantes.first { it.id == a.id }.iniciativa)
        c.terminarAcao()
        assertEquals(9, c.state.value.participantes.first { it.id == a.id }.iniciativa)
    }
    @Test fun logPreservaRodadaAtacanteAlvoEDecisivoAposTermino() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 12)
        c.adicionarParticipante("B", 8)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        c.selecionarTipoAtaque(TipoAtaque.DECISIVO)
        c.confirmarSucessoAtaque(false)
        c.terminarAcao()

        val reg = c.encerrarCombate()!!
        val evento = reg.eventosLog.first()
        assertEquals(1, evento.rodada)
        assertEquals("A", evento.combatenteAtivoNome)
        assertEquals("B", evento.alvoNome)
        assertEquals(TipoAtaque.DECISIVO, evento.tipoAtaque)
        assertEquals(3, evento.iniciativaPerdidaPorId[a.id])
        assertTrue(c.state.value.participantes.isEmpty())
        assertTrue(reg.eventosLog.isNotEmpty())
    }

    @Test fun codecHistoricoPreservaRodadaEPerdasIndividuais() {
        val evento = LogTurnoSnapshot(
            rodada = 3,
            combatenteAtivoId = "a",
            combatenteAtivoNome = "A",
            alvoId = "b",
            alvoNome = "B",
            tipoAtaque = TipoAtaque.DECISIVO,
            iniciativaPerdidaPorId = mapOf("b" to 7),
            turnStatus = TurnStatus.RESOLVIDO
        )
        val original = HistoricoCombateEntry(
            dataHora = 1L,
            participantes = listOf(
                ParticipanteHistorico(id = "a", nome = "A", iniciativaFinal = 3),
                ParticipanteHistorico(id = "b", nome = "B", iniciativaFinal = 1)
            ),
            rodadasTotais = 3,
            eventosCrash = emptyList(),
            eventosLog = listOf(evento)
        )
        val restaurado = HistoricoCombateJsonCodec.decode(HistoricoCombateJsonCodec.encode(listOf(original))).single()
        assertEquals(3, restaurado.eventosLog.single().rodada)
        assertEquals(7, restaurado.eventosLog.single().iniciativaPerdidaPorId["b"])
        assertEquals("B", restaurado.participantes.first { it.id == "b" }.nome)
        assertEquals(TipoAtaque.DECISIVO, restaurado.eventosLog.single().tipoAtaque)
    }

    @Test fun logFulminanteRegistraPerdaDoAlvoAPartirDoSnapshotCanonico() {
        val c = IniciativasController()
        c.adicionarParticipante("A", 12)
        c.adicionarParticipante("B", 8)
        val a = c.participantesOrdenados().first { it.nome == "A" }
        val b = c.participantesOrdenados().first { it.nome == "B" }
        c.declararAtaque(a.id, b.id)
        assertTrue(c.travarAtaque())
        c.selecionarTipoAtaque(TipoAtaque.FULMINANTE)
        c.confirmarSucessoAtaque(true)
        c.transferirIniciativa(4)
        c.terminarAcao()

        val evento = c.state.value.eventosLog.last()
        assertEquals(1, evento.rodada)
        assertEquals("A", evento.combatenteAtivoNome)
        assertEquals("B", evento.alvoNome)
        assertEquals(4, evento.iniciativaPerdidaPorId[b.id])
        assertFalse(evento.iniciativaPerdidaPorId.containsKey(a.id))
    }

}
