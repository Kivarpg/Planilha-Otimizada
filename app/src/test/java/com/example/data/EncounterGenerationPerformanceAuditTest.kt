package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.ArquetipoEncontro
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Diagnóstico reproduzível, sem limites rígidos de tempo para evitar testes
 * instáveis em runners compartilhados. Execução explícita de auditoria.
 * As amostras incluem custo do gerador, não o carregamento inicial do catálogo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncounterGenerationPerformanceAuditTest {
    @Test
    fun `mede geracao por tipo e arquetipo com catalogos reais`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val solar = EncantosSolaresCatalog(app).definitions
        val dragon = EncantosSangueDosDragoesCatalog(app).definitions
        val lunar = EncantosLunaresCatalog(app).definitions
        val feiticos = FeiticariaCatalog(app).definitions
        val meritos = MeritosCatalog(app).definitions
        val estilos = ArtesMarciaisCatalog(app).definitions
        val repeticoes = System.getProperty("exalted.perfSamples")?.toIntOrNull()
            ?.coerceIn(3, 100) ?: 5
        val tipos = listOf("Solar", "Sangue de Dragao", "Lunar")
        // Intercalar as configuracoes reduz o vies de aquecimento da JVM e
        // de variacoes de carga do runner durante a medicao.
        val ordem = ArquetipoEncontro.entries.flatMap { arquetipo ->
            tipos.map { tipo -> tipo to arquetipo }
        }
        val amostrasPorConfiguracao = ordem.associateWith { ArrayList<Pair<Int, Long>>(repeticoes) }
        repeat(repeticoes + 1) { rodada ->
            val sequencia = if (rodada % 2 == 0) ordem else ordem.reversed()
            for ((tipo, arquetipo) in sequencia) {
                    val seed = 80_000 + rodada
                    val tempo = measureNanoTime {
                        val npc = when (tipo) {
                            "Solar" -> EncounterGenerator.gerarSolar(
                                "Perf Solar", arquetipo, solar, feiticos,
                                random = Random(seed), meritosCatalogo = meritos,
                                estilosArtesMarciais = estilos
                            )
                            "Sangue de Dragao" -> EncounterGenerator.gerarSangueDeDragao(
                                "Perf DB", arquetipo, dragon, feiticos,
                                random = Random(seed), meritosCatalogo = meritos,
                                estilosArtesMarciais = estilos
                            )
                            else -> EncounterGenerator.gerarLunar(
                                "Perf Lunar", arquetipo, lunar, feiticos,
                                random = Random(seed), meritosCatalogo = meritos,
                                estilosArtesMarciais = estilos
                            )
                        }
                        check(npc.id.isNotBlank())
                    }
                    if (rodada > 0) amostrasPorConfiguracao.getValue(tipo to arquetipo).add(seed to tempo)
            }
        }
        for ((tipo, arquetipo) in ordem) {
            val amostras = amostrasPorConfiguracao.getValue(tipo to arquetipo)
            amostras.sortBy { it.second }
            fun percentile(p: Int): Long =
                amostras[((p * amostras.size + 99) / 100 - 1).coerceIn(0, amostras.lastIndex)].second
                println(
                    "EXALTED_PERF tipo=$tipo arquetipo=$arquetipo n=$repeticoes " +
                        "p50_ms=${percentile(50) / 1_000_000.0} " +
                        "p95_ms=${percentile(95) / 1_000_000.0} " +
                        "max_ms=${amostras.last().second / 1_000_000.0} " +
                        "slowest_seed=${amostras.last().first}"
                )
        }
    }
}
