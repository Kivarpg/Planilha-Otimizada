package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.EspecialidadeEncontro
import com.example.model.NpcEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterGeneratorXpTest {
    private fun npcBase(
        habilidade: String = "Armas Brancas",
        nivel: Int = 1,
        especialidades: List<EspecialidadeEncontro> = emptyList()
    ) = NpcEncontro(
        arquetipo = ArquetipoEncontro.FISICO,
        habilidadePrincipal = habilidade,
        habilidadeSupernal = "",
        habilidadesFavorecidas = emptyList(),
        abilities = mapOf(habilidade to nivel),
        especialidades = especialidades,
        healthBoxes = EncounterGenerator.trilhaVitalidadePorVigor(3, 0)
    )

    @Test
    fun `lunar social pode adquirir encanto de Forca sem bloqueio fisico`() {
        val encanto = EncantoLunarDefinition(
            id = "forca-social", atributo = "Força", subdivisao = null,
            nome = "Encanto Forca Social", nomeIngles = "", custo = "1",
            minsTexto = "Força 2, Essência 1", minAtributo = 2,
            minEssencia = 1, tipo = "Reflexivo", palavrasChave = "",
            duracao = "", preRequisitos = "Nenhum", descricao = ""
        )
        val inicial = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            arquetipo = ArquetipoEncontro.SOCIAL,
            attributes = mapOf("Força" to 3, "Destreza" to 3, "Vigor" to 3),
            xpAtual = 20
        )
        val resultado = EncounterExperienceLunar.expandLunar(
            inicial, listOf(encanto), listOf("Força", "Destreza", "Vigor")
        )
        assertTrue(resultado.charms.any { it.nome == encanto.nome })
        assertEquals(null, resultado.lunarAtaqueEscolhido)
    }

    @Test
    fun `lunar expansao repetida equivale a lotes individuais com compras reais`() {
        fun encanto(nome: String, atributo: String, pre: String = "Nenhum") =
            EncantoLunarDefinition(
                id = nome, atributo = atributo, subdivisao = null,
                nome = nome, nomeIngles = "", custo = "1",
                minsTexto = "$atributo 2, Essência 1", minAtributo = 2,
                minEssencia = 1, tipo = "Reflexivo", palavrasChave = "",
                duracao = "", preRequisitos = pre, descricao = ""
            )
        val catalogo = listOf(
            encanto("Base ofensiva", "Destreza"),
            encanto("Seguimento ofensivo", "Destreza", "Base ofensiva"),
            encanto("Protecao", "Vigor"),
            encanto("Ataque descartado", "Força")
        )
        val inicial = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = "Destreza",
            attributes = mapOf("Força" to 3, "Destreza" to 4, "Vigor" to 4),
            xpAtual = 12
        )
        val ordem = listOf("Força", "Vigor", "Destreza")
        val sequencial = (1..5).fold(inicial) { atual, _ ->
            EncounterExperienceLunar.expandLunar(atual, catalogo, ordem)
        }
        val otimizado = EncounterExperienceLunar.expandLunarRepeated(
            inicial, catalogo, ordem, 5
        )
        assertEquals(sequencial, otimizado)
        assertTrue(otimizado.charms.any { it.nome == "Seguimento ofensivo" })
        assertTrue(otimizado.charms.none { it.nome == "Ataque descartado" })
    }

    @Test
    fun `lunar nao deixa Vigor ultrapassar tres encantos da ofensiva em lotes sucessivos`() {
        fun encanto(nome: String, atributo: String) = EncantoLunarDefinition(
            id = nome, atributo = atributo, subdivisao = null,
            nome = nome, nomeIngles = "", custo = "1",
            minsTexto = "$atributo 2, Essência 1", minAtributo = 2,
            minEssencia = 1, tipo = "Reflexivo", palavrasChave = "",
            duracao = "", preRequisitos = "Nenhum", descricao = ""
        )
        val catalogo = (1..7).map { encanto("Defesa $it", "Vigor") } +
            listOf(encanto("Ataque unico", "Destreza"))
        val inicial = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = "Destreza",
            attributes = mapOf("Força" to 3, "Destreza" to 4, "Vigor" to 4),
            xpAtual = 100
        )
        val evoluido = EncounterExperienceLunar.expandLunarRepeated(
            inicial, catalogo, listOf("Vigor", "Força", "Destreza"), 3
        )
        val ofensivos = evoluido.charms.count { it.habilidadeVinculada == "Destreza" }
        val defensivos = evoluido.charms.count { it.habilidadeVinculada == "Vigor" }
        assertEquals(1, ofensivos)
        assertTrue(defensivos <= ofensivos + 3)
        assertTrue(evoluido.charms.none { it.habilidadeVinculada == "Força" })
    }

    @Test
    fun `lunar expansao repetida preserva resultado dos lotes individuais`() {
        val inicial = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = "Força",
            attributes = mapOf("Força" to 4, "Destreza" to 3, "Vigor" to 3)
        )
        val ordem = listOf("Destreza", "Vigor", "Força")
        val individual = (1..3).fold(inicial) { atual, _ ->
            EncounterExperienceLunar.expandLunar(atual, emptyList(), ordem)
        }
        val repetido = EncounterExperienceLunar.expandLunarRepeated(
            inicial, emptyList(), ordem, 3
        )
        assertEquals(individual, repetido)
        assertEquals("Força", repetido.lunarAtaqueEscolhido)
        assertEquals(3, repetido.historicoXpBatches.size)
    }

    @Test
    fun `lunar legado fixa especializacao ofensiva no primeiro lote de XP`() {
        val legado = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = null,
            focoProgressaoExplicito = "Força",
            attributes = mapOf("Força" to 4, "Destreza" to 3, "Vigor" to 3)
        )
        val primeiro = EncounterExperienceLunar.expandLunar(
            legado, emptyList(), listOf("Destreza", "Força", "Vigor")
        )
        assertEquals("Força", primeiro.lunarAtaqueEscolhido)
        val segundo = EncounterExperienceLunar.expandLunar(
            primeiro, emptyList(), listOf("Destreza", "Força", "Vigor")
        )
        assertEquals("Força", segundo.lunarAtaqueEscolhido)
    }

    @Test
    fun `lunar aprofunda cadeia ofensiva quando Essencia aumenta`() {
        fun encanto(nome: String, essencia: Int, pre: String) = EncantoLunarDefinition(
            id = nome, atributo = "Destreza", subdivisao = null,
            nome = nome, nomeIngles = "", custo = "1",
            minsTexto = "Destreza 2, Essência $essencia",
            minAtributo = 2, minEssencia = essencia, tipo = "Reflexivo",
            palavrasChave = "", duracao = "", preRequisitos = pre, descricao = ""
        )
        val catalogo = listOf(
            encanto("Base da cadeia", 1, "Nenhum"),
            encanto("Meio da cadeia", 1, "Base da cadeia"),
            encanto("Topo da cadeia", 2, "Meio da cadeia")
        )
        val inicial = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = "Destreza",
            attributes = mapOf("Força" to 3, "Destreza" to 4, "Vigor" to 3),
            xpGastoTotal = 47,
            xpAtual = 30
        )
        val evoluido = EncounterExperienceLunar.expandLunar(
            inicial, catalogo, listOf("Vigor", "Força", "Destreza")
        )
        assertEquals(
            listOf("Base da cadeia", "Meio da cadeia", "Topo da cadeia"),
            evoluido.charms.map { it.nome }
        )
        assertTrue(evoluido.essencia >= 2)
        assertEquals("Destreza", evoluido.lunarAtaqueEscolhido)
    }

    @Test
    fun `lunar fisico bloqueia rota alternativa da ofensiva descartada`() {
        val alternativo = EncantoLunarDefinition(
            id = "alternativo", atributo = "Destreza", subdivisao = null,
            nome = "Encanto de rota alternativa", nomeIngles = "", custo = "1",
            minsTexto = "Destreza 2, Essência 1", minAtributo = 2,
            minEssencia = 1, tipo = "Reflexivo", palavrasChave = "",
            duracao = "", preRequisitos = "Nenhum", descricao = "",
            rotasArquetipo = listOf(
                LunarCharmArchetypeRoute("Força", "VISAO_NOTURNA", 2, "Nenhum")
            )
        )
        val npc = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = "Destreza",
            attributes = mapOf("Força" to 4, "Destreza" to 4, "Vigor" to 3),
            lunarArchetypeTraits = listOf("VISAO_NOTURNA"),
            xpAtual = 20
        )
        val evoluido = EncounterExperienceLunar.expandLunar(
            npc, listOf(alternativo), listOf("Força", "Destreza", "Vigor")
        )
        assertEquals(1, evoluido.charms.count { it.nome == alternativo.nome })
        assertEquals("Destreza", evoluido.charms.single().habilidadeVinculada)
        assertEquals("Destreza", evoluido.lunarAtaqueEscolhido)
    }

    @Test
    fun `lunar fisico compra somente arvore ofensiva escolhida e respeita prerequisitos`() {
        fun encanto(nome: String, atributo: String, pre: String = "Nenhum") =
            EncantoLunarDefinition(
                id = nome, atributo = atributo, subdivisao = null, nome = nome, nomeIngles = "",
                custo = "1", minsTexto = "$atributo 2, Essência 1",
                minAtributo = 2, minEssencia = 1, tipo = "Reflexivo",
                palavrasChave = "", duracao = "", preRequisitos = pre, descricao = ""
            )
        val catalogo = listOf(
            encanto("Ataque Destreza Base", "Destreza"),
            encanto("Ataque Destreza Avancado", "Destreza", "Ataque Destreza Base"),
            encanto("Ataque Forca Proibido", "Força"),
            encanto("Defesa Vigor", "Vigor")
        )
        val npc = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = "Destreza",
            attributes = mapOf("Força" to 3, "Destreza" to 4, "Vigor" to 4),
            xpAtual = 40
        )
        val evoluido = EncounterExperienceLunar.expandLunar(
            npc, catalogo, listOf("Força", "Vigor", "Destreza")
        )
        assertTrue(evoluido.charms.any { it.nome == "Ataque Destreza Base" })
        assertTrue(evoluido.charms.any { it.nome == "Ataque Destreza Avancado" })
        assertTrue(evoluido.charms.none { it.nome == "Ataque Forca Proibido" })
        val ataque = evoluido.charms.count { it.habilidadeVinculada == "Destreza" }
        val vigor = evoluido.charms.count { it.habilidadeVinculada == "Vigor" }
        assertTrue(vigor <= ataque + 3)
    }

    @Test
    fun `lunar preserva especializacao ofensiva entre lotes e reversao de XP`() {
        val original = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            lunarAtaqueEscolhido = "Destreza",
            focoProgressaoExplicito = null,
            attributes = mapOf("Força" to 3, "Destreza" to 4, "Vigor" to 3)
        )
        val ordem = listOf("Força", "Vigor", "Destreza")
        val lote1 = EncounterExperienceLunar.expandLunar(original, emptyList(), ordem)
        val lote2 = EncounterExperienceLunar.expandLunar(lote1, emptyList(), ordem)
        assertEquals("Destreza", lote1.lunarAtaqueEscolhido)
        assertEquals("Destreza", lote2.lunarAtaqueEscolhido)
        assertEquals(2, lote2.historicoXpBatches.size)
        val revertido = EncounterExperienceLunar.reduceLunar(lote2)
        assertEquals("Destreza", revertido.lunarAtaqueEscolhido)
        assertEquals(lote1.historicoXpBatches, revertido.historicoXpBatches)
    }

    @Test
    fun `encanto favorecido e comprado assim que o saldo chega a 8 XP`() {
        val encanto = EncantoSolarDefinition(
            id = "xp-favored", habilidade = "Armas Brancas", nome = "Encanto Favorecido",
            nomeIngles = "Favored Charm", custo = "1", minsTexto = "Armas Brancas •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "", descricao = ""
        )
        val antes = npcBase().copy(
            habilidadeSupernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Armas Brancas")
        )
        val lote1 = EncounterGenerator.expandirEncantosPorExperiencia(antes, listOf(encanto))
        assertTrue(lote1.charms.isEmpty())
        assertEquals(5, lote1.xpAtual)

        val lote2 = EncounterGenerator.expandirEncantosPorExperiencia(lote1, listOf(encanto))
        assertEquals(1, lote2.charms.count { it.nome == "Encanto Favorecido" })
        assertEquals(8, lote2.xpGastoTotal)
        assertEquals(2, lote2.xpAtual)
        assertEquals(8, lote2.historicoXpBatches.last().xpGasto)
    }

    @Test
    fun `sem encanto elegivel o lote continua podendo gastar em habilidade e especializacao`() {
        // Especialização exige Habilidade 2+; o fixture deve representar um candidato legal.
        val antes = npcBase(nivel = 2)
        val depois = EncounterGenerator.expandirEncantosPorExperiencia(antes, emptyList())
        assertEquals(2, depois.abilities["Armas Brancas"])
        assertEquals(1, depois.especialidades.count { it.habilidade == "Armas Brancas" })
        assertEquals(3, depois.xpGastoTotal)
        assertEquals(2, depois.xpAtual)
        assertEquals(1, depois.historicoXpBatches.size)
        assertEquals(3, depois.historicoXpBatches.single().xpGasto)
    }

    @Test
    fun `desfazer lote reverte habilidade especializacao XP e historico`() {
        val antes = npcBase()
        val depois = EncounterGenerator.expandirEncantosPorExperiencia(antes, emptyList())
        val revertido = EncounterGenerator.reduzirExperiencia(depois)
        assertEquals(antes.abilities, revertido.abilities)
        assertEquals(antes.especialidades, revertido.especialidades)
        assertEquals(0, revertido.xpGastoTotal)
        assertEquals(0, revertido.xpAtual)
        assertTrue(revertido.historicoXpBatches.isEmpty())
    }

    @Test
    fun `desfazer lote remove somente a especializacao adicionada pelo ultimo lote`() {
        val antes = npcBase(especialidades = listOf(EspecialidadeEncontro("Investigação")))
        val depois = EncounterGenerator.expandirEncantosPorExperiencia(antes, emptyList())
        val revertido = EncounterGenerator.reduzirExperiencia(depois)
        assertEquals(listOf(EspecialidadeEncontro("Investigação")), revertido.especialidades)
    }

    @Test
    fun `sangue de dragao usa o mesmo pipeline de XP sem exigir catalogo`() {
        // Mesmo contrato Solar: especialização só é legal com Habilidade 2+.
        val antes = npcBase(nivel = 2)
        val depois = EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(antes, emptyList())
        assertEquals(2, depois.abilities["Armas Brancas"])
        assertEquals(1, depois.especialidades.count { it.habilidade == "Armas Brancas" })
        assertEquals(3, depois.xpGastoTotal)
    }
    @Test
    fun `expansao de XP remove alerta de validacao que deixou de ser verdadeiro`() {
        val antes = npcBase().copy(
            charms = listOf(
                com.example.model.EncantoEncontro(
                    com.example.model.NOME_CORPO_DE_TOURO,
                    "Resistência",
                    "1"
                )
            ),
            corpoDeTouroCount = 1,
            alertasValidacao = listOf(
                "Corpo de Touro não foi adquirido — os 3 arquétipos esperam pelo menos 1 aquisição."
            )
        )

        val depois = EncounterGenerator.expandirEncantosPorExperiencia(antes, emptyList())

        assertTrue(depois.charms.any { it.nome == com.example.model.NOME_CORPO_DE_TOURO })
        assertTrue(depois.alertasValidacao.none { it.startsWith("Corpo de Touro não foi adquirido") })
    }

    @Test
    fun `desfazer quando nao existe lote nao altera NPC`() {
        val antes = npcBase()
        assertEquals(antes, EncounterGenerator.reduzirExperiencia(antes))
    }

    @Test
    fun `dois lotes podem ser desfeitos em ordem inversa`() {
        val antes = npcBase()
        val lote1 = EncounterGenerator.expandirEncantosPorExperiencia(antes, emptyList())
        val lote2 = EncounterGenerator.expandirEncantosPorExperiencia(lote1, emptyList())

        val aposSegundo = EncounterGenerator.reduzirExperiencia(lote2)
        val aposPrimeiro = EncounterGenerator.reduzirExperiencia(aposSegundo)

        assertEquals(lote1.xpAtual, aposSegundo.xpAtual)
        assertEquals(lote1.xpGastoTotal, aposSegundo.xpGastoTotal)
        assertEquals(lote1.abilities, aposSegundo.abilities)
        assertEquals(lote1.especialidades, aposSegundo.especialidades)
        assertEquals(lote1.historicoXpBatches, aposSegundo.historicoXpBatches)
        assertEquals(antes.xpAtual, aposPrimeiro.xpAtual)
        assertEquals(antes.xpGastoTotal, aposPrimeiro.xpGastoTotal)
        assertEquals(antes.abilities, aposPrimeiro.abilities)
        assertEquals(antes.especialidades, aposPrimeiro.especialidades)
        assertTrue(aposPrimeiro.historicoXpBatches.isEmpty())
        assertEquals(antes.healthBoxes.map { it.id }, aposPrimeiro.healthBoxes.map { it.id })
    }


    @Test
    fun `aumentar XP atualiza motes quando a Essencia sobe`() {
        // Com um Encanto favorecido disponível, 8 XP são gastos assim que
        // houver saldo suficiente. Partindo de 47, o lote chega a 52, compra
        // o Encanto e o total gasto vai a 55, cruzando o marco de Essência.
        val encanto = EncantoSolarDefinition(
            id = "xp-essence", habilidade = "Armas Brancas", nome = "Encanto Essencia",
            nomeIngles = "Essence Charm", custo = "1", minsTexto = "Armas Brancas •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "", descricao = ""
        )
        val antes = npcBase().copy(
            habilidadeSupernal = "Armas Brancas", habilidadesFavorecidas = listOf("Armas Brancas"),
            xpGastoTotal = 47,
            xpAtual = 3,
            essencia = 1,
            motesPersonais = 13,
            motesPerifericos = 26
        )
        val depois = EncounterGenerator.expandirEncantosPorExperiencia(antes, listOf(encanto))

        assertEquals(2, depois.essencia)
        assertEquals(16, depois.motesPersonais)
        assertEquals(40, depois.motesPerifericos)
    }

    @Test
    fun `desfazer XP restaura motes da Essencia anterior para Sangue de Dragao`() {
        val antes = npcBase(nivel = 2).copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            xpGastoTotal = 72,
            xpAtual = 0,
            essencia = 2,
            motesPersonais = 13,
            motesPerifericos = 31
        )
        val depois = EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(antes, emptyList())
        val revertido = EncounterGenerator.reduzirExperiencia(depois)

        assertEquals(3, depois.essencia)
        assertEquals(14, depois.motesPersonais)
        assertEquals(35, depois.motesPerifericos)
        assertEquals(antes.motesPersonais, revertido.motesPersonais)
        assertEquals(antes.motesPerifericos, revertido.motesPerifericos)
    }

    @Test
    fun `desfazer lote remove somente as aquisicoes de Corpo de Touro daquele lote`() {
        val nome = com.example.model.NOME_CORPO_DE_TOURO
        val antes = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.SOLAR,
            charms = listOf(
                com.example.model.EncantoEncontro(nome, "Resistência", "1"),
                com.example.model.EncantoEncontro(nome, "Resistência", "1")
            ),
            corpoDeTouroCount = 2,
            xpGastoTotal = 5,
            xpAtual = 0,
            historicoXpBatches = listOf(
                com.example.model.HistoricoXpBatch(
                    xpGasto = 5,
                    nomesEncantosAdicionados = listOf(nome)
                )
            )
        )

        val revertido = EncounterGenerator.reduzirExperiencia(antes)
        assertEquals(1, revertido.charms.count { it.nome == nome })
    }

    @Test
    fun `desfazer lote Lunar remove somente a aquisicao de Corpo de Touro daquele lote`() {
        val nome = com.example.model.NOME_CORPO_DE_TOURO
        val antes = npcBase().copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.LUNAR,
            charms = listOf(
                com.example.model.EncantoEncontro(nome, "Vigor", "1"),
                com.example.model.EncantoEncontro(nome, "Vigor", "1")
            ),
            corpoDeTouroCount = 2,
            xpGastoTotal = 10,
            xpAtual = 0,
            historicoXpBatches = listOf(
                com.example.model.HistoricoXpBatch(
                    xpGasto = 10,
                    nomesEncantosAdicionados = listOf(nome)
                )
            )
        )

        val revertido = EncounterGenerator.reduzirExperiencia(antes)
        assertEquals(1, revertido.charms.count { it.nome == nome })
    }

    @Test
    fun `lote sem compra ainda e totalmente reversivel`() {
        // Habilidades já no teto e especialidade já presente: o lote só
        // acrescenta XP_POR_CHAMADA sem gastar nada (catálogo vazio).
        val antes = npcBase(
            habilidade = "Armas Brancas",
            nivel = 5,
            especialidades = listOf(EspecialidadeEncontro("Armas Brancas"))
        ).copy(
            charms = emptyList(),
            xpAtual = 0,
            xpGastoTotal = 0
        )
        val depois = EncounterGenerator.expandirEncantosPorExperiencia(antes, emptyList())
        assertEquals(5, depois.xpAtual)
        assertEquals(0, depois.xpGastoTotal)
        assertEquals(1, depois.historicoXpBatches.size)
        assertEquals(0, depois.historicoXpBatches.single().xpGasto)

        val revertido = EncounterGenerator.reduzirExperiencia(depois)
        // Compara campos de progressão (recalcularDerivados preenche stats de
        // combate que o npcBase deixa em 0).
        assertEquals(antes.xpAtual, revertido.xpAtual)
        assertEquals(antes.xpGastoTotal, revertido.xpGastoTotal)
        assertEquals(antes.abilities, revertido.abilities)
        assertEquals(antes.especialidades, revertido.especialidades)
        assertEquals(antes.charms, revertido.charms)
        assertTrue(revertido.historicoXpBatches.isEmpty())
    }

    @Test
    fun `tres lotes de Corpo de Touro sao desfeitos um por um`() {
        val nome = com.example.model.NOME_CORPO_DE_TOURO
        var npc = npcBase().copy(
            abilities = mapOf("Armas Brancas" to 1, "Resistência" to 3),
            charms = listOf(com.example.model.EncantoEncontro(nome, "Resistência", "1")),
            corpoDeTouroCount = 1,
            xpGastoTotal = 15,
            xpAtual = 0,
            historicoXpBatches = listOf(
                com.example.model.HistoricoXpBatch(5, listOf(nome)),
                com.example.model.HistoricoXpBatch(5, listOf(nome)),
                com.example.model.HistoricoXpBatch(5, listOf(nome))
            )
        )
        npc = npc.copy(charms = npc.charms + listOf(
            com.example.model.EncantoEncontro(nome, "Resistência", "1"),
            com.example.model.EncantoEncontro(nome, "Resistência", "1"),
            com.example.model.EncantoEncontro(nome, "Resistência", "1")
        ), corpoDeTouroCount = 4)

        val apos3 = EncounterGenerator.reduzirExperiencia(npc)
        assertEquals(3, apos3.charms.count { it.nome == nome })
        val apos2 = EncounterGenerator.reduzirExperiencia(apos3)
        assertEquals(2, apos2.charms.count { it.nome == nome })
        val apos1 = EncounterGenerator.reduzirExperiencia(apos2)
        assertEquals(1, apos1.charms.count { it.nome == nome })
    }

    @Test
    fun `expansao repetida preserva exatamente a semantica de lotes sucessivos`() {
        val antes = npcBase()
        var sucessivo = antes
        repeat(8) {
            sucessivo = EncounterGenerator.expandirEncantosPorExperiencia(sucessivo, emptyList())
        }

        val emLote = EncounterGenerator.expandirEncantosPorExperienciaRepetidos(
            antes, emptyList(), 8
        )

        assertEquals(sucessivo, emLote)
    }

    @Test
    fun `contagem de pre requisito considera aquisicoes repetiveis do mesmo encanto`() {
        val nomeCorpo = com.example.model.NOME_CORPO_DE_TOURO
        val corpo = EncantoSolarDefinition(
            id = "corpo-contagem", habilidade = "Resistência", nome = nomeCorpo,
            nomeIngles = "Ox-Body", custo = "1", minsTexto = "Resistência •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "", descricao = ""
        )
        val dependente = EncantoSolarDefinition(
            id = "dependente-contagem", habilidade = "Resistência", nome = "Dependente de Dois",
            nomeIngles = "Two-Prerequisite", custo = "1", minsTexto = "Resistência •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente",
            preRequisitos = "Quaisquer dois Encantos de Resistência", descricao = ""
        )
        val antes = npcBase(habilidade = "Resistência", nivel = 5).copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.SOLAR,
            especialidades = listOf(EspecialidadeEncontro("Resistência")),
            charms = listOf(com.example.model.EncantoEncontro(nomeCorpo, "Resistência", "1")),
            corpoDeTouroCount = 1
        )

        // Dependente antes de Corpo no catálogo: o pipeline ainda precisa
        // adquirir um segundo Corpo (repetível) para satisfazer "dois Encantos
        // de Resistência" e só então libera o Dependente.
        val depois = EncounterExperienceService.expand(
            npc = antes,
            catalogo = listOf(dependente, corpo),
            ordemHabilidades = listOf("Resistência"),
            custoEncanto = { 1 },
            trilhaVitalidade = EncounterGenerator::trilhaVitalidadePorVigor
        )

        assertTrue(depois.charms.any { it.nome == "Dependente de Dois" })
        assertTrue(depois.charms.count { it.nome == nomeCorpo } >= 2)
    }
    @Test
    fun `progressao XP nao bloqueia ramo posterior por candidato caro do primeiro ramo`() {
        val ramoPrioritarioCaro = EncantoSolarDefinition(
            id = "ramo-caro", habilidade = "Armas Brancas", nome = "Encanto Caro",
            nomeIngles = "Expensive", custo = "1", minsTexto = "Armas Brancas •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "", descricao = ""
        )
        val ramoAlternativoBarato = EncantoSolarDefinition(
            id = "ramo-barato", habilidade = "Presença", nome = "Encanto Alternativo",
            nomeIngles = "Alternative", custo = "1", minsTexto = "Presença •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "", descricao = ""
        )
        val antes = npcBase("Armas Brancas", 1).copy(
            habilidadeSupernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Armas Brancas"),
            // Presença precisa aparecer aqui — sem isso o ramo alternativo nunca
            // fica elegível (nível de habilidade 0 < minHabilidade 1) e o teste
            // não exercita o cenário que se propõe a cobrir.
            abilities = mapOf("Armas Brancas" to 1, "Presença" to 1),
            // xpAtual + XP_POR_CHAMADA (5) precisa ficar ABAIXO do custo do ramo
            // caro (10) e ainda cobrir o ramo barato (8) — com xpAtual = 5 o total
            // batia exatamente no custo do ramo caro (10), então ele cabia no saldo
            // e era comprado primeiro, o oposto do que o teste verifica.
            xpAtual = 3
        )
        val depois = EncounterExperienceService.expand(
            npc = antes,
            catalogo = listOf(ramoPrioritarioCaro, ramoAlternativoBarato),
            ordemHabilidades = listOf("Armas Brancas", "Presença"),
            custoEncanto = { habilidade -> if (habilidade == "Armas Brancas") 10 else 8 },
            trilhaVitalidade = EncounterGenerator::trilhaVitalidadePorVigor
        )
        assertTrue("O ramo alternativo deveria ser considerado", depois.charms.any { it.nome == "Encanto Alternativo" })
        assertTrue("O ramo caro não deve bloquear o alternativo", depois.charms.none { it.nome == "Encanto Caro" })
    }

    @Test
    fun `progressao XP completa a arvore prioritaria antes de abrir outra`() {
        val a1 = EncantoSolarDefinition("a1", "Armas Brancas", "A1", "A1", "1", "Armas Brancas •", 1, 1, "Encanto", "", "Permanente", "", "")
        val a2 = EncantoSolarDefinition("a2", "Armas Brancas", "A2", "A2", "1", "Armas Brancas •", 1, 1, "Encanto", "", "Permanente", "A1", "")
        val b1 = EncantoSolarDefinition("b1", "Presença", "B1", "B1", "1", "Presença •", 1, 1, "Encanto", "", "Permanente", "", "")
        val antes = npcBase("Armas Brancas", 1).copy(
            habilidadeSupernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Armas Brancas", "Presença"),
            abilities = mapOf("Armas Brancas" to 1, "Presença" to 1),
            xpAtual = 11
        )
        val depois = EncounterExperienceService.expand(
            npc = antes,
            catalogo = listOf(a1, a2, b1),
            ordemHabilidades = listOf("Armas Brancas", "Presença"),
            custoEncanto = { 8 },
            trilhaVitalidade = EncounterGenerator::trilhaVitalidadePorVigor
        )
        assertTrue(depois.charms.any { it.nome == "A1" })
        assertTrue(depois.charms.any { it.nome == "A2" })
        assertTrue(depois.charms.none { it.nome == "B1" })
    }
    @Test
    fun `encanto de habilidade de Casta usa custo reduzido mesmo sem ser Favorecida`() {
        val encanto = EncantoSolarDefinition(
            id = "xp-casta", habilidade = "Resistência", nome = "Encanto de Casta",
            nomeIngles = "Caste Charm", custo = "1", minsTexto = "Resistência •",
            minHabilidade = 1, minEssencia = 1, tipo = "Encanto", palavrasChave = "",
            duracao = "Permanente", preRequisitos = "", descricao = ""
        )
        val antes = npcBase(habilidade = "Resistência", nivel = 1).copy(
            casta = com.example.model.Casta.Dawn.displayName,
            habilidadesFavorecidas = listOf("Armas Brancas")
        )
        val lote1 = EncounterGenerator.expandirEncantosPorExperiencia(antes, listOf(encanto))
        val lote2 = EncounterGenerator.expandirEncantosPorExperiencia(lote1, listOf(encanto))
        assertTrue(lote2.charms.any { it.nome == "Encanto de Casta" })
        assertEquals(8, lote2.historicoXpBatches.last().xpGasto)
    }

    @Test
    fun `expansor Solar preparado preserva exatamente a progressao direta`() {
        val catalogo = listOf(
            EncantoSolarDefinition("eq-a1", "Armas Brancas", "EQ A1", "EQ A1", "1", "Armas Brancas •", 1, 1, "Encanto", "", "Permanente", "", ""),
            EncantoSolarDefinition("eq-a2", "Armas Brancas", "EQ A2", "EQ A2", "1", "Armas Brancas •", 1, 1, "Encanto", "", "Permanente", "EQ A1", ""),
            EncantoSolarDefinition("eq-b1", "Presença", "EQ B1", "EQ B1", "1", "Presença •", 1, 1, "Encanto", "", "Permanente", "", "")
        )
        val antes = npcBase("Armas Brancas", 2).copy(
            habilidadeSupernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Presença"),
            abilities = mapOf("Armas Brancas" to 2, "Presença" to 2),
            xpAtual = 11
        )
        val direto = SolarEncounterGenerator.expandirEncantosPorExperiencia(antes, catalogo)
        val preparado = SolarEncounterGenerator.criarExpansorXpPreparado(catalogo).expand(antes).npcResultante
        assertEquals(direto, preparado)
    }

    @Test
    fun `expansor Sangue de Dragao preparado preserva Signature e progressao direta`() {
        val catalogo = listOf(
            EncantoSolarDefinition("db-eq-a", "Armas Brancas", "DB EQ A", "DB EQ A", "1", "Armas Brancas •", 1, 1, "Encanto", "Assinatura (Fogo)", "Permanente", "", ""),
            EncantoSolarDefinition("db-eq-b", "Armas Brancas", "DB EQ B", "DB EQ B", "1", "Armas Brancas •", 1, 1, "Encanto", "Assinatura (Água)", "Permanente", "", "")
        )
        val antes = npcBase("Armas Brancas", 3).copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            casta = com.example.model.Aspecto.Fogo.displayName,
            habilidadesFavorecidas = listOf("Armas Brancas"),
            abilities = mapOf("Armas Brancas" to 3),
            xpAtual = 11
        )
        val filtrado = DragonBloodedSignaturePolicy.filterForProgression(
            catalog = catalogo,
            selectedNames = antes.charms.map { it.nome }.toSet(),
            essence = antes.essencia,
            aspecto = antes.casta,
            favorecidas = antes.habilidadesFavorecidas
        )
        val direto = EncounterExperienceService.expandWithBatch(
            npc = antes,
            catalogo = filtrado,
            ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(antes),
            custoEncanto = { 8 },
            trilhaVitalidade = DragonBloodedEncounterGenerator::trilhaVitalidadeSangueDeDragaoPorVigor,
            habilidadeCombate = antes.habilidadePrincipal
        ).npcResultante
        val preparado = DragonBloodedEncounterGenerator
            .criarExpansorXpComCatalogoConvertido(catalogo, antes)
            .expand(antes)
            .npcResultante
        assertEquals(direto, preparado)
    }

    @Test
    fun `expansor Solar preparado permanece equivalente em varios lotes`() {
        val catalogo = listOf(
            EncantoSolarDefinition("multi-a1", "Armas Brancas", "Multi A1", "Multi A1", "1", "Armas Brancas •", 1, 1, "Encanto", "", "Permanente", "", ""),
            EncantoSolarDefinition("multi-a2", "Armas Brancas", "Multi A2", "Multi A2", "1", "Armas Brancas •", 1, 1, "Encanto", "", "Permanente", "Multi A1", ""),
            EncantoSolarDefinition("multi-p1", "Presença", "Multi P1", "Multi P1", "1", "Presença •", 1, 1, "Encanto", "", "Permanente", "", "")
        )
        val inicial = npcBase("Armas Brancas", 3).copy(
            casta = com.example.model.Casta.Dawn.displayName,
            habilidadeSupernal = "Armas Brancas",
            habilidadesFavorecidas = listOf("Presença"),
            abilities = mapOf("Armas Brancas" to 3, "Presença" to 3)
        )
        val expansor = SolarEncounterGenerator.criarExpansorXpPreparado(catalogo)
        var direto = inicial
        var preparado = inicial
        repeat(8) {
            direto = SolarEncounterGenerator.expandirEncantosPorExperiencia(direto, catalogo)
            preparado = expansor.expand(preparado).npcResultante
            assertEquals(direto, preparado)
        }
    }

    @Test
    fun `expansor Sangue de Dragao preparado permanece equivalente quando Signature muda entre lotes`() {
        val catalogo = listOf(
            EncantoSolarDefinition("db-multi-a", "Armas Brancas", "DB Multi A", "DB Multi A", "1", "Armas Brancas •", 1, 1, "Encanto", "Assinatura (Fogo)", "Permanente", "", ""),
            EncantoSolarDefinition("db-multi-b", "Armas Brancas", "DB Multi B", "DB Multi B", "1", "Armas Brancas •", 1, 1, "Encanto", "Assinatura (Água)", "Permanente", "", "")
        )
        val inicial = npcBase("Armas Brancas", 5).copy(
            tipoExaltado = com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            casta = com.example.model.Aspecto.Fogo.displayName,
            habilidadesFavorecidas = listOf("Armas Brancas"),
            abilities = mapOf("Armas Brancas" to 5)
        )
        val expansor = DragonBloodedEncounterGenerator.criarExpansorXpComCatalogoConvertido(catalogo, inicial)
        var direto = inicial
        var preparado = inicial
        repeat(8) {
            val filtrado = DragonBloodedSignaturePolicy.filterForProgression(
                catalog = catalogo,
                selectedNames = direto.charms.map { it.nome }.toSet(),
                essence = direto.essencia,
                aspecto = direto.casta,
                favorecidas = direto.habilidadesFavorecidas
            )
            direto = EncounterExperienceService.expandWithBatch(
                npc = direto,
                catalogo = filtrado,
                ordemHabilidades = EncounterRulePolicy.abilityPriorityFor(direto),
                custoEncanto = { 8 },
                trilhaVitalidade = DragonBloodedEncounterGenerator::trilhaVitalidadeSangueDeDragaoPorVigor,
                habilidadeCombate = direto.habilidadePrincipal
            ).npcResultante
            preparado = expansor.expand(preparado).npcResultante
            assertEquals(direto, preparado)
        }
    }


}
