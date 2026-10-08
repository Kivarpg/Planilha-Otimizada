package com.example.data

import com.example.model.CharacterType
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * JSON persistence boundary for CharacterSheet.
 *
 * Keeping this codec outside the domain model prevents the model from knowing
 * about JSONObject/JSONArray and makes future migration to another serializer
 * (including kotlinx.serialization) an isolated operation. The JSON schema and
 * legacy migrations are intentionally preserved for backward compatibility.
 */
object CharacterSheetJsonCodec {
    fun encode(sheet: CharacterSheet): String {
        val obj = JSONObject()
        obj.put("id", sheet.id)
        obj.put("numeroSequencial", sheet.numeroSequencial)
        obj.put("dataSalvamento", sheet.dataSalvamento)
        obj.put("nome", sheet.nome)
        obj.put("jogador", sheet.jogador)
        obj.put("conceito", sheet.conceito)
        obj.put("descricaoAnima", sheet.descricaoAnima)
        obj.put("falhaVirtude", sheet.falhaVirtude)
        obj.put("limiteGatilho", sheet.limiteGatilho)
        obj.put("limiteContador", sheet.limiteContador)
        obj.put("essencia", sheet.essencia)
        obj.put("tipoPersonagem", sheet.tipoPersonagem)
        obj.put("casta", sheet.casta.name)
        obj.put("castaEscolhida", sheet.castaEscolhida)
        obj.put("aspecto", sheet.aspecto)
        obj.put("lunarCasta", sheet.lunarCasta.name)
        obj.put("lunarCastaEscolhida", sheet.lunarCastaEscolhida)
        obj.put("lunarCasteAttributesEscolhidos", JSONArray(sheet.lunarCasteAttributesEscolhidos))
        obj.put("lunarFormaEspiritual", sheet.lunarFormaEspiritual)
        obj.put("lunarSinal", sheet.lunarSinal)
        obj.put("casteAbilities", JSONArray(sheet.casteAbilities))
        obj.put("favoredAbilities", JSONArray(sheet.favoredAbilities))
        obj.put("favoredAttributes", JSONArray(sheet.favoredAttributes))
        obj.put("supernalAbility", sheet.supernalAbility ?: JSONObject.NULL)
        obj.put("abilities", JSONObject().apply { sheet.abilities.forEach { (k, v) -> put(k, v) } })
        obj.put("attributes", JSONObject().apply { sheet.attributes.forEach { (k, v) -> put(k, v) } })
        obj.put("attributePriorities", JSONObject().apply { sheet.attributePriorities.forEach { (k, v) -> put(k, v) } })
        obj.put("specializations", JSONArray().apply { sheet.specializations.forEach { put(JSONObject(ModelJsonCodecs.especializacaoToMap(it))) } })
        obj.put("motesPessoaisGastos", sheet.motesPessoaisGastos)
        obj.put("motesPerifericosGastos", sheet.motesPerifericosGastos)
        obj.put("motesPessoaisComitados", sheet.motesPessoaisComitados)
        obj.put("motesPerifericosComitados", sheet.motesPerifericosComitados)
        obj.put("forcaVontadeBase", sheet.forcaVontadeBase)
        obj.put("forcaVontadeUsados", JSONArray(sheet.forcaVontadeUsados.toList()))
        obj.put("weapons", JSONArray().apply { sheet.weapons.forEach { put(JSONObject(ModelJsonCodecs.armaToMap(it))) } })
        obj.put("armaduras", JSONArray().apply { sheet.armaduras.forEach { put(JSONObject(ModelJsonCodecs.armaduraToMap(it))) } })
        obj.put("martialArts", JSONArray().apply { sheet.martialArts.forEach { put(JSONObject(ModelJsonCodecs.habilidadeCustomizadaToMap(it))) } })
        obj.put("healthBoxes", JSONArray().apply { sheet.healthBoxes.forEach { put(JSONObject(ModelJsonCodecs.caixaVitalidadeToMap(it))) } })
        obj.put("intimacies", JSONArray().apply { sheet.intimacies.forEach { put(JSONObject(ModelJsonCodecs.intimidadeToMap(it))) } })
        obj.put("linguaNativa", sheet.linguaNativa ?: JSONObject.NULL)
        obj.put("linguasAdicionais", JSONArray(sheet.linguasAdicionais))
        obj.put("merits", JSONArray().apply { sheet.merits.forEach { put(JSONObject(ModelJsonCodecs.meritoToMap(it))) } })
        obj.put("charms", JSONArray().apply { sheet.charms.forEach { put(JSONObject(ModelJsonCodecs.encantoToMap(it))) } })
        obj.put("battleGroups", JSONArray().apply { sheet.battleGroups.forEach { put(JSONObject(ModelJsonCodecs.battleGroupToMap(it))) } })
        obj.put("pertences", sheet.pertences)
        obj.put("sessoes", sheet.sessoes)
        obj.put("fichaConcluida", sheet.planilhaConcluida)
        obj.put("modoLivre", sheet.modoLivre)
        obj.put("experienciaGastaTotal", sheet.experienciaGastaTotal)
        obj.put("historicoExperiencia", JSONArray().apply { sheet.historicoExperiencia.forEach { put(JSONObject(ModelJsonCodecs.gastoExperienciaToMap(it))) } })
        obj.put("snapshotConclusao", sheet.snapshotConclusao)
        obj.put("iniciativaValor", sheet.iniciativaValor ?: JSONObject.NULL)
        return obj.toString()
    }

    fun decode(jsonStr: String): CharacterSheet {
        val obj = JSONObject(jsonStr)
        val castaEnum = try { Casta.valueOf(obj.optString("casta", Casta.Dawn.name)) } catch (_: Exception) { Casta.Dawn }
        val lunarCastaEnum = try { LunarCasta.valueOf(obj.optString("lunarCasta", LunarCasta.FullMoon.name)) } catch (_: Exception) { LunarCasta.FullMoon }
        val casteAbList = obj.optJSONArray("casteAbilities").toStringList()
        val favAbList = obj.optJSONArray("favoredAbilities").toStringList()
        val lunarCasteAttrList = obj.optJSONArray("lunarCasteAttributesEscolhidos").toStringList()
        val lunarFormaEspiritualLida = obj.optString("lunarFormaEspiritual", "")
        val lunarSinalLido = obj.optString("lunarSinal", "")
        val favAttrList = obj.optJSONArray("favoredAttributes").toStringList()
        val supAb = if (obj.has("supernalAbility") && !obj.isNull("supernalAbility")) obj.getString("supernalAbility").takeIf { it.isNotBlank() } else null

        val abMap = ExaltedConstants.ALL_25_ABILITIES.associateWith { 0 }.toMutableMap()
        obj.optJSONObject("abilities")?.let { o -> o.keys().forEach { k -> abMap[k] = o.optInt(k, 0) } }

        val attrMap = ExaltedConstants.DEFAULT_ATTRIBUTES.toMutableMap()
        obj.optJSONObject("attributes")?.let { o ->
            o.keys().forEach { k -> attrMap[k] = o.optInt(k, 1) }
            if (!o.has("Aparência") && o.has("Apelo")) { attrMap["Aparência"] = o.optInt("Apelo", 1); attrMap.remove("Apelo") }
        }

        val prioMap = mutableMapOf("Físicos" to "1º", "Sociais" to "2º", "Mentais" to "3º")
        obj.optJSONObject("attributePriorities")?.let { o ->
            prioMap["Físicos"] = o.optString("Físicos", "1º")
            prioMap["Sociais"] = o.optString("Sociais", "2º")
            prioMap["Mentais"] = o.optString("Mentais", "3º")
        }

        val specList = obj.optJSONArray("specializations").toObjectList { ModelJsonCodecs.especializacaoFromJson(it) }
        val weapList = obj.optJSONArray("weapons").toObjectList { ModelJsonCodecs.armaFromJson(it) }
        val armorList = obj.optJSONArray("armaduras").toObjectList { ModelJsonCodecs.armaduraFromJson(it) }.toMutableList()
        if (armorList.isEmpty()) obj.optJSONObject("armadura")?.let { armorList += ModelJsonCodecs.armaduraFromJson(it) }
        val martialArtsList = obj.optJSONArray("martialArts").toObjectList { ModelJsonCodecs.habilidadeCustomizadaFromJson(it) }
        val hbList = obj.optJSONArray("healthBoxes").toObjectList { ModelJsonCodecs.caixaVitalidadeFromJson(it) }.toMutableList().ifEmpty { ExaltedConstants.defaultHealthBoxes().toMutableList() }
        val intList = obj.optJSONArray("intimacies").toObjectList { ModelJsonCodecs.intimidadeFromJson(it) }
        val linguaNativaLida = if (obj.isNull("linguaNativa")) null else obj.optString("linguaNativa").ifBlank { null }
        val linguasAdicLidas = obj.optJSONArray("linguasAdicionais").toStringList()
        val merListLida = obj.optJSONArray("merits").toObjectList { ModelJsonCodecs.meritoFromJson(it) }
        // Migração da integração de Idiomas: planilhas antigas podem conter
        // idiomas adicionais sem os respectivos Méritos automáticos.
        // Méritos automáticos órfãos também são removidos.
        val idiomasAdicionaisNormalizados = linguasAdicLidas.distinctBy { it.trim().lowercase() }.filter { it.isNotBlank() }
        val meritosIdiomaManuais = merListLida.filterNot { it.origemAutomatica == "Idioma" }
        // PERFORMANCE: indexa os Méritos automáticos de Idioma uma única vez.
        // Antes, cada idioma adicional percorria novamente toda a lista de Méritos.
        val meritosIdiomaAutomaticosPorDetalhe = merListLida.asSequence()
            .filter { it.origemAutomatica == "Idioma" }
            .associateBy { it.detalhe.trim().lowercase() }
        val meritosIdiomaAutomaticos = idiomasAdicionaisNormalizados.map { idioma ->
            meritosIdiomaAutomaticosPorDetalhe[idioma.trim().lowercase()]
                ?: Merito(nome = "Idioma", valor = 1, categoria = "Idioma", detalhe = idioma, origemAutomatica = "Idioma")
        }
        val merList = meritosIdiomaManuais + meritosIdiomaAutomaticos
        val charmList = obj.optJSONArray("charms").toObjectList { ModelJsonCodecs.encantoFromJson(it) }
        val battleGroupList = obj.optJSONArray("battleGroups").toObjectList { ModelJsonCodecs.battleGroupFromJson(it) }
        val essenciaLida = obj.optInt("essencia", 1)
        val essenciaSegura = essenciaLida.coerceIn(1, 5)

        return CharacterSheet(
            id = obj.optString("id", UUID.randomUUID().toString()),
            numeroSequencial = obj.optInt("numeroSequencial", 0), dataSalvamento = obj.optString("dataSalvamento", ""),
            nome = obj.optString("nome", ""), jogador = obj.optString("jogador", ""), conceito = obj.optString("conceito", ""),
            descricaoAnima = obj.optString("descricaoAnima", ""), falhaVirtude = obj.optString("falhaVirtude", ""),
            limiteGatilho = obj.optString("limiteGatilho", ""), limiteContador = obj.optInt("limiteContador", 0),
            essencia = essenciaLida, tipoPersonagem = obj.optString("tipoPersonagem", CharacterType.SOLAR), casta = castaEnum, castaEscolhida = obj.optBoolean("castaEscolhida", false), aspecto = obj.optString("aspecto", ""),
            lunarCasta = lunarCastaEnum, lunarCastaEscolhida = obj.optBoolean("lunarCastaEscolhida", false),
            lunarCasteAttributesEscolhidos = lunarCasteAttrList,
            lunarFormaEspiritual = lunarFormaEspiritualLida,
            lunarSinal = lunarSinalLido,
            casteAbilities = casteAbList, favoredAbilities = favAbList, favoredAttributes = favAttrList, supernalAbility = supAb, abilities = abMap, attributes = attrMap,
            attributePriorities = prioMap, specializations = specList,
            motesPessoaisGastos = obj.optInt("motesPessoaisGastos", 0).coerceIn(0, essenciaSegura * 3 + 10),
            motesPerifericosGastos = obj.optInt("motesPerifericosGastos", 0).coerceIn(0, essenciaSegura * 7 + 26),
            motesPessoaisComitados = obj.optInt("motesPessoaisComitados", 0).coerceAtLeast(0), motesPerifericosComitados = obj.optInt("motesPerifericosComitados", 0).coerceAtLeast(0),
            forcaVontadeBase = obj.optInt("forcaVontadeBase", 5).coerceIn(5, 10),
            forcaVontadeUsados = obj.optJSONArray("forcaVontadeUsados")?.toIntSet() ?: emptySet(), weapons = weapList, armaduras = armorList,
            martialArts = martialArtsList, healthBoxes = hbList, intimacies = intList, linguaNativa = linguaNativaLida, linguasAdicionais = idiomasAdicionaisNormalizados,
            merits = merList, charms = charmList, battleGroups = battleGroupList, pertences = obj.optString("pertences", ""), sessoes = obj.optInt("sessoes", 0).coerceIn(0, 60),
            planilhaConcluida = obj.optBoolean("fichaConcluida", false), modoLivre = obj.optBoolean("modoLivre", false),
            experienciaGastaTotal = obj.optInt("experienciaGastaTotal", 0).coerceAtLeast(0),
            historicoExperiencia = obj.optJSONArray("historicoExperiencia").toObjectList { ModelJsonCodecs.gastoExperienciaFromJson(it) },
            snapshotConclusao = obj.optString("snapshotConclusao", ""),
            iniciativaValor = if (obj.has("iniciativaValor") && !obj.isNull("iniciativaValor")) obj.optInt("iniciativaValor").coerceIn(-999, 999) else null
        )
    }

    private fun JSONArray?.toStringList(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
    private fun JSONArray?.toIntSet(): Set<Int> = if (this == null) emptySet() else (0 until length()).map { optInt(it) }.toSet()
}

// Nível de arquivo (fora do object) — pedido do patch: NpcEncontroJsonCodec.kt
// também precisa chamar isso diretamente, e uma função-membro de extensão só
// fica em escopo implícito dentro do próprio object que a declara.
internal fun <T> JSONArray?.toObjectList(mapper: (JSONObject) -> T): List<T> = if (this == null) emptyList() else (0 until length()).map { mapper(getJSONObject(it)) }
