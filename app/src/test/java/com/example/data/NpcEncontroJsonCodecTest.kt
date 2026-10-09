package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ArmaEncontro
import com.example.model.ArmaduraEncontro
import com.example.model.HistoricoXpBatch
import com.example.model.EncounterProgressionRoadmap
import com.example.model.EncounterProgressionStep
import com.example.model.EncantoEncontro
import com.example.model.NpcEncontro
import com.example.model.Merito
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NpcEncontroJsonCodecTest {
    @Test
    fun `round trip preserva NPC de encontro`() {
        val original = NpcEncontro(
            nome = "Solar de teste",
            arquetipo = ArquetipoEncontro.FISICO,
            casta = "Alvorada",
            habilidadesFavorecidas = listOf("Armas Brancas", "Atletismo"),
            habilidadeSupernal = "Armas Brancas",
            attributes = mapOf("Força" to 4, "Destreza" to 5),
            abilities = mapOf("Armas Brancas" to 5),
            merits = listOf(Merito(nome = "Influência", valor = 5, categoria = "normal", origemAutomatica = "Encontro: pontos restritos")),
            corpoDeTouroCount = 1,
            essencia = 2,
            idioma = "Alto Reino",
            xpAtual = 17,
            xpGastoTotal = 33,
            iniciativaAtual = 8,
            dano = "+5L"
        )

        val restored = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))

        assertEquals(original, restored)
    }

    @Test
    fun `encode grava schemaVersion atual`() {
        val obj = NpcEncontroJsonCodec.encodeObject(NpcEncontro(nome = "Schema atual"))
        assertEquals(NpcEncontroSchema.CURRENT_VERSION, obj.getInt("schemaVersion"))
    }

    @Test
    fun `decode migra save legado sem schemaVersion sem alterar dados`() {
        val original = NpcEncontro(nome = "Legado", genero = "Feminino", dano = "+3L")
        val legado = NpcEncontroJsonCodec.encodeObject(original).apply { remove("schemaVersion") }

        val restaurado = NpcEncontroJsonCodec.decode(legado.toString())

        assertEquals(original, restaurado)
    }

    @Test(expected = NpcSaveCorruptedException::class)
    fun `decode rejeita schema futuro desconhecido`() {
        val futuro = NpcEncontroJsonCodec.encodeObject(NpcEncontro(nome = "Futuro")).apply {
            put("schemaVersion", NpcEncontroSchema.CURRENT_VERSION + 1)
        }
        NpcEncontroJsonCodec.decode(futuro.toString())
    }

    @Test(expected = NpcSaveCorruptedException::class)
    fun `decode lanca NpcSaveCorruptedException para JSON invalido`() {
        NpcEncontroJsonCodec.decode("isto nao é um JSON")
    }

    @Test(expected = NpcSaveCorruptedException::class)
    fun `decode lanca NpcSaveCorruptedException para objeto JSON vazio truncado`() {
        NpcEncontroJsonCodec.decode("{")
    }

    @Test
    fun `decode pula entrada malformada dentro de uma lista em vez de travar`() {
        val original = NpcEncontro(nome = "Teste", casta = "Alvorada")
        val obj = NpcEncontroJsonCodec.encodeObject(original)
        // Corrompe só um elemento da lista de charms (string no lugar de objeto),
        // simulando exatamente o tipo de corrupção parcial coberto pelo stress test.
        obj.put("charms", org.json.JSONArray().apply { put("isto deveria ser um objeto") })

        val restored = NpcEncontroJsonCodec.decode(obj.toString())

        assertEquals(emptyList<Any>(), restored.charms)
    }

    @org.junit.Test
    fun sinalEhPersistidoNoNpc() {
        val original = NpcEncontro(
            nome = "Lunar",
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            sinal = "Webbed fingers and toes"
        )
        val restaurado = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))
        org.junit.Assert.assertEquals("Webbed fingers and toes", restaurado.sinal)
    }

    @org.junit.Test
    fun formaEspiritualEhPersistidaNoNpc() {
        val original = NpcEncontro(
            nome = "Lunar",
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            formaEspiritual = "Onça-pintada"
        )
        val restaurado = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))
        org.junit.Assert.assertEquals("Onça-pintada", restaurado.formaEspiritual)
    }

    @Test
    fun `round trip preserva equipamento manual progressao e derivados defensivos`() {
        val armaManual = ArmaEncontro(
            nome = "Daiklave de regressao",
            peso = "Pesada",
            tipo = "Artefato",
            precisao = 1,
            dano = 12,
            defesa = 0,
            motesComitados = 5,
            etiquetas = listOf("Letal", "Armas brancas")
        )
        val armaduraManual = ArmaduraEncontro(
            nome = "Armadura de regressao",
            peso = "Pesada",
            absorcao = 11,
            dureza = 10,
            penalidadeMobilidade = -2,
            motesComitados = 5,
            marcadores = listOf("Ocultável", "Silenciosa"),
            custoMeritoArtefato = 4
        )
        val original = NpcEncontro(
            id = "npc-persistencia-abas",
            nome = "Regressao Persistencia Abas",
            arma = armaManual,
            armadura = armaduraManual,
            absorcaoNatural = 4,
            absorcaoArmadura = armaduraManual.absorcao,
            absorcao = 4 + armaduraManual.absorcao,
            dureza = armaduraManual.dureza,
            iniciativaAtual = 13,
            xpAtual = 2,
            xpGastoTotal = 10,
            historicoXpBatches = listOf(
                HistoricoXpBatch(
                    xpGasto = 10,
                    nomesEncantosAdicionados = listOf("Encanto de regressao"),
                    habilidadeMelhorada = "Armas Brancas",
                    pontosGanhosNaHabilidade = 1
                )
            )
        )

        val restaurado = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))

        assertEquals(original, restaurado)
        assertEquals(armaManual, restaurado.arma)
        assertEquals(armaduraManual, restaurado.armadura)
        assertEquals(armaduraManual.absorcao, restaurado.absorcaoArmadura)
        assertEquals(restaurado.absorcaoNatural + armaduraManual.absorcao, restaurado.absorcao)
        assertEquals(armaduraManual.dureza, restaurado.dureza)
        assertEquals(original.historicoXpBatches, restaurado.historicoXpBatches)
    }

    @Test
    fun `decode preserva compatibilidade com equipamento de save antigo`() {
        val antigo = NpcEncontroJsonCodec.encodeObject(
            NpcEncontro(
                nome = "Save antigo",
                arma = ArmaEncontro(
                    nome = "Daiklave antiga",
                    peso = "Pesada",
                    tipo = "Artefato",
                    precisao = 1,
                    dano = 12,
                    defesa = 0,
                    motesComitados = 5
                ),
                armadura = ArmaduraEncontro(
                    nome = "Armadura antiga",
                    peso = "Pesada",
                    tipo = "Artefato",
                    absorcao = 11,
                    dureza = 10,
                    penalidadeMobilidade = -2,
                    motesComitados = 5
                )
            )
        )
        antigo.getJSONObject("arma").remove("etiquetas")
        antigo.getJSONObject("armadura").apply {
            remove("marcadores")
            remove("custoMeritoArtefato")
            put("tipo", "   ")
        }

        val restaurado = NpcEncontroJsonCodec.decode(antigo.toString())

        assertEquals(emptyList<String>(), restaurado.arma?.etiquetas)
        assertEquals(emptyList<String>(), restaurado.armadura?.marcadores)
        assertEquals(3, restaurado.armadura?.custoMeritoArtefato)
        assertEquals("Artefato", restaurado.armadura?.tipo)
    }

    @Test
    fun `round trip nao persiste roadmap de progressao`() {
        val original = NpcEncontro(nome = "Solar com cache transitório")
        val restored = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))
        assertEquals(original, restored)
        assertTrue(!NpcEncontroJsonCodec.encodeObject(original).has("roadmapProgressao"))
    }


    @Test
    fun taxonomiaArquetipoLunarEhPersistidaComSegundaForma() {
        val original = NpcEncontro(
            nome = "Lunar Arquétipo",
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            formaEspiritual = "Lobo",
            formaEspiritualSecundaria = "Morcego",
            lunarArchetypeTraits = listOf("CACA_EM_GRUPO", "PREDATORIO", "VISAO_NOTURNA"),
            lunarPrimaryArchetypeTraits = listOf("CACA_EM_GRUPO", "PREDATORIO")
        )
        val restaurado = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))
        org.junit.Assert.assertEquals("Lobo", restaurado.formaEspiritual)
        org.junit.Assert.assertEquals("Morcego", restaurado.formaEspiritualSecundaria)
        org.junit.Assert.assertEquals(original.lunarArchetypeTraits, restaurado.lunarArchetypeTraits)
        org.junit.Assert.assertEquals(original.lunarPrimaryArchetypeTraits, restaurado.lunarPrimaryArchetypeTraits)
    }


    @Test
    fun `round trip preserva arvore ofensiva Lunar sem foco explicito`() {
        val original = NpcEncontro(
            nome = "Lunar especializado",
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            arquetipo = ArquetipoEncontro.FISICO,
            lunarAtaqueEscolhido = "Destreza"
        )
        val restaurado = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))
        assertEquals("Destreza", restaurado.lunarAtaqueEscolhido)
        assertEquals(original, restaurado)
    }

    @Test
    fun `decode ignora especializacao ofensiva invalida sem alterar foco`() {
        val payload = NpcEncontroJsonCodec.encodeObject(
            NpcEncontro(
                nome = "Lunar foco preservado",
                tipoExaltado = TipoExaltadoEncontro.LUNAR,
                focoProgressaoExplicito = "Vigor"
            )
        ).apply { put("lunarAtaqueEscolhido", "Manipulação") }
        val restaurado = NpcEncontroJsonCodec.decode(payload.toString())
        assertEquals(null, restaurado.lunarAtaqueEscolhido)
        assertEquals("Vigor", restaurado.focoProgressaoExplicito)
    }

    @Test
    fun `schema 5 migra sem inventar arvore ofensiva Lunar`() {
        val v5 = NpcEncontroJsonCodec.encodeObject(
            NpcEncontro(nome = "Legado Lunar", lunarAtaqueEscolhido = "Força")
        ).apply {
            put("schemaVersion", 5)
            remove("lunarAtaqueEscolhido")
        }
        val restaurado = NpcEncontroJsonCodec.decode(v5.toString())
        assertEquals(null, restaurado.lunarAtaqueEscolhido)
    }

    @Test
    fun `round trip preserva foco explicito de progressao`() {
        val original = NpcEncontro(nome = "Foco", focoProgressaoExplicito = "Destreza")
        val restaurado = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(original))

        assertEquals("Destreza", restaurado.focoProgressaoExplicito)
        assertEquals(original, restaurado)
    }

    @Test
    fun `schema 4 migra sem inventar foco explicito`() {
        val v4 = NpcEncontroJsonCodec.encodeObject(
            NpcEncontro(nome = "Schema 4", focoProgressaoExplicito = "Destreza")
        ).apply {
            put("schemaVersion", 4)
            remove("focoProgressaoExplicito")
        }

        val restaurado = NpcEncontroJsonCodec.decode(v4.toString())
        assertEquals(null, restaurado.focoProgressaoExplicito)
    }

    @Test
    fun `decode migra explicitamente schema 2 para schema atual`() {
        val original = NpcEncontro(
            nome = "Schema 2",
            tipoExaltado = TipoExaltadoEncontro.LUNAR,
            formaEspiritual = "Onça-pintada",
            primeiroXpRecebido = true
        )
        val v2 = NpcEncontroJsonCodec.encodeObject(original).apply {
            put("schemaVersion", 2)
        }
        assertEquals(original, NpcEncontroJsonCodec.decode(v2.toString()))
        assertEquals(6, NpcEncontroSchema.CURRENT_VERSION)
    }
}
