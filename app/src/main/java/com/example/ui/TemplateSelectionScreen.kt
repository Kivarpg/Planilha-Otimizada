package com.example.ui
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AutoSizeText
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedBackdropCore
import com.example.ui.theme.ExaltedBackdropGlow
import com.example.ui.theme.ExaltedDarkBackground
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.ExaltedMetalGoldCore
import com.example.ui.theme.ExaltedVisualBlueprints

// Um "template" é uma linha do Exaltado com sua própria planilha (Solar,
// Sangue de Dragão e Lunar implementados). Os nomes existentes permanecem
// intactos; esta tela apenas apresenta a escolha de forma mais monumental.
data class ExaltTemplate(val nome: String, val habilitado: Boolean)

val TEMPLATES_DISPONIVEIS = listOf(
    ExaltTemplate(nome = "Solar", habilitado = true),
    ExaltTemplate(nome = "Sangue de Dragão", habilitado = true),
    ExaltTemplate(nome = "Lunar", habilitado = true)
)

@Composable
fun TemplateSelectionScreen(onTemplateSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(ExaltedBackdropGlow.copy(alpha = 0.18f), ExaltedBackdropCore, ExaltedDarkBackground),
                    radius = 1100f
                )
            )
            .drawBehind {
                val line = ExaltedAccentBright.copy(alpha = 0.12f)
                drawLine(line, Offset(size.width * 0.05f, 0f), Offset(size.width * 0.05f, size.height), 1f)
                drawLine(line, Offset(size.width * 0.95f, 0f), Offset(size.width * 0.95f, size.height), 1f)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            // APPROVED VISUAL CUSTOMIZATION
            // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
            // Logotipo oficial (novo asset): fixo no topo. Altura limitada para
            // cabeçalho sticky — proporção ~2.36:1 do PNG; ~88dp de altura
            // mantém legibilidade sem competir com os cards de template.
            Image(
                painter = painterResource(R.drawable.logo_exalted),
                contentDescription = "EXALTED",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(min = 56.dp, max = 76.dp)
                    .padding(horizontal = 4.dp)
            )
            AutoSizeText(
                text = "Gerador Harmônico",
                color = ExaltedOnSurface,
                fontWeight = FontWeight.SemiBold,
                maxFontSize = 17.sp,
                minFontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Lista rolável: Lunar e futuros botões não deformam o layout.
            // O logo e o subtítulo ficam fora do scroll (sempre visíveis).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TEMPLATES_DISPONIVEIS.forEach { template ->
                    val isSolar = template.nome == "Solar"
                    val isDragon = template.nome == "Sangue de Dragão"
                    val templatePalette = when {
                        isDragon -> ExaltedVisualBlueprints.SangueDeDragao.palette
                        template.nome == "Lunar" -> ExaltedVisualBlueprints.Lunar.palette
                        else -> ExaltedVisualBlueprints.Solar.palette
                    }
                    val accent = templatePalette.primaryBright
                    val accentDeep = templatePalette.metalDeep
                    val icon = when {
                        isDragon -> R.drawable.tab_icon_dragao
                        template.nome == "Lunar" -> R.drawable.tab_icon_lua
                        else -> R.drawable.tab_icon_sol
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentDeep.copy(alpha = 0.58f), ExaltedDarkSurface, ExaltedDarkSurfaceVariant)
                                )
                            )
                            .border(1.5.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                            .feedbackClickable(enabled = template.habilitado) {
                                if (template.habilitado) onTemplateSelected(template.nome)
                            }
                            .drawBehind {
                                drawLine(accent.copy(alpha = 0.75f), Offset(12.dp.toPx(), 4.dp.toPx()), Offset(size.width - 12.dp.toPx(), 4.dp.toPx()), 2.5f)
                                drawLine(accent.copy(alpha = 0.25f), Offset(12.dp.toPx(), size.height - 4.dp.toPx()), Offset(size.width - 12.dp.toPx(), size.height - 8.dp.toPx()), 1f)
                                drawCircle(accent.copy(alpha = 0.14f), size.minDimension * 0.52f, Offset(size.width * 0.88f, size.height * 0.5f))
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.07f))
                                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(icon),
                                    contentDescription = template.nome,
                                    modifier = Modifier
                                        .size(86.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                AppText(
                                    text = if (isDragon) "Dragon-Blooded" else template.nome,
                                    color = accent,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                AppText(
                                    text = if (isSolar) "Solar" else if (isDragon) "Sangue de Dragão" else "Lunar",
                                    color = ExaltedMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                            AppText(
                                text = "›",
                                color = accent,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            }
        }
    }
}
