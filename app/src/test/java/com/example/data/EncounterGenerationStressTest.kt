package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * Stress determinístico do núcleo do gerador. O modo normal mantém o CI rápido;
 * -Dexalted.fullAudit=true amplia automaticamente a amostra para a maratona.
 * Cada seed fica na mensagem da asserção para tornar qualquer falha reproduzível.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterGenerationStressTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val solares = EncantosSolaresCatalog(app).definitions
    private val dragoes = EncantosSangueDosDragoesCatalog(app).definitions
    private val lunares = EncantosLunaresCatalog(app).definitions
    private val feiticos = FeiticariaCatalog(app).definitions
    private val meritos = MeritosCatalog(app).definitions
    private val estilos = ArtesMarciaisCatalog(app).definitions

    private fun assertInvariants(npc: NpcEncontro, expected: TipoExaltadoEncontro, seed: Int) {
        val tag = "tipo=$expected seed=$seed arquetipo=${npc.arquetipo} nome=${npc.nome}"
        assertEquals(tag, expected, npc.tipoExaltado)
        assertTrue("$tag: id vazio", npc.id.isNotBlank())
        assertTrue("$tag: nome vazio", npc.nome.isNotBlank())
        assertTrue("$tag: Essencia invalida ${npc.essencia}", npc.essencia >= 1)
        assertEquals("$tag: conjunto de Atributos incompleto", ExaltedConstants.ALL_ATTRIBUTES.toSet(), npc.attributes.keys.toSet())
        assertTrue("$tag: Atributo fora de 1..5", npc.attributes.values.all { it in 1..5 })
        assertEquals("$tag: conjunto de Habilidades incompleto", ExaltedConstants.ALL_25_ABILITIES.toSet(), npc.abilities.keys.toSet())
        assertTrue("$tag: Habilidade negativa", npc.abilities.values.all { it >= 0 })
        assertTrue("$tag: XP atual negativo", npc.xpAtual >= 0)
        assertTrue("$tag: XP gasto negativo", npc.xpGastoTotal >= 0)
        assertTrue("$tag: Vontade fora do limite", npc.forcaDeVontade in 5..10)
        assertTrue("$tag: absorcao inconsistente", npc.absorcao == npc.absorcaoNatural + npc.absorcaoArmadura)
        assertTrue("$tag: charms com nome vazio", npc.charms.none { it.nome.isBlank() })
        assertTrue("$tag: feiticos com nome vazio", npc.feiticos.none { it.nome.isBlank() })
        assertTrue("$tag: estilos adicionais duplicados", npc.estilosArtesMarciaisAdicionais.distinct().size == npc.estilosArtesMarciaisAdicionais.size)
        if (expected == TipoExaltadoEncontro.LUNAR) {
            assertTrue("$tag: forma espiritual ausente", npc.formaEspiritual.isNotBlank())
            assertEquals("$tag: Lunar deve ter 2 Atributos de Casta", 2, npc.lunarAtributosCasta.size)
            assertEquals("$tag: Atributos de Casta Lunar duplicados", 2, npc.lunarAtributosCasta.distinct().size)
        }
        val restored = NpcEncontroJsonCodec.decode(NpcEncontroJsonCodec.encode(npc))
        assertEquals("$tag: round-trip alterou o NPC", npc, restored)
    }

    @Test fun `maratona deterministica cobre os tres tipos e todos arquetipos`() {
        val rounds = EncounterTestSamples.count(120)
        var seed = 0
        repeat(rounds) {
            ArquetipoEncontro.entries.forEach { arquetipo ->
                val solar = EncounterGenerator.gerarSolar("Stress Solar $seed", arquetipo, solares, feiticos,
                    random = Random(seed), meritosCatalogo = meritos, estilosArtesMarciais = estilos)
                assertInvariants(solar, TipoExaltadoEncontro.SOLAR, seed++)
                val db = EncounterGenerator.gerarSangueDeDragao("Stress DB $seed", arquetipo, dragoes, feiticos,
                    random = Random(seed), meritosCatalogo = meritos, estilosArtesMarciais = estilos)
                assertInvariants(db, TipoExaltadoEncontro.SANGUE_DE_DRAGAO, seed++)
                val lunar = EncounterGenerator.gerarLunar("Stress Lunar $seed", arquetipo, lunares, feiticos,
                    random = Random(seed), meritosCatalogo = meritos, estilosArtesMarciais = estilos)
                assertInvariants(lunar, TipoExaltadoEncontro.LUNAR, seed++)
            }
        }
    }

    @Test fun `maratona lunar consecutiva preserva identidade e invariantes`() {
        val count = EncounterTestSamples.count(300)
        val ids = HashSet<String>(count)
        repeat(count) { seed ->
            val arquetipo = ArquetipoEncontro.entries[seed % ArquetipoEncontro.entries.size]
            val npc = EncounterGenerator.gerarLunar("Lunar Stress $seed", arquetipo, lunares, feiticos,
                random = Random(100_000 + seed), meritosCatalogo = meritos, estilosArtesMarciais = estilos)
            assertInvariants(npc, TipoExaltadoEncontro.LUNAR, seed)
            assertTrue("ID Lunar repetido na seed $seed", ids.add(npc.id))
        }
    }
}
