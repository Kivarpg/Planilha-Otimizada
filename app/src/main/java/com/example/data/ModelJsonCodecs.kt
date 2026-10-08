package com.example.data

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** JSON boundary helpers for small domain value objects. Domain models stay JSON-agnostic. */
object ModelJsonCodecs {
    private const val MAX_ATTACKS = 64
    fun armaToMap(value: Arma): Map<String, Any> = mapOf(
        "id" to value.id, "nome" to value.nome,
        "habilidadeVinculada" to value.habilidadeVinculada, "atributoBriga" to (value.atributoBriga ?: ""),
        "tipoArma" to value.tipoArma, "categoriaPeso" to value.categoriaPeso,
        "modificadaManualmente" to value.modificadaManualmente, "iniciativa" to value.iniciativa,
        "decisivo" to value.decisivo, "defesa" to value.defesa, "dano" to value.dano,
        "danoMinimo" to value.danoMinimo, "equipada" to value.equipada,
        "ataqueDesarmado" to value.ataqueDesarmado,
        "motesPessoaisComitados" to value.motesPessoaisComitados,
        "motesPerifericosComitados" to value.motesPerifericosComitados
    )
    fun armaFromJson(map: JSONObject) = Arma(
        id = map.optString("id", UUID.randomUUID().toString()), nome = map.optString("nome", ""),
        habilidadeVinculada = map.optString("habilidadeVinculada", "Armas Brancas"),
        atributoBriga = map.optString("atributoBriga", "").ifBlank { null },
        tipoArma = map.optString("tipoArma", "Mundana"), categoriaPeso = map.optString("categoriaPeso", "Leve"),
        modificadaManualmente = map.optBoolean("modificadaManualmente", false), iniciativa = map.optString("iniciativa", "0"),
        decisivo = map.optString("decisivo", "0"), defesa = map.optInt("defesa", 0), dano = map.optString("dano", "0"),
        danoMinimo = map.optString("danoMinimo", ""), equipada = map.optBoolean("equipada", false),
        ataqueDesarmado = map.optBoolean("ataqueDesarmado", false),
        motesPessoaisComitados = map.optInt("motesPessoaisComitados", 0),
        motesPerifericosComitados = map.optInt("motesPerifericosComitados", 0)
    )

    fun armaduraToMap(value: Armadura): Map<String, Any> = mapOf(
        "id" to value.id, "nome" to value.nome, "tipoArmadura" to value.tipoArmadura,
        "categoriaPeso" to value.categoriaPeso, "absorcao" to value.absorcao, "dureza" to value.dureza,
        "penalidadeMobilidade" to value.penalidadeMobilidade, "equipada" to value.equipada,
        "motesPessoaisComitados" to value.motesPessoaisComitados,
        "motesPerifericosComitados" to value.motesPerifericosComitados
    )
    fun armaduraFromJson(map: JSONObject) = Armadura(
        id = map.optString("id", UUID.randomUUID().toString()), nome = map.optString("nome", ""),
        tipoArmadura = map.optString("tipoArmadura", "Mundana"), categoriaPeso = map.optString("categoriaPeso", "Leve"),
        absorcao = map.optInt("absorcao", 0), dureza = map.optInt("dureza", 0),
        penalidadeMobilidade = map.optInt("penalidadeMobilidade", 0), equipada = map.optBoolean("equipada", false),
        motesPessoaisComitados = map.optInt("motesPessoaisComitados", 0),
        motesPerifericosComitados = map.optInt("motesPerifericosComitados", 0)
    )

    fun caixaVitalidadeToMap(value: CaixaVitalidade): Map<String, Any> = mapOf(
        "id" to value.id, "penalidade" to value.penalidade, "tipoDano" to value.tipoDano,
        "isPermanente" to value.isPermanente, "origemAutomatica" to (value.origemAutomatica ?: "")
    )
    fun caixaVitalidadeFromJson(map: JSONObject) = CaixaVitalidade(
        id = map.optString("id", UUID.randomUUID().toString()), penalidade = map.optString("penalidade", "-0"),
        tipoDano = map.optInt("tipoDano", 0), isPermanente = map.optBoolean("isPermanente", false),
        origemAutomatica = map.optString("origemAutomatica", "").ifBlank { null }
    )

    fun encantoToMap(value: Encanto): Map<String, Any> = mapOf(
        "id" to value.id, "nome" to value.nome, "nomeIngles" to value.nomeIngles,
        "habilidadeVinculada" to value.habilidadeVinculada, "custo" to value.custo, "mins" to value.mins,
        "minHabilidade" to value.minHabilidade, "minEssencia" to value.minEssencia, "tipo" to value.tipo,
        "palavrasChave" to value.palavrasChave, "duracao" to value.duracao, "preRequisitos" to value.preRequisitos,
        "descricao" to value.descricao, "quadros" to JSONArray().apply { value.quadros.forEach { put(JSONObject().put("titulo", it.titulo).put("texto", it.texto)) } },
        "categoria" to value.categoria, "circulo" to value.circulo, "pinOrder" to (value.pinOrder ?: -1)
    )
    fun encantoFromJson(map: JSONObject) = Encanto(
        id = map.optString("id", UUID.randomUUID().toString()), nome = map.optString("nome", ""),
        nomeIngles = map.optString("nomeIngles", ""), habilidadeVinculada = map.optString("habilidadeVinculada", ""),
        custo = map.optString("custo", ""), mins = map.optString("mins", ""), minHabilidade = map.optInt("minHabilidade", 0),
        minEssencia = map.optInt("minEssencia", 0), tipo = map.optString("tipo", ""), palavrasChave = map.optString("palavrasChave", ""),
        duracao = map.optString("duracao", ""), preRequisitos = map.optString("preRequisitos", ""), descricao = map.optString("descricao", ""),
        quadros = map.optJSONArray("quadros")?.let { arr -> (0 until arr.length()).map { i -> arr.optJSONObject(i)?.let { q -> EncantoQuadro(q.optString("titulo", ""), q.optString("texto", "")) } ?: EncantoQuadro() } } ?: emptyList(),
        categoria = map.optString("categoria", if (map.optBoolean("isFeitico", false)) "Feitiçaria" else "Encanto"),
        circulo = map.optString("circulo", ""), pinOrder = map.optInt("pinOrder", -1).takeIf { it in 1..5 }
    )

    fun meritoToMap(value: Merito): Map<String, Any> = mapOf("id" to value.id, "nome" to value.nome, "valor" to value.valor, "categoria" to value.categoria, "preRequisitoTexto" to value.preRequisitoTexto, "detalhe" to value.detalhe, "origemAutomatica" to value.origemAutomatica)
    fun meritoFromJson(map: JSONObject) = Merito(map.optString("id", UUID.randomUUID().toString()), map.optString("nome", ""), map.optInt("valor", 1), map.optString("categoria", ""), map.optString("preRequisitoTexto", ""), map.optString("detalhe", ""), map.optString("origemAutomatica", ""))

    fun intimidadeToMap(value: Intimidade) = mapOf("id" to value.id, "nome" to value.nome, "tipo" to value.tipo, "intensidade" to value.intensidade)
    fun intimidadeFromJson(map: JSONObject) = Intimidade(map.optString("id", UUID.randomUUID().toString()), map.optString("nome", ""), map.optString("tipo", "Laço"), map.optString("intensidade", "Menor"))
    fun especializacaoToMap(value: Especializacao) = mapOf("id" to value.id, "nome" to value.nome, "habilidade" to value.habilidade, "valor" to value.valor)
    fun especializacaoFromJson(map: JSONObject) = Especializacao(map.optString("id", UUID.randomUUID().toString()), map.optString("nome", ""), map.optString("habilidade", ""), map.optInt("valor", 1))
    fun habilidadeCustomizadaToMap(value: HabilidadeCustomizada) = mapOf("id" to value.id, "nome" to value.nome, "valor" to value.valor)
    fun habilidadeCustomizadaFromJson(map: JSONObject) = HabilidadeCustomizada(map.optString("id", UUID.randomUUID().toString()), map.optString("nome", "Nova Arte Marcial"), map.optInt("valor", 0))
    fun gastoExperienciaToMap(value: GastoExperiencia) = mapOf("descricao" to value.descricao, "custo" to value.custo)
    fun gastoExperienciaFromJson(map: JSONObject) = GastoExperiencia(map.optString("descricao", ""), map.optInt("custo", 0))

    fun battleGroupToMap(value: BattleGroup): Map<String, Any?> = mapOf(
        "id" to value.id,
        "name" to value.name,
        "troopTypeName" to value.troopTypeName,
        "size" to value.size,
        "drill" to value.drill.name,
        "might" to value.might,
        "customStats" to value.customStats?.let { custom ->
            mapOf(
                "joinBattle" to custom.joinBattle,
                "attacks" to custom.attacks.map { attack ->
                    mapOf(
                        "name" to attack.name,
                        "attackBase" to attack.attackBase,
                        "damageBase" to attack.damageBase,
                        "minimumDice" to attack.minimumDice
                    )
                },
                "defenseBase" to custom.defenseBase,
                "magnitudeBase" to custom.magnitudeBase,
                "soakBase" to custom.soakBase,
                "senses" to custom.senses,
                "resolve" to custom.resolve,
                "resist" to custom.resist,
                "routDifficulty" to custom.routDifficulty,
                "perfectMorale" to custom.perfectMorale
            )
        },
        "currentMagnitude" to value.currentMagnitude
    )

    fun battleGroupFromJson(map: JSONObject): BattleGroup {
        val custom = map.optJSONObject("customStats")?.let { customMap ->
            val attacks = customMap.optJSONArray("attacks")?.let { array ->
                (0 until minOf(array.length(), MAX_ATTACKS)).map { index ->
                    val attack = array.getJSONObject(index)
                    BattleGroupCustomAttack(
                        name = attack.optString("name", ""),
                        attackBase = attack.optInt("attackBase", 0),
                        damageBase = attack.optInt("damageBase", 0),
                        minimumDice = if (attack.isNull("minimumDice")) null else attack.optInt("minimumDice")
                    )
                }
            } ?: emptyList()
            BattleGroupCustomStats(
                joinBattle = customMap.optInt("joinBattle", 0),
                attacks = attacks,
                defenseBase = customMap.optInt("defenseBase", 0),
                magnitudeBase = customMap.optInt("magnitudeBase", 0),
                soakBase = customMap.optInt("soakBase", 0),
                senses = customMap.optInt("senses", 0),
                resolve = customMap.optInt("resolve", 0),
                resist = customMap.optString("resist", ""),
                routDifficulty = if (customMap.isNull("routDifficulty")) null else customMap.optInt("routDifficulty"),
                perfectMorale = customMap.optBoolean("perfectMorale", false)
            )
        }
        return BattleGroup(
            id = map.optString("id", UUID.randomUUID().toString()),
            name = map.optString("name", ""),
            troopTypeName = if (map.isNull("troopTypeName")) null else map.optString("troopTypeName", "").ifBlank { null },
            size = map.optInt("size", 1).coerceIn(1, 5),
            drill = runCatching { BattleGroupDrill.valueOf(map.optString("drill", BattleGroupDrill.POOR.name)) }.getOrDefault(BattleGroupDrill.POOR),
            might = map.optInt("might", 0).coerceIn(0, 3),
            customStats = custom,
            currentMagnitude = if (map.isNull("currentMagnitude")) null else map.optInt("currentMagnitude")
        )
    }

    fun npcToMap(value: Npc) = mapOf("id" to value.id, "nome" to value.nome, "lealdade" to value.lealdade, "tipo" to value.tipo, "descricao" to value.descricao)
    fun npcFromJson(map: JSONObject) = Npc(map.optString("id", UUID.randomUUID().toString()), map.optString("nome", ""), map.optString("lealdade", "Neutro"), map.optString("tipo", ""), map.optString("descricao", ""))
}
