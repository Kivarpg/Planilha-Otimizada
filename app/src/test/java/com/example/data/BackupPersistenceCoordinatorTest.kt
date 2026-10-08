package com.example.data

import com.example.model.CharacterSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections

class BackupPersistenceCoordinatorTest {
    @Test
    fun `backup coalesce estados e termina com o mais recente`() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<CharacterSheet>())
        val coordinator = BackupPersistenceCoordinator(
            write = { sheet ->
                delay(10)
                escritos += sheet
            },
            onFailure = { throw it }
        )
        val primeiro = CharacterSheet(id = "backup-1")
        val segundo = CharacterSheet(id = "backup-2")
        coordinator.submit(primeiro)
        coordinator.submit(segundo)
        delay(100)
        coordinator.close()
        assertEquals("backup-2", escritos.last().id)
    }

    @Test
    fun `backup duplicado nao gera segunda gravacao`() = runBlocking {
        var quantidade = 0
        val coordinator = BackupPersistenceCoordinator(
            write = { quantidade++ },
            onFailure = { throw it }
        )
        val sheet = CharacterSheet(id = "mesmo")
        coordinator.submit(sheet)
        delay(50)
        coordinator.submit(sheet)
        delay(50)
        coordinator.close()
        assertEquals(1, quantidade)
    }
}
