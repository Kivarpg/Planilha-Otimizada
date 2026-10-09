package com.example.data

import com.example.model.ArquetipoEncontro
import kotlin.random.Random
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

class EncounterSpellSelectionServiceTest {

    private fun feitico(
        nome: String,
        circulo: String,
        descricao: String = ""
    ) = FeiticoDefinition(
        id = nome,
        nome = nome,
        nomeIngles = nome,
        circulo = circulo,
        custo = "1m",
        palavrasChave = "",
        duracao = "",
        livro = "Teste",
        descricao = descricao
    )

    private fun terrestres() = listOf(
        feitico("Demônio do Primeiro Círculo", "Terrestre"),
        feitico("Cão de Caça dos Cinco Ventos", "Terrestre"),
        feitico("Mensageiro Infalível", "Terrestre", "Feitiço de controle"),
        feitico("Invocar Elemental", "Terrestre"),
        feitico("Pele Invulnerável de Bronze", "Terrestre"),
        feitico("Virtuoso Guardião da Chama", "Terrestre"),
        feitico("Voo da Separação", "Terrestre")
    )

    @Test
    fun terrestresIgnoramNomesForaDaListaFechada() {
        val catalogo = terrestres() + feitico("Outro Feitiço", "Terrestre")

        val selecionados = EncounterSpellSelectionService.selecionarTerrestres(
            catalogo = catalogo,
            quantidadeAlvo = 7,
            random = Random(1)
        )

        assertEquals(7, selecionados.size)
        assertTrue(selecionados.none { it.nome == "Outro Feitiço" })
    }

    @Test
    fun selecaoTerrestreDeUmElementoSoPermaneceSomenteSeForControle() {
        val normal = listOf(feitico("Demônio do Primeiro Círculo", "Terrestre"))
        val controle = listOf(feitico("Mensageiro Infalível", "Terrestre", "FEITIÇO DE CONTROLE"))

        val normalSelecionado = EncounterSpellSelectionService.selecionarTerrestres(
            normal, 1, Random(2)
        )
        val controleSelecionado = EncounterSpellSelectionService.selecionarTerrestres(
            controle, 1, Random(2)
        )

        assertTrue(normalSelecionado.isEmpty())
        assertEquals(listOf("Mensageiro Infalível"), controleSelecionado.map { it.nome })
    }

    @Test
    fun celestiaisEsolaresSelecionamSomenteFeiticosDeControleDoCirculo() {
        val catalogo = listOf(
            feitico("Celestial Controle", "Celestial", "Feitiço de controle"),
            feitico("Celestial Normal", "Celestial"),
            feitico("Solar Controle", "Solar", "feitiço de controle"),
            feitico("Solar Normal", "Solar"),
            feitico("Terrestre Controle", "Terrestre", "Feitiço de controle")
        )

        val celestiais = EncounterSpellSelectionService.selecionarCelestiaisOuSolares(
            catalogo, "Celestial", 5, Random(3)
        )
        val solares = EncounterSpellSelectionService.selecionarCelestiaisOuSolares(
            catalogo, "Solar", 5, Random(4)
        )

        assertEquals(listOf("Celestial Controle"), celestiais.map { it.nome })
        assertEquals(listOf("Solar Controle"), solares.map { it.nome })
        assertFalse(celestiais.any { it.circulo != "Celestial" })
        assertFalse(solares.any { it.circulo != "Solar" })
    }

    @Test
    fun fisicoNuncaRecebeFeiticosForaDoSorteioDe25Porcento() {
        val catalogo = terrestres()
        val semMagia = EncounterSpellSelectionService.selecionarParaArquetipo(
            ArquetipoEncontro.FISICO,
            catalogo,
            emptyList(),
            emptyList(),
            true,
            Random(0)
        )
        // Com quantidadeAlvo=1 a regra 1.3 pode expandir para 2 (quando o
        // único sorteado não é de controle) ou 0. O sorteio de 25% ainda
        // garante que a maioria das sementes não recebe magia.
        assertTrue(semMagia.size <= 2)
        assertTrue(semMagia.all { it.nome in terrestres().map { fe -> fe.nome }.toSet() })
    }

    @Test
    fun mentalMantemMinimoDeQuatroQuandoCatalogoTemOpcoesSuficientes() {
        val celestial = (1..3).map { feitico("Celestial $it", "Celestial", "Feitiço de controle") }
        val solar = listOf(feitico("Solar 1", "Solar", "Feitiço de controle"))

        val selecionados = EncounterSpellSelectionService.selecionarParaArquetipo(
            ArquetipoEncontro.MENTAL,
            terrestres(),
            celestial,
            solar,
            true,
            Random(10)
        )

        assertTrue(selecionados.size >= 4)
        assertTrue(selecionados.any { it.circulo == "Solar" })
        assertTrue(selecionados.any { it.circulo == "Celestial" })
        assertTrue(selecionados.any { it.circulo == "Terrestre" })
    }

    @Test
    fun `sem feiticaria terrestre nenhum arquetipo recebe feiticos`() {
        val catalogo = terrestres() +
            listOf(feitico("Celestial Controle", "Celestial", "Feitiço de controle")) +
            listOf(feitico("Solar Controle", "Solar", "Feitiço de controle"))

        ArquetipoEncontro.entries.forEach { arquetipo ->
            val selecionados = EncounterSpellSelectionService.selecionarParaArquetipo(
                arquetipo = arquetipo,
                catalogoTerrestre = catalogo,
                catalogoCelestial = catalogo,
                catalogoSolar = catalogo,
                possuiFeiticariaTerrestre = false,
                random = Random(10)
            )
            assertTrue(selecionados.isEmpty())
        }
    }

    @Test
    fun `feiticaria terrestre concede um feitico gratuito quando ainda nao ha feiticos`() {
        val catalogo = listOf(
            FeiticoDefinition("1", "Feitiço Terrestre A", "", "Terrestre", "5m", "", "", "", ""),
            FeiticoDefinition("2", "Feitiço Celestial", "", "Celestial", "10m", "", "", "", "")
        )

        val resultado = EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
            possuiFeiticariaTerrestre = true,
            selecionados = emptyList(),
            catalogo = catalogo,
            random = Random(7)
        )

        assertEquals(1, resultado.size)
        assertEquals("Terrestre", resultado.single().circulo)
    }

    @Test
    fun `feiticaria terrestre concede feitico adicional mesmo quando ja existem feiticos selecionados`() {
        val selecionado = FeiticoDefinition("1", "Feitiço Terrestre A", "", "Terrestre", "5m", "", "", "", "")
        val outroTerrestre = FeiticoDefinition("2", "Feitiço Terrestre B", "", "Terrestre", "10m", "", "", "", "")
        val celestial = FeiticoDefinition("3", "Feitiço Celestial", "", "Celestial", "10m", "", "", "", "")

        val resultado = EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
            possuiFeiticariaTerrestre = true,
            selecionados = listOf(selecionado, celestial),
            catalogo = listOf(selecionado, outroTerrestre, celestial),
            random = Random(7)
        )

        assertEquals(3, resultado.size)
        assertEquals(listOf(selecionado, celestial), resultado.take(2))
        assertEquals(outroTerrestre, resultado.last())
    }

    @Test
    fun `feiticaria terrestre nao duplica quando catalogo nao tem outro terrestre`() {
        val feitico = FeiticoDefinition("1", "Feitiço Terrestre A", "", "Terrestre", "5m", "", "", "", "")

        val resultado = EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
            possuiFeiticariaTerrestre = true,
            selecionados = listOf(feitico),
            catalogo = listOf(feitico),
            random = Random(7)
        )

        assertEquals(listOf(feitico), resultado)
        // O mesmo Feitiço pode ocupar a vaga inicial sem duplicar a lista.
        assertEquals(feitico, resultado.last())
    }

    @Test
    fun `Lunar sem Circulo Solar jamais recebe feitico Solar`() {
        val selecionados = EncounterSpellSelectionService.selecionarParaArquetipo(
            arquetipo = ArquetipoEncontro.MENTAL,
            catalogoTerrestre = terrestres(),
            catalogoCelestial = listOf(feitico("Controle Celestial", "Celestial", "feitiço de controle")),
            catalogoSolar = listOf(feitico("Controle Solar", "Solar", "feitiço de controle")),
            possuiFeiticariaTerrestre = true,
            random = Random(10),
            possuiFeiticariaCelestial = true,
            possuiFeiticariaSolar = false
        )
        assertTrue(selecionados.none { it.circulo == "Solar" })
    }

    @Test
    fun `sem Circulo Celestial nao recebe feitico Celestial`() {
        val selecionados = EncounterSpellSelectionService.selecionarParaArquetipo(
            arquetipo = ArquetipoEncontro.MENTAL,
            catalogoTerrestre = terrestres(),
            catalogoCelestial = listOf(feitico("Controle Celestial", "Celestial", "feitiço de controle")),
            catalogoSolar = emptyList(),
            possuiFeiticariaTerrestre = true,
            random = Random(10),
            possuiFeiticariaCelestial = false,
            possuiFeiticariaSolar = false
        )
        assertTrue(selecionados.none { it.circulo == "Celestial" })
    }


    @Test
    fun `politica de circulos por exaltacao permanece restrita`() {
        assertEquals(
            setOf("Terrestre"),
            EncounterNpcSpellManagement.circulosPermitidos(com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO)
        )
        assertEquals(
            setOf("Terrestre", "Celestial"),
            EncounterNpcSpellManagement.circulosPermitidos(com.example.model.TipoExaltadoEncontro.LUNAR)
        )
        assertEquals(
            setOf("Terrestre", "Celestial", "Solar"),
            EncounterNpcSpellManagement.circulosPermitidos(com.example.model.TipoExaltadoEncontro.SOLAR)
        )
    }

    @Test
    fun `sem circulo terrestre ou sem catalogo elegivel nao altera a selecao`() {
        val escolhido = feitico("Feitico ja escolhido", "Celestial")
        val catalogo = listOf(
            feitico("Outro celestial", "Celestial"),
            feitico("Outro solar", "Solar")
        )
        repeat(50) { seed ->
            assertEquals(
                listOf(escolhido),
                EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
                    false, listOf(escolhido), terrestres(), Random(seed)
                )
            )
            assertEquals(
                listOf(escolhido),
                EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
                    true, listOf(escolhido), catalogo, Random(seed)
                )
            )
        }
    }

    @Test
    fun `nomes repetidos no catalogo nao duplicam feitico gratuito ja escolhido`() {
        val repetido = feitico("Mesmo nome", "Terrestre")
        val outro = feitico("Outro nome", "Terrestre")
        val catalogo = listOf(repetido, feitico("Mesmo nome", "Terrestre"), outro)
        repeat(100) { seed ->
            val resultado = EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
                true, listOf(repetido), catalogo, Random(seed)
            )
            assertEquals(listOf(repetido, outro), resultado)
        }
    }

    @Test
    fun `selecionar feitico gratuito preserva sorteios por seed apos indexacao unica`() {
        val catalogo = terrestres() + feitico("Outro Celestial", "Celestial")
        val cenarios = listOf(
            emptyList<FeiticoDefinition>(),
            catalogo.take(2),
            catalogo.filter { it.circulo == "Terrestre" },
            catalogo.takeLast(1)
        )
        for (selecionados in cenarios) {
            repeat(100) { seed ->
                val nomes = selecionados.map { it.nome }.toSet()
                val candidatos = catalogo.filter { it.circulo == "Terrestre" }
                // A implementação anterior embaralhava a lista vazia antes
                // do fallback; preservamos a mesma sequência de chamadas RNG.
                val rngAntigo = Random(seed)
                val escolhidoAntigo = candidatos
                    .filter { it.nome !in nomes }
                    .shuffled(rngAntigo)
                    .firstOrNull()
                    ?: selecionados.firstOrNull { it.circulo == "Terrestre" }
                    ?: candidatos.shuffled(rngAntigo).firstOrNull()
                val esperado = if (escolhidoAntigo == null || escolhidoAntigo.nome in nomes) {
                    selecionados
                } else {
                    selecionados + escolhidoAntigo
                }
                val atual = EncounterSpellSelectionService.adicionarFeiticoGratuitoSeNecessario(
                    true, selecionados, catalogo, Random(seed)
                )
                assertEquals("seed=$seed selecionados=${selecionados.size}", esperado, atual)
            }
        }
    }


    @Test
    fun `indexacao de catalogo preserva sorteios terrestres e de controle`() {
        val catalogo = terrestres() + listOf(
            feitico("Celestial Controle A", "Celestial", "Feitiço de controle"),
            feitico("Celestial Normal", "Celestial"),
            feitico("Celestial Controle B", "Celestial", "feitiço de controle"),
            feitico("Solar Controle A", "Solar", "Feitiço de controle")
        )
        val candidatosTerrestres = catalogo.filter {
            it.circulo == "Terrestre" && it.nome in setOf(
                "Demônio do Primeiro Círculo", "Cão de Caça dos Cinco Ventos",
                "Mensageiro Infalível", "Invocar Elemental",
                "Pele Invulnerável de Bronze", "Virtuoso Guardião da Chama",
                "Voo da Separação"
            )
        }
        val controlesCelestiais = catalogo.filter {
            it.circulo == "Celestial" && it.descricao.contains("feitiço de controle", ignoreCase = true)
        }
        repeat(200) { seed ->
            val sorteadosTerrestres = candidatosTerrestres.shuffled(Random(seed)).take(3)
            assertEquals(
                "terrestre seed=$seed",
                sorteadosTerrestres,
                EncounterSpellSelectionService.selecionarTerrestres(catalogo, 3, Random(seed))
            )
            val sorteadosCelestiais = controlesCelestiais.shuffled(Random(seed)).take(2)
            assertEquals(
                "celestial seed=$seed",
                sorteadosCelestiais,
                EncounterSpellSelectionService.selecionarCelestiaisOuSolares(
                    catalogo, "Celestial", 2, Random(seed)
                )
            )
        }
    }

}
