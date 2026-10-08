package com.example.ui.tabs
import com.example.ui.components.InkButtonSize
import com.example.ui.components.feedbackOnPress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.example.ui.components.AppText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CharacterSheet
import com.example.viewmodel.SheetViewModel
import com.example.ui.theme.*
import com.example.ui.components.InkButton

@Composable
internal fun IntimidadeDialog(show:Boolean,nome:String,tipo:String,intensidade:String,onNome:(String)->Unit,onTipo:(String)->Unit,onIntensidade:(String)->Unit,onDismiss:()->Unit,viewModel:SheetViewModel){
    if(!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = { AppText("Intimidade", color = ExaltedGold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = com.example.ui.components.rememberTypingFeedback { onNome(it.take(60)) },
                    label = { AppText("Intimidade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExaltedAccentBright,
                        unfocusedBorderColor = ExaltedOutline.copy(alpha = 0.55f),
                        focusedLabelColor = ExaltedAccentBright,
                        unfocusedLabelColor = ExaltedMuted,
                        cursorColor = ExaltedGold,
                        focusedTextColor = ExaltedOnSurface,
                        unfocusedTextColor = ExaltedOnSurface,
                        focusedContainerColor = ExaltedDarkSurface,
                        unfocusedContainerColor = ExaltedDarkSurface,
                        focusedPlaceholderColor = ExaltedMuted,
                        unfocusedPlaceholderColor = ExaltedMuted
                    )
                )
                AppText("Tipo", color = ExaltedAmber, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Princípio", "Laço").forEach { opcaoTipo ->
                        InkButton(
                            label = opcaoTipo,
                            selected = tipo == opcaoTipo,
                            onClick = { onTipo(opcaoTipo) },
                            modifier = Modifier.weight(1f),
                            fillMaxWidth = true,
                            brushIndex = Math.floorMod(opcaoTipo.hashCode(), 3)
                        )
                    }
                }
                AppText("Intensidade", color = ExaltedAmber, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Definidora", "Maior", "Menor").forEach { opcaoIntensidade ->
                        InkButton(
                            label = opcaoIntensidade,
                            selected = intensidade == opcaoIntensidade,
                            onClick = { onIntensidade(opcaoIntensidade) },
                            modifier = Modifier.weight(1f),
                            fillMaxWidth = true,
                            brushIndex = Math.floorMod(opcaoIntensidade.hashCode(), 3)
                        )
                    }
                }
            }
        },
        dismissButton = {
            com.example.ui.components.GildedDialogButton(
                text = "Cadastrar",
                onClick = {
                    viewModel.addIntimidade(nome, tipo, intensidade)
                    if (nome.isNotBlank()) {
                        onNome("")
                        onDismiss()
                    }
                },
                enabled = nome.isNotBlank()
            )
        },
        confirmButton = {
            com.example.ui.components.GildedDialogTextButton(text = "Cancelar", onClick = { onDismiss() })
        },
        containerColor = ExaltedDarkSurfaceVariant
    )
}

@Composable
internal fun LinguaDialog(show:Boolean,sheet:CharacterSheet,onDismiss:()->Unit,viewModel:SheetViewModel){
    if(!show) return

    // Lista base fixa (sem "Outra", que é só um gatilho de UI, nunca
    // um valor persistido) + "Outra" sempre por último.
    val idiomasBase = listOf(
        "Alto Reino", "Antigo Reino", "Baixo Reino", "Dialeto da Guilda",
        "Dialeto dos Rios", "Idioma da Floresta", "Idioma do Fogo",
        "Idioma do Céu", "Idioma do Mar", "Idioma dos Dragões"
    )
    val todasAsOpcoes = idiomasBase + "Outra"

    var menuNativaAberto by remember { mutableStateOf(false) }
    var menuAdicionarAberto by remember { mutableStateOf(false) }
    // Se o idioma nativo atual não está na lista base, foi digitada
    // manualmente via "Outra" — pré-preenche o campo de texto com ela.
    var textoCustomNativa by remember(sheet.linguaNativa) {
        mutableStateOf(if (sheet.linguaNativa != null && sheet.linguaNativa !in idiomasBase) sheet.linguaNativa else "")
    }
    var editandoNativaCustom by remember(sheet.linguaNativa) {
        mutableStateOf(sheet.linguaNativa != null && sheet.linguaNativa !in idiomasBase)
    }
    var textoCustomNova by remember { mutableStateOf("") }
    var adicionandoCustomNova by remember { mutableStateOf(false) }

    // Opções disponíveis para Idioma Nativo: todas menos as já usadas
    // como adicionais (mas incluindo a própria nativa atual, senão ela
    // desapareceria do próprio seletor).
    val opcoesParaNativa = todasAsOpcoes.filter { it == "Outra" || it !in sheet.linguasAdicionais }
    // Opções disponíveis para Idiomas Adicionais: todas menos a nativa e menos
    // as adicionais já escolhidas.
    val opcoesParaAdicionar = todasAsOpcoes.filter {
        (it == "Outra" || it != sheet.linguaNativa) && it !in sheet.linguasAdicionais
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(com.example.ui.components.gildedDialogBorder()),
        shape = com.example.ui.components.dialogShape,
        title = {
            AppText("Idioma", color = ExaltedAccentBright, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), forceStroke = true)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // --- Idioma Nativo ---
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppText("Idioma nativo", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                    Box {
                        InkButton(label = sheet.linguaNativa ?: "Selecionar…", onClick = { menuNativaAberto = true }, modifier = Modifier.fillMaxWidth(), fillMaxWidth = true, size = InkButtonSize.Small)
                        DropdownMenu(expanded = menuNativaAberto, onDismissRequest = { menuNativaAberto = false }) {
                            opcoesParaNativa.forEach { opcao ->
                                DropdownMenuItem(
                                    text = { AppText(opcao) },
                                    onClick = {
                                        menuNativaAberto = false
                                        if (opcao == "Outra") {
                                            editandoNativaCustom = true
                                            textoCustomNativa = ""
                                        } else {
                                            editandoNativaCustom = false
                                            viewModel.updateLinguaNativa(opcao)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (editandoNativaCustom) {
                        OutlinedTextField(
                            value = textoCustomNativa,
                            onValueChange = com.example.ui.components.rememberTypingFeedback { novoBruto ->
                                val novo = novoBruto.take(40)
                                textoCustomNativa = novo
                                val texto = novo.trim()
                                // Valida: não permite ficar igual a um idioma já usado nos adicionais.
                                if (texto.isNotEmpty() && texto !in sheet.linguasAdicionais) {
                                    viewModel.updateLinguaNativa(texto)
                                }
                            },
                            label = { AppText("Nome do idioma") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // --- Idiomas Adicionais ---
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppText("Idiomas adicionais", style = MaterialTheme.typography.labelMedium, color = ExaltedGold)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sheet.linguasAdicionais.forEach { lingua ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AppText(
                                    lingua,
                                    color = ExaltedOnSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                com.example.ui.components.GildedDialogTextButton(
                                    text = "Remover",
                                    onClick = { viewModel.removeLinguaAdicional(lingua) },
                                    isDanger = true
                                )
                            }
                        }
                    }
                    Box {
                        InkButton(label = "+ Adicionar idioma", onClick = { menuAdicionarAberto = true }, modifier = Modifier.fillMaxWidth(), fillMaxWidth = true, enabled = opcoesParaAdicionar.isNotEmpty(), size = InkButtonSize.Small)
                        DropdownMenu(expanded = menuAdicionarAberto, onDismissRequest = { menuAdicionarAberto = false }) {
                            opcoesParaAdicionar.forEach { opcao ->
                                DropdownMenuItem(
                                    text = { AppText(opcao) },
                                    onClick = {
                                        menuAdicionarAberto = false
                                        if (opcao == "Outra") {
                                            adicionandoCustomNova = true
                                            textoCustomNova = ""
                                        } else {
                                            viewModel.addLinguaAdicional(opcao)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (adicionandoCustomNova) {
                        OutlinedTextField(
                            value = textoCustomNova,
                            onValueChange = com.example.ui.components.rememberTypingFeedback { textoCustomNova = it.take(40) },
                            label = { AppText("Nome do idioma") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        com.example.ui.components.GildedDialogButton(
                            text = "Confirmar",
                            onClick = {
                                val texto = textoCustomNova.trim()
                                if (texto.isNotEmpty() && texto != sheet.linguaNativa && texto !in sheet.linguasAdicionais) {
                                    viewModel.addLinguaAdicional(texto)
                                    textoCustomNova = ""
                                    adicionandoCustomNova = false
                                }
                            },
                            enabled = textoCustomNova.isNotBlank()
                        )
                    }
                }
            }
        },
        confirmButton = {
            com.example.ui.components.GildedDialogButton(text = "Fechar", onClick = { onDismiss() })
        },
        containerColor = ExaltedDarkSurfaceVariant
    )

}