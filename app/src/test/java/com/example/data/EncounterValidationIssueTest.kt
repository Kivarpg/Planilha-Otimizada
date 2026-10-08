package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import com.example.model.NOME_CORPO_DE_TOURO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncounterValidationIssueTest {
    private fun atributosFisicosFortes(): Map<String, Int> =
        ExaltedConstants.ALL_ATTRIBUTES.associateWith { nome ->
            if (nome == ExaltedConstants.PHYSICAL_ATTRIBUTES.first()) 5 else 3
        }

    @Test
    fun `API legada preserva exatamente as mensagens da API tipada`() {
        val issues = EncounterValidationService.validarIssues(
            ArquetipoEncontro.FISICO,
            atributosFisicosFortes(),
            charms = emptyList(),
            feiticos = emptyList<Any>()
        )
        val legacy = EncounterValidationService.validar(
            ArquetipoEncontro.FISICO,
            atributosFisicosFortes(),
            charms = emptyList(),
            feiticos = emptyList<Any>()
        )
        assertEquals(issues.map { it.message }, legacy)
    }

    @Test
    fun `ausencia de Corpo de Touro em fisico e informacao e nao erro`() {
        val issues = EncounterValidationService.validarIssues(
            ArquetipoEncontro.FISICO,
            atributosFisicosFortes(),
            charms = emptyList(),
            feiticos = emptyList<Any>()
        )
        val issue = issues.single { it.code == "PROFILE_PHYSICAL_WITHOUT_OX_BODY" }
        assertEquals(EncounterValidationSeverity.INFO, issue.severity)
        assertTrue(issues.none { it.severity == EncounterValidationSeverity.ERROR })
    }
}
