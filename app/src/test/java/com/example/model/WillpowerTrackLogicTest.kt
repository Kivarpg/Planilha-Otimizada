package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WillpowerTrackLogicTest {
    @Test
    fun clickingEmptyBoxMarksFirstAvailableBox() {
        assertEquals(setOf(1, 2, 3), WillpowerTrackLogic.toggle(setOf(1, 2), 5, 5))
    }

    @Test
    fun clickingMarkedBoxRemovesLastMarkedBox() {
        assertEquals(setOf(1, 2), WillpowerTrackLogic.toggle(setOf(1, 2, 3), 5, 1))
    }

    @Test
    fun clickingLastMarkedBoxAlsoRemovesOnlyThatBox() {
        assertEquals(setOf(1, 2), WillpowerTrackLogic.toggle(setOf(1, 2, 3), 5, 3))
    }

    @Test
    fun clickingRightmostEmptyBoxMarksFirstAvailableBox() {
        assertEquals(setOf(1), WillpowerTrackLogic.toggle(emptySet(), 5, 5))
        assertEquals(setOf(1, 2), WillpowerTrackLogic.toggle(setOf(1), 5, 5))
    }

    @Test
    fun clickingWhenAllBoxesAreFullRemovesLastMarkedBox() {
        assertEquals(setOf(1, 2, 3, 4), WillpowerTrackLogic.toggle(setOf(1, 2, 3, 4, 5), 5, 5))
    }

    @Test
    fun clickingInvalidIndexDoesNothing() {
        val usados = setOf(1, 2)
        assertEquals(usados, WillpowerTrackLogic.toggle(usados, 5, 0))
        assertEquals(usados, WillpowerTrackLogic.toggle(usados, 5, 6))
    }
}
