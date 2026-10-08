package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

/**
 * Feedback tátil/sonoro centralizado para interações do aplicativo.
 * O sistema usa o próprio feedback do Android para manter a sensação de
 * toque coerente com o dispositivo, sem criar arquivos de áudio próprios.
 */
object InteractionFeedback {
    private const val PREFS = "interaction_feedback_preferences"
    private const val HAPTIC = "haptic_enabled"
    private const val SOUND = "sound_enabled"

    fun isHapticEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(HAPTIC, true)

    fun isSoundEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(SOUND, true)

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(HAPTIC, enabled).apply()
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(SOUND, enabled).apply()
    }

    fun perform(context: Context) {
        val activity = context.findActivity() ?: return
        perform(activity.window.decorView)
    }

    fun perform(view: View) {
        performInternal(view, typing = false, soundEffect = AudioManager.FX_KEY_CLICK)
    }

    fun performTyping(view: View) {
        performInternal(view, typing = true, soundEffect = AudioManager.FX_KEYPRESS_STANDARD)
    }

    private fun performInternal(view: View, typing: Boolean, soundEffect: Int) {
        val context = runCatching { view.context }.getOrNull() ?: return
        if (isHapticEnabled(context)) {
            // A preferência do próprio aplicativo é a fonte de verdade para o feedback.
            // performHapticFeedback respeita a configuração global do Android e pode
            // aceitar a chamada sem produzir um pulso perceptível em alguns aparelhos.
            // Para os botões do Exalted, usamos diretamente um efeito curto e previsível.
            val vibrator = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            }.getOrNull()

            val vibrated = if (vibrator?.hasVibrator() == true) {
                val durationMs = if (typing) 18L else 36L
                // Intensidade explícita: DEFAULT_AMPLITUDE ficava quase imperceptível
                // em alguns aparelhos. Mantemos um pulso curto, mais firme e perceptível, preservando pulsos curtos para não tornar a interação cansativa.
                val amplitude = if (typing) 185 else 230
                runCatching {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, amplitude)
                    )
                    true
                }.getOrDefault(false)
            } else false

            if (!vibrated) {
                // Última linha de defesa para aparelhos/ROMs que não expõem o
                // serviço Vibrator corretamente.
                runCatching {
                    view.performHapticFeedback(
                        if (typing) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.VIRTUAL_KEY
                    )
                }
            }
        }
        if (isSoundEnabled(context)) {
            runCatching {
                context.getSystemService(AudioManager::class.java)?.playSoundEffect(soundEffect, 1.0f)
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
fun rememberTypingFeedback(onValueChange: (String) -> Unit): (String) -> Unit {
    val view = LocalView.current
    return remember(view, onValueChange) {
        { newValue -> InteractionFeedback.performTyping(view); onValueChange(newValue) }
    }
}

fun Modifier.feedbackClickable(
    enabled: Boolean = true,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val view = LocalView.current
    val source = interactionSource ?: MutableInteractionSource()
    Modifier.clickable(
        interactionSource = source,
        indication = indication,
        enabled = enabled,
        role = role,
        onClick = {
            InteractionFeedback.perform(view)
            onClick()
        }
    )
}

/** Feedback de pressão para componentes Compose interativos sem substituir seu onClick. */
fun Modifier.feedbackOnPress(enabled: Boolean = true): Modifier = composed {
    val view = LocalView.current
    Modifier.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            InteractionFeedback.perform(view)
            waitForUpOrCancellation()
        }
    }
}

/** Variante de combinedClickable com feedback no clique e no long press. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.feedbackCombinedClickable(enabled: Boolean = true, onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val view = LocalView.current
    Modifier.combinedClickable(
        enabled = enabled,
        onClick = { InteractionFeedback.perform(view); onClick() },
        onLongClick = onLongClick?.let { action -> { InteractionFeedback.perform(view); action() } },
    )
}
