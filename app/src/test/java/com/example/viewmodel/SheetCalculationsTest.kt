package com.example.viewmodel

import com.example.model.CharacterSheet
import com.example.model.CharacterType
import org.junit.Assert.assertEquals
import org.junit.Test

class SheetCalculationsTest {
    @Test fun essenciaSolarUsaXpGasto() {
        assertEquals(1, SheetCalculations.essenceFromXp(0))
        assertEquals(2, SheetCalculations.essenceFromXp(50))
        assertEquals(3, SheetCalculations.essenceFromXp(125))
        assertEquals(4, SheetCalculations.essenceFromXp(200))
        assertEquals(5, SheetCalculations.essenceFromXp(300))
    }

    @Test fun essenciaSangueDeDragaoUsaXpGasto() {
        val base = CharacterSheet(tipoPersonagem = CharacterType.DRAGON_BLOODED, essencia = 2)
        assertEquals(2, SheetCalculations.normalizeEssence(base.copy(experienciaGastaTotal = 0)).essencia)
        assertEquals(3, SheetCalculations.normalizeEssence(base.copy(experienciaGastaTotal = 75)).essencia)
        assertEquals(4, SheetCalculations.normalizeEssence(base.copy(experienciaGastaTotal = 250)).essencia)
        assertEquals(5, SheetCalculations.normalizeEssence(base.copy(experienciaGastaTotal = 325)).essencia)
    }
}
