package com.example.data

import kotlin.test.Test
import kotlin.test.assertTrue
import java.io.File

class EncounterMutationPipelineContractTest {
    @Test
    fun `pipeline centraliza recálculo canônico e derivados de armadura`() {
        val source = File("src/main/java/com/example/data/EncounterMutationPipeline.kt").readText()
        assertTrue(source.contains("EncounterExperienceService.recalcularDerivados"))
        assertTrue(source.contains("absorcaoArmadura = armadura?.absorcao ?: 0"))
        assertTrue(source.contains("dureza = armadura?.dureza ?: 0"))
    }

    @Test
    fun `editores de equipamento usam pipeline central`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val calls = Regex("EncounterMutationPipeline\\.recalcular").findAll(source).count()
        assertTrue(calls >= 2)
    }


    @Test
    fun `xp positivo e negativo passam pelo pipeline antes da persistencia`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        assertTrue(source.contains("EncounterMutationPipeline.recalcular(expanded)"))
        assertTrue(source.contains("EncounterMutationPipeline.recalcular(reduzido)"))
    }


    @Test
    fun `gestao manual de feiticos passa pelo pipeline antes da persistencia`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("fun atualizarFeitico")
        val fim = source.indexOf("fun removeExtraHealthBox", inicio)
        val metodo = source.substring(inicio, fim)
        assertTrue(metodo.contains("EncounterMutationPipeline.recalcular(novo)"))
        assertTrue(metodo.contains("repository.salvarNpcsEncontro(publicado)"))
        assertTrue(metodo.indexOf("EncounterMutationPipeline.recalcular(novo)") <
            metodo.indexOf("repository.salvarNpcsEncontro(publicado)"))
    }


    @Test
    fun `carregamento de NPC recalcula derivados antes de publicar e persistir`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("fun carregarDeArquivo")
        val fim = source.indexOf("fun atualizarArmaNpc", inicio)
        val metodo = source.substring(inicio, fim)
        assertTrue(metodo.contains("EncounterMutationPipeline.recalcular("))
        assertTrue(metodo.indexOf("EncounterMutationPipeline.recalcular(") <
            metodo.indexOf("substituirNpcNoEstado"))
        assertTrue(metodo.indexOf("EncounterMutationPipeline.recalcular(") <
            metodo.indexOf("atualizarNaIniciativa"))
    }


    @Test
    fun `iniciativa e penalidades temporarias nao recalculam derivados`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()

        val inicioIniciativa = source.indexOf("fun adjustInitiative")
        val fimIniciativa = source.indexOf("fun aplicarPenalidadeClash", inicioIniciativa)
        val iniciativa = source.substring(inicioIniciativa, fimIniciativa)
        assertTrue(!iniciativa.contains("EncounterMutationPipeline.recalcular("))
        assertTrue(iniciativa.contains("atualizarNaIniciativa"))

        val inicioClash = fimIniciativa
        val fimClash = source.indexOf("fun obterMetricasRoadmap", inicioClash)
        val clash = source.substring(inicioClash, fimClash)
        assertTrue(!clash.contains("EncounterMutationPipeline.recalcular("))
        assertTrue(!source.contains("fun removerPenalidadeClash("))

        val inicioLimpeza = source.indexOf("fun limparPenalidadesDefesa")
        val fimLimpeza = source.indexOf("fun adjustInitiative", inicioLimpeza)
        val limpeza = source.substring(inicioLimpeza, fimLimpeza)
        assertTrue(!limpeza.contains("EncounterMutationPipeline.recalcular("))
        assertTrue(limpeza.contains("penalidadeClashDefesa = 0"))
        assertTrue(limpeza.contains("penalidadeAtaquesDefesa = 0"))
    }

    @Test
    fun `xp assincrono publica somente sobre o snapshot que calculou`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("fun expandirEncantos")
        val fim = source.indexOf("private fun expandirXpSemRoadmap", inicio)
        val metodo = source.substring(inicio, fim)

        assertTrue(metodo.contains("substituirNpcSeSnapshotAtual("))
        assertTrue(metodo.contains("snapshotEsperado = currentOriginal"))
        assertTrue(metodo.contains("roadmapCache.remove(npcId)"))
        assertTrue(metodo.contains("continue"))
    }

    @Test
    fun `reducao de xp tambem rejeita publicacao sobre snapshot obsoleto`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("fun reduzirExperiencia")
        val fim = source.indexOf("fun toggleForcaDeVontade", inicio)
        val metodo = source.substring(inicio, fim)

        assertTrue(metodo.contains("substituirNpcSeSnapshotAtual("))
        assertTrue(metodo.contains("snapshotEsperado = current"))
        assertTrue(metodo.contains("continue"))
    }

    @Test
    fun `edicao de feitico transforma o npc atual dentro de state update`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("fun atualizarFeitico")
        val fim = source.indexOf("fun removeExtraHealthBox", inicio)
        val metodo = source.substring(inicio, fim)

        assertTrue(metodo.contains("state.update"))
        assertTrue(metodo.contains("val atual = lista[indice]"))
        assertTrue(!metodo.contains("state.value.firstOrNull"))
    }


    @Test
    fun `carregar npc invalida roadmap derivado`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("fun carregarDeArquivo")
        val fim = source.indexOf("fun atualizarArmaNpc", inicio)
        val metodo = source.substring(inicio, fim)

        assertTrue(metodo.contains("roadmapCache.remove(npcId)"))
        assertTrue(metodo.indexOf("substituirNpcNoEstado") < metodo.indexOf("roadmapCache.remove(npcId)"))
    }


    @Test
    fun `prefetch propaga cancelamento sem registrar falha`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("fun expandirEncantos")
        val fim = source.indexOf("private fun expandirXpSemRoadmap", inicio)
        val metodo = source.substring(inicio, fim)

        assertTrue(metodo.contains("catch (e: CancellationException)"))
        assertTrue(metodo.contains("throw e"))
        assertTrue(
            metodo.indexOf("catch (e: CancellationException)") <
                metodo.indexOf("roadmapMetrics.recordPrefetchFailed()")
        )
    }

    @Test
    fun `limpeza cancela jobs derivados de prefetch e auditoria`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val inicio = source.indexOf("private fun limparRecursosTransitórios")
        val fim = source.indexOf("fun gerarSolar", inicio)
        val metodo = source.substring(inicio, fim)

        assertTrue(metodo.contains("backgroundJobs.forEach { it.cancel() }"))
        assertTrue(metodo.contains("backgroundJobs.clear()"))
        assertTrue(metodo.contains("roadmapPrefetchAtivos.clear()"))
    }


    @Test
    fun `xp positivo e negativo propagam cancelamento do lifecycle`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()

        val maisInicio = source.indexOf("fun expandirEncantos")
        val maisFim = source.indexOf("private fun expandirXpSemRoadmap", maisInicio)
        val maisXp = source.substring(maisInicio, maisFim)
        assertTrue(maisXp.contains("catch (e: CancellationException)"))
        assertTrue(maisXp.contains("throw e"))

        val menosInicio = source.indexOf("fun reduzirExperiencia")
        val menosFim = source.indexOf("fun toggleForcaDeVontade", menosInicio)
        val menosXp = source.substring(menosInicio, menosFim)
        assertTrue(menosXp.contains("catch (e: CancellationException)"))
        assertTrue(menosXp.contains("throw e"))
    }

    @Test
    fun `jobs derivados respeitam epoch de limpeza`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        assertTrue(source.contains("@Volatile private var backgroundEpoch = 0L"))
        assertTrue(source.contains("backgroundEpoch++"))
        assertTrue(source.contains("val epoch = backgroundEpoch"))
        assertTrue(source.contains("if (epoch != backgroundEpoch) return@launch"))
    }


    @Test
    fun `reorganizacao de vitalidade remove somente o proprio job de debounce`() {
        val source = File("src/main/java/com/example/viewmodel/EncounterNpcVitalityActions.kt").readText()
        val inicio = source.indexOf("private fun agendarReorganizacao")
        val fim = source.indexOf("fun removeExtraHealthBox", inicio)
        val metodo = source.substring(inicio, fim)

        assertTrue(metodo.contains("CoroutineStart.LAZY"))
        assertTrue(metodo.contains("reorganizationJobs[npcId] = job"))
        assertTrue(metodo.contains("reorganizationJobs.remove(npcId, job)"))
        assertTrue(metodo.contains("job.start()"))
        assertTrue(!metodo.lines().any { it.trim() == "reorganizationJobs.remove(npcId)" })
    }


    @Test
    fun `jobs de vitalidade acompanham lifecycle global e remocao do npc`() {
        val actions = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()
        val vitality = File("src/main/java/com/example/viewmodel/EncounterNpcVitalityActions.kt").readText()

        val limpezaInicio = actions.indexOf("private fun limparRecursosTransitórios")
        val limpezaFim = actions.indexOf("fun gerarSolar", limpezaInicio)
        assertTrue(actions.substring(limpezaInicio, limpezaFim).contains("vitality.dispose()"))

        val removeInicio = actions.indexOf("fun remove(id: String)")
        val removeFim = actions.indexOf("fun feiticosDisponiveis", removeInicio)
        assertTrue(actions.substring(removeInicio, removeFim).contains("vitality.remove(id)"))

        assertTrue(vitality.contains("fun dispose()"))
        assertTrue(vitality.contains("reorganizationJobs.forEach { (_, job) -> job.cancel() }"))
        assertTrue(vitality.contains("fun remove(npcId: String)"))
        assertTrue(vitality.contains("reorganizationJobs.remove(npcId)?.cancel()"))
    }


    @Test
    fun remocaoDeCaixaExtraCancelaDebounceDeVitalidadeSomenteQuandoMuta() {
        val vitality = File("src/main/java/com/example/viewmodel/EncounterNpcVitalityActions.kt").readText()
        val inicio = vitality.indexOf("fun removeExtraHealthBox")
        val fim = vitality.indexOf("fun clearHealthDamage", inicio)
        val metodo = vitality.substring(inicio, fim)

        assertTrue(metodo.contains("if (novoEstado != null)"))
        assertTrue(metodo.contains("reorganizationJobs.remove(npcId)?.cancel()"))
        assertTrue(
            metodo.indexOf("reorganizationJobs.remove(npcId)?.cancel()") <
                metodo.indexOf("repository.salvarNpcsEncontro(state.value)")
        )
    }


    @Test
    fun mutacoesSomenteDeCombateNaoDisparamRecalculoDerivadoCompleto() {
        val actions = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()

        val intervalos = listOf(
            "adjustInitiative" to "aplicarPenalidadeClash",
            "aplicarPenalidadeClash" to "fun obterMetricasRoadmap",
            "limparPenalidadesDefesa" to "adjustInitiative",
        )
        intervalos.forEach { (inicioNome, fimNome) ->
            val inicio = actions.indexOf("fun $inicioNome")
            val fim = actions.indexOf(fimNome, inicio + 1)
            val metodo = actions.substring(inicio, fim)
            assertTrue(
                !metodo.contains("EncounterMutationPipeline.recalcular"),
                "$inicioNome não deve recalcular equipamento/motes/XP"
            )
        }
    }

    @Test
    fun encerramentoDeCombateLimpaDefesasTemporariasDosNpcsVinculados() {
        val viewModel = File("src/main/java/com/example/viewmodel/SheetViewModel.kt").readText()
        val inicio = viewModel.indexOf("fun encerrarCombateEGuardarHistorico")
        val fim = viewModel.indexOf("private val encantoCatalog", inicio)
        val metodo = viewModel.substring(inicio, fim)

        assertTrue(metodo.contains("mapNotNull { it.origemNpcId }"))
        assertTrue(metodo.contains("encounterNpcActions.limparPenalidadesDefesa(npcIdsDoCombate)"))
        assertTrue(
            metodo.indexOf("mapNotNull { it.origemNpcId }") <
                metodo.indexOf("iniciativasController.encerrarCombate()")
        )
    }


    @Test
    fun mutacoesDaAba11PersistemSnapshotPublicadoSemRelerStateValue() {
        val actions = File("src/main/java/com/example/viewmodel/EncounterNpcActions.kt").readText()

        assertTrue(!actions.contains("state.value.let(repository::salvarNpcsEncontro)"))
        assertTrue(actions.contains("repository.salvarNpcsEncontro(novoEstado)"))
        assertTrue(actions.contains("novoEstado?.let(repository::salvarNpcsEncontro)"))
    }


}
