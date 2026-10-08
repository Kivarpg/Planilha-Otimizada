package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.example.ui.components.InkButton
import com.example.model.CharacterType
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import com.example.ui.MainSheetScreen
import com.example.ui.components.SplashScreen
import com.example.ui.components.AppText
import com.example.ui.theme.ExaltedTheme
import com.example.viewmodel.SheetViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SheetViewModel by viewModels()

    private companion object {
        const val MAX_CRASH_LOG_CHARS = 262_144
    }

    @androidx.compose.runtime.Composable
    private fun TelaDeErroAnterior(texto: String, onDescartar: () -> Unit) {
        val contexto = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .padding(16.dp)
        ) {
            AppText(
                "O app fechou inesperadamente na última vez. Aqui está o motivo exato — copie e envie:",
                color = androidx.compose.ui.graphics.Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row {
                InkButton(
                    label = "Copiar erro",
                    onClick = {
                        val clipboard = contexto.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("erro", texto))
                    },
                    size = com.example.ui.components.InkButtonSize.Small,
                    brushIndex = 1
                )
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.width(8.dp))
                InkButton(
                    label = "Descartar e continuar",
                    onClick = onDescartar,
                    size = com.example.ui.components.InkButtonSize.Small,
                    brushIndex = 2
                )
            }
            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                AppText(
                    texto,
                    color = androidx.compose.ui.graphics.Color(0xFFFF8A80),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ (targetSdk 31+) sempre mostra a splash do sistema antes
        // do primeiro frame Compose. Sem installSplashScreen(), essa tela
        // padrão (clara) some de imediato e a animação Compose da Aba de
        // abertura ou não aparece, ou só um clarão branco. Mantemos a splash
        // do sistema (fundo preto + logo) até o Compose montar a SplashScreen
        // animada; em seguida liberamos e a sequência de emblemas roda.
        val systemSplash = installSplashScreen()
        var composeSplashPronta = false
        systemSplash.setKeepOnScreenCondition { !composeSplashPronta }

        super.onCreate(savedInstanceState)
        // Captura qualquer erro não tratado (o que causaria o "app parou")
        // e salva o stack trace completo num arquivo dentro da própria
        // pasta do app, ANTES de deixar o comportamento padrão do Android
        // seguir seu curso (que ainda mostra o diálogo normal de "parou" —
        // isso aqui só grava o motivo exato pra consulta depois, em
        // Opções > Ver Log de Erros, sem precisar de ADB nem computador).
        val manipuladorPadrao = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erro ->
            try {
                val arquivo = java.io.File(filesDir, "ultimo_erro.txt")
                val buffer = java.io.StringWriter()
                java.io.PrintWriter(buffer).use { escritor ->
                    escritor.println("Data: ${java.util.Date()}")
                    escritor.println("Thread: ${thread.name}")
                    erro.printStackTrace(escritor)
                }
                // Diagnóstico local é útil, mas não deve poder crescer sem limite
                // por causa de mensagens/causas patológicas.
                arquivo.writeText(buffer.toString().take(MAX_CRASH_LOG_CHARS))
            } catch (e: Exception) {
                // Se nem gravar o log for possível, não há mais nada a
                // fazer além de deixar o comportamento padrão assumir.
            }
            manipuladorPadrao?.uncaughtException(thread, erro)
        }
        enableEdgeToEdge()
        // Se a última sessão terminou em crash, mostra o log IMEDIATAMENTE
        // na abertura, antes de qualquer outra coisa — pedido implícito do
        // usuário: se o app trava/fecha muito cedo, ele nunca consegue
        // navegar até "Opções > Ver Log de Erros" pra ver o motivo. Assim
        // dá pra copiar o erro e mandar sem precisar de ADB/computador.
        val arquivoDeErro = java.io.File(filesDir, "ultimo_erro.txt")
        setContent {
            var erroAnterior by remember { mutableStateOf<String?>(null) }
            var verificacaoErroConcluida by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                erroAnterior = withContext(Dispatchers.IO) {
                    runCatching { if (arquivoDeErro.exists()) arquivoDeErro.readText() else null }.getOrNull()
                }
                verificacaoErroConcluida = true
                if (erroAnterior != null) {
                    composeSplashPronta = true
                }
            }
            if (!verificacaoErroConcluida) return@setContent
            ExaltedTheme {
                val erroAnteriorExibicao = erroAnterior
                if (erroAnteriorExibicao != null) {
                    TelaDeErroAnterior(
                        texto = erroAnteriorExibicao,
                        onDescartar = {
                            arquivoDeErro.delete()
                            erroAnterior = null
                        }
                    )
                    return@ExaltedTheme
                }
                var showSplash by remember { mutableStateOf(true) }
                var templateEscolhido by rememberSaveable { mutableStateOf<String?>(null) }
                if (showSplash) {
                    var appReady by remember { mutableStateOf(false) }
                    val contextoAtual = androidx.compose.ui.platform.LocalContext.current
                    LaunchedEffect(Unit) {
                        // Pequeno atraso para o primeiro frame preto da
                        // SplashScreen Compose ser desenhado antes de
                        // soltar a splash do sistema — evita salto
                        // preto→branco sem os emblemas.
                        androidx.compose.runtime.withFrameNanos { }
                        androidx.compose.runtime.withFrameNanos { }
                        composeSplashPronta = true
                        appReady = true
                    }
                    SplashScreen(
                        isAppReady = appReady,
                        onFinished = { showSplash = false }
                    )
                } else if (templateEscolhido == null) {
                    com.example.ui.TemplateSelectionScreen(
                        onTemplateSelected = { templateEscolhido = it }
                    )
                } else {
                    val tipoPersonagem = when (templateEscolhido) {
                        "Sangue de Dragão" -> CharacterType.DRAGON_BLOODED
                        "Lunar" -> CharacterType.LUNAR
                        else -> CharacterType.SOLAR
                    }
                    MainSheetScreen(viewModel = viewModel, tipoPersonagem = tipoPersonagem)
                }
            }
        }
    }
}
