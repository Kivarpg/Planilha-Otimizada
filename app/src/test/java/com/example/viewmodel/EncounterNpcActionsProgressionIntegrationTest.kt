package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.EncantosLunaresCatalog
import com.example.data.EncantosSangueDosDragoesCatalog
import com.example.data.EncantosSolaresCatalog
import com.example.data.ArmorCatalog
import com.example.data.EncounterEquipmentService
import com.example.data.WeaponCatalog
import com.example.data.EncounterGenerator
import com.example.data.FeiticariaCatalog
import com.example.data.MeritosCatalog
import com.example.data.SheetRepository
import com.example.model.ArquetipoEncontro
import com.example.model.NpcEncontro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterNpcActionsProgressionIntegrationTest {
    private lateinit var app: Application
    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app = ApplicationProvider.getApplicationContext()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `segundo mais XP com prefetch permanece equivalente a progressao direta`() {
        val solar = EncantosSolaresCatalog(app)
        val npcInicial = EncounterGenerator.gerarSolar(
            nomeManual = "Regressao Roadmap",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSolares = solar.definitions
        )
        val state = MutableStateFlow(listOf(npcInicial))
        val loading = MutableStateFlow(false)
        val error = MutableStateFlow<String?>(null)
        val actions = actions(state, loading, error, solar)

        actions.expandirEncantos(npcInicial.id)
        esperar { !loading.value && state.value.single().historicoXpBatches.size == 1 }
        val depoisDoPrimeiro = state.value.single()
        val esperadoSegundo = EncounterGenerator.expandirEncantosPorExperiencia(
            depoisDoPrimeiro,
            solar.definitions
        )

        actions.expandirEncantos(npcInicial.id)
        esperar { !loading.value && state.value.single().historicoXpBatches.size == 2 }

        assertEquals(esperadoSegundo, state.value.single())
        assertEquals(null, error.value)
        esperar {
            val metrics = actions.obterMetricasRoadmap()
            metrics.prefetchCompleted + metrics.prefetchInvalidated +
                metrics.prefetchDiscarded + metrics.prefetchFailed >= 1L
        }
        val metrics = actions.obterMetricasRoadmap()
        assertTrue("primeiro +XP deve agendar prefetch para os proximos passos", metrics.prefetchScheduled >= 1L)
        assertTrue("primeiro +XP sem roadmap deve usar a progressao direta", metrics.roadmapFallbacks >= 1L)
    }

    @Test
    fun `equipamento manual escolhido entre expansoes sobrevive ao mais XP`() {
        val solar = EncantosSolaresCatalog(app)
        val npcInicial = EncounterGenerator.gerarSolar(
            nomeManual = "Regressao Roadmap Equipamento",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSolares = solar.definitions
        )
        val state = MutableStateFlow(listOf(npcInicial))
        val loading = MutableStateFlow(false)
        val error = MutableStateFlow<String?>(null)
        val actions = actions(state, loading, error, solar)

        actions.expandirEncantos(npcInicial.id)
        esperar { !loading.value && state.value.single().historicoXpBatches.size == 1 }

        val antesDaTroca = state.value.single()
        val armaCatalogo = WeaponCatalog.candidatasPorHabilidade(antesDaTroca.habilidadePrincipal)
            .first { candidata ->
                EncounterEquipmentService.montarArmaSelecionada(
                    antesDaTroca.habilidadePrincipal, candidata
                ) != antesDaTroca.arma
            }
        val armaManual = EncounterEquipmentService.montarArmaSelecionada(
            antesDaTroca.habilidadePrincipal, armaCatalogo
        )
        val armaduraCatalogo = listOf("Leve", "Média", "Pesada")
            .flatMap(ArmorCatalog::candidatas)
            .first { candidata ->
                EncounterEquipmentService.montarArmaduraSelecionada(candidata) != antesDaTroca.armadura
            }
        val armaduraManual = EncounterEquipmentService.montarArmaduraSelecionada(armaduraCatalogo)

        // A troca acontece entre dois lotes de XP e deve permanecer fonte de verdade.
        actions.atualizarArmaNpc(npcInicial.id, armaManual)
        actions.atualizarArmaduraNpc(npcInicial.id, armaduraManual)

        val depoisDaTroca = state.value.single()
        assertEquals(armaManual, depoisDaTroca.arma)
        assertEquals(armaduraManual, depoisDaTroca.armadura)

        actions.expandirEncantos(npcInicial.id)
        esperar { !loading.value && state.value.single().historicoXpBatches.size == 2 }

        val depoisDoRoadmap = state.value.single()
        assertEquals(null, error.value)
        assertEquals("arma manual nao pode ser revertida pelo roadmap", armaManual, depoisDoRoadmap.arma)
        assertEquals("armadura manual nao pode ser revertida pelo roadmap", armaduraManual, depoisDoRoadmap.armadura)
        assertEquals(armaduraManual.absorcao, depoisDoRoadmap.absorcaoArmadura)
        assertEquals(
            depoisDoRoadmap.absorcaoNatural + armaduraManual.absorcao,
            depoisDoRoadmap.absorcao
        )
        assertEquals(armaduraManual.dureza, depoisDoRoadmap.dureza)
        assertTrue(
            "runtime deve poder preparar os proximos passos sem alterar equipamento manual",
            actions.obterMetricasRoadmap().prefetchScheduled >= 1L
        )
    }

    @Test
    fun `reduzir XP invalida roadmap antes de uma nova expansao`() {
        val solar = EncantosSolaresCatalog(app)
        val npcInicial = EncounterGenerator.gerarSolar(
            nomeManual = "Regressao Invalidacao",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSolares = solar.definitions
        )
        val state = MutableStateFlow(listOf(npcInicial))
        val loading = MutableStateFlow(false)
        val error = MutableStateFlow<String?>(null)
        val actions = actions(state, loading, error, solar)

        actions.expandirEncantos(npcInicial.id)
        esperar { !loading.value && state.value.single().historicoXpBatches.size == 1 }
        actions.reduzirExperiencia(npcInicial.id)
        esperar { !loading.value && state.value.single().historicoXpBatches.isEmpty() }
        assertEquals(
            npcInicial.copy(primeiroXpRecebido = true),
            state.value.single()
        )

        val fallbackAntes = actions.obterMetricasRoadmap().roadmapFallbacks
        actions.expandirEncantos(npcInicial.id)
        esperar { !loading.value && state.value.single().historicoXpBatches.size == 1 }

        assertEquals(null, error.value)
        assertEquals(
            "depois da reducao, a nova expansao deve executar a rota direta",
            fallbackAntes + 1,
            actions.obterMetricasRoadmap().roadmapFallbacks
        )
    }

    @Test
    fun `expiracao de defesa limpa apenas os npcs do proximo slot`() {
        val solar = EncantosSolaresCatalog(app)
        val base = EncounterGenerator.gerarSolar(
            nomeManual = "Regressao Penalidade",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSolares = solar.definitions
        )
        val expirando = base.copy(
            id = "npc-expirando",
            penalidadeClashDefesa = 2,
            penalidadeAtaquesDefesa = 3
        )
        val preservado = base.copy(
            id = "npc-preservado",
            nome = "Regressao Penalidade Preservada",
            penalidadeClashDefesa = 2,
            penalidadeAtaquesDefesa = 1
        )
        val state = MutableStateFlow(listOf(expirando, preservado))
        val actions = actions(state, MutableStateFlow(false), MutableStateFlow(null), solar)

        actions.limparPenalidadesDefesa(setOf(expirando.id))

        val resultadoExpirando = state.value.first { it.id == expirando.id }
        val resultadoPreservado = state.value.first { it.id == preservado.id }
        assertEquals(0, resultadoExpirando.penalidadeClashDefesa)
        assertEquals(0, resultadoExpirando.penalidadeAtaquesDefesa)
        assertEquals(2, resultadoPreservado.penalidadeClashDefesa)
        assertEquals(1, resultadoPreservado.penalidadeAtaquesDefesa)
    }

    private fun actions(
        state: MutableStateFlow<List<NpcEncontro>>,
        loading: MutableStateFlow<Boolean>,
        error: MutableStateFlow<String?>,
        solar: EncantosSolaresCatalog
    ) : EncounterNpcActions {
        val dragon = EncantosSangueDosDragoesCatalog(app)
        val lunar = EncantosLunaresCatalog(app)
        val feiticaria = FeiticariaCatalog(app)
        val meritos = MeritosCatalog(app)
        val prepared = com.example.data.PreparedEncounterCatalog.prepare(
            solares = solar.definitions,
            sangueDeDragao = dragon.definitions,
            lunares = lunar.definitions,
            feiticos = feiticaria.definitions,
            estilosMarciais = emptyList()
        )
        return EncounterNpcActions(
        state = state,
        loading = loading,
        error = error,
        repository = SheetRepository(app),
        solarCatalog = solar,
        dragonBloodedCatalog = dragon,
        lunarCatalog = lunar,
        feiticariaCatalog = feiticaria,
        meritosCatalog = meritos,
        preparedEncounterCatalog = prepared,
        scope = scope,
        removerDaIniciativa = {},
        atualizarNaIniciativa = {}
        )
    }

    private fun esperar(timeoutSegundos: Long = 30, condicao: () -> Boolean) {
        val limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSegundos)
        while (!condicao() && System.nanoTime() < limite) {
            Thread.sleep(5)
        }
        assertTrue("condicao assincrona nao foi satisfeita dentro do timeout", condicao())
    }
}
