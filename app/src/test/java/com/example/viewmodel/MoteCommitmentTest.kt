package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// LOGICA: cobre o fluxo de comitamento de motes entre a Aba 7
// (Equipamentos) e a Aba 5 (Combate). Regra de dependência: Comitar é
// independente (não equipa automaticamente); Equipar EXIGE que a arma já
// esteja Comitada (para Artefato — Mundana nunca precisa comitar). Desequipar
// NÃO libera os motes (Comitar persiste); só Descomitar libera, e ao
// descomitar a arma também é desequipada (não dá pra ficar equipada sem
// estar comitada). Existiam bugs reais nesse fluxo antes desta sessão;
// estes testes existem para pegar cedo qualquer regressão futura.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MoteCommitmentTest {

    private lateinit var viewModel: SheetViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = SheetViewModel(application)
    }

    private fun ultimaArmaAdicionada() = viewModel.sheetState.value.weapons.last()
    private fun ultimaArmaduraAdicionada() = viewModel.sheetState.value.armaduras.last()

    @Test
    fun `comitar arma artefato desconta motes mas nao equipa automaticamente`() {
        val motesAntes = viewModel.sheetState.value.motesPessoaisDisponiveis()
        viewModel.addWeapon("Espada Solar", "Armas Brancas", null, "Artefato", "Média")
        val arma = ultimaArmaAdicionada()

        val ok = viewModel.equipWeaponWithMoteSource(arma.id, motesPessoais = 5, motesPerifericos = 0)

        assertEquals(true, ok)
        assertEquals(motesAntes - 5, viewModel.sheetState.value.motesPessoaisDisponiveis())
        // Comitar não deve marcar Equipar automaticamente.
        assertEquals(false, viewModel.sheetState.value.weapons.first { it.id == arma.id }.equipada)
    }

    @Test
    fun `equipar arma artefato sem comitar antes e bloqueado`() {
        viewModel.addWeapon("Espada Solar", "Armas Brancas", null, "Artefato", "Média")
        val arma = ultimaArmaAdicionada()

        viewModel.toggleWeaponEquipped(arma.id)

        assertEquals(false, viewModel.sheetState.value.weapons.first { it.id == arma.id }.equipada)
    }

    @Test
    fun `equipar arma artefato apos comitar funciona e nao mexe nos motes`() {
        viewModel.addWeapon("Espada Solar", "Armas Brancas", null, "Artefato", "Média")
        val arma = ultimaArmaAdicionada()
        viewModel.equipWeaponWithMoteSource(arma.id, motesPessoais = 5, motesPerifericos = 0)
        val motesAposComitar = viewModel.sheetState.value.motesPessoaisDisponiveis()

        viewModel.toggleWeaponEquipped(arma.id)

        assertEquals(true, viewModel.sheetState.value.weapons.first { it.id == arma.id }.equipada)
        assertEquals(motesAposComitar, viewModel.sheetState.value.motesPessoaisDisponiveis())
    }

    @Test
    fun `desequipar arma comitada nao libera os motes`() {
        val motesAntes = viewModel.sheetState.value.motesPessoaisDisponiveis()
        viewModel.addWeapon("Espada Solar", "Armas Brancas", null, "Artefato", "Média")
        val arma = ultimaArmaAdicionada()
        viewModel.equipWeaponWithMoteSource(arma.id, motesPessoais = 5, motesPerifericos = 0)
        viewModel.toggleWeaponEquipped(arma.id) // equipa (já comitada)

        viewModel.toggleWeaponEquipped(arma.id) // desequipa

        assertEquals(false, viewModel.sheetState.value.weapons.first { it.id == arma.id }.equipada)
        // Comitar persiste — motes continuam descontados mesmo desequipada.
        assertEquals(motesAntes - 5, viewModel.sheetState.value.motesPessoaisDisponiveis())
    }

    @Test
    fun `descomitar devolve os motes e tambem desequipa`() {
        val motesAntes = viewModel.sheetState.value.motesPessoaisDisponiveis()
        viewModel.addWeapon("Espada Solar", "Armas Brancas", null, "Artefato", "Média")
        val arma = ultimaArmaAdicionada()
        viewModel.equipWeaponWithMoteSource(arma.id, motesPessoais = 5, motesPerifericos = 0)
        viewModel.toggleWeaponEquipped(arma.id) // equipa

        viewModel.descomitarArma(arma.id)

        assertEquals(motesAntes, viewModel.sheetState.value.motesPessoaisDisponiveis())
        // Invariante "Equipar exige Comitar": descomitar também desequipa.
        assertEquals(false, viewModel.sheetState.value.weapons.first { it.id == arma.id }.equipada)
    }

    @Test
    fun `trocar de armadura devolve os motes da anterior antes de debitar a nova`() {
        val motesAntes = viewModel.sheetState.value.motesPessoaisDisponiveis()

        viewModel.addArmor("Couraça Antiga", "Artefato", "Leve") // comitamento 4
        val armaduraA = ultimaArmaduraAdicionada()
        viewModel.equipArmorWithMoteSource(armaduraA.id, motesPessoais = 4, motesPerifericos = 0)
        assertEquals(motesAntes - 4, viewModel.sheetState.value.motesPessoaisDisponiveis())

        viewModel.addArmor("Couraça Nova", "Artefato", "Média") // comitamento 5
        val armaduraB = ultimaArmaduraAdicionada()
        viewModel.equipArmorWithMoteSource(armaduraB.id, motesPessoais = 5, motesPerifericos = 0)

        // Se a armadura A não tivesse devolvido seus motes, o disponível
        // aqui seria motesAntes - 4 - 5 = motesAntes - 9 (vazamento).
        assertEquals(motesAntes - 5, viewModel.sheetState.value.motesPessoaisDisponiveis())
    }

    @Test
    fun `desequipar armadura sem substituir devolve seus motes`() {
        val motesAntes = viewModel.sheetState.value.motesPessoaisDisponiveis()
        viewModel.addArmor("Couraça Antiga", "Artefato", "Leve")
        val armadura = ultimaArmaduraAdicionada()
        viewModel.equipArmorWithMoteSource(armadura.id, motesPessoais = 4, motesPerifericos = 0)

        viewModel.toggleArmorEquipped(armadura.id)

        assertEquals(motesAntes, viewModel.sheetState.value.motesPessoaisDisponiveis())
    }

    @Test
    fun `arma mundana equipa livremente sem precisar comitar`() {
        val motesAntes = viewModel.sheetState.value.motesPessoaisDisponiveis()
        viewModel.addWeapon("Adaga Simples", "Armas Brancas", null, "Mundana", "Leve")
        val arma = ultimaArmaAdicionada()

        viewModel.toggleWeaponEquipped(arma.id)

        assertEquals(true, viewModel.sheetState.value.weapons.first { it.id == arma.id }.equipada)
        assertEquals(motesAntes, viewModel.sheetState.value.motesPessoaisDisponiveis())
    }
}
