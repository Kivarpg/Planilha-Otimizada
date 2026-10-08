package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedOnSurface

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Marcador quadrado compartilhado pela Aba 9 e pela apresentação equivalente
// da Aba 11. Preenchimento indica Favorecido/Casta/Aspecto; vazio mantém apenas
// a moldura para preservar o alinhamento da coluna.
@Composable
internal fun SummarySpecialIndicator(marked: Boolean, compact: Boolean = false) {
    Box(
        modifier = Modifier
            .size(if (compact) 11.dp else 14.dp)
            .then(if (marked) Modifier.background(ExaltedAccentBright) else Modifier)
            .border(
                1.dp,
                if (marked) ExaltedAccentBright else ExaltedOnSurface
            )
    )
}
