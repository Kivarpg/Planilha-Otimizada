package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class EncounterRecentUiRegressionContractTest {
    private val card = File("src/main/java/com/example/ui/tabs/EncounterNpcCard.kt").readText()
    private val generator = File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt").readText()

    @Test fun `initiative keeps Combat tab three block layout`() {
        val start = card.indexOf("AppText(\"Controle de Iniciativa\"")
        val end = card.indexOf("NpcCardJuntarSeDialog(", start)
        val section = card.substring(start, end)
        assertTrue(section.contains("horizontalArrangement = Arrangement.SpaceEvenly"))
        assertTrue(section.contains("NPC_DAMAGE_DELTAS_NEGATIVE.forEach"))
        assertTrue(section.contains("NPC_DAMAGE_DELTAS_POSITIVE.forEach"))
        assertTrue(section.contains("width = 88.dp"))
        assertTrue(section.contains("Modifier.width(84.dp)"))
        assertTrue(!section.contains("horizontalScroll("))
    }

    @Test fun `xp numeric display remains emphasized without deforming row`() {
        val start = card.indexOf("LinhaInfo(\n                \"Ganhos -")
        val end = card.indexOf("\n            )", start)
        val line = card.substring(start, end)
        assertTrue(line.contains("(npc.xpAtual + npc.xpGastoTotal).toString()"))
        assertTrue(line.contains("sufixo = \"XP\""))
        assertTrue(line.contains("valueWidth = 52.dp"))
        assertTrue(line.contains("valueTextStyle = MaterialTheme.typography.bodyLarge.copy("))
        assertTrue(line.contains("fontSize = 16.sp"))
        assertTrue(line.contains("lineHeight = 20.sp"))
        assertTrue(line.contains("visualTemplate = visualTemplate"))
    }

    @Test fun `action statistic rows remain bound to npc palette`() {
        val start = card.indexOf("EncounterCardTitle(\"Ações\"")
        val end = card.indexOf("// Mantém os recursos de motes", start)
        val section = card.substring(start, end)
        val calls = Regex("LinhaInfo\\(").findAll(section).count()
        val bindings = Regex("visualTemplate = visualTemplate").findAll(section).count()
        assertTrue(calls > 0)
        assertTrue(bindings >= calls)
    }

    @Test fun `compact encounter choices preserve two column policy`() {
        val archetypeStart = generator.indexOf("AppText(\"3. Arquétipo\"")
        val archetypeEnd = generator.indexOf("if (tipoSangueDeDragao)", archetypeStart)
        val archetype = generator.substring(archetypeStart, archetypeEnd)
        assertTrue(archetype.contains("maxItemsInEachRow = 2"))

        val typeStart = generator.indexOf("// Tipo de Exaltado")
        val typeEnd = generator.indexOf("AppText(\"2. Gênero\"", typeStart)
        val type = generator.substring(typeStart, typeEnd)
        assertTrue(type.contains("val compactTypeButtons = maxWidth < 720.dp"))
        assertTrue(type.contains("TipoButton(TipoExaltadoEncontro.SOLAR, \"Solar\", Modifier.weight(1f))"))
        assertTrue(type.contains("TipoButton(TipoExaltadoEncontro.SANGUE_DE_DRAGAO, \"Sangue de Dragão\", Modifier.weight(1f))"))
        assertTrue(type.contains("TipoButton(TipoExaltadoEncontro.LUNAR, \"Lunar\", Modifier.fillMaxWidth(0.5f))"))
    }
    @Test fun `encounter button groups preserve cardinality layout policy`() {
        val xpStart = card.indexOf("// Botões de XP")
        val xpEnd = card.indexOf("// XP movido pra cá", xpStart)
        val xp = card.substring(xpStart, xpEnd)
        assertTrue(xp.contains("Row("))
        assertTrue(xp.contains("Modifier.weight(1f)"))
        assertTrue(!xp.contains("Column("))

        val actionStart = card.indexOf("val compactActions")
        val actionEnd = card.indexOf("// Exportar / Salvar / Carregar: exceção explícita", actionStart)
        val actions = card.substring(actionStart, actionEnd)
        assertTrue(actions.contains("Row("))
        assertTrue(!actions.contains("FlowRow("))
        assertTrue(actions.contains("Arrangement.SpaceEvenly"))

        val archetypeStart = generator.indexOf("AppText(\"3. Arquétipo\"")
        val archetypeEnd = generator.indexOf("if (tipoSangueDeDragao)", archetypeStart)
        val archetype = generator.substring(archetypeStart, archetypeEnd)
        assertTrue(archetype.contains("maxItemsInEachRow = 2"))
    }

    @Test fun `charm drawers distinguish headings and number entries`() {
        val start = card.indexOf("val encantosPorHabilidade")
        val end = card.indexOf("// Feitiços", start).takeIf { it > start }
            ?: card.indexOf("val feiticosDisponiveis", start)
        val section = card.substring(start, end)
        assertTrue(section.contains("color = visualTemplate.accentBright"))
        assertTrue(section.contains("forEachIndexed { indice, grupo ->"))
        assertTrue(section.contains("val nomeExibicao = \"\${indice + 1}. \${formatGroupedEncounterCharmName(grupo)}\""))
        assertTrue(section.contains("color = visualTemplate.onSurface"))
        assertTrue(!section.contains("val nomeExibicao = \"• "))
    }


}
