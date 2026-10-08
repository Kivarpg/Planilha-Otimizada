package com.example.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Arma
import com.example.ui.components.IniciativaAjusteButton
import com.example.ui.components.GildedCard
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedEssenciaClaro
import com.example.ui.theme.ExaltedEssenciaEscuro
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted

@Composable
internal fun AtaqueCard(arma: Arma) {
    GildedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                com.example.ui.components.AutoSizeText(
                    text = arma.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                    minFontSize = 13.sp,
                    color = ExaltedAccentBright,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (arma.ataqueDesarmado) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = ExaltedAmber.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ExaltedAmber.copy(alpha = 0.6f))
                    ) {
                        AppText(
                            text = "Desarmado",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExaltedAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AppText(
                text = "Ini: ${arma.iniciativa} | Dec: ${arma.decisivo} | Defesa: ${arma.defesa} | Dano: ${arma.dano} | Dano Mín: ${arma.danoMinimo.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = ExaltedMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Degradê amarelo (claro -> escuro) para os 5 níveis de Essência Permanente,
// no mesmo espírito do degradê usado no contador de Limite (Aba 1).
internal fun essenciaGradientColor(nivel: Int): Color {
    val claro = ExaltedEssenciaClaro
    val escuro = ExaltedEssenciaEscuro
    val t = (nivel - 1) / 4f
    return androidx.compose.ui.graphics.lerp(claro, escuro, t.coerceIn(0f, 1f))
}

// IniciativaAjusteButton foi movido pra com.example.ui.components.RatingControls.kt
// (2026-09) — reutilizado também pela Aba 11 (Encontros).
