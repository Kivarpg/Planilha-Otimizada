package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageAcquisitionRulesTest {
    @Test fun `Antigo Reino exige Ocultismo ou Conhecimento um`() {
        assertFalse(LanguageAcquisitionRules.podeAdquirir("Antigo Reino", emptyMap()))
        assertTrue(LanguageAcquisitionRules.podeAdquirir("Antigo Reino", mapOf("Ocultismo" to 1)))
        assertTrue(LanguageAcquisitionRules.podeAdquirir("Antigo Reino", mapOf("Conhecimento" to 1)))
    }

    @Test fun `outros idiomas nao dependem de Ocultismo ou Conhecimento`() {
        assertTrue(LanguageAcquisitionRules.podeAdquirir("Alto Reino", emptyMap()))
    }
}
