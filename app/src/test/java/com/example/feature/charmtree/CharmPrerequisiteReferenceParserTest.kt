package com.example.feature.charmtree

import com.example.model.Encanto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharmPrerequisiteReferenceParserTest {
    @Test
    fun resolvesCharmNameContainingCommaWithoutSplittingIt() {
        val catalog = listOf(
            CharmTreeEntry("1", "Golpe Excelente"),
            CharmTreeEntry("2", "Uma Arma, Dois Golpes"),
            CharmTreeEntry("3", "Técnica da Flor de Peônia")
        )

        val result = CharmPrerequisiteReferenceParser.resolve(
            "Uma Arma, Dois Golpes",
            catalog
        )

        assertEquals(listOf("2"), result.prerequisiteIds)
        assertEquals(null, result.unresolvedText)
    }

    @Test
    fun resolvesMultiplePrerequisitesFromNaturalJsonText() {
        val catalog = listOf(
            CharmTreeEntry("a", "Encanto A"),
            CharmTreeEntry("b", "Encanto B"),
            CharmTreeEntry("c", "Encanto C")
        )

        val result = CharmPrerequisiteReferenceParser.resolve(
            "Encanto A; Encanto B e Encanto C",
            catalog
        )

        assertEquals(listOf("a", "b", "c"), result.prerequisiteIds)
    }

    @Test
    fun ignoresGenericQuantityRequirementsWithoutInventingTreeEdges() {
        val result = CharmPrerequisiteReferenceParser.resolve(
            "Quaisquer quatro Encantamentos de Ocultismo",
            listOf(CharmTreeEntry("a", "Encanto A"))
        )

        assertTrue(result.prerequisiteIds.isEmpty())
        assertEquals(null, result.unresolvedText)
    }

    @Test
    fun doesNotSilentlyChooseBetweenDuplicateCharmNames() {
        val result = CharmPrerequisiteReferenceParser.resolve(
            "Encanto Repetido",
            listOf(
                CharmTreeEntry("a", "Encanto Repetido"),
                CharmTreeEntry("b", "Encanto Repetido")
            )
        )

        assertTrue(result.prerequisiteIds.isEmpty())
        assertEquals("Encanto Repetido", result.unresolvedText)
    }

    @Test
    fun mappingUsesStableJsonIdsAndResolvesPrerequisitesByName() {
        val charms = listOf(
            Encanto(
                id = "json-root",
                nome = "Encanto Dependente",
                habilidadeVinculada = "Armas brancas",
                preRequisitos = "Uma Arma, Dois Golpes"
            ),
            Encanto(
                id = "json-prereq",
                nome = "Uma Arma, Dois Golpes",
                habilidadeVinculada = "Armas brancas",
                preRequisitos = "Nenhum"
            )
        )

        val entries = charms.paraArvoreDePreRequisitos()
        assertEquals(listOf("json-prereq"), entries.first().prerequisiteIds)
        assertEquals("json-root", entries.first().id)
        assertEquals("json-prereq", entries[1].id)
    }
    @Test
    fun doesNotResolveCharmNameEmbeddedInsideAnotherWord() {
        val catalog = listOf(CharmTreeEntry("a", "Sol"))
        val result = CharmPrerequisiteReferenceParser.resolve("Consolidação", catalog)

        assertTrue(result.prerequisiteIds.isEmpty())
        assertEquals("Consolidação", result.unresolvedText)
    }

    @Test
    fun overlappingNamesPreferLongestMatchAndPreserveTextOrder() {
        val catalog = listOf(
            CharmTreeEntry("short", "Golpe"),
            CharmTreeEntry("long", "Golpe Perfeito"),
            CharmTreeEntry("other", "Defesa")
        )
        val result = CharmPrerequisiteReferenceParser.resolve(
            "Defesa e Golpe Perfeito; Golpe",
            catalog
        )

        assertEquals(listOf("other", "long", "short"), result.prerequisiteIds)
    }

    @Test
    fun repeatedPrerequisiteDoesNotCreateDuplicateTreeEdges() {
        val catalog = listOf(CharmTreeEntry("a", "Golpe Perfeito"))
        val result = CharmPrerequisiteReferenceParser.resolve(
            "Golpe Perfeito; Golpe Perfeito",
            catalog
        )

        assertEquals(listOf("a"), result.prerequisiteIds)
    }

    @Test
    fun punctuationDelimitsCompleteCharmNames() {
        val catalog = listOf(
            CharmTreeEntry("a", "Defesa"),
            CharmTreeEntry("b", "Golpe")
        )
        val result = CharmPrerequisiteReferenceParser.resolve(
            "(Defesa), [Golpe]",
            catalog
        )

        assertEquals(listOf("a", "b"), result.prerequisiteIds)
    }

}
