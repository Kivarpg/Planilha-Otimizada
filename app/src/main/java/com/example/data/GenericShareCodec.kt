package com.example.data

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

// Núcleo genérico do pipeline de código de compartilhamento: compressão
// DEFLATE bruto com dicionário pré-carregado, checksum CRC32, Base64
// URL-safe, envelope com prefixo+versão. Extraído de ShareCodeCodec (o
// código de planilha, criado primeiro) pra ser reaproveitado também por
// NpcShareCodec (código de NPCs) — o algoritmo é idêntico entre os dois,
// só prefixo/payload/dicionário mudam por chamador. Evita manter dois
// pipelines de compressão idênticos em arquivos separados.
object GenericShareCodec {

    sealed class Resultado {
        data class Sucesso(val payload: String) : Resultado()
        data class Erro(val mensagem: String) : Resultado()
    }

    // Retorna o código pronto, ou uma falha com mensagem amigável — nunca
    // uma exceção crua — se o código final ultrapassar o limite.
    fun codificar(prefixo: String, versao: Int, payload: String, dicionario: ByteArray, tamanhoMaximo: Int): Result<String> {
        return try {
            val comprimido = comprimir(payload.toByteArray(Charsets.UTF_8), dicionario)
            val checksum = calcularChecksum(comprimido)
            val codificadoB64 = base64UrlSafeCodificar(comprimido)
            val codigo = "$prefixo-V$versao.$checksum.$codificadoB64"
            if (codigo.length > tamanhoMaximo) {
                Result.failure(
                    IllegalStateException(
                        "O código ficou grande demais (${codigo.length} caracteres, limite de $tamanhoMaximo)."
                    )
                )
            } else {
                Result.success(codigo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Nunca lança exceção — todo caminho de erro vira uma mensagem
    // específica e compreensível. Retorna o payload em texto já
    // descomprimido em caso de sucesso — quem chama decide como
    // interpretar esse texto (JSON de planilha, JSON de lista de NPCs, etc.).
    fun decodificar(codigoBruto: String, prefixo: String, versaoEsperada: Int, dicionario: ByteArray, tamanhoMaximo: Int): Resultado {
        val codigo = codigoBruto.trim()
        if (codigo.isEmpty()) {
            return Resultado.Erro("Insira um código.")
        }
        if (codigo.length > tamanhoMaximo) {
            return Resultado.Erro("O código excede o tamanho permitido.")
        }
        if (!codigo.startsWith("$prefixo-V")) {
            return Resultado.Erro("O código não pertence a este aplicativo.")
        }

        val semPrefixo = codigo.removePrefix("$prefixo-V")
        val primeiroPonto = semPrefixo.indexOf('.')
        if (primeiroPonto == -1) {
            return Resultado.Erro("O código está incompleto ou corrompido.")
        }
        val versao = semPrefixo.substring(0, primeiroPonto).toIntOrNull()
        if (versao == null || versao != versaoEsperada) {
            return Resultado.Erro("A versão deste código não é compatível.")
        }

        val resto = semPrefixo.substring(primeiroPonto + 1)
        val segundoPonto = resto.indexOf('.')
        if (segundoPonto == -1) {
            return Resultado.Erro("O código está incompleto ou corrompido.")
        }
        val checksumRecebido = resto.substring(0, segundoPonto)
        val parteCodificada = resto.substring(segundoPonto + 1)
        if (parteCodificada.isEmpty()) {
            return Resultado.Erro("O código está incompleto ou corrompido.")
        }

        val comprimido = try {
            base64UrlSafeDecodificar(parteCodificada)
        } catch (e: Exception) {
            return Resultado.Erro("O conteúdo do código não pôde ser lido.")
        }

        val checksumCalculado = calcularChecksum(comprimido)
        if (checksumCalculado != checksumRecebido) {
            return Resultado.Erro("O código está incompleto ou corrompido.")
        }

        val payload = try {
            String(descomprimir(comprimido, dicionario), Charsets.UTF_8)
        } catch (e: LimiteDescompressaoExcedidoException) {
            return Resultado.Erro("O conteúdo do código é maior do que o permitido.")
        } catch (e: Exception) {
            return Resultado.Erro("O conteúdo do código não pôde ser lido.")
        }

        return Resultado.Sucesso(payload)
    }

    // Lançada internamente por descomprimir() ao ultrapassar o teto —
    // nunca escapa desta classe, sempre convertida numa mensagem amigável
    // em decodificar().
    private class LimiteDescompressaoExcedidoException : Exception()

    // DEFLATE permite fatores de expansão de 1000x ou mais em dados
    // repetitivos (ex.: um bloco grande de bytes iguais) — um código
    // malicioso de ~68 mil caracteres, bem abaixo do TAMANHO_MAXIMO_CODIGO
    // de qualquer chamador, já basta pra descomprimir mais de 50 MB
    // (validado por simulação antes de implementar esta defesa). Sem um
    // teto aqui, importar um código/QR de origem não confiável poderia
    // esgotar a memória do aparelho de quem importa. 8 MB é bem acima de
    // qualquer planilha ou lista de NPCs legítima (a maior já vista nos
    // testes deste projeto fica na casa de poucos KB).
    private const val LIMITE_DESCOMPRESSAO_BYTES = 8 * 1024 * 1024

    // DEFLATE bruto (nowrap=true: sem cabeçalho/rodapé zlib) com um
    // dicionário pré-carregado fornecido por quem chama.
    private fun comprimir(dados: ByteArray, dicionario: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setDictionary(dicionario)
        deflater.setInput(dados)
        deflater.finish()
        val buffer = ByteArray(4096)
        val saida = ByteArrayOutputStream(dados.size)
        while (!deflater.finished()) {
            val n = deflater.deflate(buffer)
            saida.write(buffer, 0, n)
        }
        deflater.end()
        return saida.toByteArray()
    }

    // Em modo bruto (nowrap=true) não existe o sinalizador automático
    // "precisa de dicionário" que existe no formato zlib com cabeçalho —
    // esse sinal vive dentro do próprio cabeçalho, que aqui não existe.
    // Por isso o dicionário é aplicado de forma PROATIVA, antes de
    // qualquer chamada a inflate(), em vez de esperar
    // Inflater.needsDictionary() retornar true (validado por simulação em
    // Python antes de implementar pela primeira vez — esse sinal nunca
    // dispara em modo bruto).
    private fun descomprimir(dados: ByteArray, dicionario: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setDictionary(dicionario)
        inflater.setInput(dados)
        val buffer = ByteArray(4096)
        val saida = ByteArrayOutputStream()
        var totalDescomprimido = 0
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0) {
                // Evita loop infinito em streams DEFLATE malformados que não
                // pedem mais entrada, mas também não chegam a finished().
                if (inflater.needsInput() || inflater.needsDictionary()) break
                inflater.end()
                throw IllegalArgumentException("Fluxo comprimido inválido.")
            }
            totalDescomprimido += n
            if (totalDescomprimido > LIMITE_DESCOMPRESSAO_BYTES) {
                inflater.end()
                throw LimiteDescompressaoExcedidoException()
            }
            saida.write(buffer, 0, n)
        }
        inflater.end()
        return saida.toByteArray()
    }

    // CRC32 — suficiente para detectar corrupção/truncamento acidental
    // (o objetivo declarado). Não é mecanismo de segurança nem
    // criptografia; Base64 e compressão também não são.
    private fun calcularChecksum(dados: ByteArray): String {
        val crc = CRC32()
        crc.update(dados)
        return crc.value.toString(16)
    }

    // Base64 URL-safe (RFC 4648 §5, sem padding) implementado à mão em vez
    // de usar java.util.Base64 (só existe a partir da API 26 — travava o
    // app inteiro com NoClassDefFoundError em qualquer Android 7.x/7.1,
    // já que o minSdk deste projeto é 24) ou android.util.Base64 (existe
    // desde a API 8, mas é stub-only fora de um runtime Android real —
    // quebraria ShareCodeCodecTest/NpcShareCodecTest, que rodam
    // deliberadamente como JVM pura, sem Robolectric, pra ficarem
    // rápidos). Essa implementação roda igual em qualquer JVM e em
    // qualquer versão do Android suportada pelo app. Validada por
    // simulação em Python contra a biblioteca padrão antes de escrever
    // esta versão, em várias combinações de tamanho (incluindo os 3
    // casos de resto de divisão por 3: 0, 1 e 2 bytes sobrando).
    private const val BASE64_URL_SAFE_ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val BASE64_URL_SAFE_INDICE = IntArray(128) { -1 }.also { indice ->
        BASE64_URL_SAFE_ALFABETO.forEachIndexed { i, c -> indice[c.code] = i }
    }

    private fun base64UrlSafeCodificar(dados: ByteArray): String {
        val sb = StringBuilder((dados.size + 2) / 3 * 4)
        var i = 0
        while (i < dados.size) {
            val b0 = dados[i].toInt() and 0xFF
            val b1 = if (i + 1 < dados.size) dados[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < dados.size) dados[i + 2].toInt() and 0xFF else 0
            val n = (b0 shl 16) or (b1 shl 8) or b2
            sb.append(BASE64_URL_SAFE_ALFABETO[(n shr 18) and 0x3F])
            sb.append(BASE64_URL_SAFE_ALFABETO[(n shr 12) and 0x3F])
            if (i + 1 < dados.size) sb.append(BASE64_URL_SAFE_ALFABETO[(n shr 6) and 0x3F])
            if (i + 2 < dados.size) sb.append(BASE64_URL_SAFE_ALFABETO[n and 0x3F])
            i += 3
        }
        return sb.toString()
    }

    private fun base64UrlSafeDecodificar(texto: String): ByteArray {
        var buffer = 0
        var bits = 0
        val saida = ByteArrayOutputStream((texto.length * 3) / 4 + 1)
        for (c in texto) {
            if (c == '=') continue
            val valor = if (c.code < 128) BASE64_URL_SAFE_INDICE[c.code] else -1
            if (valor < 0) throw IllegalArgumentException("Caractere Base64 inválido: $c")
            buffer = (buffer shl 6) or valor
            bits += 6
            if (bits >= 8) {
                bits -= 8
                saida.write((buffer shr bits) and 0xFF)
            }
        }
        return saida.toByteArray()
    }
}
