package com.example.ui.tabs
import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackOnPress
import com.example.ui.components.feedbackClickable

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import com.example.ui.components.AutoSizeAppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.model.NpcEncontro
import com.example.model.ArmorStatsTable
import com.example.model.WeaponStatsTable
import com.example.ui.theme.ExaltVisualTemplate
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.visualTemplateParaExaltado

// Extraído de EncounterNpcCard.kt (refatoração de organização — pedido
// explícito do usuário, sem mudança de comportamento). Diálogo de
// confirmação simples, sem estado próprio além da visibilidade — bom
// candidato a extração isolada, já que não depende do restante do card.
@Composable
internal fun NpcCardJuntarSeDialog(
    visible: Boolean,
    npcNome: String,
    visualTemplate: ExaltVisualTemplate,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = com.example.ui.components.gildedDialogBorder(),
        title = { AppText("Juntar-se à Batalha", color = visualTemplate.accentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            AppText(
                "Tem certeza que deseja que $npcNome se junte à batalha?",
                color = visualTemplate.onSurface,
                    forceStroke = true
)
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(
                visualTemplate = visualTemplate,
                text = "Juntar-se",
                onClick = onConfirm
            )
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(
                visualTemplate = visualTemplate,
                text = "Cancelar",
                onClick = onDismiss
            )
        },
        containerColor = visualTemplate.surface,
        titleContentColor = visualTemplate.accentBright,
        textContentColor = visualTemplate.onSurface
    )
}

// Extraído de EncounterNpcCard.kt (refatoração de organização — mesmo
// pedido). Busca o próprio Context via LocalContext, evitando precisar
// repassá-lo como parâmetro desde o chamador.
@Composable
internal fun NpcCardCarregarDialog(
    visible: Boolean,
    visualTemplate: ExaltVisualTemplate,
    onCarregarNpc: (com.example.model.NpcEncontro) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val context = androidx.compose.ui.platform.LocalContext.current
    var salvos by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var carregandoSalvos by remember { mutableStateOf(true) }
    var arquivoEmCarregamento by remember { mutableStateOf<java.io.File?>(null) }
    LaunchedEffect(visible) {
        if (visible) {
            carregandoSalvos = true
            salvos = com.example.data.NpcSaveLoadService.listarSalvos(context)
            carregandoSalvos = false
        }
    }
    // Corrigido: um .save corrompido (JSON estruturalmente inválido) fazia
    // NpcEncontroJsonCodec.decode() lançar exceção direto no clique, sem
    // try/catch nenhum entre a UI e o codec — derrubava a tela inteira.
    // O carregamento agora é iniciado por LaunchedEffect, garantindo que a
    // função suspend permaneça fora do callback síncrono do Modifier.
    var erroCarregamento by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(arquivoEmCarregamento) {
        val arquivo = arquivoEmCarregamento ?: return@LaunchedEffect
        try {
            val carregado = com.example.data.NpcSaveLoadService.carregar(context, arquivo)
            erroCarregamento = null
            onCarregarNpc(carregado)
        } catch (e: com.example.data.NpcSaveCorruptedException) {
            erroCarregamento = "Não foi possível carregar \"${arquivo.name.removeSuffix(".save")}\": arquivo corrompido ou inválido."
        } catch (e: Exception) {
            erroCarregamento = "Não foi possível carregar o NPC salvo."
        } finally {
            arquivoEmCarregamento = null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Carregar NPC salvo", color = visualTemplate.accentBright, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            Column {
                erroCarregamento?.let { mensagem ->
                    AppText(
                        mensagem,
                        style = MaterialTheme.typography.bodySmall,
                        color = visualTemplate.accentBright,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    forceStroke = true
)
                }
                if (carregandoSalvos) {
                    AppText("Carregando NPCs salvos...", color = visualTemplate.muted, forceStroke = true)
                } else if (salvos.isEmpty()) {
                    AppText("Nenhum NPC salvo encontrado.", color = visualTemplate.muted,
                    forceStroke = true
)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        salvos.forEach { arquivo ->
                            AppText(
                                arquivo.name.removeSuffix(".save"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = visualTemplate.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .feedbackClickable(enabled = arquivoEmCarregamento == null) {
                                        arquivoEmCarregamento = arquivo
                                    }
                                    .padding(vertical = 8.dp),
                    forceStroke = true
)
                        }
                    }
                }
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(text = "Fechar", onClick = onDismiss, visualTemplate = visualTemplate)
        },
        containerColor = visualTemplate.surfaceVariant
    )
}

// Editor de equipamento do NPC — pedido explícito do usuário: ao tocar na
// linha "Equipamento" do card, poder trocar a Arma/Armadura já atribuídas
// por qualquer uma cadastrada no catálogo, com estatísticas preenchidas
// automaticamente. Mesmo padrão de agrupamento por peso já usado nas
// sugestões de catálogo da Aba 5 (WeaponSection.kt).
@Composable
internal fun EditarEquipamentoNpcDialog(
    visible: Boolean,
    npc: NpcEncontro,
    detalhesAtuais: String,
    visualTemplate: ExaltVisualTemplate,
    onAtualizarArma: (com.example.model.ArmaEncontro) -> Unit,
    onAtualizarArmadura: (com.example.model.ArmaduraEncontro) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    // Aba 11 — Encontros: cada NPC possui exatamente uma arma e uma armadura.
    // O equipamento é atribuído pelo gerador; o popup é somente informativo.
    // Não permitir uma segunda seleção evita que o usuário crie múltiplos
    // equipamentos ou substitua acidentalmente o equipamento gerado.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            AppText(
                "Equipamento",
                color = visualTemplate.accentBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                    forceStroke = true
)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                npc.arma?.let { arma ->
                    AppText(
                        "ARMA",
                        style = MaterialTheme.typography.labelLarge,
                        color = visualTemplate.gold,
                        fontWeight = FontWeight.Bold,
                        forceStroke = true
                    )
                    AppText(
                        arma.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        color = visualTemplate.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp),
                        forceStroke = true
                    )
                    AppText(
                        "${npc.habilidadePrincipal.ifBlank { "—" }} — ${arma.tipo}, ${arma.peso}",
                        style = MaterialTheme.typography.labelMedium,
                        color = visualTemplate.gold,
                        modifier = Modifier.padding(bottom = 4.dp),
                        forceStroke = true
                    )
                    AppText(
                        "Fulminante: ${npc.acaoPrincipal}   Decisivo: ${npc.acaoDecisiva}   Defesa: ${arma.defesa}",
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                    val danoMinimoArma = if (npc.habilidadePrincipal == "Arremesso" || npc.habilidadePrincipal == "Arqueirismo") {
                        WeaponStatsTable.distancia(arma.tipo, arma.peso).second
                    } else {
                        WeaponStatsTable.corpoACorpo(arma.tipo, arma.peso).danoMinimo
                    }
                    AppText(
                        "Dano: ${npc.dano.removePrefix("+").removeSuffix("L")}   Dano Mínimo: $danoMinimoArma   Comitamento: ${WeaponStatsTable.comitamento(arma.tipo)}",
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                    if (arma.etiquetas.isNotEmpty()) {
                        AppText(
                            "Etiquetas: ${arma.etiquetas.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = visualTemplate.muted,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                            forceStroke = true
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (npc.arma != null && npc.armadura != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                npc.armadura?.let { armadura ->
                    AppText(
                        "ARMADURA",
                        style = MaterialTheme.typography.labelLarge,
                        color = visualTemplate.gold,
                        fontWeight = FontWeight.Bold,
                        forceStroke = true
                    )
                    AppText(
                        armadura.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        color = visualTemplate.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp),
                        forceStroke = true
                    )
                    AppText(
                        "${armadura.tipo}, ${armadura.peso}",
                        style = MaterialTheme.typography.labelMedium,
                        color = visualTemplate.gold,
                        modifier = Modifier.padding(bottom = 4.dp),
                        forceStroke = true
                    )
                    val statsArmadura = ArmorStatsTable.stats(armadura.tipo, armadura.peso)
                    AppText(
                        "Absorção: ${statsArmadura.absorcao}   Dureza: ${statsArmadura.dureza}   Penalidade: ${statsArmadura.penalidadeMobilidade}",
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                    AppText(
                        "Comitamento: ${statsArmadura.comitamento}",
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                    if (armadura.marcadores.isNotEmpty()) {
                        AppText(
                            "Características: ${armadura.marcadores.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = visualTemplate.muted,
                            modifier = Modifier.padding(top = 2.dp),
                            forceStroke = true
                        )
                    }
                }

                if (npc.arma == null && npc.armadura == null) {
                    AppText(
                        "Nenhum equipamento atribuído.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = visualTemplate.muted,
                    forceStroke = true
)
                }
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(
                visualTemplate = visualTemplate,
                text = "Fechar",
                onClick = onDismiss
            )
        },
        containerColor = visualTemplate.surfaceVariant
    )
}

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
@Composable
private fun NpcSpecialIndicator(
    marked: Boolean,
    npc: NpcEncontro,
    visualTemplate: ExaltVisualTemplate,
    compact: Boolean = false
) {
    // O marcador pertence semanticamente ao Exaltado, não à área visual
    // atualmente aberta. Assim, Favorecida/Casta/Aspecto sempre usa a
    // paleta do próprio NPC mesmo quando o card estiver sob outro tema.
    // O template recebido já pertence ao Tipo de Exaltado do NPC.
    // Evitar uma nova resolução da paleta para cada marcador de atributo
    // ou habilidade renderizado na ficha.
    val markerColor = visualTemplate.accentBright
    Box(
        modifier = Modifier
            .size(if (compact) 11.dp else 14.dp)
            .then(
                if (marked) {
                    Modifier.background(markerColor)
                } else {
                    Modifier
                }
            )
            .border(1.dp, if (marked) markerColor else visualTemplate.onSurface)
    )
}

private fun habilidadesDeCastaOuAspecto(npc: NpcEncontro): Set<String> = when (npc.tipoExaltado) {
    com.example.model.TipoExaltadoEncontro.SOLAR ->
        com.example.model.Casta.entries.firstOrNull { it.displayName == npc.casta }?.allowedAbilities()?.toSet().orEmpty()
    com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO ->
        com.example.model.Aspecto.entries.firstOrNull { it.displayName == npc.casta }?.allowedAbilities()?.toSet().orEmpty()
    com.example.model.TipoExaltadoEncontro.LUNAR -> emptySet()
}

private fun atributoLunarEspecial(npc: NpcEncontro, nome: String): Boolean =
    npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR &&
        nome in (npc.lunarAtributosCasta + npc.habilidadesFavorecidas)

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
@Composable
internal fun NpcCardAttributes(npc: NpcEncontro, visualTemplate: ExaltVisualTemplate) {
// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
EncounterCardTitle("Atributos", color = visualTemplate.accentBright)
Spacer(modifier = Modifier.height(28.dp))
val compactPhone = LocalConfiguration.current.screenWidthDp < 480
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(if (compactPhone) 4.dp else 12.dp)
) {
    NPC_ATTRIBUTE_GROUPS.forEach { (_, nomes) ->
        Column(modifier = Modifier.weight(1f)) {
            nomes.forEach { nomeAtributo ->
                val valor = npc.attributes[nomeAtributo] ?: 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compactPhone) 2.dp else 5.dp)
                ) {
                    if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR) {
                        NpcSpecialIndicator(
                            marked = atributoLunarEspecial(npc, nomeAtributo),
                            npc = npc,
                            visualTemplate = visualTemplate,
                            compact = compactPhone
                        )
                    }
                    AutoSizeAppText(
                        text = "$nomeAtributo $valor",
                        modifier = Modifier.weight(1f),
                        style = if (compactPhone) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                        color = visualTemplate.onSurface,
                        maxFontSize = if (compactPhone) 12.sp else 16.sp,
                        minFontSize = 8.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
}

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
@Composable
internal fun NpcCardSkills(
    npc: NpcEncontro,
    visualTemplate: ExaltVisualTemplate,
    especialidadesSet: Set<String>
) {
// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
EncounterCardTitle("Habilidades", color = visualTemplate.accentBright)
Spacer(modifier = Modifier.height(28.dp))
run {
    val habilidadesOrdenadas = remember(npc.abilities, npc.tipoExaltado, npc.casta, npc.habilidadesFavorecidas) {
        val habilidadesEspeciais = if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR) {
            emptySet()
        } else {
            habilidadesDeCastaOuAspecto(npc) + npc.habilidadesFavorecidas.toSet()
        }
        npc.abilities.asSequence()
            .filter { it.value > 0 || it.key in habilidadesEspeciais }
            .map { it.key to it.value }
            .toList()
            .let { existentes: List<Pair<String, Int>> ->
                val presentes = existentes.asSequence().map { it.first }.toSet()
                val faltantes: List<Pair<String, Int>> = habilidadesEspeciais
                    .asSequence()
                    .filter { it !in presentes }
                    .map { habilidade -> habilidade to 0 }
                    .toList()
                val todas: List<Pair<String, Int>> = existentes + faltantes
                todas.sortedBy { it.first }
            }
    }
    val compactPhone = LocalConfiguration.current.screenWidthDp < 480
    val numeroColunas = if (compactPhone) 2 else 3
    val colunas = remember(habilidadesOrdenadas, numeroColunas) {
        val tamanhoColuna = kotlin.math.ceil(habilidadesOrdenadas.size / numeroColunas.toDouble()).toInt().coerceAtLeast(1)
        habilidadesOrdenadas.chunked(tamanhoColuna)
    }
    val habilidadesEspeciaisMarcadas = remember(npc.tipoExaltado, npc.casta, npc.habilidadesFavorecidas) {
        if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR) emptySet()
        else habilidadesDeCastaOuAspecto(npc) + npc.habilidadesFavorecidas.toSet()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compactPhone) 8.dp else 14.dp)
    ) {
        colunas.forEach { coluna ->
            Column(modifier = Modifier.weight(1f)) {
                coluna.forEach { (k, v) ->
                    val temEspecializacao = k in especialidadesSet
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (npc.tipoExaltado != com.example.model.TipoExaltadoEncontro.LUNAR) {
                            NpcSpecialIndicator(
                                marked = k in habilidadesEspeciaisMarcadas,
                                npc = npc,
                                visualTemplate = visualTemplate
                            )
                        }
                        AutoSizeAppText(
                            text = if (temEspecializacao) "$k $v (+1)" else "$k $v",
                            modifier = Modifier.weight(1f),
                            style = if (compactPhone) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                            color = visualTemplate.onSurface,
                            maxFontSize = if (compactPhone) 12.sp else 16.sp,
                            minFontSize = 8.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
}

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
@Composable
internal fun NpcCardMerits(
    npc: NpcEncontro,
    visualTemplate: ExaltVisualTemplate,
    findMeritDefinition: (String) -> com.example.data.MeritoDefinition?
) {
    val ehSangueDeDragaoInfo = npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO
val orcamentoMeritos = if (ehSangueDeDragaoInfo) 18 else 10
val totalMeritos = npc.merits.sumOf { it.valor }
// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
EncounterCardTitle("Méritos ($totalMeritos/$orcamentoMeritos)", color = visualTemplate.accentBright)
Spacer(modifier = Modifier.height(28.dp))
if (npc.merits.isEmpty()) {
    AppText("Nenhum Mérito.", style = MaterialTheme.typography.bodyLarge, color = visualTemplate.muted,
                    forceStroke = true
)
} else {
    val meritosOrdenados = remember(npc.merits) {
        npc.merits.sortedWith(compareBy<com.example.model.Merito> { it.nome }.thenBy { it.valor })
    }
    meritosOrdenados.forEach { merito ->
        var mostrarDescricao by remember(merito.id) { mutableStateOf(false) }
        Box {
            AppText(
                merito.linhaExibicao(),
                style = MaterialTheme.typography.bodyLarge,
                color = visualTemplate.onSurface,
                softWrap = false,
                modifier = Modifier
                    .pointerInput(merito.id) {
                        detectTapGestures(onLongPress = { mostrarDescricao = true })
                    }
                    .feedbackOnPress(),
                    forceStroke = true
)
            if (mostrarDescricao) {
                // Long press na Aba 11 deve abrir a mesma linguagem visual
                // da caixa de detalhe usada na Aba 6 — Méritos, em vez de
                // um Popup simples contendo apenas a descrição.
                val nomeCatalogo = when (merito.nome.trim().lowercase()) {
                    "artefato", "artefato (arma)", "artefato (armadura)" -> "Artefato"
                    else -> merito.nome
                }
                val definicao = findMeritDefinition(nomeCatalogo)
                NpcMeritDetailsDialog(
                    merito = merito,
                    definicao = definicao,
                    fallbackDescricao = merito.preRequisitoTexto.ifBlank { "Sem descrição disponível." },
                    visualTemplate = visualTemplate,
                    onDismiss = { mostrarDescricao = false }
                )
            }
        }
    }
}
}

@Composable
private fun NpcMeritDetailsDialog(
    merito: com.example.model.Merito,
    definicao: com.example.data.MeritoDefinition?,
    fallbackDescricao: String,
    visualTemplate: ExaltVisualTemplate,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = com.example.ui.components.gildedDialogBorder(),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText(
                merito.tituloExibicao(),
                color = visualTemplate.accentBright,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                    forceStroke = true
)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                com.example.ui.components.JustifiedBodyAppText(
                    text = definicao?.descricao ?: fallbackDescricao,
                    color = visualTemplate.onSurface,
                    forceStroke = true
)

                Spacer(modifier = Modifier.height(10.dp))
                AppText("Graduação:", fontWeight = FontWeight.Bold, color = ExaltedGold,
                    forceStroke = true
)
                AppText(
                    "${merito.valor} ${if (merito.valor == 1) "ponto" else "pontos"}",
                    color = visualTemplate.onSurface,
                    forceStroke = true
)

                val preRequisitos = definicao?.preRequisitos.orEmpty()
                if (preRequisitos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AppText("Pré-requisitos:", fontWeight = FontWeight.Bold, color = ExaltedGold,
                    forceStroke = true
)
                    preRequisitos.forEach { requisito ->
                        AppText("• $requisito", color = visualTemplate.onSurface, style = MaterialTheme.typography.bodySmall,
                    forceStroke = true
)
                    }
                }

                val custos = definicao?.custosPermitidos.orEmpty()
                if (custos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AppText("Custo:", fontWeight = FontWeight.Bold, color = ExaltedGold,
                    forceStroke = true
)
                    AppText(
                        custos.joinToString(" / ") { custo ->
                            if (custo == 0) "Grátis" else "$custo ${if (custo == 1) "ponto" else "pontos"}"
                        },
                        color = visualTemplate.onSurface,
                    forceStroke = true
)
                }
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(
                visualTemplate = visualTemplate,
                text = "Fechar",
                onClick = onDismiss
            )
        },
        containerColor = visualTemplate.surface,
        titleContentColor = visualTemplate.accentBright,
        textContentColor = visualTemplate.onSurface
    )
}
