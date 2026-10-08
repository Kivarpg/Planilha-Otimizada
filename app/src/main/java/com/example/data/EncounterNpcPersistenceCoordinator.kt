package com.example.data

import com.example.model.NpcEncontro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serializa/coalesce a persistência dos NPCs de Encontros fora da thread da UI.
 *
 * A Aba 11 pode alterar o mesmo conjunto de NPCs várias vezes em sequência
 * (vitalidade, vontade, iniciativa etc.). Persistir e codificar a lista inteira
 * a cada toque no thread principal pode causar ANR. Mantemos somente o snapshot
 * mais recente enquanto uma gravação estiver em andamento.
 */
class EncounterNpcPersistenceCoordinator(
    private val write: suspend (List<NpcEncontro>) -> Unit,
    private val onFailure: (Exception) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pending: List<NpcEncontro>? = null
    private var running = false
    private var inFlight: List<NpcEncontro>? = null
    private var lastPersisted: List<NpcEncontro>? = null
    private var closed = false

    @Synchronized
    fun submit(npcs: List<NpcEncontro>) {
        if (closed) return
        if (pending == npcs || lastPersisted == npcs) return
        pending = npcs.toList()
        if (running) return
        running = true
        launchWorkerLocked()
    }

    @Synchronized
    private fun launchWorkerLocked() {
        scope.launch {
            while (true) {
                val snapshot = synchronized(this@EncounterNpcPersistenceCoordinator) {
                    val value = pending
                    pending = null
                    inFlight = value
                    value
                }

                if (snapshot != null) {
                    try {
                        write(snapshot)
                        synchronized(this@EncounterNpcPersistenceCoordinator) {
                            lastPersisted = snapshot
                            if (inFlight == snapshot) inFlight = null
                        }
                    } catch (e: Exception) {
                        synchronized(this@EncounterNpcPersistenceCoordinator) {
                            if (inFlight == snapshot) inFlight = null
                        }
                        onFailure(e)
                    }
                    continue
                }

                val restart = synchronized(this@EncounterNpcPersistenceCoordinator) {
                    if (pending != null) {
                        true
                    } else {
                        running = false
                        false
                    }
                }
                if (!restart) return@launch
            }
        }
    }

    @Synchronized
    fun latestPending(): List<NpcEncontro>? = pending ?: inFlight ?: lastPersisted

    fun close() {
        val snapshot = synchronized(this) {
            if (closed) return
            closed = true
            // pending é sempre mais novo que inFlight quando ambos existem.
            val value = pending ?: inFlight
            pending = null
            inFlight = null
            running = false
            value
        }
        scope.cancel()
        if (snapshot != null && snapshot != lastPersisted) {
            try {
                // onCleared pode ocorrer logo após um submit. Depois de cancelar
                // o worker, preserva o último snapshot ainda não gravado em vez
                // de descartá-lo silenciosamente.
                kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    write(snapshot)
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
        synchronized(this) {
            lastPersisted = null
        }
    }
}
