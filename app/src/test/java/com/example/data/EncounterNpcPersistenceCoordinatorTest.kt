package com.example.data

import com.example.model.NpcEncontro
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections

class EncounterNpcPersistenceCoordinatorTest {
    @Test
    fun coalesceAlteracoesRapidasEGravaEstadoMaisRecente() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<List<NpcEncontro>>())
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { npcs ->
                delay(20)
                escritos += npcs
            },
            onFailure = { throw it },
        )
        val primeiro = listOf(NpcEncontro(id = "primeiro"))
        val segundo = listOf(NpcEncontro(id = "segundo"))

        coordinator.submit(primeiro)
        coordinator.submit(segundo)
        delay(100)
        coordinator.close()

        assertEquals("segundo", escritos.last().single().id)
    }

    @Test
    fun ignoraSnapshotDuplicado() = runBlocking {
        var quantidade = 0
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { quantidade++ },
            onFailure = { throw it },
        )
        val npcs = listOf(NpcEncontro(id = "mesmo"))

        coordinator.submit(npcs)
        coordinator.submit(npcs)
        delay(50)
        coordinator.close()

        assertEquals(1, quantidade)
    }

    @Test
    fun mutacaoDuranteGravacaoPreservaOSnapshotMaisRecente() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<List<NpcEncontro>>())
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { npcs ->
                delay(30)
                escritos += npcs
            },
            onFailure = { throw it },
        )
        coordinator.submit(listOf(NpcEncontro(id = "primeiro")))
        delay(5)
        coordinator.submit(listOf(NpcEncontro(id = "intermediario")))
        coordinator.submit(listOf(NpcEncontro(id = "final")))
        delay(120)
        coordinator.close()

        assertEquals("final", escritos.last().single().id)
        assertEquals("primeiro", escritos.first().single().id)
    }


    @Test
    fun snapshotQueFalhouPodeSerReenviado() = runBlocking {
        var tentativas = 0
        val falhas = Collections.synchronizedList(mutableListOf<Exception>())
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = {
                tentativas++
                if (tentativas == 1) error("falha simulada")
            },
            onFailure = { falhas += it },
        )
        val npcs = listOf(NpcEncontro(id = "retry"))

        coordinator.submit(npcs)
        delay(50)
        coordinator.submit(npcs)
        delay(50)
        coordinator.close()

        assertEquals(2, tentativas)
        assertEquals(1, falhas.size)
    }


    @Test
    fun closePreservaUltimoSnapshotPendente() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<List<NpcEncontro>>())
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { npcs ->
                delay(100)
                escritos += npcs
            },
            onFailure = { throw it },
        )
        val primeiro = listOf(NpcEncontro(id = "primeiro"))
        val final = listOf(NpcEncontro(id = "final"))

        coordinator.submit(primeiro)
        coordinator.submit(final)
        coordinator.close()

        assertEquals("final", escritos.last().single().id)
    }


    @Test
    fun closePreservaSnapshotJaRetiradoDaFilaPeloWorker() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<List<NpcEncontro>>())
        val iniciou = kotlinx.coroutines.CompletableDeferred<Unit>()
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { npcs ->
                iniciou.complete(Unit)
                delay(5_000)
                escritos += npcs
            },
            onFailure = { throw it },
        )
        val emVoo = listOf(NpcEncontro(id = "em-voo"))

        coordinator.submit(emVoo)
        iniciou.await()
        coordinator.close()

        assertEquals("em-voo", escritos.last().single().id)
    }


    @Test
    fun closePreferePendenteMaisNovoAoSnapshotEmVoo() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<List<NpcEncontro>>())
        val iniciouPrimeiro = kotlinx.coroutines.CompletableDeferred<Unit>()
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { npcs ->
                if (npcs.single().id == "antigo") {
                    iniciouPrimeiro.complete(Unit)
                    delay(5_000)
                }
                escritos += npcs
            },
            onFailure = { throw it },
        )

        coordinator.submit(listOf(NpcEncontro(id = "antigo")))
        iniciouPrimeiro.await()
        coordinator.submit(listOf(NpcEncontro(id = "novo")))
        coordinator.close()

        assertEquals("novo", escritos.last().single().id)
        assertEquals(1, escritos.count { it.single().id == "novo" })
    }


    @Test
    fun latestPendingEnxergaSnapshotEnquantoWorkerEstaGravando() = runBlocking {
        val iniciou = kotlinx.coroutines.CompletableDeferred<Unit>()
        val liberar = kotlinx.coroutines.CompletableDeferred<Unit>()
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = {
                iniciou.complete(Unit)
                liberar.await()
            },
            onFailure = { throw it },
        )
        val atual = listOf(NpcEncontro(id = "atual"))

        coordinator.submit(atual)
        iniciou.await()

        assertEquals("atual", coordinator.latestPending()?.single()?.id)

        liberar.complete(Unit)
        delay(20)
        coordinator.close()
    }


    @Test
    fun closeEhIdempotenteERejeitaNovasSubmissoes() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<List<NpcEncontro>>())
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { escritos += it },
            onFailure = { throw it },
        )

        coordinator.submit(listOf(NpcEncontro(id = "antes")))
        delay(20)
        coordinator.close()
        coordinator.close()
        coordinator.submit(listOf(NpcEncontro(id = "depois")))
        delay(20)

        assertEquals("antes", escritos.last().single().id)
        assertEquals(null, coordinator.latestPending())
    }

    @Test
    fun retornoAoSnapshotAnteriorNaoPodeSerDescartadoDuranteOutraGravacao() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<List<NpcEncontro>>())
        val iniciouSegundo = kotlinx.coroutines.CompletableDeferred<Unit>()
        val liberarSegundo = kotlinx.coroutines.CompletableDeferred<Unit>()
        val coordinator = EncounterNpcPersistenceCoordinator(
            write = { npcs ->
                if (npcs.single().id == "segundo") {
                    iniciouSegundo.complete(Unit)
                    liberarSegundo.await()
                }
                escritos += npcs
            },
            onFailure = { throw it },
        )
        val primeiro = listOf(NpcEncontro(id = "primeiro"))
        coordinator.submit(primeiro)
        // Garante que o primeiro snapshot já foi persistido antes do segundo.
        repeat(100) {
            if (escritos.isNotEmpty()) return@repeat
            delay(5)
        }
        assertEquals("primeiro", escritos.last().single().id)
        coordinator.submit(listOf(NpcEncontro(id = "segundo")))
        iniciouSegundo.await()
        coordinator.submit(primeiro)
        liberarSegundo.complete(Unit)
        coordinator.close()
        assertEquals("primeiro", escritos.last().single().id)
    }

}
