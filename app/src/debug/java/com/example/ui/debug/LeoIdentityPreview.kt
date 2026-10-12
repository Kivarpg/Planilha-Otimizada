package com.example.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Aspecto
import com.example.model.Casta
import com.example.model.LunarCasta
import com.example.ui.components.LeoEmblemCard
import com.example.ui.components.LeoEmblemGrid
import com.example.ui.components.LeoSection
import com.example.ui.theme.ExaltedTheme
import com.example.ui.theme.ExaltedVisualMotif
import com.example.ui.theme.ProvideExaltedVisualTemplate
import com.example.ui.theme.visualTemplatePorMotif

/** Real Compose previews, not executed by the HTML/SVG design-board renderer. */
@Composable
private fun LeoSelectionPreview(motif: ExaltedVisualMotif) {
    val choices = when (motif) {
        ExaltedVisualMotif.SOLAR -> listOf(
            Casta.Dawn.displayName to R.drawable.caste_dawn,
            Casta.Zenith.displayName to R.drawable.caste_zenith,
            Casta.Twilight.displayName to R.drawable.caste_twilight,
            Casta.Night.displayName to R.drawable.caste_night,
            Casta.Eclipse.displayName to R.drawable.caste_eclipse,
        )
        ExaltedVisualMotif.DRAGON_BLOODED -> listOf(
            Aspecto.Ar.displayName to R.drawable.aspecto_ar,
            Aspecto.Terra.displayName to R.drawable.aspecto_terra,
            Aspecto.Fogo.displayName to R.drawable.aspecto_fogo,
            Aspecto.Agua.displayName to R.drawable.aspecto_agua,
            Aspecto.Madeira.displayName to R.drawable.aspecto_madeira,
        )
        ExaltedVisualMotif.LUNAR -> listOf(
            LunarCasta.FullMoon.displayName to R.drawable.caste_lua_cheia,
            LunarCasta.ChangingMoon.displayName to R.drawable.caste_lua_minguante,
            LunarCasta.NoMoon.displayName to R.drawable.caste_lua_nova,
            LunarCasta.Casteless.displayName to R.drawable.caste_sem_casta,
        )
    }
    var selected by remember(motif) { mutableStateOf(choices.first().first) }
    ExaltedTheme {
        ProvideExaltedVisualTemplate(visualTemplatePorMotif(motif)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                LeoSection(motif, illustrated = true, title = if (motif == ExaltedVisualMotif.DRAGON_BLOODED) "Aspecto" else "Casta") {
                    LeoEmblemGrid(choices, columns = if (motif == ExaltedVisualMotif.LUNAR) 2 else 3) { choice, cardModifier ->
                        LeoEmblemCard(choice.first, choice.second, motif, choice.first == selected,
                            onClick = { selected = choice.first }, modifier = cardModifier)
                    }
                }
            }
        }
    }
}

@Preview(name = "LEO Solar 320 large text", widthDp = 320, heightDp = 740, fontScale = 1.5f)
@Preview(name = "LEO Solar 390", widthDp = 390, heightDp = 500)
@Preview(name = "LEO Solar 840", widthDp = 840, heightDp = 500)
@Composable
private fun LeoSolarPreview() = LeoSelectionPreview(ExaltedVisualMotif.SOLAR)

@Preview(name = "LEO Dragon 320 large text", widthDp = 320, heightDp = 740, fontScale = 1.5f)
@Preview(name = "LEO Dragon 390", widthDp = 390, heightDp = 500)
@Preview(name = "LEO Dragon 840", widthDp = 840, heightDp = 500)
@Composable
private fun LeoDragonPreview() = LeoSelectionPreview(ExaltedVisualMotif.DRAGON_BLOODED)

@Preview(name = "LEO Lunar 320 large text", widthDp = 320, heightDp = 650, fontScale = 1.5f)
@Preview(name = "LEO Lunar 390", widthDp = 390, heightDp = 550)
@Preview(name = "LEO Lunar 840", widthDp = 840, heightDp = 550)
@Composable
private fun LeoLunarPreview() = LeoSelectionPreview(ExaltedVisualMotif.LUNAR)
