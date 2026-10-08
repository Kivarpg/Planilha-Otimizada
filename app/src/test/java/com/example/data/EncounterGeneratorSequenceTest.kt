package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

// Reproduz diretamente o relato do usuário ("ao gerar outro NPC em
// sequência, o app trava") — chama EncounterGenerator.gerarSolar() repetidas
// vezes, sem passar pela camada de ViewModel/coroutine/UI, isolando se o
// problema é uma exceção real na lógica de geração (o que este teste
// pegaria) ou algo específico do ambiente onde foi observado (emulador
// Nox, por exemplo — o que este teste NÃO seria capaz de reproduzir,
// já que roda em JVM pura via Robolectric, sem GPU nem renderização).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterGeneratorSequenceTest {

    private lateinit var catalogo: List<EncantoSolarDefinition>
    private lateinit var catalogoMeritos: List<MeritoDefinition>

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        catalogo = EncantosSolaresCatalog(app).definitions
        catalogoMeritos = MeritosCatalog(app).definitions
    }

    @Test
    fun `gerar dois NPCs fisicos em sequencia nao lanca excecao`() {
        val primeiro = EncounterGenerator.gerarSolar(
            nomeManual = "",
            arquetipo = ArquetipoEncontro.FISICO, encantosSolares = catalogo, meritosCatalogo = catalogoMeritos
        )
        val segundo = EncounterGenerator.gerarSolar(
            nomeManual = "",
            arquetipo = ArquetipoEncontro.FISICO, encantosSolares = catalogo, meritosCatalogo = catalogoMeritos
        )
        assertTrue(primeiro.id != segundo.id)
    }

    @Test
    fun `gerar cinco NPCs em sequencia, um de cada arquetipo alternado, nao lanca excecao`() {
        val arquetipos = listOf(
            ArquetipoEncontro.FISICO, ArquetipoEncontro.SOCIAL, ArquetipoEncontro.MENTAL,
            ArquetipoEncontro.FISICO, ArquetipoEncontro.SOCIAL
        )
        val gerados = arquetipos.map { arq ->
            EncounterGenerator.gerarSolar(
                nomeManual = "",
                arquetipo = arq, encantosSolares = catalogo, meritosCatalogo = catalogoMeritos
            )
        }
        assertEquals(5, gerados.map { it.id }.toSet().size)
    }

    @Test
    fun `gerar NPC e depois expandir por experiencia em sequencia nao lanca excecao`() {
        var npc = EncounterGenerator.gerarSolar(
            nomeManual = "",
            arquetipo = ArquetipoEncontro.MENTAL, encantosSolares = catalogo, meritosCatalogo = catalogoMeritos
        )
        val totalAntes = npc.charms.size
        npc = EncounterGenerator.expandirEncantosPorExperiencia(npc, catalogo)
        assertTrue(npc.charms.size >= totalAntes)
    }

    @Test
    fun `sangue de dragao social sem feiticaria nao recebe falso alerta de feiticos`() {
        val npc = EncounterGenerator.gerarSangueDeDragao(
            nomeManual = "",
            arquetipo = ArquetipoEncontro.SOCIAL,
            encantosSangueDeDragao = emptyList(),
            feiticos = emptyList(),
            random = Random(777),
            meritosCatalogo = emptyList()
        )

        assertTrue(
            "Social sem acesso à Feitiçaria não deve exigir Feitiços",
            npc.alertasValidacao.none { it.contains("Nenhum Feitiço selecionado") }
        )
    }


}
