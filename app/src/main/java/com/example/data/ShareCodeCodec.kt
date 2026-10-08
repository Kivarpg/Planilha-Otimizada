package com.example.data

import com.example.model.CharacterSheet
import com.example.model.Casta
import java.util.UUID

// Sistema de código de compartilhamento de PLANILHA — escopo RESTRITO por
// decisão explícita do usuário: o código (e o QR gerado a partir dele)
// representa só a "build" mecânica do personagem, não a planilha completa.
//
// Exporta exclusivamente:
//   - Atributos (valores + qual conjunto é primário/secundário/terciário)
//   - Casta + quais habilidades são de casta/favorecida/suprema
//   - Valores de todas as habilidades
//   - Méritos adquiridos
//   - Encantos adquiridos (Solares, Feitiços e golpes de Arte Marcial —
//     unificados nesse campo, já que são armazenados da mesma forma
//     internamente)
//
// O gasto de Pontos de Bônus NÃO é um campo próprio: é recalculado
// automaticamente pelo app a partir dos campos acima, exatamente como já
// acontece hoje pra qualquer planilha (o app nunca confia num total
// embutido, sempre recalcula pelas regras vigentes).
//
// Nome, Jogador, Conceito, Casta (como identidade), equipamentos,
// Trilha de Vitalidade, motes, Força de Vontade, sessões, histórico de
// XP, Intimidades, Idiomas e NPCs ficam de fora — ao importar, a planilha
// resultante é uma planilha NOVA com esses campos em branco/padrão, não uma
// mescla com a planilha que estava aberta.
//
// O pipeline de compressão/checksum/codificação em si (DEFLATE bruto +
// dicionário + CRC32 + Base64 URL-safe + envelope com versão) vive em
// GenericShareCodec, compartilhado com NpcShareCodec — este arquivo só
// cuida do que é específico da build de planilha.
object ShareCodeCodec {
    private const val PREFIXO = "APPPLAN"
    // V3: formato restrito à build mecânica (atributos, casta, habilidades,
    // méritos, encantos) — estruturalmente diferente e incompatível com
    // V1/V2 (planilha completa). Códigos V1/V2 são rejeitados com mensagem
    // clara ("versão não compatível"), nunca interpretados incorretamente.
    private const val VERSAO_ATUAL = 3
    const val TAMANHO_MAXIMO_CODIGO = 100_000

    // Vocabulário do formato restrito — só os campos que de fato aparecem
    // no payload desta versão, mais as 25 habilidades e 9 atributos (usados
    // repetidamente como chave/valor em vários pontos da estrutura).
    private val DICIONARIO_COMPRESSAO: ByteArray = run {
        val campos = listOf(
            "abilities", "attributePriorities", "attributes", "tipoPersonagem", "aspecto", "lunarCasta", "lunarCastaEscolhida", "lunarCasteAttributesEscolhidos", "lunarFormaEspiritual", "lunarSinal", "favoredAttributes", "casta", "casteAbilities",
            "charms", "favoredAbilities", "merits", "specializations", "supernalAbility",
            "categoria", "custo", "descricao", "habilidade", "habilidadeVinculada",
            "mins", "minEssencia", "minHabilidade", "nomeIngles", "palavrasChave",
            "duracao", "preRequisitos", "quadros", "tipo", "valor", "nome", "pinOrder", "id"
        )
        val habilidades = listOf(
            "Armas Brancas", "Arqueirismo", "Arremesso", "Atletismo", "Briga", "Burocracia",
            "Cavalgar", "Conhecimento", "Crime", "Esquiva", "Furtividade", "Guerra", "Integridade",
            "Investigação", "Linguística", "Medicina", "Navegação", "Ocultismo", "Ofícios", "Performance",
            "Presença", "Prontidão", "Resistência", "Sobrevivência", "Socialização"
        )
        val atributos = listOf(
            "Força", "Destreza", "Vigor", "Carisma", "Manipulação", "Aparência",
            "Percepção", "Inteligência", "Raciocínio"
        )
        val texto = campos.joinToString(",") { "\"$it\":" } + (habilidades + atributos).joinToString(",") { "\"$it\"" }
        texto.toByteArray(Charsets.UTF_8)
    }

    sealed class ResultadoImportacao {
        data class Sucesso(val sheet: CharacterSheet) : ResultadoImportacao()
        data class Erro(val mensagem: String) : ResultadoImportacao()
    }

    // Retorna o código pronto, ou uma falha com mensagem amigável — nunca
    // uma exceção crua — se a planilha estiver em Modo Livre (não exportável
    // por regra do produto) ou se o código final ultrapassar o limite.
    //
    // encantosSolares/feiticos: catálogos embutidos no app, usados só pra
    // COMPACTAR os encantos adquiridos (referência de catálogo + ID, em
    // vez de duplicar nome/custo/descrição inteiros — ver
    // PoderesReferenciaCodec). Nenhum encanto some se não bater com o
    // catálogo: nesse caso ele é serializado por extenso.
    fun exportar(sheet: CharacterSheet, encantosSolares: List<EncantoSolarDefinition>, feiticos: List<FeiticoDefinition>): Result<String> {
        if (sheet.modoLivre) {
            return Result.failure(IllegalStateException("Planilhas em Modo Livre não podem ser exportadas por código."))
        }
        val json = org.json.JSONObject()
        json.put("tipoPersonagem", sheet.tipoPersonagem)
        json.put("aspecto", sheet.aspecto)
        json.put("lunarCasta", sheet.lunarCasta.name)
        json.put("lunarCastaEscolhida", sheet.lunarCastaEscolhida)
        json.put("lunarCasteAttributesEscolhidos", org.json.JSONArray(sheet.lunarCasteAttributesEscolhidos))
        json.put("lunarFormaEspiritual", sheet.lunarFormaEspiritual)
        json.put("lunarSinal", sheet.lunarSinal)
        json.put("favoredAttributes", org.json.JSONArray(sheet.favoredAttributes))
        json.put("casta", sheet.casta.name)
        json.put("casteAbilities", org.json.JSONArray(sheet.casteAbilities))
        json.put("favoredAbilities", org.json.JSONArray(sheet.favoredAbilities))
        json.put("supernalAbility", sheet.supernalAbility ?: org.json.JSONObject.NULL)
        json.put("attributes", org.json.JSONObject(sheet.attributes))
        json.put("attributePriorities", org.json.JSONObject(sheet.attributePriorities))
        json.put("abilities", org.json.JSONObject(sheet.abilities))
        json.put("specializations", org.json.JSONArray(sheet.specializations.map { ModelJsonCodecs.especializacaoToMap(it) }))
        json.put("merits", org.json.JSONArray(sheet.merits.map { ModelJsonCodecs.meritoToMap(it) }))
        val charmsCompactos = PoderesReferenciaCodec.codificar(sheet.charms, encantosSolares, feiticos)
        json.put("charms", org.json.JSONArray(charmsCompactos))
        return GenericShareCodec.codificar(PREFIXO, VERSAO_ATUAL, json.toString(), DICIONARIO_COMPRESSAO, TAMANHO_MAXIMO_CODIGO)
    }

    fun importar(codigoBruto: String, encantosSolares: List<EncantoSolarDefinition>, feiticos: List<FeiticoDefinition>): ResultadoImportacao {
        val resultado = GenericShareCodec.decodificar(codigoBruto, PREFIXO, VERSAO_ATUAL, DICIONARIO_COMPRESSAO, TAMANHO_MAXIMO_CODIGO)
        val json = when (resultado) {
            is GenericShareCodec.Resultado.Erro -> return ResultadoImportacao.Erro(resultado.mensagem)
            is GenericShareCodec.Resultado.Sucesso -> resultado.payload
        }

        val sheetImportada = try {
            val obj = org.json.JSONObject(json)

            fun validarTamanho(chave: String, limite: Int) {
                val tamanho = obj.optJSONArray(chave)?.length() ?: 0
                require(tamanho <= limite) { "O campo '$chave' excede o limite permitido." }
            }

            // O código é externo ao processo atual do app; rejeitamos estruturas
            // acima dos limites legítimos em vez de materializar listas arbitrárias.
            validarTamanho("casteAbilities", 5)
            validarTamanho("favoredAbilities", 5)
            validarTamanho("specializations", 100)
            validarTamanho("merits", 100)
            validarTamanho("charms", 512)

            fun mapaDeInt(chave: String): Map<String, Int> {
                val o = obj.optJSONObject(chave) ?: org.json.JSONObject()
                val resultado = mutableMapOf<String, Int>()
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    resultado[k] = o.optInt(k, 0)
                }
                return resultado
            }
            fun mapaDeString(chave: String): Map<String, String> {
                val o = obj.optJSONObject(chave) ?: org.json.JSONObject()
                val resultado = mutableMapOf<String, String>()
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    resultado[k] = o.optString(k, "")
                }
                return resultado
            }
            fun listaDeString(chave: String): List<String> {
                val arr = obj.optJSONArray(chave) ?: org.json.JSONArray()
                return (0 until arr.length()).map { arr.optString(it, "") }
            }

            val tipoPersonagem = obj.optString("tipoPersonagem", com.example.model.CharacterType.SOLAR)
            require(com.example.model.CharacterType.isSupported(tipoPersonagem)) { "Tipo de personagem inválido." }
            fun listaLimitada(chave: String, limite: Int): List<String> = listaDeString(chave).also { require(it.size <= limite) }
            val charmsArr = obj.optJSONArray("charms") ?: org.json.JSONArray()
            val charmsItens = (0 until charmsArr.length()).map { charmsArr.getJSONObject(it) }
            val charmsExpandidos = PoderesReferenciaCodec.decodificar(charmsItens, encantosSolares, feiticos)

            val meritsArr = obj.optJSONArray("merits") ?: org.json.JSONArray()
            val merits = (0 until meritsArr.length()).map { ModelJsonCodecs.meritoFromJson(meritsArr.getJSONObject(it)) }

            val specsArr = obj.optJSONArray("specializations") ?: org.json.JSONArray()
            val specs = (0 until specsArr.length()).map { ModelJsonCodecs.especializacaoFromJson(specsArr.getJSONObject(it)) }

            CharacterSheet(
                casta = Casta.valueOf(obj.optString("casta", Casta.Dawn.name)),
                tipoPersonagem = tipoPersonagem,
                aspecto = obj.optString("aspecto", ""),
                lunarCasta = try { com.example.model.LunarCasta.valueOf(obj.optString("lunarCasta", com.example.model.LunarCasta.FullMoon.name)) } catch (_: Exception) { com.example.model.LunarCasta.FullMoon },
                lunarCastaEscolhida = obj.optBoolean("lunarCastaEscolhida", false),
                lunarCasteAttributesEscolhidos = listaLimitada("lunarCasteAttributesEscolhidos", 2),
                lunarFormaEspiritual = obj.optString("lunarFormaEspiritual", ""),
                lunarSinal = obj.optString("lunarSinal", ""),
                favoredAttributes = listaLimitada("favoredAttributes", 4),
                casteAbilities = listaDeString("casteAbilities"),
                favoredAbilities = listaDeString("favoredAbilities"),
                supernalAbility = if (obj.isNull("supernalAbility")) null else obj.optString("supernalAbility", ""),
                attributes = mapaDeInt("attributes"),
                attributePriorities = mapaDeString("attributePriorities"),
                abilities = mapaDeInt("abilities"),
                specializations = specs,
                merits = merits,
                charms = charmsExpandidos
            )
        } catch (e: Exception) {
            return ResultadoImportacao.Erro("A planilha contém dados inválidos.")
        }

        // Sempre uma planilha nova local — nunca reaproveita id/numeração do
        // dispositivo de origem, e nunca mescla com a planilha que estava
        // aberta antes de importar.
        val sheetPronta = sheetImportada.copy(
            id = UUID.randomUUID().toString(),
            numeroSequencial = 0,
            dataSalvamento = ""
        )
        return ResultadoImportacao.Sucesso(sheetPronta)
    }
}
