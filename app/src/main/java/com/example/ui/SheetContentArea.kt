package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExaltedBackdropGlow
import com.example.ui.theme.ExaltedDarkBackground
import com.example.ui.theme.ExaltedBackdropCore
import com.example.ui.theme.ExaltedBlack

/** Hosts one tab at a time; each tab owns its vertical scrolling. */
@Composable
internal fun SheetContentArea(tabs: List<SheetTab>, selectedTabIndex: Int) {
    val focusManager = LocalFocusManager.current
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // IDENTIDADE VISUAL v4 — o conteúdo é um "fólio" contínuo. A moldura
    // externa desaparece; os componentes internos definem sua própria
    // hierarquia e a tela passa a ter um único campo de leitura.
    Box(
        Modifier
            .fillMaxSize()
            .background(sheetBackgroundBrush())
            .pointerInput(Unit){detectTapGestures(onTap={focusManager.clearFocus()})}

    ){
        val widthDp = LocalConfiguration.current.screenWidthDp
        val contentModifier = when {
            widthDp >= 1000 -> Modifier.widthIn(max=980.dp).align(Alignment.TopCenter)
            widthDp >= 720 -> Modifier.widthIn(max=900.dp).align(Alignment.TopCenter)
            else -> Modifier.fillMaxWidth()
        }
        Box(
            contentModifier
                .fillMaxHeight()
                .padding(start=14.dp,end=8.dp,top=6.dp,bottom=8.dp)
        ){
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(selectedTabIndex){tabs[selectedTabIndex].content()}
        }
    }
}

/** Original opaque backdrop, kept separate from tab content. */
private fun sheetBackgroundBrush(): Brush = Brush.verticalGradient(
    listOf(ExaltedBackdropGlow, ExaltedDarkBackground, ExaltedBackdropCore, ExaltedBlack)
)
