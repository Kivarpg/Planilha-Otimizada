package com.example.ui.tabs
import com.example.data.EncounterMoteService

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.feedbackOnPress
import com.example.ui.components.feedbackClickable

import com.example.iniciativas.ajustarIniciativa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.AppText
import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArquetipoEncontro
import com.example.model.CaixaVitalidade
import com.example.model.HistoricoXpBatch
import com.example.model.NpcEncontro
import com.example.model.CharacterSheet
import com.example.ui.components.GildedCard
import com.example.ui.theme.visualTemplateParaExaltado
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface


internal val NPC_ATTRIBUTE_GROUPS = listOf(
    "Físico" to com.example.model.ExaltedConstants.PHYSICAL_ATTRIBUTES,
    "Social" to com.example.model.ExaltedConstants.SOCIAL_ATTRIBUTES,
    "Mental" to com.example.model.ExaltedConstants.MENTAL_ATTRIBUTES
)
private val NPC_DAMAGE_DELTAS_NEGATIVE = listOf(-1, -5, -10)
private val NPC_DAMAGE_DELTAS_POSITIVE = listOf(1, 5, 10)
private val NPC_HEALTH_PENALTY_ORDER = listOf("-0", "-1", "-2", "-4", "Inc")
private val NPC_MELEE_SKILLS = setOf("Armas Brancas", "Briga")

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Deriva apenas dados de apresentação. Não altera regras do NPC.
private data class NpcXpDisplayData(
    val todosEncantos: List<String>,
    val melhorias: List<HistoricoXpBatch>,
    val especializacoes: List<String>,
    val fvComprada: Int
)

private data class NpcDamageDisplay(
    val numeric: String,
    val suffix: String?
)

private fun parseNpcDamage(value: String): NpcDamageDisplay {
    var numeric = StringBuilder()
    var suffix = StringBuilder()
    value.forEach { char ->
        when {
            char.isDigit() -> numeric.append(char)
            char.isLetter() -> suffix.append(char)
        }
    }
    return NpcDamageDisplay(numeric.toString(), suffix.toString().ifBlank { null })
}

// Extraído do corpo de NpcEncontroCard (refatoração de organização —
// pedido explícito do usuário, sem mudança de comportamento). Padrão
// repetido 4 vezes (Investida, Desengajamento, Perseverança, Astúcia):
// "(Atributo (X) + Habilidade (Y) [+ Especialização (1)]) ÷ 2 (arred.
// pra cima) = resultado". Testado string por string contra os 4 usos
// originais antes de substituí-los.
private fun calculoMediaComEspecializacao(
    nomeAtributo: String,
    valorAtributo: Int,
    nomeHabilidade: String,
    valorHabilidade: Int,
    temEspecializacao: Boolean,
    resultado: Int
): String = "($nomeAtributo ($valorAtributo) + $nomeHabilidade ($valorHabilidade)" +
    (if (temEspecializacao) " + Especialização (1)" else "") +
    ") ÷ 2 (arred. pra cima) = $resultado"

internal fun buildEquipamentoDetalhesAba7(npc: NpcEncontro): String = buildString {
    // Aba 11 — Encontros: esta é a única fonte do conteúdo do long press
    // de Equipamento. A identidade do item vem do equipamento efetivamente
    // sorteado e as características são consultadas nos mesmos catálogos e
    // tabelas usados pela Aba 7.
    npc.arma?.let { arma ->
        appendLine("ARMA")
        appendLine("Nome: ${arma.nome}")
        appendLine("Tipo: ${arma.tipo}")
        appendLine("Peso: ${arma.peso}")
        appendLine("Habilidade: ${npc.habilidadePrincipal.ifBlank { "—" }}")

        val habilidade = npc.habilidadePrincipal
        val nomeCatalogo = arma.nome.substringBeforeLast(" (").trim()
        val catalogoArtefato = com.example.data.WeaponCatalog.candidatasPorHabilidade(habilidade)
            .firstOrNull { it.nome.equals(nomeCatalogo, ignoreCase = true) && it.peso == arma.peso }
        val catalogoMundano = com.example.data.WeaponCatalogMundano.candidatasPorHabilidade(habilidade)
            .firstOrNull { it.nome.equals(nomeCatalogo, ignoreCase = true) && it.peso == arma.peso }

        when {
            catalogoArtefato != null -> {
                appendLine("Fonte Aba 7: catálogo de armas Artefato — ${catalogoArtefato.nome}")
                appendLine("Características: ${catalogoArtefato.etiquetas.joinToString(", ").ifBlank { "Nenhuma" }}")
            }
            catalogoMundano != null -> {
                appendLine("Fonte Aba 7: catálogo de armas Mundanas — ${catalogoMundano.nome}")
                appendLine("Recursos: ${catalogoMundano.custoRecursos}")
                appendLine("Características: ${catalogoMundano.etiquetas.joinToString(", ").ifBlank { "Nenhuma" }}")
            }
            else -> appendLine("Fonte Aba 7: item correspondente não localizado no catálogo")
        }

        if (habilidade == "Arremesso" || habilidade == "Arqueirismo") {
            val stats = com.example.model.WeaponStatsTable.distancia(arma.tipo, arma.peso)
            appendLine("Precisão: —")
            appendLine("Dano: ${stats.first}")
            appendLine("Dano Mínimo: ${stats.second}")
            appendLine("Defesa: —")
            appendLine("Cometimento: ${com.example.model.WeaponStatsTable.comitamento(arma.tipo)} motes")
            val distancias = if (habilidade == "Arremesso") {
                com.example.model.WeaponStatsTable.distanciasArremesso(arma.tipo)
            } else {
                com.example.model.WeaponStatsTable.distanciasArqueirismo(arma.tipo)
            }
            appendLine("Distância: " + distancias.joinToString(" | ") { (nome, valor) ->
                "$nome ${if (valor >= 0) "+" else ""}$valor"
            })
        } else {
            val stats = com.example.model.WeaponStatsTable.corpoACorpo(arma.tipo, arma.peso)
            appendLine("Precisão: ${stats.precisao}")
            appendLine("Dano: ${stats.dano}")
            appendLine("Dano Mínimo: ${stats.danoMinimo}")
            appendLine("Defesa: ${stats.defesa}")
            appendLine("Cometimento: ${com.example.model.WeaponStatsTable.comitamento(arma.tipo)} motes")
        }
    }

    npc.armadura?.let { armadura ->
        if (isNotEmpty()) appendLine()
        appendLine("ARMADURA")
        appendLine("Nome: ${armadura.nome}")
        appendLine("Tipo: ${armadura.tipo}")
        appendLine("Peso: ${armadura.peso}")

        val nomeCatalogo = armadura.nome.substringBeforeLast(" (").trim()
        val catalogoArtefato = com.example.data.ArmorCatalog.candidatas(armadura.peso)
            .firstOrNull { it.nome.equals(nomeCatalogo, ignoreCase = true) }
        val catalogoMundano = com.example.data.ArmorCatalogMundano.candidatas(armadura.peso)
            .firstOrNull { it.nome.equals(nomeCatalogo, ignoreCase = true) }

        when {
            catalogoArtefato != null -> {
                appendLine("Fonte Aba 7: catálogo de armaduras Artefato — ${catalogoArtefato.nome}")
                appendLine("Custo de Mérito: ${catalogoArtefato.custoMerito}")
                appendLine("Características: ${catalogoArtefato.marcadores.joinToString(", ").ifBlank { "Nenhuma" }}")
            }
            catalogoMundano != null -> {
                appendLine("Fonte Aba 7: catálogo de armaduras Mundanas — ${catalogoMundano.nome}")
                appendLine("Recursos: ${catalogoMundano.custoRecursos}")
                appendLine("Características: ${catalogoMundano.etiquetas.joinToString(", ").ifBlank { "Nenhuma" }}")
            }
            else -> appendLine("Fonte Aba 7: item correspondente não localizado no catálogo")
        }

        val stats = com.example.model.ArmorStatsTable.stats(armadura.tipo, armadura.peso)
        appendLine("Absorção: ${stats.absorcao}")
        appendLine("Dureza: ${stats.dureza}")
        appendLine("Penalidade de Mobilidade: ${stats.penalidadeMobilidade}")
        appendLine("Cometimento: ${stats.comitamento} motes")
    }

    if (npc.arma == null && npc.armadura == null) {
        append("Nenhum equipamento sorteado para este NPC.")
    }
}.ifBlank { "Nenhum equipamento sorteado para este NPC." }


@Composable
private fun EquipamentoDetalhesAba7(
    npc: NpcEncontro
) {
    val arma = npc.arma
    val armadura = npc.armadura

    arma?.let {
        AppText("ARMA", color = ExaltedGold, fontWeight = FontWeight.Bold)
        AppText("Nome: ${it.nome}", color = ExaltedOnSurface)
        AppText("Habilidade: ${npc.habilidadePrincipal.ifBlank { "—" }}", color = ExaltedOnSurface)
        AppText("Tipo: ${it.tipo}", color = ExaltedOnSurface)
        AppText("Peso: ${it.peso}", color = ExaltedOnSurface)
        AppText(
            "Estatísticas de combate",
            color = ExaltedAmber,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        val ehDistancia = npc.habilidadePrincipal == "Arremesso" || npc.habilidadePrincipal == "Arqueirismo"
        val statsDistancia = if (ehDistancia) {
            com.example.model.WeaponStatsTable.distancia(it.tipo, it.peso)
        } else null
        val statsCorpo = if (!ehDistancia) {
            com.example.model.WeaponStatsTable.corpoACorpo(it.tipo, it.peso)
        } else null

        AppText("Precisão: ${if (ehDistancia) "—" else it.precisao}", color = ExaltedOnSurface)
        AppText("Dano: ${if (ehDistancia) statsDistancia!!.first else it.dano}", color = ExaltedOnSurface)
        AppText("Dano Mínimo: ${if (ehDistancia) statsDistancia!!.second else statsCorpo!!.danoMinimo}", color = ExaltedOnSurface)
        AppText("Defesa: ${if (ehDistancia) "—" else it.defesa}", color = ExaltedOnSurface)
        AppText("Cometimento: ${com.example.model.WeaponStatsTable.comitamento(it.tipo)} motes", color = ExaltedOnSurface)

        if (ehDistancia) {
            val distancias = if (npc.habilidadePrincipal == "Arremesso") {
                com.example.model.WeaponStatsTable.distanciasArremesso(it.tipo)
            } else {
                com.example.model.WeaponStatsTable.distanciasArqueirismo(it.tipo)
            }
            AppText(
                "Distância: " + distancias.joinToString(" | ") { (nome, valor) ->
                    "$nome ${if (valor >= 0) "+" else ""}$valor"
                },
                color = ExaltedOnSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (it.etiquetas.isNotEmpty()) {
            AppText(
                "Etiquetas: ${it.etiquetas.joinToString(", ")}",
                color = ExaltedMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    if (arma != null && armadura != null) {
        Spacer(modifier = Modifier.height(12.dp))
    }

    armadura?.let {
        AppText("ARMADURA", color = ExaltedGold, fontWeight = FontWeight.Bold)
        AppText("Nome: ${it.nome}", color = ExaltedOnSurface)
        AppText("Tipo: ${it.tipo}", color = ExaltedOnSurface)
        AppText("Peso: ${it.peso}", color = ExaltedOnSurface)
        AppText(
            "Estatísticas de defesa",
            color = ExaltedAmber,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        val stats = com.example.model.ArmorStatsTable.stats(it.tipo, it.peso)
        AppText("Absorção: ${stats.absorcao}", color = ExaltedOnSurface)
        AppText("Dureza: ${stats.dureza}", color = ExaltedOnSurface)
        AppText("Penalidade: ${stats.penalidadeMobilidade}", color = ExaltedOnSurface)
        AppText("Cometimento: ${stats.comitamento} motes", color = ExaltedOnSurface)
        if (it.marcadores.isNotEmpty()) {
            AppText(
                "Características: ${it.marcadores.joinToString(", ")}",
                color = ExaltedMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    if (arma == null && armadura == null) {
        AppText("Nenhum equipamento sorteado para este NPC.", color = ExaltedOnSurface)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NpcEncontroCard(
    npc: NpcEncontro,
    sheet: CharacterSheet,
    viewModel: com.example.viewmodel.SheetViewModel,
    gerando: Boolean,
    onCiclarDano: (String) -> Unit,
    onLimparDano: () -> Unit,
    onAjustarIniciativa: (Int) -> Unit,
    onJuntarSeABatalha: () -> Boolean,
    onAumentarExperiencia: () -> Unit,
    onDiminuirExperiencia: () -> Unit
) {
    var mostrarConfirmacaoJuntarSe by remember { mutableStateOf(false) }
    // Sincronização Aba 11 <-> Aba 12 (pedido explícito do usuário): quando
    // este NPC está em combate na Aba 12 (existe um ParticipanteIniciativa
    // com origemNpcId == npc.id), a iniciativa exibida/editada aqui É a
    // mesma da Aba 12 — não um valor independente. Só volta a usar
    // npc.iniciativaAtual (o próprio rastreador da Aba 11) quando o NPC
    // não está em nenhum combate ativo.
    val estadoIniciativas by viewModel.iniciativasController.state.collectAsState()
    val participanteEmCombate = estadoIniciativas.participantes.firstOrNull { it.origemNpcId == npc.id }
    val iniciativaSincronizada = participanteEmCombate?.iniciativa ?: npc.iniciativaAtual
    val ajustarIniciativaSincronizada: (Int) -> Unit = { delta ->
        if (participanteEmCombate != null) {
            viewModel.iniciativasController.ajustarIniciativa(participanteEmCombate.id, delta)
        } else {
            onAjustarIniciativa(delta)
        }
    }

    val visualTemplate = visualTemplateParaExaltado(npc.tipoExaltado)
    val especialidadesSet = remember(npc.especialidades) {
        npc.especialidades.asSequence().map { it.habilidade }.toSet()
    }
    val danoDisplay = remember(npc.dano) { parseNpcDamage(npc.dano) }

    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // A aparência deste NPC depende exclusivamente de npc.tipoExaltado; a
    // planilha/aba atualmente aberta pelo jogador não participa desta decisão.
    GildedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = visualTemplate.surfaceVariant),
        border = null,
        visualTemplate = visualTemplate
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var mostrarCarregar by remember { mutableStateOf(false) }
        var mostrarEditorEquipamento by remember { mutableStateOf(false) }
        var boxParaRemover by remember { mutableStateOf<com.example.model.CaixaVitalidade?>(null) }
        val compactPhone = LocalConfiguration.current.screenWidthDp < 480
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (compactPhone) 50.dp else 0.dp)
        ) {
            val ehSangueDeDragaoInfo = npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO
            val ehLunarInfo = npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR
            // APPROVED VISUAL CUSTOMIZATION
            // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
            // O nome do NPC é o título principal da planilha e deve se destacar
            // visualmente mais que os textos de conteúdo abaixo. Logo
            // abaixo, só a classificação do Tipo de Exaltado (Solar/Sangue
            // de Dragão/Lunar), sem rótulo — pedido explícito do usuário.
            // Casta/Aspecto permanece na seção seguinte.
            val simboloGenero = when (npc.genero.uppercase()) {
                "FEMININO" -> "♀"
                "MASCULINO" -> "♂"
                else -> ""
            }
            val nomeComGenero = if (simboloGenero.isNotEmpty()) {
                "${npc.nome} ($simboloGenero)"
            } else {
                npc.nome
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.AutoSizeText(
                    text = nomeComGenero,
                    style = MaterialTheme.typography.headlineSmall,
                    maxFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    minFontSize = 10.sp,
                    color = visualTemplate.accentBright,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            if (ehLunarInfo) {
                // Lunar segue uma estrutura fixa na Aba 11: nome, tipo, casta,
                // essência, forma espiritual, sinal e idioma. O sinal é apenas
                // exibido; ele já é sorteado uma vez durante a geração do NPC.
                AppText(
                    "Tipo: Lunar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
                AppText(
                    "Casta: ${npc.casta}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
                AppText(
                    "Essência: ${npc.essencia}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
                AppText(
                    "Forma Espiritual: ${npc.formaEspiritual.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
                AppText(
                    "Sinal: ${npc.sinal.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
                AppText(
                    "Limite: ${npc.limite.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
                AppText(
                    "Idioma: ${npc.idioma}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
            } else {
                AppText(
                    when (npc.tipoExaltado) {
                        com.example.model.TipoExaltadoEncontro.SOLAR -> "Solar"
                        com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> "Sangue de Dragão"
                        com.example.model.TipoExaltadoEncontro.LUNAR -> "Lunar"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = visualTemplate.onSurface,
                    forceStroke = true
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppText(
                        "${if (ehSangueDeDragaoInfo) "Aspecto" else "Casta"}: ${npc.casta}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                    AppText(
                        "Essência: ${npc.essencia}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                    if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.SOLAR) {
                        AppText(
                            "Limite: ${npc.limite.ifBlank { "—" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = visualTemplate.onSurface,
                            forceStroke = true
                        )
                    }
                    AppText(
                        "Idioma: ${npc.idioma}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            NpcCardCarregarDialog(
                visible = mostrarCarregar,
                visualTemplate = visualTemplate,
                onCarregarNpc = { carregado ->
                    viewModel.carregarNpcDeArquivo(npc.id, carregado)
                    mostrarCarregar = false
                },
                onDismiss = { mostrarCarregar = false }
            )
            boxParaRemover?.let { box ->
                com.example.ui.components.ConfirmDeleteDialog(
                    itemTitle = "Caixa Extra (${box.penalidade})",
                    onConfirm = {
                        viewModel.removeExtraHealthBoxNpcEncontro(npc.id, box.id)
                        boxParaRemover = null
                    },
                    onDismiss = { boxParaRemover = null }
                )
            }
            NpcCardAttributes(npc, visualTemplate)
            Spacer(modifier = Modifier.height(18.dp))
            NpcCardSkills(npc, visualTemplate, especialidadesSet)
            Spacer(modifier = Modifier.height(18.dp))
            NpcCardMerits(npc, visualTemplate, viewModel::meritoDefinitionPorNome)

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                com.example.ui.components.GildedDialogButton(
                    text = "Juntar-se à Batalha",
                    onClick = { mostrarConfirmacaoJuntarSe = true },
                    visualTemplate = visualTemplate
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            val penalidade = npc.penalidadeFerimentoAtual() + npc.penalidadeClashDefesa + npc.penalidadeAtaquesDefesa
            val destrezaNpc = npc.attributes["Destreza"] ?: 1
            // Contador simples de linha, pra alternar o fundo zebrado — é uma
            // var local comum (não remember), porque precisa reiniciar do
            // zero em toda passada de composição desta função, nunca
            // acumular entre elas. Corrige o listrado errado que aparecia
            // quando linhas como Aparar/Evasão passaram a ser exibidas (ou
            // omitidas) condicionalmente conforme a perícia do NPC: ver
            // comentário em LinhaInfo/corZebrada (EncounterNpcCardComponents.kt)
            // para a explicação completa.
            var zebra = 0
            val totaisMotes = EncounterMoteService.totais(npc.tipoExaltado, npc.essencia, npc.arma, npc.armadura)
            val motesComitadosEquipamento = totaisMotes.comitados
            val tipoArmaTooltip = when {
                npc.arma == null -> "Desarmado"
                npc.habilidadePrincipal == "Arremesso" -> "Arremesso (${npc.arma.nome})"
                npc.habilidadePrincipal == "Arqueirismo" -> "Arqueirismo (${npc.arma.nome})"
                else -> "Arma Branca (${npc.arma.nome})"
            }
            // APPROVED VISUAL CUSTOMIZATION
            // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
            EncounterCardTitle("Ações", color = visualTemplate.accentBright)
            Spacer(modifier = Modifier.height(14.dp))
            when (npc.arquetipo) {
                ArquetipoEncontro.FISICO -> {
                    val habValor = npc.abilities[npc.habilidadePrincipal] ?: 0
                    val ehArmaBrancaOuBriga = npc.habilidadePrincipal in NPC_MELEE_SKILLS
                    if (ehArmaBrancaOuBriga) {
                        val precisaoArma = npc.arma?.precisao ?: 0
                        LinhaInfo(
                            "Ataque Fulminante", "${npc.acaoPrincipal}", zebra++,
                            calculo = "Destreza ($destrezaNpc) + ${npc.habilidadePrincipal} ($habValor) + Especialização (1) + Precisão da Arma ($precisaoArma) = ${npc.acaoPrincipal}",
                            visualTemplate = visualTemplate
                        )
                        LinhaInfo(
                            "Dano", danoDisplay.numeric, zebra++,
                            calculo = "Força (${npc.attributes["Força"] ?: 1}) + Dano da Arma (${npc.arma?.dano ?: 0}) = ${npc.dano}",
                            prefixo = if (npc.dano.startsWith("+")) "+" else null,
                            sufixo = danoDisplay.suffix,
                            visualTemplate = visualTemplate
                        )
                        LinhaInfo(
                            "Ataque Decisivo", "${npc.acaoDecisiva}", zebra++,
                            calculo = "Destreza ($destrezaNpc) + ${npc.habilidadePrincipal} ($habValor) + Especialização (1) = ${npc.acaoDecisiva}",
                            visualTemplate = visualTemplate
                        )
                        val defesaArma = npc.arma?.defesa ?: 0
                        LinhaInfo(
                            "Aparar", (npc.defesaPrimaria ?: 0).let { (it - penalidade).coerceAtLeast(0) }.toString(), zebra++,
                            calculo = "(Destreza ($destrezaNpc) + ${npc.habilidadePrincipal} ($habValor) + Especialização (1)) ÷ 2 (arred. pra cima) + Defesa da Arma ($defesaArma)" +
                                (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                                " = ${((npc.defesaPrimaria ?: 0) - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                        val esquivaHab = npc.abilities["Esquiva"] ?: 0
                        val mobilidade = npc.armadura?.penalidadeMobilidade ?: 0
                        LinhaInfo(
                            "Evasão", (npc.esquiva - penalidade).coerceAtLeast(0).toString(), zebra++,
                            calculo = "(Destreza ($destrezaNpc) + Esquiva ($esquivaHab) + Especialização (1)) ÷ 2 (arred. pra cima) − Mobilidade ($mobilidade)" +
                                (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                                " = ${(npc.esquiva - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                    } else {
                        LinhaInfo(
                            "Ataque Fulminante", "${npc.acaoPrincipal}", zebra++,
                            calculo = "Destreza ($destrezaNpc) + ${npc.habilidadePrincipal} ($habValor) + Especialização (1) = ${npc.acaoPrincipal}",
                            visualTemplate = visualTemplate
                        )
                        LinhaInfo(
                            "Dano", danoDisplay.numeric, zebra++,
                            calculo = "Dano da Arma (${npc.arma?.dano ?: 0}) = ${npc.dano}",
                            prefixo = if (npc.dano.startsWith("+")) "+" else null,
                            sufixo = danoDisplay.suffix,
                            visualTemplate = visualTemplate
                        )
                        LinhaInfo(
                            "Ataque Decisivo", "${npc.acaoDecisiva}", zebra++,
                            calculo = "Destreza ($destrezaNpc) + ${npc.habilidadePrincipal} ($habValor) + Especialização (1) = ${npc.acaoDecisiva}",
                            visualTemplate = visualTemplate
                        )
                        val brigaHab = npc.abilities["Briga"] ?: 0
                        val defesaArma = npc.arma?.defesa ?: 0
                        LinhaInfo(
                            "Aparar", (npc.defesaPrimaria ?: 0).let { (it - penalidade).coerceAtLeast(0) }.toString(), zebra++,
                            calculo = "(Destreza ($destrezaNpc) + Briga ($brigaHab) + Especialização (1)) ÷ 2 (arred. pra cima) + Defesa da Arma ($defesaArma)" +
                                (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                                " = ${((npc.defesaPrimaria ?: 0) - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                        val esquivaHab = npc.abilities["Esquiva"] ?: 0
                        val mobilidade = npc.armadura?.penalidadeMobilidade ?: 0
                        LinhaInfo(
                            "Evasão", (npc.esquiva - penalidade).coerceAtLeast(0).toString(), zebra++,
                            calculo = "(Destreza ($destrezaNpc) + Esquiva ($esquivaHab) + Especialização (1)) ÷ 2 (arred. pra cima) − Mobilidade ($mobilidade)" +
                                (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                                " = ${(npc.esquiva - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                    }
                }
                ArquetipoEncontro.SOCIAL -> {
                    val habValor = npc.abilities[npc.habilidadePrincipal] ?: 0
                    LinhaInfo(
                        "Ataque Fulminante", "${npc.acaoPrincipal}", zebra++,
                        calculo = "Destreza ($destrezaNpc) + ${npc.habilidadePrincipal} ($habValor) + Especialização (1) = ${npc.acaoPrincipal}",
                            visualTemplate = visualTemplate
                        )
                    val brigaHab = npc.abilities["Briga"] ?: 0
                    val defesaArma = npc.arma?.defesa ?: 0
                    LinhaInfo(
                        "Aparar", (npc.defesaPrimaria ?: 0).let { (it - penalidade).coerceAtLeast(0) }.toString(), zebra++,
                        calculo = "(Destreza ($destrezaNpc) + Briga ($brigaHab) + Especialização (1)) ÷ 2 (arred. pra cima) + Defesa da Arma ($defesaArma)" +
                            (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                            " = ${((npc.defesaPrimaria ?: 0) - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                    val esquivaHab = npc.abilities["Esquiva"] ?: 0
                    val mobilidade = npc.armadura?.penalidadeMobilidade ?: 0
                    LinhaInfo(
                        "Evasão", (npc.esquiva - penalidade).coerceAtLeast(0).toString(), zebra++,
                        calculo = "(Destreza ($destrezaNpc) + Esquiva ($esquivaHab) + Especialização (1)) ÷ 2 (arred. pra cima) − Mobilidade ($mobilidade)" +
                            (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                            " = ${(npc.esquiva - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                }
                ArquetipoEncontro.MENTAL -> {
                    val habValor = npc.abilities[npc.habilidadePrincipal] ?: 0
                    LinhaInfo(
                        "Ataque Fulminante", "${npc.acaoPrincipal}", zebra++,
                        calculo = "Destreza ($destrezaNpc) + ${npc.habilidadePrincipal} ($habValor) + Especialização (1) = ${npc.acaoPrincipal}",
                            visualTemplate = visualTemplate
                        )
                    val brigaHab = npc.abilities["Briga"] ?: 0
                    val defesaArma = npc.arma?.defesa ?: 0
                    LinhaInfo(
                        "Aparar", (npc.defesaPrimaria ?: 0).let { (it - penalidade).coerceAtLeast(0) }.toString(), zebra++,
                        calculo = "(Destreza ($destrezaNpc) + Briga ($brigaHab) + Especialização (1)) ÷ 2 (arred. pra cima) + Defesa da Arma ($defesaArma)" +
                            (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                            " = ${((npc.defesaPrimaria ?: 0) - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                    val esquivaHab = npc.abilities["Esquiva"] ?: 0
                    val mobilidade = npc.armadura?.penalidadeMobilidade ?: 0
                    LinhaInfo(
                        "Evasão", (npc.esquiva - penalidade).coerceAtLeast(0).toString(), zebra++,
                        calculo = "(Destreza ($destrezaNpc) + Esquiva ($esquivaHab) + Especialização (1)) ÷ 2 (arred. pra cima) − Mobilidade ($mobilidade)" +
                            (if (penalidade > 0) " − Penalidade($penalidade)" else "") +
                            " = ${(npc.esquiva - penalidade).coerceAtLeast(0)}",
                            visualTemplate = visualTemplate
                        )
                }
            }
            LinhaInfo(
                "Absorção", npc.absorcao.toString(), zebra++,
                calculo = "Vigor (${npc.attributes["Vigor"] ?: 1}) + Absorção da Armadura (${npc.armadura?.absorcao ?: 0}) = ${npc.absorcao}" +
                    (npc.armadura?.let { " · ${it.nome} — Mobilidade −${it.penalidadeMobilidade}" } ?: ""),
                            visualTemplate = visualTemplate
                        )
            val emCrash = participanteEmCombate != null && iniciativaSincronizada <= 0
            LinhaInfo(
                "Dureza",
                if (emCrash) "0" else npc.dureza.toString(),
                zebra++,
                calculo = if (emCrash)
                    "Iniciativa($iniciativaSincronizada) <= 0 → Dureza considerada 0"
                else
                    "Dureza da Armadura(${npc.armadura?.dureza ?: 0}) = ${npc.dureza}",
                            visualTemplate = visualTemplate
                        )
            val atletismoHab = npc.abilities["Atletismo"] ?: 0
            val temEspecialidadeAtletismo = "Atletismo" in especialidadesSet
            LinhaInfo(
                "Investida", npc.investida.toString(), zebra++,
                calculo = calculoMediaComEspecializacao("Destreza", destrezaNpc, "Atletismo", atletismoHab, temEspecialidadeAtletismo, npc.investida),
                            visualTemplate = visualTemplate
                        )
            val esquivaParaDesengajar = npc.abilities["Esquiva"] ?: 0
            val temEspecialidadeEsquivaDesengajar = "Esquiva" in especialidadesSet
            LinhaInfo(
                "Desengajamento", npc.desengajamento.toString(), zebra++,
                calculo = calculoMediaComEspecializacao("Destreza", destrezaNpc, "Esquiva", esquivaParaDesengajar, temEspecialidadeEsquivaDesengajar, npc.desengajamento),
                            visualTemplate = visualTemplate
                        )
            val temEspecialidadeIntegridade = "Integridade" in especialidadesSet
            val raciocinioPerseveranca = npc.attributes["Raciocínio"] ?: 1
            val integridadePerseveranca = npc.abilities["Integridade"] ?: 0
            LinhaInfo(
                "Perseverança", npc.perseveranca.toString(), zebra++,
                calculo = calculoMediaComEspecializacao("Raciocínio", raciocinioPerseveranca, "Integridade", integridadePerseveranca, temEspecialidadeIntegridade, npc.perseveranca),
                            visualTemplate = visualTemplate
                        )
            val temEspecialidadeSocializacao = "Socialização" in especialidadesSet
            val manipulacaoAstucia = npc.attributes["Manipulação"] ?: 1
            val socializacaoAstucia = npc.abilities["Socialização"] ?: 0
            LinhaInfo(
                "Astúcia", npc.astucia.toString(), zebra++,
                calculo = calculoMediaComEspecializacao("Manipulação", manipulacaoAstucia, "Socialização", socializacaoAstucia, temEspecialidadeSocializacao, npc.astucia),
                            visualTemplate = visualTemplate
                        )
            // Mantém os recursos de motes fora da sequência de Ações, seguindo a organização
            // do modelo 001 - V'neef Faqiang: após todos os campos de ação.
            val formulaPessoais = when (npc.tipoExaltado) {
                com.example.model.TipoExaltadoEncontro.SOLAR -> "10 + (Essência (${npc.essencia}) × 3)"
                com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> "11 + Essência (${npc.essencia})"
                com.example.model.TipoExaltadoEncontro.LUNAR -> "15 + Essência (${npc.essencia})"
            }
            val formulaPerifericos = when (npc.tipoExaltado) {
                com.example.model.TipoExaltadoEncontro.SOLAR -> "26 + (Essência (${npc.essencia}) × 7)"
                com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> "23 + (Essência (${npc.essencia}) × 4)"
                com.example.model.TipoExaltadoEncontro.LUNAR -> "34 + (Essência (${npc.essencia}) × 4)"
            }
            LinhaInfo(
                "Motes Pessoais", npc.motesPersonais.toString(), zebra++,
                calculo = "$formulaPessoais = ${totaisMotes.pessoaisMax}; Comitado = 0",
                visualTemplate = visualTemplate
            )
            LinhaInfo(
                "Motes Periféricos", npc.motesPerifericos.toString(), zebra++,
                calculo = "$formulaPerifericos = ${totaisMotes.perifericosMax}; Comitado = $motesComitadosEquipamento; Disponível = ${totaisMotes.perifericosDisponiveis}",
                visualTemplate = visualTemplate
            )
            val equipamentoDetalhes = buildEquipamentoDetalhesAba7(npc)
            LinhaInfo(
                "Equipamento",
                "",
                zebra++,
                calculoLongPress = equipamentoDetalhes,
                onTapOverride = { mostrarEditorEquipamento = true },
                longPressTitulo = "Equipamentos",
                longPressContent = { EquipamentoDetalhesAba7(npc) },
                visualTemplate = visualTemplate
            )
            EditarEquipamentoNpcDialog(
                visible = mostrarEditorEquipamento,
                npc = npc,
                detalhesAtuais = equipamentoDetalhes,
                visualTemplate = visualTemplate,
                onAtualizarArma = { novaArma -> viewModel.atualizarArmaNpcEncontro(npc.id, novaArma) },
                onAtualizarArmadura = { novaArmadura -> viewModel.atualizarArmaduraNpcEncontro(npc.id, novaArmadura) },
                onDismiss = { mostrarEditorEquipamento = false }
            )
            val historicoXp = remember(npc.historicoXpBatches) { npc.historicoXpBatches }
            // APPROVED PERFORMANCE REFACTOR
            // Um único percurso do histórico produz todos os dados exibidos
            // nesta seção. Antes eram quatro percursos independentes
            // (flatMap/filter/mapNotNull/sumOf), todos sobre a mesma lista.
            val xpDisplay = remember(historicoXp) {
                val todosEncantos = ArrayList<String>()
                val melhorias = ArrayList<HistoricoXpBatch>()
                val especializacoes = ArrayList<String>()
                var fvComprada = 0
                historicoXp.forEach { lote ->
                    todosEncantos.addAll(lote.nomesEncantosAdicionados)
                    if (lote.habilidadeMelhorada != null) melhorias.add(lote)
                    lote.especializacaoAdicionada?.let(especializacoes::add)
                    fvComprada += lote.pontosForcaDeVontadeComprados
                }
                NpcXpDisplayData(
                    todosEncantos = todosEncantos,
                    melhorias = melhorias,
                    especializacoes = especializacoes,
                    fvComprada = fvComprada
                )
            }
            val todosEncantos = xpDisplay.todosEncantos
            val melhorias = xpDisplay.melhorias
            val especializacoes = xpDisplay.especializacoes
            val fvComprada = xpDisplay.fvComprada
            val detalhamentoXp = buildString {
                if (todosEncantos.isNotEmpty()) {
                    appendLine("Encantos adquiridos:")
                    todosEncantos.forEach { appendLine("• $it") }
                }
                if (melhorias.isNotEmpty()) {
                    if (todosEncantos.isNotEmpty()) appendLine()
                    appendLine("Habilidade/Atributo melhorado:")
                    melhorias.forEach { lote -> appendLine("• ${lote.habilidadeMelhorada} (+${lote.pontosGanhosNaHabilidade})") }
                }
                if (especializacoes.isNotEmpty()) {
                    if (todosEncantos.isNotEmpty() || melhorias.isNotEmpty()) appendLine()
                    appendLine("Especializações adicionadas:")
                    especializacoes.forEach { appendLine("• $it") }
                }
                if (fvComprada > 0) {
                    if (isNotEmpty()) appendLine()
                    appendLine("Força de Vontade: +$fvComprada ponto(s)")
                }
            }.ifBlank { "Esta planilha ainda não utilizou nenhum ponto de experiência." }

            Spacer(modifier = Modifier.height(18.dp))
            EncounterCardTitle("Força de Vontade", color = visualTemplate.accentBright)
            Spacer(modifier = Modifier.height(14.dp))
            com.example.ui.components.WillpowerTrack(
                valor = npc.forcaDeVontade,
                usados = npc.forcaDeVontadeUsados,
                planilhaConcluida = true,
                onValorChange = { novoValor -> viewModel.updateForcaDeVontadeBaseNpcEncontro(npc.id, novoValor) },
                onToggleUsado = { i -> viewModel.toggleForcaDeVontadeNpcEncontro(npc.id, i) },
                minValor = 5,
                maxValor = 10,
                visualTemplate = visualTemplate
            )

            Spacer(modifier = Modifier.height(18.dp))
            EncounterCardTitle("Trilha de Vitalidade", color = visualTemplate.accentBright)
            // Separação adicional entre o cabeçalho e a primeira linha (-0).
            Spacer(modifier = Modifier.height(18.dp))
            // Mesmo padrão exato da Aba 5 (Combate) — pedido explícito do
            // usuário: agrupa por penalidade (rótulo aparece uma vez por
            // linha, não espremido dentro de cada caixa — era a causa do
            // número "preso"/invisível antes), cores por tipo de dano, e
            // reorganização automática de verdade: a ordem é recalculada
            // a cada renderização a partir de npc.healthBoxes, nunca
            // guardada separadamente, então apagar uma caixa já reflete
            // na hora, sem nenhum código extra de "reorganizar".
            // APPROVED PERFORMANCE REFACTOR
            // A ordenação e o agrupamento dependem exclusivamente da trilha
            // de vitalidade. Mudanças em iniciativa, Essência, vontade ou
            // outros campos do NPC não precisam repetir esse processamento.
            val gruposPorPenalidadeNpc = remember(npc.healthBoxes) {
                // APPROVED PERFORMANCE REFACTOR
                // A trilha possui apenas cinco classes de penalidade. Em vez de
                // ordenar a lista e depois agrupá-la, distribuímos cada caixa
                // diretamente no seu grupo. A inserção sequencial preserva a
                // ordem original entre caixas com a mesma penalidade.
                val grupos = Array(5) { ArrayList<CaixaVitalidade>() }
                for (box in npc.healthBoxes) {
                    val rank = box.penaltyRank()
                    if (rank in 0..4) grupos[rank].add(box)
                }
                grupos
            }
            val ordemPenalidadesNpc = NPC_HEALTH_PENALTY_ORDER
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ordemPenalidadesNpc.forEach { penalidadeRotulo ->
                    val caixasDoGrupo = gruposPorPenalidadeNpc[ordemPenalidadesNpc.indexOf(penalidadeRotulo)]
                    if (caixasDoGrupo.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.weight(1f)) {
                                val labelWidth = 36.dp
                                val boxSize = 40.dp
                                val gap = 6.dp
                                val availableForBoxes = (maxWidth - labelWidth).value.coerceAtLeast(boxSize.value)
                                val maxPerRow = ((availableForBoxes + gap.value) / (boxSize.value + gap.value)).toInt().coerceAtLeast(1)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    caixasDoGrupo.chunked(maxPerRow).forEachIndexed { rowIndex, rowBoxes ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (rowIndex == 0) {
                                                com.example.ui.components.AutoSizeText(
                                                    text = if (penalidadeRotulo == "Inc") "Inc." else penalidadeRotulo,
                                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                                    maxFontSize = MaterialTheme.typography.titleMedium.fontSize, minFontSize = 10.sp,
                                                    color = visualTemplate.gold, modifier = Modifier.width(labelWidth)
                                                )
                                            } else Spacer(modifier = Modifier.width(labelWidth))
                                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                                rowBoxes.forEach { box ->
                                                    val (textoDano, corDano) = when (box.tipoDano) {
                                                        1 -> "/" to com.example.ui.theme.ExaltedDanoContundente
                                                        2 -> "X" to com.example.ui.theme.ExaltedDanoLetal
                                                        3 -> "*" to com.example.ui.theme.ExaltedDanoAgravado
                                                        else -> "" to visualTemplate.muted
                                                    }
                                                    val ehExtra = !box.isPermanente
                                                    Box(
                                                        modifier = Modifier.size(boxSize).clip(RoundedCornerShape(6.dp)).background(visualTemplate.surface)
                                                            .border(if (ehExtra) 2.dp else 1.dp, if (ehExtra) visualTemplate.accent else corDano.takeIf { box.tipoDano != 0 } ?: visualTemplate.gold.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                            .pointerInput(box.id) { detectTapGestures(onTap = { onCiclarDano(box.id) }, onLongPress = { if (ehExtra) boxParaRemover = box }) }
                                                            .feedbackOnPress(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AppText(textoDano, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = corDano, forceStroke = true)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (penalidadeRotulo == "Inc") {
                                Spacer(modifier = Modifier.width(8.dp))
                                InkButton(
                                    visualTemplate = visualTemplate, label = "Limpar", onClick = onLimparDano,
                                    enabled = npc.healthBoxes.any { it.tipoDano != 0 }, size = InkButtonSize.Small,
                                    variant = InkButtonVariant.Secondary, brushIndex = 0
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            var gavetasExpandidas by remember(npc.id) { mutableStateOf(setOf<String>()) }
            // Na Aba 11, Solares e Sangue de Dragão são organizados pela
            // Habilidade vinculada. Lunares seguem a mesma apresentação em
            // gavetas da Aba 8, mas a chave da gaveta é o Atributo do Encanto.
            // A resolução pelo catálogo também corrige NPCs Lunares antigos
            // cujo valor persistido em habilidadeVinculada esteja incompleto.
            // Ordenação dinâmica das gavetas: mais Encantos primeiro;
            // empate por nome. A chave de remember inclui npc.charms, então
            // qualquer mudança de quantidade recalcula a ordem.
            val encantosPorHabilidade = remember(viewModel, npc.charms, npc.tipoExaltado) {
                npc.charms
                    .groupBy { encanto ->
                        if (npc.tipoExaltado == com.example.model.TipoExaltadoEncontro.LUNAR) {
                            viewModel.encantoDefinitionPorNomeParaTipo(encanto.nome, npc.tipoExaltado)
                                ?.habilidade
                                ?.takeIf { it.isNotBlank() }
                                ?: encanto.habilidadeVinculada
                        } else {
                            encanto.habilidadeVinculada
                        }
                    }
                    .entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String, List<com.example.model.EncantoEncontro>>> { it.value.size }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.key }
                    )
            }
            EncounterCardTitle(
                "Encantos (${npc.charms.size})",
                color = visualTemplate.accentBright
            )
            Spacer(modifier = Modifier.height(14.dp))
            encantosPorHabilidade.forEach { (habilidade, encantosDaGaveta) ->
                val expandida = habilidade in gavetasExpandidas
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .feedbackClickable {
                            gavetasExpandidas = if (expandida) gavetasExpandidas - habilidade else gavetasExpandidas + habilidade
                        }
                ) {
                    AppText(if (expandida) "▾" else "▸", color = visualTemplate.gold, modifier = Modifier.padding(end = 6.dp),
                    forceStroke = true
)
                    AppText(
                        "$habilidade (${encantosDaGaveta.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        // O título é o nível hierárquico da gaveta: usa a cor
                        // própria do Tipo de Exaltado sem o contorno branco.
                        // Os nomes dos Encantos permanecem em onSurface.
                        color = visualTemplate.accentBright,
                        forceStroke = false
                    )
                }
                if (expandida) {
                    // Encantos legalmente repetíveis são consolidados visualmente
                    // em uma única linha. A quantidade representa quantas vezes
                    // o NPC adquiriu o mesmo Encanto nesta gaveta. Encantos que
                    // não são repetíveis continuam em linhas independentes.
                    groupAccumulatedEncounterCharms(encantosDaGaveta).forEachIndexed { indice, grupo ->
                        val c = grupo.charm
                        var mostrarDetalhes by androidx.compose.runtime.remember(npc.id, c.nome) { androidx.compose.runtime.mutableStateOf(false) }
                        val nomeExibicao = "${indice + 1}. ${formatGroupedEncounterCharmName(grupo)}"
                        AppText(
                            nomeExibicao,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = visualTemplate.onSurface,
                            modifier = Modifier
                                .padding(start = 18.dp, bottom = 2.dp)
                                .pointerInput(npc.id, c.nome) {
                                    detectTapGestures(onLongPress = { mostrarDetalhes = true })
                                }
                                .feedbackOnPress(),
                            forceStroke = false
                        )
                        if (mostrarDetalhes) {
                            val definicao = viewModel.encantoDefinitionPorNomeParaTipo(c.nome, npc.tipoExaltado)
                            if (definicao != null) {
                                com.example.ui.tabs.CatalogDetailsDialog(
                                    def = definicao,
                                    onDismiss = { mostrarDetalhes = false },
                                    viewModel = viewModel,
                                    charmsParaArvore = viewModel.todosOsEncantosParaArvore(
                                        when (npc.tipoExaltado) {
                                            com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> com.example.model.CharacterType.DRAGON_BLOODED
                                            com.example.model.TipoExaltadoEncontro.LUNAR -> com.example.model.CharacterType.LUNAR
                                            com.example.model.TipoExaltadoEncontro.SOLAR -> com.example.model.CharacterType.SOLAR
                                        }
                                    )
                                )
                            } else {
                                mostrarDetalhes = false
                            }
                        }
                    }
                }
            }

            // Gaveta exclusiva de Feitiços da Aba 11. Feitiços não são
            // misturados às gavetas de Encantos porque são poderes de
            // Feitiçaria e possuem seu próprio círculo/custo.
            val feiticosDisponiveis = remember(viewModel, npc.id, npc.tipoExaltado, npc.charms, npc.feiticos, npc.primeiroXpRecebido) {
                viewModel.feiticosDisponiveisNpcEncontro(npc.id)
            }
            val podeGerenciarFeiticoInicial = npc.podeGerenciarFeiticoInicial()
            if (npc.feiticos.isNotEmpty() || feiticosDisponiveis.isNotEmpty()) {
                var feiticosExpandidos by remember(npc.id) { mutableStateOf(false) }
                var gerenciarFeiticos by remember(npc.id) { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .feedbackClickable { feiticosExpandidos = !feiticosExpandidos }
                ) {
                    AppText(
                        if (feiticosExpandidos) "▾" else "▸",
                        color = visualTemplate.gold,
                        modifier = Modifier.padding(end = 6.dp),
                        forceStroke = true
                    )
                    AppText(
                        "Feitiços (${npc.feiticos.size})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = visualTemplate.onSurface,
                        forceStroke = true
                    )
                }
                if (feiticosExpandidos) {
                    if (npc.feiticos.isEmpty()) {
                        AppText(
                            "Nenhum Feitiço selecionado.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ExaltedMuted,
                            modifier = Modifier.padding(start = 18.dp, bottom = 3.dp),
                            forceStroke = true
                        )
                    } else {
                        npc.feiticos.forEach { feitico ->
                            var mostrarDetalhesFeitico by remember(npc.id, feitico.nome) { mutableStateOf(false) }
                            AppText(
                                "• ${feitico.nome} — Círculo ${feitico.circulo} | ${feitico.custo}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = visualTemplate.onSurface,
                                modifier = Modifier
                                    .padding(start = 18.dp, bottom = 3.dp)
                                    .pointerInput(npc.id, feitico.nome) {
                                        detectTapGestures(onLongPress = { mostrarDetalhesFeitico = true })
                                    }
                                    .feedbackOnPress(),
                                forceStroke = true
                            )
                            if (mostrarDetalhesFeitico) {
                                // Detalhes usam o catálogo completo: depois do primeiro XP,
                                // feiticosDisponiveis fica vazio de propósito para bloquear a
                                // troca gratuita, mas o long press deve continuar funcionando.
                                val definicaoFeitico = viewModel.feiticoDefinitionPorNome(feitico.nome)
                                if (definicaoFeitico != null) {
                                    FeiticoDetailsDialog(
                                        def = definicaoFeitico,
                                        onDismiss = { mostrarDetalhesFeitico = false }
                                    )
                                } else {
                                    mostrarDetalhesFeitico = false
                                }
                            }
                        }
                    }
                    if (feiticosDisponiveis.isNotEmpty() && podeGerenciarFeiticoInicial) {
                        InkButton(
                    visualTemplate = visualTemplate,
                            label = "Gerenciar Feitiço Inicial",
                            onClick = { gerenciarFeiticos = true },
                            size = InkButtonSize.Small,
                            variant = InkButtonVariant.Secondary,
                            brushIndex = 1
                        )
                    }
                }
                if (gerenciarFeiticos && podeGerenciarFeiticoInicial) {
                    AlertDialog(
                        onDismissRequest = { gerenciarFeiticos = false },
                        title = { AppText(
                    text = "Gerenciar Feitiço Inicial",
                    modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, forceStroke = true,
                    textAlign = TextAlign.Center
                ) },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AppText(
                                    "Escolha um Feitiço inicial. A seleção gratuita é encerrada permanentemente ao receber os primeiros 5 XP.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    forceStroke = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                feiticosDisponiveis.groupBy { it.circulo }.forEach { (circulo, defs) ->
                                    AppText("Círculo $circulo", fontWeight = FontWeight.Bold, color = ExaltedGold, forceStroke = true)
                                    defs.forEach { def ->
                                        val marcado = npc.feiticoInicialNome?.let {
                                            com.example.data.EncantosSolaresCatalog.sameName(it, def.nome)
                                        } == true
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(
                                                modifier = Modifier.feedbackOnPress(),
                                                checked = marcado,
                                                onCheckedChange = { adicionar ->
                                                    viewModel.atualizarFeiticoNpcEncontro(npc.id, def, adicionar)
                                                }
                                            )
                                            AppText(def.nome, style = MaterialTheme.typography.bodyMedium, forceStroke = true)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            InkButton(visualTemplate = visualTemplate, label = "Fechar", onClick = { gerenciarFeiticos = false }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary)
                        }
                    )
                }
            }

            // Botões de XP ficam na linha imediatamente abaixo da última
            // gaveta de Encantos e alinhados à direita, sem competir com o
            // título da seção.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InkButton(
                    visualTemplate = visualTemplate,
                    label = "+ Aumentar XP",
                    onClick = onAumentarExperiencia,
                    enabled = !gerando,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = true,
                    size = InkButtonSize.Small,
                    variant = InkButtonVariant.Secondary,
                    brushIndex = 1
                )
                InkButton(
                    visualTemplate = visualTemplate,
                    label = "− Diminuir XP",
                    onClick = onDiminuirExperiencia,
                    enabled = !gerando && npc.historicoXpBatches.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = true,
                    size = InkButtonSize.Small,
                    variant = InkButtonVariant.Secondary,
                    brushIndex = 2
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // XP movido pra cá — pedido explícito do usuário: agora fica
            // logo abaixo da seção de Encantos, em vez de perto de
            // Equipamentos/Força de Vontade. Atualiza automaticamente
            // porque lê os mesmos campos (npc.xpAtual, npc.xpGastoTotal,
            // npc.historicoXpBatches) que os botões "+ Aumentar XP"/
            // "− Diminuir XP" modificam — qualquer clique neles já
            // recompõe esta linha normalmente.
            // APPROVED VISUAL CUSTOMIZATION
            // Divisão visual entre Encantos e XP no fim da planilha.
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider(color = visualTemplate.muted.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            // Toque (tap) mostra SÓ os Encantos adicionados via "+ Aumentar
            // XP" — pedido explícito do usuário, corrigindo o comportamento
            // anterior que mostrava TODOS os Encantos (incluindo os da
            // criação do personagem, que não são XP de verdade).
            val encantosAdicionadosPorXp = todosEncantos
                .joinToString("\n") { "• $it" }
                .ifBlank { "Essa planilha ainda não fez alterações com experiência" }
            LinhaInfo(
                "Ganhos - ${npc.xpGastoTotal} Gastos - ${npc.xpAtual} Sobrando",
                (npc.xpAtual + npc.xpGastoTotal).toString(),
                zebra++,
                calculo = encantosAdicionadosPorXp,
                calculoLongPress = detalhamentoXp,
                sufixo = "XP",
                visualTemplate = visualTemplate,
                // Mantém o total de XP legível sem dominar a linha.
                // 16sp acompanha a escala do restante do quadro; o sufixo
                // permanece em uma única linha no componente compartilhado.
                valueWidth = 52.dp,
                valueTextStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                ),
                inlineSummary = true
            )

            if (npc.alertasValidacao.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                npc.alertasValidacao.forEach { alerta ->
                    AppText(
                        "⚠ $alerta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    forceStroke = true
)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.HorizontalDivider(color = visualTemplate.muted.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            AppText("Controle de Iniciativa", style = MaterialTheme.typography.labelMedium, color = visualTemplate.accent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    forceStroke = true
)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NPC_DAMAGE_DELTAS_NEGATIVE.forEach { delta ->
                        com.example.ui.components.IniciativaAjusteButton(
                            delta = delta,
                            onClick = { ajustarIniciativaSincronizada(delta) },
                            width = 88.dp,
                            visualTemplate = visualTemplate
                        )
                    }
                }
                Box(
                    modifier = Modifier.width(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        formatarNumeroPlanilha(iniciativaSincronizada),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.components.corIniciativaPorValor(iniciativaSincronizada),
                        textAlign = TextAlign.Center,
                        forceStroke = true
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NPC_DAMAGE_DELTAS_POSITIVE.forEach { delta ->
                        com.example.ui.components.IniciativaAjusteButton(
                            delta = delta,
                            onClick = { ajustarIniciativaSincronizada(delta) },
                            width = 88.dp,
                            visualTemplate = visualTemplate
                        )
                    }
                }
            }
        }

    NpcCardJuntarSeDialog(
        visible = mostrarConfirmacaoJuntarSe,
        npcNome = npc.nome,
        visualTemplate = visualTemplate,
        onConfirm = {
            mostrarConfirmacaoJuntarSe = false
            onJuntarSeABatalha()
        },
        onDismiss = { mostrarConfirmacaoJuntarSe = false }
    ) // fim do Column principal

    // Ícones pequenos no canto superior direito, sobrepostos ao card —
    // pedido explícito do usuário: botões menores, substituídos por
    // símbolos, na parte superior direita em vez de uma linha cheia.
    val npcIoScope = rememberCoroutineScope()
    var operacaoNpcEmAndamento by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .fillMaxWidth()
    ) {
        // Em qualquer largura, respeitar o espaço real do cartão.
        // Abaixo de 152 dp, manter o toque mínimo de 48 dp e permitir rolagem.
        val actionButtonWidth = ((maxWidth - 8.dp) / 3).coerceAtLeast(48.dp)
        val actionRowModifier = if (maxWidth < 152.dp) Modifier.horizontalScroll(rememberScrollState()) else Modifier
        Row(modifier = actionRowModifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        InkButton(
                    visualTemplate = visualTemplate,
            label = "Exportar",
            onClick = {
                if (operacaoNpcEmAndamento) return@InkButton
                operacaoNpcEmAndamento = true
                npcIoScope.launch {
                    val arquivo = com.example.ui.tabs.EncounterCombatEquations.gerarPdfNpc(context, npc)
                    operacaoNpcEmAndamento = false
                    val mensagem = if (arquivo != null) "PDF salvo em Documentos: ${arquivo.name}" else "Falha ao gerar o PDF"
                    android.widget.Toast.makeText(context, mensagem, android.widget.Toast.LENGTH_LONG).show()
                }
            },
            size = InkButtonSize.Small,
            variant = InkButtonVariant.Secondary,
            // Largura ampliada para caber "Exportar" numa linha só (o texto
            // quebrava em duas por falta de espaço dentro do botão).
            brushIndex = 0, customWidth = actionButtonWidth.coerceAtMost(136.dp), customHeight = 44.dp
        )
        InkButton(
                    visualTemplate = visualTemplate,
            label = "Salvar",
            onClick = {
                if (operacaoNpcEmAndamento) return@InkButton
                operacaoNpcEmAndamento = true
                npcIoScope.launch {
                    try {
                        val proximoNumero = com.example.data.NpcSaveLoadService.listarSalvos(context).size + 1
                        com.example.data.NpcSaveLoadService.salvar(context, npc, proximoNumero)
                        android.widget.Toast.makeText(context, "NPC salvo com sucesso.", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Falha ao salvar NPC.", android.widget.Toast.LENGTH_LONG).show()
                    } finally {
                        operacaoNpcEmAndamento = false
                    }
                }
            },
            size = InkButtonSize.Small,
            variant = InkButtonVariant.Secondary,
            brushIndex = 1, customWidth = actionButtonWidth.coerceAtMost(116.dp), customHeight = 44.dp
        )
        InkButton(
                    visualTemplate = visualTemplate,
            label = "Carregar",
            onClick = { mostrarCarregar = true },
            size = InkButtonSize.Small,
            variant = InkButtonVariant.Secondary,
            brushIndex = 2, customWidth = actionButtonWidth.coerceAtMost(134.dp), customHeight = 44.dp
        )
        } // Exportar / Salvar / Carregar: sempre na mesma linha
    } // BoxWithConstraints
    } // fim do Box
}
}