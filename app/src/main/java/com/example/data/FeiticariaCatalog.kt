package com.example.data

import android.content.Context
import com.example.model.Encanto
import com.example.model.EncantoQuadro
import org.json.JSONObject

data class FeiticoDefinition(
    val id: String,
    val nome: String,
    val nomeIngles: String,
    val circulo: String, // "Terrestre" | "Celestial" | "Solar"
    val custo: String,
    val palavrasChave: String,
    val duracao: String,
    val livro: String,
    val descricao: String,
    val quadros: List<EncantoQuadro> = emptyList()
) {
    // Feitiços são adquiridos e armazenados como Encantos (categoria
    // "Feitiçaria", vinculados a Ocultismo), reaproveitando toda a pipeline
    // já existente de aquisição, custo em XP e persistência.
    fun toEncanto(): Encanto = Encanto(
        nome = nome,
        nomeIngles = nomeIngles,
        habilidadeVinculada = "Ocultismo",
        custo = custo,
        palavrasChave = palavrasChave,
        duracao = duracao,
        preRequisitos = "Feitiçaria do Círculo $circulo",
        descricao = descricao,
        quadros = quadros,
        categoria = "Feitiçaria",
        circulo = circulo
    )
}

// Fonte oficial: app/src/main/assets/feiticaria.json.
class FeiticariaCatalog(context: Context) {
    val definitions: List<FeiticoDefinition>
    private val porCirculo: Map<String, List<FeiticoDefinition>>

    private companion object {
        val WHITESPACE_REGEX = Regex("\\s+")
    }

    init {
        val json = context.assets.open("feiticaria.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(json)
        val list = mutableListOf<FeiticoDefinition>()
        val arr = root.optJSONArray("feitiços") ?: root.optJSONArray("feiticos")
            ?: throw IllegalStateException("feiticaria.json sem o campo 'feitiços'.")

        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val nome = f.optString("nome", "").trim()
            if (nome.isBlank()) continue
            val idBruto = if (f.has("id") && !f.isNull("id")) f.optString("id") else null
            val id = idBruto?.takeIf { it.isNotBlank() } ?: "feitico_${EncantosSolaresCatalog.normalize(nome).replace(WHITESPACE_REGEX, "_")}"

            val descricaoPartes = mutableListOf<String>()
            val quadros = mutableListOf<EncantoQuadro>()
            val conteudoArr = f.optJSONArray("conteudo")
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

            list += FeiticoDefinition(
                id = id,
                nome = nome,
                nomeIngles = f.optString("nome_ingles", "").trim(),
                circulo = f.optString("circulo", "").trim(),
                custo = f.optString("custo", ""),
                palavrasChave = f.optString("palavras_chave", ""),
                duracao = f.optString("duracao", ""),
                livro = f.optString("livro", ""),
                descricao = descricaoPartes.joinToString("\n\n"),
                quadros = quadros
            )
        }
        definitions = list
        // Feitiços/Necromancias são ordenados alfabeticamente dentro de cada
        // Círculo — a ordenação recomeça do zero a cada Círculo, que
        // permanecem sempre separados entre si (agrupados por chave).
        val collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("pt-BR"))
        porCirculo = list.groupBy { it.circulo }.mapValues { (_, feiticos) -> feiticos.sortedWith(compareBy(collator) { it.nome }) }
    }

    fun paraCirculo(circulo: String): List<FeiticoDefinition> = porCirculo[circulo] ?: emptyList()
}
