package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.LunarCasta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LunarEncounterGeneratorTest {
    @Test
    fun `lunar fisico prioriza cadeia ofensiva de Essencia alta sobre volume raso`() {
        fun encanto(nome: String, atributo: String, essencia: Int, requisito: String) =
            EncantoLunarDefinition(
                id = nome, atributo = atributo, subdivisao = null,
                nome = nome, nomeIngles = "", custo = "1",
                minsTexto = "$atributo 3, Essência $essencia",
                minAtributo = 3, minEssencia = essencia,
                tipo = "Reflexivo", palavrasChave = "", duracao = "",
                preRequisitos = requisito, descricao = ""
            )
        val catalogo = listOf(
            encanto("Forca inicial", "Força", 1, "Nenhum"),
            encanto("Forca intermediaria", "Força", 2, "Forca inicial"),
            encanto("Forca superior", "Força", 3, "Forca intermediaria")
        ) + (1..5).map { encanto("Destreza rasa $it", "Destreza", 1, "Nenhum") }
        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = catalogo,
            attributes = mapOf("Força" to 4, "Destreza" to 4, "Vigor" to 3),
            essencia = 1,
            ordemAtributos = listOf("Destreza", "Força", "Vigor"),
            quantidade = 0,
            random = Random(42),
            arquetipo = ArquetipoEncontro.FISICO
        )
        assertEquals("Força", resultado.ataqueEscolhido)
    }

    @Test
    fun `lunar fisico nao valoriza encanto alto com prerequisito inacessivel`() {
        fun encanto(nome: String, atributo: String, essencia: Int, requisito: String) =
            EncantoLunarDefinition(
                id = nome, atributo = atributo, subdivisao = null,
                nome = nome, nomeIngles = "", custo = "1",
                minsTexto = "$atributo 3, Essência $essencia",
                minAtributo = 3, minEssencia = essencia,
                tipo = "Reflexivo", palavrasChave = "", duracao = "",
                preRequisitos = requisito, descricao = ""
            )
        val catalogo = listOf(
            encanto("Forca bloqueada", "Força", 5, "Encanto inexistente"),
            encanto("Destreza inicial", "Destreza", 1, "Nenhum"),
            encanto("Destreza avancada", "Destreza", 2, "Destreza inicial")
        )
        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = catalogo,
            attributes = mapOf("Força" to 4, "Destreza" to 4, "Vigor" to 3),
            essencia = 1,
            ordemAtributos = listOf("Força", "Destreza", "Vigor"),
            quantidade = 0,
            random = Random(42),
            arquetipo = ArquetipoEncontro.FISICO
        )
        assertEquals("Destreza", resultado.ataqueEscolhido)
    }

    @Test
    fun `foco ofensivo explicito prevalece sobre pontuacao automatica`() {
        val resultado = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = emptyList(),
            attributes = mapOf("Força" to 5, "Destreza" to 3, "Vigor" to 3),
            essencia = 1,
            ordemAtributos = listOf("Força", "Destreza", "Vigor"),
            quantidade = 0,
            random = Random(7),
            arquetipo = ArquetipoEncontro.FISICO,
            atributoFocoUsuario = "Destreza"
        )
        assertEquals("Destreza", resultado.ataqueEscolhido)
    }

    @Test
    fun `projecao Lunar considera rota arquetipo somente com traco espiritual habilitado`() {
        fun encanto(nome: String, atributo: String, essencia: Int, pre: String = "Nenhum") =
            EncantoLunarDefinition(
                id = nome, atributo = atributo, subdivisao = null,
                nome = nome, nomeIngles = "", custo = "1",
                minsTexto = "$atributo 3, Essência $essencia",
                minAtributo = 3, minEssencia = essencia,
                tipo = "Reflexivo", palavrasChave = "", duracao = "",
                preRequisitos = pre, descricao = ""
            )
        val base = encanto("Base destreza", "Destreza", 1)
        val avancado = encanto("Avancado destreza", "Destreza", 3, "Base destreza")
        val alternativo = encanto("Encanto de Percepcao", "Percepção", 5).copy(
            rotasArquetipo = listOf(
                LunarCharmArchetypeRoute("Destreza", "VISAO_NOTURNA", 3, "Avancado destreza")
            )
        )
        val catalogo = listOf(encanto("Base forca", "Força", 4), base, avancado, alternativo)
        fun escolher(tracos: Set<LunarSpiritTrait>) =
            LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
                catalogo = catalogo,
                attributes = mapOf("Força" to 4, "Destreza" to 4, "Percepção" to 4, "Vigor" to 3),
                essencia = 1,
                ordemAtributos = listOf("Força", "Destreza", "Vigor"),
                quantidade = 0,
                random = Random(7),
                arquetipo = ArquetipoEncontro.FISICO,
                spiritTraits = tracos
            ).ataqueEscolhido
        assertEquals("Destreza", escolher(setOf(LunarSpiritTrait.VISAO_NOTURNA)))
        assertEquals("Força", escolher(emptySet()))
    }

    @Test
    fun `lunar fisico limita Corpo de Touro inicial a uma aquisicao`() {
        val corpo = EncantoLunarDefinition(
            id = "corpo-touro", atributo = "Vigor", subdivisao = null,
            nome = com.example.model.NOME_CORPO_DE_TOURO, nomeIngles = "",
            custo = "1", minsTexto = "Vigor 1, Essência 1",
            minAtributo = 1, minEssencia = 1, tipo = "Permanente",
            palavrasChave = "", duracao = "Permanente",
            preRequisitos = "Nenhum", descricao = ""
        )
        val selecionados = LunarEncounterCharmSelection.selecionarEncantosIniciaisComRotas(
            catalogo = listOf(corpo),
            attributes = mapOf("Força" to 4, "Destreza" to 3, "Vigor" to 5, "Inteligência" to 2),
            essencia = 1,
            ordemAtributos = listOf("Vigor", "Força", "Destreza"),
            quantidade = 15,
            random = Random(17),
            arquetipo = ArquetipoEncontro.FISICO
        ).charms
        assertEquals(1, selecionados.count { it.nome == com.example.model.NOME_CORPO_DE_TOURO })
    }

    @Test
    fun `geracao Lunar fisica registra arvore ofensiva mesmo sem encantos disponiveis`() {
        repeat(EncounterTestSamples.count(30)) { seed ->
            val npc = EncounterGenerator.gerarLunar(
                nomeManual = "Lunar ofensivo",
                arquetipo = ArquetipoEncontro.FISICO,
                encantosLunares = emptyList(),
                random = Random(seed)
            )
            assertTrue(npc.lunarAtaqueEscolhido == "Força" || npc.lunarAtaqueEscolhido == "Destreza")
            assertEquals(null, npc.focoProgressaoExplicito)
            assertEquals(npc.lunarAtaqueEscolhido, npc.copy(xpAtual = 15).lunarAtaqueEscolhido)
        }
    }

    @Test
    fun `geracao Lunar nao fisica nao inventa arvore ofensiva`() {
        listOf(ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL).forEach { arquetipo ->
            val npc = EncounterGenerator.gerarLunar(
                nomeManual = "Lunar nao fisico",
                arquetipo = arquetipo,
                encantosLunares = emptyList(),
                random = Random(42)
            )
            assertEquals(null, npc.lunarAtaqueEscolhido)
        }
    }

    @Test
    fun `xp lunar preserva saldo e exclusividade ofensiva na progressao real`() {
        val catalogo = (1..8).map { indice ->
            EncantoLunarDefinition(
                id = "forca-$indice", atributo = "Força", subdivisao = null,
                nome = "Forca XP $indice", nomeIngles = "", custo = "1",
                minsTexto = "Força 3, Essência 1", minAtributo = 3, minEssencia = 1,
                tipo = "Reflexivo", palavrasChave = "", duracao = "",
                preRequisitos = if (indice == 1) "Nenhum" else "Forca XP ${indice - 1}",
                descricao = ""
            )
        } + (1..8).map { indice ->
            EncantoLunarDefinition(
                id = "destreza-$indice", atributo = "Destreza", subdivisao = null,
                nome = "Destreza XP $indice", nomeIngles = "", custo = "1",
                minsTexto = "Destreza 3, Essência 1", minAtributo = 3, minEssencia = 1,
                tipo = "Reflexivo", palavrasChave = "", duracao = "",
                preRequisitos = if (indice == 1) "Nenhum" else "Destreza XP ${indice - 1}",
                descricao = ""
            )
        }
        val inicial = EncounterGenerator.gerarLunar(
            nomeManual = "Lunar progressao XP",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosLunares = emptyList(),
            random = Random(81)
        )
        val npc = inicial.copy(
            charms = emptyList(),
            attributes = inicial.attributes + mapOf("Força" to 4, "Destreza" to 4),
            lunarAtaqueEscolhido = "Força",
            focoProgressaoExplicito = "Força"
        )
        val expandido = EncounterExperienceLunar.expandLunarWithBatch(
            npc, catalogo, listOf("Força", "Destreza", "Vigor")
        )
        val depois = expandido.npcResultante
        assertEquals("Força", depois.lunarAtaqueEscolhido)
        assertTrue("O cenário deve comprar ao menos um Encanto", depois.charms.isNotEmpty())
        assertTrue("A progressão deve iniciar pela raiz ofensiva", depois.charms.any { it.nome == "Forca XP 1" })
        assertTrue(depois.charms.none { it.habilidadeVinculada.equals("Destreza", ignoreCase = true) })
        assertEquals(
            npc.xpAtual + EncounterExperienceService.XP_POR_CHAMADA,
            depois.xpAtual + (depois.xpGastoTotal - npc.xpGastoTotal)
        )
        assertEquals(depois.xpGastoTotal - npc.xpGastoTotal, expandido.batchAplicado.xpGasto)
        assertEquals(
            depois.charms.map { it.nome },
            expandido.batchAplicado.nomesEncantosAdicionados
        )
        assertEquals(null, expandido.batchAplicado.habilidadeMelhorada)
        assertEquals(0, expandido.batchAplicado.pontosGanhosNaHabilidade)
        assertEquals(null, expandido.batchAplicado.especializacaoAdicionada)
        assertEquals(npc.attributes, depois.attributes)
        assertEquals(npc.especialidades, depois.especialidades)
        val adquiridos = depois.charms.map { it.nome }.toSet()
        val segundaExpansao = EncounterExperienceLunar.expandLunarWithBatch(
            depois, catalogo, listOf("Força", "Destreza", "Vigor")
        )
        val segundo = segundaExpansao.npcResultante
        assertEquals("Força", segundo.lunarAtaqueEscolhido)
        assertTrue(segundo.charms.none { it.habilidadeVinculada.equals("Destreza", ignoreCase = true) })
        assertEquals(
            depois.xpAtual + EncounterExperienceService.XP_POR_CHAMADA,
            segundo.xpAtual + (segundo.xpGastoTotal - depois.xpGastoTotal)
        )
        assertEquals(segundo.xpGastoTotal - depois.xpGastoTotal, segundaExpansao.batchAplicado.xpGasto)
        assertEquals(
            segundo.charms.drop(depois.charms.size).map { it.nome },
            segundaExpansao.batchAplicado.nomesEncantosAdicionados
        )
        assertEquals(null, segundaExpansao.batchAplicado.habilidadeMelhorada)
        assertEquals(0, segundaExpansao.batchAplicado.pontosGanhosNaHabilidade)
        assertEquals(null, segundaExpansao.batchAplicado.especializacaoAdicionada)
        assertEquals(depois.attributes, segundo.attributes)
        assertEquals(depois.especialidades, segundo.especialidades)
        assertEquals(2, segundo.historicoXpBatches.size - npc.historicoXpBatches.size)
        assertEquals(
            expandido.batchAplicado.xpGasto + segundaExpansao.batchAplicado.xpGasto,
            segundo.xpGastoTotal - npc.xpGastoTotal
        )
        assertTrue("Nenhum lote pode gastar XP negativo",
            expandido.batchAplicado.xpGasto >= 0 && segundaExpansao.batchAplicado.xpGasto >= 0)
        assertTrue("O saldo de XP não pode ficar negativo", segundo.xpAtual >= 0)

        assertTrue("A progressão não deve remover Encantos anteriores",
            segundo.charms.map { it.nome }.containsAll(adquiridos))
        val revertidoUm = EncounterExperienceLunar.reduceLunar(segundo)
        assertEquals("Força", revertidoUm.lunarAtaqueEscolhido)
        assertEquals(depois.xpAtual, revertidoUm.xpAtual)
        assertEquals(depois.xpGastoTotal, revertidoUm.xpGastoTotal)
        assertEquals(depois.charms.map { it.nome }, revertidoUm.charms.map { it.nome })
        assertEquals(depois.historicoXpBatches.size, revertidoUm.historicoXpBatches.size)
        assertEquals(depois.attributes, revertidoUm.attributes)
        assertEquals(depois.corpoDeTouroCount, revertidoUm.corpoDeTouroCount)
        assertEquals(depois.essencia, revertidoUm.essencia)
        assertEquals(depois.formaEspiritualSecundaria, revertidoUm.formaEspiritualSecundaria)
        val revertidoDois = EncounterExperienceLunar.reduceLunar(revertidoUm)
        assertEquals(npc.xpAtual, revertidoDois.xpAtual)
        assertEquals(npc.xpGastoTotal, revertidoDois.xpGastoTotal)
        assertEquals(npc.charms.map { it.nome }, revertidoDois.charms.map { it.nome })
        assertEquals(npc.historicoXpBatches.size, revertidoDois.historicoXpBatches.size)
        assertEquals(npc.attributes, revertidoDois.attributes)
        assertEquals(npc.corpoDeTouroCount, revertidoDois.corpoDeTouroCount)
        assertEquals(npc.essencia, revertidoDois.essencia)
        assertEquals(npc.formaEspiritualSecundaria, revertidoDois.formaEspiritualSecundaria)

        assertEquals(
            "A expansão não deve comprar o mesmo Encanto mais de uma vez",
            segundo.charms.size,
            segundo.charms.map { it.nome }.distinct().size
        )
        assertEquals(
            "O histórico dos dois lotes não deve repetir Encantos",
            (expandido.batchAplicado.nomesEncantosAdicionados +
                segundaExpansao.batchAplicado.nomesEncantosAdicionados).size,
            (expandido.batchAplicado.nomesEncantosAdicionados +
                segundaExpansao.batchAplicado.nomesEncantosAdicionados).distinct().size
        )
        assertEquals(
            "Reverter sem histórico não deve alterar a ficha",
            revertidoDois,
            EncounterExperienceLunar.reduceLunar(revertidoDois)
        )

        val historicoAdicionado = segundo.historicoXpBatches.drop(npc.historicoXpBatches.size)
        assertEquals(
            "O primeiro lote deve estar persistido no histórico",
            expandido.batchAplicado,
            historicoAdicionado[0]
        )
        assertEquals(
            "O segundo lote deve estar persistido no histórico",
            segundaExpansao.batchAplicado,
            historicoAdicionado[1]
        )
        assertEquals(
            "Reverter um lote deve preservar exatamente o histórico anterior",
            depois.historicoXpBatches,
            revertidoUm.historicoXpBatches
        )
        assertEquals(
            "Reverter ambos os lotes deve recuperar o histórico original",
            npc.historicoXpBatches,
            revertidoDois.historicoXpBatches
        )

        val nomesNaOrdem = segundo.charms.map { it.nome }
        nomesNaOrdem.forEachIndexed { posicao, nome ->
            val indice = nome.removePrefix("Forca XP ").toIntOrNull() ?: return@forEachIndexed
            if (indice > 1) assertTrue(
                "Pré-requisito deve ser adquirido antes de $nome",
                nomesNaOrdem.indexOf("Forca XP ${indice - 1}") in 0 until posicao
            )
        }
        depois.charms.forEach { encanto ->
            val indice = encanto.nome.removePrefix("Forca XP ").toIntOrNull() ?: return@forEach
            if (indice > 1) assertTrue("Faltou requisito de ${encanto.nome}", "Forca XP ${indice - 1}" in adquiridos)
        }
    }

    @Test
    fun `xp lunar respeita Destreza como unica arvore ofensiva escolhida`() {
        fun raiz(atributo: String) = EncantoLunarDefinition(
            id = "raiz-$atributo", atributo = atributo, subdivisao = null,
            nome = "Raiz $atributo", nomeIngles = "", custo = "1",
            minsTexto = "$atributo 3, Essência 1", minAtributo = 3, minEssencia = 1,
            tipo = "Reflexivo", palavrasChave = "", duracao = "",
            preRequisitos = "Nenhum", descricao = ""
        )
        val inicial = EncounterGenerator.gerarLunar(
            nomeManual = "Lunar Destreza XP",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosLunares = emptyList(),
            random = Random(82)
        )
        val npc = inicial.copy(
            charms = emptyList(),
            attributes = inicial.attributes + mapOf("Força" to 4, "Destreza" to 4),
            lunarAtaqueEscolhido = "Destreza",
            focoProgressaoExplicito = "Destreza"
        )
        val resultado = EncounterExperienceLunar.expandLunarWithBatch(
            npc, listOf(raiz("Força"), raiz("Destreza")),
            listOf("Força", "Destreza", "Vigor")
        )
        assertEquals("Destreza", resultado.npcResultante.lunarAtaqueEscolhido)
        assertEquals(
            npc.xpAtual + EncounterExperienceService.XP_POR_CHAMADA,
            resultado.npcResultante.xpAtual +
                (resultado.npcResultante.xpGastoTotal - npc.xpGastoTotal)
        )
        assertEquals(
            resultado.npcResultante.xpGastoTotal - npc.xpGastoTotal,
            resultado.batchAplicado.xpGasto
        )
        assertEquals(npc.attributes, resultado.npcResultante.attributes)
        assertTrue(
            "A árvore ofensiva escolhida deve poder adquirir sua raiz",
            resultado.npcResultante.charms.any { it.nome == "Raiz Destreza" }
        )
        assertTrue(
            "Força deve permanecer excluída mesmo quando aparece primeiro na prioridade",
            resultado.npcResultante.charms.none { it.habilidadeVinculada.equals("Força", ignoreCase = true) }
        )
    }

    @Test
    fun `xp lunar limita Vigor a tres encantos acima da arvore ofensiva`() {
        fun encanto(atributo: String, indice: Int) = EncantoLunarDefinition(
            id = "$atributo-$indice", atributo = atributo, subdivisao = null,
            nome = "$atributo progressao $indice", nomeIngles = "", custo = "1",
            minsTexto = "$atributo 3, Essência 1", minAtributo = 3, minEssencia = 1,
            tipo = "Reflexivo", palavrasChave = "", duracao = "",
            preRequisitos = if (indice == 1) "Nenhum" else "$atributo progressao ${indice - 1}",
            descricao = ""
        )
        val catalogo = (1..12).map { encanto("Vigor", it) } +
            (1..12).map { encanto("Força", it) }
        val inicial = EncounterGenerator.gerarLunar(
            nomeManual = "Lunar equilibrio Vigor",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosLunares = emptyList(),
            random = Random(83)
        )
        val npc = inicial.copy(
            charms = emptyList(),
            attributes = inicial.attributes + mapOf("Força" to 4, "Vigor" to 4),
            lunarAtaqueEscolhido = "Força",
            focoProgressaoExplicito = "Força"
        )
        val evoluido = EncounterExperienceLunar.expandLunarRepeated(
            npc, catalogo, listOf("Vigor", "Força"), 12
        )
        val quantidadeVigor = evoluido.charms.count {
            it.habilidadeVinculada.equals("Vigor", ignoreCase = true)
        }
        val quantidadeForca = evoluido.charms.count {
            it.habilidadeVinculada.equals("Força", ignoreCase = true)
        }
        assertTrue("O cenário deve adquirir Encantos de ambas as árvores",
            quantidadeVigor > 0 && quantidadeForca > 0)
        assertTrue("Vigor não pode ultrapassar o ataque escolhido em mais de três Encantos",
            quantidadeVigor <= quantidadeForca + 3)
        assertTrue(evoluido.charms.none {
            it.habilidadeVinculada.equals("Destreza", ignoreCase = true)
        })
        assertEquals("Força", evoluido.lunarAtaqueEscolhido)
        assertEquals(12, evoluido.historicoXpBatches.size - npc.historicoXpBatches.size)
        assertEquals(
            npc.xpAtual + 12 * EncounterExperienceService.XP_POR_CHAMADA,
            evoluido.xpAtual + (evoluido.xpGastoTotal - npc.xpGastoTotal)
        )
        assertEquals(npc.attributes, evoluido.attributes)
        assertTrue("O XP disponível deve permanecer não negativo", evoluido.xpAtual >= 0)

        // O expansor otimizado deve produzir o mesmo resultado da aplicação
        // sequencial de lotes independentes, inclusive no histórico.
        var sequencial = npc
        repeat(12) {
            sequencial = EncounterExperienceLunar.expandLunarWithBatch(
                sequencial, catalogo, listOf("Vigor", "Força")
            ).npcResultante
        }
        assertEquals(sequencial.charms, evoluido.charms)
        assertEquals(sequencial.xpAtual, evoluido.xpAtual)
        assertEquals(sequencial.xpGastoTotal, evoluido.xpGastoTotal)
        assertEquals(sequencial.historicoXpBatches, evoluido.historicoXpBatches)
        assertEquals(sequencial.lunarAtaqueEscolhido, evoluido.lunarAtaqueEscolhido)

        var revertido = evoluido
        repeat(12) {
            val historicoAnterior = revertido.historicoXpBatches.size
            revertido = EncounterExperienceLunar.reduceLunar(revertido)
            assertEquals(historicoAnterior - 1, revertido.historicoXpBatches.size)
        }
        assertEquals(npc.charms, revertido.charms)
        assertEquals(npc.attributes, revertido.attributes)
        assertEquals(npc.xpAtual, revertido.xpAtual)
        assertEquals(npc.xpGastoTotal, revertido.xpGastoTotal)
        assertEquals(npc.historicoXpBatches, revertido.historicoXpBatches)
        assertEquals(npc.lunarAtaqueEscolhido, revertido.lunarAtaqueEscolhido)
        assertEquals(npc.corpoDeTouroCount, revertido.corpoDeTouroCount)
        assertEquals(npc.essencia, revertido.essencia)
        assertEquals(npc.formaEspiritualSecundaria, revertido.formaEspiritualSecundaria)
        assertEquals(npc.lunarArchetypeTraits, revertido.lunarArchetypeTraits)
        assertEquals(
            "A reversão integral deve ser idempotente quando não há lotes restantes",
            revertido, EncounterExperienceLunar.reduceLunar(revertido)
        )
    }

    @Test
    fun `Lunar possui dois atributos de casta e dois favorecidos adicionais distintos`() {
        ArquetipoEncontro.entries.forEach { arquetipo ->
            repeat(EncounterTestSamples.count(100)) { seed ->
                val npc = EncounterGenerator.gerarLunar(
                    nomeManual = "Teste",
                    arquetipo = arquetipo,
                    encantosLunares = emptyList(),
                    random = Random(seed + arquetipo.ordinal * 1000)
                )

                assertEquals(2, npc.lunarAtributosCasta.size)
                assertEquals(2, npc.lunarAtributosCasta.distinct().size)
                assertEquals(2, npc.habilidadesFavorecidas.size)
                assertEquals(2, npc.habilidadesFavorecidas.distinct().size)
                assertTrue(npc.lunarAtributosCasta.toSet().intersect(npc.habilidadesFavorecidas.toSet()).isEmpty())
                assertEquals(4, (npc.lunarAtributosCasta + npc.habilidadesFavorecidas).distinct().size)

                val casta = LunarCasta.entries.first { it.displayName == npc.casta }
                assertTrue(npc.lunarAtributosCasta.all { it in casta.poolAtributosCasta() })
                assertTrue(npc.habilidadesFavorecidas.all { it !in npc.lunarAtributosCasta.toSet() })
            }
        }
    }
}
