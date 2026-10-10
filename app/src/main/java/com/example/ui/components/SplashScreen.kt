package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash animada:
 * 1. 14 emblemas convergem ao centro (queda em linha reta, escalonada).
 * 2. Clarão breve.
 * 3. Logo final (img_000) emerge e permanece um instante.
 *
 * Problema anterior: a tela ia preto → branco → app, sem os emblemas.
 * Causas corrigidas:
 * - distância fixa em px empurrava ícones para fora de telas pequenas;
 * - leitura de Animatable sem asState confiável em alguns caminhos;
 * - clarão branco cobria a cena antes dos ícones serem perceptíveis;
 * - flash ia a alpha 1.0 (tela inteira branca).
 */
private const val N = 14
private const val ATRASO_ENTRE_ICONES_MS = 350L
private const val DURACAO_QUEDA_MS = 700

@Composable
fun SplashScreen(
    isAppReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fallingDrawables = remember {
        listOf(
            R.drawable.img_001, R.drawable.img_002, R.drawable.img_003,
            R.drawable.img_004, R.drawable.img_005, R.drawable.img_006,
            R.drawable.img_007, R.drawable.img_008, R.drawable.img_009,
            R.drawable.img_010, R.drawable.img_011, R.drawable.img_012,
            R.drawable.img_013, R.drawable.img_014
        )
    }

    val finalImageScale = remember { Animatable(0.6f) }
    val finalImageAlpha = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    var animacaoConcluida by remember { mutableStateOf(false) }
    var mostrarLogo by remember { mutableStateOf(false) }
    var iconeAtivo by remember { mutableStateOf(-1) }
    var pularAnimacao by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Quedas escalonadas — cada ícone de uma direção.
        repeat(N) { index ->
            if (index > 0) delay(ATRASO_ENTRE_ICONES_MS)
            // Só publica o índice no instante em que a queda realmente começa.
            // Antes disso o símbolo nem participa da composição.
            iconeAtivo = index
        }
        delay(DURACAO_QUEDA_MS.toLong())
        // Espera a última queda terminar.
        delay(80L)

        // Clarão curto e incompleto (não “lava” a tela de branco puro).
        flashAlpha.animateTo(0.75f, tween(180, easing = LinearEasing))
        mostrarLogo = true
        coroutineScope {
            launch {
                finalImageAlpha.animateTo(1f, tween(450))
            }
            launch {
                finalImageScale.animateTo(
                    1f,
                    tween(550, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(120)
                flashAlpha.animateTo(0f, tween(400, easing = LinearEasing))
            }
        }
        delay(700)
        animacaoConcluida = true
    }

    LaunchedEffect(animacaoConcluida, isAppReady, pularAnimacao) {
        if (pularAnimacao && isAppReady) {
            onFinished()
        } else if (animacaoConcluida && isAppReady) {
            delay(500)
            onFinished()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { pularAnimacao = true },
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        // Distância proporcional à tela — garante que o ícone parta de
        // dentro/perto da borda e seja sempre visível durante a queda.
        val distanciaPx = with(density) {
            min(maxWidth.toPx(), maxHeight.toPx()) * 0.40f
        }
        val iconSize = 72.dp

        // Camada dos emblemas em queda.
        fallingDrawables.forEachIndexed { index, drawableId ->
            // Observa o valor a cada frame (Animatable notifica o snapshot).
            val progress by animateFloatAsState(targetValue = if (index <= iconeAtivo) 1f else 0f, animationSpec = tween(durationMillis = DURACAO_QUEDA_MS, easing = FastOutSlowInEasing), label = "splashIconProgress$index")
            // Mantém cada símbolo totalmente oculto até o instante em que sua queda começa.
            if (index <= iconeAtivo && progress < 1f) {
                val angleRad = (index * (360.0 / N)) * (Math.PI / 180.0)
                val restante = distanciaPx * (1f - progress)
                val offsetX = (cos(angleRad) * restante).toFloat()
                val offsetY = (sin(angleRad) * restante).toFloat()
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize)
                            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                            .graphicsLayer {
                                alpha = 1f
                                // Leve escala ao aproximar do centro.
                                val s = 0.75f + 0.35f * progress
                                scaleX = s
                                scaleY = s
                            }
                    )
                }
            }

        // Logo final.
        if (mostrarLogo) {
            Image(
                painter = painterResource(id = R.drawable.img_000),
                contentDescription = "Logo Principal",
                modifier = Modifier
                    .size(168.dp)
                    .scale(finalImageScale.value)
                    .alpha(finalImageAlpha.value)
            )
        }

        // Clarão (por cima, mas nunca opaco demais).
        if (flashAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }
    }
}
