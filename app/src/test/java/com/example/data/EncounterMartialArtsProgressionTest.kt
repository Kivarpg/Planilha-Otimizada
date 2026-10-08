package com.example.data

import com.example.model.EncantoEncontro
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterMartialArtsProgressionTest {
    private fun charm(nome: String, prereq: String) = EncantoArteMarcialDefinition(
        id=nome, estiloId="s", habilidade="Estilo Teste", nome=nome, nomeIngles="",
        custo="", minsTexto="", minHabilidade=1, minEssencia=1, tipo="Suplementar",
        palavrasChave="", duracao="", preRequisitos=prereq, descricao=nome, quadros=emptyList()
    )
    private val estilo = EstiloArteMarcialDefinition(
        id="s", nomePt="Estilo Teste", nomeEn="", descricao="", armaDoEstiloTexto=null,
        armaDoEstiloModo="desarmado", armasEspecificas=emptyList(), armaduraTexto=null,
        armaduraCategoria="todas", habilidadesComplementares=null,
        tiposExaltadosPermitidos=setOf(TipoExaltadoEncontro.SOLAR),
        encantos=listOf(charm("Raiz","Nenhum"), charm("Meio","Raiz"), charm("Topo","Meio"))
    )

    @Test fun `xp continua estilo principal sem usar Briga como sinergia`() {
        val npc = NpcEncontro(
            tipoExaltado=TipoExaltadoEncontro.SOLAR, essencia=3,
            abilities=mapOf("Briga" to 5), estiloArtesMarciais="Estilo Teste",
            charms=listOf(
                EncantoEncontro("Raiz","Estilo Teste",""),
                EncantoEncontro("Encanto de Briga","Briga","")
            )
        )
        val next = EncounterMartialArtsProgression.expandirSePossivel(npc, listOf(estilo))!!
        assertTrue(next.charms.any { it.nome == "Meio" })
        assertEquals(5, next.xpGastoTotal - npc.xpGastoTotal)
        assertEquals(npc.xpAtual, next.xpAtual)
        assertEquals(listOf("Meio"), next.historicoXpBatches.last().nomesEncantosAdicionados)
    }

    @Test fun `sem descendente legal devolve controle a progressao comum`() {
        val npc = NpcEncontro(
            tipoExaltado=TipoExaltadoEncontro.SOLAR, essencia=3,
            abilities=mapOf("Briga" to 5), estiloArtesMarciais="Estilo Teste",
            charms=estilo.encantos.map { EncantoEncontro(it.nome,"Estilo Teste","") }
        )
        assertNull(EncounterMartialArtsProgression.expandirSePossivel(npc, listOf(estilo)))
    }

    @Test fun `novo estilo so abre depois de principal desenvolvido e esgotado`() {
        fun outroCharm(nome: String, prereq: String) = EncantoArteMarcialDefinition(
            id="o-$nome", estiloId="o", habilidade="Outro Estilo", nome=nome, nomeIngles="",
            custo="", minsTexto="", minHabilidade=1, minEssencia=1, tipo="Suplementar",
            palavrasChave="", duracao="", preRequisitos=prereq, descricao=nome, quadros=emptyList()
        )
        val outro = EstiloArteMarcialDefinition(
            id="o", nomePt="Outro Estilo", nomeEn="", descricao="", armaDoEstiloTexto=null,
            armaDoEstiloModo="desarmado", armasEspecificas=emptyList(), armaduraTexto=null,
            armaduraCategoria="todas", habilidadesComplementares=null,
            tiposExaltadosPermitidos=setOf(TipoExaltadoEncontro.SOLAR),
            encantos=listOf(outroCharm("Outra Raiz","Nenhum"), outroCharm("Outro Meio","Outra Raiz"))
        )
        val completo = NpcEncontro(
            tipoExaltado=TipoExaltadoEncontro.SOLAR, essencia=3,
            abilities=mapOf("Briga" to 5), estiloArtesMarciais="Estilo Teste",
            charms=estilo.encantos.map { EncantoEncontro(it.nome,"Estilo Teste","") }
        )
        val next = EncounterMartialArtsProgression.expandirSePossivel(completo, listOf(estilo, outro))!!
        assertEquals(listOf("Outro Estilo"), next.estilosArtesMarciaisAdicionais)
        assertTrue(next.charms.any { it.nome == "Outra Raiz" })
    }


    @Test fun `novo estilo pode abrir removendo armadura incompatível`() {
        fun outroCharm(nome: String) = EncantoArteMarcialDefinition(
            id="x-$nome", estiloId="x", habilidade="Estilo Incompatível", nome=nome, nomeIngles="",
            custo="", minsTexto="", minHabilidade=1, minEssencia=1, tipo="Suplementar",
            palavrasChave="", duracao="", preRequisitos="Nenhum", descricao=nome, quadros=emptyList()
        )
        val incompatível = EstiloArteMarcialDefinition(
            id="x", nomePt="Estilo Incompatível", nomeEn="", descricao="", armaDoEstiloTexto=null,
            armaDoEstiloModo="desarmado", armasEspecificas=emptyList(), armaduraTexto=null,
            armaduraCategoria="incompativel", habilidadesComplementares=null,
            tiposExaltadosPermitidos=setOf(TipoExaltadoEncontro.SOLAR),
            encantos=listOf(outroCharm("Raiz Incompatível"))
        )
        val pesada = com.example.model.ArmaduraEncontro(
            nome="Pesada", peso="Pesada", tipo="Artefato", absorcao=11, dureza=10,
            penalidadeMobilidade=-2, motesComitados=5
        )
        val completo = NpcEncontro(
            tipoExaltado=TipoExaltadoEncontro.SOLAR, essencia=3,
            abilities=mapOf("Briga" to 5), estiloArtesMarciais="Estilo Teste",
            charms=estilo.encantos.map { EncantoEncontro(it.nome,"Estilo Teste","") },
            armadura=pesada
        )
        val next = EncounterMartialArtsProgression.expandirSePossivel(completo, listOf(estilo, incompatível))
        assertTrue(next != null)
        assertTrue(next!!.charms.any { it.nome == "Raiz Incompatível" })
        assertTrue("Estilo Incompatível" in next.estilosArtesMarciaisAdicionais)
        assertEquals("Sem armadura", next.armadura?.nome)
        assertEquals(0, next.armadura?.absorcao)
        assertTrue(EncounterMartialArmorAffinity.commonConfigurations(listOf(estilo, incompatível)).contains(null))
    }

    @Test fun `resultado informa quando progressao abre estilo adicional`() {
        val principal = estilo.copy(
            id = "principal",
            nomePt = "Principal",
            encantos = (1..5).map { i -> charm("P$i", "Nenhum").copy(estiloId = "principal", habilidade = "Principal") }
        )
        val adicional = estilo.copy(
            id = "adicional",
            nomePt = "Adicional",
            encantos = listOf(charm("A1", "Nenhum").copy(estiloId = "adicional", habilidade = "Adicional"))
        )
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            estiloArtesMarciais = "Principal",
            abilities = mapOf("Briga" to 5),
            essencia = 1,
            charms = principal.encantos.map { EncantoEncontro(it.nome, "Principal", it.custo) }
        )
        val resultado = EncounterMartialArtsProgression.expandirComResultado(npc, listOf(principal, adicional))
        assertTrue(resultado != null)
        assertTrue(resultado!!.abriuNovoEstilo)
        assertEquals("Adicional", resultado.estiloSelecionado)
        assertTrue("Adicional" in resultado.npc.estilosArtesMarciaisAdicionais)
    }
}
