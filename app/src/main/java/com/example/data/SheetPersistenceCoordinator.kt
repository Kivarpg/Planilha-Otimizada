package com.example.data

import com.example.model.CharacterSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serializa a persistência da planilha ativa em Dispatchers.IO.
 * Mantém apenas o snapshot mais recente e evita a condição de corrida entre
 * o último submit e o término da coroutine anterior.
 */
class SheetPersistenceCoordinator(
    private val write: suspend (CharacterSheet) -> Unit,
    private val onFailure: (Exception) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var pendingSheet: CharacterSheet? = null

    @Volatile
    private var writeJob: Job? = null

    @Volatile
    private var lastPersistedSheet: CharacterSheet? = null

    @Synchronized
    fun submit(sheet: CharacterSheet) {
        if (pendingSheet == sheet || lastPersistedSheet == sheet) return
        pendingSheet = sheet
        ensureWorkerLocked()
    }

    @Synchronized
    private fun ensureWorkerLocked() {
        if (writeJob?.isActive == true) return
        writeJob = scope.launch {
            persistPending()
        }
    }

    private suspend fun persistPending() {
        while (true) {
            val sheet = synchronized(this@SheetPersistenceCoordinator) {
                val value = pendingSheet
                pendingSheet = null
                value
            } ?: run {
                synchronized(this@SheetPersistenceCoordinator) {
                    writeJob = null
                    if (pendingSheet != null) ensureWorkerLocked()
                }
                return
            }
            try {
                write(sheet)
                synchronized(this@SheetPersistenceCoordinator) {
                    lastPersistedSheet = sheet
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    @Synchronized
    fun latestPending(): CharacterSheet? = pendingSheet

    @Synchronized
    fun close() {
        pendingSheet = null
        writeJob = null
        lastPersistedSheet = null
        scope.cancel()
    }
}
