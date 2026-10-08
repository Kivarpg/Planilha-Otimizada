package com.example.data

import com.example.model.CharacterSheet
import com.example.model.Casta
import com.example.model.Encanto
import com.example.model.Especializacao
import com.example.model.Merito
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Cobre o pipeline de ShareCodeCodec (exportar/importar por código de
// texto) no escopo RESTRITO em vigor (V3): só a build mecânica —
// Atributos (+ prioridade), Casta (+ habilidades de casta/favorecida/
// suprema), Habilidades, Especializações, Méritos e Encantos. Nome,
// Jogador, Conceito, equipamentos, Trilha de Vitalidade, motes, Força de
// Vontade, sessões, histórico de XP, Intimidades e Língua ficam de fora
// por decisão explícita — os testes abaixo confirmam tanto o que
// sobrevive quanto o que fica em branco.
//
// RobolectricTestRunner é necessário aqui (não é um teste puramente
// lógico): ShareCodeCodec usa org.json.JSONObject internamente, que em
// JVM pura (sem Robolectric) vem stubado pelo Android e lança
// "Method X not mocked" em qualquer chamada real.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareCodeCodecTest {

    private fun exportarOuFalhar(sheet: CharacterSheet): String =
        ShareCodeCodec.exportar(sheet, emptyList(), emptyList()).getOrElse { throw AssertionError("Exportação deveria ter sucesso: ${it.message}") }

    private fun importarOuFalhar(codigo: String): CharacterSheet {
        val resultado = ShareCodeCodec.importar(codigo, emptyList(), emptyList())
        return when (resultado) {
            is ShareCodeCodec.ResultadoImportacao.Sucesso -> resultado.sheet
            is ShareCodeCodec.ResultadoImportacao.Erro -> throw AssertionError("Importação deveria ter sucesso: ${resultado.mensagem}")
        }
    }

    private fun mensagemDeErro(codigo: String): String {
        val resultado = ShareCodeCodec.importar(codigo, emptyList(), emptyList())
        return when (resultado) {
            is ShareCodeCodec.ResultadoImportacao.Erro -> resultado.mensagem
            is ShareCodeCodec.ResultadoImportacao.Sucesso -> throw AssertionError("Importação deveria ter falhado, mas teve sucesso")
        }
    }

    // --- Campos incluídos no escopo restrito sobrevivem a ida e volta ---

    @Test
    fun `planilha vazia sobrevive a ida e volta`() {
        val original = CharacterSheet()
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(original.casta, importada.casta)
        assertEquals(original.attributes, importada.attributes)
    }

    @Test
    fun `casta e habilidades especiais sobrevivem a ida e volta`() {
        val original = CharacterSheet(
            casta = Casta.Zenith,
            casteAbilities = listOf("Presença", "Integridade", "Resistência"),
            favoredAbilities = listOf("Briga", "Atletismo"),
            supernalAbility = "Presença"
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(Casta.Zenith, importada.casta)
        assertEquals(listOf("Presença", "Integridade", "Resistência"), importada.casteAbilities)
        assertEquals(listOf("Briga", "Atletismo"), importada.favoredAbilities)
        assertEquals("Presença", importada.supernalAbility)
    }

    @Test
    fun `supernalAbility nula sobrevive como nula`() {
        val original = CharacterSheet(supernalAbility = null)
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertNull(importada.supernalAbility)
    }

    @Test
    fun `atributos e prioridades sobrevivem a ida e volta`() {
        val original = CharacterSheet(
            attributes = mapOf("Força" to 5, "Destreza" to 4, "Vigor" to 3),
            attributePriorities = mapOf("Físicos" to "1º", "Sociais" to "3º", "Mentais" to "2º")
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(5, importada.attributes["Força"])
        assertEquals(4, importada.attributes["Destreza"])
        assertEquals("1º", importada.attributePriorities["Físicos"])
        assertEquals("2º", importada.attributePriorities["Mentais"])
    }

    @Test
    fun `habilidades e especializacoes sobrevivem a ida e volta`() {
        val original = CharacterSheet(
            abilities = mapOf("Armas Brancas" to 5, "Presença" to 3),
            specializations = listOf(Especializacao(nome = "Sabres", habilidade = "Armas Brancas", valor = 2))
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(5, importada.abilities["Armas Brancas"])
        assertEquals(1, importada.specializations.size)
        assertEquals("Sabres", importada.specializations.first().nome)
    }

    @Test
    fun `meritos sobrevivem a ida e volta`() {
        val original = CharacterSheet(merits = listOf(Merito(nome = "Recursos", valor = 3, categoria = "normal")))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(1, importada.merits.size)
        assertEquals("Recursos", importada.merits.first().nome)
        assertEquals(3, importada.merits.first().valor)
    }

    @Test
    fun `estado de pin de encanto sobrevive a ida e volta`() {
        val charm = Encanto(nome = "Punho da Fúria Solar", habilidadeVinculada = "Briga", pinOrder = 2)
        val original = CharacterSheet(charms = listOf(charm))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(2, importada.charms.first().pinOrder)
    }

    @Test
    fun `nome de merito com acentos emoji e caracteres especiais sobrevive`() {
        // O texto livre da planilha (conceito) fica fora do escopo restrito
        // — mas o pipeline de compressão/acentuação/emoji ainda precisa
        // funcionar corretamente para os campos que SAO exportados, como
        // o nome de um Mérito customizado.
        val original = CharacterSheet(merits = listOf(Merito(nome = "Nação em Ruínas 🗡️ \"Guerreiro\" — Sol", valor = 1)))
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals("Nação em Ruínas 🗡️ \"Guerreiro\" — Sol", importada.merits.first().nome)
    }

    // --- Campos FORA do escopo restrito ficam em branco/padrão após importar ---

    @Test
    fun `nome jogador e conceito NAO sobrevivem, ficam em branco`() {
        val original = CharacterSheet(nome = "Batata", jogador = "Fulano", conceito = "Guerreiro errante")
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals("", importada.nome)
        assertEquals("", importada.jogador)
        assertEquals("", importada.conceito)
    }

    @Test
    fun `equipamentos e pertences NAO sobrevivem`() {
        val original = CharacterSheet(
            weapons = listOf(com.example.model.Arma(nome = "Espada Longa")),
            pertences = "Uma mochila de couro"
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertTrue(importada.weapons.isEmpty())
        assertEquals("", importada.pertences)
    }

    // --- Regras de exportação ---

    @Test
    fun `exportar planilha em modo livre falha`() {
        val sheet = CharacterSheet(modoLivre = true)
        val resultado = ShareCodeCodec.exportar(sheet, emptyList(), emptyList())
        assertTrue(resultado.isFailure)
    }

    // --- Identificadores não reaproveitados ---

    @Test
    fun `importar gera novo id, nao reaproveita o da origem`() {
        val original = CharacterSheet(id = "id-original-fixo")
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertNotEquals("id-original-fixo", importada.id)
    }

    @Test
    fun `importar zera numero sequencial e data de salvamento`() {
        val original = CharacterSheet(numeroSequencial = 7, dataSalvamento = "25.08.26")
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(0, importada.numeroSequencial)
        assertEquals("", importada.dataSalvamento)
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
        assertEquals("O código não pertence a este aplicativo.", mensagemDeErro("OUTRACOISA-V3.abc.def"))
    }

    @Test
    fun `codigo com versao incompativel eh rejeitado`() {
        val codigoValido = exportarOuFalhar(CharacterSheet())
        // Precisa bater com VERSAO_ATUAL real do codec (privada, não
        // acessível daqui) — se a versão mudar de novo no futuro, este
        // replaceFirst precisa ser atualizado junto, senão silenciosamente
        // deixa de substituir nada e o teste passa a testar o caminho
        // errado (já aconteceu na mudança de V1 pra V2, e de novo agora
        // na mudança de V2 pra V3).
        val comVersaoErrada = codigoValido.replaceFirst("-V3.", "-V99.")
        assertEquals("A versão deste código não é compatível.", mensagemDeErro(comVersaoErrada))
    }

    @Test
    fun `codigo truncado (sem payload) eh rejeitado como incompleto`() {
        assertEquals("O código está incompleto ou corrompido.", mensagemDeErro("APPPLAN-V3.semSegundoPonto"))
    }

    @Test
    fun `codigo com checksum corrompida eh rejeitado`() {
        val codigoValido = exportarOuFalhar(CharacterSheet(attributes = mapOf("Força" to 3)))
        // Corrompe alguns caracteres do payload (apos os dois primeiros
        // pontos), sem alterar a checksum declarada — deve ser pego pela
        // verificacao de integridade.
        val partes = codigoValido.split(".", limit = 3)
        val payloadCorrompido = partes[2].reversed()
        val codigoCorrompido = "${partes[0]}.${partes[1]}.$payloadCorrompido"
        assertEquals("O código está incompleto ou corrompido.", mensagemDeErro(codigoCorrompido))
    }

    @Test
    fun `codigo com espacos acidentais no inicio e fim eh aceito normalmente`() {
        val codigoValido = exportarOuFalhar(CharacterSheet(attributes = mapOf("Força" to 5)))
        val comEspacos = "  \n$codigoValido\n  "
        val importada = importarOuFalhar(comEspacos)
        assertEquals(5, importada.attributes["Força"])
    }

    @Test
    fun `codigo acima do limite maximo eh rejeitado explicitamente`() {
        val codigoEnorme = "APPPLAN-V3.a.${"x".repeat(ShareCodeCodec.TAMANHO_MAXIMO_CODIGO + 10)}"
        assertEquals("O código excede o tamanho permitido.", mensagemDeErro(codigoEnorme))
    }
    @Test
    fun `tipo de personagem lunar e dados lunares sobrevivem a ida e volta`() {
        val original = CharacterSheet(
            tipoPersonagem = com.example.model.CharacterType.LUNAR,
            lunarCasta = com.example.model.LunarCasta.NoMoon,
            lunarCastaEscolhida = true,
            lunarFormaEspiritual = "Grande Gato",
            lunarSinal = "Lua Crescente"
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(com.example.model.CharacterType.LUNAR, importada.tipoPersonagem)
        assertEquals(com.example.model.LunarCasta.NoMoon, importada.lunarCasta)
        assertTrue(importada.lunarCastaEscolhida)
        assertEquals("Grande Gato", importada.lunarFormaEspiritual)
        assertEquals("Lua Crescente", importada.lunarSinal)
    }

    @Test
    fun `tipo de personagem sangue de dragao sobrevive a ida e volta`() {
        val original = CharacterSheet(
            tipoPersonagem = com.example.model.CharacterType.DRAGON_BLOODED,
            aspecto = "Ar"
        )
        val importada = importarOuFalhar(exportarOuFalhar(original))
        assertEquals(com.example.model.CharacterType.DRAGON_BLOODED, importada.tipoPersonagem)
        assertEquals("Ar", importada.aspecto)
    }

}
