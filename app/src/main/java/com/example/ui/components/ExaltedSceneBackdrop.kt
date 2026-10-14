package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.isDragonBlooded
import com.example.model.isLunar

/**
 * Cenários LEO aprovados no piloto 947, recuperados sem modificar os PNGs.
 *
 * Componente exclusivamente decorativo: deve ser colocado como primeiro
 * filho de um Box, com Modifier.matchParentSize(). Não intercepta toques,
 * não altera o layout e nunca define o tamanho de outros componentes.
 */
@Composable
fun ExaltedSceneBackdrop(
    exaltedType: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    val drawable = when {
        exaltedType.isDragonBlooded() -> R.drawable.leo_scene_dragon
        exaltedType.isLunar() -> R.drawable.leo_scene_lunar
        else -> R.drawable.leo_scene_solar
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = modifier.background(Color.Black),
        // Cobre todo o viewport sem deformar a pintura; recorta apenas bordas.
        contentScale = ContentScale.Crop,
        alignment = alignment,
    )
}
