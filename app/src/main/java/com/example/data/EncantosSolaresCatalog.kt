package com.example.data

import android.content.Context
import com.example.model.Encanto
import com.example.model.EncantoQuadro
import com.example.model.ExaltedConstants
import org.json.JSONObject


data class EncantoSolarDefinition(
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
) {
    fun toEncanto(): Encanto = Encanto(
        id = id,
        nome = nome,
        nomeIngles = nomeIngles,
        habilidadeVinculada = habilidade,
        custo = custo,
        mins = minsTexto,
        minHabilidade = minHabilidade,
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

// Fonte oficial: app/src/main/assets/encantos_solares.json — lista plana de
// Encantos, um objeto por Encanto, com o campo "mins" combinando habilidade e
// Essência mínimas em um único texto (ex.: "Armas brancas 2, Essência 1").
class EncantosSolaresCatalog(context: Context) {
    val definitions: List<EncantoSolarDefinition>

    // Índice em memória por habilidade (normalizada), montado uma única vez
    // no carregamento — evita filtrar a lista inteira a cada abertura de popup.
    private val porHabilidade: Map<String, List<EncantoSolarDefinition>>
    private val porIdMap: Map<String, EncantoSolarDefinition>
    // Índice por nome normalizado — evita busca linear em sanitizeKnownCharms
    // (CharmsActions.kt), que precisa achar a definição de cada Encanto já
    // adquirido pelo personagem toda vez que um pré-requisito pode ter mudado.
    private val porNomeMap: Map<String, EncantoSolarDefinition>

    init {
        val json = context.assets.open("encantos_solares.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(json)
        val list = mutableListOf<EncantoSolarDefinition>()
        val encantos = root.optJSONArray("encantos") ?: throw IllegalStateException("encantos_solares.json sem o campo 'encantos'.")
        val minsRegex = Regex("""^(.+?)\s+(\d+),\s*Essência\s+(\d+)$""", RegexOption.IGNORE_CASE)
        // Pré-computado uma única vez (25 entradas) — evita normalizar e
        // comparar contra as 25 habilidades pra cada um dos ~751 encantos
        // do catálogo (751×25 normalizações seria bem mais lento que 25
        // normalizações + 751 lookups O(1) num mapa).
        val mapaHabilidadeCanonica = ExaltedConstants.ALL_25_ABILITIES.associateBy { EncantosSolaresCatalog.normalize(it) }

        for (i in 0 until encantos.length()) {
            val c = encantos.optJSONObject(i) ?: continue
            val nome = c.optString("nome", "").trim()
            if (nome.isBlank()) continue
            val idBruto = if (c.has("id") && !c.isNull("id")) c.optString("id") else null
            val id = idBruto?.takeIf { it.isNotBlank() } ?: "encanto_${EncantosSolaresCatalog.normalize(nome).replace(WHITESPACE_REGEX, "_")}"
            val nomeIngles = c.optString("nome_ingles", "").trim()
            val minsTexto = c.optString("mins", "").trim()
            val match = minsRegex.find(minsTexto)
            val habilidadeBruta = match?.groupValues?.get(1)?.trim() ?: ""
            val minHabilidade = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val minEssencia = match?.groupValues?.get(3)?.toIntOrNull() ?: 0
            // Usa a grafia canônica da habilidade (ex.: "Armas Brancas"), já
            // que o JSON pode vir com capitalização diferente ("Armas brancas").
            val habilidade = mapaHabilidadeCanonica[EncantosSolaresCatalog.normalize(habilidadeBruta)] ?: habilidadeBruta

            val descricaoPartes = mutableListOf<String>()
            val quadros = mutableListOf<EncantoQuadro>()
            val conteudoArr = c.optJSONArray("conteudo")
            if (conteudoArr != null) {
                for (j in 0 until conteudoArr.length()) {
                    val item = conteudoArr.optJSONObject(j) ?: continue
                    when (item.optString("tipo")) {
                        "descricao" -> {
                            // O JSON agora traz cada parágrafo real como um bloco
                            // "descricao" separado (reconstruído a partir do
                            // documento original — 2026-08-27), então não há mais
                            // "\n" cru pra normalizar aqui: o join com "\n\n" entre
                            // os elementos de descricaoPartes já basta.
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

            list += EncantoSolarDefinition(
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
        // A ordenação por habilidade é feita uma única vez durante o carregamento.
        // Assim, consultas repetidas da UI não precisam criar uma nova lista
        // ordenada a cada recomposição/abertura de seção.
        porHabilidade = list
            .groupBy { normalize(it.habilidade) }
            .mapValues { (_, encantos) ->
                encantos.sortedWith(
                    compareBy<EncantoSolarDefinition> { it.minEssencia }
                        .thenBy { it.nome }
                )
            }
        porIdMap = list.associateBy { it.id }
        porNomeMap = list.associateBy { normalize(it.nome) }
    }

    // Encantos de uma habilidade, via índice em memória (O(1)).
    fun paraHabilidade(habilidade: String): List<EncantoSolarDefinition> =
        porHabilidade[normalize(habilidade)] ?: emptyList()

    fun porId(id: String): EncantoSolarDefinition? = porIdMap[id]

    fun porNome(nome: String): EncantoSolarDefinition? = porNomeMap[normalize(nome)]

    companion object {
        private val DIACRITICS_REGEX = Regex("\\p{M}+")
        private val WHITESPACE_REGEX = Regex("\\s+")

        fun normalize(value: String): String =
            java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replace(DIACRITICS_REGEX, "")
                .lowercase()
                .trim()

        fun sameName(a: String, b: String): Boolean = normalize(a) == normalize(b)
    }
}
