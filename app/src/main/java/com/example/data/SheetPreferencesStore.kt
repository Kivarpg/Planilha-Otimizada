package com.example.data

import android.content.SharedPreferences
import com.example.model.RatingStyle

/** Small global preferences that are not part of a character sheet. */
class SheetPreferencesStore(private val prefs: SharedPreferences) {
    fun nextSequence(): Int {
        val next = prefs.getInt(KEY_NEXT_SEQUENCE, 1)
        prefs.edit().putInt(KEY_NEXT_SEQUENCE, next + 1).apply()
        return next
    }

    fun ratingStyle(): RatingStyle = runCatching {
        RatingStyle.valueOf(
            prefs.getString(KEY_RATING_STYLE, RatingStyle.STEPPER.name)
                ?: RatingStyle.STEPPER.name
        )
    }.getOrDefault(RatingStyle.STEPPER)

    fun setRatingStyle(style: RatingStyle) {
        prefs.edit().putString(KEY_RATING_STYLE, style.name).apply()
    }

    private companion object {
        const val KEY_NEXT_SEQUENCE = "exalted_next_save_sequence"
        const val KEY_RATING_STYLE = "exalted_rating_style"
    }
}
