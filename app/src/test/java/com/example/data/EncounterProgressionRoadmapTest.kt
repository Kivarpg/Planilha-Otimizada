package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import com.example.model.NpcEncontro
import com.example.model.EncounterProgressionRoadmap
import com.example.model.EspecialidadeEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterProgressionRoadmapTest {
    private fun npcBase() = NpcEncontro(
        arquetipo = ArquetipoEncontro.FISICO,
        habilidadePrincipal = "Armas Brancas",
        abilities = mapOf("Armas Brancas" to 1),
        healthBoxes = EncounterGenerator.trilhaVitalidadePorVigor(3, 0)
    )

    private fun catalogoSolarDeTeste(quantidade: Int = 20): List<EncantoSolarDefinition> =
        (1..quantidade).map { indice ->
            EncantoSolarDefinition(
                id = "roadmap_$indice",
                habilidade = "Armas Brancas",
                nome = "Encanto Roadmap $indice",
                nomeIngles = "Roadmap Charm $indice",
                custo = "5m",
                minsTexto = "Armas Brancas 1, Essência 1",
                minHabilidade = 1,
                minEssencia = 1,
                tipo = "Encanto",
                palavrasChave = "",
                duracao = "",
                preRequisitos = "Nenhum",
                descricao = ""
            )
        }

    private fun semRoadmap(npc: NpcEncontro) = npc

    @Test
    fun `solar multi lote com zero chamadas nao altera npc`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.SOLAR)
        assertEquals(
            base,
            EncounterExperienceSolar.expandRepeated(
                base,
                emptyList(),
                listOf("Armas Brancas"),
                { 10 },
                { vigor, corpo -> EncounterGenerator.trilhaVitalidadePorVigor(vigor, corpo) },
                0
            )
        )
    }

    @Test
    fun `sangue de dragao XP em lote preserva resultado da expansao direta`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO)
        val catalogo = emptyList<EncantoSangueDeDragaoDefinition>()
        val direto = DragonBloodedEncounterGenerator
            .expandirEncantosPorExperienciaSangueDeDragao(base, catalogo)
        val lote = DragonBloodedEncounterGenerator
            .expandirEncantosPorExperienciaSangueDeDragaoComBatch(base, catalogo)
        assertEquals(direto, lote.npcResultante)
    }

    @Test
    fun `solar multi lote preserva resultado de expansoes independentes`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.SOLAR)
        val catalogo = catalogoSolarDeTeste(3)
        val ordem = listOf("Armas Brancas")
        val custo: (String) -> Int = { 10 }
        val trilha: (Int, Int) -> List<com.example.model.CaixaVitalidade> =
            { vigor, corpo -> EncounterGenerator.trilhaVitalidadePorVigor(vigor, corpo) }
        val repetido = EncounterExperienceSolar.expandRepeated(
            base, catalogo, ordem, custo, trilha, 3
        )
        var individual = base
        repeat(3) {
            individual = EncounterExperienceSolar.expand(
                individual, catalogo, ordem, custo, trilha
            )
        }
        assertEquals(individual, repetido)
    }

    @Test
    fun `lunar multi lote preserva resultado de expansoes independentes`() {
        val ordem = listOf("Força", "Destreza", "Vigor")
        val catalogo = emptyList<EncantoLunarDefinition>()
        for (ataque in listOf("Força", "Destreza")) {
            val base = npcBase().copy(
                tipoExaltado = TipoExaltadoEncontro.LUNAR,
                lunarAtaqueEscolhido = ataque
            )
            val repetido = EncounterExperienceLunar.expandLunarRepeated(base, catalogo, ordem, 3)
            var individual = base
            repeat(3) {
                individual = EncounterExperienceLunar.expandLunar(individual, catalogo, ordem)
            }
            assertEquals(individual, repetido)
            assertEquals(ataque, repetido.lunarAtaqueEscolhido)
        }
    }

    @Test
    fun `lunar rollback de XP preserva ambas arvores ofensivas persistidas`() {
        for (ataque in listOf("Força", "Destreza")) {
            val base = npcBase().copy(
                tipoExaltado = TipoExaltadoEncontro.LUNAR,
                lunarAtaqueEscolhido = ataque
            )
            val evoluido = EncounterExperienceLunar.expandLunar(
                base, emptyList(), listOf("Força", "Destreza", "Vigor")
            )
            assertEquals(ataque, evoluido.lunarAtaqueEscolhido)
            assertEquals(base.historicoXpBatches.size + 1, evoluido.historicoXpBatches.size)
            val revertido = EncounterExperienceLunar.reduceLunar(evoluido)
            assertEquals(ataque, revertido.lunarAtaqueEscolhido)
            assertEquals(base.xpGastoTotal, revertido.xpGastoTotal)
            assertEquals(base.xpAtual, revertido.xpAtual)
            assertEquals(base.historicoXpBatches, revertido.historicoXpBatches)
            assertEquals(base.charms, revertido.charms)
        }
    }

    @Test
    fun `lunar multi lote com zero chamadas nao altera npc`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.LUNAR)
        assertEquals(
            base,
            EncounterExperienceLunar.expandLunarRepeated(
                base, emptyList(), listOf("Força"), 0
            )
        )
    }

    @Test
    fun `cancelamento do prefetch interrompe antes de preparar expansores`() {
        var verificacoes = 0
        val cancelamento = kotlinx.coroutines.CancellationException("prefetch obsoleto")
        try {
            EncounterProgressionRoadmapService.construir(
                npc = npcBase(),
                solarCatalogo = catalogoSolarDeTeste(),
                dragonCatalogo = emptyList(),
                lunarCatalogo = emptyList(),
                verificarCancelamento = {
                    verificacoes++
                    throw cancelamento
                }
            )
            org.junit.Assert.fail("O prefetch cancelado não pode produzir um roadmap")
        } catch (e: kotlinx.coroutines.CancellationException) {
            assertTrue(e === cancelamento)
        }
        assertEquals(1, verificacoes)
    }

    @Test
    fun `cancelamento antes do planejamento impede construir o roadmap`() {
        var verificacoes = 0
        val cancelamento = kotlinx.coroutines.CancellationException("planejamento invalidado")
        try {
            EncounterProgressionRoadmapService.construir(
                npc = npcBase(),
                solarCatalogo = emptyList(),
                dragonCatalogo = emptyList(),
                lunarCatalogo = emptyList(),
                catalogFingerprintPrecalculado = "catalogo-estavel",
                verificarCancelamento = {
                    verificacoes++
                    if (verificacoes == 2) throw cancelamento
                }
            )
            org.junit.Assert.fail("Deveria interromper antes de preparar o expansor")
        } catch (e: kotlinx.coroutines.CancellationException) {
            assertTrue(e === cancelamento)
        }
        assertEquals(2, verificacoes)
    }

    @Test
    fun `cancelamento entre expansoes nao devolve roadmap parcial`() {
        var verificacoes = 0
        val cancelamento = kotlinx.coroutines.CancellationException("cancelado entre passos")
        try {
            EncounterProgressionRoadmapService.construir(
                npc = npcBase(),
                solarCatalogo = emptyList(),
                dragonCatalogo = emptyList(),
                lunarCatalogo = emptyList(),
                catalogFingerprintPrecalculado = "catalogo-estavel",
                verificarCancelamento = {
                    verificacoes++
                    if (verificacoes == 5) throw cancelamento
                }
            )
            org.junit.Assert.fail("Roadmap parcial nao deve ser publicado")
        } catch (e: kotlinx.coroutines.CancellationException) {
            assertTrue(e === cancelamento)
        }
        assertEquals(5, verificacoes)
    }

    @Test
    fun `construcao explicita do roadmap usa uma janela curta de prefetch`() {
        val roadmap = EncounterProgressionRoadmapService.construir(
            npcBase(),
            solarCatalogo = catalogoSolarDeTeste(),
            dragonCatalogo = emptyList(),
            lunarCatalogo = emptyList()
        )

        assertTrue(roadmap.passos.isNotEmpty())
        assertTrue(roadmap.catalogFingerprint.isNotBlank())
        assertEquals(0, roadmap.proximoPasso)
        assertTrue(roadmap.passos.all { it.xpGasto >= 0 })
        assertTrue(roadmap.passos.size <= EncounterProgressionRoadmapService.PASSOS_PREFETCH)
    }

    @Test
    fun `roadmap respeita uma janela curta mesmo antes do proximo marco de essencia`() {
        val npc = npcBase().copy(xpGastoTotal = 50)
        val roadmap = EncounterProgressionRoadmapService.construir(
            npc, catalogoSolarDeTeste(), emptyList(), emptyList()
        )

        // 50 XP já é Essência 2; o próximo marco Solar continua sendo usado
        // como limite lógico, mas o prefetch materializa no máximo poucos passos.
        assertTrue(roadmap.passos.isNotEmpty())
        assertTrue(roadmap.passos.size <= EncounterProgressionRoadmapService.PASSOS_PREFETCH)
    }

    @Test
    fun `execucao do primeiro passo do roadmap preserva o resultado do algoritmo atual`() {
        val antes = npcBase()
        val planejado = EncounterProgressionRoadmapService.construir(
            antes,
            solarCatalogo = catalogoSolarDeTeste(),
            dragonCatalogo = emptyList(),
            lunarCatalogo = emptyList()
        )
        val passo = EncounterProgressionRoadmapService.proximo(antes, planejado)!!

        val porRoadmap = EncounterExperienceService.aplicarPassoRoadmap(antes, passo)!!
        val algoritmoAtual = EncounterGenerator.expandirEncantosPorExperiencia(antes, catalogoSolarDeTeste())

        assertEquals(semRoadmap(algoritmoAtual), semRoadmap(porRoadmap))
        assertEquals(1, EncounterProgressionRoadmapService.avancar(planejado).proximoPasso)
    }

    @Test
    fun `expansor retorna o lote recem produzido mesmo quando ja existe historico`() {
        val loteAnterior = com.example.model.HistoricoXpBatch(
            xpGasto = 99,
            nomesEncantosAdicionados = listOf("Lote anterior")
        )
        val antes = npcBase().copy(historicoXpBatches = listOf(loteAnterior))
        val expansor = EncounterXpExpanderFactory.from(
            TipoExaltadoEncontro.SOLAR,
            catalogoSolarDeTeste(),
            emptyList(),
            emptyList()
        )

        val resultado = expansor.expand(antes)

        assertEquals(2, resultado.npcResultante.historicoXpBatches.size)
        assertEquals(loteAnterior, resultado.npcResultante.historicoXpBatches.first())
        assertEquals(
            resultado.npcResultante.historicoXpBatches[1],
            resultado.batchAplicado
        )
    }

    @Test
    fun `cada passo do roadmap equivale a uma expansao normal`() {
        val base = npcBase()
        val planejado = EncounterProgressionRoadmapService.construir(
            base, catalogoSolarDeTeste(), emptyList(), emptyList()
        )

        var cache = planejado
        var porRoadmap = base
        var porAlgoritmo = base
        var passosAplicados = 0

        while (true) {
            val passo = EncounterProgressionRoadmapService.proximo(porRoadmap, cache) ?: break
            porRoadmap = EncounterExperienceService.aplicarPassoRoadmap(porRoadmap, passo)
                ?: error("Passo planejado deveria ser aplicável")
            cache = EncounterProgressionRoadmapService.avancar(cache)
            porAlgoritmo = EncounterGenerator.expandirEncantosPorExperiencia(
                porAlgoritmo, catalogoSolarDeTeste()
            )
            assertEquals(semRoadmap(porAlgoritmo), semRoadmap(porRoadmap))
            passosAplicados++
        }

        assertTrue(passosAplicados > 1)
    }

    @Test
    fun `cada passo do roadmap equivale a uma expansao normal para sangue de dragao`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO)
        val planejado = EncounterProgressionRoadmapService.construir(
            base, emptyList(), emptyList(), emptyList()
        )

        var cache = planejado
        var porRoadmap = base
        var porAlgoritmo = base
        var passosAplicados = 0

        while (passosAplicados < 3) {
            val passo = EncounterProgressionRoadmapService.proximo(porRoadmap, cache)
                ?: error("Passo planejado deveria existir")
            porRoadmap = EncounterExperienceService.aplicarPassoRoadmap(
                porRoadmap, passo
            ) ?: error("Passo planejado deveria ser aplicável")
            cache = EncounterProgressionRoadmapService.avancar(cache)
            porAlgoritmo = EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(
                porAlgoritmo, emptyList()
            )
            assertEquals(semRoadmap(porAlgoritmo), semRoadmap(porRoadmap))
            passosAplicados++
        }

        assertEquals(3, passosAplicados)
    }

    @Test
    fun `roadmap preparado de sangue de dragao equivale a expansao normal com catalogo real`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSangueDosDragoesCatalog(app).definitions
        val base = EncounterGenerator.gerarSangueDeDragao(
            nomeManual = "Roadmap DB",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSangueDeDragao = catalogo,
            random = kotlin.random.Random(739)
        )
        val planejado = EncounterProgressionRoadmapService.construir(
            base,
            solarCatalogo = emptyList(),
            dragonCatalogo = catalogo,
            lunarCatalogo = emptyList(),
            maxPassos = 1
        )
        val passo = EncounterProgressionRoadmapService.proximo(base, planejado)
            ?: error("Passo DB planejado deveria existir")
        val porRoadmap = EncounterExperienceService.aplicarPassoRoadmap(base, passo)
            ?: error("Passo DB planejado deveria ser aplicável")
        val porAlgoritmo = EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(
            base,
            catalogo
        )

        assertEquals(semRoadmap(porAlgoritmo), semRoadmap(porRoadmap))
    }

    @Test
    fun `roadmap preparado de sangue de dragao preserva equivalencia por tres passos`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSangueDosDragoesCatalog(app).definitions
        val base = EncounterGenerator.gerarSangueDeDragao(
            nomeManual = "Roadmap DB cache",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSangueDeDragao = catalogo,
            random = kotlin.random.Random(1741)
        )
        val planejado = EncounterProgressionRoadmapService.construir(
            base,
            solarCatalogo = emptyList(),
            dragonCatalogo = catalogo,
            lunarCatalogo = emptyList()
        )

        var cache = planejado
        var porRoadmap = base
        var porAlgoritmo = base
        var passos = 0
        while (passos < 3) {
            val passo = EncounterProgressionRoadmapService.proximo(porRoadmap, cache) ?: break
            porRoadmap = EncounterExperienceService.aplicarPassoRoadmap(porRoadmap, passo)
                ?: error("Passo DB preparado deveria ser aplicável")
            cache = EncounterProgressionRoadmapService.avancar(cache)
            porAlgoritmo = EncounterGenerator.expandirEncantosPorExperienciaSangueDeDragao(
                porAlgoritmo, catalogo
            )
            assertEquals(semRoadmap(porAlgoritmo), semRoadmap(porRoadmap))
            passos++
        }
        assertTrue(passos > 0)
    }

    @Test
    fun `cada passo do roadmap equivale a uma expansao normal para lunar`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.LUNAR)
        val planejado = EncounterProgressionRoadmapService.construir(
            base, emptyList(), emptyList(), emptyList()
        )

        var cache = planejado
        var porRoadmap = base
        var porAlgoritmo = base
        var passosAplicados = 0

        while (passosAplicados < 3) {
            val passo = EncounterProgressionRoadmapService.proximo(porRoadmap, cache)
                ?: error("Passo planejado deveria existir")
            porRoadmap = EncounterExperienceService.aplicarPassoRoadmap(
                porRoadmap, passo
            ) ?: error("Passo planejado deveria ser aplicável")
            cache = EncounterProgressionRoadmapService.avancar(cache)
            porAlgoritmo = EncounterGenerator.expandirEncantosPorExperienciaLunar(
                porAlgoritmo, emptyList()
            )
            assertEquals(semRoadmap(porAlgoritmo), semRoadmap(porRoadmap))
            passosAplicados++
        }

        assertEquals(3, passosAplicados)
    }

    @Test
    fun `roadmap e rejeitado quando o estado do NPC muda depois do planejamento`() {
        val planejado = EncounterProgressionRoadmapService.construir(
            npcBase(), catalogoSolarDeTeste(), emptyList(), emptyList()
        )
        val alterado = npcBase().copy(
            abilities = npcBase().abilities + ("Armas Brancas" to 2)
        )

        val passo = EncounterProgressionRoadmapService.proximo(alterado, planejado)
        assertEquals(null, passo)
        assertEquals(
            null,
            EncounterExperienceService.aplicarPassoRoadmap(
                alterado, planejado.passos.first()
            )
        )
    }

    @Test
    fun `sha256 hexadecimal otimizado preserva contrato textual`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            EncounterProgressionRoadmapService.sha256Hex("abc")
        )
    }

    @Test
    fun `fingerprint permanece igual quando colecoes sem ordem sao reordenadas`() {
        val base = npcBase().copy(
            abilities = linkedMapOf("Armas Brancas" to 1, "Esquiva" to 2),
            attributes = linkedMapOf("Força" to 3, "Destreza" to 4),
            especialidades = listOf(
                EspecialidadeEncontro("Esquiva"),
                EspecialidadeEncontro("Armas Brancas")
            )
        )
        val reordenado = base.copy(
            abilities = linkedMapOf("Esquiva" to 2, "Armas Brancas" to 1),
            attributes = linkedMapOf("Destreza" to 4, "Força" to 3),
            especialidades = base.especialidades.reversed()
        )

        assertEquals(
            EncounterProgressionRoadmapService.fingerprint(base),
            EncounterProgressionRoadmapService.fingerprint(reordenado)
        )
    }

    @Test
    fun `fingerprint ignora campos que a expansao de XP nao consulta`() {
        val base = npcBase()
        val derivadoAlterado = base.copy(
            nome = "Outro nome",
            motesPersonais = base.motesPersonais + 3,
            motesPerifericos = base.motesPerifericos + 4,
            healthBoxes = base.healthBoxes.reversed(),
            iniciativaAtual = base.iniciativaAtual + 2,
            dano = "-2",
            alertasValidacao = listOf("alterado")
        )

        assertEquals(
            EncounterProgressionRoadmapService.fingerprint(base),
            EncounterProgressionRoadmapService.fingerprint(derivadoAlterado)
        )
    }

    @Test
    fun `fingerprint solar muda quando muda uma entrada efetivamente lida pela expansao`() {
        val base = npcBase()
        val alterado = base.copy(
            abilities = base.abilities + ("Armas Brancas" to ((base.abilities["Armas Brancas"] ?: 0) + 1))
        )

        assertTrue(
            EncounterProgressionRoadmapService.fingerprint(base) !=
                EncounterProgressionRoadmapService.fingerprint(alterado)
        )
    }

    @Test
    fun `fingerprint muda quando foco explicito de progressao muda`() {
        val base = npcBase().copy(focoProgressaoExplicito = null)
        val focado = base.copy(focoProgressaoExplicito = "Armas Brancas")

        assertTrue(
            EncounterProgressionRoadmapService.fingerprint(base) !=
                EncounterProgressionRoadmapService.fingerprint(focado)
        )
    }

    @Test
    fun `fingerprint lunar muda quando muda um atributo efetivamente lido pela expansao`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.LUNAR)
        val alterado = base.copy(
            attributes = base.attributes + ("Vigor" to ((base.attributes["Vigor"] ?: 0) + 1))
        )

        assertTrue(
            EncounterProgressionRoadmapService.fingerprint(base) !=
                EncounterProgressionRoadmapService.fingerprint(alterado)
        )
    }

    @Test
    fun `fingerprint do catalogo permanece igual quando a ordem dos encantos muda`() {
        val catalogo = catalogoSolarDeTeste()
        val reordenado = catalogo.shuffled(java.util.Random(1234))

        assertEquals(
            EncounterProgressionRoadmapService.catalogFingerprint(
                catalogo, emptyList(), emptyList()
            ),
            EncounterProgressionRoadmapService.catalogFingerprint(
                reordenado, emptyList(), emptyList()
            )
        )
    }

    @Test
    fun `roadmap invalida catalogo alterado`() {
        val catalogo = catalogoSolarDeTeste()
        val planejado = EncounterProgressionRoadmapService.construir(
            npcBase(), catalogo, emptyList(), emptyList()
        )
        val alterado = catalogo.mapIndexed { indice, encanto ->
            if (indice == 0) encanto.copy(nome = "Nome alterado") else encanto
        }
        val fingerprintAlterado = EncounterProgressionRoadmapService.catalogFingerprint(
            alterado, emptyList(), emptyList()
        )

        assertTrue(planejado.catalogFingerprint != fingerprintAlterado)
        assertEquals(null, EncounterProgressionRoadmapService.proximo(npcBase(), planejado, fingerprintAlterado))
        assertEquals(
            null,
            EncounterExperienceService.aplicarPassoRoadmap(
                npcBase(),
                planejado.passos.first(),
                fingerprintAlterado
            )
        )
    }

    @Test
    fun `roadmap Lunar preserva escolha ofensiva ao aplicar XP`() {
        val base = npcBase().copy(tipoExaltado = TipoExaltadoEncontro.LUNAR)
        val planejado = EncounterProgressionRoadmapService.construir(
            base, emptyList(), emptyList(), emptyList()
        )
        val passo = EncounterProgressionRoadmapService.proximo(base, planejado)
            ?: error("Passo Lunar esperado")
        val direto = EncounterGenerator.expandirEncantosPorExperienciaLunar(base, emptyList())
        assertEquals(direto.lunarAtaqueEscolhido, passo.lunarAtaqueEscolhido)
        assertTrue(passo.lunarAtaqueEscolhido == "Força" || passo.lunarAtaqueEscolhido == "Destreza")
        val aplicado = EncounterExperienceService.aplicarPassoRoadmap(base, passo)
            ?: error("Passo Lunar deveria ser aplicavel")
        assertEquals(passo.lunarAtaqueEscolhido, aplicado.lunarAtaqueEscolhido)
        assertEquals(direto.lunarAtaqueEscolhido, aplicado.lunarAtaqueEscolhido)
        assertEquals(direto.xpAtual, aplicado.xpAtual)
        assertEquals(direto.xpGastoTotal, aplicado.xpGastoTotal)
        assertEquals(direto.charms, aplicado.charms)
        assertEquals(direto.historicoXpBatches, aplicado.historicoXpBatches)
        assertEquals(direto.corpoDeTouroCount, aplicado.corpoDeTouroCount)
        assertEquals(direto.essencia, aplicado.essencia)
        assertEquals(direto.attributes, aplicado.attributes)
        assertEquals(direto.formaEspiritualSecundaria, aplicado.formaEspiritualSecundaria)
        assertEquals(direto.lunarArchetypeTraits, aplicado.lunarArchetypeTraits)
        assertEquals(direto.healthBoxes, aplicado.healthBoxes)
        assertEquals(direto.motesPersonais, aplicado.motesPersonais)
        assertEquals(direto.motesPerifericos, aplicado.motesPerifericos)
        assertEquals(direto.abilities, aplicado.abilities)
        assertEquals(direto.especialidades, aplicado.especialidades)
        assertEquals(null, EncounterProgressionRoadmapService.proximo(
            base.copy(lunarAtaqueEscolhido = if (passo.lunarAtaqueEscolhido == "Força") "Destreza" else "Força"), planejado
        ))
    }

    @Test
    fun `roadmap invalida versao antiga`() {
        val planejado = EncounterProgressionRoadmapService.construir(
            npcBase(), catalogoSolarDeTeste(), emptyList(), emptyList()
        )
        val antigo = planejado.copy(algorithmVersion = 6)

        assertEquals(null, EncounterProgressionRoadmapService.proximo(npcBase(), antigo))
    }

    @Test
    fun `reduzir experiencia volta um passo do roadmap`() {
        val antes = npcBase()
        val planejado = EncounterProgressionRoadmapService.construir(
            antes,
            catalogoSolarDeTeste(), emptyList(), emptyList()
        )
        val apos = EncounterExperienceService.aplicarPassoRoadmap(
            antes, EncounterProgressionRoadmapService.proximo(antes, planejado)!!
        )!!
        val reduzido = EncounterGenerator.reduzirExperiencia(apos)

        assertEquals(antes.xpAtual, reduzido.xpAtual)
        assertEquals(antes.xpGastoTotal, reduzido.xpGastoTotal)
    }

    @Test
    fun expansor_abstrato_preserva_a_expansao_por_tipo() {
        val solarCatalogo = emptyList<EncantoSolarDefinition>()
        val dragonCatalogo = emptyList<EncantoSangueDeDragaoDefinition>()
        val lunarCatalogo = emptyList<EncantoLunarDefinition>()

        val solar = EncounterXpExpanderFactory.from(
            com.example.model.TipoExaltadoEncontro.SOLAR,
            solarCatalogo, dragonCatalogo, lunarCatalogo
        )
        val dragon = EncounterXpExpanderFactory.from(
            com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            solarCatalogo, dragonCatalogo, lunarCatalogo
        )
        val lunar = EncounterXpExpanderFactory.from(
            com.example.model.TipoExaltadoEncontro.LUNAR,
            solarCatalogo, dragonCatalogo, lunarCatalogo
        )

        // Mesmo sem catálogo, uma chamada de +XP é registrada pelo algoritmo
        // normal como um lote de progressão (neste caso, sem gasto de XP).
        // O teste verifica que cada implementação da fábrica realmente
        // executa uma expansão, sem impor um comportamento diferente ao
        // algoritmo apenas para satisfazer o teste da abstração.
        val solarResult = solar.expand(npcBase().copy(tipoExaltado = TipoExaltadoEncontro.SOLAR))
        val dragonResult = dragon.expand(npcBase().copy(tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO))
        val lunarResult = lunar.expand(npcBase().copy(tipoExaltado = TipoExaltadoEncontro.LUNAR))

        assertEquals(1, solarResult.npcResultante.historicoXpBatches.size)
        assertEquals(1, dragonResult.npcResultante.historicoXpBatches.size)
        assertEquals(1, lunarResult.npcResultante.historicoXpBatches.size)

        // O batch devolvido pela abstração deve ser exatamente o batch produzido
        // pela expansão correspondente. O teste não fixa artificialmente o custo
        // de XP de uma implementação concreta.
        assertEquals(solarResult.npcResultante.historicoXpBatches.last(), solarResult.batchAplicado)
        assertEquals(dragonResult.npcResultante.historicoXpBatches.last(), dragonResult.batchAplicado)
        assertEquals(lunarResult.npcResultante.historicoXpBatches.last(), lunarResult.batchAplicado)
    }
    @Test
    fun `passo ja validado preserva exatamente o resultado do aplicador defensivo`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val catalogo = EncantosSolaresCatalog(app).definitions
        val npc = EncounterGenerator.gerarSolar(
            nomeManual = "Baseline 295",
            arquetipo = com.example.model.ArquetipoEncontro.FISICO,
            encantosSolares = catalogo,
            feiticos = emptyList(),
            culturaNome = null,
            generoNome = null,
            meritosCatalogo = emptyList(),
            random = kotlin.random.Random(295)
        )
        // Este contrato compara apenas o próximo passo. Construir a janela padrão
        // de três passos executava duas expansões caras que nunca eram observadas
        // pela asserção e inflava desnecessariamente o tempo do CI.
        val roadmap = EncounterProgressionRoadmapService.construir(
            npc, catalogo, emptyList(), emptyList(), maxPassos = 1
        )
        val passo = EncounterProgressionRoadmapService.proximo(npc, roadmap)
            ?: return

        val defensivo = EncounterExperienceService.aplicarPassoRoadmap(npc, passo)
        val jaValidado = EncounterExperienceService.aplicarPassoRoadmapJaValidado(npc, passo)

        kotlin.test.assertEquals(defensivo, jaValidado)
    }


    @Test fun `fingerprint muda quando catalogo marcial muda`() {
        fun estilo(nomeEncanto: String) = EstiloArteMarcialDefinition(
            id = "tigre", nomePt = "Tigre", nomeEn = "Tiger", descricao = "",
            armaDoEstiloTexto = null, armaDoEstiloModo = "desarmado",
            armasEspecificas = emptyList(), armaduraTexto = null,
            armaduraCategoria = "todas", habilidadesComplementares = null,
            tiposExaltadosPermitidos = setOf(TipoExaltadoEncontro.SOLAR),
            encantos = listOf(
                EncantoArteMarcialDefinition(
                    id = nomeEncanto, estiloId = "tigre", habilidade = "Tigre",
                    nome = nomeEncanto, nomeIngles = "", custo = "", minsTexto = "",
                    minHabilidade = 1, minEssencia = 1, tipo = "Suplementar",
                    palavrasChave = "", duracao = "", preRequisitos = "Nenhum",
                    descricao = "", quadros = emptyList()
                )
            )
        )
        val base = EncounterProgressionRoadmapService.catalogFingerprint(
            emptyList(), emptyList(), emptyList(), listOf(estilo("Raiz"))
        )
        val alterado = EncounterProgressionRoadmapService.catalogFingerprint(
            emptyList(), emptyList(), emptyList(), listOf(estilo("Raiz Alterada"))
        )
        assertTrue(base != alterado)
    }


    @Test
    fun `fingerprint lunar muda quando contexto das formas espirituais muda`() {
        val base = npcBase().copy(
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            formaEspiritual = "Lobo",
            formaEspiritualSecundaria = "",
            lunarPrimaryArchetypeTraits = listOf("PREDATOR"),
            lunarArchetypeTraits = listOf("PREDATOR")
        )
        val formaAlterada = base.copy(formaEspiritual = "Urso")
        val secundariaAlterada = base.copy(formaEspiritualSecundaria = "Coruja")
        val traitsAlterados = base.copy(lunarArchetypeTraits = listOf("PREDATOR", "FLYER"))

        val fingerprint = EncounterProgressionRoadmapService.fingerprint(base)
        assertTrue(fingerprint != EncounterProgressionRoadmapService.fingerprint(formaAlterada))
        assertTrue(fingerprint != EncounterProgressionRoadmapService.fingerprint(secundariaAlterada))
        assertTrue(fingerprint != EncounterProgressionRoadmapService.fingerprint(traitsAlterados))
    }

    @Test
    fun `saldo abaixo do custo preserva XP quando existe encanto elegivel`() {
        val antes = npcBase().copy(xpAtual = 0)
        val depois = EncounterGenerator.expandirEncantosPorExperiencia(
            antes,
            catalogoSolarDeTeste(1)
        )

        assertEquals(EncounterExperienceService.XP_POR_CHAMADA, depois.xpAtual)
        assertEquals(antes.xpGastoTotal, depois.xpGastoTotal)
        assertEquals(antes.charms.map { it.nome }, depois.charms.map { it.nome })
        assertEquals(antes.abilities, depois.abilities)
    }


}
