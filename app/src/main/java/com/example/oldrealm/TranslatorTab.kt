package com.example.oldrealm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.components.exaltedContentStage
import com.example.ui.components.exaltedTabIdentity
import com.example.ui.components.InkButtonSize
import com.example.ui.components.InkButton
import com.example.ui.components.feedbackOnPress

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.oldrealm.highrealm.HighRealmGlyphCanvas
import com.example.oldrealm.highrealm.HighRealmTranslator
import com.example.oldrealm.TranslatorEngine.WritingSystem
import com.example.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Single converter tab. Old Realm and High Realm keep independent linguistic engines. */
@Composable
fun TranslatorTab(
    modifier: Modifier = Modifier,
    background: Color = Color(0xFF050505),
    gold: Color = com.example.ui.theme.ExaltedAccentBright,
    onOriginalInputChanged: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember(context) { context.getSharedPreferences("translator_preferences", 0) }
    var system by remember {
        mutableStateOf(if (prefs.getString("writing_system", "OLD") == "HIGH") WritingSystem.HIGH else WritingSystem.OLD)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var inputValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var committedInput by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<TranslatorEngine.Result?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val language = remember(locale.language) { OldRealmTranslator.detectLanguage(locale) }
    val resultGraphicsLayer = rememberGraphicsLayer()

    // WRITE_EXTERNAL_STORAGE só é necessária em Android 7–9 (API 24–28); a
    // partir do Android 10 o MediaStore com armazenamento com escopo não
    // exige essa permissão para salvar na coleção de Imagens.
    fun capturarEsalvar() {
        isCapturing = true
        coroutineScope.launch {
            try {
                val bitmap = resultGraphicsLayer.toImageBitmap().asAndroidBitmap()
                val sucesso = salvarBitmapComoImagem(context, bitmap, system.label, committedInput)
                if (!sucesso) {
                    snackbarHostState.showSnackbar("Não foi possível salvar a imagem. Tente novamente.")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Erro ao salvar a imagem: ${e.message ?: "erro desconhecido"}.")
            } finally {
                isCapturing = false
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida -> if (concedida) capturarEsalvar() }

    LaunchedEffect(committedInput, language, system) {
        val current = committedInput
        result = withContext(Dispatchers.Default) {
            TranslatorEngine.translate(current, system, locale)
        }
    }

    LaunchedEffect(committedInput) {
        onOriginalInputChanged?.invoke(committedInput)
    }

    fun selectSystem(newSystem: WritingSystem) {
        system = newSystem
        prefs.edit().putString("writing_system", newSystem.key).apply()
        menuExpanded = false
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().exaltedTabIdentity(15).exaltedContentStage(15).background(background.copy(alpha = 0.78f)).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // APPROVED VISUAL CUSTOMIZATION
        // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box {
                InkButton(label = system.label, onClick = { menuExpanded = true }, size = InkButtonSize.Small)
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    WritingSystem.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { AppText(option.label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = { selectSystem(option) }
                        ,
    modifier = Modifier.feedbackOnPress(enabled = true)
)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            InkButton(
                label = "Limpar",
                onClick = {
                    inputValue = TextFieldValue("")
                    committedInput = ""
                },
                size = InkButtonSize.Small,
                modifier = Modifier.feedbackOnPress(enabled = inputValue.text.isNotEmpty())
            )
            Spacer(Modifier.weight(1f))
            // Copia a área de resultado traduzido como imagem e inicia o
            // download automaticamente, sem nenhuma ação adicional do usuário.
            if (isCapturing) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), color = gold, strokeWidth = 2.dp)
            } else {
                InkButton(
                    onClick = {
                        if (result == null) return@InkButton
                        val precisaPermissao = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        if (precisaPermissao) {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            capturarEsalvar()
                        }
                    }
                ,
    modifier = Modifier.feedbackOnPress(enabled = true)
) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Copiar como imagem", tint = gold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // key(system): força uma instância nova do campo (e uma conexão de
        // IME nova) sempre que o sistema de escrita muda. Sem isso, abrir o
        // DropdownMenu de seleção de idioma podia deixar a conexão de IME
        // anterior num estado inconsistente — o campo parava de exibir o que
        // era digitado até o usuário trocar de aba e voltar (o que força uma
        // recomposição completa e, de quebra, uma conexão de IME nova).
        androidx.compose.runtime.key(system) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = { value ->
                inputValue = value
                if (value.composition == null) committedInput = value.text
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            maxLines = 8,
            label = { AppText("Digite", style = MaterialTheme.typography.bodyMedium) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = gold,
                unfocusedBorderColor = gold.copy(alpha = 0.45f),
                focusedLabelColor = gold,
                cursorColor = gold,
                focusedTextColor = Color(0xFFECE6D8),
                unfocusedTextColor = Color(0xFFECE6D8),
                focusedContainerColor = Color(0xFF0F1113),
                unfocusedContainerColor = Color(0xFF0F1113)
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default,
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = false
            )
        )
        }
        // Espaço generoso sob o campo de digitação: antes o Spacer de 28.dp
        // não bastava e a caixa (label flutuante + minLines=2) cobria o topo
        // dos glifos traduzidos. Empurra o resultado para baixo e clipa
        // para o conteúdo não "vazar" por cima do campo.
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 24.dp)
                .clipToBounds()
                .drawWithContent {
                    // Grava o conteúdo renderizado nesta camada, para poder ser
                    // exportado como bitmap sob demanda pelo botão de captura.
                    resultGraphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(resultGraphicsLayer)
                }
        ) {
            when (val current = result) {
                is TranslatorEngine.Result.OldRealm -> OldRealmResult(current.value, gold)
                is TranslatorEngine.Result.HighRealm -> HighRealmResult(current.value, gold)
                null -> Unit
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
    )
    }
}

// Salva o bitmap capturado na coleção de imagens do dispositivo (Pictures),
// iniciando o "download" imediatamente — nenhuma ação adicional é exigida
// do usuário além de tocar no botão de captura.

// Converte o texto original (já digitado pelo usuário, normalmente em
// português/alfabeto romano) num nome de arquivo seguro: remove acentos
// via decomposição Unicode (é->e, ã->a, ç->c, etc. — cobre o caso real de
// uso deste app), descarta qualquer caractere que sobre fora do alfabeto
// romano básico (script realmente diferente, tipo cirílico/CJK, não tem
// como "transliterar" sem uma tabela de mapeamento fonético dedicada — o
// caractere é simplesmente removido em vez de gerar lixo no nome), e troca
// os caracteres inválidos em nome de arquivo (/ \ : * ? " < > |) por "-".
// Limitado a 80 caracteres pra não estourar o limite do sistema de
// arquivos em textos de entrada muito longos.
private fun textoParaNomeDeArquivoSeguro(textoOriginal: String): String {
    val semAcentos = java.text.Normalizer.normalize(textoOriginal, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "") // remove os marcadores de acento isolados pela decomposicao NFD
    val apenasRomano = semAcentos.filter { ch -> ch.code < 128 || ch == ' ' }
    val semInvalidos = apenasRomano
        .replace(Regex("[/\\\\:*?\"<>|]"), "-")
        .trim()
    val resultado = if (semInvalidos.isBlank()) "Sem Texto" else semInvalidos
    return resultado.take(80).trim()
}

private fun salvarBitmapComoImagem(context: android.content.Context, bitmap: Bitmap, idioma: String, textoOriginal: String): Boolean {
    val textoSeguro = textoParaNomeDeArquivoSeguro(textoOriginal)
    val nomeArquivo = "[$idioma] $textoSeguro.png"
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, nomeArquivo)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Tradutor")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    val escritaOk = resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    } ?: false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    return escritaOk
}

private typealias WritingSystem = TranslatorEngine.WritingSystem

@Composable
private fun OldRealmResult(translation: OldRealmTranslator.Translation, gold: Color) {
    if (translation.lines.isEmpty()) return
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 32.dp)
                .verticalScroll(vertical)
                .horizontalScroll(horizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            translation.lines.forEach { line ->
                OldRealmGlyphCanvas(
                    words = line,
                    glyphColor = gold,
                    modifier = Modifier.fillMaxWidth(),
                    viewportWidthPx = viewportWidthPx
                )
            }
        }
    }
}

@Composable
private fun HighRealmResult(translation: HighRealmTranslator.Translation, gold: Color) {
    if (translation.lines.isEmpty()) return
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxRows = with(density) { (maxHeight.toPx() / 44.dp.toPx()).toInt().coerceIn(4, 24) }
        val columnWidth = (maxWidth / 4f).coerceIn(56.dp, 96.dp)
        // Cada quebra de linha (Enter) do usuário força o início de uma nova
        // coluna — o texto flui horizontalmente, nunca empilhado verticalmente.
        // Cada "linha" do input é empacotada separadamente (garantindo que
        // sempre comece em uma coluna nova), e todas as colunas resultantes
        // de todas as linhas são então exibidas lado a lado, em sequência.
        val colunas = remember(translation, maxRows) {
            translation.lines.flatMap { linha ->
                com.example.oldrealm.highrealm.HighRealmLayout.pack(linha, maxRows = maxRows)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .verticalScroll(vertical)
                .horizontalScroll(horizontal),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            colunas.forEach { coluna ->
                HighRealmGlyphCanvas(
                    column = coluna,
                    glyphColor = gold,
                    modifier = Modifier.width(columnWidth)
                )
            }
        }
    }
}

/** Backward-compatible entry point for integrations that already call the old tab. */
@Composable
fun OldRealmTranslatorTab(
    modifier: Modifier = Modifier,
    background: Color = Color(0xFF050505),
    gold: Color = com.example.ui.theme.ExaltedAccentBright,
    onOriginalInputChanged: ((String) -> Unit)? = null
) = TranslatorTab(modifier, background, gold, onOriginalInputChanged)
