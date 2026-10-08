package com.example.data

import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterInitialSpellLockTest {
    @Test
    fun `primeiro xp fecha gerenciamento inicial permanentemente no modelo`() {
        val inicial = NpcEncontro(tipoExaltado = TipoExaltadoEncontro.SOLAR)
        assertTrue(EncounterNpcSpellManagement.podeGerenciarFeiticoInicial(inicial))
        val depois = inicial.copy(primeiroXpRecebido = true, xpAtual = 0, xpGastoTotal = 0, historicoXpBatches = emptyList())
        assertFalse(EncounterNpcSpellManagement.podeGerenciarFeiticoInicial(depois))
    }
}
