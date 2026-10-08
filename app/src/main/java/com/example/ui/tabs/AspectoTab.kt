package com.example.ui.tabs
import com.example.ui.components.exaltedContentStage

import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Aspecto
import com.example.model.CharacterSheet
import com.example.model.ExaltedConstants
import com.example.ui.components.ErgonomicMultiWordButton
import com.example.ui.components.GildedCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedBlack
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.viewmodel.SheetViewModel

// Aba 2 do template Sangue de Dragão — reformulada (pedido explícito do
// usuário) pra espelhar exatamente o padrão da contraparte Solar
// (CasteTab.kt): a troca de Aspecto acontece só na Aba 1 agora (Dados
// Pessoais); aqui só mostra o Aspecto já escolhido (somente leitura) e
// permite escolher 5 habilidades ADICIONAIS, além das 5 fixas do
// Aspecto — reaproveitando favoredAbilities/toggleFavoredAbility, que já
// existem no modelo e no ViewModel pro equivalente Solar (mesma
// mecânica de limite de 5, sem duplicar lógica nova).
@OptIn(ExperimentalLayoutApi::class)
// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
@Composable
fun AspectoTab(
    sheet: CharacterSheet,
    viewModel: SheetViewModel,
    modifier: Modifier = Modifier
) {
    val aspectoAtual = Aspecto.entries.firstOrNull { it.displayName == sheet.aspecto }

    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(2).exaltedContentStage(2)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Aspecto selecionado")

        if (aspectoAtual == null) {
            AppText(
                "Aspecto não selecionado",
                style = MaterialTheme.typography.bodyMedium,
                color = ExaltedMuted,
                textAlign = TextAlign.Center
            )
            return@Column
        }

        val imagem = when (aspectoAtual) {
            Aspecto.Ar -> R.drawable.aspecto_ar
            Aspecto.Terra -> R.drawable.aspecto_terra
            Aspecto.Fogo -> R.drawable.aspecto_fogo
            Aspecto.Agua -> R.drawable.aspecto_agua
            Aspecto.Madeira -> R.drawable.aspecto_madeira
        }
        Image(
            painter = painterResource(id = imagem),
            contentDescription = aspectoAtual.displayName,
            modifier = Modifier
                .width(com.example.ui.theme.Dimens.CasteAspectIconSize * 2)
                .height(com.example.ui.theme.Dimens.CasteAspectIconSize * 2)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        AppText(
            text = aspectoAtual.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ExaltedAccentBright,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        com.example.ui.components.ExpandableTextCard(
            titulo = "Poder da Anima",
            texto = com.example.data.AnimaDescriptions.paraAspecto(aspectoAtual),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        val habilidadesRestantes = androidx.compose.runtime.remember(aspectoAtual) {
            val habilidadesDoAspecto = aspectoAtual.allowedAbilities().toHashSet()
            ExaltedConstants.ALL_25_ABILITIES.filter { it !in habilidadesDoAspecto }
        }
        SectionHeader(title = "Habilidades Favorecidas (${sheet.favoredAbilities.size}/5)")

        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2),
            colors = CardDefaults.cardColors(containerColor = ExaltedBlack)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                habilidadesRestantes.forEach { ab ->
                    val isSelected = sheet.favoredAbilities.contains(ab)
                    ErgonomicMultiWordButton(
                        text = ab,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleFavoredAbility(ab) },
                        modifier = Modifier.width(com.example.ui.theme.Dimens.PillMinWidth).height(com.example.ui.theme.Dimens.PillMinHeight),
                        matchCharmPaletteWhenUnselected = true
                    )
                }
            }
        }
    }
}
