package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.ArmorCatalog
import com.example.data.EncantosLunaresCatalog
import com.example.data.EncantosSangueDosDragoesCatalog
import com.example.data.EncantosSolaresCatalog
import com.example.data.EncounterEquipmentService
import com.example.data.EncounterExperienceService
import com.example.data.EncounterMutationPipeline
import com.example.data.EncounterGenerator
import com.example.data.FeiticariaCatalog
import com.example.data.MeritosCatalog
import com.example.data.SheetRepository
import com.example.data.WeaponCatalog
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
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterNpcActionsEquipmentIntegrationTest {
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
    fun `trocar arma preserva NPC e sincroniza snapshot atualizado com iniciativas`() {
        val solar = EncantosSolaresCatalog(app)
        val npcInicial = EncounterGenerator.gerarSolar(
            nomeManual = "Regressao Equipamento Arma",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSolares = solar.definitions
        )
        val state = MutableStateFlow(listOf(npcInicial))
        val sincronizados = mutableListOf<NpcEncontro>()
        val actions = actions(state, solar, sincronizados)

        val candidata = WeaponCatalog.candidatasPorHabilidade(npcInicial.habilidadePrincipal).first()
        val novaArma = EncounterEquipmentService.montarArmaSelecionada(
            habilidadeCombate = npcInicial.habilidadePrincipal,
            armaCatalogo = candidata
        )

        actions.atualizarArmaNpc(npcInicial.id, novaArma)

        val atualizado = state.value.single()
        assertEquals(npcInicial.id, atualizado.id)
        assertEquals(
            EncounterMutationPipeline.recalcular(npcInicial.copy(arma = novaArma)),
            atualizado
        )
        assertEquals(novaArma, atualizado.arma)
        assertEquals(listOf(atualizado), sincronizados)
    }

    @Test
    fun `trocar armadura recalcula derivados defensivos e sincroniza iniciativas`() {
        val solar = EncantosSolaresCatalog(app)
        val npcInicial = EncounterGenerator.gerarSolar(
            nomeManual = "Regressao Equipamento Armadura",
            arquetipo = ArquetipoEncontro.FISICO,
            encantosSolares = solar.definitions
        )
        val state = MutableStateFlow(listOf(npcInicial))
        val sincronizados = mutableListOf<NpcEncontro>()
        val actions = actions(state, solar, sincronizados)

        val candidata = listOf("Leve", "Média", "Pesada")
            .flatMap(ArmorCatalog::candidatas)
            .first { catalogo ->
                val montada = EncounterEquipmentService.montarArmaduraSelecionada(catalogo)
                montada.absorcao != npcInicial.absorcaoArmadura || montada.dureza != npcInicial.dureza
            }
        val novaArmadura = EncounterEquipmentService.montarArmaduraSelecionada(candidata)

        actions.atualizarArmaduraNpc(npcInicial.id, novaArmadura)

        val atualizado = state.value.single()
        assertEquals(npcInicial.id, atualizado.id)
        assertEquals(novaArmadura, atualizado.armadura)
        assertEquals(novaArmadura.absorcao, atualizado.absorcaoArmadura)
        assertEquals(npcInicial.absorcaoNatural + novaArmadura.absorcao, atualizado.absorcao)
        assertEquals(novaArmadura.dureza, atualizado.dureza)
        assertNotEquals(npcInicial.armadura, atualizado.armadura)
        assertEquals(listOf(atualizado), sincronizados)
    }

    private fun actions(
        state: MutableStateFlow<List<NpcEncontro>>,
        solar: EncantosSolaresCatalog,
        sincronizados: MutableList<NpcEncontro>
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
        loading = MutableStateFlow(false),
        error = MutableStateFlow(null),
        repository = SheetRepository(app),
        solarCatalog = solar,
        dragonBloodedCatalog = dragon,
        lunarCatalog = lunar,
        feiticariaCatalog = feiticaria,
        meritosCatalog = meritos,
        preparedEncounterCatalog = prepared,
        scope = scope,
        removerDaIniciativa = {},
        atualizarNaIniciativa = { sincronizados += it }
        )
    }
}
