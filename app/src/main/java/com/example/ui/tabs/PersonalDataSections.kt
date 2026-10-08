package com.example.ui.tabs
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SheetViewModel

@Composable
internal fun AspectSelectionSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    if (!sheet.tipoPersonagem.isDragonBlooded()) return
    SectionHeader(title = BoxNames.PersonalData.ASPECT)
    val aspectoAtual = Aspecto.entries.firstOrNull { it.displayName == sheet.aspecto }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(Aspecto.Ar, Aspecto.Terra, Aspecto.Fogo).forEach { ItemAspecto(it, aspectoAtual, viewModel) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(Aspecto.Agua, Aspecto.Madeira).forEach { ItemAspecto(it, aspectoAtual, viewModel) }
        }
    }
    Spacer(Modifier.height(5.dp))
    aspectoAtual?.let { aspecto ->
        GildedCard(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppText(BoxNames.PersonalData.ASPECT_ABILITY, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ExaltedGold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(5.dp))
                AppText(aspecto.allowedAbilities().joinToString("  •  "), style = MaterialTheme.typography.bodySmall, color = ExaltedOnSurface, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ItemAspecto(aspecto: Aspecto, aspectoAtual: Aspecto?, viewModel: SheetViewModel, modifier: Modifier = Modifier) {
    val selecionado = aspecto == aspectoAtual
    val imagem = when (aspecto) {
        Aspecto.Ar -> com.example.R.drawable.aspecto_ar
        Aspecto.Terra -> com.example.R.drawable.aspecto_terra
        Aspecto.Fogo -> com.example.R.drawable.aspecto_fogo
        Aspecto.Agua -> com.example.R.drawable.aspecto_agua
        Aspecto.Madeira -> com.example.R.drawable.aspecto_madeira
    }
    // Mesmo padrão visual em caixa do Solar/Lunar — pedido explícito do
    // usuário: fundo, borda em gradiente e largura fixa, em vez de só o
    // ícone circular "flutuando" sem moldura. A borda usa as mesmas
    // variáveis reativas de paleta (ficam vermelhas pro Sangue de Dragão
    // automaticamente, sem precisar de cor fixa).
    Column(
        modifier = modifier
            .width(102.dp)
            .padding(5.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.verticalGradient(
                    listOf(
                        ExaltedDarkSurface,
                        if (selecionado) ExaltedAccentBright.copy(alpha = 0.10f) else ExaltedDarkBackground,
                        ExaltedDarkSurface
                    )
                )
            )
            .border(
                width = if (selecionado) 2.5.dp else 1.5.dp,
                brush = if (selecionado) Brush.linearGradient(
                    listOf(ExaltedStructuralMetalDeep, ExaltedAccentBright, ExaltedStructuralMetalShine, ExaltedAccentBright, ExaltedStructuralMetalDeep)
                ) else Brush.linearGradient(
                    listOf(ExaltedOutline, ExaltedStructuralMetal, ExaltedOutline)
                ),
                shape = MaterialTheme.shapes.medium
            )
            .feedbackClickable { viewModel.updateAspecto(aspecto) }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(imagem),
            contentDescription = aspecto.displayName,
            modifier = Modifier.size(com.example.ui.theme.Dimens.CasteAspectIconSize).clip(CircleShape),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(Modifier.height(7.dp))
        AppText(
            aspecto.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = ExaltedAccentBright,
            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun CasteSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    if (sheet.tipoPersonagem.isDragonBlooded()) return
    if (sheet.tipoPersonagem.isLunar()) {
        LunarCasteSection(sheet, viewModel)
        return
    }
    SectionHeader(title = BoxNames.PersonalData.CASTE)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(Casta.Dawn, Casta.Zenith, Casta.Twilight).forEach { c -> CasteEmblemButton(caste=c,isSelected=sheet.castaEscolhida && sheet.casta==c,onClick={viewModel.updateCasta(c)}) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(Casta.Night, Casta.Eclipse).forEach { c -> CasteEmblemButton(caste=c,isSelected=sheet.castaEscolhida && sheet.casta==c,onClick={viewModel.updateCasta(c)}) }
        }
    }
    Spacer(Modifier.height(5.dp))
    if (!sheet.castaEscolhida) AppText("Toque num símbolo de Casta acima para ver suas habilidades.", style=MaterialTheme.typography.bodyMedium, color=ExaltedMuted, textAlign=TextAlign.Center)
    else GildedCard(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=ExaltedDarkSurfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            AppText("Habilidades de ${sheet.casta.displayName}", style=MaterialTheme.typography.titleSmall, fontWeight=FontWeight.Bold, color=ExaltedGold, textAlign=TextAlign.Center)
            Spacer(Modifier.height(5.dp)); AppText(sheet.casta.allowedAbilities().joinToString("  •  "), style=MaterialTheme.typography.bodySmall, color=ExaltedOnSurface, textAlign=TextAlign.Center)
        }
    }
}

/**
 * Seção de Casta Lunar — 4 emblemas de lua (Lua Cheia, Lua Minguante, Lua
 * Nova, Sem Casta) e, ao selecionar, os Atributos de Casta (não
 * Habilidades). Os nomes em inglês (Full Moon, Changing Moon, No Moon,
 * Casteless) ficam no modelo (LunarCasta.englishName) — não exibidos
 * ainda, preparados pra futura opção de idioma.
 */
@Composable
internal fun LunarCasteSection(sheet: CharacterSheet, viewModel: SheetViewModel) {
    SectionHeader(title = BoxNames.PersonalData.CASTE)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(com.example.model.LunarCasta.FullMoon, com.example.model.LunarCasta.ChangingMoon).forEach { c ->
                com.example.ui.components.LunarCasteEmblemButton(caste=c, isSelected=sheet.lunarCastaEscolhida && sheet.lunarCasta==c, onClick={viewModel.updateLunarCasta(c)})
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(com.example.model.LunarCasta.NoMoon, com.example.model.LunarCasta.Casteless).forEach { c ->
                com.example.ui.components.LunarCasteEmblemButton(caste=c, isSelected=sheet.lunarCastaEscolhida && sheet.lunarCasta==c, onClick={viewModel.updateLunarCasta(c)})
            }
        }
    }
    Spacer(Modifier.height(5.dp))
    if (!sheet.lunarCastaEscolhida) {
        AppText("Toque num símbolo de Casta acima para ver seus atributos de casta.", style=MaterialTheme.typography.bodyMedium, color=ExaltedMuted, textAlign=TextAlign.Center)
    } else {
        val atributos = sheet.lunarCasta.poolAtributosCasta()
        GildedCard(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=ExaltedDarkSurfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                AppText(
                    "Atributos disponíveis para ${sheet.lunarCasta.displayName} (escolha 2 na Casta)",
                    style=MaterialTheme.typography.titleSmall, fontWeight=FontWeight.Bold, color=ExaltedGold, textAlign=TextAlign.Center
                )
                if (atributos.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    AppText(atributos.joinToString(", "), style=MaterialTheme.typography.bodySmall, color=ExaltedOnSurface, textAlign=TextAlign.Center)
                }
            }
        }
    }
}

@Composable
internal fun LanguageSection(sheet: CharacterSheet, onOpen: () -> Unit) {
    SectionHeader(title=BoxNames.PersonalData.LANGUAGE)
    GildedCard(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=ExaltedDarkSurfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            val resumoLinguas=buildString { append(sheet.linguaNativa?.let{"Idioma nativo: $it"} ?: "Idioma nativo: não definido"); if(sheet.linguasAdicionais.isNotEmpty()){append("  •  Adicionais: ");append(sheet.linguasAdicionais.joinToString(", "))} }
            AppText(resumoLinguas,style=MaterialTheme.typography.bodySmall,color=ExaltedOnSurface,textAlign=TextAlign.Center,modifier=Modifier.padding(bottom=8.dp))
            InkButton(label=BoxNames.PersonalData.LANGUAGE, selected=true, onClick=onOpen, modifier=Modifier.fillMaxWidth(), fillMaxWidth=true, brushIndex=0)
        }
    }
}

@Composable
internal fun IntimaciesSection(sheet: CharacterSheet, onOpenAdd: () -> Unit, onDelete: (Intimidade) -> Unit) {
    SectionHeader(title=BoxNames.PersonalData.INTIMACIES)
    GildedCard(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=ExaltedDarkSurfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            InkButton(label="Intimidade", selected=true, onClick=onOpenAdd, modifier=Modifier.fillMaxWidth(), fillMaxWidth=true, brushIndex=1)
        }
    }
    Spacer(Modifier.height(7.dp))
    if(sheet.intimacies.isNotEmpty()) sheet.intimacies.forEach { intm ->
        LongPressCard(onLongClick={onDelete(intm)},modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    MedallionIcon(size=34.dp){Icon(Icons.Outlined.FavoriteBorder,contentDescription=null,tint=ExaltedAccentBright,modifier=Modifier.size(17.dp))}
                    Column(Modifier.weight(1f)) { AppText(intm.nome,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(4.dp)); Row { Surface(color=ExaltedAmber.copy(alpha=.2f),shape=RoundedCornerShape(4.dp)){AppText(intm.tipo,style=MaterialTheme.typography.labelSmall,color=ExaltedAmber,modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp))}; Spacer(Modifier.width(8.dp)); Surface(color=ExaltedGold.copy(alpha=.2f),shape=RoundedCornerShape(4.dp)){AppText(intm.intensidade,style=MaterialTheme.typography.labelSmall,color=ExaltedGold,modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp))} } }
                }
            }
        }
    }
}
