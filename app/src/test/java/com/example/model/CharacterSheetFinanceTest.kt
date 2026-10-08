package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Test

// Testes das contas de motes/comitamento do personagem — funções puras,
// sem dependência de Context Android. Cobrem especificamente a fórmula de
// máximos por Essência e o efeito de gastos/comitamento sobre o
// disponível, para pegar cedo qualquer regressão numérica silenciosa
// nessas contas (o tipo de bug que não quebra a compilação nem a tela).
class CharacterSheetFinanceTest {

    @Test
    fun `motes pessoais maximo segue a formula essencia vezes 3 mais 10`() {
        val sheet = CharacterSheet(essencia = 3)
        assertEquals(19, sheet.motesPessoaisMax())
    }

    @Test
    fun `motes perifericos maximo segue a formula essencia vezes 7 mais 26`() {
        val sheet = CharacterSheet(essencia = 3)
        assertEquals(47, sheet.motesPerifericosMax())
    }

    @Test
    fun `motes disponiveis desconta o gasto do maximo`() {
        val sheet = CharacterSheet(essencia = 3, motesPessoaisGastos = 5)
        assertEquals(14, sheet.motesPessoaisDisponiveis())
    }

    @Test
    fun `motes disponiveis nunca fica negativo mesmo com gasto acima do maximo`() {
        val sheet = CharacterSheet(essencia = 1, motesPessoaisGastos = 999)
        assertEquals(0, sheet.motesPessoaisDisponiveis())
    }

    @Test
    fun `limite de comitamento e a soma dos dois maximos de motes`() {
        val sheet = CharacterSheet(essencia = 2)
        assertEquals(sheet.motesPessoaisMax() + sheet.motesPerifericosMax(), sheet.limiteComitamentoCalculado())
    }

    @Test
    fun `comitamento total soma motes realmente comitados nas armas e armadura equipada`() {
        val artefato = Arma(nome = "Espada", tipoArma = "Artefato", equipada = true, motesPessoaisComitados = 5)
        val mundana = Arma(nome = "Adaga", tipoArma = "Mundana", equipada = true)
        val armaduraArtefato = Armadura(nome = "Couraça", tipoArmadura = "Artefato", categoriaPeso = "Média", equipada = true)
        val sheet = CharacterSheet(weapons = listOf(artefato, mundana), armaduras = listOf(armaduraArtefato))
        // Espada comitada = 5 motes; Adaga Mundana nunca comita = 0; armadura
        // Artefato Média = 5 (ver ArmorStatsTable.stats) — armadura não separa
        // Equipar de Comitar, então equipada já implica o custo cheio.
        assertEquals(10, sheet.comitamentoTotalCalculado())
    }

    @Test
    fun `comitamento total ignora arma equipada mas nao comitada`() {
        val equipadaSemComitar = Arma(nome = "Espada", tipoArma = "Artefato", equipada = true)
        val comitada = Arma(nome = "Lança", tipoArma = "Artefato", equipada = true, motesPessoaisComitados = 5)
        val sheet = CharacterSheet(weapons = listOf(equipadaSemComitar, comitada))
        // Espada está equipada mas não comitada (Equipar e Comitar são ações
        // separadas) — não deve contar. Só a Lança, de fato comitada, conta.
        assertEquals(5, sheet.comitamentoTotalCalculado())
    }

    @Test
    fun `arma com ataque desarmado nunca custa comitamento mesmo sendo artefato`() {
        val desarmado = Arma(nome = "Punhos", tipoArma = "Artefato", equipada = true, ataqueDesarmado = true)
        val sheet = CharacterSheet(weapons = listOf(desarmado))
        assertEquals(0, sheet.comitamentoTotalCalculado())
    }
}
