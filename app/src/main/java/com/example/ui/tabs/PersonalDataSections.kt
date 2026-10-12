package com.example.ui.tabs
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
internal fun AspectSelectionSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    val visual = LocalExaltedVisualTemplate.current
    if (!sheet.tipoPersonagem.isDragonBlooded()) return
    val aspectoAtual = Aspecto.entries.firstOrNull { it.displayName == sheet.aspecto }
    LeoSection(leoMotif(sheet.tipoPersonagem), title = BoxNames.PersonalData.ASPECT, illustrated = true) {
        LeoEmblemGrid(Aspecto.entries.toList(), columns = 3) { aspecto, cardModifier ->
            val imagem = when (aspecto) {
                Aspecto.Ar -> com.example.R.drawable.aspecto_ar
                Aspecto.Terra -> com.example.R.drawable.aspecto_terra
                Aspecto.Fogo -> com.example.R.drawable.aspecto_fogo
                Aspecto.Agua -> com.example.R.drawable.aspecto_agua
                Aspecto.Madeira -> com.example.R.drawable.aspecto_madeira
            }
            LeoEmblemCard(aspecto.displayName, imagem, ExaltedVisualMotif.DRAGON_BLOODED,
                selected = aspecto == aspectoAtual,
                onClick = { viewModel.updateAspecto(aspecto) }, modifier = cardModifier)
        }
        aspectoAtual?.let { aspecto ->
            Spacer(Modifier.height(18.dp))
            AppText(BoxNames.PersonalData.ASPECT_ABILITY, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = visual.accentBright,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(5.dp))
            AppText(aspecto.allowedAbilities().joinToString("  •  "), style = MaterialTheme.typography.bodySmall,
                color = visual.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun CasteSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    val visual = LocalExaltedVisualTemplate.current
    if (sheet.tipoPersonagem.isDragonBlooded()) return
    if (sheet.tipoPersonagem.isLunar()) {
        LunarCasteSection(sheet, viewModel)
        return
    }
    LeoSection(ExaltedVisualMotif.SOLAR, title = BoxNames.PersonalData.CASTE, illustrated = true) {
        LeoEmblemGrid(listOf(Casta.Dawn, Casta.Zenith, Casta.Twilight, Casta.Night, Casta.Eclipse), columns = 3) { caste, cardModifier ->
            val imagem = when (caste) {
                Casta.Dawn -> com.example.R.drawable.caste_dawn
                Casta.Zenith -> com.example.R.drawable.caste_zenith
                Casta.Twilight -> com.example.R.drawable.caste_twilight
                Casta.Night -> com.example.R.drawable.caste_night
                Casta.Eclipse -> com.example.R.drawable.caste_eclipse
            }
            LeoEmblemCard(caste.displayName, imagem, ExaltedVisualMotif.SOLAR,
                selected = sheet.castaEscolhida && sheet.casta == caste,
                onClick = { viewModel.updateCasta(caste) }, modifier = cardModifier)
        }
        Spacer(Modifier.height(18.dp))
        if (!sheet.castaEscolhida) {
            AppText("Toque num símbolo de Casta acima para ver suas habilidades.",
                style = MaterialTheme.typography.bodyMedium, color = visual.muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            AppText("Habilidades de ${sheet.casta.displayName}", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = visual.accentBright,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(5.dp))
            AppText(sheet.casta.allowedAbilities().joinToString("  •  "), style = MaterialTheme.typography.bodySmall,
                color = visual.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun LunarCasteSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    val visual = LocalExaltedVisualTemplate.current
    LeoSection(ExaltedVisualMotif.LUNAR, title = BoxNames.PersonalData.CASTE, illustrated = true) {
        LeoEmblemGrid(listOf(LunarCasta.FullMoon, LunarCasta.ChangingMoon, LunarCasta.NoMoon, LunarCasta.Casteless), columns = 2) { caste, cardModifier ->
            val imagem = when (caste) {
                LunarCasta.FullMoon -> com.example.R.drawable.caste_lua_cheia
                LunarCasta.ChangingMoon -> com.example.R.drawable.caste_lua_minguante
                LunarCasta.NoMoon -> com.example.R.drawable.caste_lua_nova
                LunarCasta.Casteless -> com.example.R.drawable.caste_sem_casta
            }
            LeoEmblemCard(caste.displayName, imagem, ExaltedVisualMotif.LUNAR,
                selected = sheet.lunarCastaEscolhida && sheet.lunarCasta == caste,
                onClick = { viewModel.updateLunarCasta(caste) }, modifier = cardModifier)
        }
        Spacer(Modifier.height(18.dp))
        if (!sheet.lunarCastaEscolhida) {
            AppText("Toque num símbolo de Casta acima para ver seus atributos de casta.",
                style = MaterialTheme.typography.bodyMedium, color = visual.muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            val atributos = sheet.lunarCasta.poolAtributosCasta()
            AppText("Atributos disponíveis para ${sheet.lunarCasta.displayName} (escolha 2 na Casta)",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = visual.accentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            if (atributos.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                AppText(atributos.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                    color = visual.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun LanguageSection(sheet: CharacterSheet, onOpen: () -> Unit) {
    val visual = LocalExaltedVisualTemplate.current
    LeoSection(leoMotif(sheet.tipoPersonagem), title = BoxNames.PersonalData.LANGUAGE) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally) {
            val resumoLinguas=buildString { append(sheet.linguaNativa?.let{"Idioma nativo: $it"} ?: "Idioma nativo: não definido"); if(sheet.linguasAdicionais.isNotEmpty()){append("  •  Adicionais: ");append(sheet.linguasAdicionais.joinToString(", "))} }
            AppText(resumoLinguas,style=MaterialTheme.typography.bodySmall,color=visual.onSurface,textAlign=TextAlign.Center,modifier=Modifier.padding(bottom=8.dp))
            InkButton(styleOverride = InkButtonStyle(gradientStart = visual.surface, gradientMiddle = visual.surface, gradientEnd = visual.surfaceVariant, labelColor = visual.accentBright, outlineColor = visual.accentBright), label=BoxNames.PersonalData.LANGUAGE, selected=true, onClick=onOpen, modifier=Modifier.fillMaxWidth(), fillMaxWidth=true, brushIndex=0)
        }
    }
}

@Composable
internal fun IntimaciesSection(sheet: CharacterSheet, onOpenAdd: () -> Unit, onDelete: (Intimidade) -> Unit) {
    val visual = LocalExaltedVisualTemplate.current
    LeoSection(leoMotif(sheet.tipoPersonagem), title = BoxNames.PersonalData.INTIMACIES) {
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            InkButton(styleOverride = InkButtonStyle(gradientStart = visual.surface, gradientMiddle = visual.surface, gradientEnd = visual.surfaceVariant, labelColor = visual.accentBright, outlineColor = visual.accentBright), label="Intimidade", selected=true, onClick=onOpenAdd, modifier=Modifier.fillMaxWidth(), fillMaxWidth=true, brushIndex=1)
        }
    }
    Spacer(Modifier.height(7.dp))
    if(sheet.intimacies.isNotEmpty()) sheet.intimacies.forEach { intm ->
        LongPressCard(onLongClick={onDelete(intm)},modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    MedallionIcon(size=34.dp){Icon(Icons.Outlined.FavoriteBorder,contentDescription=null,tint=visual.accentBright,modifier=Modifier.size(17.dp))}
                    Column(Modifier.weight(1f)) { AppText(intm.nome,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=visual.onSurface); Spacer(Modifier.height(4.dp)); Row { Surface(color=visual.accent.copy(alpha=.2f),shape=RoundedCornerShape(4.dp)){AppText(intm.tipo,style=MaterialTheme.typography.labelSmall,color=visual.accent,modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp))}; Spacer(Modifier.width(8.dp)); Surface(color=visual.accentBright.copy(alpha=.2f),shape=RoundedCornerShape(4.dp)){AppText(intm.intensidade,style=MaterialTheme.typography.labelSmall,color=visual.accentBright,modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp))} } }
                }
            }
        }
    }
}
