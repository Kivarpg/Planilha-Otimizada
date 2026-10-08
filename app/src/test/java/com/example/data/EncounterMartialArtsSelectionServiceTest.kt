package com.example.data

import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

class EncounterMartialArtsSelectionServiceTest {
    private fun charm(nome: String, prereq: String, min: Int = 1) =
        EncantoArteMarcialDefinition(
            id = nome, estiloId = "style", habilidade = "Estilo Teste",
            nome = nome, nomeIngles = "", custo = "", minsTexto = "",
            minHabilidade = min, minEssencia = 1, tipo = "Suplementar",
            palavrasChave = "", duracao = "", preRequisitos = prereq,
            descricao = nome, quadros = emptyList()
        )

    private fun style(armadura: String = "todas") = EstiloArteMarcialDefinition(
        id = "style", nomePt = "Estilo Teste", nomeEn = "", descricao = "",
        armaDoEstiloTexto = null, armaDoEstiloModo = "hibrido", armasEspecificas = emptyList(),
        armaduraTexto = null, armaduraCategoria = armadura, habilidadesComplementares = null,
        tiposExaltadosPermitidos = setOf(TipoExaltadoEncontro.SOLAR),
        encantos = listOf(
            charm("Raiz", "Nenhum"),
            charm("Meio", "Raiz"),
            charm("Topo", "Meio")
        )
    )

    @Test fun `rota marcial aprofunda a mesma arvore respeitando prerequisitos`() {
        val result = EncounterMartialArtsSelectionService.selecionar(
            listOf(style()), TipoExaltadoEncontro.SOLAR, briga = 5, essencia = 3,
            random = Random(1), armaduraPeso = "Pesada", quantidade = 3
        )
        assertNotNull(result)
        assertEquals(listOf("Raiz", "Meio", "Topo"), result!!.encantos.map { it.nome })
    }

    @Test fun `armadura atual nao bloqueia estilo que pode lutar sem armadura`() {
        val result = EncounterMartialArtsSelectionService.selecionar(
            listOf(style("incompativel")), TipoExaltadoEncontro.SOLAR, 5, 3,
            Random(1), armaduraPeso = "Leve"
        )
        assertNotNull(result)
        assertEquals("Estilo Teste", result!!.estilo.nomePt)
        assertTrue(EncounterMartialArmorAffinity.commonConfigurations(listOf(result.estilo)).contains(null))
    }

    @Test fun `pre requisito ausente com aparência de encanto não é aceito silenciosamente`() {
        val conhecidos = emptySet<String>()
        assertFalse(
            EncounterMartialArtsSelectionService.requisitoMarcialSatisfeito(
                "Golpe Fantasma Ancestral", emptyMap(), conhecidos
            )
        )
    }

    @Test fun `requisito textual externo continua delegado às camadas de rota`() {
        assertTrue(
            EncounterMartialArtsSelectionService.requisitoMarcialSatisfeito(
                "Briga 3", emptyMap(), emptySet()
            )
        )
    }


    @Test fun `estilo adicional com arma especifica exige arma atual compativel`() {
        val base = style().copy(
            armaDoEstiloModo = "armado",
            armasEspecificas = listOf("Daiklave")
        )
        assertTrue(EncounterMartialArtsSelectionService.armaNpcCompativel(base, "Daiklave da Aurora (Média)"))
        assertFalse(EncounterMartialArtsSelectionService.armaNpcCompativel(base, "Arco de Poder (Média)"))
        assertFalse(EncounterMartialArtsSelectionService.armaNpcCompativel(base, null))
    }


    @Test fun `integracao marcial preserva encanto estrutural protegido`() {
        val normais = (1..15).map { indice ->
            com.example.model.EncantoEncontro(
                nome = if (indice == 15) "Expressão da Alma da Quimera" else "Normal $indice",
                habilidadeVinculada = "Percepção",
                custo = ""
            )
        }
        val selecao = EncounterMartialArtsSelectionService.Selection(
            estilo = style(),
            encantos = style().encantos.take(3)
        )

        val integrado = EncounterMartialArtsSelectionService.integrarMantendoQuantidade(
            normais,
            selecao,
            quantidadeTotal = 15,
            nomesProtegidos = setOf("Expressão da Alma da Quimera")
        )

        assertEquals(15, integrado.size)
        assertTrue(integrado.any { it.nome == "Expressão da Alma da Quimera" })
        assertEquals(3, integrado.count { it.habilidadeVinculada == "Estilo Teste" })
    }

}
