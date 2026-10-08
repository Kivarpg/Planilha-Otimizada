package com.example.data

import com.example.model.Encanto
import org.json.JSONObject

/**
 * Converte Encantos/Feitiços adquiridos em referências compactas (catálogo +
 * ID numérico) em vez de duplicar nome, custo, descrição etc. — que já
 * existem, idênticos, no catálogo embutido no próprio app.
 *
 * Motivação (medido antes de implementar): um encanto serializado por
 * completo ocupa em média ~900 caracteres; só a referência, ~5. Numa planilha
 * com muitos encantos, isso é a diferença entre um código genuinamente
 * grande e um QR Code confiável.
 *
 * Formato por item (chaves curtas de propósito — o payload inteiro ainda
 * passa por DEFLATE depois):
 *   "c": catálogo — "S" (Encantos Solares) ou "F" (Feitiçaria). Ausente
 *        significa "não bateu com nenhum catálogo" — nesse caso o item
 *        inteiro é serializado por extenso (fallback de segurança, nunca
 *        perde dado por não encontrar uma correspondência).
 *   "i": ID numérico dentro do catálogo indicado.
 *   "h": habilidadeVinculada, só incluída quando DIFERE da habilidade
 *        própria do catálogo — é o caso de golpes de Arte Marcial, onde o
 *        nome do estilo é escolhido pelo jogador e não existe em catálogo
 *        nenhum.
 *   "p": pinOrder, só incluído quando definido (Artefatos fixados).
 *   "id": o id (UUID) da própria aquisição — preservado pra manter
 *        identidade estável em edições dentro da mesma sessão; ao
 *        importar em outro aparelho, essa identidade não precisa
 *        corresponder a nada externo, mas preservá-la evita reordenação
 *        supérflua se o mesmo código for processado mais de uma vez.
 */
object PoderesReferenciaCodec {

    fun codificar(charms: List<Encanto>, encantosSolares: List<EncantoSolarDefinition>, feiticos: List<FeiticoDefinition>): List<JSONObject> {
        // PERFORMANCE: os catálogos podem conter centenas de definições.
        // O codec precisa de lookup por nome, então materializamos cada índice
        // diretamente em vez de associateBy + estruturas intermediárias.
        val porNomeEncanto = buildMap<String, EncantoSolarDefinition>(encantosSolares.size) {
            for (def in encantosSolares) put(normalizar(def.nome), def)
        }
        val porNomeFeitico = buildMap<String, FeiticoDefinition>(feiticos.size) {
            for (def in feiticos) put(normalizar(def.nome), def)
        }

        return charms.map { charm ->
            val obj = JSONObject()
            obj.put("id", charm.id)
            charm.pinOrder?.let { obj.put("p", it) }

            val nomeNormalizado = normalizar(charm.nome)
            val defEncanto = porNomeEncanto[nomeNormalizado]
            val defFeitico = porNomeFeitico[nomeNormalizado]

            when {
                defEncanto != null -> {
                    obj.put("c", "S")
                    obj.put("i", defEncanto.id.toIntOrNull() ?: return@map fallbackCompleto(charm))
                    if (!mesmoTexto(charm.habilidadeVinculada, defEncanto.habilidade)) {
                        obj.put("h", charm.habilidadeVinculada)
                    }
                }
                defFeitico != null -> {
                    obj.put("c", "F")
                    obj.put("i", defFeitico.id.toIntOrNull() ?: return@map fallbackCompleto(charm))
                }
                else -> return@map fallbackCompleto(charm)
            }
            obj
        }
    }

    fun decodificar(
        itens: List<JSONObject>,
        encantosSolares: List<EncantoSolarDefinition>,
        feiticos: List<FeiticoDefinition>
    ): List<Encanto> {
        // PERFORMANCE: lookup direto por ID durante a decodificação compacta.
        // buildMap evita a cadeia associateBy e deixa a capacidade conhecida
        // para os catálogos estáveis recebidos pelo codec.
        val porIdEncanto = buildMap<String, EncantoSolarDefinition>(encantosSolares.size) {
            for (def in encantosSolares) put(def.id, def)
        }
        val porIdFeitico = buildMap<String, FeiticoDefinition>(feiticos.size) {
            for (def in feiticos) put(def.id, def)
        }

        return itens.mapNotNull { obj ->
            val catalogo = obj.optString("c", "")
            if (catalogo.isBlank()) {
                // Sem "c": item veio no formato completo (fallback ou
                // código antigo, de antes desta mudança) — decodifica como
                // um Encanto normal, sem passar pelo catálogo.
                return@mapNotNull runCatching { ModelJsonCodecs.encantoFromJson(obj) }.getOrNull()
            }
            val id = obj.optInt("i", -1).takeIf { it >= 0 }?.toString() ?: return@mapNotNull null
            val base = when (catalogo) {
                "S" -> porIdEncanto[id]?.let { def ->
                    val base = def.toEncanto()
                    val habilidade = obj.optString("h", "").ifBlank { def.habilidade }
                    base.copy(habilidadeVinculada = habilidade)
                }
                "F" -> porIdFeitico[id]?.toEncanto()
                else -> null
            } ?: return@mapNotNull null

            base.copy(
                id = obj.optString("id", base.id),
                pinOrder = obj.optInt("p", -1).let { if (it in 1..5) it else null }
            )
        }
    }

    private fun fallbackCompleto(charm: Encanto): JSONObject = JSONObject(ModelJsonCodecs.encantoToMap(charm))

    private fun normalizar(s: String): String = s.trim().lowercase()
    private fun mesmoTexto(a: String, b: String): Boolean = normalizar(a) == normalizar(b)
}
