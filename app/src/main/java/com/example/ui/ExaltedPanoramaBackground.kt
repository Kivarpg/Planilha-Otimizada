package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.CharacterType
import com.example.ui.components.OnyxTexturedBackground
import kotlin.math.roundToInt

/**
 * Fundo panorâmico único para todas as abas da planilha.
 * O início corresponde à aba 1 e o fim à última aba.
 * A imagem não acompanha a rolagem vertical do conteúdo.
 *
 * Recursos opcionais: drawable-nodpi/panorama_solar.webp,
 * panorama_dragao.webp e panorama_lunar.webp.
 * Se um recurso não estiver presente, mantém o fundo Onyx existente.
 */
@Composable
internal fun ExaltedPanoramaBackground(
    tipoPersonagem: String,
    selectedTabIndex: Int,
    tabCount: Int,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resourceName = when (tipoPersonagem) {
        CharacterType.DRAGON_BLOODED -> "panorama_dragao"
        CharacterType.LUNAR -> "panorama_lunar"
        else -> "panorama_solar"
    }
    val resourceId = remember(context, resourceName) {
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
    if (resourceId == 0) {
        OnyxTexturedBackground(Modifier.fillMaxSize(), content)
        return
    }
    val progress by animateFloatAsState(
        targetValue = (selectedTabIndex.toFloat() / (tabCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "panorama-tab-position"
    )
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val width = maxWidth
        // Cinco larguras de tela: revela a paisagem progressivamente
        // sem mover o cenário durante a rolagem vertical.
        val panoramaWidth = width * 5
        val density = androidx.compose.ui.platform.LocalDensity.current
        val travelPx = with(density) { (panoramaWidth - width).toPx() }
        Image(
            painter = painterResource(resourceId),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .width(panoramaWidth)
                .fillMaxHeight()
                .offset { IntOffset((-travelPx * progress).roundToInt(), 0) }
                .align(Alignment.TopStart)
        )
        // Camada de contraste para preservar a leitura dos controles.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f)))
        content()
    }
}
