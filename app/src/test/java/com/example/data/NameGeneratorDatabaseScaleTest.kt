package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameGeneratorDatabaseScaleTest {
    @Test fun `taxonomia cultural consolidada possui nove origens`() {
        assertEquals(
            setOf(
                "JAPONESA", "CHINESA", "COREANA", "ISLANDESA",
                "IRLANDESA_GAELICA", "HUNGARA", "AFRICANA",
                "ARABE", "MESOAMERICANA"
            ),
            CulturaNome.entries.map { it.name }.toSet()
        )
    }

    @Test fun `categorias genericas removidas nao retornam`() {
        val nomes = CulturaNome.entries.map { it.name }
        assertTrue("EUROPEIA" !in nomes)
        assertTrue("TRIBAL" !in nomes)
    }
}
