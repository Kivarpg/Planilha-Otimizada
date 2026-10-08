package com.example.ui.tabs
import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.exaltedSectionPanel
import com.example.ui.components.feedbackClickable
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro
import com.example.model.isDragonBlooded
import com.example.model.isLunar
import com.example.ui.components.GildedCard
import com.example.ui.components.InkButton
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButtonVariant
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ExaltedAmber
import com.example.ui.theme.ExaltedDarkSurface
import com.example.ui.theme.ExaltedDarkSurfaceVariant
import com.example.ui.theme.ExaltedStructuralMetalDeep
import com.example.ui.theme.ExaltedGold
import com.example.ui.theme.ExaltedAccentBright
import com.example.ui.theme.ExaltedMuted
import com.example.ui.theme.ExaltedOnSurface
import com.example.ui.theme.visualTemplateParaExaltado
import com.example.viewmodel.SheetViewModel

private const val MAX_NPCS_ENCONTRO = 10
private const val FOCUS_OPTION_NONE = "__FOCUS_NONE__"

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
private val GENERO_OPCOES = listOf(
    com.example.data.GeneroNome.MASCULINO to "Masculino",
    com.example.data.GeneroNome.FEMININO to "Feminino"
)

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
private val ARQUETIPO_OPCOES = listOf(
    ArquetipoEncontro.FISICO to "Físico",
    ArquetipoEncontro.SOCIAL to "Social",
    ArquetipoEncontro.MENTAL to "Mental"
)

private val ORIGEM_SANGUE_DE_DRAGAO_OPCOES = listOf(
    com.example.data.OrigemNomeSangueDeDragao.IMPERIO to "Império",
    com.example.data.OrigemNomeSangueDeDragao.LOOKSHY to "Lookshy",
    com.example.data.OrigemNomeSangueDeDragao.SEM_CASTA to com.example.model.BoxNames.LunarCaste.CASTELESS
)

// VISUAL IDENTITY 314: remodelação específica desta aba; somente apresentação, sem novos campos.
private val ENCOUNTER_WEAPON_WEIGHT_CATEGORIES = listOf("Leve", "Média", "Pesada")
private val ENCOUNTER_ARMOR_WEIGHT_CATEGORIES = listOf("Sem Armadura", "Leve", "Média", "Pesada")

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun EncounterGeneratorTab(
    sheetAtual: com.example.model.CharacterSheet,
    viewModel: SheetViewModel,
    nomeManual: String,
    onNomeManualChange: (String) -> Unit,
    arquetipoSelecionado: ArquetipoEncontro,
    onArquetipoChange: (ArquetipoEncontro) -> Unit,
    culturaSelecionada: com.example.data.CulturaNome?,
    generoSelecionado: com.example.data.GeneroNome?,
    onGeneroChange: (com.example.data.GeneroNome?) -> Unit,
    abaSelecionadaId: String?,
    onAbaSelecionadaChange: (String?) -> Unit,
    mensagemLimite: String?,
    onMensagemLimiteChange: (String?) -> Unit
) {
    val npcsGerados by viewModel.npcsEncontro.collectAsState()
    val gerando by viewModel.gerandoNpcEncontro.collectAsState()
    val erroGeracao by viewModel.erroNpcEncontro.collectAsState()

    // Correção de um crash real (log de logcat confirmou): selecionar uma
    // aba recém-criada NO MESMO frame em que ela é adicionada pode
    // derrubar o ScrollableTabRow com IndexOutOfBoundsException — a lista
    // interna de posições das abas, usada pelo indicador, só é preenchida
    // depois que cada aba termina de ser medida na tela; se a seleção
    // aponta pra uma aba que ainda não foi medida, o Compose tenta acessar
    // um índice que ainda não existe nessa lista interna. A correção:
    // guardar a intenção de selecionar aqui, e só aplicar de fato depois
    // de esperar pelo menos 2 frames (withFrameNanos, margem extra pra
    // emuladores mais lentos), dando tempo do Compose processar a aba
    // nova antes de apontar o indicador pra ela.
    var pendingSelectId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingSelectId) {
        val idPendente = pendingSelectId
        if (idPendente != null) {
            androidx.compose.runtime.withFrameNanos { }
            androidx.compose.runtime.withFrameNanos { }
            onAbaSelecionadaChange(idPendente)
            pendingSelectId = null
        }
    }

    // Tipo de Exaltado a gerar — CORREÇÃO: era remember (perdia a escolha ao
    // trocar de aba e voltar); virou rememberSaveable a pedido explícito do
    // usuário, que quer TODAS as opções desta aba preservadas ao sair e
    // retornar, não só o padrão "Solar" de antes.
    var tipoSelecionado by rememberSaveable {
        mutableStateOf(
            when {
                sheetAtual.tipoPersonagem.isDragonBlooded() -> TipoExaltadoEncontro.SANGUE_DE_DRAGAO
                sheetAtual.tipoPersonagem.isLunar() -> TipoExaltadoEncontro.LUNAR
                else -> TipoExaltadoEncontro.SOLAR
            }
        )
    }
    val tipoSangueDeDragao = tipoSelecionado == TipoExaltadoEncontro.SANGUE_DE_DRAGAO
    val tipoLunar = tipoSelecionado == TipoExaltadoEncontro.LUNAR
    // Contraste automático via corTextoContraste() em cada botão — pedido
    // explícito do usuário, substituindo o design anterior de tipografia
    // prata uniforme (que dificultava a leitura em fundos claros quando
    // não selecionado). Ver histórico no changelog.
    var origemNomeSelecionada by rememberSaveable { mutableStateOf(com.example.data.OrigemNomeSangueDeDragao.IMPERIO) }
    // Checkbox "enviar pro Conflito" — pedido explícito do usuário,
    // substitui o antigo botão "Gerar Combatente". Quando marcado, a imagem
    // de geração cria o NPC e já manda pra Aba 12 (Conflito) automaticamente.
    var enviarParaConflito by rememberSaveable { mutableStateOf(false) }
    var mostrarPersonalizar by remember { mutableStateOf(false) }
    var customFoco by rememberSaveable { mutableStateOf<String?>(null) }
    var customAtaque by rememberSaveable { mutableStateOf<String?>(null) }
    var customDefesa by rememberSaveable { mutableStateOf<String?>(null) }
    var customSecundaria by rememberSaveable { mutableStateOf<String?>(null) }
    var campoCustomizacao by remember { mutableStateOf<String?>(null) }
    var npcEquipamentoPendente by remember { mutableStateOf<NpcEncontro?>(null) }
    var mostrarCatalogoArmas by remember { mutableStateOf(false) }
    var mostrarCatalogoArmaduras by remember { mutableStateOf(false) }
    var armaSelecionadaManual by rememberSaveable { mutableStateOf<String?>(null) }
    var armaduraSelecionadaManual by rememberSaveable { mutableStateOf<String?>(null) }
    // No novo Foco, equipamento continua automático por padrão. Ao desmarcar,
    // a arma e a armadura são escolhidas manualmente no catálogo após a geração.
    val equipamentoAutomatico = true

    // As opções só mudam quando o tipo Lunar é selecionado; não reconstruir
    // listas a cada recomposição da Aba 11.
    val opcoesFocoBase: List<String> = remember(tipoLunar) {
        if (tipoLunar) ExaltedConstants.ALL_ATTRIBUTES.toList()
        else ExaltedConstants.ALL_25_ABILITIES.toList()
    }
    // Feitiçaria é uma diretiva estrutural do Foco; não entra nas listas canônicas
    // de Habilidades/Atributos usadas pelos cálculos mecânicos.
    val opcoesFoco: List<String> = remember(opcoesFocoBase) {
        opcoesFocoBase + com.example.data.ENCOUNTER_FOCUS_SORCERY
    }


    val dialogListHeight = if (LocalConfiguration.current.screenHeightDp < 700) 300.dp else 420.dp

    if (campoCustomizacao != null) {
        val campo = campoCustomizacao!!
        val opcoes = when (campo) {
            "Foco" -> opcoesFoco
            "Ataque" -> com.example.data.EncounterGenerationRules.ATTACK_METHODS
             "Defesa" -> when (com.example.data.EncounterGenerationRules.habilidadeRealDoMetodoDeAtaque(customAtaque?.takeUnless { it == FOCUS_OPTION_NONE })
                ?: customFoco?.takeUnless { it == FOCUS_OPTION_NONE }?.takeIf { it in com.example.data.EncounterGenerationRules.COMBAT_ABILITIES }) {
                "Briga" -> listOf("Esquiva", "Briga")
                "Armas Brancas" -> listOf("Armas Brancas")
                "Arqueirismo", "Arremesso" -> listOf("Esquiva")
                else -> listOf("Esquiva", "Armas Brancas", "Briga")
            }
            "Secundária" -> opcoesFocoBase.filter { it != customFoco }
            else -> emptyList()
        }
        AlertDialog(
            onDismissRequest = { campoCustomizacao = null },
            title = { AppText(
                    text = "Foco — $campo",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(dialogListHeight)
                        .background(ExaltedDarkSurface.copy(alpha = .72f))
                        .border(1.dp, ExaltedStructuralMetalDeep.copy(alpha = .52f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    item {
                        InkButton(label = "Automático", onClick = {
                            when (campo) {
                                "Foco" -> { customFoco = null; if (customAtaque == null) customDefesa = null }
                                "Ataque" -> { customAtaque = null; customDefesa = null }
                                "Defesa" -> customDefesa = null
                                "Secundária" -> customSecundaria = null
                            }
                            campoCustomizacao = null; mostrarPersonalizar = true
                        }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), size = InkButtonSize.Small, variant = InkButtonVariant.Secondary, brushIndex = 2)
                    }
                    item {
                        InkButton(label = "Nenhum", onClick = {
                            when (campo) {
                                "Foco" -> customFoco = FOCUS_OPTION_NONE
                                "Ataque" -> { customAtaque = FOCUS_OPTION_NONE; customDefesa = FOCUS_OPTION_NONE }
                                "Defesa" -> customDefesa = FOCUS_OPTION_NONE
                                "Secundária" -> customSecundaria = FOCUS_OPTION_NONE
                            }
                            campoCustomizacao = null; mostrarPersonalizar = true
                        }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), size = InkButtonSize.Small, variant = InkButtonVariant.Secondary, brushIndex = 2)
                    }
                    items(opcoes) { opcao ->
                        InkButton(label = opcao, onClick = {
                            when (campo) {
                                "Foco" -> { customFoco = opcao; if (customSecundaria == opcao) customSecundaria = null; if (customAtaque == null) customDefesa = null }
                                "Ataque" -> { customAtaque = opcao; customDefesa = null }
                                "Defesa" -> customDefesa = opcao
                                "Secundária" -> customSecundaria = opcao
                            }
                            campoCustomizacao = null; mostrarPersonalizar = true
                        }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), size = InkButtonSize.Small, variant = InkButtonVariant.Secondary, brushIndex = 2)
                    }
                }
            },
            confirmButton = { InkButton(label = "Fechar", onClick = { campoCustomizacao = null }, size = InkButtonSize.Small, variant = InkButtonVariant.Secondary) },
            containerColor = ExaltedDarkSurface,
            titleContentColor = ExaltedOnSurface,
            textContentColor = ExaltedOnSurface
        )
    }

    if (mostrarPersonalizar) {
        AlertDialog(
            onDismissRequest = { mostrarPersonalizar = false },
            title = { AppText(
                    text = "Foco do NPC",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // O estado da escolha é interno. No menu principal exibimos
                    // somente o nome curto do campo para evitar truncamento; null
                    // continua significando AUTOMATICO na geração.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 2
                    ) {
                        listOf(
                            Triple(customFoco ?: "Foco", "Foco", 0),
                            Triple(customSecundaria ?: "Foco Secundário", "Secundária", 1),
                            Triple(customAtaque ?: "Ofensivo", "Ataque", 2),
                            Triple(customDefesa ?: "Defensivo", "Defesa", 3)
                        ).forEach { (rotulo, campo, _) ->
                            InkButton(
                                label = rotulo,
                                onClick = { mostrarPersonalizar = false; campoCustomizacao = campo },
                                modifier = Modifier.fillMaxWidth(0.49f),
                                size = InkButtonSize.Small,
                                variant = InkButtonVariant.Secondary,
                                brushIndex = 2
                            )
                        }
                    }
                }
            },
            dismissButton = {},
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InkButton(
                        label = "Aplicar",
                        onClick = { mostrarPersonalizar = false },
                        modifier = Modifier.weight(1f),
                        fillMaxWidth = true,
                        size = InkButtonSize.Small
                    )
                    InkButton(
                        label = "Limpar",
                        onClick = { customFoco=null; customAtaque=null; customDefesa=null; customSecundaria=null },
                        modifier = Modifier.weight(1f),
                        fillMaxWidth = true,
                        size = InkButtonSize.Small,
                        variant = InkButtonVariant.Secondary
                    )
                }
            }
        )
    }

    if (mostrarCatalogoArmas) {
        val npc = npcEquipamentoPendente
        val armasCatalogo = npc?.let { com.example.data.WeaponCatalog.candidatasPorHabilidade(it.habilidadePrincipal) }.orEmpty()
        AlertDialog(
            onDismissRequest = { mostrarCatalogoArmas = false; mostrarCatalogoArmaduras = true },
            title = { AppText(modifier = Modifier.fillMaxWidth(), text = "Equipamento — Arma", textAlign = TextAlign.Center) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(dialogListHeight)) {
                    items(armasCatalogo) { arma ->
                        InkButton(
                            label = "${arma.nome} — ${arma.peso}",
                            onClick = {
                                armaSelecionadaManual = arma.nome
                                npc?.let { viewModel.atualizarArmaNpcEncontro(it.id, com.example.data.EncounterEquipmentService.montarArmaSelecionada(it.habilidadePrincipal, arma)) }
                                mostrarCatalogoArmas = false
                                mostrarCatalogoArmaduras = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            size = InkButtonSize.Small, variant = InkButtonVariant.Secondary,
                            selected = armaSelecionadaManual == arma.nome, brushIndex = 2
                        )
                    }
                }
            },
            confirmButton = { InkButton(label = "Próximo", onClick = { mostrarCatalogoArmas = false; mostrarCatalogoArmaduras = true }, size = InkButtonSize.Small) }
        )
    }

    if (mostrarCatalogoArmaduras) {
        val armadurasCatalogo = ENCOUNTER_ARMOR_WEIGHT_CATEGORIES
            .filter { it != "Sem Armadura" }
            .flatMap { com.example.data.ArmorCatalog.candidatas(it) }
        AlertDialog(
            onDismissRequest = { mostrarCatalogoArmaduras = false },
            title = { AppText(modifier = Modifier.fillMaxWidth(), text = "Equipamento — Armadura", textAlign = TextAlign.Center) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(dialogListHeight)) {
                    item {
                        InkButton(
                            label = "Sem Armadura",
                            onClick = {
                                armaduraSelecionadaManual = "Sem Armadura"
                                npcEquipamentoPendente?.let { npc ->
                                    viewModel.atualizarArmaduraNpcEncontro(npc.id, com.example.model.ArmaduraEncontro(nome = "Sem Armadura", peso = "Sem Armadura", tipo = "Mundana", absorcao = 0, dureza = 0, penalidadeMobilidade = 0, motesComitados = 0))
                                }
                                mostrarCatalogoArmaduras = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), size = InkButtonSize.Small, variant = InkButtonVariant.Secondary, selected = armaduraSelecionadaManual == "Sem Armadura", brushIndex = 2
                        )
                    }
                    items(armadurasCatalogo) { armadura ->
                        InkButton(
                            label = "${armadura.nome} — ${armadura.peso}",
                            onClick = {
                                armaduraSelecionadaManual = armadura.nome
                                npcEquipamentoPendente?.let { npc -> viewModel.atualizarArmaduraNpcEncontro(npc.id, com.example.data.EncounterEquipmentService.montarArmaduraSelecionada(armadura)) }
                                mostrarCatalogoArmaduras = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), size = InkButtonSize.Small, variant = InkButtonVariant.Secondary, selected = armaduraSelecionadaManual == armadura.nome, brushIndex = 2
                        )
                    }
                }
            },
            confirmButton = { InkButton(label = "Concluir", onClick = { mostrarCatalogoArmaduras = false }, size = InkButtonSize.Small) }
        )
    }

    // Geração completa: o ícone 0000.png dispara a mesma ação que o antigo
    // botão "Gerar NPC", preservando todas as regras de geração existentes.
    fun gerarNpc(aposGerar: (com.example.model.NpcEncontro) -> Unit) {
        if (npcsGerados.size >= MAX_NPCS_ENCONTRO) {
            onMensagemLimiteChange("Limite de $MAX_NPCS_ENCONTRO NPCs atingido — feche uma das abas antes de gerar outro.")
        } else {
            onMensagemLimiteChange(null)
            // A geração agora usa diretamente as escolhas visíveis na interface.
            // O antigo modo separado "Personalizado/Aleatório" foi removido.
            // O gênero é definido diretamente pela escolha visível na interface.
            val nomeParaGerar = nomeManual
            val generoParaGerar = generoSelecionado
            fun modoDaEscolha(valor: String?): com.example.data.EncounterCustomizationMode = when (valor) {
                null -> com.example.data.EncounterCustomizationMode.AUTOMATICO
                FOCUS_OPTION_NONE -> com.example.data.EncounterCustomizationMode.NENHUM
                else -> com.example.data.EncounterCustomizationMode.EXPLICITO
            }
            fun valorExplicito(valor: String?): String? = valor?.takeUnless { it == FOCUS_OPTION_NONE }
            val customizacao = com.example.data.EncounterCustomization(
                foco = valorExplicito(customFoco),
                ataque = valorExplicito(customAtaque),
                defesa = valorExplicito(customDefesa),
                secundaria = valorExplicito(customSecundaria),
                focoMode = modoDaEscolha(customFoco),
                ataqueMode = modoDaEscolha(customAtaque),
                defesaMode = modoDaEscolha(customDefesa),
                secundariaMode = modoDaEscolha(customSecundaria)
            ).takeUnless { it.isEmpty }
            // Arquétipo é a direção ampla escolhida pelo usuário. Foco especializa
            // essa direção; não a substitui silenciosamente.
            val focoEfetivo = customizacao?.focoExplicito
            val arquetipoParaGerar = arquetipoSelecionado
            val aoTerminar: (com.example.model.NpcEncontro?) -> Unit = { gerado ->
                if (gerado != null) {
                    pendingSelectId = gerado.id
                    onNomeManualChange("")
                    aposGerar(gerado)
                }
            }
            if (tipoSangueDeDragao) {
                viewModel.gerarNpcSangueDeDragao(
                    nomeParaGerar, arquetipoParaGerar, origemNomeSelecionada, generoParaGerar, aoTerminar, focoEfetivo, customizacao
                )
            } else if (tipoLunar) {
                viewModel.gerarNpcLunar(
                    nomeParaGerar, arquetipoParaGerar, generoParaGerar, aoTerminar, focoEfetivo, customizacao
                )
            } else {
                viewModel.gerarNpcEncontro(
                    nomeParaGerar, arquetipoParaGerar, culturaSelecionada, generoParaGerar, aoTerminar, focoEfetivo, customizacao
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .exaltedTabIdentity(11).exaltedContentStage(11)
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        GildedCard(
            modifier = Modifier.fillMaxWidth().exaltedSectionPanel(11),
            colors = CardDefaults.cardColors(containerColor = ExaltedDarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // Campo de Nome com floating label nativo do Compose (o
                // próprio parâmetro "label" já sobe pro topo ao focar/
                // digitar — não precisa de nada manual) — pedido explícito
                // do usuário: só "Nome" como rótulo, sem texto auxiliar
                // explicando o preenchimento automático.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compactHeader = maxWidth < 430.dp
                    if (compactHeader) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = nomeManual,
                                onValueChange = com.example.ui.components.rememberTypingFeedback { onNomeManualChange(it.take(60)) },
                                label = { AppText("Nome") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ExaltedAccentBright,
                                    unfocusedBorderColor = com.example.ui.theme.ExaltedOutline.copy(alpha = 0.55f),
                                    focusedLabelColor = ExaltedAccentBright,
                                    unfocusedLabelColor = ExaltedMuted,
                                    cursorColor = ExaltedGold,
                                    focusedTextColor = ExaltedOnSurface,
                                    unfocusedTextColor = ExaltedOnSurface
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.Checkbox(
                                        modifier = Modifier.feedbackOnPress(),
                                        checked = enviarParaConflito,
                                        onCheckedChange = { enviarParaConflito = it },
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = ExaltedAmber,
                                            uncheckedColor = ExaltedMuted
                                        ))
                                    AppText("Conflito", style = MaterialTheme.typography.bodySmall, color = if (enviarParaConflito) ExaltedAccentBright else ExaltedMuted)
                                }
                                Box(
                                    modifier = Modifier.size(64.dp).feedbackClickable(
                                        enabled = !gerando, role = Role.Button,
                                        onClick = {
                                            gerarNpc(aposGerar = { gerado ->
                                                if (!equipamentoAutomatico) {
                                                    npcEquipamentoPendente = gerado
                                                    armaSelecionadaManual = null
                                                    armaduraSelecionadaManual = null
                                                    mostrarCatalogoArmas = true
                                                }
                                                if (enviarParaConflito) viewModel.juntarNpcEncontroABatalha(gerado.id)
                                            })
                                        }
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.gerar_npc_0000),
                                        contentDescription = "Gerar NPC",
                                        modifier = Modifier.size(60.dp),
                                        alpha = if (gerando) 0.45f else 1f
                                    )
                                }
                            }
                        }
                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = nomeManual,
                                                onValueChange = com.example.ui.components.rememberTypingFeedback { onNomeManualChange(it.take(60)) },
                                                label = { AppText("Nome") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = ExaltedAccentBright,
                                                    unfocusedBorderColor = com.example.ui.theme.ExaltedOutline.copy(alpha = 0.55f),
                                                    focusedLabelColor = ExaltedAccentBright,
                                                    unfocusedLabelColor = ExaltedMuted,
                                                    cursorColor = ExaltedGold,
                                                    focusedTextColor = ExaltedOnSurface,
                                                    unfocusedTextColor = ExaltedOnSurface
                                                )
                                            )
                        
                                            // O checkbox permanece com exatamente o mesmo estado e callback;
                                            // apenas foi reposicionado para o cabeçalho, ao lado do nome
                                            // e do botão de gerar NPC, conforme a referência visual.
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                androidx.compose.material3.Checkbox(
                                                    modifier = Modifier.feedbackOnPress(),
                                                    checked = enviarParaConflito,
                                                    onCheckedChange = { enviarParaConflito = it },
                                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                                        checkedColor = ExaltedAmber,
                                                        uncheckedColor = ExaltedMuted
                                                    ))
                                                AppText(
                                                    "Conflito",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (enviarParaConflito) ExaltedAccentBright else ExaltedMuted
                                                )
                                            }
                        
                                            // A imagem 0000.png substitui visualmente o antigo botão
                                            // "Gerar NPC", mantendo exatamente o mesmo callback e
                                            // o mesmo estado de habilitação durante a geração.
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .feedbackClickable(
                                                        enabled = !gerando,
                                                        role = Role.Button,
                                                        onClick = {
                                                            gerarNpc(aposGerar = { gerado ->
                                                                if (!equipamentoAutomatico) {
                                                                    npcEquipamentoPendente = gerado
                                                                    armaSelecionadaManual = null
                                                                    armaduraSelecionadaManual = null
                                                                    mostrarCatalogoArmas = true
                                                                }
                                                                if (enviarParaConflito) viewModel.juntarNpcEncontroABatalha(gerado.id)
                                                            })
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.gerar_npc_0000),
                                                    contentDescription = "Gerar NPC",
                                                    modifier = Modifier.size(60.dp),
                                                    alpha = if (gerando) 0.45f else 1f
                                                )
                                            }
                                        }
                        
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Agrupamento visual da configuração: os controles e regras permanecem
                // os mesmos; apenas a hierarquia espacial foi organizada em seções.
                Column(
                    modifier = Modifier.fillMaxWidth().exaltedSectionPanel(1).padding(12.dp)
                ) {
                    AppText("1. Personagem", style = MaterialTheme.typography.titleMedium, color = ExaltedAccentBright)
                    Spacer(modifier = Modifier.height(8.dp))

                // Tipo de Exaltado — botões sempre visíveis, lado a lado
                // (pedido explícito do usuário: nada de dropdown/toque pra
                // abrir menu). Pré-selecionado com o tipo da planilha atual.
                // O tipo de Exaltado selecionado nesta linha é a restrição
                // usada pela geração e não é sorteado automaticamente.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compactTypeButtons = maxWidth < 720.dp

                    @Composable
                    fun TipoButton(tipo: TipoExaltadoEncontro, rotulo: String, modifier: Modifier) {
                        val selecionado = tipoSelecionado == tipo
                        InkButton(
                            label = rotulo,
                            onClick = {
                                tipoSelecionado = tipo
                                val focoValido = if (tipo == TipoExaltadoEncontro.LUNAR) {
                                    customFoco in ExaltedConstants.ALL_ATTRIBUTES
                                } else {
                                    customFoco in ExaltedConstants.ALL_25_ABILITIES
                                }
                                if (!focoValido && customFoco != com.example.data.ENCOUNTER_FOCUS_SORCERY) customFoco = null
                            },
                            modifier = modifier,
                            size = InkButtonSize.Small,
                            variant = InkButtonVariant.Secondary,
                            selected = selecionado,
                            brushIndex = 0
                        )
                    }

                    if (!compactTypeButtons) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TipoButton(TipoExaltadoEncontro.SOLAR, "Solar", Modifier.weight(1f))
                            TipoButton(TipoExaltadoEncontro.SANGUE_DE_DRAGAO, "Sangue de Dragão", Modifier.weight(1f))
                            TipoButton(TipoExaltadoEncontro.LUNAR, "Lunar", Modifier.weight(1f))
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TipoButton(TipoExaltadoEncontro.SOLAR, "Solar", Modifier.weight(1f))
                                TipoButton(TipoExaltadoEncontro.SANGUE_DE_DRAGAO, "Sangue de Dragão", Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TipoButton(TipoExaltadoEncontro.LUNAR, "Lunar", Modifier.fillMaxWidth(0.5f))
                            }
                        }
                    }
                }

                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().exaltedSectionPanel(2).padding(12.dp)
                ) {
                    AppText("2. Gênero", style = MaterialTheme.typography.titleMedium, color = ExaltedAccentBright)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GENERO_OPCOES.forEach { (genero, rotulo) ->
                            val selecionado = generoSelecionado == genero
                            InkButton(
                                label = rotulo,
                                onClick = { onGeneroChange(genero) },
                                modifier = Modifier.weight(1f),
                                size = InkButtonSize.Small,
                                variant = InkButtonVariant.Secondary,
                                selected = selecionado,
                                brushIndex = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().exaltedSectionPanel(3).padding(12.dp)
                ) {
                AppText("3. Arquétipo", style = MaterialTheme.typography.titleMedium, color = ExaltedAccentBright)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ARQUETIPO_OPCOES.forEach { (arq, rotulo) ->
                        val selecionado = arquetipoSelecionado == arq && customFoco == null
                        InkButton(
                            label = rotulo,
                            onClick = {
                                customFoco = null
                                customAtaque = null
                                customDefesa = null
                                customSecundaria = null
                                onArquetipoChange(arq)
                            },
                            modifier = Modifier.fillMaxWidth(0.49f),
                            size = InkButtonSize.Small,
                            customHeight = 48.dp,
                            variant = InkButtonVariant.Secondary,
                            selected = selecionado,
                            brushIndex = 2
                        )
                    }
                    InkButton(
                        label = "Foco",
                        onClick = { mostrarPersonalizar = true },
                        modifier = Modifier.fillMaxWidth(0.49f),
                        size = InkButtonSize.Small,
                        customHeight = 48.dp,
                        variant = InkButtonVariant.Secondary,
                        selected = customFoco != null || customAtaque != null || customDefesa != null || customSecundaria != null,
                        brushIndex = 2
                    )
                }
                }

                if (tipoSangueDeDragao) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().exaltedSectionPanel(4).padding(12.dp)
                    ) {
                    AppText("4. Origem", style = MaterialTheme.typography.titleMedium, color = ExaltedAccentBright)
                    Spacer(modifier = Modifier.height(8.dp))
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val compactOriginButtons = maxWidth < 600.dp
                        if (!compactOriginButtons) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ORIGEM_SANGUE_DE_DRAGAO_OPCOES.forEach { (origem, rotulo) ->
                                    val selecionado = origemNomeSelecionada == origem
                                    InkButton(
                                        label = rotulo,
                                        onClick = { origemNomeSelecionada = origem },
                                        modifier = Modifier.weight(1f),
                                        size = InkButtonSize.Small,
                                        variant = InkButtonVariant.Secondary,
                                        selected = selecionado,
                                        brushIndex = 0
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ORIGEM_SANGUE_DE_DRAGAO_OPCOES.take(2).forEach { (origem, rotulo) ->
                                        val selecionado = origemNomeSelecionada == origem
                                        InkButton(
                                            label = rotulo,
                                            onClick = { origemNomeSelecionada = origem },
                                            modifier = Modifier.weight(1f),
                                            size = InkButtonSize.Small,
                                            variant = InkButtonVariant.Secondary,
                                            selected = selecionado,
                                            brushIndex = 0
                                        )
                                    }
                                }
                                ORIGEM_SANGUE_DE_DRAGAO_OPCOES.getOrNull(2)?.let { (origem, rotulo) ->
                                    val selecionado = origemNomeSelecionada == origem
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        InkButton(
                                            label = rotulo,
                                            onClick = { origemNomeSelecionada = origem },
                                            modifier = Modifier.fillMaxWidth(0.5f),
                                            size = InkButtonSize.Small,
                                            variant = InkButtonVariant.Secondary,
                                            selected = selecionado,
                                            brushIndex = 0
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // O antigo modo separado foi consolidado em Foco. A geração
                // agora usa diretamente os botões e campos visíveis acima.
                Spacer(modifier = Modifier.height(12.dp))
                if (mensagemLimite != null) {
                    AppText(
                        mensagemLimite,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
                val erroGeracaoAtual = erroGeracao
                if (erroGeracaoAtual != null) {
                    AppText(
                        erroGeracaoAtual,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
                if (gerando) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ExaltedAccentBright,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AppText("Gerando NPC…", style = MaterialTheme.typography.bodySmall, color = ExaltedMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (npcsGerados.isEmpty() || (npcsGerados.size == 1 && pendingSelectId != null)) {
            // O segundo caso (1 NPC + seleção ainda pendente) é o próprio
            // primeiro NPC gerado: diferente de quando já existem abas
            // anteriores, aqui não existe nenhum índice "seguro" pra
            // recuar enquanto a aba nova não termina de ser medida — não
            // tem aba antiga nenhuma. Mostrar o mesmo texto de carregamento
            // por 2 frames evita renderizar o ScrollableTabRow nesse
            // instante, em vez de arriscar um índice que ainda não existe.
            AppText(
                "Nenhum NPC gerado ainda.",
                style = MaterialTheme.typography.bodySmall,
                color = ExaltedMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            // Até 10 NPCs simultâneos, cada um em sua própria aba — a aba
            // selecionada é rastreada pelo id do NPC (não pelo índice), pra
            // continuar apontando pro NPC certo mesmo depois de fechar
            // outra aba no meio da lista.
            // PERFORMANCE: resolve NPC ativo e índice da aba em uma única
            // passagem. O índice era recalculado separadamente a cada
            // recomposição do ScrollableTabRow.
            val selecaoAba = remember(npcsGerados, abaSelecionadaId) {
                val indiceAtivo = npcsGerados.indexOfFirst { it.id == abaSelecionadaId }
                    .takeIf { it >= 0 } ?: npcsGerados.lastIndex
                npcsGerados[indiceAtivo] to indiceAtivo
            }
            val npcAtivo = selecaoAba.first

            val rotulosAba = remember(npcsGerados) {
                val nomeOcorrenciaAteAqui = mutableMapOf<String, Int>()
                npcsGerados.mapIndexed { index, npc ->
                    val base = npc.nome.ifBlank { "NPC ${index + 1}" }
                    val ocorrencia = (nomeOcorrenciaAteAqui[base] ?: 0) + 1
                    nomeOcorrenciaAteAqui[base] = ocorrencia
                    npc.id to if (ocorrencia > 1) "$base ($ocorrencia)" else base
                }.toMap()
            }

            // Posição horizontal dos botões/NPCs é estado da Aba 11, não
            // estado descartável da composição do ScrollableTabRow. O
            // rememberSaveable mantém exatamente a posição de rolagem
            // enquanto o usuário navega entre as abas da planilha.
            // Ao encerrar o app, esse estado deixa de existir junto com a
            // instância da tela.
            val npcBotoesScrollState = rememberSaveable(
                saver = ScrollState.Saver
            ) { ScrollState(0) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(npcBotoesScrollState)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    npcsGerados.forEach { npc ->
                        val selecionada = npc.id == npcAtivo.id
                        // A identidade da aba pertence ao NPC, não à área de origem
                        // nem à skin global atualmente aberta.
                        val npcTabTemplate = visualTemplateParaExaltado(npc.tipoExaltado)
                        val corNome = if (selecionada) {
                            npcTabTemplate.accentBright
                        } else {
                            npcTabTemplate.accent
                        }
                        androidx.compose.material3.Tab(
                            selected = selecionada,
                            onClick = { onAbaSelecionadaChange(npc.id) },
                            selectedContentColor = npcTabTemplate.accentBright,
                            unselectedContentColor = npcTabTemplate.accent,
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppText(
                                        rotulosAba[npc.id] ?: npc.nome,
                                        color = corNome,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .feedbackClickable {
                                                viewModel.removeNpcEncontro(npc.id)
                                                if (abaSelecionadaId == npc.id) onAbaSelecionadaChange(null)
                                                onMensagemLimiteChange(null)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppText(
                                            "✕",
                                            color = npcTabTemplate.muted,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            NpcEncontroCard(
                npc = npcAtivo,
                sheet = sheetAtual,
                viewModel = viewModel,
                gerando = gerando,
                onCiclarDano = { boxId -> viewModel.cycleHealthDamageNpcEncontro(npcAtivo.id, boxId) },
                onLimparDano = { viewModel.clearHealthDamageNpcEncontro(npcAtivo.id) },
                onAjustarIniciativa = { delta -> viewModel.ajustarIniciativaNpcEncontro(npcAtivo.id, delta) },
                onJuntarSeABatalha = { viewModel.juntarNpcEncontroABatalha(npcAtivo.id) },
                onAumentarExperiencia = { viewModel.expandirEncantosNpcEncontro(npcAtivo.id) },
                onDiminuirExperiencia = { viewModel.reduzirExperienciaNpcEncontro(npcAtivo.id) }
            )
        }
    }
}

