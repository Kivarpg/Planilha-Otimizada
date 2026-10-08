package com.example.data

import com.example.model.CharacterSheet
import com.example.model.CharacterType
import com.example.model.Casta
import com.example.model.Especializacao
import com.example.model.ExaltedConstants
import com.example.model.Merito
import com.example.model.LunarCasta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharacterSheetJsonCodecTest {
    @Test
    fun `round trip preserva estado relevante da planilha`() {
        val original = CharacterSheet(
            nome = "Teste",
            jogador = "Jogador",
            conceito = "Guerreiro",
            tipoPersonagem = "Solar",
            casta = Casta.Dawn,
            aspecto = "",
            casteAbilities = listOf("Armas Brancas"),
            favoredAbilities = listOf("Atletismo"),
            supernalAbility = "Armas Brancas",
            abilities = ExaltedConstants.DEFAULT_ABILITIES + mapOf("Armas Brancas" to 5, "Atletismo" to 3),
            attributes = ExaltedConstants.DEFAULT_ATTRIBUTES + mapOf("Força" to 4, "Destreza" to 5),
            attributePriorities = mapOf("Físicos" to "1º", "Sociais" to "3º", "Mentais" to "2º"),
            specializations = listOf(Especializacao(nome = "Duelo", habilidade = "Armas Brancas")),
            motesPessoaisGastos = 2,
            motesPerifericosGastos = 4,
            forcaVontadeBase = 7,
            forcaVontadeUsados = setOf(1, 4),
            linguaNativa = "Português",
            linguasAdicionais = listOf("High Realm"),
            merits = listOf(Merito(nome = "Idioma", valor = 1, categoria = "Idioma", detalhe = "High Realm", origemAutomatica = "Idioma")),
            sessoes = 12,
            planilhaConcluida = true,
            modoLivre = false,
            experienciaGastaTotal = 25,
            iniciativaValor = 3
        )

        val restored = CharacterSheetJsonCodec.decode(CharacterSheetJsonCodec.encode(original))

        assertEquals(original, restored)
    }

    @Test
    fun `round trip preserva selecao 2 mais 2 do Lunar`() {
        val original = CharacterSheet(
            tipoPersonagem = CharacterType.LUNAR,
            lunarCasta = LunarCasta.FullMoon,
            lunarCastaEscolhida = true,
            lunarCasteAttributesEscolhidos = listOf("Força", "Destreza"),
            favoredAttributes = listOf("Vigor", "Percepção")
        )

        val restored = CharacterSheetJsonCodec.decode(CharacterSheetJsonCodec.encode(original))

        assertEquals(listOf("Força", "Destreza"), restored.lunarCasteAttributesEscolhidos)
        assertEquals(listOf("Vigor", "Percepção"), restored.favoredAttributes)
    }

    @Test
    fun `decode mantém migracao de armadura legada`() {
        val json = """
            {"id":"x","armadura":{"nome":"Armadura antiga","tipoArmadura":"Leve","categoriaPeso":"Leve"}}
        """.trimIndent()

        val restored = CharacterSheetJsonCodec.decode(json)

        assertTrue(restored.armaduras.isNotEmpty())
        assertEquals("Armadura antiga", restored.armaduras.first().nome)
    }
}
