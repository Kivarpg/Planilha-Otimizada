package com.example.data

import com.example.model.EncantoEncontro
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class EncounterNpcSpellManagementTest {
    private val terrestre = FeiticoDefinition("1", "Teste", "Test", "Terrestre", "5mf", "", "", "", "")

    private fun feiticeiro() = NpcEncontro(
        charms = listOf(EncantoEncontro(nome = "Feitiçaria do Círculo Terrestre", habilidadeVinculada = "Ocultismo", custo = ""))
    )

    @Test fun `adiciona e remove feitico de circulo desbloqueado sem duplicar`() {
        val base = feiticeiro()
        val adicionado = EncounterNpcSpellManagement.atualizarInicial(base, terrestre, true)
        assertEquals(listOf("Teste"), adicionado.feiticos.map { it.nome })
        assertEquals("Teste", adicionado.feiticoInicialNome)
        assertSame(adicionado, EncounterNpcSpellManagement.atualizarInicial(adicionado, terrestre, true))
        val removido = EncounterNpcSpellManagement.atualizarInicial(adicionado, terrestre, false)
        assertEquals(emptyList(), removido.feiticos)
        assertEquals(null, removido.feiticoInicialNome)
    }

    @Test fun `nao permite administrar feitico de circulo bloqueado`() {
        val base = NpcEncontro()
        assertSame(base, EncounterNpcSpellManagement.atualizarInicial(base, terrestre, true))
    }

    @Test fun `lunar nunca desbloqueia circulo solar mesmo com encanto invalido persistido`() {
        val lunar = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            charms = listOf(
                EncantoEncontro(nome = "Feitiçaria do Círculo Terrestre", habilidadeVinculada = "Ocultismo", custo = ""),
                EncantoEncontro(nome = "Feitiçaria do Círculo Celestial", habilidadeVinculada = "Ocultismo", custo = ""),
                EncantoEncontro(nome = "Feitiçaria do Círculo Solar", habilidadeVinculada = "Ocultismo", custo = "")
            )
        )
        assertEquals(setOf("Terrestre", "Celestial"), EncounterNpcSpellManagement.circulosDesbloqueados(lunar))
    }

    @Test fun `sangue de dragao nunca desbloqueia circulos acima do terrestre`() {
        val dragon = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            charms = listOf(
                EncantoEncontro(nome = "Feitiçaria do Círculo Terrestre", habilidadeVinculada = "Ocultismo", custo = ""),
                EncantoEncontro(nome = "Feitiçaria do Círculo Celestial", habilidadeVinculada = "Ocultismo", custo = "")
            )
        )
        assertEquals(setOf("Terrestre"), EncounterNpcSpellManagement.circulosDesbloqueados(dragon))
    }

    @Test fun `circulos sao detectados com acentos caixa e espacos equivalentes`() {
        val solar = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            charms = listOf(
                EncantoEncontro("  FEITICARIA DO CIRCULO TERRESTRE ", "Ocultismo", ""),
                EncantoEncontro("Feitiçaria do Círculo Celestial", "Ocultismo", ""),
                EncantoEncontro("Feitiçaria do Círculo Solar", "Ocultismo", "")
            )
        )
        assertEquals(
            setOf("Terrestre", "Celestial", "Solar"),
            EncounterNpcSpellManagement.circulosDesbloqueados(solar)
        )
    }

    @Test fun `normalizacao nao permite circulo solar para lunar ou sangue de dragao`() {
        val charms = listOf(
            EncantoEncontro(" FEITICARIA DO CIRCULO TERRESTRE ", "Ocultismo", ""),
            EncantoEncontro(" FEITICARIA DO CIRCULO CELESTIAL ", "Ocultismo", ""),
            EncantoEncontro(" FEITICARIA DO CIRCULO SOLAR ", "Ocultismo", "")
        )
        val lunar = NpcEncontro(tipoExaltado = TipoExaltadoEncontro.LUNAR, charms = charms)
        val dragao = NpcEncontro(tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO, charms = charms)
        assertEquals(setOf("Terrestre", "Celestial"), EncounterNpcSpellManagement.circulosDesbloqueados(lunar))
        assertEquals(setOf("Terrestre"), EncounterNpcSpellManagement.circulosDesbloqueados(dragao))
    }

    @Test fun `primeiro xp bloqueia alteracao gratuita mesmo se saldo voltar a zero`() {
        val bloqueado = feiticeiro().copy(primeiroXpRecebido = true, xpAtual = 0, xpGastoTotal = 0)
        assertSame(bloqueado, EncounterNpcSpellManagement.atualizarInicial(bloqueado, terrestre, true))
    }

    @Test fun `trocar feitico inicial preserva todos os outros feiticos do npc`() {
        val outro = FeiticoDefinition("2", "Outro", "Other", "Terrestre", "5mf", "", "", "", "")
        val extra = com.example.model.FeiticoEncontro("Extra", "Terrestre", "5mf")
        val primeiro = EncounterNpcSpellManagement.atualizarInicial(
            feiticeiro().copy(feiticos = listOf(extra)), terrestre, true
        )
        val segundo = EncounterNpcSpellManagement.atualizarInicial(primeiro, outro, true)
        assertEquals(setOf("Extra", "Outro"), segundo.feiticos.map { it.nome }.toSet())
        assertEquals("Outro", segundo.feiticoInicialNome)
    }

    @Test fun `marcar como inicial um feitico ja existente nao duplica nem remove os demais`() {
        val extra = com.example.model.FeiticoEncontro("Teste", "Terrestre", "5mf")
        val outro = com.example.model.FeiticoEncontro("Outro", "Terrestre", "5mf")
        val base = feiticeiro().copy(feiticos = listOf(extra, outro))
        val resultado = EncounterNpcSpellManagement.atualizarInicial(base, terrestre, true)
        assertEquals(listOf("Teste", "Outro"), resultado.feiticos.map { it.nome })
        assertEquals("Teste", resultado.feiticoInicialNome)
    }

    @Test fun `sorteio do feitico inicial e reproduzivel com a mesma semente`() {
        val outro = FeiticoDefinition("2", "Outro", "Other", "Terrestre", "5mf", "", "", "", "")
        val catalogo = listOf(terrestre, outro)
        val primeiro = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            feiticeiro(), catalogo, kotlin.random.Random(42)
        )
        val segundo = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            feiticeiro(), catalogo, kotlin.random.Random(42)
        )
        assertEquals(primeiro.feiticos, segundo.feiticos)
        assertEquals(primeiro.feiticoInicialNome, segundo.feiticoInicialNome)
        assertEquals(1, primeiro.feiticos.size)
        assertEquals(primeiro.feiticos.single().nome, primeiro.feiticoInicialNome)
    }

    @Test fun `sorteio gratuito ignora nomes equivalentes ja possuidos`() {
        val outro = FeiticoDefinition("2", "Outro", "Other", "Terrestre", "5mf", "", "", "", "")
        val existente = com.example.model.FeiticoEncontro("teste", "Terrestre", "5mf")
        val npc = feiticeiro().copy(feiticos = listOf(existente))
        val resultado = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            npc, listOf(terrestre, outro), kotlin.random.Random(42)
        )
        assertEquals("Outro", resultado.feiticoInicialNome)
        assertEquals(listOf("teste", "Outro"), resultado.feiticos.map { it.nome })
    }

    @Test fun `sorteio gratuito com todos os feiticos existentes nao duplica`() {
        val outro = FeiticoDefinition("2", "Outro", "Other", "Terrestre", "5mf", "", "", "", "")
        val npc = feiticeiro().copy(feiticos = listOf(
            com.example.model.FeiticoEncontro("teste", "Terrestre", "5mf"),
            com.example.model.FeiticoEncontro("outro", "Terrestre", "5mf")
        ))
        val resultado = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            npc, listOf(terrestre, outro), kotlin.random.Random(42)
        )
        assertEquals(npc.feiticos, resultado.feiticos)
        assertEquals(true, resultado.feiticoInicialNome in setOf("Teste", "Outro"))
    }

    @Test fun `fallback preserva primeira definicao de nome equivalente no catalogo`() {
        val primeira = FeiticoDefinition("1", "Teste", "Test", "Terrestre", "5mf", "", "", "", "")
        val duplicada = FeiticoDefinition("2", "TESTE", "Test", "Terrestre", "9mf", "", "", "", "")
        val npc = feiticeiro().copy(
            feiticos = listOf(com.example.model.FeiticoEncontro("teste", "Terrestre", "5mf"))
        )
        val resultado = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            npc, listOf(primeira, duplicada), kotlin.random.Random(4)
        )
        assertEquals("Teste", resultado.feiticoInicialNome)
        assertEquals(npc.feiticos, resultado.feiticos)
    }

    @Test fun `nome inicial existente em outro circulo nao valida vaga terrestre`() {
        val celestial = com.example.model.FeiticoEncontro("Teste", "Celestial", "10mf")
        val base = feiticeiro().copy(
            feiticos = listOf(celestial),
            feiticoInicialNome = "Teste"
        )
        val resultado = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            base, listOf(terrestre), kotlin.random.Random(7)
        )
        assertEquals(listOf(celestial, com.example.model.FeiticoEncontro("Teste", "Terrestre", "5mf")), resultado.feiticos)
        assertEquals("Teste", resultado.feiticoInicialNome)
    }

    @Test fun `geracao de feitico inicial e idempotente`() {
        val inicial = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            feiticeiro(), listOf(terrestre), kotlin.random.Random(7)
        )
        val repetido = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            inicial, listOf(terrestre), kotlin.random.Random(8)
        )
        assertSame(inicial, repetido)
    }

    @Test fun `npc sem encanto de circulo nao recebe feitico inicial`() {
        val base = NpcEncontro()
        assertSame(
            base,
            EncounterNpcSpellManagement.garantirInicialNaCriacao(
                base, listOf(terrestre), kotlin.random.Random(7)
            )
        )
        assertEquals(emptySet<String>(), EncounterNpcSpellManagement.circulosDesbloqueados(base))
    }

    @Test fun `apos primeiro xp nao e possivel substituir feitico inicial`() {
        val inicial = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            feiticeiro(), listOf(terrestre), kotlin.random.Random(7)
        )
        val bloqueado = inicial.copy(primeiroXpRecebido = true)
        val outro = FeiticoDefinition("2", "Outro", "Other", "Terrestre", "5mf", "", "", "", "")
        assertSame(bloqueado, EncounterNpcSpellManagement.atualizarInicial(bloqueado, outro, true))
        assertEquals("Teste", bloqueado.feiticoInicialNome)
    }

    @Test fun `geracao materializa automaticamente feitico inicial terrestre ausente`() {
        val base = feiticeiro()
        val resultado = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            npc = base,
            catalogo = listOf(terrestre),
            random = kotlin.random.Random(7)
        )
        assertEquals(listOf("Teste"), resultado.feiticos.map { it.nome })
        assertEquals("Teste", resultado.feiticoInicialNome)
        assertEquals(true, EncounterNpcSpellManagement.podeGerenciarFeiticoInicial(resultado))
    }

    @Test fun `garantia de criacao nao reabre gerenciamento depois do primeiro xp`() {
        val bloqueado = feiticeiro().copy(primeiroXpRecebido = true)
        val resultado = EncounterNpcSpellManagement.garantirInicialNaCriacao(
            npc = bloqueado,
            catalogo = listOf(terrestre),
            random = kotlin.random.Random(7)
        )
        assertSame(bloqueado, resultado)
        assertEquals(false, EncounterNpcSpellManagement.podeGerenciarFeiticoInicial(resultado))
    }



    @Test fun `indexacao unica preserva sorteio do feitico inicial em varias sementes`() {
        val catalogo = listOf(
            terrestre,
            FeiticoDefinition("2", "Outro", "Other", "Terrestre", "5mf", "", "", "", ""),
            FeiticoDefinition("3", "Terceiro", "Third", "Terrestre", "5mf", "", "", "", ""),
            FeiticoDefinition("4", "Celestial", "Celestial", "Celestial", "5mf", "", "", "", ""),
            FeiticoDefinition("5", "Quarto", "Fourth", "Terrestre", "5mf", "", "", "", "")
        )
        val cenarios = listOf(
            feiticeiro(),
            feiticeiro().copy(feiticos = listOf(com.example.model.FeiticoEncontro("Teste", "Terrestre", "5mf"))),
            feiticeiro().copy(feiticos = listOf(com.example.model.FeiticoEncontro("Outro", "Terrestre", "5mf")))
        )
        for (npc in cenarios) {
            val possuidos = npc.feiticos.map { EncantosSolaresCatalog.normalize(it.nome) }.toSet()
            val terrestresAntigos = catalogo.filter { it.circulo == "Terrestre" }
            val candidatosAntigos = terrestresAntigos.filter {
                EncantosSolaresCatalog.normalize(it.nome) !in possuidos
            }
            repeat(200) { seed ->
                val esperado = candidatosAntigos.randomOrNull(kotlin.random.Random(seed))
                // Estes cenarios sempre tem pelo menos um candidato novo.
                val atual = EncounterNpcSpellManagement.garantirInicialNaCriacao(
                    npc, catalogo, kotlin.random.Random(seed)
                )
                assertEquals(esperado?.nome, atual.feiticoInicialNome, "seed=$seed")
            }
        }
    }


    @Test fun `duplicatas no catalogo nao distorcem sorteio do feitico inicial`() {
        val outro = FeiticoDefinition("2", "Outro", "Other", "Terrestre", "5mf", "", "", "", "")
        val repetido = FeiticoDefinition("3", " TESTE ", "Test", "Terrestre", "5mf", "", "", "", "")
        val catalogoUnico = listOf(terrestre, outro)
        val catalogoDuplicado = listOf(terrestre, repetido, outro, repetido)
        repeat(200) { seed ->
            val esperado = EncounterNpcSpellManagement.garantirInicialNaCriacao(
                feiticeiro(), catalogoUnico, kotlin.random.Random(seed)
            )
            val atual = EncounterNpcSpellManagement.garantirInicialNaCriacao(
                feiticeiro(), catalogoDuplicado, kotlin.random.Random(seed)
            )
            assertEquals(esperado.feiticoInicialNome, atual.feiticoInicialNome, "seed=$seed")
        }
    }

}
