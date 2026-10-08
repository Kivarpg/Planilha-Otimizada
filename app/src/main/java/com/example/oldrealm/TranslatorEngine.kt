package com.example.oldrealm

import com.example.oldrealm.highrealm.HighRealmTranslator
import java.util.Locale

/** Stable integration API. The host application only needs this facade and TranslatorTab. */
object TranslatorEngine {
    enum class WritingSystem(val key: String, val label: String) {
        OLD("OLD", "Antigo Reino"),
        HIGH("HIGH", "Alto Reino")
    }

    sealed interface Result {
        val originalInput: String
        val normalizedInput: String

        data class OldRealm(val value: OldRealmTranslator.Translation) : Result {
            override val originalInput get() = value.originalInput
            override val normalizedInput get() = value.normalizedInput
        }

        data class HighRealm(val value: HighRealmTranslator.Translation) : Result {
            override val originalInput get() = value.originalInput
            override val normalizedInput get() = value.normalizedInput
        }
    }

    fun translate(
        input: String,
        system: WritingSystem,
        locale: Locale = Locale.getDefault()
    ): Result = when (system) {
        WritingSystem.OLD -> Result.OldRealm(OldRealmTranslator.translate(input, locale))
        WritingSystem.HIGH -> Result.HighRealm(HighRealmTranslator.translate(input))
    }
}
