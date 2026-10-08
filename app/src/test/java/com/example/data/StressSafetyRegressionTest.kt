package com.example.data

import com.example.feature.charmtree.CharmPrerequisiteTreeBuilder
import com.example.feature.charmtree.CharmTreeEntry
import com.example.oldrealm.OldRealmTranslator
import com.example.oldrealm.highrealm.HighRealmLayout
import com.example.oldrealm.highrealm.HighRealmTranslator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StressSafetyRegressionTest {
    @Test
    fun `tradutores permanecem limitados para entradas extremas`() {
        val entrada = "EXALTED ".repeat(10_000)
        val old = OldRealmTranslator.translate(entrada, OldRealmTranslator.SupportedLanguage.ENGLISH)
        val high = HighRealmTranslator.translate(entrada)
        assertTrue(old.normalizedInput.length <= 20_000)
        assertTrue(high.normalizedInput.length <= 20_000)
        assertTrue(old.warnings.isNotEmpty())
    }

    @Test
    fun `layout high realm nao cai com dimensao invalida`() {
        assertTrue(HighRealmLayout.pack(emptyList(), 0).isEmpty())
        assertTrue(HighRealmLayout.pack(emptyList(), -1).isEmpty())
    }

    @Test
    fun `arvore de encantos permanece limitada com ciclo e referencias ausentes`() {
        val entries = listOf(
            CharmTreeEntry("A", "A", prerequisiteIds = listOf("B", "MISSING")),
            CharmTreeEntry("B", "B", prerequisiteIds = listOf("A"))
        )
        val result = CharmPrerequisiteTreeBuilder.build("A", entries)
        assertTrue(result != null)
        assertTrue(result!!.cycleDetected)
        assertTrue("MISSING" in result.missingPrerequisiteIds)
    }

    @Test
    fun `codigo de compartilhamento rejeita entradas invalidas sem excecao`() {
        val casos = listOf(
            "",
            "APPNPCS-V1.",
            "APPNPCS-V999.0.invalid",
            "APPNPCS-V1.0.%%%%",
            "texto completamente invalido"
        )
        casos.forEach { codigo ->
            val resultado = NpcShareCodec.importar(codigo)
            assertTrue("Importação não deve lançar e deve retornar erro", resultado is NpcShareCodec.ResultadoImportacao.Erro)
        }
    }
}
