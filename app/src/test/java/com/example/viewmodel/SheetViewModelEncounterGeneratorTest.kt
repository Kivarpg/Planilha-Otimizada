package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Testa gerarNpcEncontro() pelo caminho real (assíncrono, via
// viewModelScope), reproduzindo o relato do usuário: gerar um NPC e, em
// seguida, gerar outro.
//
// IMPORTANTE sobre sincronização: gerarNpcEncontro() faz o trabalho pesado
// dentro de withContext(Dispatchers.Default) — um dispatcher de THREAD REAL,
// não controlado por UnconfinedTestDispatcher/TestScope. Isso significa que
// a corrotina de fato SUSPENDE nesse ponto e devolve o controle pro chamador
// imediatamente; sem esperar de verdade, o callback pode não ter disparado
// ainda quando a asserção roda (uma versão anterior deste teste, mais
// simples, tinha exatamente essa falha — corrida de fato, não só teórica).
// Por isso cada chamada aqui espera um CountDownLatch ser liberado dentro
// do próprio callback, com timeout generoso, antes de prosseguir com as
// asserções.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SheetViewModelEncounterGeneratorTest {

    private lateinit var viewModel: SheetViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = SheetViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun gerarESincronizar(
        arquetipo: ArquetipoEncontro,
        nomeManual: String = ""
    ): com.example.model.NpcEncontro? {
        val latch = CountDownLatch(1)
        var resultado: com.example.model.NpcEncontro? = null
        viewModel.gerarNpcEncontro(
            nomeManual = nomeManual,
            arquetipo = arquetipo,
            onResultado = { npc ->
                resultado = npc
                latch.countDown()
            }
        )
        val terminouATempo = latch.await(30, TimeUnit.SECONDS)
        assertTrue("gerarNpcEncontro nao chamou o callback dentro do timeout", terminouATempo)

        // O callback é publicado antes do finally de gerarComTratamento(),
        // onde gerandoNpcEncontro volta para false. Esperar somente o callback
        // ainda permite uma corrida entre o callback e a finalização da operação.
        val limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (viewModel.gerandoNpcEncontro.value && System.nanoTime() < limite) {
            Thread.yield()
        }
        assertTrue("gerarNpcEncontro terminou o callback, mas o estado de geração não foi finalizado", !viewModel.gerandoNpcEncontro.value)
        return resultado
    }

    @Test
    fun `gerarNpcEncontro chama o callback com um NPC valido e sem erro`() {
        val resultado = gerarESincronizar(ArquetipoEncontro.FISICO)

        assertNotNull("o callback deveria receber um NPC, nao null", resultado)
        assertNull("nao deveria haver mensagem de erro", viewModel.erroNpcEncontro.value)
        assertEquals(false, viewModel.gerandoNpcEncontro.value)
        assertEquals(1, viewModel.npcsEncontro.value.size)
        assertNotNull(resultado)
    }

    @Test
    fun `gerar dois NPCs em sequencia pelo ViewModel nao lanca excecao e resulta em 2 na lista`() {
        val primeiro = gerarESincronizar(ArquetipoEncontro.FISICO)
        val segundo = gerarESincronizar(ArquetipoEncontro.SOCIAL)

        assertNotNull(primeiro)
        assertNotNull(segundo)
        assertNull(viewModel.erroNpcEncontro.value)
        assertEquals(2, viewModel.npcsEncontro.value.size)
        assertTrue(primeiro!!.id != segundo!!.id)
    }

    @Test
    fun `gerar cinco NPCs seguidos, um apos o outro, preenche a lista sem erro`() {
        val arquetipos = listOf(
            ArquetipoEncontro.FISICO, ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL,
            ArquetipoEncontro.FISICO, ArquetipoEncontro.SOCIAL
        )
        arquetipos.forEach { arq -> gerarESincronizar(arq) }

        assertEquals(5, viewModel.npcsEncontro.value.size)
        assertNull(viewModel.erroNpcEncontro.value)
    }


    @Test
    fun `stress gera lunares consecutivos sem crash nem perda de estado`() {
        val quantidade = com.example.data.EncounterTestSamples.count(60)
        repeat(quantidade) { indice ->
            val arquetipo = ArquetipoEncontro.entries[indice % ArquetipoEncontro.entries.size]
            val latch = CountDownLatch(1)
            var resultado: com.example.model.NpcEncontro? = null
            viewModel.gerarNpcLunar(
                nomeManual = "Lunar VM Stress $indice",
                arquetipo = arquetipo,
                onResultado = { npc -> resultado = npc; latch.countDown() }
            )
            assertTrue("Lunar $indice excedeu timeout", latch.await(30, TimeUnit.SECONDS))
            assertNotNull("Lunar $indice retornou null: ${viewModel.erroNpcEncontro.value}", resultado)
        }
        val limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (viewModel.gerandoNpcEncontro.value && System.nanoTime() < limite) Thread.yield()
        assertEquals(quantidade, viewModel.npcsEncontro.value.size)
        assertEquals(quantidade, viewModel.npcsEncontro.value.map { it.id }.distinct().size)
        assertTrue(viewModel.npcsEncontro.value.all { it.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR })
        assertNull(viewModel.erroNpcEncontro.value)
        assertEquals(false, viewModel.gerandoNpcEncontro.value)
    }

    @Test
    fun `rajada concorrente de lunares finaliza todos callbacks e contador de loading`() {
        val quantidade = com.example.data.EncounterTestSamples.count(24)
        val latch = CountDownLatch(quantidade)
        val resultados = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        repeat(quantidade) { indice ->
            viewModel.gerarNpcLunar(
                nomeManual = "Lunar Burst $indice",
                arquetipo = ArquetipoEncontro.entries[indice % ArquetipoEncontro.entries.size],
                onResultado = { npc -> if (npc != null) resultados.add(npc.id); latch.countDown() }
            )
        }
        assertTrue("rajada Lunar excedeu timeout", latch.await(90, TimeUnit.SECONDS))
        val limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (viewModel.gerandoNpcEncontro.value && System.nanoTime() < limite) Thread.yield()
        assertEquals(quantidade, resultados.size)
        assertEquals(quantidade, viewModel.npcsEncontro.value.size)
        assertEquals(false, viewModel.gerandoNpcEncontro.value)
        assertNull(viewModel.erroNpcEncontro.value)
    }
}
