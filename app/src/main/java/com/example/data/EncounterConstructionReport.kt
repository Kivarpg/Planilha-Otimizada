package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro

/** Relatório interno e determinístico da construção de um NPC. */
data class EncounterConstructionReport(
    val tipo: TipoExaltadoEncontro,
    val arquetipo: ArquetipoEncontro,
    val identidade: List<String>,
    val regrasObrigatorias: List<String>,
    val preferencias: List<String>,
    val reservasEncantos: List<String>,
    val observacoes: List<String>
) {
    fun resumo(): String = buildString {
        append("${tipo.name}/${arquetipo.name}: ")
        append(identidade.joinToString(" | "))
        if (reservasEncantos.isNotEmpty()) {
            append(" | Reservas: ")
            append(reservasEncantos.joinToString(", "))
        }
        if (observacoes.isNotEmpty()) {
            append(" | Observações: ")
            append(observacoes.joinToString("; "))
        }
    }
}

/**
 * Gera uma fotografia interna da construção final sem alterar o NPC.
 * É usada por testes/regressões e pode ser usada futuramente para diagnóstico
 * sem misturar diagnóstico com a planilha persistida.
 */
object EncounterConstructionReportService {
    fun gerar(npc: NpcEncontro): EncounterConstructionReport {
        val identidade = when (npc.tipoExaltado) {
            TipoExaltadoEncontro.SOLAR -> listOf(
                "Solar",
                "Supernal=${npc.habilidadeSupernal.ifBlank { "ausente" }}",
                "Favorecidas=${npc.habilidadesFavorecidas.size}"
            )
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> listOf(
                "Sangue de Dragão",
                "Aspecto=${npc.casta}",
                "Favorecidas adicionais=${npc.habilidadesFavorecidas.size}"
            )
            TipoExaltadoEncontro.LUNAR -> listOf(
                "Lunar",
                "Casta=${npc.casta}",
                "Casta attrs=${npc.lunarAtributosCasta.size}",
                "Favorecidos attrs=${npc.habilidadesFavorecidas.size}"
            )
        }

        val reservas = EncounterNpcSpellManagement.circulosDesbloqueados(npc)
            .map { "Feitiçaria $it" }

        val observacoes = buildList {
            add("Atributos=${npc.attributes.values.sum()}")
            add("Encantos=${npc.charms.size}")
            add("Feitiços=${npc.feiticos.size}")
            add("Arma=${npc.arma?.tipo ?: "ausente"}")
            add("Armadura=${npc.armadura?.tipo ?: "ausente"}")
            val motes = EncounterMoteService.totais(npc.tipoExaltado, npc.essencia, npc.arma, npc.armadura)
            add("Motes=${motes.pessoaisMax}/${motes.perifericosDisponiveis}; comprometidos=${motes.comitados}")
            if (npc.alertasValidacao.isNotEmpty()) {
                add("alertas=${npc.alertasValidacao.size}")
            }
        }

        return EncounterConstructionReport(
            tipo = npc.tipoExaltado,
            arquetipo = npc.arquetipo,
            identidade = identidade,
            regrasObrigatorias = (EncounterConstructionPolicy.baseConstructionRules() +
                EncounterConstructionPolicy.identityRules(npc.tipoExaltado))
                .filter { it.kind == EncounterConstructionPolicy.Kind.MANDATORY }
                .map { it.description },
            preferencias = EncounterConstructionPolicy.archetypePreferences(npc.arquetipo)
                .filter { it.kind == EncounterConstructionPolicy.Kind.PREFERENCE }
                .map { it.description },
            reservasEncantos = reservas,
            observacoes = observacoes
        )
    }
}
