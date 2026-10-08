package com.example.data

import com.example.model.CharacterSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections

class SheetPersistenceCoordinatorTest {
    @Test
    fun serializaESempreTerminaComOEstadoMaisRecente() = runBlocking {
        val escritos = Collections.synchronizedList(mutableListOf<CharacterSheet>())
        val coordinator = SheetPersistenceCoordinator(
            write = { sheet ->
                delay(10)
                escritos += sheet
            },
            onFailure = { throw it }
        )
        val primeiro = CharacterSheet()
        val segundo = primeiro.copy(id = "mais-recente")
        coordinator.submit(primeiro)
        coordinator.submit(segundo)
        delay(100)
        coordinator.close()
        assertEquals("mais-recente", escritos.last().id)
    }

    @Test
    fun ignoraSubmitDuplicadoEnquantoOEstadoEstaPendente() = runBlocking {
        var quantidade = 0
        val coordinator = SheetPersistenceCoordinator(
            write = { quantidade++ },
            onFailure = { throw it }
        )
        val sheet = CharacterSheet()
        coordinator.submit(sheet)
        coordinator.submit(sheet)
        delay(50)
        coordinator.close()
        assertEquals(1, quantidade)
    }
}
