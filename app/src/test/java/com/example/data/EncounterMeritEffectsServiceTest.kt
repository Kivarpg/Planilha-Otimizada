package com.example.data

import com.example.model.CaixaVitalidade
import com.example.model.Merito
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterMeritEffectsServiceTest {
    private fun merito(nome: String, valor: Int = 1) = Merito(
        id = nome,
        nome = nome,
        valor = valor,
        categoria = "normal"
    )

    @Test
    fun reflexosRapidosConcedeUmDadoDeJuntarABatalha() {
        assertEquals(1, EncounterMeritEffectsService.bonusJuntarABatalha(listOf(merito("Reflexos Rápidos"))))
        assertEquals(0, EncounterMeritEffectsService.bonusJuntarABatalha(emptyList()))
    }

    @Test
    fun peVelozConcedeUmDadoNasAcoesDeMovimentoRepresentadas() {
        val merits = listOf(merito("Pé Veloz"))
        assertEquals(1, EncounterMeritEffectsService.bonusInvestida(merits))
        assertEquals(1, EncounterMeritEffectsService.bonusDesengajamento(merits))
    }


    @Test
    fun gigantePermanecePermanenteAoReconstruirTrilha() {
        val base = listOf(
            CaixaVitalidade(penalidade = "-0"),
            CaixaVitalidade(penalidade = "-1"),
            CaixaVitalidade(penalidade = "Inc")
        )
        val reconstruida = EncounterMeritEffectsService.adicionarVitalidade(
            listOf(merito("Gigante")),
            base
        )

        assertEquals(4, reconstruida.size)
        assertEquals("-0", reconstruida.last().penalidade)
        assertTrue(reconstruida.last().isPermanente)
    }

    @Test
    fun giganteAdicionaUmNivelZero() {
        val base = listOf(CaixaVitalidade(penalidade = "-0"))
        val sem = EncounterMeritEffectsService.adicionarVitalidade(emptyList(), base)
        val com = EncounterMeritEffectsService.adicionarVitalidade(listOf(merito("Gigante")), base)
        assertEquals(1, sem.size)
        assertEquals(2, com.size)
        assertTrue(com.last().penalidade == "-0")
    }

    @Test
    fun toleranciaADorReduzSomentePenalidadesDeFerimentoIndicadas() {
        val merits = listOf(merito("Tolerância à dor"))
        assertEquals(1, EncounterMeritEffectsService.penalidadeFerimentoEfetiva(merits, "-2"))
        assertEquals(3, EncounterMeritEffectsService.penalidadeFerimentoEfetiva(merits, "-4"))
        assertEquals(4, EncounterMeritEffectsService.penalidadeFerimentoEfetiva(merits, "Inc"))
        assertEquals(1, EncounterMeritEffectsService.penalidadeFerimentoEfetiva(merits, "-1"))
        assertEquals(4, EncounterMeritEffectsService.penalidadeFerimentoEfetiva(emptyList(), "-4"))
    }

    @Test
    fun snapshotDeEfeitosConsolidaTodosOsBonusEmUmaLeitura() {
        val efeitos = EncounterMeritEffectsService.efeitos(
            listOf(
                merito("Reflexos Rápidos"),
                merito("Pé Veloz"),
                merito("Gigante"),
                merito("Tolerância à dor")
            )
        )

        assertEquals(1, efeitos.bonusJuntarABatalha)
        assertEquals(1, efeitos.bonusInvestida)
        assertEquals(1, efeitos.bonusDesengajamento)
        assertTrue(efeitos.temGigante)
        assertTrue(efeitos.temToleranciaADor)
    }

    @Test
    fun meritosSemEfeitoRepresentavelNaoGanhamBonusInventado() {
        val merits = listOf(merito("Sentido de Perigo"), merito("Músculos Poderosos"), merito("Saque Rápido"))
        assertEquals(0, EncounterMeritEffectsService.bonusJuntarABatalha(merits))
        assertEquals(0, EncounterMeritEffectsService.bonusInvestida(merits))
        assertEquals(0, EncounterMeritEffectsService.bonusDesengajamento(merits))
        assertFalse(EncounterMeritEffectsService.adicionarVitalidade(merits, listOf(CaixaVitalidade(penalidade = "-0"))).size > 1)
    }

    @Test
    fun adicionarVitalidadeComSnapshotMantemMesmoResultado() {
        val merits = listOf(merito("Gigante"))
        val base = listOf(CaixaVitalidade(penalidade = "-0"))
        val efeitos = EncounterMeritEffectsService.efeitos(merits)

        val viaLista = EncounterMeritEffectsService.adicionarVitalidade(merits, base)
        val viaSnapshot = EncounterMeritEffectsService.adicionarVitalidade(efeitos, base)

        assertEquals(viaLista.size, viaSnapshot.size)
        assertEquals(viaLista.map { it.penalidade }, viaSnapshot.map { it.penalidade })
        assertEquals(viaLista.map { it.isPermanente }, viaSnapshot.map { it.isPermanente })
    }
}
