package com.example.data

import org.junit.Assert.*
import org.junit.Test

class LunarCharmArchetypePolicyTest {
    private fun charm(
        nome: String = "Teste",
        atributo: String = "Destreza",
        min: Int = 3,
        pre: String = "Nenhum",
        routes: List<LunarCharmArchetypeRoute> = emptyList()
    ) = EncantoLunarDefinition(
        id = nome, atributo = atributo, subdivisao = null, nome = nome, nomeIngles = "",
        custo = "", minsTexto = "$atributo $min, Essência 1", minAtributo = min,
        minEssencia = 1, tipo = "Reflexivo", palavrasChave = "", duracao = "",
        preRequisitos = pre, descricao = "", rotasArquetipo = routes
    )

    @Test
    fun `indice de dependencias conta encantos distintos sem alterar elegibilidade`() {
        val catalogo = listOf(
            charm(nome = "Raiz"),
            charm(nome = "Ramo A", pre = "Raiz"),
            charm(nome = "Ramo B", pre = "Raiz"),
            charm(nome = "Folha", pre = "Ramo A")
        )
        val context = LunarCharmArchetypePolicy.prepare(catalogo, emptySet())
        assertEquals(2, context.directCharmDependentCount("Raiz"))
        assertEquals(1, context.directCharmDependentCount("Ramo A"))
        assertEquals(0, context.directCharmDependentCount("Folha"))
        assertEquals(0, context.directCharmDependentCount("Inexistente"))
        assertEquals(3, context.transitiveCharmDependentCount("Raiz"))
        assertEquals(1, context.transitiveCharmDependentCount("Ramo A"))
        assertEquals(0, context.transitiveCharmDependentCount("Folha"))
        assertEquals(0, context.transitiveCharmDependentCount("Inexistente"))
        val essencias = mapOf("Raiz" to 1, "Ramo A" to 2, "Ramo B" to 3, "Folha" to 5)
        assertEquals(5, context.highestDependentEssence("Raiz", essencias))
        assertEquals(5, context.highestDependentEssence("Ramo A", essencias))
        assertEquals(null, context.highestDependentEssence("Folha", essencias))
    }

    @Test
    fun `alcance transitivo nao duplica caminhos convergentes nem entra em ciclo`() {
        val catalogo = listOf(
            charm(nome = "Raiz", pre = "Folha"),
            charm(nome = "Ramo A", pre = "Raiz"),
            charm(nome = "Ramo B", pre = "Raiz"),
            charm(nome = "Folha", pre = "Ramo A ou Ramo B")
        )
        val context = LunarCharmArchetypePolicy.prepare(catalogo, emptySet())
        assertEquals(3, context.transitiveCharmDependentCount("Raiz"))
        assertEquals(3, context.transitiveCharmDependentCount("Folha"))
        assertEquals(1, context.directCharmDependentCount("Ramo A"))
        assertEquals(1, context.directCharmDependentCount("Ramo B"))
    }

    @Test
    fun `rota arquetipo so existe quando forma satisfaz condicao`() {
        val def = charm(routes = listOf(
            LunarCharmArchetypeRoute("Percepção", "VISAO_NOTURNA", 3, "Nenhum")
        ))
        assertEquals(1, LunarCharmArchetypePolicy.routes(def, emptySet()).size)
        val routes = LunarCharmArchetypePolicy.routes(def, setOf(LunarSpiritTrait.VISAO_NOTURNA))
        assertEquals(listOf("Destreza", "Percepção"), routes.map { it.atributo })
    }

    @Test
    fun `arquetipo usa atributo alternativo para legalidade sem mudar original`() {
        val def = charm(min = 4, routes = listOf(
            LunarCharmArchetypeRoute("Percepção", "VISAO_NOTURNA", 4, "Nenhum")
        ))
        val attrs = mapOf("Destreza" to 2, "Percepção" to 4)
        val eligible = LunarCharmArchetypePolicy.eligibleRoutes(
            def, attrs, 1, emptySet(), listOf(def), setOf(LunarSpiritTrait.VISAO_NOTURNA)
        )
        assertEquals(listOf("Percepção"), eligible.map { it.atributo })
        assertEquals("Destreza", def.atributo)
    }

    @Test
    fun `presa minuscula exige as duas caracteristicas`() {
        val def = charm(routes = listOf(
            LunarCharmArchetypeRoute("Aparência", "PRESA_E_MINUSCULO", 3, "Nenhum")
        ))
        assertEquals(1, LunarCharmArchetypePolicy.routes(def, setOf(LunarSpiritTrait.PRESA)).size)
        assertEquals(1, LunarCharmArchetypePolicy.routes(def, setOf(LunarSpiritTrait.MINUSCULO)).size)
        assertEquals(2, LunarCharmArchetypePolicy.routes(
            def, setOf(LunarSpiritTrait.PRESA, LunarSpiritTrait.MINUSCULO)
        ).size)
    }

    @Test
    fun `taxonomia cobre exatamente as 600 formas auditadas`() {
        assertEquals(600, LunarSpiritShapeArchetypeTraits.auditedAnimalCount)
    }
    @Test
    fun `contexto preparado preserva exatamente as rotas habilitadas`() {
        val normal = charm(nome = "Normal")
        val arquetipo = charm(nome = "Arquetipo", routes = listOf(
            LunarCharmArchetypeRoute("Percepção", "VISAO_NOTURNA", 3, "Nenhum")
        ))
        val catalogo = listOf(normal, arquetipo)
        val traits = setOf(LunarSpiritTrait.VISAO_NOTURNA)
        val contexto = LunarCharmArchetypePolicy.prepare(catalogo, traits)

        catalogo.forEach { def ->
            assertEquals(
                LunarCharmArchetypePolicy.routes(def, traits),
                contexto.routesFor(def)
            )
        }
        assertTrue(arquetipo in contexto.charmsByAttribute["Percepção"].orEmpty())
        assertTrue(normal !in contexto.charmsByAttribute["Percepção"].orEmpty())
    }

    @Test
    fun `hot path booleano preserva elegibilidade das rotas preparadas`() {
        val base = charm(nome = "Base", atributo = "Destreza", min = 2)
        val dependente = charm(
            nome = "Dependente",
            atributo = "Destreza",
            min = 3,
            pre = "Base",
            routes = listOf(LunarCharmArchetypeRoute("Percepção", "VISAO_NOTURNA", 3, "Base"))
        )
        val catalogo = listOf(base, dependente)
        val contexto = LunarCharmArchetypePolicy.prepare(catalogo, setOf(LunarSpiritTrait.VISAO_NOTURNA))
        val atributos = mapOf("Destreza" to 3, "Percepção" to 3)

        for (selecionados in listOf(emptySet(), setOf("Base"))) {
            val lista = LunarCharmArchetypePolicy.eligibleRoutes(
                dependente, contexto, atributos, 1, selecionados, catalogo
            )
            val booleano = LunarCharmArchetypePolicy.hasEligibleRoute(
                dependente, contexto, atributos, 1, selecionados, catalogo
            )
            assertEquals(lista.isNotEmpty(), booleano)
        }
    }

    @Test
    fun `contexto compila prerequisitos uma vez sem mudar semantica`() {
        val base = charm(nome = "Base", atributo = "Destreza", min = 2)
        val alternativa = charm(nome = "Alternativa", atributo = "Percepção", min = 2)
        val alvo = charm(nome = "Alvo", atributo = "Destreza", min = 2, pre = "Base ou Alternativa")
        val catalogo = listOf(base, alternativa, alvo)
        val contexto = LunarCharmArchetypePolicy.prepare(catalogo, emptySet())
        val rota = contexto.routesFor(alvo).single()

        assertTrue(rota.requisitosCompilados.isNotEmpty())
        assertFalse(LunarCharmArchetypePolicy.hasEligibleRoute(alvo, contexto, mapOf("Destreza" to 3), 1, emptySet(), catalogo))
        assertTrue(LunarCharmArchetypePolicy.hasEligibleRoute(alvo, contexto, mapOf("Destreza" to 3), 1, setOf("Alternativa"), catalogo))
    }

    @Test
    fun `indice reverso limita delta aos encantos realmente afetados`() {
        val base = charm(nome = "Base", atributo = "Destreza", min = 2)
        val dependente = charm(nome = "Dependente", atributo = "Destreza", min = 2, pre = "Base")
        val independente = charm(nome = "Independente", atributo = "Percepção", min = 2)
        val catalogo = listOf(base, dependente, independente)
        val contexto = LunarCharmArchetypePolicy.prepare(catalogo, emptySet())

        val afetados = contexto.affectedAfterAcquisition("Base", "Destreza")
        assertTrue("Dependente" in afetados)
        assertFalse("Independente" in afetados)
    }


    @Test
    fun `indice reverso inclui dependente de contagem da categoria adquirida`() {
        val base = charm(nome = "Base", atributo = "Destreza", min = 2)
        val alvo = charm(
            nome = "Alvo Categoria",
            atributo = "Destreza",
            min = 2,
            pre = "Quaisquer dois Encantos de Destreza"
        )
        val contexto = LunarCharmArchetypePolicy.prepare(listOf(base, alvo), emptySet())

        val afetados = contexto.affectedAfterAcquisition("Base", "Destreza")
        assertTrue("Alvo Categoria" in afetados)
    }

    @Test
    fun `indice reverso inclui dependente de contagem total apos qualquer aquisicao`() {
        val base = charm(nome = "Base", atributo = "Destreza", min = 2)
        val alvo = charm(
            nome = "Alvo Total",
            atributo = "Percepção",
            min = 2,
            pre = "Quaisquer dois Encantos"
        )
        val contexto = LunarCharmArchetypePolicy.prepare(listOf(base, alvo), emptySet())

        val afetados = contexto.affectedAfterAcquisition("Base", "Destreza")
        assertTrue("Alvo Total" in afetados)
    }

}
