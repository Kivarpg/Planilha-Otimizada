package com.example.data

import android.content.Context
import org.json.JSONObject

data class MeritoDefinition(
    val id: String,
    val nome: String,
    val categoria: String, // "normal" | "sobrenatural"
    val tipo: String, // "historia" | "inato" | "adquirivel"
    val custosPermitidos: List<Int>,
    val descricao: String,
    val preRequisitos: List<String>,
    val podeSerAdquiridoNovamente: Boolean,
    // null = disponível pra qualquer template. Caso contrário, restringe
    // o Mérito exclusivamente ao valor de CharacterType indicado (ex.:
    // "SangueDeDragao") — pedido explícito do usuário.
    val restritoAoTemplate: String? = null
)

// Fonte oficial: app/src/main/assets/meritos.json — extraído do documento
// de regras original (méritos sobrenaturais marcados em vermelho no
// documento-fonte determinam a categoria; a cor em si nunca aparece no
// app nem no JSON, só usada como sinal durante a extração).
class MeritosCatalog(context: Context) {
    val definitions: List<MeritoDefinition>
    val normais: List<MeritoDefinition>
    val sobrenaturais: List<MeritoDefinition>
    private val porNome: Map<String, MeritoDefinition>

    init {
        val json = context.assets.open("meritos.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(json)
        val arr = root.optJSONArray("meritos")
            ?: throw IllegalStateException("meritos.json sem o campo 'meritos'.")

        val list = mutableListOf<MeritoDefinition>()
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            val nome = m.optString("nome", "").trim()
            if (nome.isBlank()) continue
            val custosArr = m.optJSONArray("custosPermitidos")
            val custos = if (custosArr != null) (0 until custosArr.length()).map { custosArr.optInt(it, 0) } else emptyList()
            val preReqArr = m.optJSONArray("preRequisitos")
            val preReqs = if (preReqArr != null) (0 until preReqArr.length()).map { preReqArr.optString(it, "") }.filter { it.isNotBlank() } else emptyList()

            list += MeritoDefinition(
                id = m.optString("id", nome),
                nome = nome,
                categoria = m.optString("categoria", "normal"),
                tipo = m.optString("tipo", "adquirivel"),
                custosPermitidos = custos,
                descricao = m.optString("descricao", ""),
                preRequisitos = preReqs,
                podeSerAdquiridoNovamente = m.optBoolean("podeSerAdquiridoNovamente", false),
                restritoAoTemplate = m.optString("restritoAoTemplate", "").ifBlank { null }
            )
        }
        definitions = list
        normais = list.filter { it.categoria == "normal" }.sortedBy { it.nome }
        sobrenaturais = list.filter { it.categoria == "sobrenatural" }.sortedBy { it.nome }
        porNome = list.associateBy { normalizar(it.nome) }
    }

    fun porNome(nome: String): MeritoDefinition? = porNome[normalizar(nome)]

    private fun normalizar(s: String): String = s.trim().lowercase()
}

// Resultado da checagem de um pré-requisito individual — usado pra montar
// a lista exibida ao usuário, com o texto original preservado sempre
// (mesmo quando dá pra verificar automaticamente), pra clareza.
data class ChecagemPreRequisito(
    val textoOriginal: String,
    val atendido: Boolean?, // null = não dá pra verificar automaticamente (texto livre)
)

// Interpreta o padrão simples "Nome[, Nome, ... ou Nome] ••" (um ou mais
// atributos/habilidades alternativos, qualquer um bastando, no nível
// indicado pelas bolinhas). Cobre 10 dos 14 pré-requisitos reais do
// documento original — os que não seguem esse padrão (ex.: "Outro Mérito
// Sobrenatural fisicamente evidente...") voltam como "não verificável",
// exibidos como texto pro próprio usuário julgar, nunca bloqueando nem
// liberando automaticamente algo que o app não consegue confirmar de
// verdade.
private val PADRAO_PRE_REQUISITO_SIMPLES = Regex("^(.+?)\\s*(•+)$")

fun checarPreRequisito(
    texto: String,
    attributes: Map<String, Int>,
    abilities: Map<String, Int>
): ChecagemPreRequisito {
    val m = PADRAO_PRE_REQUISITO_SIMPLES.find(texto.trim())
        ?: return ChecagemPreRequisito(texto, null)
    val nivel = m.groupValues[2].length
    val nomes = m.groupValues[1].split(",", " ou ").map { it.trim() }.filter { it.isNotBlank() }
    val atendido = nomes.any { nome ->
        (attributes[nome] ?: 0) >= nivel || (abilities[nome] ?: 0) >= nivel
    }
    return ChecagemPreRequisito(texto, atendido)
}

// Retorna os nomes dos Méritos JÁ ADQUIRIDOS cujo pré-requisito deixaria
// de ser atendido se os atributos/habilidades da planilha passassem a ser
// os informados em novosAtributos/novasHabilidades (o restante da planilha
// não muda). Usado antes de aplicar uma REDUÇÃO de atributo/habilidade,
// pra avisar o jogador do que ele perderia. Só considera pré-requisitos
// no padrão simples verificável (ver checarPreRequisito) — texto livre
// não pode ser confirmado como quebrado, então nunca aparece aqui.
fun meritosQuebradosPor(
    merits: List<com.example.model.Merito>,
    catalog: MeritosCatalog,
    attributesAntes: Map<String, Int>,
    abilitiesAntes: Map<String, Int>,
    novosAtributos: Map<String, Int>,
    novasHabilidades: Map<String, Int>
): List<String> {
    val quebrados = mutableListOf<String>()
    for (merito in merits) {
        val definicao = catalog.porNome(merito.nome)
        val preRequisitos = definicao?.preRequisitos
            ?: merito.preRequisitoTexto.takeIf { it.isNotBlank() }?.let { listOf(it) }
            ?: continue
        for (texto in preRequisitos) {
            val antes = checarPreRequisito(texto, attributesAntes, abilitiesAntes)
            val depois = checarPreRequisito(texto, novosAtributos, novasHabilidades)
            if (antes.atendido == true && depois.atendido == false) {
                quebrados += merito.nome
                break
            }
        }
    }
    return quebrados
}
