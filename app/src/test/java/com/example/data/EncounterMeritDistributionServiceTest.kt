package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ArmaEncontro
import com.example.model.ArmaduraEncontro
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EncounterMeritDistributionServiceTest {
    private val catalogo = listOf(
        def("Apoio", listOf(2, 3, 4), repeatable = true),
        def("Comando", listOf(2, 3, 4, 5), repeatable = true),
        def("Contatos", listOf(1, 3, 5), repeatable = true),
        def("Seguidores", listOf(1, 2, 3), repeatable = true),
        def("Influência", listOf(1, 2, 3, 4, 5), repeatable = true),
        def("Idioma", listOf(1), repeatable = true),
        def("Recursos", listOf(1, 2, 3, 4, 5)),
        def("Vassalos", listOf(2, 4), repeatable = true),
        def("Aliados", listOf(1, 3, 5), repeatable = true),
        def("Ambidestro", listOf(1, 2)),
        def("Couro Incomum", listOf(1, 2, 3), categoria = "sobrenatural")
    )

    @Test
    fun `os tres tipos de exaltado recebem arma e armadura artefato`() {
        listOf(
            TipoExaltadoEncontro.SOLAR,
            TipoExaltadoEncontro.LUNAR,
            TipoExaltadoEncontro.SANGUE_DE_DRAGAO
        ).forEach { tipo ->
            val (arma, armadura) = EncounterEquipmentService.selecionarEquipamentoParaEncontro(
                habilidadeCombate = "Armas Brancas", random = Random(tipo.ordinal + 100), permitirDoisArtefatos = true
            )
            assertEquals("Artefato", arma?.tipo)
            assertEquals("Artefato", armadura.tipo)
        }
    }

    @Test
    fun `solar usa 10 pontos com dois artefatos e 4 pontos livres`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogo, Random(1))
        assertEquals(10, merits.sumOf { it.valor })
        assertEquals(1, merits.count { it.nome == "Artefato" && it.detalhe == "Arma" && it.valor == 3 })
        assertEquals(1, merits.count { it.nome == "Artefato" && it.detalhe == "Armadura" && it.valor == 3 })
        assertEquals(4, merits.filter { it.nome !in setOf("Artefato") }.sumOf { it.valor })
        assertEquals(0, merits.filter { it.origemAutomatica == "Encontro: Méritos restritos adicionais" }.sumOf { it.valor })
    }

    @Test
    fun `solar pode receber supernatural no pool livre`() {
        val catalogoSomenteSupernatural = catalogo.filter { it.nome == "Apoio" || it.nome == "Couro Incomum" }
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR, arquetipo = ArquetipoEncontro.MENTAL,
            arma = arma("Artefato"), armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogoSomenteSupernatural, Random(22))
        assertEquals(10, merits.sumOf { it.valor })
        assertTrue(merits.any { it.categoria == "sobrenatural" })
    }

    @Test
    fun `lunar pode receber supernatural no pool livre`() {
        val catalogoSomenteSupernatural = catalogo.filter { it.nome == "Apoio" || it.nome == "Couro Incomum" }
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.LUNAR, arquetipo = ArquetipoEncontro.SOCIAL,
            arma = arma("Artefato"), armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogoSomenteSupernatural, Random(23))
        assertEquals(10, merits.sumOf { it.valor })
        assertTrue(merits.any { it.categoria == "sobrenatural" })
    }

    @Test
    fun `sangue de dragao limita a cinco e funde apoio entre pool normal e lista restrita`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val catalogoApoio = listOf(
            def("Apoio", listOf(2, 3, 4), repeatable = true),
            def("Recursos", listOf(1, 2, 3, 4, 5)),
            def("Contatos", listOf(1, 3, 5), repeatable = true),
            def("Seguidores", listOf(1, 2, 3), repeatable = true),
            def("Influência", listOf(1, 2, 3, 4, 5), repeatable = true),
            def("Idioma", listOf(1), repeatable = true),
            def("Vassalos", listOf(2, 4), repeatable = true),
            def("Comando", listOf(2, 3, 4, 5), repeatable = true)
        )

        repeat(50) { seed ->
            val merits = EncounterMeritDistributionService.distribuir(npc, catalogoApoio, Random(seed))
            assertTrue(merits.all { it.valor <= 5 })
            assertTrue(merits.filter { it.nome !in setOf("Idioma", "Artefato") }.groupBy { it.nome.trim().lowercase() }.values.all { it.size == 1 })
            assertEquals(18, merits.sumOf { it.valor })
        }
    }

    @Test
    fun `sangue de dragao usa 13 normais mais 5 restritos e aceita dois artefatos`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogo, Random(3))
        assertEquals(18, merits.sumOf { it.valor })
        assertTrue(merits.all { it.valor <= 5 })
        assertTrue(merits.filter { it.nome !in setOf("Idioma", "Artefato") }.groupBy { it.nome.trim().lowercase() }.values.all { it.size == 1 })
        assertTrue(merits.none { it.categoria == "sobrenatural" })
        assertEquals(1, merits.count { it.nome == "Artefato" && it.detalhe == "Arma" && it.valor == 3 })
        assertEquals(1, merits.count { it.nome == "Artefato" && it.detalhe == "Armadura" && it.valor == 3 })
    }

    @Test
    fun `meritos com pre requisitos de atributo ou habilidade nunca sao gerados sem requisito`() {
        val catalogoComRequisitos = listOf(
            def("Músculos Poderosos", listOf(1, 2, 3), repeatable = true, preRequisitos = listOf("Força •••")),
            def("Pé Veloz", listOf(4), preRequisitos = listOf("Destreza •••")),
            def("Artista Marcial", listOf(4), preRequisitos = listOf("Briga •")),
            def("Livre", listOf(4))
        )
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.FISICO,
            attributes = mapOf("Força" to 1, "Destreza" to 1),
            abilities = mapOf("Briga" to 0),
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogoComRequisitos, Random(41))
        assertTrue(merits.none { it.nome in setOf("Músculos Poderosos", "Pé Veloz", "Artista Marcial") })
        assertEquals(10, merits.sumOf { it.valor })
    }


    @Test
    fun `prioriza merito que abre caminho de outro merito quando o orcamento permite`() {
        val base = def("Influência", listOf(1), repeatable = true)
        val dependente = def(
            "Sobrenome de Renome",
            listOf(2),
            preRequisitos = listOf("Influência 1+")
        )
        val terminal = def("Livre", listOf(4))
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.SOCIAL,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )

        val merits = EncounterMeritDistributionService.distribuir(
            npc,
            listOf(base, dependente, terminal),
            Random(123)
        )

        assertTrue(merits.any { it.nome == "Influência" })
        assertTrue(merits.any { it.nome == "Sobrenome de Renome" })
        assertEquals(10, merits.sumOf { it.valor })
    }

    @Test
    fun `pre requisito por merito e sobrenatural exige dependencia ja adquirida`() {
        val sutileza = def(
            "Sutileza", listOf(2), categoria = "sobrenatural",
            preRequisitos = listOf("Outro Mérito Sobrenatural fisicamente evidente, como garras")
        )
        val influencia = def("Influência", listOf(1), repeatable = true)
        val sobrenome = def("Sobrenome de Renome", listOf(2), preRequisitos = listOf("Influência 1+"))
        val sobrenatural = def("Bênção", listOf(2), categoria = "sobrenatural")
        val catalogoComRequisitos = listOf(sutileza, sobrenome, influencia, sobrenatural, def("Livre", listOf(4)))
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.SOCIAL,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        repeat(8) { seed ->
            val merits = EncounterMeritDistributionService.distribuir(npc, catalogoComRequisitos, Random(100 + seed))
            val sobrenomeGerado = merits.any { it.nome == "Sobrenome de Renome" }
            if (sobrenomeGerado) assertTrue(merits.any { it.nome == "Influência" && it.valor >= 1 })
            val sutilezaGerada = merits.any { it.nome == "Sutileza" }
            if (sutilezaGerada) assertTrue(merits.any { it.categoria == "sobrenatural" && it.nome != "Sutileza" })
        }
    }

    @Test
    fun `pre requisito de merito usa maior nivel adquirido durante backtracking`() {
        val influencia = def("Influência", listOf(1, 2), repeatable = true)
        val sobrenome = def("Sobrenome de Renome", listOf(2), preRequisitos = listOf("Influência 2+"))
        val livre = def("Livre", listOf(1, 3), repeatable = true)
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.SOCIAL,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(
            npc, listOf(influencia, sobrenome, livre), Random(91)
        )
        val influenciaMax = merits.filter { it.nome == "Influência" }.maxOfOrNull { it.valor } ?: 0
        if (merits.any { it.nome == "Sobrenome de Renome" }) {
            assertTrue(influenciaMax >= 2)
        }
        assertEquals(10, merits.sumOf { it.valor })
    }

    @Test
    fun `sangue de dragao continua excluindo sobrenaturais mesmo com pre requisitos`() {
        val catalogoComRequisitos = listOf(
            def("Bênção", listOf(1, 2, 3, 4), categoria = "sobrenatural"),
            def("Livre", listOf(1, 2, 3, 4), repeatable = true),
            def("Talento", listOf(1, 2, 3, 4), repeatable = true),
            def("Apoio", listOf(2, 3, 4), repeatable = true)
        )
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogoComRequisitos, Random(77))
        assertTrue(merits.none { it.categoria == "sobrenatural" })
        assertEquals(18, merits.sumOf { it.valor })
    }

    @Test
    fun `sangue de dragao funde meritos iguais entre pool normal e lista restrita e nunca passa de cinco`() {
        val catalogoDuplicado = listOf(
            def("Recursos", listOf(1, 3, 5)),
            def("Apoio", listOf(2, 4), repeatable = true),
            def("Comando", listOf(2, 3, 5), repeatable = true),
            def("Contatos", listOf(1, 3, 5), repeatable = true),
            def("Seguidores", listOf(1, 2, 3), repeatable = true),
            def("Influência", listOf(1, 2, 3, 4, 5), repeatable = true),
            def("Idioma", listOf(1), repeatable = true),
            def("Vassalos", listOf(2, 4), repeatable = true)
        )
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )

        repeat(EncounterTestSamples.count(100)) { seed ->
            val merits = EncounterMeritDistributionService.distribuir(npc, catalogoDuplicado, Random(seed))
            val agrupados = merits.groupBy { it.nome.trim().lowercase() }
            assertTrue(agrupados.filterKeys { it != "idioma" && it != "artefato" }.values.all { entradas -> entradas.size == 1 })
            assertTrue(merits.all { it.valor <= 5 })
            assertTrue(merits.filter { it.nome !in setOf("Artefato") }.all { it.valor <= 5 })
            assertEquals(18, merits.sumOf { it.valor })
        }
    }

    @Test
    fun `sangue de dragao funde recursos normal e restrito em uma unica entrada de quatro`() {
        val catalogoDuplicado = listOf(
            def("Recursos", listOf(1, 3, 5)),
            def("Apoio", listOf(2, 4), repeatable = true),
            def("Comando", listOf(2, 3, 5), repeatable = true),
            def("Contatos", listOf(1, 3, 5), repeatable = true),
            def("Seguidores", listOf(1, 2, 3), repeatable = true),
            def("Influência", listOf(1, 2, 3, 4, 5), repeatable = true),
            def("Vassalos", listOf(2, 4), repeatable = true)
        )
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )

        // Este teste isola a fusão entre os dois pools; Idioma possui uma regra
        // probabilística própria e, portanto, não deve consumir aleatoriedade
        // deste cenário determinístico. As regras de Idioma são cobertas pelos
        // testes específicos abaixo.
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogoDuplicado, Random(0))
        val recursos = merits.single { it.nome == "Recursos" }
        assertEquals(4, recursos.valor)
        assertEquals(1, merits.count { it.nome == "Recursos" })
        assertEquals(18, merits.sumOf { it.valor })
        assertTrue(merits.all { it.valor <= 5 })
    }

    private fun def(nome: String, custos: List<Int>, repeatable: Boolean = false, categoria: String = "normal", preRequisitos: List<String> = emptyList()) = MeritoDefinition(
        id = nome,
        nome = nome,
        categoria = categoria,
        tipo = "historia",
        custosPermitidos = custos,
        descricao = "",
        preRequisitos = preRequisitos,
        podeSerAdquiridoNovamente = repeatable
    )

    private fun arma(tipo: String) = ArmaEncontro("Arma", "Média", tipo, 0, 0, 0, 0)
    private fun armadura(tipo: String) = ArmaduraEncontro("Armadura", "Média", tipo, 0, 0, 0, 0)


    @Test
    fun `sangue de dragao do Imperio e de Lookshy nao pode adquirir Culto`() {
        val catalogoComCulto = catalogo + def("Culto", listOf(1, 2, 3, 4, 5), repeatable = true)
        val origens = listOf(
            OrigemNomeSangueDeDragao.IMPERIO,
            OrigemNomeSangueDeDragao.LOOKSHY
        )

        origens.forEach { origem ->
            repeat(50) { seed ->
                val npc = NpcEncontro(
                    tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
                    arquetipo = ArquetipoEncontro.SOCIAL,
                    arma = arma("Artefato"),
                    armadura = armadura("Artefato")
                )
                val merits = EncounterMeritDistributionService.distribuir(
                    npc, catalogoComCulto, Random(seed), origem
                )
                assertTrue(
                    "Culto não deveria ser adquirido para origem $origem",
                    merits.none { it.nome.equals("Culto", ignoreCase = true) }
                )
            }
        }
    }

    @Test
    fun `idioma automatico nunca e Antigo Reino`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.MENTAL,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogo, Random(1234))
        val idioma = merits.first { it.nome == "Idioma" }
        assertTrue(idioma.detalhe != "Antigo Reino")
    }

    @Test
    fun `idioma adicional de sangue de dragao nunca repete o idioma nativo`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.MENTAL,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        repeat(EncounterTestSamples.count(100)) { seed ->
            val merits = EncounterMeritDistributionService.distribuir(
                npc, catalogo, Random(seed), OrigemNomeSangueDeDragao.IMPERIO
            )
            val idiomas = merits.filter { it.nome.equals("Idioma", ignoreCase = true) }.map { it.detalhe.trim() }
            assertTrue("Deve existir pelo menos um idioma adicional", idiomas.isNotEmpty())
            assertTrue(
                "Idioma nativo não pode ser comprado como Mérito: $idiomas",
                idiomas.none { it.equals("Alto Reino", ignoreCase = true) }
            )
            assertEquals(idiomas.size, idiomas.distinctBy { it.lowercase() }.size)
        }
    }

    @Test
    fun `sangue de dragao do Imperio nunca compra Alto Reino como idioma adicional`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.MENTAL,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        repeat(EncounterTestSamples.count(100)) { seed ->
            val merits = EncounterMeritDistributionService.distribuir(
                npc, catalogo, Random(seed), OrigemNomeSangueDeDragao.IMPERIO
            )
            val idiomas = merits.filter { it.nome.equals("Idioma", ignoreCase = true) }.map { it.detalhe.trim() }
            assertTrue(idiomas.isNotEmpty())
            assertTrue(
                "Alto Reino é o idioma nativo do Império e não pode aparecer como Mérito: $idiomas",
                idiomas.none { it.equals("Alto Reino", ignoreCase = true) }
            )
        }
    }

    @Test
    fun `sangue de dragao de Lookshy nunca compra Dialeto dos Rios como idioma adicional`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SANGUE_DE_DRAGAO,
            arquetipo = ArquetipoEncontro.MENTAL,
            arma = arma("Artefato"),
            armadura = armadura("Artefato")
        )
        val merits = EncounterMeritDistributionService.distribuir(
            npc, catalogo, Random(4321), OrigemNomeSangueDeDragao.LOOKSHY
        )
        val idiomas = merits.filter { it.nome.equals("Idioma", ignoreCase = true) }.map { it.detalhe.trim() }
        assertTrue(idiomas.isNotEmpty())
        assertTrue(
            "Dialeto dos Rios é o idioma nativo de Lookshy e não pode aparecer como Mérito: $idiomas",
            idiomas.none { it.equals("Dialeto dos Rios", ignoreCase = true) }
        )
    }

    @Test
    fun `armadura de seda usa quatro pontos de merito no artefato`() {
        val armaduraSeda = ArmaduraEncontro(
            nome = "Armadura de Seda (Leve)",
            peso = "Leve",
            tipo = "Artefato",
            absorcao = 0,
            dureza = 0,
            penalidadeMobilidade = 0,
            motesComitados = 0,
            custoMeritoArtefato = 4
        )
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = arma("Artefato"),
            armadura = armaduraSeda
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogo, Random(1))
        assertEquals(4, merits.single { it.nome == "Artefato" && it.detalhe == "Armadura de Seda (Leve)" }.valor)
        assertEquals(10, merits.sumOf { it.valor })
    }

    @Test
    fun `artefatos exibem o nome real do item no detalhe`() {
        val npc = NpcEncontro(
            tipoExaltado = TipoExaltadoEncontro.SOLAR,
            arquetipo = ArquetipoEncontro.FISICO,
            arma = ArmaEncontro("Lança do Crepúsculo", "Pesada", "Artefato", 0, 0, 0),
            armadura = ArmaduraEncontro("Peitoral Reforçado", "Pesada", "Artefato", 0, 0, 0, 0)
        )
        val merits = EncounterMeritDistributionService.distribuir(npc, catalogo, Random(2))
        assertTrue(merits.any { it.nome == "Artefato" && it.detalhe == "Lança do Crepúsculo" })
        assertTrue(merits.any { it.nome == "Artefato" && it.detalhe == "Peitoral Reforçado" })
        assertTrue(merits.none { it.nome == "Artefato (arma)" || it.nome == "Artefato (armadura)" })
    }

}

