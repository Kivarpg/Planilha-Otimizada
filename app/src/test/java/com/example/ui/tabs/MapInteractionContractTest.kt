package com.example.ui.tabs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MapInteractionContractTest {
    private val source = File("src/main/java/com/example/ui/tabs/MapTab.kt").readText()
    private val tabs = File("src/main/java/com/example/ui/SheetTabs.kt").readText()
    private val viewModel = File("src/main/java/com/example/viewmodel/SheetViewModel.kt").readText()

    @Test fun `map stays fixed at one x and pans after zoom`() {
        val start = source.indexOf("val transformState = rememberTransformableState")
        val end = source.indexOf("// Em tela cheia", start)
        val block = source.substring(start, end)
        assertTrue(block.contains("zoomChange, panChange"))
        assertTrue(block.contains("if (newScale > 1f)"))
        assertTrue(block.contains("panChange.x / fittedDrawW"))
        assertTrue(block.contains("panChange.y / fittedDrawH"))
        assertTrue(block.contains("onNormalizedOffsetChange(Offset.Zero)"))
    }

    @Test fun `map zoom range remains calibrated`() {
        assertTrue(source.contains("coerceIn(1f, 8f)"))
        assertTrue(source.contains("MAP_INTRINSIC_WIDTH_PX"))
        assertTrue(source.contains("MAP_INTRINSIC_HEIGHT_PX"))
    }
    @Test fun `map route survives tab navigation until explicitly cleared`() {
        assertTrue(viewModel.contains("val mapRoutePoints = androidx.compose.runtime.mutableStateListOf"))
        assertTrue(tabs.contains("routePoints = viewModel.mapRoutePoints"))
        assertTrue(source.contains("routePoints: SnapshotStateList<Offset>"))
        assertTrue(source.contains("routePoints.clear()"))
    }
    @Test fun `route distance tracks mutable points without copying list per recomposition`() {
        assertTrue(source.contains("remember(routePoints)"))
        assertTrue(source.contains("derivedStateOf { mapRouteDistanceKm(routePoints) }"))
        assertTrue(!source.contains("remember(routePoints.toList())"))
    }
    @Test fun `route line does not grow with map zoom`() {
        assertTrue(source.contains("width = 3.dp.toPx() / effectiveScale"))
        assertTrue(source.contains("markerLayerScale = markerVisualScale / effectiveScale"))
    }
}
