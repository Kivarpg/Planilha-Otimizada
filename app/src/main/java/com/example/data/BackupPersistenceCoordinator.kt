package com.example.data

import com.example.model.CharacterSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Mantém a gravação de backups periódicos fora da thread chamadora. */
class BackupPersistenceCoordinator(
    private val write: suspend (CharacterSheet) -> Unit,
    private val onFailure: (Exception) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var pending: CharacterSheet? = null
    private var running = false
    private var lastPersisted: CharacterSheet? = null

    @Synchronized
    fun submit(sheet: CharacterSheet) {
        if (pending == sheet || lastPersisted == sheet) return
        pending = sheet
        if (running) return
        running = true
        launchWorkerLocked()
    }

    @Synchronized
    private fun launchWorkerLocked() {
        scope.launch {
            while (true) {
                val snapshot = synchronized(this@BackupPersistenceCoordinator) {
                    val value = pending
                    pending = null
                    value
                }
                if (snapshot != null) {
                    try {
                        write(snapshot)
                        synchronized(this@BackupPersistenceCoordinator) {
                            lastPersisted = snapshot
                        }
                    } catch (e: Exception) {
                        onFailure(e)
                    }
                    continue
                }

                val restart = synchronized(this@BackupPersistenceCoordinator) {
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
    fun close() {
        pending = null
        running = false
        lastPersisted = null
        scope.cancel()
    }
}
