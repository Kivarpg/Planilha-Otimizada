package com.example.data

import android.content.Context
import com.example.model.Encanto
import com.example.model.EncantoQuadro
import com.example.model.ExaltedConstants
import org.json.JSONObject


// Diferente de EncantoSolarDefinition (habilidade + minHabilidade), Lunares
// usam Atributo + minAtributo como requisito mínimo — pedido explícito do
// usuário: "Solares e Sangue de Dragão possuem como Mins: Habilidade e
// Essência. Lunares funcionam de forma diferente: Mins: Atributo e
// Essência." O campo "secao" preserva o agrupamento original do documento
// (Universal + os 9 Atributos), usado tanto para navegação do catálogo
// quanto, por ora, como valor de habilidadeVinculada em toEncanto() — ver
// nota abaixo sobre essa decisão.
data class LunarCharmArchetypeRoute(
    val atributo: String,
    val condicao: String,
    val minAtributo: Int,
    val preRequisitosAlternativos: String
)

data class EncantoLunarDefinition(
    val id: String,
    val atributo: String,
    // Subdivisão temática dentro do Atributo (ex.: "Sangue do Coração",
    // "Ofensivo", "Mobilidade") — pedido explícito do usuário, extraída
    // diretamente da estrutura hierárquica do documento Lunares – Encantos.
    // Nula para Encantos Universais (Universal não tem subdivisões).
    val subdivisao: String?,
    val nome: String,
    val nomeIngles: String,
    val custo: String,
    val minsTexto: String,
    val minAtributo: Int,
    val minEssencia: Int,
    val tipo: String,
    val palavrasChave: String,
    val duracao: String,
    val preRequisitos: String,
    val descricao: String,
    val quadros: List<EncantoQuadro> = emptyList(),
    val rotasArquetipo: List<LunarCharmArchetypeRoute> = emptyList()
) {
    // NOTA (estrutura da rodada 1): o modelo compartilhado Encanto exige
    // habilidadeVinculada (usado por ~19 arquivos — agrupamento de gaveta
    // na Aba 8, custo de XP, filtros da Aba 4, etc.). Em vez de adaptar
    // todos esses pontos agora, reaproveitamos o campo passando o nome do
    // Atributo — funciona para exibição/agrupamento sem mudança nenhuma
    // nesses 19 arquivos. O campo "atributo" desta classe continua
    // disponível, correto e sem ambiguidade, para quando a lógica de
    // elegibilidade específica de Lunar (checar attributes[] em vez de
    // abilities[]) for implementada numa próxima rodada.
    fun toEncanto(): Encanto = Encanto(
        id = id,
        nome = nome,
        nomeIngles = nomeIngles,
        habilidadeVinculada = atributo,
        custo = custo,
        mins = minsTexto,
        minHabilidade = minAtributo,
        minEssencia = minEssencia,
        tipo = tipo,
        palavrasChave = palavrasChave,
        duracao = duracao,
        preRequisitos = preRequisitos,
        descricao = descricao,
        quadros = quadros,
        categoria = "Encanto"
    )
}

// Conversão pro formato compartilhado — mesmo padrão já usado por
// EncantoSangueDeDragaoDefinition.paraFormatoSolar(), pra reaproveitar a
// busca/navegação da Aba 8 sem duplicar essa lógica. IMPORTANTE: isso
// resolve a EXIBIÇÃO do catálogo certo (nomes/textos dos Encantos
// Lunares), mas a elegibilidade (nível mínimo) continua checando
// abilities[] em vez de attributes[] nesses pontos compartilhados —
// trabalho pendente, fora do escopo desta correção pontual.
fun EncantoLunarDefinition.paraFormatoSolar(): EncantoSolarDefinition = EncantoSolarDefinition(
    id = id, habilidade = atributo, nome = nome, nomeIngles = nomeIngles,
    custo = custo, minsTexto = minsTexto, minHabilidade = minAtributo,
    minEssencia = minEssencia, tipo = tipo, palavrasChave = palavrasChave,
    duracao = duracao, preRequisitos = preRequisitos, descricao = descricao,
    quadros = quadros
)

// Fonte oficial: app/src/main/assets/encantos_lunares.json — lista plana de
// Encantos Lunares, um objeto por Encanto. Diferente do JSON Solar, este
// já vem com os campos "atributo" (seção: Universal ou um dos 9 Atributos),
// "atributo_mins", "min_atributo" e "min_essencia" pré-extraídos no momento
// da geração — não precisa de regex aqui para separar o texto de "mins".
class EncantosLunaresCatalog(context: Context) {
    val definitions: List<EncantoLunarDefinition>

    private val porAtributo: Map<String, List<EncantoLunarDefinition>>
    private val porAtributoESubdivisao: Map<String, List<EncantoLunarDefinition>>
    private val porIdMap: Map<String, EncantoLunarDefinition>
    private val porNomeMap: Map<String, EncantoLunarDefinition>

    init {
        val json = context.assets.open("encantos_lunares.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(json)
        val list = mutableListOf<EncantoLunarDefinition>()
        val encantos = root.optJSONArray("encantos") ?: throw IllegalStateException("encantos_lunares.json sem o campo 'encantos'.")
        val nove_atributos = ExaltedConstants.PHYSICAL_ATTRIBUTES + ExaltedConstants.SOCIAL_ATTRIBUTES + ExaltedConstants.MENTAL_ATTRIBUTES
        val mapaAtributoCanonico = nove_atributos.associateBy { normalize(it) }

        for (i in 0 until encantos.length()) {
            val c = encantos.optJSONObject(i) ?: continue
            val nome = c.optString("nome", "").trim()
            if (nome.isBlank()) continue
            val idBruto = if (c.has("id") && !c.isNull("id")) c.optString("id") else null
            val id = idBruto?.takeIf { it.isNotBlank() } ?: "lunar_${normalize(nome).replace(WHITESPACE_REGEX, "_")}"
            val nomeIngles = c.optString("nome_ingles", "").trim()
            val minsTexto = c.optString("mins", "").trim()
            val secao = c.optString("atributo", "").trim() // "Universal" ou um dos 9 Atributos
            val subdivisaoBruta = c.optString("subdivisao", "").trim()
            val subdivisao = subdivisaoBruta.ifBlank { null }
            val atributoBruto = c.optString("atributo_mins", "").trim()
            val minAtributo = c.optInt("min_atributo", 0)
            val minEssencia = c.optInt("min_essencia", 0)
            // Usa a grafia canônica do Atributo quando reconhecível — casos
            // como "Carisma 2 ou Raciocínio" (requisito alternativo raro,
            // 1 de 596) não batem com nenhum Atributo único e ficam como
            // vieram do texto original, sem normalizar errado.
            val atributoParaAgrupar = if (secao.isNotBlank() && secao != "Universal") secao
                else mapaAtributoCanonico[normalize(atributoBruto)] ?: atributoBruto.ifBlank { "Universal" }

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

            val rotasArquetipo = mutableListOf<LunarCharmArchetypeRoute>()
            val rotasArr = c.optJSONArray("rotas_arquetipo")
            if (rotasArr != null) {
                for (j in 0 until rotasArr.length()) {
                    val rota = rotasArr.optJSONObject(j) ?: continue
                    val preArr = rota.optJSONArray("pre_requisitos_alternativos")
                    val pre = if (preArr == null) emptyList() else (0 until preArr.length())
                        .mapNotNull { k -> preArr.optString(k, "").trim().takeIf(String::isNotBlank) }
                    rotasArquetipo += LunarCharmArchetypeRoute(
                        atributo = rota.optString("atributo", "").trim(),
                        condicao = rota.optString("condicao", "").trim(),
                        minAtributo = rota.optInt("min_atributo", minAtributo),
                        preRequisitosAlternativos = if (pre.isEmpty()) "Nenhum" else pre.joinToString(", ")
                    )
                }
            }

            list += EncantoLunarDefinition(
                id = id,
                atributo = atributoParaAgrupar,
                subdivisao = subdivisao,
                nome = nome,
                nomeIngles = nomeIngles,
                custo = c.optString("custo", ""),
                minsTexto = minsTexto,
                minAtributo = minAtributo,
                minEssencia = minEssencia,
                tipo = c.optString("tipo", ""),
                palavrasChave = c.optString("palavras_chave", ""),
                duracao = c.optString("duracao", ""),
                preRequisitos = c.optPrerequisitosJson(),
                descricao = descricaoPartes.joinToString("\n\n"),
                quadros = quadros,
                rotasArquetipo = rotasArquetipo
            )
        }
        definitions = list
        porAtributo = list
            .groupBy { normalize(it.atributo) }
            .mapValues { (_, encantos) ->
                encantos.sortedWith(
                    compareBy<EncantoLunarDefinition> { it.minEssencia }
                        .thenBy { it.nome }
                )
            }
        // Índice composto para a consulta da Aba 8 Lunar. A UI consulta
        // repetidamente Atributo + Subdivisão; filtrar `definitions` inteira
        // a cada abertura era O(n). A chave normalizada preserva exatamente a
        // semântica case/acentos-insensitive usada por `sameName`.
        porAtributoESubdivisao = list
            .mapNotNull { encanto -> encanto.subdivisao?.let { sub -> chaveAtributoESubdivisao(encanto.atributo, sub) to encanto } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, encantos) ->
                encantos.sortedWith(
                    compareBy<EncantoLunarDefinition> { it.minEssencia }
                        .thenBy { it.nome }
                )
            }
        porIdMap = list.associateBy { it.id }
        porNomeMap = list.associateBy { normalize(it.nome) }
    }

    fun paraAtributo(atributo: String): List<EncantoLunarDefinition> =
        porAtributo[normalize(atributo)] ?: emptyList()

    fun paraAtributoESubdivisao(atributo: String, subdivisao: String): List<EncantoLunarDefinition> =
        porAtributoESubdivisao[chaveAtributoESubdivisao(atributo, subdivisao)] ?: emptyList()

    private fun chaveAtributoESubdivisao(atributo: String, subdivisao: String): String =
        normalize(atributo) + "\u0000" + normalizarSubdivisao(subdivisao)

    private fun normalizarSubdivisao(value: String): String =
        value.trim().lowercase()

    fun porId(id: String): EncantoLunarDefinition? = porIdMap[id]

    fun porNome(nome: String): EncantoLunarDefinition? = porNomeMap[normalize(nome)]

    companion object {
        private val DIACRITICS_REGEX = Regex("\\p{M}+")
        private val WHITESPACE_REGEX = Regex("\\s+")

        fun normalize(value: String): String =
            java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replace(DIACRITICS_REGEX, "")
                .lowercase()
                .trim()
    }
}
