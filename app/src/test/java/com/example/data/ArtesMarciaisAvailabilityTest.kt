package com.example.data

import com.example.model.TipoExaltadoEncontro
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArtesMarciaisAvailabilityTest {
    @Test
    fun cincoEstilosDeDragaoSaoExclusivosDeSangueDeDragao() {
        val nomes = ArtesMarciaisAvailability.estilosExclusivosSangueDeDragao
        assertEquals(5, nomes.size)
        nomes.forEach { nome ->
            assertTrue(ArtesMarciaisAvailability.disponivel(nome, TipoExaltadoEncontro.SANGUE_DE_DRAGAO))
            assertFalse(ArtesMarciaisAvailability.disponivel(nome, TipoExaltadoEncontro.SOLAR))
            assertFalse(ArtesMarciaisAvailability.disponivel(nome, TipoExaltadoEncontro.LUNAR))
        }

        // A nomenclatura alternativa da Madeira continua restrita ao Sangue de Dragão.
        assertTrue(
            ArtesMarciaisAvailability.disponivel(
                "Estilo do Dragão da Madeira",
                TipoExaltadoEncontro.SANGUE_DE_DRAGAO
            )
        )
        assertFalse(
            ArtesMarciaisAvailability.disponivel(
                "Estilo do Dragão da Madeira",
                TipoExaltadoEncontro.SOLAR
            )
        )
        assertFalse(
            ArtesMarciaisAvailability.disponivel(
                "Estilo do Dragão da Madeira",
                TipoExaltadoEncontro.LUNAR
            )
        )
    }

    @Test
    fun outrosEstilosContinuamDisponiveisParaOsTresTipos() {
        val nome = "Estilo Solar" // não pertence à lista restrita; usado apenas como contrato de fallback
        assertEquals(TipoExaltadoEncontro.values().toSet(), ArtesMarciaisAvailability.tiposPermitidos(nome))
    }
}
