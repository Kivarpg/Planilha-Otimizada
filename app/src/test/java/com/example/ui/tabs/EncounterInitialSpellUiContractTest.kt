package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Contrato da gaveta de Feiticos da Aba 11, independente de testes de Compose. */
class EncounterInitialSpellUiContractTest {
    private val source = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()

    @Test
    fun `initial spell manager is shown only before first xp`() {
        assertTrue(source.contains("if (feiticosDisponiveis.isNotEmpty() && podeGerenciarFeiticoInicial)"))
        assertTrue(source.contains("if (gerenciarFeiticos && podeGerenciarFeiticoInicial)"))
    }

    @Test
    fun `spell details remain accessible after initial manager is locked`() {
        assertTrue(source.contains("viewModel.feiticoDefinitionPorNome(feitico.nome)"))
        assertTrue(source.contains("FeiticoDetailsDialog("))
        assertTrue(source.contains("npc.feiticos.forEach { feitico ->"))
    }
}
