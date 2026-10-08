package com.example.data

import android.content.Context
import com.example.model.EncantoQuadro
import com.example.model.ExaltedConstants
import org.json.JSONObject


// Estrutura idêntica a EncantoSolarDefinition (mesmo schema de JSON), mas
// com nome próprio — misturar os dois tipos sob o mesmo nome de classe
// confundiria de onde cada encanto vem, mesmo os campos sendo iguais.
data class EncantoSangueDeDragaoDefinition(
    val id: String,
    val habilidade: String,
    val nome: String,
    val nomeIngles: String,
    val custo: String,
    val minsTexto: String,
    val minHabilidade: Int,
    val minEssencia: Int,
    val tipo: String,
    val palavrasChave: String,
    val duracao: String,
    val preRequisitos: String,
    val descricao: String,
    val quadros: List<EncantoQuadro> = emptyList()
)

// Fonte: app/src/main/assets/encantos_sangue_dragoes.json — construído a
// partir de "Sangue_dos_Dragões___Encantos.docx" (2026-09-02), mesmo
// schema do encantos_solares.json (mesma origem: lista plana, "mins"
// combinando habilidade e Essência mínimas num único texto). Catálogo
// consultável (busca e navegação, ver CharmsTab/SkillSearch) — este app
// ainda não suporta Sangue de Dragão como tipo de personagem jogável
// (planilhas continuam Solar-only), então estes encantos não são
// adquiríveis numa planilha, só consultáveis como referência.
//
// REGRA PENDENTE (quando um gerador de NPC de Sangue de Dragão for
// construído na Aba 11, hoje só implementada pra Solar em
// EncounterGenerator.kt): NPC de Sangue de Dragão nasce com 15 Encantos
// (igual ao Solar) MAIS 5 Encantos gratuitos adicionais que tenham a
// palavra-chave "Excelência" — pedido explícito do usuário. Aquisições
// além disso saem de Pontos de Bônus ou XP, a critério de quem gera.
class EncantosSangueDosDragoesCatalog(context: Context) {
    val definitions: List<EncantoSangueDeDragaoDefinition>

    private val porHabilidade: Map<String, List<EncantoSangueDeDragaoDefinition>>
    private val porIdMap: Map<String, EncantoSangueDeDragaoDefinition>

    init {
        val json = context.assets.open("encantos_sangue_dragoes.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(json)
        val list = mutableListOf<EncantoSangueDeDragaoDefinition>()
        val encantos = root.optJSONArray("encantos") ?: throw IllegalStateException("encantos_sangue_dragoes.json sem o campo 'encantos'.")
        val minsRegex = Regex("""^(.+?)\s+(\d+),\s*Essência\s+(\d+)$""", RegexOption.IGNORE_CASE)
        // Mesma otimização de EncantosSolaresCatalog.kt: pré-computa o
        // mapa (25 entradas) uma única vez em vez de renormalizar e
        // comparar contra as 25 habilidades pra cada um dos encantos.
        val mapaHabilidadeCanonica = ExaltedConstants.ALL_25_ABILITIES.associateBy { EncantosSolaresCatalog.normalize(it) }

        for (i in 0 until encantos.length()) {
            val c = encantos.optJSONObject(i) ?: continue
            val nome = c.optString("nome", "").trim()
            if (nome.isBlank()) continue
            val idBruto = if (c.has("id") && !c.isNull("id")) c.optString("id") else null
            val id = idBruto?.takeIf { it.isNotBlank() } ?: "sdd_${EncantosSolaresCatalog.normalize(nome).replace(WHITESPACE_REGEX, "_")}"
            val nomeIngles = c.optString("nome_ingles", "").trim()
            val minsTexto = c.optString("mins", "").trim()
            val match = minsRegex.find(minsTexto)
            val habilidadeBruta = match?.groupValues?.get(1)?.trim() ?: ""
            val minHabilidade = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val minEssencia = match?.groupValues?.get(3)?.toIntOrNull() ?: 0
            // Reaproveita a mesma normalização/lista canônica de habilidades
            // usada pros Solares — as 25 habilidades são as mesmas pra
            // qualquer tipo de Exaltado.
            val habilidade = mapaHabilidadeCanonica[EncantosSolaresCatalog.normalize(habilidadeBruta)] ?: habilidadeBruta

            val descricaoPartes = mutableListOf<String>()
            val quadros = mutableListOf<EncantoQuadro>()
            val conteudoArr = c.optJSONArray("conteudo")
            if (conteudoArr != null) {
                for (j in 0 until conteudoArr.length()) {
                    val item = conteudoArr.optJSONObject(j) ?: continue
                    when (item.optString("tipo")) {
                        "descricao" -> {
                            val texto = item.optString("texto", "").trim()
                            if (texto.isNotBlank()) descricaoPartes += texto
                        }
                        "quadro" -> {
                            val textoQuadro = item.optString("conteudo", "").trim()
                            if (textoQuadro.isNotBlank()) {
                                val linhas = textoQuadro.split("\n", limit = 2)
                                val titulo = linhas.getOrNull(0)?.trim() ?: ""
                                val corpo = linhas.getOrNull(1)?.trim() ?: textoQuadro
                                quadros += EncantoQuadro(titulo, corpo)
                            }
                        }
                    }
                }
            }

            list += EncantoSangueDeDragaoDefinition(
                id = id,
                habilidade = habilidade,
                nome = nome,
                nomeIngles = nomeIngles,
                custo = c.optString("custo", ""),
                minsTexto = minsTexto,
                minHabilidade = minHabilidade,
                minEssencia = minEssencia,
                tipo = c.optString("tipo", ""),
                palavrasChave = c.optString("palavras_chave", ""),
                duracao = c.optString("duracao", ""),
                preRequisitos = c.optPrerequisitosJson(),
                descricao = descricaoPartes.joinToString("\n\n"),
                quadros = quadros
            )
        }
        definitions = list
        // Mantém cada grupo já ordenado para que consultas posteriores sejam
        // apenas lookups no índice, sem sort repetido.
        porHabilidade = list
            .groupBy { EncantosSolaresCatalog.normalize(it.habilidade) }
            .mapValues { (_, encantos) ->
                encantos.sortedWith(
                    compareBy<EncantoSangueDeDragaoDefinition> { it.minEssencia }
                        .thenBy { it.nome }
                )
            }
        porIdMap = list.associateBy { it.id }
    }

    fun paraHabilidade(habilidade: String): List<EncantoSangueDeDragaoDefinition> =
        porHabilidade[EncantosSolaresCatalog.normalize(habilidade)] ?: emptyList()

    fun porId(id: String): EncantoSangueDeDragaoDefinition? = porIdMap[id]

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}

// Conversão pro mesmo formato de EncantoSolarDefinition (campos
// idênticos nos dois catálogos) — extraída aqui porque estava duplicada
// de forma idêntica em 3 lugares (EncounterGenerator.kt ×2,
// CharmsActions.kt ×1), sempre pro mesmo propósito: reaproveitar toda a
// UI e lógica de elegibilidade/aquisição já construída pro catálogo
// Solar, sem duplicar esse código também pro catálogo de Sangue de
// Dragão. Refatoração puramente estrutural — nenhum campo ou valor muda,
// só onde a conversão está definida.
fun EncantoSangueDeDragaoDefinition.paraFormatoSolar(): EncantoSolarDefinition = EncantoSolarDefinition(
    id = id, habilidade = habilidade, nome = nome, nomeIngles = nomeIngles,
    custo = custo, minsTexto = minsTexto, minHabilidade = minHabilidade,
    minEssencia = minEssencia, tipo = tipo, palavrasChave = palavrasChave,
    duracao = duracao, preRequisitos = preRequisitos, descricao = descricao,
    quadros = quadros
)
