package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterCustomizationRegressionTest {
    @Test fun `customizacao vazia permanece vazia`() { assertTrue(EncounterCustomization().isEmpty) }
    @Test fun `foco secundario ausente significa nenhum segundo foco`() {
        val c = EncounterCustomization(foco = "Presença")
        assertNull(c.secundaria); assertFalse(c.isEmpty)
    }
    @Test fun `foco secundario explicito participa da customizacao`() {
        assertFalse(EncounterCustomization(foco = "Presença", secundaria = "Integridade").isEmpty)
    }
    @Test fun `artes marciais e metodo de ataque mas usa Briga como habilidade real`() {
        val c = EncounterCustomization(ataque = EncounterGenerationRules.ATTACK_MARTIAL_ARTS)
        assertTrue(c.rotaArtesMarciais)
        assertTrue(EncounterGenerationRules.ATTACK_MARTIAL_ARTS in EncounterGenerationRules.ATTACK_METHODS)
        assertFalse(EncounterGenerationRules.ATTACK_MARTIAL_ARTS in EncounterGenerationRules.COMBAT_ABILITIES)
        assertTrue(EncounterGenerationRules.habilidadeRealDoMetodoDeAtaque(c.ataque) == "Briga")
    }
    @Test fun `nenhum permanece semanticamente distinto de automatico`() {
        val automatico = EncounterCustomization()
        val nenhum = EncounterCustomization(focoMode = EncounterCustomizationMode.NENHUM)
        assertTrue(automatico.isEmpty)
        assertFalse(nenhum.isEmpty)
        assertNull(nenhum.focoEfetivo("Presença"))
    }
    @Test fun `automatico permite fallback e explicito prevalece`() {
        val automatico = EncounterCustomization()
        val explicito = EncounterCustomization(foco = "Presença")
        assertTrue(automatico.focoEfetivo("Ocultismo") == "Ocultismo")
        assertTrue(explicito.focoEfetivo("Ocultismo") == "Presença")
    }
    @Test fun `nenhum ofensivo nao herda foco como preferencia`() {
        val nenhum = EncounterCustomization(
            foco = "Briga",
            ataqueMode = EncounterCustomizationMode.NENHUM
        )
        assertNull(nenhum.focoParaAtaque(nenhum.focoEfetivo(null)))
    }

}
