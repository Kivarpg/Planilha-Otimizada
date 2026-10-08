package com.example.data

import android.content.Context
import com.example.model.EncantoQuadro
import com.example.model.TipoExaltadoEncontro
import org.json.JSONObject


/**
 * Catálogo dos estilos de Artes Marciais usados na Aba 8.
 *
 * O catálogo contém todos os estilos, mas a disponibilidade de cada estilo
 * para a Aba 11 respeita as restrições do tipo de Exaltado.
 */
data class EstiloArteMarcialDefinition(
    val id: String,
    val nomePt: String,
    val nomeEn: String,
    val descricao: String,
    val armaDoEstiloTexto: String?,
    val armaDoEstiloModo: String,
    val armasEspecificas: List<String>,
    val armaduraTexto: String?,
    val armaduraCategoria: String,
    val habilidadesComplementares: String?,
    /** Tipos de Exaltado que podem adquirir este estilo na Aba 11. */
    val tiposExaltadosPermitidos: Set<TipoExaltadoEncontro> = setOf(
        TipoExaltadoEncontro.SOLAR,
        TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
        TipoExaltadoEncontro.LUNAR
    ),
    val encantos: List<EncantoArteMarcialDefinition>
)

data class EncantoArteMarcialDefinition(
    val id: String,
    val estiloId: String,
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
    val quadros: List<EncantoQuadro>
) {
    fun toEncanto(): com.example.model.Encanto = com.example.model.Encanto(
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
        quadros = quadros
    )

    fun toEncantoSolarDefinition(): EncantoSolarDefinition = EncantoSolarDefinition(
        id = id,
        habilidade = habilidade,
        nome = nome,
        nomeIngles = nomeIngles,
        custo = custo,
        minsTexto = minsTexto,
        minHabilidade = minHabilidade,
        minEssencia = minEssencia,
        tipo = tipo,
        palavrasChave = palavrasChave,
        duracao = duracao,
        preRequisitos = preRequisitos,
        descricao = descricao,
        quadros = quadros
    )
}

class ArtesMarciaisCatalog(context: Context) {
    val definitions: List<EstiloArteMarcialDefinition>

    private val porId: Map<String, EstiloArteMarcialDefinition>
    private val porNome: Map<String, EstiloArteMarcialDefinition>
    private val encantoPorId: Map<String, EncantoArteMarcialDefinition>
    private val encantoPorNome: Map<String, EncantoArteMarcialDefinition>
    private val encantosPorEstilo: Map<String, List<EncantoArteMarcialDefinition>>

    init {
        val json = context.assets.open("estilos_artes_marciais.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(json)
        val estilosJson = root.optJSONArray("estilos")
            ?: throw IllegalStateException("estilos_artes_marciais.json sem o campo 'estilos'.")

        val minsRegex = Regex("""^Arte Marcial\s+(\d+),\s*Essência\s+(\d+);?$""", RegexOption.IGNORE_CASE)
        val estilos = mutableListOf<EstiloArteMarcialDefinition>()

        for (i in 0 until estilosJson.length()) {
            val estiloJson = estilosJson.optJSONObject(i) ?: continue
            val nomePt = estiloJson.optString("nome_pt", "").trim()
            if (nomePt.isBlank()) continue

            val estiloId = "martial_" + EncantosSolaresCatalog.normalize(nomePt)
                .replace(Regex("\\s+"), "_")
            val arma = estiloJson.optJSONObject("arma_do_estilo")
            val armadura = estiloJson.optJSONObject("armadura")
            val encantosJson = estiloJson.optJSONArray("encantos")
            val encantos = mutableListOf<EncantoArteMarcialDefinition>()

            if (encantosJson != null) {
                for (j in 0 until encantosJson.length()) {
                    val encantoJson = encantosJson.optJSONObject(j) ?: continue
                    val nome = encantoJson.optString("nome_pt", "").trim()
                    if (nome.isBlank()) continue
                    val id = "$estiloId::" + EncantosSolaresCatalog.normalize(nome)
                        .replace(Regex("\\s+"), "_")
                    val mins = encantoJson.optString("mins", "").trim()
                    val match = minsRegex.find(mins)
                    val minHabilidade = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val minEssencia = match?.groupValues?.get(2)?.toIntOrNull() ?: 0

                    val secoesEspeciais = encantoJson.optJSONObject("secoes_especiais")
                    val descricao = buildString {
                        append(encantoJson.optString("texto", "").trim())
                        if (secoesEspeciais != null) {
                            val keys = secoesEspeciais.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val value = secoesEspeciais.optString(key, "").trim()
                                if (value.isNotBlank()) {
                                    // Bug corrigido: "\\n\\n" (com escape duplo) produzia o texto
                                    // literal "\n\n" na tela, em vez de uma quebra de parágrafo real.
                                    // Kotlin já usa \n como escape de nova linha em string comum —
                                    // não precisa (e não deve) escapar a barra de novo.
                                    if (isNotBlank()) append("\n\n")
                                    append(key).append(": ").append(value)
                                }
                            }
                        }
                    }

                    encantos += EncantoArteMarcialDefinition(
                        id = id,
                        estiloId = estiloId,
                        habilidade = nomePt,
                        nome = nome,
                        nomeIngles = encantoJson.optString("nome_en", "").trim(),
                        custo = encantoJson.optString("custo", "").trim(),
                        minsTexto = mins,
                        minHabilidade = minHabilidade,
                        minEssencia = minEssencia,
                        tipo = encantoJson.optString("tipo", "").trim(),
                        palavrasChave = encantoJson.optJSONArray("palavras_chave")?.let { arr ->
                            (0 until arr.length()).map { arr.optString(it).trim() }.filter { it.isNotBlank() }.joinToString(", ")
                        } ?: "",
                        duracao = encantoJson.optString("duracao", "").trim(),
                        preRequisitos = encantoJson.optPrerequisitosJson().trim(),
                        descricao = descricao,
                        quadros = emptyList()
                    )
                }
            }

            estilos += EstiloArteMarcialDefinition(
                id = estiloId,
                nomePt = nomePt,
                nomeEn = estiloJson.optString("nome_en", "").trim(),
                descricao = estiloJson.optString("descricao", "").trim(),
                armaDoEstiloTexto = arma?.optString("texto")?.trim()?.takeIf { it.isNotBlank() },
                armaDoEstiloModo = arma?.optString("modo", "")?.trim() ?: "",
                armasEspecificas = arma?.optJSONArray("armas_especificas")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it).trim() }.filter { it.isNotBlank() }
                } ?: emptyList(),
                armaduraTexto = armadura?.optString("texto")?.trim()?.takeIf { it.isNotBlank() },
                armaduraCategoria = armadura?.optString("categoria", "")?.trim() ?: "",
                habilidadesComplementares = estiloJson.optString("habilidades_complementares", "")
                    .trim().takeIf { it.isNotBlank() },
                tiposExaltadosPermitidos = ArtesMarciaisAvailability.tiposPermitidos(nomePt),
                encantos = encantos
            )
        }

        definitions = estilos
        porId = definitions.associateBy { it.id }
        porNome = definitions.associateBy { EncantosSolaresCatalog.normalize(it.nomePt) }
        encantoPorId = definitions.flatMap { it.encantos }.associateBy { it.id }
        encantoPorNome = definitions.flatMap { it.encantos }
            .associateBy { EncantosSolaresCatalog.normalize(it.nome) }
        encantosPorEstilo = definitions.associate { it.id to it.encantos }
    }

    fun porId(id: String): EstiloArteMarcialDefinition? = porId[id]

    fun porNome(nome: String): EstiloArteMarcialDefinition? =
        porNome[EncantosSolaresCatalog.normalize(nome)]

    /** Retorna apenas estilos disponíveis para o tipo de Exaltado informado. */
    fun disponiveisPara(tipoExaltado: TipoExaltadoEncontro): List<EstiloArteMarcialDefinition> =
        definitions.filter { tipoExaltado in it.tiposExaltadosPermitidos }

    fun estaDisponivelPara(estilo: String, tipoExaltado: TipoExaltadoEncontro): Boolean =
        porNome(estilo)?.let { tipoExaltado in it.tiposExaltadosPermitidos } == true

    fun estiloDoEncanto(id: String): EstiloArteMarcialDefinition? =
        encantoPorId[id]?.let { encanto -> porId[encanto.estiloId] }

    fun encantoPorId(id: String): EncantoArteMarcialDefinition? = encantoPorId[id]

    fun encantoPorNome(nome: String): EncantoArteMarcialDefinition? =
        encantoPorNome[EncantosSolaresCatalog.normalize(nome)]

    fun encantosDoEstilo(estilo: String): List<EncantoArteMarcialDefinition> =
        (porId(estilo) ?: porNome(estilo))?.let { encantosPorEstilo[it.id].orEmpty() } ?: emptyList()
}
