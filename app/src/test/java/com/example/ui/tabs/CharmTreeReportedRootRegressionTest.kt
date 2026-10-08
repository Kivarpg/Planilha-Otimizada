package com.example.ui.tabs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CharmTreeReportedRootRegressionTest {
    @Test
    fun `lunar manipulation source keeps glib tongue as linked minimum root`() {
        val json = File("src/main/assets/encantos_lunares.json").readText()
        assertTrue(json.contains("\"nome\": \"Técnica da Língua Ágil\""))
        assertTrue(json.contains("\"mins\": \"Manipulação 2, Essência 1\""))
        assertTrue(json.contains("\"pre_requisitos\": \"Técnica da Língua Ágil\""))
    }

    @Test
    fun `solar bureaucracy source contains multiple minimum linked roots`() {
        val json = File("src/main/assets/encantos_solares.json").readText()
        assertTrue(json.contains("\"nome\":\"Maneira Oficial Hábil\""))
        assertTrue(json.contains("\"nome\":\"Método do Comerciante Frugal\""))
        assertTrue(json.contains("\"mins\":\"Burocracia 1, Essência 1\""))
        assertTrue(json.contains("Maneira Oficial Hábil, Método do Comerciante Frugal"))
    }
}
