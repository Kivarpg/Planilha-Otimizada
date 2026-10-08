package com.example.data

import com.example.model.Npc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Cobre o pipeline completo de NpcShareCodec (exportar/importar a lista
// de NPCs por código de texto) — mesmo padrão de ShareCodeCodecTest.kt.
// Além dos casos espelhados de lá (ida e volta com caracteres especiais,
// códigos inválidos), cobre também a independência entre os dois
// codecs: um código de planilha não deve ser aceito pelo codec de NPCs, e
// vice-versa.
//
// RobolectricTestRunner é necessário (ao contrário do que o comentário
// original desta classe dizia): NpcShareCodec usa org.json.JSONObject
// internamente, que em JVM pura vem stubado pelo Android e lança
// "Method X not mocked" em qualquer chamada real — confirmado por uma
// execução real de ./gradlew test, que falhava em 100% dos testes
// desta classe antes desta correção.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NpcShareCodecTest {

    private fun exportarOuFalhar(npcs: List<Npc>): String =
        NpcShareCodec.exportar(npcs).getOrElse { throw AssertionError("Exportação deveria ter sucesso: ${it.message}") }

    private fun importarOuFalhar(codigo: String): List<Npc> {
        val resultado = NpcShareCodec.importar(codigo)
        return when (resultado) {
            is NpcShareCodec.ResultadoImportacao.Sucesso -> resultado.npcs
            is NpcShareCodec.ResultadoImportacao.Erro -> throw AssertionError("Importação deveria ter sucesso: ${resultado.mensagem}")
        }
    }

    private fun mensagemDeErro(codigo: String): String {
        val resultado = NpcShareCodec.importar(codigo)
        return when (resultado) {
            is NpcShareCodec.ResultadoImportacao.Erro -> resultado.mensagem
            is NpcShareCodec.ResultadoImportacao.Sucesso -> throw AssertionError("Importação deveria ter falhado, mas teve sucesso")
        }
    }

    // --- Ida e volta preservando conteúdo ---

    @Test
    fun `lista com um npc sobrevive a ida e volta`() {
        val original = listOf(Npc(nome = "Capitão Vento-do-Norte", lealdade = "Aliado", tipo = "Mortal veterano", descricao = "Comanda a guarda do porto."))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(1, importada.size)
        assertEquals("Capitão Vento-do-Norte", importada.first().nome)
        assertEquals("Aliado", importada.first().lealdade)
        assertEquals("Mortal veterano", importada.first().tipo)
        assertEquals("Comanda a guarda do porto.", importada.first().descricao)
    }

    @Test
    fun `lista com varios npcs sobrevive a ida e volta`() {
        val original = listOf(
            Npc(nome = "Alfa", lealdade = "Aliado"),
            Npc(nome = "Beta", lealdade = "Inimigo"),
            Npc(nome = "Gama", lealdade = "Neutro")
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(3, importada.size)
        assertEquals(setOf("Alfa", "Beta", "Gama"), importada.map { it.nome }.toSet())
    }

    @Test
    fun `as 3 lealdades sobrevivem a ida e volta corretamente`() {
        val original = listOf(
            Npc(nome = "A", lealdade = "Aliado"),
            Npc(nome = "B", lealdade = "Inimigo"),
            Npc(nome = "C", lealdade = "Neutro")
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals("Aliado", importada.first { it.nome == "A" }.lealdade)
        assertEquals("Inimigo", importada.first { it.nome == "B" }.lealdade)
        assertEquals("Neutro", importada.first { it.nome == "C" }.lealdade)
    }

    @Test
    fun `descricoes com acentos sobrevivem a ida e volta`() {
        val original = listOf(Npc(nome = "Órfã do Sol", descricao = "Nação em ruínas, coração partido"))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals("Nação em ruínas, coração partido", importada.first().descricao)
    }

    @Test
    fun `emojis sobrevivem a ida e volta`() {
        val original = listOf(Npc(nome = "Guerreiro", descricao = "Solar ☀️ com espada 🗡️"))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals("Solar ☀️ com espada 🗡️", importada.first().descricao)
    }

    @Test
    fun `quebras de linha sobrevivem a ida e volta`() {
        val original = listOf(Npc(nome = "Teste", descricao = "Linha um\nLinha dois\n\nLinha quatro"))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals("Linha um\nLinha dois\n\nLinha quatro", importada.first().descricao)
    }

    @Test
    fun `aspas e caracteres especiais sobrevivem a ida e volta`() {
        val original = listOf(Npc(nome = "Teste", descricao = "Ele disse \"não\" — e usou < > & / \\ | * ? : também"))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals("Ele disse \"não\" — e usou < > & / \\ | * ? : também", importada.first().descricao)
    }

    @Test
    fun `texto longo sobrevive a ida e volta`() {
        val textoLongo = "Lorem ipsum dolor sit amet. ".repeat(2000)
        val original = listOf(Npc(nome = "Teste", descricao = textoLongo))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(textoLongo, importada.first().descricao)
    }

    // --- Regras de exportação ---

    @Test
    fun `exportar lista vazia falha`() {
        val resultado = NpcShareCodec.exportar(emptyList())
        assertTrue(resultado.isFailure)
    }

    // --- Identificadores não reaproveitados ---

    @Test
    fun `importar gera novos ids, nao reaproveita os da origem`() {
        val original = listOf(Npc(id = "id-fixo-1", nome = "Teste"))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertNotEquals("id-fixo-1", importada.first().id)
    }

    // --- Códigos inválidos ---

    @Test
    fun `codigo vazio retorna mensagem para inserir um codigo`() {
        assertEquals("Insira um código.", mensagemDeErro(""))
    }

    @Test
    fun `codigo so com espacos e tratado como vazio`() {
        assertEquals("Insira um código.", mensagemDeErro("   \n  "))
    }

    @Test
    fun `codigo com prefixo errado eh rejeitado`() {
        assertEquals("O código não pertence a este aplicativo.", mensagemDeErro("OUTRACOISA-V1.abc.def"))
    }

    // --- Independência entre os dois codecs (o requisito central do pedido) ---

    @Test
    fun `codigo de planilha (APPPLAN) eh rejeitado pelo codec de NPCs`() {
        val codigoDePlanilha = ShareCodeCodec.exportar(com.example.model.CharacterSheet(), emptyList(), emptyList()).getOrElse {
            throw AssertionError("Setup do teste falhou: ${it.message}")
        }
        assertEquals("O código não pertence a este aplicativo.", mensagemDeErro(codigoDePlanilha))
    }

    @Test
    fun `codigo de NPCs (APPNPCS) eh rejeitado pelo codec de planilha`() {
        val codigoDeNpcs = exportarOuFalhar(listOf(Npc(nome = "Teste")))
        val resultado = ShareCodeCodec.importar(codigoDeNpcs, emptyList(), emptyList())
        val mensagem = when (resultado) {
            is ShareCodeCodec.ResultadoImportacao.Erro -> resultado.mensagem
            is ShareCodeCodec.ResultadoImportacao.Sucesso -> throw AssertionError("Deveria ter sido rejeitado pelo codec de planilha")
        }
        assertEquals("O código não pertence a este aplicativo.", mensagem)
    }

    @Test
    fun `codigo com versao incompativel eh rejeitado`() {
        val codigoValido = exportarOuFalhar(listOf(Npc(nome = "Teste")))
        // Precisa bater com a VERSAO_ATUAL real do NpcShareCodec (privada,
        // não acessível daqui) — se a versão mudar no futuro, atualizar
        // este replaceFirst junto (mesma lição aprendida em
        // ShareCodeCodecTest quando a versão de planilha mudou de V1 pra V2).
        val comVersaoErrada = codigoValido.replaceFirst("-V1.", "-V99.")
        assertEquals("A versão deste código não é compatível.", mensagemDeErro(comVersaoErrada))
    }

    @Test
    fun `codigo truncado (sem payload) eh rejeitado como incompleto`() {
        assertEquals("O código está incompleto ou corrompido.", mensagemDeErro("APPNPCS-V1.semSegundoPonto"))
    }

    @Test
    fun `codigo com checksum corrompida eh rejeitado`() {
        val codigoValido = exportarOuFalhar(listOf(Npc(nome = "Teste")))
        val partes = codigoValido.split(".", limit = 3)
        val payloadCorrompido = partes[2].reversed()
        val codigoCorrompido = "${partes[0]}.${partes[1]}.$payloadCorrompido"
        assertEquals("O código está incompleto ou corrompido.", mensagemDeErro(codigoCorrompido))
    }

    @Test
    fun `codigo com espacos acidentais no inicio e fim eh aceito normalmente`() {
        val codigoValido = exportarOuFalhar(listOf(Npc(nome = "Com Espaco")))
        val comEspacos = "  \n$codigoValido\n  "
        val importada = importarOuFalhar(comEspacos)
        assertEquals("Com Espaco", importada.first().nome)
    }

    @Test
    fun `codigo acima do limite maximo eh rejeitado explicitamente`() {
        val codigoEnorme = "APPNPCS-V1.a.${"x".repeat(NpcShareCodec.TAMANHO_MAXIMO_CODIGO + 10)}"
        assertEquals("O código excede o tamanho permitido.", mensagemDeErro(codigoEnorme))
    }
}
