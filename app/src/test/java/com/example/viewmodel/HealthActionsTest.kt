package com.example.viewmodel

import com.example.model.CaixaVitalidade
import com.example.model.CharacterSheet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthActionsTest {
    @Test
    fun limparTrilhaDesmarcaTodasAsCaixas() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val state = MutableStateFlow(
            CharacterSheet(
                healthBoxes = listOf(
                    CaixaVitalidade(id = "a", penalidade = "-0", tipoDano = 1),
                    CaixaVitalidade(id = "b", penalidade = "-1", tipoDano = 2),
                    CaixaVitalidade(id = "c", penalidade = "-2", tipoDano = 3)
                )
            )
        )
        val actions = HealthActions(state, scope)

        actions.clearHealthDamage()

        assertEquals(listOf(0, 0, 0), state.value.healthBoxes.map { it.tipoDano })
    }
}
