package com.example.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.model.TipoExaltadoEncontro
import com.example.ui.components.AppText
import com.example.ui.components.GildedCard
import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButtonVariant
import com.example.ui.theme.ExaltedTheme
import com.example.ui.theme.visualTemplateParaExaltado

/**
 * Laboratório visual de debug. Não entra no APK release.
 *
 * A matriz abaixo mantém InkButton e GildedCard isolados em três Tipos de
 * Exaltado e duas larguras/fontScale de telefone. O objetivo é permitir
 * inspeção rápida de contraste, recorte e centralização antes de alterar
 * qualquer uma das 15 abas.
 */
@Composable
private fun InkButtonDiagnostic(type: TipoExaltadoEncontro) {
    val visual = visualTemplateParaExaltado(type)
    ExaltedTheme {
        Surface(color = visual.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AppText(
                    text = type.name,
                    color = visual.accentBright,
                    modifier = Modifier.fillMaxWidth()
                )
                InkButton(
                    label = "Gerar NPC",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    fillMaxWidth = true,
                    size = InkButtonSize.Medium,
                    variant = InkButtonVariant.Primary,
                    visualTemplate = visual
                )
                InkButton(
                    label = "Gerenciar Feitiço inicial",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    fillMaxWidth = true,
                    size = InkButtonSize.Small,
                    variant = InkButtonVariant.Secondary,
                    visualTemplate = visual
                )
            }
        }
    }
}

@Composable
private fun GildedCardDiagnostic(type: TipoExaltadoEncontro) {
    val visual = visualTemplateParaExaltado(type)
    ExaltedTheme {
        Surface(color = visual.surface) {
            GildedCard(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                visualTemplate = visual
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    AppText(
                        text = "Encantos",
                        color = visual.accentBright,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppText(
                        text = "Título da gaveta",
                        color = visual.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    AppText(
                        text = "Conteúdo legível sobre a superfície do cartão.",
                        color = visual.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "Ink Solar 320", widthDp = 320, heightDp = 190, fontScale = 1.3f, showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Ink Solar 411", widthDp = 411, heightDp = 190, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewInkSolar() = InkButtonDiagnostic(TipoExaltadoEncontro.SOLAR)

@Preview(name = "Ink Dragão 320", widthDp = 320, heightDp = 190, fontScale = 1.3f, showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Ink Dragão 411", widthDp = 411, heightDp = 190, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewInkDragonBlooded() = InkButtonDiagnostic(TipoExaltadoEncontro.SANGUE_DE_DRAGAO)

@Preview(name = "Ink Lunar 320", widthDp = 320, heightDp = 190, fontScale = 1.3f, showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Ink Lunar 411", widthDp = 411, heightDp = 190, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewInkLunar() = InkButtonDiagnostic(TipoExaltadoEncontro.LUNAR)

@Preview(name = "Card Solar 320", widthDp = 320, heightDp = 190, fontScale = 1.5f, showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Card Solar 411", widthDp = 411, heightDp = 190, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewCardSolar() = GildedCardDiagnostic(TipoExaltadoEncontro.SOLAR)

@Preview(name = "Card Dragão 320", widthDp = 320, heightDp = 190, fontScale = 1.5f, showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Card Dragão 411", widthDp = 411, heightDp = 190, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewCardDragonBlooded() = GildedCardDiagnostic(TipoExaltadoEncontro.SANGUE_DE_DRAGAO)

@Preview(name = "Card Lunar 320", widthDp = 320, heightDp = 190, fontScale = 1.5f, showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Card Lunar 411", widthDp = 411, heightDp = 190, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewCardLunar() = GildedCardDiagnostic(TipoExaltadoEncontro.LUNAR)
