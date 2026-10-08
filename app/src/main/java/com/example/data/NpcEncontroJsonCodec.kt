package com.example.data

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lançada por [NpcEncontroJsonCodec.decode] quando o JSON de entrada está
 * estruturalmente corrompido (ex.: arquivo .save editado à mão, truncado,
 * ou de uma versão incompatível) e não pôde ser recuperado.
 *
 * Existe pra dar a qualquer chamador — atual ou futuro — um único tipo de
 * exceção documentado pra capturar, em vez de exigir que cada ponto de
 * chamada conheça e trate as exceções internas do org.json (JSONException
 * e afins) na unha.
 */
class NpcSaveCorruptedException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Versionamento explícito do formato persistido de [NpcEncontro].
 *
 * Saves anteriores à versão 372 não possuíam `schemaVersion`; eles são
 * tratados como schema 1 e migrados de forma incremental antes do decode.
 * O schema 2 não muda regras de jogo: apenas torna a evolução do formato
 * rastreável e impede que uma versão antiga do app leia silenciosamente um
 * save de schema futuro que ela não conhece.
 */
object NpcEncontroSchema {
    const val LEGACY_VERSION = 1
    const val CURRENT_VERSION = 5
}

/** Migra somente a representação JSON. Nunca altera regras de construção. */
private object NpcEncontroMigrationRegistry {
    fun migrate(source: JSONObject): JSONObject {
        val declared = source.optInt("schemaVersion", NpcEncontroSchema.LEGACY_VERSION)
        if (declared < NpcEncontroSchema.LEGACY_VERSION) {
            throw NpcSaveCorruptedException("schemaVersion de NPC inválido: $declared")
        }
        if (declared > NpcEncontroSchema.CURRENT_VERSION) {
            throw NpcSaveCorruptedException(
                "Save de NPC usa schemaVersion $declared, mais novo que o suportado (${NpcEncontroSchema.CURRENT_VERSION})"
            )
        }

        // Cópia defensiva: o chamador pode reutilizar o JSONObject original.
        val migrated = JSONObject(source.toString())
        var version = declared
        while (version < NpcEncontroSchema.CURRENT_VERSION) {
            when (version) {
                1 -> {
                    // v1 -> v2: o payload existente já é compatível. A migração
                    // formaliza a versão sem reinterpretar nenhum dado antigo.
                    version = 2
                    migrated.put("schemaVersion", version)
                }
                2 -> {
                    // v2 -> v3: IDs canônicos são metadados novos. Saves antigos
                    // continuam válidos por nome e serão hidratados pelo catálogo
                    // quando disponíveis; nenhuma escolha do personagem é alterada.
                    version = 3
                    migrated.put("schemaVersion", version)
                }
                3 -> {
                    // v3 -> v4: Limite passa a ser persistido para NPCs Solar/Lunar.
                    // Saves antigos recebem vazio; não inventamos um novo sorteio ao carregar.
                    version = 4
                    migrated.put("schemaVersion", version)
                    if (!migrated.has("limite")) migrated.put("limite", "")
                }
                4 -> {
                    // v4 -> v5: preserva somente Foco explicitamente escolhido.
                    // Saves antigos não permitem reconstruí-lo com segurança.
                    version = 5
                    migrated.put("schemaVersion", version)
                    if (!migrated.has("focoProgressaoExplicito")) migrated.put("focoProgressaoExplicito", JSONObject.NULL)
                }
                else -> throw NpcSaveCorruptedException("Não existe migrador para schemaVersion $version")
            }
        }
        migrated.put("schemaVersion", NpcEncontroSchema.CURRENT_VERSION)
        return migrated
    }
}

/** JSON persistence boundary for encounter NPCs. The existing schema is preserved. */
object NpcEncontroJsonCodec {

    // Limites defensivos do boundary JSON. NPCs gerados pelo app ficam
    // muito abaixo desses tetos; eles existem para impedir listas artificiais
    // ou dados corrompidos de provocar trabalho/memória desnecessários.
    private const val MAX_LISTA = 256
    private const val MAX_CHARMS = 512
    private const val MAX_HISTORICO_XP = 256
    private const val MAX_NOME_TEXTO = 500
    /**
     * Builds the JSON object once. The String API below remains the public
     * compatibility boundary, while EncounterNpcStore can persist the object
     * directly without reparsing its own JSON.
     */
    fun encodeObject(npc: NpcEncontro): JSONObject {
        val obj = JSONObject()
        obj.put("schemaVersion", NpcEncontroSchema.CURRENT_VERSION)
        obj.put("id", npc.id)
        obj.put("nome", npc.nome)
        obj.put("genero", npc.genero)
        obj.put("tipoExaltado", npc.tipoExaltado.name)
        obj.put("arquetipo", npc.arquetipo.name)
        obj.put("casta", npc.casta)
        obj.put("habilidadesFavorecidas", JSONArray().apply { npc.habilidadesFavorecidas.forEach { put(it) } })
        obj.put("lunarAtributosCasta", JSONArray().apply { npc.lunarAtributosCasta.forEach { put(it) } })
        obj.put("habilidadeSupernal", npc.habilidadeSupernal)
        obj.put("attributes", JSONObject().apply { npc.attributes.forEach { (k, v) -> put(k, v) } })
        obj.put("abilities", JSONObject().apply { npc.abilities.forEach { (k, v) -> put(k, v) } })
        obj.put("merits", JSONArray().apply { npc.merits.forEach { put(JSONObject(ModelJsonCodecs.meritoToMap(it))) } })
        obj.put("especialidades", JSONArray().apply { npc.especialidades.forEach { put(JSONObject().apply { put("habilidade", it.habilidade) }) } })
        obj.put("habilidadePrincipal", npc.habilidadePrincipal)
        obj.put("habilidadeDefensiva", npc.habilidadeDefensiva ?: JSONObject.NULL)
        obj.put("habilidadeSuporte", npc.habilidadeSuporte)
        obj.put("estiloArtesMarciais", npc.estiloArtesMarciais)
        obj.put("estilosArtesMarciaisAdicionais", JSONArray().apply {
            npc.estilosArtesMarciaisAdicionais.forEach(::put)
        })
        obj.put("charms", JSONArray().apply {
            npc.charms.forEach { c ->
                put(JSONObject().apply {
                    put("nome", c.nome)
                    put("habilidadeVinculada", c.habilidadeVinculada)
                    put("custo", c.custo)
                })
            }
        })
        obj.put("feiticos", JSONArray().apply {
            npc.feiticos.forEach { f ->
                put(JSONObject().apply {
                    put("nome", f.nome)
                    put("circulo", f.circulo)
                    put("custo", f.custo)
                })
            }
        })
        obj.put("feiticoInicialNome", npc.feiticoInicialNome ?: JSONObject.NULL)
        obj.put("corpoDeTouroCount", npc.corpoDeTouroCount)
        obj.put("essencia", npc.essencia)
        obj.put("limite", npc.limite)
        obj.put("focoProgressaoExplicito", npc.focoProgressaoExplicito ?: JSONObject.NULL)
        obj.put("idioma", npc.idioma)
        obj.put("formaEspiritual", npc.formaEspiritual)
        obj.put("formaEspiritualSecundaria", npc.formaEspiritualSecundaria)
        obj.put("lunarArchetypeTraits", JSONArray(npc.lunarArchetypeTraits))
        obj.put("lunarPrimaryArchetypeTraits", JSONArray(npc.lunarPrimaryArchetypeTraits))
        obj.put("sinal", npc.sinal)
        obj.put("motesPersonais", npc.motesPersonais)
        obj.put("motesPerifericos", npc.motesPerifericos)
        obj.put("forcaDeVontade", npc.forcaDeVontade)
        obj.put("forcaDeVontadeUsados", org.json.JSONArray(npc.forcaDeVontadeUsados.toList()))
        obj.put("xpAtual", npc.xpAtual)
        obj.put("xpGastoTotal", npc.xpGastoTotal)
        obj.put("primeiroXpRecebido", npc.primeiroXpRecebido)
        obj.put("historicoXpBatches", org.json.JSONArray().apply {
            npc.historicoXpBatches.forEach { lote ->
                put(JSONObject().apply {
                    put("xpGasto", lote.xpGasto)
                    put("nomesEncantosAdicionados", org.json.JSONArray(lote.nomesEncantosAdicionados))
                    put("habilidadeMelhorada", lote.habilidadeMelhorada ?: JSONObject.NULL)
                    put("pontosGanhosNaHabilidade", lote.pontosGanhosNaHabilidade)
                    put("especializacaoAdicionada", lote.especializacaoAdicionada ?: JSONObject.NULL)
                    put("pontosForcaDeVontadeComprados", lote.pontosForcaDeVontadeComprados)
                })
            }
        })
        obj.put("arma", npc.arma?.let {
            JSONObject().apply {
                put("nome", it.nome); put("peso", it.peso); put("tipo", it.tipo)
                put("precisao", it.precisao); put("dano", it.dano); put("defesa", it.defesa)
                put("motesComprometidos", it.motesComitados)
                put("etiquetas", JSONArray(it.etiquetas))
            }
        } ?: JSONObject.NULL)
        obj.put("armadura", npc.armadura?.let {
            JSONObject().apply {
                put("nome", it.nome); put("peso", it.peso); put("tipo", it.tipo); put("absorcao", it.absorcao)
                put("dureza", it.dureza); put("penalidadeMobilidade", it.penalidadeMobilidade)
                put("motesComprometidos", it.motesComitados)
                put("marcadores", JSONArray(it.marcadores))
                put("custoMeritoArtefato", it.custoMeritoArtefato)
            }
        } ?: JSONObject.NULL)
        obj.put("acaoPrincipal", npc.acaoPrincipal)
        obj.put("acaoDecisiva", npc.acaoDecisiva)
        obj.put("defesaPrimaria", npc.defesaPrimaria ?: JSONObject.NULL)
        obj.put("esquiva", npc.esquiva)
        obj.put("absorcaoNatural", npc.absorcaoNatural)
        obj.put("absorcaoArmadura", npc.absorcaoArmadura)
        obj.put("absorcao", npc.absorcao)
        obj.put("dureza", npc.dureza)
        obj.put("perseveranca", npc.perseveranca)
        obj.put("astucia", npc.astucia)
        obj.put("juntarABatalha", npc.juntarABatalha)
        obj.put("investida", npc.investida)
        obj.put("desengajamento", npc.desengajamento)
        obj.put("penalidadeClashDefesa", npc.penalidadeClashDefesa)
        obj.put("penalidadeAtaquesDefesa", npc.penalidadeAtaquesDefesa)
        obj.put("healthBoxes", JSONArray().apply { npc.healthBoxes.forEach { put(JSONObject(ModelJsonCodecs.caixaVitalidadeToMap(it))) } })
        obj.put("iniciativaAtual", npc.iniciativaAtual)
        obj.put("dano", npc.dano)
        obj.put("alertasValidacao", JSONArray().apply { npc.alertasValidacao.forEach { put(it) } })
        return obj
    }

    fun encode(npc: NpcEncontro): String = encodeObject(npc).toString()

    /**
     * @throws NpcSaveCorruptedException se [json] não for um objeto JSON válido,
     * ou se sua estrutura estiver corrompida a ponto de impedir a leitura do NPC.
     * Entradas individuais malformadas dentro de listas (ex.: um charm que não
     * é um objeto) são puladas em vez de abortar a leitura inteira.
     */
    fun decode(json: String): NpcEncontro {
        try {
            val obj = NpcEncontroMigrationRegistry.migrate(JSONObject(json))
            val attrMap = mutableMapOf<String, Int>()
            obj.optJSONObject("attributes")?.let { o ->
                val keys = o.keys()
                while (keys.hasNext()) { val k = keys.next(); attrMap[k] = o.optInt(k, 1) }
            }
            val abMap = mutableMapOf<String, Int>()
            obj.optJSONObject("abilities")?.let { o ->
                val keys = o.keys()
                while (keys.hasNext()) { val k = keys.next(); abMap[k] = o.optInt(k, 0) }
            }
            val meritsList = mutableListOf<Merito>()
            obj.optJSONArray("merits")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_LISTA)) {
                    arr.optJSONObject(i)?.let { meritsList.add(ModelJsonCodecs.meritoFromJson(it)) }
                }
            }
            val especializacoesList = mutableListOf<EspecialidadeEncontro>()
            obj.optJSONArray("especialidades")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_LISTA)) {
                    arr.optJSONObject(i)?.let { especializacoesList.add(EspecialidadeEncontro(it.optString("habilidade", ""))) }
                }
            }
            val favorecidasList = mutableListOf<String>()
            obj.optJSONArray("habilidadesFavorecidas")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_LISTA)) favorecidasList.add(arr.optString(i, ""))
            }
            val lunarAtributosCastaList = mutableListOf<String>()
            obj.optJSONArray("lunarAtributosCasta")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_LISTA)) lunarAtributosCastaList.add(arr.optString(i, ""))
            }
            val charmsList = mutableListOf<EncantoEncontro>()
            obj.optJSONArray("charms")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_CHARMS)) {
                    val c = arr.optJSONObject(i) ?: continue
                    charmsList.add(
                        EncantoEncontro(
                            nome = c.optString("nome", ""),
                            habilidadeVinculada = c.optString("habilidadeVinculada", ""),
                            custo = c.optString("custo", "")
                        )
                    )
                }
            }
            val feiticosList = mutableListOf<FeiticoEncontro>()
            obj.optJSONArray("feiticos")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_LISTA)) {
                    val f = arr.optJSONObject(i) ?: continue
                    feiticosList.add(
                        FeiticoEncontro(
                            nome = f.optString("nome", ""),
                            circulo = f.optString("circulo", ""),
                            custo = f.optString("custo", "")
                        )
                    )
                }
            }
            val alertas = mutableListOf<String>()
            obj.optJSONArray("alertasValidacao")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_LISTA)) {
                    val alerta = arr.optString(i, "")
                    val avisoEsperado = alerta.startsWith("Nenhum Atributo ") ||
                        alerta.startsWith("Corpo de Touro não foi adquirido") || alerta.startsWith("Combatente físico sem Corpo de Touro") ||
                        alerta.startsWith("Círculo de Magia não foi sorteado") ||
                        (alerta.startsWith("Só ") && (alerta.contains("Feitiço(s) — o esperado pro Mental é pelo menos 4.") || alerta.contains("apesar do acesso à Feitiçaria"))) ||
                        alerta.startsWith("Só ") && alerta.contains("Encantos iniciais elegíveis") ||
                        alerta.startsWith("Nenhum Feitiço selecionado — Social costuma ter 1-2")
                    if (alerta.isNotBlank() && !avisoEsperado) alertas.add(alerta)
                }
            }
            val healthList = mutableListOf<CaixaVitalidade>()
            obj.optJSONArray("healthBoxes")?.let { arr ->
                for (i in 0 until minOf(arr.length(), MAX_LISTA)) {
                    arr.optJSONObject(i)?.let { healthList.add(ModelJsonCodecs.caixaVitalidadeFromJson(it)) }
                }
            }
            val armaObj = if (obj.isNull("arma")) null else obj.optJSONObject("arma")
            val arma = armaObj?.let {
                ArmaEncontro(
                    nome = it.optString("nome", ""), peso = it.optString("peso", ""), tipo = it.optString("tipo", ""),
                    precisao = it.optInt("precisao", 0), dano = it.optInt("dano", 0), defesa = it.optInt("defesa", 0),
                    motesComitados = it.optInt("motesComprometidos", 0),
                    etiquetas = (it.optJSONArray("etiquetas") ?: org.json.JSONArray()).let { arr ->
                        (0 until minOf(arr.length(), MAX_LISTA)).map { i -> arr.optString(i).take(MAX_NOME_TEXTO) }
                    }
                )
            }
            val armaduraObj = if (obj.isNull("armadura")) null else obj.optJSONObject("armadura")
            val armadura = armaduraObj?.let {
                ArmaduraEncontro(
                    nome = it.optString("nome", ""), peso = it.optString("peso", ""),
                    tipo = it.optString("tipo", "Artefato").trim().ifBlank { "Artefato" },
                    absorcao = it.optInt("absorcao", 0), dureza = it.optInt("dureza", 0),
                    penalidadeMobilidade = it.optInt("penalidadeMobilidade", 0),
                    motesComitados = it.optInt("motesComprometidos", 0),
                    marcadores = (it.optJSONArray("marcadores") ?: org.json.JSONArray()).let { arr ->
                        (0 until minOf(arr.length(), MAX_LISTA)).map { i -> arr.optString(i).take(MAX_NOME_TEXTO) }
                    },
                    custoMeritoArtefato = it.optInt("custoMeritoArtefato", 3)
                )
            }
            return NpcEncontro(
                id = obj.optString("id", UUID.randomUUID().toString()),
                nome = obj.optString("nome", ""),
                genero = obj.optString("genero", ""),
                tipoExaltado = try { TipoExaltadoEncontro.valueOf(obj.optString("tipoExaltado", "SOLAR")) } catch (e: Exception) { TipoExaltadoEncontro.SOLAR },
                arquetipo = try { ArquetipoEncontro.valueOf(obj.optString("arquetipo", "FISICO")) } catch (e: Exception) { ArquetipoEncontro.FISICO },
                casta = obj.optString("casta", ""),
                habilidadesFavorecidas = favorecidasList,
                lunarAtributosCasta = lunarAtributosCastaList,
                habilidadeSupernal = obj.optString("habilidadeSupernal", ""),
                attributes = attrMap,
                abilities = abMap,
                merits = meritsList,
                especialidades = especializacoesList,
                habilidadePrincipal = obj.optString("habilidadePrincipal", ""),
                habilidadeDefensiva = if (obj.isNull("habilidadeDefensiva")) null else obj.optString("habilidadeDefensiva").ifBlank { null },
                habilidadeSuporte = obj.optString("habilidadeSuporte", ""),
                charms = charmsList,
                estiloArtesMarciais = obj.optString("estiloArtesMarciais", ""),
                estilosArtesMarciaisAdicionais = buildList {
                    val arr = obj.optJSONArray("estilosArtesMarciaisAdicionais")
                    if (arr != null) for (i in 0 until arr.length()) {
                        arr.optString(i).takeIf(String::isNotBlank)?.let(::add)
                    }
                },
                feiticos = feiticosList,
                feiticoInicialNome = if (obj.has("feiticoInicialNome")) {
                    if (obj.isNull("feiticoInicialNome")) null else obj.optString("feiticoInicialNome").ifBlank { null }
                } else {
                    // 457/458 acrescentavam o Feitiço gratuito ao final da lista.
                    // Só inferimos essa posição enquanto a edição inicial ainda pode existir.
                    val jaRecebeuXp = obj.optBoolean("primeiroXpRecebido", false) ||
                        obj.optInt("xpAtual", 0) > 0 || obj.optInt("xpGastoTotal", 0) > 0 ||
                        (obj.optJSONArray("historicoXpBatches")?.length() ?: 0) > 0
                    if (jaRecebeuXp) null else feiticosList.lastOrNull()?.nome
                },
                corpoDeTouroCount = obj.optInt("corpoDeTouroCount", 0),
                essencia = obj.optInt("essencia", 1),
                limite = obj.optString("limite", ""),
                focoProgressaoExplicito = if (obj.isNull("focoProgressaoExplicito")) null else obj.optString("focoProgressaoExplicito").ifBlank { null },
                idioma = obj.optString("idioma", ""),
                formaEspiritual = obj.optString("formaEspiritual", ""),
                formaEspiritualSecundaria = obj.optString("formaEspiritualSecundaria", ""),
                lunarArchetypeTraits = (obj.optJSONArray("lunarArchetypeTraits") ?: JSONArray()).let { arr ->
                    (0 until minOf(arr.length(), MAX_LISTA)).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }
                },
                lunarPrimaryArchetypeTraits = (obj.optJSONArray("lunarPrimaryArchetypeTraits") ?: JSONArray()).let { arr ->
                    (0 until minOf(arr.length(), MAX_LISTA)).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }
                },
                sinal = obj.optString("sinal", ""),
                motesPersonais = obj.optInt("motesPersonais", 0),
                motesPerifericos = obj.optInt("motesPerifericos", 0),
                forcaDeVontade = obj.optInt("forcaDeVontade", 0),
                forcaDeVontadeUsados = (obj.optJSONArray("forcaDeVontadeUsados") ?: org.json.JSONArray()).let { arr ->
                    (0 until minOf(arr.length(), MAX_LISTA)).map { arr.optInt(it) }.toSet()
                },
                xpAtual = obj.optInt("xpAtual", 0),
                xpGastoTotal = obj.optInt("xpGastoTotal", 0),
                primeiroXpRecebido = if (obj.has("primeiroXpRecebido")) {
                    obj.optBoolean("primeiroXpRecebido", false)
                } else {
                    // Migração conservadora: saves antigos que já mostram qualquer
                    // evidência de progressão não recuperam edição inicial gratuita.
                    obj.optInt("xpAtual", 0) > 0 ||
                        obj.optInt("xpGastoTotal", 0) > 0 ||
                        (obj.optJSONArray("historicoXpBatches")?.length() ?: 0) > 0
                },
                historicoXpBatches = (obj.optJSONArray("historicoXpBatches") ?: org.json.JSONArray()).let { arr ->
                    (0 until minOf(arr.length(), MAX_HISTORICO_XP)).mapNotNull { i ->
                        val loteObj = arr.optJSONObject(i) ?: return@mapNotNull null
                        val nomesArr = loteObj.optJSONArray("nomesEncantosAdicionados") ?: org.json.JSONArray()
                        HistoricoXpBatch(
                            xpGasto = loteObj.optInt("xpGasto", 0),
                            nomesEncantosAdicionados = (0 until minOf(nomesArr.length(), MAX_CHARMS)).map { nomesArr.optString(it).take(MAX_NOME_TEXTO) },
                            habilidadeMelhorada = if (loteObj.isNull("habilidadeMelhorada")) null else loteObj.optString("habilidadeMelhorada"),
                            pontosGanhosNaHabilidade = loteObj.optInt("pontosGanhosNaHabilidade", 0),
                            especializacaoAdicionada = if (loteObj.isNull("especializacaoAdicionada")) null else loteObj.optString("especializacaoAdicionada"),
                            pontosForcaDeVontadeComprados = loteObj.optInt("pontosForcaDeVontadeComprados", 0)
                        )
                    }
                },
                arma = arma,
                armadura = armadura,
                acaoPrincipal = obj.optInt("acaoPrincipal", 0),
                acaoDecisiva = obj.optInt("acaoDecisiva", 0),
                defesaPrimaria = if (obj.isNull("defesaPrimaria")) null else obj.optInt("defesaPrimaria"),
                esquiva = obj.optInt("esquiva", 0),
                absorcaoNatural = obj.optInt("absorcaoNatural", 0),
                absorcaoArmadura = obj.optInt("absorcaoArmadura", 0),
                absorcao = obj.optInt("absorcao", 0),
                dureza = obj.optInt("dureza", 0),
                perseveranca = obj.optInt("perseveranca", 0),
                astucia = obj.optInt("astucia", 0),
                juntarABatalha = obj.optInt("juntarABatalha", 0),
                investida = obj.optInt("investida", 0),
                desengajamento = obj.optInt("desengajamento", 0),
                penalidadeClashDefesa = obj.optInt("penalidadeClashDefesa", 0).coerceAtLeast(0),
                penalidadeAtaquesDefesa = obj.optInt("penalidadeAtaquesDefesa", 0).coerceAtLeast(0),
                healthBoxes = if (healthList.isNotEmpty()) healthList else ExaltedConstants.defaultHealthBoxes(),
                iniciativaAtual = obj.optInt("iniciativaAtual", 0),
                dano = obj.optString("dano", ""),
                alertasValidacao = alertas
            )
        } catch (e: NpcSaveCorruptedException) {
            throw e
        } catch (e: Exception) {
            throw NpcSaveCorruptedException("NPC salvo corrompido ou em formato inválido: ${e.message ?: e::class.simpleName}", e)
        }
    }
}
