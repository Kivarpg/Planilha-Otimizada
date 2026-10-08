package com.example.ui.tabs

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterGeneratorArchetypeSwitchContractTest {
    @Test
    fun `selecionar arquetipo substitui foco sem exigir limpar`() {
        val source = sequenceOf(
            File("src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt"),
            File("app/src/main/java/com/example/ui/tabs/EncounterGeneratorTab.kt")
        ).firstOrNull { it.exists() }?.readText()
            ?: error("EncounterGeneratorTab.kt não encontrado")

        val marker = "ARQUETIPO_OPCOES.forEach"
        val start = source.indexOf(marker)
        assertTrue("Bloco de arquétipos não encontrado", start >= 0)
        val block = source.substring(start, minOf(source.length, start + 2_500))

        val foco = block.indexOf("customFoco = null")
        val ataque = block.indexOf("customAtaque = null")
        val defesa = block.indexOf("customDefesa = null")
        val secundaria = block.indexOf("customSecundaria = null")
        val troca = block.indexOf("onArquetipoChange(arq)")

        assertTrue(foco >= 0)
        assertTrue(ataque > foco)
        assertTrue(defesa > ataque)
        assertTrue(secundaria > defesa)
        assertTrue("O estado de Foco deve ser limpo antes da troca de arquétipo", troca > secundaria)
    }
}
