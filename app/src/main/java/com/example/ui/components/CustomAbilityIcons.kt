package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.PathBuilder

/**
 * Ícones vetoriais da skin "Gerador de Personagens Harmônico".
 * Todos com peso visual igual, legíveis em tamanho pequeno,
 * traço limpo e performance mínima (poucos paths).
 */
private fun abilityVector(name: String, draw: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 1f,
        pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero
    ) {
        draw()
    }.build()

/** Arte Marcial — dois punhos fechados, um de cada lado, com uma pequena
 * marca de impacto entre eles. Reaproveita o desenho do punho de Briga em
 * escala reduzida (dois lados), diferenciando-se por essa dualidade — não
 * por um traço totalmente novo, conforme a especificação. */
val AbilityMartialArt: ImageVector = abilityVector("AbilityMartialArt") {
    // Punho esquerdo
    moveTo(3.2f, 8.5f)
    lineTo(3.2f, 14.5f)
    curveTo(3.2f, 16f, 4.3f, 17.1f, 5.8f, 17.1f)
    lineTo(7f, 17.1f)
    curveTo(8.5f, 17.1f, 9.6f, 16f, 9.6f, 14.5f)
    lineTo(9.6f, 9.2f)
    moveTo(3.9f, 10.4f)
    lineTo(8.9f, 10.4f)
    moveTo(3.9f, 12.1f)
    lineTo(8.9f, 12.1f)
    moveTo(3.2f, 10.8f)
    curveTo(2.1f, 10.8f, 1.6f, 11.8f, 1.9f, 12.8f)
    curveTo(2.2f, 13.5f, 2.9f, 13.8f, 3.5f, 13.7f)

    // Punho direito (espelhado)
    moveTo(20.8f, 8.5f)
    lineTo(20.8f, 14.5f)
    curveTo(20.8f, 16f, 19.7f, 17.1f, 18.2f, 17.1f)
    lineTo(17f, 17.1f)
    curveTo(15.5f, 17.1f, 14.4f, 16f, 14.4f, 14.5f)
    lineTo(14.4f, 9.2f)
    moveTo(20.1f, 10.4f)
    lineTo(15.1f, 10.4f)
    moveTo(20.1f, 12.1f)
    lineTo(15.1f, 12.1f)
    moveTo(20.8f, 10.8f)
    curveTo(21.9f, 10.8f, 22.4f, 11.8f, 22.1f, 12.8f)
    curveTo(21.8f, 13.5f, 21.1f, 13.8f, 20.5f, 13.7f)

    // Marca de impacto central (pequena estrela de linhas)
    moveTo(12f, 5.5f)
    lineTo(12f, 8.5f)
    moveTo(10.7f, 6.2f)
    lineTo(13.3f, 7.8f)
    moveTo(13.3f, 6.2f)
    lineTo(10.7f, 7.8f)
}
