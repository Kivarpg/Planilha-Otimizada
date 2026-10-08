package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.CharacterSheet
import com.example.model.Encanto
import com.example.model.Merito
import com.example.model.ExaltedConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Cobre computeCompletionIssues()/isSheetComplete() — a lógica que decide
// se a Planilha pode ser marcada como concluída (Aba 9). Cada teste isola
// UMA das 7 regras, deixando as outras 6 no estado mínimo que já sabemos
// disparar issue própria — o objetivo é garantir que cada regra dispara
// sob a chave de aba certa quando (e só quando) sua condição específica
// falha, protegendo contra regressão silenciosa se a fórmula de algum
// requisito mudar no futuro sem querer.
//
// Não há aqui um teste de "planilha 100% completa, zero pendências": a
// fórmula de Pontos de Bônus (calculateBpBreakdown) é combinatória o
// bastante (atributos, habilidades com prioridade Casta/Favorecida,
// especializações, força de vontade, méritos, poderes) que construir à
// mão uma planilha zerando o saldo tem risco real de erro de cálculo — um
// teste errado que "passa" por acidente é pior que a lacuna de cobertura.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SheetViewModelCompletionTest {

    private lateinit var viewModel: SheetViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = SheetViewModel(app)
    }

    @Test
    fun `planilha totalmente em branco tem pendencias e nao esta completa`() {
        val issues = viewModel.computeCompletionIssues(CharacterSheet())
        assertTrue(issues.isNotEmpty())
        assertFalse(viewModel.isSheetComplete(CharacterSheet()))
    }

    @Test
    fun `casta incompleta gera pendencia agrupada em Aba 2`() {
        val sheet = CharacterSheet(
            casteAbilities = listOf("Presença"), // so 1 de 5
            supernalAbility = null,
            favoredAbilities = emptyList()
        )
        val issues = viewModel.computeCompletionIssues(sheet)
        assertTrue(issues.containsKey("Aba 2 — Casta"))
        // as 3 sub-regras de Casta (habilidades de casta, supernal,
        // favorecidas) devem aparecer todas sob a MESMA chave de aba, nao
        // uma chave por sub-regra — e o requisito real de agrupamento.
        assertEquals(3, issues["Aba 2 — Casta"]?.size)
    }

    @Test
    fun `casta completa nao gera pendencia de Aba 2`() {
        val sheet = CharacterSheet(
            casteAbilities = listOf("Presença", "Integridade", "Performance", "Prontidão", "Ofícios"),
            supernalAbility = "Presença",
            favoredAbilities = listOf("Furtividade", "Investigação", "Ocultismo", "Medicina", "Guerra")
        )
        val issues = viewModel.computeCompletionIssues(sheet)
        assertFalse(issues.containsKey("Aba 2 — Casta"))
    }

    @Test
    fun `atributos com pontos nao distribuidos gera pendencia por grupo`() {
        // Planilha em branco: attributes vazio -> todos os 3 grupos ficam no
        // minimo (1 em cada atributo), sem nenhum ponto extra distribuido.
        val issues = viewModel.computeCompletionIssues(CharacterSheet())
        val atributos = issues["Aba 3 — Atributos"]
        assertTrue(atributos != null && atributos.size == 3)
    }

    @Test
    fun `atributos com grupo Fisicos completo nao aparece na pendencia`() {
        // Prioridade padrao: Fisicos=1o (8 pontos). Distribui os 8 pontos
        // entre Forca/Destreza/Vigor (rating 1 = base, entao +7 leva a
        // soma de acrescimos a 8: 4+3+1 acima do base 1 cada).
        val sheet = CharacterSheet(
            attributes = mapOf("Força" to 4, "Destreza" to 4, "Vigor" to 3)
        )
        val issues = viewModel.computeCompletionIssues(sheet)
        val mensagens = issues["Aba 3 — Atributos"] ?: emptyList()
        assertFalse(mensagens.any { it.contains("Físicos") })
        // Sociais e Mentais continuam pendentes (nao mexemos neles)
        assertTrue(mensagens.any { it.contains("Sociais") })
        assertTrue(mensagens.any { it.contains("Mentais") })
    }

    @Test
    fun `habilidades com menos de 28 pontos gera pendencia em Aba 4`() {
        val sheet = CharacterSheet(abilities = mapOf("Armas Brancas" to 3))
        val issues = viewModel.computeCompletionIssues(sheet)
        assertTrue(issues.containsKey("Aba 4 — Habilidades"))
        assertTrue(issues["Aba 4 — Habilidades"]!!.first().contains("3/28"))
    }

    @Test
    fun `habilidades com 28 pontos distribuidos nao gera pendencia`() {
        // 28 pontos em 1 habilidade so (acima de 3 nao conta pro total —
        // a regra usa coerceAtMost(3) por habilidade, entao precisa
        // espalhar entre varias pra bater 28 de verdade).
        val abilities = ExaltedConstants.ALL_25_ABILITIES.take(10)
            .associateWith { 3 } // 10 habilidades x 3 = 30, mas soma real usa min(rating,3)
        val sheet = CharacterSheet(abilities = abilities)
        val issues = viewModel.computeCompletionIssues(sheet)
        assertFalse(issues.containsKey("Aba 4 — Habilidades"))
    }

    @Test
    fun `meritos com menos de 10 pontos gera pendencia em Aba 6`() {
        val sheet = CharacterSheet(merits = listOf(Merito(nome = "Recursos", valor = 3)))
        val issues = viewModel.computeCompletionIssues(sheet)
        assertTrue(issues.containsKey("Aba 6 — Méritos"))
        assertTrue(issues["Aba 6 — Méritos"]!!.first().contains("3/10"))
    }

    @Test
    fun `meritos com 10 pontos ou mais nao gera pendencia`() {
        val sheet = CharacterSheet(merits = listOf(Merito(nome = "Recursos", valor = 10)))
        val issues = viewModel.computeCompletionIssues(sheet)
        assertFalse(issues.containsKey("Aba 6 — Méritos"))
    }

    @Test
    fun `menos de 15 encantos gera pendencia em Aba 8`() {
        val charms = (1..5).map { Encanto(nome = "Encanto $it", habilidadeVinculada = "Presença") }
        val sheet = CharacterSheet(charms = charms)
        val issues = viewModel.computeCompletionIssues(sheet)
        assertTrue(issues.containsKey("Aba 8 — Encantos"))
        assertTrue(issues["Aba 8 — Encantos"]!!.first().contains("5/15"))
    }

    @Test
    fun `15 encantos ou mais nao gera pendencia`() {
        val charms = (1..15).map { Encanto(nome = "Encanto $it", habilidadeVinculada = "Presença") }
        val sheet = CharacterSheet(charms = charms)
        val issues = viewModel.computeCompletionIssues(sheet)
        assertFalse(issues.containsKey("Aba 8 — Encantos"))
    }

    @Test
    fun `pontos de bonus nao distribuidos gera pendencia com saldo restante`() {
        // Planilha em branco: 15 de 15 pontos de bonus ainda por gastar.
        val issues = viewModel.computeCompletionIssues(CharacterSheet())
        assertTrue(issues.containsKey("Pontos de Bônus"))
        assertTrue(issues["Pontos de Bônus"]!!.first().contains("15"))
    }

    @Test
    fun `pontos de bonus excedidos gera pendencia com texto de saldo negativo`() {
        // Meritos muito acima do total base (10) consomem Pontos de Bonus
        // via meritBp = (total-10)*1 — 40 de merito gasta 30 BP, bem alem
        // dos 15 disponiveis, garantindo saldo negativo com folga (sem
        // depender de acertar o valor exato de outras formulas).
        val sheet = CharacterSheet(merits = listOf(Merito(nome = "Recursos", valor = 40)))
        val issues = viewModel.computeCompletionIssues(sheet)
        assertTrue(issues.containsKey("Pontos de Bônus"))
        assertTrue(issues["Pontos de Bônus"]!!.first().contains("excedido"))
    }
}
