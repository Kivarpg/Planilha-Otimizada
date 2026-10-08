package com.example.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable

/**
 * Definição completa de uma aba da planilha.
 *
 * Título, ícone e conteúdo pertencem à mesma definição para impedir que uma
 * reordenação deixe metadados e conteúdo em posições diferentes.
 */
data class SheetTab(
    val title: String,
    @param:DrawableRes val iconRes: Int,
    val content: @Composable () -> Unit
)

/**
 * Metadados leves usados pela barra de navegação.
 *
 * O conteúdo composable fica fora deste contrato para que mudanças internas
 * das abas não obriguem a LazyRow a depender das lambdas de conteúdo.
 */
data class SheetTabNavigationItem(
    val title: String,
    @param:DrawableRes val iconRes: Int
)

fun List<SheetTab>.navigationItems(): List<SheetTabNavigationItem> =
    map { SheetTabNavigationItem(it.title, it.iconRes) }
