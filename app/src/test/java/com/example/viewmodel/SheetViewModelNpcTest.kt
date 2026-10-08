package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Cobre addNpc()/removeNpc() no nível do ViewModel — CRUD simples, mas
// vale confirmar que a lista realmente persiste via repository
// (diferente da CharacterSheet ativa, NPCs gravam a cada alteração, sem
// debounce — ver comentário em SheetViewModel.kt).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SheetViewModelNpcTest {

    private lateinit var viewModel: SheetViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = SheetViewModel(app)
    }

    @Test
    fun `lista de npcs comeca vazia`() {
        assertTrue(viewModel.npcs.value.isEmpty())
    }

    @Test
    fun `addNpc adiciona um npc com os campos informados`() {
        viewModel.addNpc("Capitão Vento-do-Norte", "Aliado", "Mortal veterano", "Comanda a guarda do porto.")
        val lista = viewModel.npcs.value
        assertEquals(1, lista.size)
        assertEquals("Capitão Vento-do-Norte", lista.first().nome)
        assertEquals("Aliado", lista.first().lealdade)
        assertEquals("Mortal veterano", lista.first().tipo)
        assertEquals("Comanda a guarda do porto.", lista.first().descricao)
    }

    @Test
    fun `addNpc com nome em branco nao adiciona nada`() {
        viewModel.addNpc("   ", "Aliado", "", "")
        assertTrue(viewModel.npcs.value.isEmpty())
    }

    @Test
    fun `addNpc aceita multiplos npcs`() {
        viewModel.addNpc("Alfa", "Aliado", "", "")
        viewModel.addNpc("Beta", "Inimigo", "", "")
        viewModel.addNpc("Gama", "Neutro", "", "")
        assertEquals(3, viewModel.npcs.value.size)
    }

    @Test
    fun `removeNpc remove pelo id correto, mantendo os outros`() {
        viewModel.addNpc("Alfa", "Aliado", "", "")
        viewModel.addNpc("Beta", "Inimigo", "", "")
        val idParaRemover = viewModel.npcs.value.first { it.nome == "Alfa" }.id

        viewModel.removeNpc(idParaRemover)

        val restantes = viewModel.npcs.value
        assertEquals(1, restantes.size)
        assertEquals("Beta", restantes.first().nome)
    }

    @Test
    fun `addNpc persiste no repository, sobrevivendo a nova instancia do ViewModel`() {
        viewModel.addNpc("Persistente", "Neutro", "", "")

        // Nova instância do ViewModel simula reabrir o app — deve
        // carregar do repository, não começar vazia.
        val app = ApplicationProvider.getApplicationContext<Application>()
        val novaInstancia = SheetViewModel(app)

        assertEquals(1, novaInstancia.npcs.value.size)
        assertEquals("Persistente", novaInstancia.npcs.value.first().nome)
    }
}
