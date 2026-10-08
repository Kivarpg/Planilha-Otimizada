package com.example.ui.tabs


import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.InkButtonVariant
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedDarkBackground
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedOnSurface
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Aba 14 — Mapa de Creation.
 * Escala fixa: 170 px = 500 km (grade do mapa 003).
 */

private data class TransportMode(
    val name: String,
    val kmh: Float,
    val hoursPerDay: Float
)

/**
 * CONTRATO DE CALIBRAÇÃO DO MAPA.
 *
 * A grade da imagem-fonte define 170 pixels originais = 500 km. Distâncias de
 * rota e a régua visual DEVEM derivar exclusivamente desta relação. Nunca
 * introduza uma segunda constante "km por barra": a largura renderizada muda
 * com ContentScale.Fit, densidade, viewport e zoom.
 */
internal const val MAP_REFERENCE_PX = 170f
internal const val MAP_REFERENCE_KM = 500f
internal const val MAP_INTRINSIC_WIDTH_PX = 5780f
internal const val MAP_INTRINSIC_HEIGHT_PX = 3740f

internal fun mapPixelsToKm(mapPixels: Float): Float =
    mapPixels * (MAP_REFERENCE_KM / MAP_REFERENCE_PX)

internal fun screenPixelsToMapKm(
    screenPixels: Float,
    fittedDrawWidthPx: Float,
    effectiveScale: Float
): Float {
    if (fittedDrawWidthPx <= 0f || effectiveScale <= 0f) return 0f
    val originalMapPixels = screenPixels * MAP_INTRINSIC_WIDTH_PX / (fittedDrawWidthPx * effectiveScale)
    return mapPixelsToKm(originalMapPixels)
}

/** Distancia de rota em km a partir de pontos normalizados na imagem original. */
internal fun mapRouteDistanceKm(points: List<Offset>): Float {
    if (points.size < 2) return 0f
    var totalPixels = 0f
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        totalPixels += hypot(
            (b.x - a.x) * MAP_INTRINSIC_WIDTH_PX,
            (b.y - a.y) * MAP_INTRINSIC_HEIGHT_PX
        )
    }
    return mapPixelsToKm(totalPixels)
}

@Composable
fun MapTab(
    modifier: Modifier = Modifier,
    background: Color = ExaltedDarkBackground,
    gold: Color = ExaltedAccentBright,
    routePoints: SnapshotStateList<Offset> = remember { mutableStateListOf() }
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var normalizedOffset by remember { mutableStateOf(Offset.Zero) }

    val modes = remember {
        mutableStateListOf(
            TransportMode("Cavalo", 6f, 10f),
            TransportMode("Navio Mercante", 13f, 24f),
            TransportMode("Esquife Cirrus", 10f, 24f),
            TransportMode("Agata", 48f, 10f),
            TransportMode("Cavaleiro do Vento da Tempestade", 161f, 10f)
        )
    }
    var selectedModeIndex by remember { mutableIntStateOf(0) }
    var transportMenuOpen by remember { mutableStateOf(false) }
    var showCadastro by remember { mutableStateOf(false) }

    // Formulário de cadastro
    var novoNome by remember { mutableStateOf("") }
    var novoKmh by remember { mutableStateOf("") }
    var novoHoras by remember { mutableStateOf("") }

    // derivedStateOf observa as leituras da lista mutavel e evita criar uma
    // copia de todos os pontos em cada recomposicao da tela do mapa.
    val distanceKm by remember(routePoints) {
        androidx.compose.runtime.derivedStateOf { mapRouteDistanceKm(routePoints) }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = gold,
        unfocusedBorderColor = gold.copy(alpha = 0.45f),
        focusedLabelColor = gold,
        cursorColor = gold,
        focusedTextColor = ExaltedOnSurface,
        unfocusedTextColor = ExaltedOnSurface,
        focusedContainerColor = ExaltedDarkSurface,
        unfocusedContainerColor = ExaltedDarkSurface,
        disabledBorderColor = gold.copy(alpha = 0.35f),
        disabledLabelColor = gold.copy(alpha = 0.7f),
        disabledTextColor = ExaltedOnSurface
    )

    val mode = modes.getOrNull(selectedModeIndex.coerceIn(0, (modes.size - 1).coerceAtLeast(0)))
    val kmPerDay = if (mode != null) mode.kmh * mode.hoursPerDay else 0f
    val days = if (kmPerDay > 0f && routePoints.size >= 2) distanceKm / kmPerDay else 0f


    var mapaEmTelaCheia by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .exaltedTabIdentity(14).exaltedContentStage(14)
            .background(background.copy(alpha = 0.82f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW

        // —— TOPO FIXO: controles principais (não se move com o mapa) ——
        // Mantemos os três comandos na mesma linha e usamos rolagem horizontal
        // apenas como proteção para larguras muito pequenas, evitando esmagar
        // ou deformar os botões.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InkButton(
                label = "Tela cheia",
                onClick = { mapaEmTelaCheia = true },
                size = InkButtonSize.Small
            )
            InkButton(
                label = "Limpar",
                onClick = { routePoints.clear() },
                size = InkButtonSize.Small
            )
        }

        // Tabela de transporte permanece no topo da área fixa.
        // Campos: Transporte | Km/h | Horas/dia
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transporte (dropdown)
            Box(modifier = Modifier.weight(1.4f)) {
                // APPROVED VISUAL CUSTOMIZATION
                // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
                // Campo visualmente integrado ao tema. O clique fica em uma camada
                // transparente acima do OutlinedTextField porque o próprio TextField
                // pode consumir o evento de toque antes de propagar o clickable.
                OutlinedTextField(
                    value = mode?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { AppText("Transporte", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                    textStyle = TextStyle(
                        fontSize = autoFontSizeForName(mode?.name.orEmpty()),
                        color = ExaltedOnSurface
                    )
                )
                // APPROVED VISUAL CUSTOMIZATION
                // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
                // Área de toque dedicada ao seletor. Não usar verticalScroll aqui.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .feedbackClickable { transportMenuOpen = true }
                )
                DropdownMenu(
                    expanded = transportMenuOpen,
                    onDismissRequest = { transportMenuOpen = false },
                    modifier = Modifier
                        .background(ExaltedDarkSurface)
                        .heightIn(max = 320.dp)
                ) {
                    // DropdownMenu já fornece o container/rolagem necessários para o conteúdo.
                    // Não envolver os itens em verticalScroll: isso cria um Lazy/scroll aninhado
                    // dentro do Popup e pode resultar em constraints infinitas durante a medição.
                    modes.forEachIndexed { index, m ->
                        DropdownMenuItem(
                            text = {
                                AppText(
                                    "${m.name}  ·  ${m.kmh.toInt()} km/h  ·  ${m.hoursPerDay.toInt()} h/dia",
                                    color = if (index == selectedModeIndex) gold else ExaltedOnSurface,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                                                selectedModeIndex = index
                                transportMenuOpen = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            AppText(
                                "Cadastrar novo",
                                color = gold,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        onClick = {
                                                        transportMenuOpen = false
                            novoNome = ""
                            novoKmh = ""
                            novoHoras = "10"
                            showCadastro = true
                        }
                    )
                }
            }

            OutlinedTextField(
                value = mode?.kmh?.toInt()?.toString().orEmpty(),
                onValueChange = { v ->
                                        val n = v.filter { it.isDigit() }.toFloatOrNull() ?: return@OutlinedTextField
                    val i = selectedModeIndex.coerceIn(0, modes.lastIndex)
                    modes[i] = modes[i].copy(kmh = n.coerceAtLeast(0.1f))
                },
                label = { AppText("Km/h", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(0.7f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                textStyle = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = mode?.hoursPerDay?.toInt()?.toString().orEmpty(),
                onValueChange = { v ->
                                        val n = v.filter { it.isDigit() }.toFloatOrNull() ?: return@OutlinedTextField
                    val i = selectedModeIndex.coerceIn(0, modes.lastIndex)
                    modes[i] = modes[i].copy(hoursPerDay = n.coerceIn(1f, 24f))
                },
                label = { AppText("Horas/dia", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(0.85f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                textStyle = MaterialTheme.typography.bodySmall
            )

        }

        // Informação da viagem: sempre visível e imediatamente abaixo da
        // tabela de transporte. Não depende do temporizador de interação.
        Spacer(Modifier.height(10.dp))
        TravelSummaryPanel(
            modifier = Modifier.fillMaxWidth(),
            gold = gold,
            transportName = mode?.name.orEmpty(),
            pointCount = routePoints.size,
            distanceKm = distanceKm,
            travelTime = if (mode != null && routePoints.size >= 2) formatDays(days) else "—"
        )

        // Respiro entre a informação/controles fixos e a área do mapa.
        Spacer(Modifier.height(12.dp))

        // —— MAPA ——
        MapaInterativo(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            scale = scale,
            normalizedOffset = normalizedOffset,
            onScaleChange = { scale = it },
            onNormalizedOffsetChange = { normalizedOffset = it },
            routePoints = routePoints,
            gold = gold
        )

        if (mapaEmTelaCheia) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { mapaEmTelaCheia = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    MapaInterativo(
                        modifier = Modifier.fillMaxSize(),
                        scale = scale,
                        compactFullscreen = true,
                        normalizedOffset = normalizedOffset,
                        onScaleChange = { scale = it },
                        onNormalizedOffsetChange = { normalizedOffset = it },
                                    routePoints = routePoints,
                        gold = gold
                    )

                    // APPROVED VISUAL CUSTOMIZATION
                    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
                    // Os dois blocos do topo ocupam áreas independentes: escala à
                    // esquerda e dados da viagem à direita. Nunca sobrepor.
                    //
                    // CORREÇÃO: a tabela de tempo de viagem usava o mesmo Crossfade
                    // de fade-out após 3s de inatividade da visão normal — na tela
                    // maximizada ela precisa ficar sempre visível (pedido explícito
                    // do usuário), então aqui ela é renderizada direto, sem o
                    // Crossfade/alpha condicionado a estatisticasVisiveis.
                    // APPROVED VISUAL CUSTOMIZATION
                    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
                    // O resumo da viagem fica no topo central, imediatamente à direita
                    // da escala. O agrupamento não usa peso/width elástico: isso evita
                    // que a maximização empurre o conteúdo para fora da área visível.
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 12.dp, end = 72.dp)
                    ) {
                        // Desktop/tablet continuam usando a faixa horizontal. Em
                        // celulares em retrato, não comprimimos escala, resumo e
                        // Limpar na mesma Row: isso era o que fazia a escala/rota
                        // perder legibilidade após portar a tela larga para mobile.
                        val fullscreenFit = mapFitGeometry(
                            maxWidth.value,
                            maxHeight.value,
                            MAP_INTRINSIC_WIDTH_PX,
                            MAP_INTRINSIC_HEIGHT_PX
                        )
                        val fullscreenEffectiveScale =
                            scale * portraitFullscreenViewportScale(maxWidth, maxHeight)
                        val scaleBarKm = screenPixelsToMapKm(
                            screenPixels = 70f,
                            fittedDrawWidthPx = fullscreenFit.drawW,
                            effectiveScale = fullscreenEffectiveScale
                        )
                        if (maxWidth < 600.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Mobile fullscreen: a referência aprovada coloca
                                // o resumo no topo; o botão de sair permanece no
                                // canto superior direito, fora desta faixa.
                                TravelSummaryPanel(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 4.dp),
                                    gold = gold,
                                    transportName = mode?.name.orEmpty(),
                                    pointCount = routePoints.size,
                                    distanceKm = distanceKm,
                                    travelTime = if (mode != null && routePoints.size >= 2) formatDays(days) else "—"
                                )
                                // Segunda linha: escala à esquerda e Limpar à
                                // direita, exatamente como na referência mobile.
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TravelScaleBar(gold = gold, representedKm = scaleBarKm)
                                    InkButton(
                                        label = "Limpar",
                                        onClick = { routePoints.clear() },
                                        modifier = Modifier.height(36.dp),
                                        customHeight = 36.dp,
                                        size = InkButtonSize.Small
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TravelScaleBar(gold = gold, representedKm = scaleBarKm)
                                TravelSummaryPanel(
                                    modifier = Modifier.weight(1f),
                                    gold = gold,
                                    transportName = mode?.name.orEmpty(),
                                    pointCount = routePoints.size,
                                    distanceKm = distanceKm,
                                    travelTime = if (mode != null && routePoints.size >= 2) formatDays(days) else "—"
                                )
                                InkButton(
                                    label = "Limpar",
                                    onClick = { routePoints.clear() },
                                    modifier = Modifier.height(40.dp),
                                    customHeight = 40.dp,
                                    size = InkButtonSize.Small
                                )
                            }
                        }
                    }

                    InkButton(
                        onClick = { mapaEmTelaCheia = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(48.dp)) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Sair da tela cheia", tint = gold)
                    }
                }
            }
        }

    }

    // Diálogo: Cadastrar novo meio de transporte
    if (showCadastro) {
        AlertDialog(
            onDismissRequest = { showCadastro = false },
            title = {
                AppText("Cadastrar novo", color = gold, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = novoNome,
                        onValueChange = { novoNome = it.take(48) },
                        label = { AppText("Nome") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = novoKmh,
                        onValueChange = { novoKmh = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { AppText("Km/h") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = novoHoras,
                        onValueChange = { novoHoras = it.filter { ch -> ch.isDigit() } },
                        label = { AppText("Horas/dia") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                InkButton(label = "Salvar", onClick = {
                                        val nome = novoNome.trim()
                    val kmh = novoKmh.toFloatOrNull()
                    val horas = novoHoras.toFloatOrNull()?.coerceIn(1f, 24f)
                    if (kmh != null && horas != null && nome.isNotBlank() && kmh > 0f) {
                        modes += TransportMode(nome, kmh, horas)
                        selectedModeIndex = modes.lastIndex
                        showCadastro = false
                    }
                }, size = InkButtonSize.Small)
            },
            confirmButton = {
                InkButton(label = "Cancelar", onClick = { showCadastro = false }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
            },
            containerColor = ExaltedDarkSurface
        )
    }
}


@Composable
private fun TravelSummaryPanel(
    modifier: Modifier = Modifier,
    gold: Color,
    transportName: String,
    pointCount: Int,
    distanceKm: Float,
    travelTime: String
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
            .border(1.dp, gold.copy(alpha = 0.42f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        AppText(
            text = "Transporte: ${transportName.ifBlank { "—" }} • Tempo de viagem: $travelTime • Pontos: $pointCount • Distância: ${formatKm(distanceKm)} km",
            color = ExaltedOnSurface,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            softWrap = true,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun portraitFullscreenViewportScale(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp): Float {
    if (height <= width) return 1f
    val mapAspect = MAP_INTRINSIC_WIDTH_PX / MAP_INTRINSIC_HEIGHT_PX
    return (height.value / (width.value / mapAspect)).coerceIn(1f, 2.25f)
}

@Composable
private fun TravelScaleBar(
    modifier: Modifier = Modifier,
    gold: Color,
    representedKm: Float
) {
    // A largura visual é fixa em 70dp, mas os quilômetros NÃO são fixos.
    // representedKm já foi derivado do contrato 170 px originais = 500 km,
    // do ContentScale.Fit e do effectiveScale (zoom do usuário + zoom-base
    // do fullscreen). Assim rota e régua compartilham a mesma calibração.
    Box(
        modifier = modifier
            .height(28.dp)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Antes usava Modifier.fillMaxSize() aqui: como este Row é um filho
        // sem peso (weight) dentro da Row externa que também contém o
        // TravelSummaryPanel(weight=1f), o fillMaxSize() fazia esta barra
        // ocupar TODA a largura disponível na primeira passagem de medição
        // (a Row mede filhos sem peso com a largura máxima do pai antes de
        // calcular o espaço restante pros filhos com peso) — sobrando 0dp
        // pro quadro de viagem, que ficava invisível à direita da escala.
        // fillMaxHeight() preserva a altura fixa do Box (28dp) sem forçar
        // a largura, deixando a barra encolher para o tamanho do conteúdo.
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(2.dp)
                    .background(gold)
            )
            AppText(
                text = "${formatKm(representedKm)} km",
                color = ExaltedOnSurface,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

/** Fonte menor para nomes longos no campo Transporte. */
private fun autoFontSizeForName(name: String) = when {
    name.length > 28 -> 10.sp
    name.length > 20 -> 11.sp
    name.length > 14 -> 12.sp
    else -> 13.sp
}

private fun formatKm(km: Float): String =
    if (km >= 100f) km.roundToInt().toString()
    else String.format("%.1f", km)

private fun formatDays(days: Float): String {
    if (days <= 0f) return "—"
    if (days < 1f) {
        val hours = days * 24f
        return if (hours < 1f) "${(hours * 60f).roundToInt()} min" else String.format("%.1f h", hours)
    }
    return if (days < 10f) String.format("%.1f dias", days) else "${days.roundToInt()} dias"
}

// Extraído do corpo de MapTab (pedido explícito do usuário: opção de tela
// cheia no mapa). Recebe scale/offset como valor + callback — o mesmo
// estado usado na visualização normal e na tela cheia, garantindo que
// zoom/pan/marcações feitos num modo continuem no outro ao alternar.
internal data class MapFitGeometry(
    val drawW: Float,
    val drawH: Float,
    val originX: Float,
    val originY: Float
)

internal fun mapFitGeometry(boxW: Float, boxH: Float, mapWidth: Float, mapHeight: Float): MapFitGeometry {
    val imgAspect = mapWidth / mapHeight
    val boxAspect = boxW / boxH
    return if (imgAspect > boxAspect) {
        val drawH = boxW / imgAspect
        MapFitGeometry(boxW, drawH, 0f, (boxH - drawH) / 2f)
    } else {
        val drawW = boxH * imgAspect
        MapFitGeometry(drawW, boxH, (boxW - drawW) / 2f, 0f)
    }
}

private fun calcularTilesVisiveis(
    scale: Float,
    offset: Offset,
    boxW: Float,
    boxH: Float,
    mapWidth: Float,
    mapHeight: Float,
    tileSize: Int,
    zoomMinHd: Float
): List<com.example.data.MapaAltaResolucaoCache.TileKey> {
    if (scale < zoomMinHd || boxW <= 0f || boxH <= 0f) return emptyList()

    val fit = mapFitGeometry(boxW, boxH, mapWidth, mapHeight)
    val drawW = fit.drawW
    val drawH = fit.drawH
    val originX = fit.originX
    val originY = fit.originY

    val center = Offset(boxW / 2f, boxH / 2f)
    val corners = arrayOf(
        Offset(0f, 0f),
        Offset(boxW, 0f),
        Offset(0f, boxH),
        Offset(boxW, boxH)
    ).map { screen ->
        Offset(
            (screen.x - center.x - offset.x) / scale + center.x,
            (screen.y - center.y - offset.y) / scale + center.y
        )
    }

    val minBaseX = corners.minOf { it.x }
    val maxBaseX = corners.maxOf { it.x }
    val minBaseY = corners.minOf { it.y }
    val maxBaseY = corners.maxOf { it.y }

    val minMapX = ((minBaseX - originX) / drawW * mapWidth).coerceIn(0f, mapWidth)
    val maxMapX = ((maxBaseX - originX) / drawW * mapWidth).coerceIn(0f, mapWidth)
    val minMapY = ((minBaseY - originY) / drawH * mapHeight).coerceIn(0f, mapHeight)
    val maxMapY = ((maxBaseY - originY) / drawH * mapHeight).coerceIn(0f, mapHeight)

    val maxTileX = (mapWidth.toInt() - 1) / tileSize
    val maxTileY = (mapHeight.toInt() - 1) / tileSize
    val firstX = (minMapX.toInt() / tileSize - 1).coerceIn(0, maxTileX)
    val lastX = (maxMapX.toInt() / tileSize + 1).coerceIn(0, maxTileX)
    val firstY = (minMapY.toInt() / tileSize - 1).coerceIn(0, maxTileY)
    val lastY = (maxMapY.toInt() / tileSize + 1).coerceIn(0, maxTileY)

    return buildList {
        for (y in firstY..lastY) {
            for (x in firstX..lastX) {
                add(com.example.data.MapaAltaResolucaoCache.TileKey(x, y))
            }
        }
    }
}

@Composable
private fun MapaInterativo(
    modifier: Modifier,
    scale: Float,
    compactFullscreen: Boolean = false,
    normalizedOffset: Offset,
    onScaleChange: (Float) -> Unit,
    onNormalizedOffsetChange: (Offset) -> Unit,
    routePoints: androidx.compose.runtime.snapshots.SnapshotStateList<Offset>,
    gold: Color
) {
    val mapIntrinsic = remember { androidx.compose.ui.geometry.Size(MAP_INTRINSIC_WIDTH_PX, MAP_INTRINSIC_HEIGHT_PX) }

    BoxWithConstraints(
        modifier = modifier
            .border(1.5.dp, gold.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .clipToBounds()
            .background(Color(0xFF0A0A0A))
    ) {
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        // O pan é persistido como fração da área efetivamente desenhada pelo
        // ContentScale.Fit. Normalizar pela viewport deslocava o ponto geográfico
        // ao alternar entre layouts com proporções diferentes (normal/tela cheia).
        val imgAspect = mapIntrinsic.width / mapIntrinsic.height
        val boxAspect = boxW / boxH
        val fittedDrawW = if (imgAspect > boxAspect) boxW else boxH * imgAspect
        val fittedDrawH = if (imgAspect > boxAspect) boxW / imgAspect else boxH
        val viewportOffset = Offset(
            normalizedOffset.x * fittedDrawW,
            normalizedOffset.y * fittedDrawH
        )
        // Em escala normal (1x), o mapa permanece ancorado para que pequenos
        // movimentos do dedo ao marcar pontos não desloquem a cartografia.
        // Depois de ampliar, o pan volta a ficar disponível em todas as direções.
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 8f)
            onScaleChange(newScale)
            if (newScale > 1f) {
                onNormalizedOffsetChange(
                    Offset(
                        normalizedOffset.x + (panChange.x / fittedDrawW),
                        normalizedOffset.y + (panChange.y / fittedDrawH)
                    )
                )
            } else if (normalizedOffset != Offset.Zero) {
                // Ao retornar a 1x, recentraliza e recupera o estado fixo.
                onNormalizedOffsetChange(Offset.Zero)
            }
        }

        // Em tela cheia de telefone em retrato, Fit puro deixava o mapa como
        // uma faixa horizontal estreita no centro. Aplicamos um zoom-base apenas
        // visual para preencher melhor o viewport; toda a matemática de toque,
        // rotas e tiles continua no mesmo sistema normalizado e recebe exatamente
        // a mesma transformação, preservando coordenadas.
        val viewportScale = if (compactFullscreen) {
            portraitFullscreenViewportScale(maxWidth, maxHeight)
        } else 1f
        val effectiveScale = scale * viewportScale

        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(effectiveScale, viewportOffset, boxW, boxH) {
                    detectTapGestures { tapOffset ->
                        val imgAspect = mapIntrinsic.width / mapIntrinsic.height
                        val boxAspect = boxW / boxH
                        val drawW: Float
                        val drawH: Float
                        val originX: Float
                        val originY: Float
                        if (imgAspect > boxAspect) {
                            drawW = boxW
                            drawH = boxW / imgAspect
                            originX = 0f
                            originY = (boxH - drawH) / 2f
                        } else {
                            drawH = boxH
                            drawW = boxH * imgAspect
                            originX = (boxW - drawW) / 2f
                            originY = 0f
                        }
                        val center = Offset(boxW / 2f, boxH / 2f)
                        val unscaled = Offset(
                            (tapOffset.x - center.x - viewportOffset.x) / effectiveScale + center.x,
                            (tapOffset.y - center.y - viewportOffset.y) / effectiveScale + center.y
                        )
                        val localX = (unscaled.x - originX) / drawW
                        val localY = (unscaled.y - originY) / drawH
                        if (localX in 0f..1f && localY in 0f..1f) {
                            routePoints += Offset(localX, localY)
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = effectiveScale
                    scaleY = effectiveScale
                    translationX = viewportOffset.x
                    translationY = viewportOffset.y
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            val context = LocalContext.current
            val visibleHdTiles = remember(effectiveScale, viewportOffset, boxW, boxH) {
                calcularTilesVisiveis(
                    scale = effectiveScale,
                    offset = viewportOffset,
                    boxW = boxW,
                    boxH = boxH,
                    mapWidth = MAP_INTRINSIC_WIDTH_PX,
                    mapHeight = MAP_INTRINSIC_HEIGHT_PX,
                    tileSize = com.example.data.MapaAltaResolucaoCache.TILE_SIZE,
                    zoomMinHd = com.example.data.MapaAltaResolucaoCache.ZOOM_MIN_HD
                )
            }
            LaunchedEffect(visibleHdTiles) {
                if (visibleHdTiles.isNotEmpty()) {
                    com.example.data.MapaAltaResolucaoCache.carregarTiles(context, visibleHdTiles)
                }
            }

            // O mapa leve permanece como base/fallback. Os tiles HD são
            // desenhados por cima apenas nas regiões visíveis, preservando
            // a resolução original sem manter o bitmap inteiro na RAM.
            Image(
                painter = painterResource(id = R.drawable.mapa_creation),
                contentDescription = "Mapa de Creation",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val mapa = com.example.data.MapaAltaResolucaoCache
                if (visibleHdTiles.isNotEmpty()) {
                    val imgAspect = mapIntrinsic.width / mapIntrinsic.height
                    val boxAspect = size.width / size.height
                    val drawW: Float
                    val drawH: Float
                    val originX: Float
                    val originY: Float
                    if (imgAspect > boxAspect) {
                        drawW = size.width
                        drawH = size.width / imgAspect
                        originX = 0f
                        originY = (size.height - drawH) / 2f
                    } else {
                        drawH = size.height
                        drawW = size.height * imgAspect
                        originX = (size.width - drawW) / 2f
                        originY = 0f
                    }
                    visibleHdTiles.forEach { key ->
                        val tile = mapa.tile(key) ?: return@forEach
                        val srcLeft = key.x * mapa.TILE_SIZE
                        val srcTop = key.y * mapa.TILE_SIZE
                        val srcRight = minOf(srcLeft + mapa.TILE_SIZE, mapa.mapaLargura)
                        val srcBottom = minOf(srcTop + mapa.TILE_SIZE, mapa.mapaAltura)
                        val dstLeft = originX + srcLeft / mapa.mapaLargura.toFloat() * drawW
                        val dstTop = originY + srcTop / mapa.mapaAltura.toFloat() * drawH
                        val dstRight = originX + srcRight / mapa.mapaLargura.toFloat() * drawW
                        val dstBottom = originY + srcBottom / mapa.mapaAltura.toFloat() * drawH
                        drawImage(
                            image = tile,
                            dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
                            dstSize = IntSize(
                                (dstRight - dstLeft).roundToInt().coerceAtLeast(1),
                                (dstBottom - dstTop).roundToInt().coerceAtLeast(1)
                            )
                        )
                    }
                }
                val imgAspect = mapIntrinsic.width / mapIntrinsic.height
                val boxAspect = size.width / size.height
                val drawW: Float
                val drawH: Float
                val originX: Float
                val originY: Float
                if (imgAspect > boxAspect) {
                    drawW = size.width
                    drawH = size.width / imgAspect
                    originX = 0f
                    originY = (size.height - drawH) / 2f
                } else {
                    drawH = size.height
                    drawW = size.height * imgAspect
                    originX = (size.width - drawW) / 2f
                    originY = 0f
                }
                fun toScreen(p: Offset) = Offset(
                    originX + p.x * drawW,
                    originY + p.y * drawH
                )
                if (routePoints.size >= 2) {
                    val path = androidx.compose.ui.graphics.Path()
                    val first = toScreen(routePoints[0])
                    path.moveTo(first.x, first.y)
                    for (i in 1 until routePoints.size) {
                        val pt = toScreen(routePoints[i])
                        path.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = path,
                        color = gold.copy(alpha = 0.9f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            // O Canvas inteiro recebe effectiveScale no graphicsLayer.
                            // Compensamos aqui para a rota não engrossar ao ampliar:
                            // a espessura visual permanece estável, como os marcadores.
                            width = 3.dp.toPx() / effectiveScale,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
                // Os marcadores acompanham a posição geográfica do mapa, mas não
                // devem crescer junto com o zoom. Como este Canvas está dentro da
                // graphicsLayer escalada, compensamos o scale e ainda reduzimos
                // suavemente o tamanho visual conforme a aproximação aumenta.
                val markerVisualScale = (1f / kotlin.math.sqrt(effectiveScale))
                    .coerceIn(0.34f, 1f)
                val markerLayerScale = markerVisualScale / effectiveScale
                routePoints.forEach { p ->
                    val c = toScreen(p)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.55f),
                        radius = 9.dp.toPx() * markerLayerScale,
                        center = c
                    )
                    drawCircle(
                        color = gold,
                        radius = 6.dp.toPx() * markerLayerScale,
                        center = c
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx() * markerLayerScale,
                        center = c
                    )
                }
            }
        }
    }
}