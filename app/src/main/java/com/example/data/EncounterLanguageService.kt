package com.example.data

import kotlin.random.Random
import com.example.model.TipoExaltadoEncontro

/**
 * Idioma nativo apresentado na descrição dos NPCs gerados pela Aba 11.
 *
 * O campo representa somente o idioma inicial do NPC. Idiomas adicionais
 * continuam sendo adquiridos por Méritos (Idioma); eles não são adicionados
 * automaticamente a este campo.
 */
object EncounterLanguageService {
    private val IDIOMAS_INICIAIS = listOf(
        "Alto Reino",
        "Baixo Reino",
        "Língua dos Dragões",
        "Dialeto dos Rios",
        "Língua do Céu",
        "Língua de Fogo",
        "Língua da Floresta",
        "Língua do Mar",
        "Dialeto da Guilda"
    )

    fun selecionar(
        tipoExaltado: TipoExaltadoEncontro,
        origemNomeSangueDeDragao: OrigemNomeSangueDeDragao = OrigemNomeSangueDeDragao.SEM_CASTA,
        random: Random = Random.Default
    ): String = when {
        tipoExaltado == TipoExaltadoEncontro.SANGUE_DE_DRAGAO &&
            origemNomeSangueDeDragao == OrigemNomeSangueDeDragao.IMPERIO -> "Alto Reino"

        tipoExaltado == TipoExaltadoEncontro.SANGUE_DE_DRAGAO &&
            origemNomeSangueDeDragao == OrigemNomeSangueDeDragao.LOOKSHY -> "Dialeto dos Rios"

        else -> IDIOMAS_INICIAIS.random(random)
    }

    fun opcoesIniciais(): List<String> = IDIOMAS_INICIAIS
}
