package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCharmSelectionServiceTest {
    private fun lunar(
        nome: String,
        atributo: String,
        minAtributo: Int = 1,
        preRequisitos: String = "Nenhum"
    ) = EncantoLunarDefinition(
        id = nome,
        atributo = atributo,
        subdivisao = null,
        nome = nome,
        nomeIngles = nome,
        custo = "1m",
        minsTexto = "$atributo $minAtributo, Essência 1",
        minAtributo = minAtributo,
        minEssencia = 1,
        tipo = "",
        palavrasChave = "",
        duracao = "",
        preRequisitos = preRequisitos,
        descricao = ""
    )

    @Test
    fun lunarReconheceQuaisquerEncantosDeAtributoMental() {
        val catalogo = listOf(
            lunar("Força Mental 1", "Inteligência"),
            lunar("Força Mental 2", "Percepção"),
            lunar("Força Mental 3", "Raciocínio"),
            lunar("Força Mental 4", "Inteligência"),
            lunar("Feitiçaria Lunar", "Inteligência", preRequisitos = "Quaisquer quatro Encantamentos de Atributo Mental")
        )
        val selecionados = setOf("Força Mental 1", "Força Mental 2", "Força Mental 3", "Força Mental 4")

        assertTrue(
            EncounterCharmSelectionService.elegivelLunar(
                catalogo.last(),
                attributes = mapOf("Inteligência" to 5),
                essencia = 1,
                nomesSelecionados = selecionados,
                catalogoCompleto = catalogo
            )
        )
    }

    @Test
    fun lunarNaoLiberaPrerequisitoDeContagemComQuantidadeInsuficiente() {
        val catalogo = listOf(
            lunar("Mental 1", "Inteligência"),
            lunar("Mental 2", "Percepção"),
            lunar("Mental 3", "Raciocínio"),
            lunar("Feitiçaria Lunar", "Inteligência", preRequisitos = "Quaisquer quatro Encantamentos de Atributo Mental")
        )
        val selecionados = setOf("Mental 1", "Mental 2", "Mental 3")

        assertFalse(
            EncounterCharmSelectionService.elegivelLunar(
                catalogo.last(),
                attributes = mapOf("Inteligência" to 5),
                essencia = 1,
                nomesSelecionados = selecionados,
                catalogoCompleto = catalogo
            )
        )
    }
    @Test
    fun requisitosPorNomeContinuamExigindoTodosOsNomes() {
        val catalogo = listOf(
            lunar("Base 1", "Inteligência"),
            lunar("Base 2", "Percepção"),
            lunar("Dependente", "Inteligência", preRequisitos = "Base 1, Base 2")
        )
        assertFalse(EncounterCharmSelectionService.elegivelLunar(
            catalogo.last(), mapOf("Inteligência" to 5), 1, setOf("Base 1"), catalogo
        ))
        assertTrue(EncounterCharmSelectionService.elegivelLunar(
            catalogo.last(), mapOf("Inteligência" to 5), 1, setOf("Base 1", "Base 2"), catalogo
        ))
    }

    @Test
    fun requisitoPorContagemPermaneceCorretoEmChamadasRepetidas() {
        val catalogo = listOf(
            lunar("Mental 1", "Inteligência"),
            lunar("Mental 2", "Percepção"),
            lunar("Mental 3", "Raciocínio"),
            lunar("Mental 4", "Inteligência"),
            lunar("Feitiçaria Lunar", "Inteligência", preRequisitos = "Quaisquer quatro Encantamentos de Atributo Mental")
        )
        val selecionados = setOf("Mental 1", "Mental 2", "Mental 3", "Mental 4")
        repeat(EncounterTestSamples.count(100)) {
            assertTrue(EncounterCharmSelectionService.elegivelLunar(
                catalogo.last(), mapOf("Inteligência" to 5), 1, selecionados, catalogo
            ))
        }
    }

    private fun charm(
            nome: String,
            habilidade: String,
            minHabilidade: Int = 1,
            preRequisitos: String = "Nenhum"
        ) = EncantoSolarDefinition(
            id = nome,
            habilidade = habilidade,
            nome = nome,
            nomeIngles = nome,
            custo = "-",
            minsTexto = "$habilidade $minHabilidade, Essência 1",
            minHabilidade = minHabilidade,
            minEssencia = 1,
            tipo = "Permanente",
            palavrasChave = "Nenhuma",
            duracao = "Permanente",
            preRequisitos = preRequisitos,
            descricao = ""
        )

        @Test
        fun `projeto de feiticaria adiciona quatro ocultismo e circulo em uma unica operacao`() {
            val catalogo = listOf(
                charm("Ocultismo A", "Ocultismo"),
                charm("Ocultismo B", "Ocultismo"),
                charm("Ocultismo C", "Ocultismo"),
                charm("Ocultismo D", "Ocultismo"),
                charm("Feitiçaria do Círculo Terrestre", "Ocultismo", 3)
            )

            val resultado = EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre(
                catalogo = catalogo,
                selecionados = emptyList(),
                abilities = mapOf("Ocultismo" to 3),
                essencia = 1,
                quantidadeTotal = 5,
                exigirProjeto = true
            )

            assertEquals(5, resultado.size)
            assertEquals(4, resultado.count { it.habilidade == "Ocultismo" && it.nome != "Feitiçaria do Círculo Terrestre" })
            assertTrue(resultado.any { it.nome == "Feitiçaria do Círculo Terrestre" })
        }

        @Test
        fun `projeto incompleto nao deixa aquisicoes parciais`() {
            val catalogo = listOf(
                charm("Ocultismo A", "Ocultismo"),
                charm("Ocultismo B", "Ocultismo"),
                charm("Ocultismo C", "Ocultismo"),
                charm("Feitiçaria do Círculo Terrestre", "Ocultismo", 3)
            )
            val base = listOf(charm("Base", "Presença"))

            val resultado = EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre(
                catalogo = catalogo,
                selecionados = base,
                abilities = mapOf("Ocultismo" to 3),
                essencia = 1,
                quantidadeTotal = 5,
                exigirProjeto = true
            )

            assertEquals(base, resultado)
            assertFalse(resultado.any { it.nome == "Feitiçaria do Círculo Terrestre" })
        }

        @Test
        fun `projeto desativado preserva selecao exatamente`() {
            val base = listOf(charm("Base", "Presença"))
            val resultado = EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre(
                catalogo = emptyList(),
                selecionados = base,
                abilities = emptyMap(),
                essencia = 1,
                quantidadeTotal = 15,
                exigirProjeto = false
            )
            assertEquals(base, resultado)
        }

    @Test
    fun `selecao Lunar Fisico com Inteligencia 3 preserva quatro mentais e feiticaria`() {
        val catalogo = listOf(
            lunar("Corpo de Touro", "Vigor"),
            lunar("Físico 1", "Força"),
            lunar("Físico 2", "Destreza"),
            lunar("Físico 3", "Vigor"),
            lunar("Universal 1", "Universal", minAtributo = 0),
            lunar("Mental 1", "Inteligência"),
            lunar("Mental 2", "Percepção"),
            lunar("Mental 3", "Raciocínio"),
            lunar("Mental 4", "Inteligência"),
            lunar("Feitiçaria do Círculo Terrestre", "Inteligência", preRequisitos = "Quaisquer quatro Encantamentos de Atributo Mental"),
            lunar("Físico 4", "Força"),
            lunar("Físico 5", "Destreza"),
            lunar("Físico 6", "Vigor"),
            lunar("Universal 2", "Universal", minAtributo = 0),
            lunar("Mental 5", "Percepção"),
            lunar("Mental 6", "Raciocínio"),
            lunar("Mental 7", "Inteligência")
        )

        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciais(
            catalogo = catalogo,
            attributes = mapOf(
                "Força" to 4,
                "Destreza" to 3,
                "Vigor" to 5,
                "Carisma" to 1,
                "Manipulação" to 1,
                "Aparência" to 1,
                "Percepção" to 4,
                "Inteligência" to 3,
                "Raciocínio" to 3
            ),
            essencia = 1,
            ordemAtributos = listOf("Força", "Destreza", "Vigor", "Universal", "Inteligência", "Percepção", "Raciocínio"),
            quantidade = 15,
            random = kotlin.random.Random(161),
            arquetipo = ArquetipoEncontro.FISICO
        )

        assertEquals(15, resultado.size)
        assertTrue(resultado.count { it.atributo in ExaltedConstants.MENTAL_ATTRIBUTES } >= 4)
        assertTrue(resultado.any { it.nome == "Feitiçaria do Círculo Terrestre" })
        assertTrue(resultado.any { it.nome == "Corpo de Touro" })
    }

    @Test
    fun `selecao Lunar respeita bloco inicial do atributo principal`() {
        val catalogo = listOf(
            lunar("Força 1", "Força"),
            lunar("Força 2", "Força"),
            lunar("Força 3", "Força"),
            lunar("Força 4", "Força"),
            lunar("Destreza 1", "Destreza"),
            lunar("Destreza 2", "Destreza"),
            lunar("Vigor 1", "Vigor"),
            lunar("Universal", "Universal", minAtributo = 0),
            lunar("Mental 1", "Inteligência"),
            lunar("Mental 2", "Percepção"),
            lunar("Mental 3", "Raciocínio"),
            lunar("Social 1", "Carisma"),
            lunar("Social 2", "Manipulação"),
            lunar("Social 3", "Aparência"),
            lunar("Mental 4", "Inteligência")
        )

        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciais(
            catalogo = catalogo,
            attributes = mapOf(
                "Força" to 5,
                "Destreza" to 3,
                "Vigor" to 3,
                "Carisma" to 1,
                "Manipulação" to 1,
                "Aparência" to 1,
                "Percepção" to 2,
                "Inteligência" to 2,
                "Raciocínio" to 2
            ),
            essencia = 1,
            ordemAtributos = listOf("Força", "Destreza", "Vigor", "Universal", "Carisma", "Manipulação", "Aparência"),
            quantidade = 15,
            random = kotlin.random.Random(162),
            arquetipo = ArquetipoEncontro.FISICO
        )

        // Duas entradas de Destreza sao excluidas pela regra de arvore ofensiva unica.
        assertEquals(13, resultado.size)
        assertTrue(resultado.none { it.atributo == "Destreza" })
        assertTrue(resultado.count { it.atributo == "Força" } >= 3)
        assertTrue(resultado.any { it.atributo == "Universal" })
    }

    @Test
    fun `selecao Solar aprofunda primeira arvore antes de abrir a segunda`() {
        val principal = (1..8).map { charm("Melee $it", "Armas Brancas") }
        val secundaria = (1..8).map { charm("Presence $it", "Presença") }
        val resultado = EncounterCharmSelectionService.selecionarIniciais(
            catalogo = principal + secundaria,
            abilities = mapOf("Armas Brancas" to 5, "Presença" to 5),
            essencia = 1,
            ordemHabilidades = listOf("Armas Brancas", "Presença"),
            quantidade = 6,
            habilidadePrincipalArquetipo = "Armas Brancas"
        )

        assertEquals(6, resultado.size)
        assertEquals(6, resultado.count { it.habilidade == "Armas Brancas" })
        assertEquals(0, resultado.count { it.habilidade == "Presença" })
    }

    @Test
    fun `selecao Lunar aprofunda atributo principal antes de abrir outro atributo`() {
        val forca = (1..9).map { lunar("Força Profunda $it", "Força") }
        val destreza = (1..9).map { lunar("Destreza Rasa $it", "Destreza") }
        val vigor = (1..9).map { lunar("Vigor Raso $it", "Vigor") }
        val universal = lunar("Universal Obrigatório", "Universal", minAtributo = 0)

        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciais(
            catalogo = forca + destreza + vigor + universal,
            attributes = mapOf(
                "Força" to 5, "Destreza" to 3, "Vigor" to 3,
                "Carisma" to 1, "Manipulação" to 1, "Aparência" to 1,
                "Percepção" to 1, "Inteligência" to 1, "Raciocínio" to 1
            ),
            essencia = 1,
            ordemAtributos = listOf("Força", "Destreza", "Vigor"),
            quantidade = 10,
            random = kotlin.random.Random(405),
            arquetipo = ArquetipoEncontro.FISICO
        )

        assertEquals(10, resultado.size)
        assertEquals(9, resultado.count { it.atributo == "Força" })
        assertEquals(1, resultado.count { it.atributo == "Universal" })
    }

    @Test
    fun `selecao Lunar preserva atributo da rota arquetipo escolhida`() {
        val arquetipoPercepcao = lunar("Passo Arquétipo", "Destreza", minAtributo = 4).copy(
            rotasArquetipo = listOf(
                LunarCharmArchetypeRoute(
                    atributo = "Percepção",
                    condicao = "VISAO_NOTURNA",
                    minAtributo = 4,
                    preRequisitosAlternativos = "Nenhum"
                )
            )
        )
        val percepcao = (1..8).map { lunar("Percepção $it", "Percepção") }
        val outros = (1..8).map { lunar("Raciocínio $it", "Raciocínio") }

        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = listOf(arquetipoPercepcao) + percepcao + outros,
            attributes = mapOf(
                "Força" to 1, "Destreza" to 2, "Vigor" to 1,
                "Carisma" to 1, "Manipulação" to 1, "Aparência" to 1,
                "Percepção" to 5, "Inteligência" to 2, "Raciocínio" to 4
            ),
            essencia = 1,
            ordemAtributos = listOf("Percepção", "Raciocínio"),
            quantidade = 8,
            random = kotlin.random.Random(448),
            arquetipo = ArquetipoEncontro.MENTAL,
            atributoFocoUsuario = "Percepção",
            spiritTraits = setOf(LunarSpiritTrait.VISAO_NOTURNA)
        )

        assertTrue(resultado.charms.any { it.nome == "Passo Arquétipo" })
        assertEquals("Percepção", resultado.acquisitionAttributes["Passo Arquétipo"])
        assertEquals("Destreza", arquetipoPercepcao.atributo)
    }


}
