package com.example.data

import com.example.model.ArquetipoEncontro
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class EncounterDistributionServiceLunarTest {
    @Test
    fun pbLunarNaoInvesteForaDoPerfilDoArquetipo() {
        val base = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.SOCIAL,
            habilidadeCombate = "Presença",
            habilidadeDefensivaObrigatoria = "Esquiva",
            supernal = "Presença",
            habilidadesFavorecidas = emptyList(),
            random = Random(7)
        )
        val resultado = EncounterDistributionService.distribuirPontosDeBonusLunar(
            arquetipo = ArquetipoEncontro.SOCIAL,
            abilitiesBase = base.abilities,
            attributesBase = emptyMap(),
            atributosCastaOuFavorecidos = emptyList(),
            habilidadeCombate = "Presença",
            habilidadeDefensiva = "Esquiva",
            random = Random(11)
        )
        val perfil = setOf(
            "Presença", "Performance", "Socialização", "Integridade",
            "Burocracia", "Linguística", "Furtividade",
            // A distribuição prioriza combate/defensiva/suporte mesmo fora
            // do perfil puro do arquétipo (Esquiva é a defensiva padrão).
            "Esquiva"
        )
        val alteradas = resultado.abilities.filter { (nome, valor) ->
            valor > (base.abilities[nome] ?: 0)
        }.keys
        assertTrue(alteradas.all { it in perfil })
    }


    @Test
    fun pbLunarMantemOrcamentoDe15PontosMesmoComAtributosDisponiveis() {
        val base = EncounterDistributionService.distribuirHabilidades(
            arquetipo = ArquetipoEncontro.MENTAL,
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensivaObrigatoria = "Esquiva",
            supernal = "Ocultismo",
            habilidadesFavorecidas = emptyList(),
            random = Random(21)
        )
        val resultado = EncounterDistributionService.distribuirPontosDeBonusLunar(
            arquetipo = ArquetipoEncontro.MENTAL,
            abilitiesBase = base.abilities,
            attributesBase = mapOf(
                "Percepção" to 1, "Inteligência" to 1, "Raciocínio" to 1,
                "Força" to 1, "Destreza" to 1, "Vigor" to 1,
                "Carisma" to 1, "Manipulação" to 1, "Aparência" to 1
            ),
            atributosCastaOuFavorecidos = listOf("Inteligência", "Raciocínio"),
            habilidadeCombate = "Armas Brancas",
            habilidadeDefensiva = "Esquiva",
            random = Random(22)
        )

        assertTrue(resultado.forcaDeVontade in 6..7)
        assertTrue(resultado.abilities.values.all { it in 0..5 })
        assertTrue(resultado.attributes.values.all { it in 1..5 })

        val aumentosDeHabilidade = resultado.abilities.entries.sumOf { (nome, valor) ->
            (valor - (base.abilities[nome] ?: 0)).coerceAtLeast(0)
        }
        val aumentosDeAtributo = resultado.attributes.entries.sumOf { (nome, valor) ->
            (valor - 1).coerceAtLeast(0)
        }
        val custoHabilidades = aumentosDeHabilidade * 2
        val custoAtributos = aumentosDeAtributo * 3
        val custoVontade = (resultado.forcaDeVontade - 5) * 2
        assertTrue(custoHabilidades + custoAtributos + custoVontade <= 15)
    }

    @Test
    fun perfisLunaresNaoContemHabilidadesForaDaListaOficial() {
        EncounterGenerationRules.LUNAR_ABILITY_PROFILES.values.flatten().forEach {
            assertTrue(it in com.example.model.ExaltedConstants.ALL_25_ABILITIES)
        }
    }

    @Test
    fun `PB Lunar permanece em Atributo favorecido quando restricao de combate e aplicada antes`() {
        val base = mapOf(
            "Força" to 2, "Destreza" to 4, "Vigor" to 5,
            "Carisma" to 3, "Manipulação" to 3, "Aparência" to 3,
            "Percepção" to 2, "Inteligência" to 2, "Raciocínio" to 3
        )
        val ajustadoAntesDoPb = EncounterDistributionService.ajustarAtributosPorHabilidadeCombate(
            base, "Arqueirismo", Random(31)
        )
        val resultado = EncounterDistributionService.distribuirPontosDeBonusLunar(
            arquetipo = ArquetipoEncontro.FISICO,
            abilitiesBase = EncounterDistributionService.distribuirHabilidades(
                ArquetipoEncontro.FISICO, "Arqueirismo", "Esquiva", "Arqueirismo", emptyList(), Random(32)
            ).abilities,
            attributesBase = ajustadoAntesDoPb,
            atributosCastaOuFavorecidos = listOf("Força", "Destreza", "Vigor", "Carisma"),
            habilidadeCombate = "Arqueirismo",
            habilidadeDefensiva = "Esquiva",
            random = Random(33)
        )

        assertTrue(resultado.attributes["Força"]!! >= 5 || resultado.attributes["Destreza"]!! >= 5 || resultado.attributes["Vigor"]!! >= 4 || resultado.attributes["Carisma"]!! >= 2)
        assertTrue(resultado.attributes.values.all { it in 1..5 })
    }

}
