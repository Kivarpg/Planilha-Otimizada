package com.example.data

import com.example.model.BattleGroup
import com.example.model.BattleGroupCustomAttack
import com.example.model.BattleGroupCustomStats
import com.example.model.BattleGroupDrill
import com.example.model.CharacterSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BattleGroupCharacterSheetPersistenceTest {
    private fun group(id: String, name: String) = BattleGroup(
        id = id,
        name = name,
        troopTypeName = null,
        size = 4,
        drill = BattleGroupDrill.ELITE,
        might = 3,
        customStats = BattleGroupCustomStats(
            joinBattle = 8,
            attacks = listOf(BattleGroupCustomAttack("Teste", 7, 12, 2)),
            defenseBase = 4,
            magnitudeBase = 9,
            soakBase = 6,
            senses = 5,
            resolve = 3,
            resist = "2",
            routDifficulty = null,
            perfectMorale = true
        )
    )

    @Test
    fun `battle groups are part of character sheet round trip`() {
        val original = CharacterSheet(id = "sheet-a", battleGroups = listOf(group("bg-a", "Exército A")))
        val restored = CharacterSheetJsonCodec.decode(CharacterSheetJsonCodec.encode(original))
        // O codec da planilha possui normalizações próprias de campos não relacionados
        // a Battle Groups. Este contrato deve validar integralmente o domínio que
        // pretende proteger, sem acoplar o teste a essas normalizações.
        assertEquals(original.battleGroups, restored.battleGroups)
        assertEquals(null, restored.battleGroups.single().troopTypeName)
        assertNotEquals("null", restored.battleGroups.single().troopTypeName)
        assertEquals("bg-a", restored.battleGroups.single().id)
        assertEquals(2, restored.battleGroups.single().customStats!!.attacks.single().minimumDice)
    }

    @Test
    fun `old sheets without battle groups remain compatible`() {
        val restored = CharacterSheetJsonCodec.decode("{\"id\":\"old\",\"nome\":\"Antiga\"}")
        assertEquals("old", restored.id)
        assertTrue(restored.battleGroups.isEmpty())
    }

    @Test
    fun `two sheets keep independent battle group lists`() {
        val a = CharacterSheet(id = "a", battleGroups = listOf(group("bg-a", "A")))
        val b = CharacterSheet(id = "b", battleGroups = listOf(group("bg-b", "B")))
        val restoredA = CharacterSheetJsonCodec.decode(CharacterSheetJsonCodec.encode(a))
        val restoredB = CharacterSheetJsonCodec.decode(CharacterSheetJsonCodec.encode(b))
        assertNotEquals(restoredA.battleGroups, restoredB.battleGroups)
        assertEquals("A", restoredA.battleGroups.single().name)
        assertEquals("B", restoredB.battleGroups.single().name)
    }
}
