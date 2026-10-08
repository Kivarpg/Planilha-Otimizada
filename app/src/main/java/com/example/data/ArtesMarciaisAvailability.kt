package com.example.data

import com.example.model.TipoExaltadoEncontro

/** Regras de disponibilidade dos estilos de Artes Marciais para a Aba 11. */
object ArtesMarciaisAvailability {
    /** Os cinco estilos dos Dragões Imaculados são exclusivos de Sangue de Dragão. */
    val estilosExclusivosSangueDeDragao: Set<String> = setOf(
        "Estilo Dragão do Ar",
        "Estilo Dragão do Fogo",
        "Estilo do Dragão da Terra",
        "Estilo do Dragão da Água",
        "Estilo Dragão da Madeira"
    )

    /** Variação de nomenclatura usada na especificação para o estilo da Madeira. */
    private val nomesAlternativos: Set<String> = setOf(
        "Estilo do Dragão da Madeira"
    )

    private val todosOsNomesRestritos: Set<String> =
        estilosExclusivosSangueDeDragao + nomesAlternativos

    fun tiposPermitidos(nomePt: String): Set<TipoExaltadoEncontro> =
        if (nomePt in todosOsNomesRestritos) {
            setOf(TipoExaltadoEncontro.SANGUE_DE_DRAGAO)
        } else {
            TipoExaltadoEncontro.values().toSet()
        }

    fun disponivel(nomePt: String, tipo: TipoExaltadoEncontro): Boolean =
        tipo in tiposPermitidos(nomePt)
}
