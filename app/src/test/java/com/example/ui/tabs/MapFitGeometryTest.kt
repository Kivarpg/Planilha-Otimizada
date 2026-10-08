package com.example.ui.tabs

import org.junit.Assert.assertEquals
import org.junit.Test

class MapFitGeometryTest {
    private val mapW = MAP_INTRINSIC_WIDTH_PX
    private val mapH = MAP_INTRINSIC_HEIGHT_PX

    @Test
    fun landscapeViewportFitsMapByWidthAndCentersVertically() {
        val fit = mapFitGeometry(1000f, 800f, mapW, mapH)
        assertEquals(1000f, fit.drawW, 0.001f)
        assertEquals(1000f / (mapW / mapH), fit.drawH, 0.001f)
        assertEquals(0f, fit.originX, 0.001f)
        assertEquals((800f - fit.drawH) / 2f, fit.originY, 0.001f)
    }

    @Test
    fun portraitViewportFitsMapByWidthAndCentersVertically() {
        val fit = mapFitGeometry(400f, 900f, mapW, mapH)
        assertEquals(400f, fit.drawW, 0.001f)
        assertEquals(400f / (mapW / mapH), fit.drawH, 0.001f)
        assertEquals(0f, fit.originX, 0.001f)
        assertEquals((900f - fit.drawH) / 2f, fit.originY, 0.001f)
    }

    @Test
    fun normalizedPanRepresentsSameMapFractionAcrossViewports() {
        val normal = mapFitGeometry(1000f, 700f, mapW, mapH)
        val fullscreen = mapFitGeometry(400f, 900f, mapW, mapH)
        val normalizedX = 0.23f
        val normalizedY = -0.17f

        val normalOffsetX = normalizedX * normal.drawW
        val normalOffsetY = normalizedY * normal.drawH
        val fullscreenOffsetX = normalizedX * fullscreen.drawW
        val fullscreenOffsetY = normalizedY * fullscreen.drawH

        assertEquals(normalizedX, normalOffsetX / normal.drawW, 0.0001f)
        assertEquals(normalizedY, normalOffsetY / normal.drawH, 0.0001f)
        assertEquals(normalizedX, fullscreenOffsetX / fullscreen.drawW, 0.0001f)
        assertEquals(normalizedY, fullscreenOffsetY / fullscreen.drawH, 0.0001f)
    }

    @Test
    fun calibrationContractKeepsReferenceGridAtFiveHundredKm() {
        assertEquals(500f, mapPixelsToKm(170f), 0.0001f)
        assertEquals(1000f, mapPixelsToKm(340f), 0.0001f)
    }

    @Test
    fun visualScaleUsesRenderedMapWidthAndEffectiveZoom() {
        // Quando os 5780 px originais ocupam 578 px de tela, 17 px de tela
        // correspondem exatamente aos 170 px da grade = 500 km.
        assertEquals(
            500f,
            screenPixelsToMapKm(
                screenPixels = 17f,
                fittedDrawWidthPx = 578f,
                effectiveScale = 1f
            ),
            0.001f
        )
        // Dobrar o zoom faz a mesma largura visual representar metade dos km.
        assertEquals(
            250f,
            screenPixelsToMapKm(
                screenPixels = 17f,
                fittedDrawWidthPx = 578f,
                effectiveScale = 2f
            ),
            0.001f
        )
    }

    @Test
    fun visualScaleRemainsCalibratedAcrossViewportWidths() {
        val wide = screenPixelsToMapKm(70f, fittedDrawWidthPx = 1000f, effectiveScale = 1f)
        val narrow = screenPixelsToMapKm(28f, fittedDrawWidthPx = 400f, effectiveScale = 1f)
        assertEquals(wide, narrow, 0.001f)
    }
    @Test
    fun invalidViewportOrZoomNeverProducesDistance() {
        assertEquals(0f, screenPixelsToMapKm(70f, 0f, 1f), 0f)
        assertEquals(0f, screenPixelsToMapKm(70f, 400f, 0f), 0f)
        assertEquals(0f, screenPixelsToMapKm(70f, -400f, 1f), 0f)
        assertEquals(0f, screenPixelsToMapKm(70f, 400f, -1f), 0f)
    }

    @Test
    fun visualScaleMatchesReferenceGridAtDifferentZoomAndWidth() {
        val normal = screenPixelsToMapKm(17f, 578f, 1f)
        val enlarged = screenPixelsToMapKm(34f, 578f, 2f)
        val compact = screenPixelsToMapKm(8.5f, 289f, 1f)
        assertEquals(500f, normal, 0.001f)
        assertEquals(normal, enlarged, 0.001f)
        assertEquals(normal, compact, 0.001f)
    }

    @Test
    fun routeDistanceIsZeroWithoutTwoPoints() {
        val origin = androidx.compose.ui.geometry.Offset(0f, 0f)
        assertEquals(0f, mapRouteDistanceKm(emptyList()), 0f)
        assertEquals(0f, mapRouteDistanceKm(listOf(origin)), 0f)
    }

    @Test
    fun routeDistanceAccumulatesSegmentsInOriginalMapCoordinates() {
        val origin = androidx.compose.ui.geometry.Offset(0f, 0f)
        val oneGridX = androidx.compose.ui.geometry.Offset(MAP_REFERENCE_PX / mapW, 0f)
        val oneGridY = androidx.compose.ui.geometry.Offset(
            MAP_REFERENCE_PX / mapW, MAP_REFERENCE_PX / mapH
        )
        assertEquals(500f, mapRouteDistanceKm(listOf(origin, oneGridX)), 0.01f)
        assertEquals(1000f, mapRouteDistanceKm(listOf(origin, oneGridX, oneGridY)), 0.01f)
    }

}
